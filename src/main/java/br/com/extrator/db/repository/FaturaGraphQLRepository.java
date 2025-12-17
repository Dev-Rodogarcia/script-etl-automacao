package br.com.extrator.db.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;

import br.com.extrator.db.entity.FaturaGraphQLEntity;

public class FaturaGraphQLRepository extends AbstractRepository<FaturaGraphQLEntity> {
    private static final String NOME_TABELA = "faturas_graphql";
    private static boolean viewVerificada = false;

    @Override
    protected String getNomeTabela() {
        return NOME_TABELA;
    }

    @Override
    protected void criarTabelaSeNaoExistir(final Connection conexao) throws SQLException {
        final String sql = """
            IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'faturas_graphql')
            BEGIN
                CREATE TABLE dbo.faturas_graphql (
                    id BIGINT PRIMARY KEY,
                    document NVARCHAR(50),
                    issue_date DATE,
                    due_date DATE,
                    value DECIMAL(18,2),
                    customer_id BIGINT,
                    customer_name NVARCHAR(255),
                    customer_cnpj NVARCHAR(50),
                    metadata NVARCHAR(MAX),
                    data_extracao DATETIME2 DEFAULT GETDATE()
                );
                CREATE INDEX IX_fg_document ON dbo.faturas_graphql(document);
            END
        """;
        executarDDL(conexao, sql);
        if (!viewVerificada) {
            criarViewPowerBISeNaoExistir(conexao);
            viewVerificada = true;
        }
    }

    @Override
    protected int executarMerge(final Connection conexao, final FaturaGraphQLEntity e) throws SQLException {
        final String sql = """
            MERGE dbo.faturas_graphql AS target
            USING (VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)) AS source
                  (id, document, issue_date, due_date, value, customer_id, customer_name, customer_cnpj, metadata, data_extracao)
            ON target.id = source.id
            WHEN MATCHED THEN
                UPDATE SET
                    document = source.document,
                    issue_date = source.issue_date,
                    due_date = source.due_date,
                    value = source.value,
                    customer_id = source.customer_id,
                    customer_name = source.customer_name,
                    customer_cnpj = source.customer_cnpj,
                    metadata = source.metadata,
                    data_extracao = source.data_extracao
            WHEN NOT MATCHED THEN
                INSERT (id, document, issue_date, due_date, value, customer_id, customer_name, customer_cnpj, metadata, data_extracao)
                VALUES (source.id, source.document, source.issue_date, source.due_date, source.value, source.customer_id, source.customer_name, source.customer_cnpj, source.metadata, source.data_extracao);
        """;
        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            int idx = 1;
            setLongParameter(ps, idx++, e.getId());
            setStringParameter(ps, idx++, e.getDocument());
            setDateParameter(ps, idx++, e.getIssueDate());
            setDateParameter(ps, idx++, e.getDueDate());
            setBigDecimalParameter(ps, idx++, e.getValue());
            setLongParameter(ps, idx++, e.getCustomerId());
            setStringParameter(ps, idx++, e.getCustomerName());
            setStringParameter(ps, idx++, e.getCustomerCnpj());
            setStringParameter(ps, idx++, e.getMetadata());
            setInstantParameter(ps, idx++, Instant.now());
            return ps.executeUpdate();
        }
    }

    private void criarViewPowerBISeNaoExistir(final Connection conexao) throws SQLException {
        final String sqlView = """
            CREATE OR ALTER VIEW dbo.vw_faturas_graphql_powerbi AS
            SELECT
                id             AS [ID],
                document       AS [Fatura/N° Documento],
                issue_date     AS [Emissão],
                due_date       AS [Vencimento],
                value          AS [Valor],
                customer_id    AS [Cliente/ID],
                customer_name  AS [Cliente/Nome],
                customer_cnpj  AS [Cliente/CNPJ],
                metadata       AS [Metadata],
                data_extracao  AS [Data de extracao]
            FROM dbo.faturas_graphql
        """;
        executarDDL(conexao, sqlView);
    }

    public int enriquecerClientesViaJoins() throws SQLException {
        int total = 0;
        try (Connection conn = obterConexao();
             PreparedStatement ps1 = conn.prepareStatement("""
                UPDATE fg
                SET 
                    fg.customer_id   = COALESCE(fg.customer_id, f.pagador_id),
                    fg.customer_name = COALESCE(fg.customer_name, f.pagador_nome),
                    fg.customer_cnpj = COALESCE(fg.customer_cnpj, f.pagador_documento)
                FROM dbo.faturas_graphql fg
                INNER JOIN dbo.fretes f ON f.accounting_credit_id = fg.id
                WHERE fg.customer_name IS NULL OR fg.customer_cnpj IS NULL
            """);
             PreparedStatement ps2 = conn.prepareStatement("""
                UPDATE fg
                SET 
                    fg.customer_name = COALESCE(fg.customer_name, fpc.pagador_nome),
                    fg.customer_cnpj = COALESCE(fg.customer_cnpj, fpc.pagador_documento)
                FROM dbo.faturas_graphql fg
                INNER JOIN dbo.faturas_por_cliente fpc ON fpc.fit_ant_document = fg.document
                WHERE fg.customer_name IS NULL OR fg.customer_cnpj IS NULL
            """)) {
            total += ps1.executeUpdate();
            if (verificarTabelaExiste(conn, "faturas_por_cliente")) {
                total += ps2.executeUpdate();
            }
        }
        return total;
    }
}
