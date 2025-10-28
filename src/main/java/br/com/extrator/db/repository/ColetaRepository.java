package br.com.extrator.db.repository;

import br.com.extrator.db.entity.ColetaEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Repositório para operações de persistência da entidade ColetaEntity.
 * Implementa a arquitetura de persistência híbrida: colunas-chave para indexação
 * e uma coluna de metadados para resiliência e completude dos dados.
 * Utiliza operações MERGE (UPSERT) com a chave primária (id) da coleta.
 */
public class ColetaRepository extends AbstractRepository<ColetaEntity> {
    private static final Logger logger = LoggerFactory.getLogger(ColetaRepository.class);
    private static final String NOME_TABELA = "coletas";

    @Override
    protected String getNomeTabela() {
        return NOME_TABELA;
    }

    /**
     * Cria a tabela 'coletas' se ela não existir, seguindo o modelo híbrido.
     * A estrutura contém apenas colunas essenciais para busca e uma coluna NVARCHAR(MAX)
     * para armazenar o JSON completo, garantindo resiliência.
     */
    @Override
    protected void criarTabelaSeNaoExistir(final Connection conexao) throws SQLException {
        if (!verificarTabelaExiste(conexao, NOME_TABELA)) {
            logger.info("Criando tabela {} com arquitetura híbrida...", NOME_TABELA);

            final String sql = """
                CREATE TABLE coletas (
                    -- Coluna de Chave Primária (String, conforme API GraphQL)
                    id NVARCHAR(50) PRIMARY KEY,

                    -- Colunas Essenciais para Indexação e Relatórios
                    sequence_code BIGINT,
                    request_date DATE,
                    service_date DATE,
                    status NVARCHAR(50),
                    total_value DECIMAL(18, 2),
                    total_weight DECIMAL(18, 3),
                    total_volumes INT,

                    -- Coluna de Metadados para Resiliência e Completude
                    metadata NVARCHAR(MAX),

                    -- Coluna de Auditoria
                    data_extracao DATETIME2 DEFAULT GETDATE(),
                    -- Constraint para chave de negócio
                    UNIQUE (sequence_code)
                )
                """;

            executarDDL(conexao, sql);
            logger.info("Tabela {} criada com sucesso.", NOME_TABELA);
        }
    }

    /**
     * Executa a operação MERGE (UPSERT) para inserir ou atualizar uma coleta no banco.
     * A lógica é segura e baseada na nova arquitetura de Entidade.
     */
    @Override
    protected int executarMerge(final Connection conexao, final ColetaEntity coleta) throws SQLException {
        // Para Coletas, o 'id' (string) é a única chave confiável para o MERGE.
        if (coleta.getId() == null || coleta.getId().trim().isEmpty()) {
            throw new SQLException("Não é possível executar o MERGE para Coleta sem um ID.");
        }

        final String sql = String.format("""
            MERGE %s AS target
            USING (VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?))
                AS source (id, sequence_code, request_date, service_date, status, total_value, total_weight, total_volumes, metadata)
            ON target.id = source.id
            WHEN MATCHED THEN
                UPDATE SET
                    sequence_code = source.sequence_code,
                    request_date = source.request_date,
                    service_date = source.service_date,
                    status = source.status,
                    total_value = source.total_value,
                    total_weight = source.total_weight,
                    total_volumes = source.total_volumes,
                    metadata = source.metadata,
                    data_extracao = GETDATE()
            WHEN NOT MATCHED THEN
                INSERT (id, sequence_code, request_date, service_date, status, total_value, total_weight, total_volumes, metadata)
                VALUES (source.id, source.sequence_code, source.request_date, source.service_date, source.status, source.total_value, source.total_weight, source.total_volumes, source.metadata);
            """, NOME_TABELA);

        try (PreparedStatement statement = conexao.prepareStatement(sql)) {
            // Define os parâmetros de forma segura e na ordem correta.
            int paramIndex = 1;
            statement.setString(paramIndex++, coleta.getId());
            statement.setObject(paramIndex++, coleta.getSequenceCode(), Types.BIGINT);
            statement.setObject(paramIndex++, coleta.getRequestDate(), Types.DATE);
            statement.setObject(paramIndex++, coleta.getServiceDate(), Types.DATE);
            statement.setString(paramIndex++, coleta.getStatus());
            statement.setBigDecimal(paramIndex++, coleta.getTotalValue());
            statement.setBigDecimal(paramIndex++, coleta.getTotalWeight());
            statement.setObject(paramIndex++, coleta.getTotalVolumes(), Types.INTEGER);
            statement.setString(paramIndex++, coleta.getMetadata());

            final int rowsAffected = statement.executeUpdate();
            logger.debug("MERGE executado para Coleta ID {}: {} linha(s) afetada(s)", coleta.getId(), rowsAffected);
            return rowsAffected;
        }
    }
}