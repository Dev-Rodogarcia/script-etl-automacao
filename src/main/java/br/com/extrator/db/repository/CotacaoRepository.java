package br.com.extrator.db.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.extrator.db.entity.CotacaoEntity;

/**
 * Repositório para operações de persistência da entidade CotacaoEntity.
 * Implementa a arquitetura de persistência híbrida: colunas-chave para indexação
 * e uma coluna de metadados para resiliência e completude dos dados.
 * Utiliza operações MERGE (UPSERT) com a chave de negócio (sequenceCode).
 */
public class CotacaoRepository extends AbstractRepository<CotacaoEntity> {
    private static final Logger logger = LoggerFactory.getLogger(CotacaoRepository.class);
    private static final String NOME_TABELA = "cotacoes";

    @Override
    protected String getNomeTabela() {
        return NOME_TABELA;
    }

    /**
     * Cria a tabela 'cotacoes' se ela não existir, seguindo o modelo híbrido.
     * A estrutura contém apenas colunas essenciais para busca e uma coluna NVARCHAR(MAX)
     * para armazenar o JSON completo, garantindo resiliência.
     */
    @Override
    protected void criarTabelaSeNaoExistir(final Connection conexao) throws SQLException {
        if (!verificarTabelaExiste(conexao, NOME_TABELA)) {
            logger.info("Criando tabela {} com arquitetura híbrida...", NOME_TABELA);

            final String sql = """
                CREATE TABLE cotacoes (
                    -- Coluna de Chave Primária (Chave de Negócio)
                    sequence_code BIGINT PRIMARY KEY,

                    -- Colunas Essenciais para Indexação e Relatórios
                    requested_at DATETIMEOFFSET,
                    total_value DECIMAL(18, 2),
                    taxed_weight DECIMAL(18, 3),
                    invoices_value DECIMAL(18, 2),
                    origin_city NVARCHAR(100),
                    origin_state NVARCHAR(2),
                    destination_city NVARCHAR(100),
                    destination_state NVARCHAR(2),
                    customer_doc NVARCHAR(14),

                    -- Coluna de Metadados para Resiliência e Completude
                    metadata NVARCHAR(MAX),

                    -- Coluna de Auditoria
                    data_extracao DATETIME2 DEFAULT GETDATE()
                )
                """;

            executarDDL(conexao, sql);
            logger.info("Tabela {} criada com sucesso.", NOME_TABELA);
        }
    }

    /**
     * Executa a operação MERGE (UPSERT) para inserir ou atualizar uma cotação no banco.
     * A lógica é segura e baseada na nova arquitetura de Entidade.
     */
    @Override
    protected int executarMerge(final Connection conexao, final CotacaoEntity cotacao) throws SQLException {
        // Para Cotações, o 'sequence_code' é a chave de negócio primária.
        if (cotacao.getSequenceCode() == null) {
            throw new SQLException("Não é possível executar o MERGE para Cotação sem um 'sequence_code'.");
        }

        final String sql = String.format("""
            MERGE %s AS target
            USING (VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?))
                AS source (sequence_code, requested_at, total_value, taxed_weight, invoices_value, origin_city, origin_state, destination_city, destination_state, customer_doc, metadata, data_extracao)
            ON target.sequence_code = source.sequence_code
            WHEN MATCHED THEN
                UPDATE SET
                    requested_at = source.requested_at,
                    total_value = source.total_value,
                    taxed_weight = source.taxed_weight,
                    invoices_value = source.invoices_value,
                    origin_city = source.origin_city,
                    origin_state = source.origin_state,
                    destination_city = source.destination_city,
                    destination_state = source.destination_state,
                    customer_doc = source.customer_doc,
                    metadata = source.metadata,
                    data_extracao = source.data_extracao
            WHEN NOT MATCHED THEN
                INSERT (sequence_code, requested_at, total_value, taxed_weight, invoices_value, origin_city, origin_state, destination_city, destination_state, customer_doc, metadata, data_extracao)
                VALUES (source.sequence_code, source.requested_at, source.total_value, source.taxed_weight, source.invoices_value, source.origin_city, source.origin_state, source.destination_city, source.destination_state, source.customer_doc, source.metadata, source.data_extracao);
            """, NOME_TABELA);

        try (PreparedStatement statement = conexao.prepareStatement(sql)) {
            // Define os parâmetros de forma segura e na ordem correta.
            int paramIndex = 1;
            statement.setObject(paramIndex++, cotacao.getSequenceCode(), Types.BIGINT);
            statement.setObject(paramIndex++, cotacao.getRequestedAt(), Types.TIMESTAMP_WITH_TIMEZONE);
            statement.setBigDecimal(paramIndex++, cotacao.getTotalValue());
            statement.setBigDecimal(paramIndex++, cotacao.getTaxedWeight());
            statement.setBigDecimal(paramIndex++, cotacao.getInvoicesValue());
            statement.setString(paramIndex++, cotacao.getOriginCity());
            statement.setString(paramIndex++, cotacao.getOriginState());
            statement.setString(paramIndex++, cotacao.getDestinationCity());
            statement.setString(paramIndex++, cotacao.getDestinationState());
            statement.setString(paramIndex++, cotacao.getCustomerDoc());
            statement.setString(paramIndex++, cotacao.getMetadata());
            statement.setTimestamp(paramIndex++, Timestamp.from(Instant.now())); // UTC timestamp

            final int rowsAffected = statement.executeUpdate();
            logger.debug("MERGE executado para Cotação sequence_code {}: {} linha(s) afetada(s)", cotacao.getSequenceCode(), rowsAffected);
            return rowsAffected;
        }
    }
}