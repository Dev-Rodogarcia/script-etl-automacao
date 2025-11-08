package br.com.extrator.db.repository;

import br.com.extrator.db.entity.ColetaEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;

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
                CREATE TABLE dbo.coletas (
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

                    -- Campos Expandidos (22 campos do CSV)
                    cliente_id BIGINT,
                    cliente_nome NVARCHAR(255),
                    local_coleta NVARCHAR(500),
                    cidade_coleta NVARCHAR(255),
                    uf_coleta NVARCHAR(10),
                    usuario_id BIGINT,
                    usuario_nome NVARCHAR(255),
                    request_hour NVARCHAR(20),
                    service_start_hour NVARCHAR(20),
                    finish_date DATE,
                    service_end_hour NVARCHAR(20),
                    requester NVARCHAR(255),
                    taxed_weight DECIMAL(18, 3),
                    comments NVARCHAR(MAX),
                    agent_id BIGINT,
                    manifest_item_pick_id BIGINT,
                    vehicle_type_id BIGINT,

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
        } else {
            // Ajuste defensivo de schema: garantir que colunas de horas comportem valores como "1999-12-31" ou "HH:mm:ss"
            // Nota: A validação no ColetaMapper já trata datas como null ou extrai HH:mm[:ss],
            // mas este ajuste garante que o schema suporte eventuais variações da API
            try {
                executarDDL(conexao, "ALTER TABLE dbo.coletas ALTER COLUMN request_hour NVARCHAR(20) NULL");
                logger.debug("Coluna request_hour ajustada para NVARCHAR(20)");
            } catch (final SQLException e) {
                logger.warn("⚠️ Não foi possível ajustar coluna request_hour: {} (pode já estar no tamanho correto)", e.getMessage());
            }
            try {
                executarDDL(conexao, "ALTER TABLE dbo.coletas ALTER COLUMN service_start_hour NVARCHAR(20) NULL");
                logger.debug("Coluna service_start_hour ajustada para NVARCHAR(20)");
            } catch (final SQLException e) {
                logger.warn("⚠️ Não foi possível ajustar coluna service_start_hour: {} (pode já estar no tamanho correto)", e.getMessage());
            }
            try {
                executarDDL(conexao, "ALTER TABLE dbo.coletas ALTER COLUMN service_end_hour NVARCHAR(20) NULL");
                logger.debug("Coluna service_end_hour ajustada para NVARCHAR(20)");
            } catch (final SQLException e) {
                logger.warn("⚠️ Não foi possível ajustar coluna service_end_hour: {} (pode já estar no tamanho correto)", e.getMessage());
            }
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
            MERGE dbo.%s AS target
            USING (VALUES (
                ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
            )) AS source (
                id, sequence_code, request_date, service_date, status, total_value, total_weight, total_volumes,
                cliente_id, cliente_nome, local_coleta, cidade_coleta, uf_coleta, usuario_id, usuario_nome,
                request_hour, service_start_hour, finish_date, service_end_hour, requester, taxed_weight,
                comments, agent_id, manifest_item_pick_id, vehicle_type_id, metadata, data_extracao
            )
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
                    cliente_id = source.cliente_id,
                    cliente_nome = source.cliente_nome,
                    local_coleta = source.local_coleta,
                    cidade_coleta = source.cidade_coleta,
                    uf_coleta = source.uf_coleta,
                    usuario_id = source.usuario_id,
                    usuario_nome = source.usuario_nome,
                    request_hour = source.request_hour,
                    service_start_hour = source.service_start_hour,
                    finish_date = source.finish_date,
                    service_end_hour = source.service_end_hour,
                    requester = source.requester,
                    taxed_weight = source.taxed_weight,
                    comments = source.comments,
                    agent_id = source.agent_id,
                    manifest_item_pick_id = source.manifest_item_pick_id,
                    vehicle_type_id = source.vehicle_type_id,
                    metadata = source.metadata,
                    data_extracao = source.data_extracao
            WHEN NOT MATCHED THEN
                INSERT (
                    id, sequence_code, request_date, service_date, status, total_value, total_weight, total_volumes,
                    cliente_id, cliente_nome, local_coleta, cidade_coleta, uf_coleta, usuario_id, usuario_nome,
                    request_hour, service_start_hour, finish_date, service_end_hour, requester, taxed_weight,
                    comments, agent_id, manifest_item_pick_id, vehicle_type_id, metadata, data_extracao
                )
                VALUES (
                    source.id, source.sequence_code, source.request_date, source.service_date, source.status, source.total_value, source.total_weight, source.total_volumes,
                    source.cliente_id, source.cliente_nome, source.local_coleta, source.cidade_coleta, source.uf_coleta, source.usuario_id, source.usuario_nome,
                    source.request_hour, source.service_start_hour, source.finish_date, source.service_end_hour, source.requester, source.taxed_weight,
                    source.comments, source.agent_id, source.manifest_item_pick_id, source.vehicle_type_id, source.metadata, source.data_extracao
                );
            """, NOME_TABELA);

        logger.debug("Preparando MERGE de Coleta ID {}", coleta.getId());
        PreparedStatement statement;
        try {
            statement = conexao.prepareStatement(sql);
        } catch (final SQLException e) {
            logger.error("Falha ao preparar MERGE de Coleta ID {}: {}", coleta.getId(), e.getMessage());
            throw e;
        }
        try (statement) {
            int expectedCount;
            try {
                final int metaCount = statement.getParameterMetaData().getParameterCount();
                expectedCount = (metaCount > 0 ? metaCount : 27);
                logger.debug("MERGE de Coletas preparado: {} parâmetro(s) esperado(s)", expectedCount);
            } catch (final SQLException pmEx) {
                // Alguns drivers podem não suportar ParameterMetaData completamente
                logger.debug("Não foi possível obter ParameterMetaData: {}", pmEx.getMessage());
                // Fallback para 27 parâmetros conhecidos
                expectedCount = 27;
            }
            // Define os parâmetros de forma segura e na ordem correta.
            int paramIndex = 1;
            statement.setString(paramIndex++, coleta.getId());
            statement.setObject(paramIndex++, coleta.getSequenceCode(), Types.BIGINT);
            setDateParameter(statement, paramIndex++, coleta.getRequestDate());
            setDateParameter(statement, paramIndex++, coleta.getServiceDate());
            statement.setString(paramIndex++, coleta.getStatus());
            setBigDecimalParameter(statement, paramIndex++, coleta.getTotalValue());
            setBigDecimalParameter(statement, paramIndex++, coleta.getTotalWeight());
            statement.setObject(paramIndex++, coleta.getTotalVolumes(), Types.INTEGER);
            // Campos expandidos (22 campos do CSV)
            statement.setObject(paramIndex++, coleta.getClienteId(), Types.BIGINT);
            statement.setString(paramIndex++, coleta.getClienteNome());
            statement.setString(paramIndex++, coleta.getLocalColeta());
            statement.setString(paramIndex++, coleta.getCidadeColeta());
            statement.setString(paramIndex++, coleta.getUfColeta());
            statement.setObject(paramIndex++, coleta.getUsuarioId(), Types.BIGINT);
            statement.setString(paramIndex++, coleta.getUsuarioNome());
            statement.setString(paramIndex++, coleta.getRequestHour());
            statement.setString(paramIndex++, coleta.getServiceStartHour());
            setDateParameter(statement, paramIndex++, coleta.getFinishDate());
            statement.setString(paramIndex++, coleta.getServiceEndHour());
            statement.setString(paramIndex++, coleta.getRequester());
            setBigDecimalParameter(statement, paramIndex++, coleta.getTaxedWeight());
            statement.setString(paramIndex++, coleta.getComments());
            statement.setObject(paramIndex++, coleta.getAgentId(), Types.BIGINT);
            statement.setObject(paramIndex++, coleta.getManifestItemPickId(), Types.BIGINT);
            statement.setObject(paramIndex++, coleta.getVehicleTypeId(), Types.BIGINT);
            statement.setString(paramIndex++, coleta.getMetadata());
            setInstantParameter(statement, paramIndex++, Instant.now()); // UTC timestamp
            
            // Verificar se todos os parâmetros foram definidos
            if ((paramIndex - 1) != expectedCount) {
                throw new SQLException(String.format("Número incorreto de parâmetros: esperado %d, definido %d", expectedCount, (paramIndex - 1)));
            }

            final int rowsAffected = statement.executeUpdate();
            logger.debug("MERGE executado para Coleta ID {}: {} linha(s) afetada(s)", coleta.getId(), rowsAffected);
            return rowsAffected;
        }
    }
}