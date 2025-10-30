package br.com.extrator.db.repository;

import br.com.extrator.db.entity.OcorrenciaEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;

/**
 * Repositório para operações de persistência da entidade OcorrenciaEntity.
 * Implementa a arquitetura de persistência híbrida: colunas-chave para indexação
 * e uma coluna de metadados para resiliência e completude dos dados.
 * Utiliza operações MERGE (UPSERT) com a chave primária (id) da ocorrência.
 */
public class OcorrenciaRepository extends AbstractRepository<OcorrenciaEntity> {
    private static final Logger logger = LoggerFactory.getLogger(OcorrenciaRepository.class);
    private static final String NOME_TABELA = "ocorrencias";

    @Override
    protected String getNomeTabela() {
        return NOME_TABELA;
    }

    /**
     * Cria a tabela 'ocorrencias' se ela não existir, seguindo o modelo híbrido.
     * A estrutura "achata" o JSON da API, promovendo os campos mais importantes
     * para colunas dedicadas e armazenando o objeto completo em uma coluna de metadados.
     */
    @Override
    protected void criarTabelaSeNaoExistir(final Connection conexao) throws SQLException {
        if (!verificarTabelaExiste(conexao, NOME_TABELA)) {
            logger.info("Criando tabela {} com arquitetura híbrida...", NOME_TABELA);

            final String sql = """
                CREATE TABLE ocorrencias (
                    -- Chave Primária
                    id BIGINT PRIMARY KEY,

                    -- Colunas Essenciais "Promovidas" para Indexação e Relatórios
                    occurrence_at DATETIMEOFFSET,
                    occurrence_code INT,
                    occurrence_description NVARCHAR(255),
                    freight_id BIGINT,
                    cte_key NVARCHAR(44),
                    invoice_id BIGINT,
                    invoice_key NVARCHAR(44),

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
     * Executa a operação MERGE (UPSERT) para inserir ou atualizar uma ocorrência no banco.
     * A lógica é segura e baseada na nova arquitetura de Entidade.
     */
    @Override
    protected int executarMerge(final Connection conexao, final OcorrenciaEntity ocorrencia) throws SQLException {
        // Para ocorrências, o 'id' é a única chave confiável para o MERGE.
        // Se não houver ID, a operação não pode continuar.
        if (ocorrencia.getId() == null) {
            throw new SQLException("Não é possível executar o MERGE para Ocorrência sem um ID.");
        }

        final String sql = String.format("""
            MERGE %s AS target
            USING (VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?))
                AS source (id, occurrence_at, occurrence_code, occurrence_description, freight_id, cte_key, invoice_id, invoice_key, metadata, data_extracao)
            ON target.id = source.id
            WHEN MATCHED THEN
                UPDATE SET
                    occurrence_at = source.occurrence_at,
                    occurrence_code = source.occurrence_code,
                    occurrence_description = source.occurrence_description,
                    freight_id = source.freight_id,
                    cte_key = source.cte_key,
                    invoice_id = source.invoice_id,
                    invoice_key = source.invoice_key,
                    metadata = source.metadata,
                    data_extracao = source.data_extracao
            WHEN NOT MATCHED THEN
                INSERT (id, occurrence_at, occurrence_code, occurrence_description, freight_id, cte_key, invoice_id, invoice_key, metadata, data_extracao)
                VALUES (source.id, source.occurrence_at, source.occurrence_code, source.occurrence_description, source.freight_id, source.cte_key, source.invoice_id, source.invoice_key, source.metadata, source.data_extracao);
            """, NOME_TABELA);

        try (PreparedStatement statement = conexao.prepareStatement(sql)) {
            // Define os parâmetros de forma segura e na ordem correta.
            int paramIndex = 1;
            statement.setObject(paramIndex++, ocorrencia.getId(), Types.BIGINT);
            statement.setObject(paramIndex++, ocorrencia.getOccurrenceAt(), Types.TIMESTAMP_WITH_TIMEZONE);
            statement.setObject(paramIndex++, ocorrencia.getOccurrenceCode(), Types.INTEGER);
            statement.setString(paramIndex++, ocorrencia.getOccurrenceDescription());
            statement.setObject(paramIndex++, ocorrencia.getFreightId(), Types.BIGINT);
            statement.setString(paramIndex++, ocorrencia.getCteKey());
            statement.setObject(paramIndex++, ocorrencia.getInvoiceId(), Types.BIGINT);
            statement.setString(paramIndex++, ocorrencia.getInvoiceKey());
            statement.setString(paramIndex++, ocorrencia.getMetadata());
            statement.setTimestamp(paramIndex++, Timestamp.from(Instant.now())); // UTC timestamp

            final int rowsAffected = statement.executeUpdate();
            logger.debug("MERGE executado para Ocorrência ID {}: {} linha(s) afetada(s)", ocorrencia.getId(), rowsAffected);
            return rowsAffected;
        }
    }
}