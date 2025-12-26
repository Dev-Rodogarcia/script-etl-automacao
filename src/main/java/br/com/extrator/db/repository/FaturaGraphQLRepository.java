package br.com.extrator.db.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;

import br.com.extrator.api.constantes.ConstantesViewsPowerBI;
import br.com.extrator.db.entity.FaturaGraphQLEntity;
import br.com.extrator.util.validacao.ConstantesEntidades;

public class FaturaGraphQLRepository extends AbstractRepository<FaturaGraphQLEntity> {
    private static final String NOME_TABELA = ConstantesEntidades.FATURAS_GRAPHQL;
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
                    original_due_date DATE,
                    value DECIMAL(18,2),
                    paid_value DECIMAL(18,2),
                    value_to_pay DECIMAL(18,2),
                    discount_value DECIMAL(18,2),
                    interest_value DECIMAL(18,2),
                    paid BIT,
                    status NVARCHAR(50),
                    type NVARCHAR(50),
                    comments NVARCHAR(MAX),
                    sequence_code INT,
                    competence_month INT,
                    competence_year INT,
                    created_at DATETIME2,
                    updated_at DATETIME2,
                    corporation_id BIGINT,
                    corporation_name NVARCHAR(255),
                    corporation_cnpj NVARCHAR(50),
                    metadata NVARCHAR(MAX),
                    data_extracao DATETIME2 DEFAULT GETDATE()
                );
                CREATE INDEX IX_fg_document ON dbo.faturas_graphql(document);
                CREATE INDEX IX_fg_due_date ON dbo.faturas_graphql(due_date);
                CREATE INDEX IX_fg_corporation_id ON dbo.faturas_graphql(corporation_id);
            END
        """;
        executarDDL(conexao, sql);
        
        // Adicionar novas colunas se a tabela já existir
        adicionarColunasNovasSeNecessario(conexao);
        
        if (!viewVerificada) {
            criarViewPowerBISeNaoExistir(conexao);
            viewVerificada = true;
        }
    }

    @Override
    protected int executarMerge(final Connection conexao, final FaturaGraphQLEntity e) throws SQLException {
        final String sql = """
            MERGE dbo.faturas_graphql AS target
            USING (VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)) AS source
                  (id, document, issue_date, due_date, original_due_date, value, paid_value, value_to_pay, discount_value, interest_value, paid, status, type, comments, sequence_code, competence_month, competence_year, created_at, updated_at, corporation_id, corporation_name, corporation_cnpj, metadata, data_extracao)
            ON target.id = source.id
            WHEN MATCHED THEN
                UPDATE SET
                    document = source.document,
                    issue_date = source.issue_date,
                    due_date = source.due_date,
                    original_due_date = source.original_due_date,
                    value = source.value,
                    paid_value = source.paid_value,
                    value_to_pay = source.value_to_pay,
                    discount_value = source.discount_value,
                    interest_value = source.interest_value,
                    paid = source.paid,
                    status = source.status,
                    type = source.type,
                    comments = source.comments,
                    sequence_code = source.sequence_code,
                    competence_month = source.competence_month,
                    competence_year = source.competence_year,
                    created_at = source.created_at,
                    updated_at = source.updated_at,
                    corporation_id = source.corporation_id,
                    corporation_name = source.corporation_name,
                    corporation_cnpj = source.corporation_cnpj,
                    metadata = source.metadata,
                    data_extracao = source.data_extracao
            WHEN NOT MATCHED THEN
                INSERT (id, document, issue_date, due_date, original_due_date, value, paid_value, value_to_pay, discount_value, interest_value, paid, status, type, comments, sequence_code, competence_month, competence_year, created_at, updated_at, corporation_id, corporation_name, corporation_cnpj, metadata, data_extracao)
                VALUES (source.id, source.document, source.issue_date, source.due_date, source.original_due_date, source.value, source.paid_value, source.value_to_pay, source.discount_value, source.interest_value, source.paid, source.status, source.type, source.comments, source.sequence_code, source.competence_month, source.competence_year, source.created_at, source.updated_at, source.corporation_id, source.corporation_name, source.corporation_cnpj, source.metadata, source.data_extracao);
        """;
        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            int idx = 1;
            setLongParameter(ps, idx++, e.getId());
            setStringParameter(ps, idx++, e.getDocument());
            setDateParameter(ps, idx++, e.getIssueDate());
            setDateParameter(ps, idx++, e.getDueDate());
            setDateParameter(ps, idx++, e.getOriginalDueDate());
            setBigDecimalParameter(ps, idx++, e.getValue());
            setBigDecimalParameter(ps, idx++, e.getPaidValue());
            setBigDecimalParameter(ps, idx++, e.getValueToPay());
            setBigDecimalParameter(ps, idx++, e.getDiscountValue());
            setBigDecimalParameter(ps, idx++, e.getInterestValue());
            setBooleanParameter(ps, idx++, e.getPaid());
            setStringParameter(ps, idx++, e.getStatus());
            setStringParameter(ps, idx++, e.getType());
            setStringParameter(ps, idx++, e.getComments());
            setIntegerParameter(ps, idx++, e.getSequenceCode());
            setIntegerParameter(ps, idx++, e.getCompetenceMonth());
            setIntegerParameter(ps, idx++, e.getCompetenceYear());
            setOffsetDateTimeParameter(ps, idx++, e.getCreatedAt());
            setOffsetDateTimeParameter(ps, idx++, e.getUpdatedAt());
            setLongParameter(ps, idx++, e.getCorporationId());
            setStringParameter(ps, idx++, e.getCorporationName());
            setStringParameter(ps, idx++, e.getCorporationCnpj());
            setStringParameter(ps, idx++, e.getMetadata());
            setInstantParameter(ps, idx++, Instant.now());
            return ps.executeUpdate();
        }
    }

    private void criarViewPowerBISeNaoExistir(final Connection conexao) throws SQLException {
        executarDDL(conexao, ConstantesViewsPowerBI.obterSqlView(NOME_TABELA));
    }

    private void adicionarColunaSeNaoExistir(final Connection conexao, final String nomeColuna, final String tipoDef) throws SQLException {
        final String verifica = "SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='dbo' AND TABLE_NAME='faturas_graphql' AND COLUMN_NAME='" + nomeColuna + "'";
        try (PreparedStatement stmt = conexao.prepareStatement(verifica); java.sql.ResultSet rs = stmt.executeQuery()) {
            if (!rs.next()) {
                final String alterar = "ALTER TABLE dbo.faturas_graphql ADD " + nomeColuna + " " + tipoDef;
                executarDDL(conexao, alterar);
            }
        }
    }

    private void adicionarColunasNovasSeNecessario(final Connection conexao) throws SQLException {
        adicionarColunaSeNaoExistir(conexao, "original_due_date", "DATE");
        adicionarColunaSeNaoExistir(conexao, "paid_value", "DECIMAL(18,2)");
        adicionarColunaSeNaoExistir(conexao, "value_to_pay", "DECIMAL(18,2)");
        adicionarColunaSeNaoExistir(conexao, "discount_value", "DECIMAL(18,2)");
        adicionarColunaSeNaoExistir(conexao, "interest_value", "DECIMAL(18,2)");
        adicionarColunaSeNaoExistir(conexao, "paid", "BIT");
        adicionarColunaSeNaoExistir(conexao, "status", "NVARCHAR(50)");
        adicionarColunaSeNaoExistir(conexao, "type", "NVARCHAR(50)");
        adicionarColunaSeNaoExistir(conexao, "comments", "NVARCHAR(MAX)");
        adicionarColunaSeNaoExistir(conexao, "sequence_code", "INT");
        adicionarColunaSeNaoExistir(conexao, "competence_month", "INT");
        adicionarColunaSeNaoExistir(conexao, "competence_year", "INT");
        adicionarColunaSeNaoExistir(conexao, "created_at", "DATETIME2");
        adicionarColunaSeNaoExistir(conexao, "updated_at", "DATETIME2");
        adicionarColunaSeNaoExistir(conexao, "corporation_id", "BIGINT");
        adicionarColunaSeNaoExistir(conexao, "corporation_name", "NVARCHAR(255)");
        adicionarColunaSeNaoExistir(conexao, "corporation_cnpj", "NVARCHAR(50)");
    }

}
