package br.com.extrator.db.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.extrator.db.entity.ManifestoEntity;

/**
 * Repositório para operações de persistência da entidade ManifestoEntity.
 * Implementa a arquitetura de persistência híbrida: colunas-chave para indexação
 * e uma coluna de metadados para resiliência e completude dos dados.
 * Utiliza operações MERGE (UPSERT) com a chave de negócio (sequenceCode).
 */
public class ManifestoRepository extends AbstractRepository<ManifestoEntity> {
    private static final Logger logger = LoggerFactory.getLogger(ManifestoRepository.class);
    private static final String NOME_TABELA = "manifestos";

    @Override
    protected String getNomeTabela() {
        return NOME_TABELA;
    }

    /**
     * Cria a tabela 'manifestos' se ela não existir, seguindo o modelo híbrido.
     * A estrutura contém apenas colunas essenciais para busca e uma coluna NVARCHAR(MAX)
     * para armazenar o JSON completo, garantindo resiliência.
     */
    @Override
    protected void criarTabelaSeNaoExistir(final Connection conexao) throws SQLException {
        if (!verificarTabelaExiste(conexao, NOME_TABELA)) {
            logger.info("Criando tabela {} com arquitetura híbrida...", NOME_TABELA);

            final String sql = """
                CREATE TABLE manifestos (
                    -- Coluna de Chave Primária (Chave de Negócio)
                    sequence_code BIGINT PRIMARY KEY,

                    -- Colunas Essenciais para Indexação e Relatórios
                    status NVARCHAR(50),
                    created_at DATETIMEOFFSET,
                    departured_at DATETIMEOFFSET,
                    finished_at DATETIMEOFFSET,
                    total_cost DECIMAL(18, 2),
                    traveled_km INT,
                    vehicle_plate NVARCHAR(10),
                    driver_name NVARCHAR(255),
                    origin_branch NVARCHAR(100),
                    mdfe_status NVARCHAR(50),

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
     * Executa a operação MERGE (UPSERT) para inserir ou atualizar um manifesto no banco.
     * A lógica é segura e baseada na nova arquitetura de Entidade.
     */
    @Override
    protected int executarMerge(final Connection conexao, final ManifestoEntity manifesto) throws SQLException {
        // Para Manifestos, o 'sequence_code' é a chave de negócio primária.
        if (manifesto.getSequenceCode() == null) {
            throw new SQLException("Não é possível executar o MERGE para Manifesto sem um 'sequence_code'.");
        }

        final String sql = String.format("""
            MERGE %s AS target
            USING (VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?))
                AS source (sequence_code, status, created_at, departured_at, finished_at, total_cost, traveled_km, vehicle_plate, driver_name, origin_branch, mdfe_status, metadata)
            ON target.sequence_code = source.sequence_code
            WHEN MATCHED THEN
                UPDATE SET
                    status = source.status,
                    created_at = source.created_at,
                    departured_at = source.departured_at,
                    finished_at = source.finished_at,
                    total_cost = source.total_cost,
                    traveled_km = source.traveled_km,
                    vehicle_plate = source.vehicle_plate,
                    driver_name = source.driver_name,
                    origin_branch = source.origin_branch,
                    mdfe_status = source.mdfe_status,
                    metadata = source.metadata,
                    data_extracao = GETDATE()
            WHEN NOT MATCHED THEN
                INSERT (sequence_code, status, created_at, departured_at, finished_at, total_cost, traveled_km, vehicle_plate, driver_name, origin_branch, mdfe_status, metadata)
                VALUES (source.sequence_code, source.status, source.created_at, source.departured_at, source.finished_at, source.total_cost, source.traveled_km, source.vehicle_plate, source.driver_name, source.origin_branch, source.mdfe_status, source.metadata);
            """, NOME_TABELA);

        try (PreparedStatement statement = conexao.prepareStatement(sql)) {
            // Define os parâmetros de forma segura e na ordem correta.
            int paramIndex = 1;
            statement.setObject(paramIndex++, manifesto.getSequenceCode(), Types.BIGINT);
            statement.setString(paramIndex++, manifesto.getStatus());
            statement.setObject(paramIndex++, manifesto.getCreatedAt(), Types.TIMESTAMP_WITH_TIMEZONE);
            statement.setObject(paramIndex++, manifesto.getDeparturedAt(), Types.TIMESTAMP_WITH_TIMEZONE);
            statement.setObject(paramIndex++, manifesto.getFinishedAt(), Types.TIMESTAMP_WITH_TIMEZONE);
            statement.setBigDecimal(paramIndex++, manifesto.getTotalCost());
            statement.setObject(paramIndex++, manifesto.getTraveledKm(), Types.INTEGER);
            statement.setString(paramIndex++, manifesto.getVehiclePlate());
            statement.setString(paramIndex++, manifesto.getDriverName());
            statement.setString(paramIndex++, manifesto.getOriginBranch());
            statement.setString(paramIndex++, manifesto.getMdfeStatus());
            statement.setString(paramIndex++, manifesto.getMetadata());

            final int rowsAffected = statement.executeUpdate();
            logger.debug("MERGE executado para Manifesto sequence_code {}: {} linha(s) afetada(s)", manifesto.getSequenceCode(), rowsAffected);
            return rowsAffected;
        }
    }
}