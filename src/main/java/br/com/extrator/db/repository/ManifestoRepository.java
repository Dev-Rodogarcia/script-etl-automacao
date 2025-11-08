package br.com.extrator.db.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;

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

                    -- Colunas Essenciais para Indexação e Relatórios conforme docs/descobertas-endpoints/manifestos.md
                    status NVARCHAR(50),
                    created_at DATETIMEOFFSET,
                    departured_at DATETIMEOFFSET,
                    closed_at DATETIMEOFFSET,
                    finished_at DATETIMEOFFSET,
                    mdfe_number INT,
                    mdfe_key NVARCHAR(100),
                    mdfe_status NVARCHAR(50),
                    distribution_pole NVARCHAR(255),
                    classification NVARCHAR(255),
                    vehicle_plate NVARCHAR(10),
                    vehicle_type NVARCHAR(255),
                    vehicle_owner NVARCHAR(255),
                    driver_name NVARCHAR(255),
                    branch_nickname NVARCHAR(255),
                    vehicle_departure_km INT,
                    closing_km INT,
                    traveled_km INT,
                    invoices_count INT,
                    invoices_volumes INT,
                    invoices_weight NVARCHAR(50),
                    total_taxed_weight NVARCHAR(50),
                    total_cubic_volume NVARCHAR(50),
                    invoices_value NVARCHAR(50),
                    manifest_freights_total NVARCHAR(50),
                    pick_sequence_code BIGINT,
                    contract_number NVARCHAR(50),
                    daily_subtotal NVARCHAR(50),
                    total_cost DECIMAL(18, 2),
                    operational_expenses_total NVARCHAR(50),
                    inss_value NVARCHAR(50),
                    sest_senat_value NVARCHAR(50),
                    ir_value NVARCHAR(50),
                    paying_total NVARCHAR(50),
                    creation_user_name NVARCHAR(255),
                    adjustment_user_name NVARCHAR(255),

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
     * Versão melhorada com validações robustas, logging detalhado e truncamento de strings.
     */
    @Override
    protected int executarMerge(final Connection conexao, final ManifestoEntity manifesto) throws SQLException {
        // ✅ VALIDAÇÃO INICIAL
        if (manifesto == null) {
            logger.error("❌ Tentativa de salvar ManifestoEntity NULL");
            throw new SQLException("Não é possível executar MERGE para Manifesto nulo");
        }
        
        // Para Manifestos, o 'sequence_code' é a chave de negócio primária.
        if (manifesto.getSequenceCode() == null) {
            logger.error("❌ Manifesto com sequence_code NULL");
            throw new SQLException("Não é possível executar o MERGE para Manifesto sem um 'sequence_code'.");
        }
        
        logger.debug("→ Salvando manifesto sequence_code={}", manifesto.getSequenceCode());

        final String sql = String.format("""
            MERGE %s AS target
            USING (VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?))
                AS source (sequence_code, status, created_at, departured_at, closed_at, finished_at, mdfe_number, mdfe_key, mdfe_status, distribution_pole, classification, vehicle_plate, vehicle_type, vehicle_owner, driver_name, branch_nickname, vehicle_departure_km, closing_km, traveled_km, invoices_count, invoices_volumes, invoices_weight, total_taxed_weight, total_cubic_volume, invoices_value, manifest_freights_total, pick_sequence_code, contract_number, daily_subtotal, total_cost, operational_expenses_total, inss_value, sest_senat_value, ir_value, paying_total, creation_user_name, adjustment_user_name, metadata, data_extracao)
            ON target.sequence_code = source.sequence_code
            WHEN MATCHED THEN
                UPDATE SET
                    status = source.status,
                    created_at = source.created_at,
                    departured_at = source.departured_at,
                    closed_at = source.closed_at,
                    finished_at = source.finished_at,
                    mdfe_number = source.mdfe_number,
                    mdfe_key = source.mdfe_key,
                    mdfe_status = source.mdfe_status,
                    distribution_pole = source.distribution_pole,
                    classification = source.classification,
                    vehicle_plate = source.vehicle_plate,
                    vehicle_type = source.vehicle_type,
                    vehicle_owner = source.vehicle_owner,
                    driver_name = source.driver_name,
                    branch_nickname = source.branch_nickname,
                    vehicle_departure_km = source.vehicle_departure_km,
                    closing_km = source.closing_km,
                    traveled_km = source.traveled_km,
                    invoices_count = source.invoices_count,
                    invoices_volumes = source.invoices_volumes,
                    invoices_weight = source.invoices_weight,
                    total_taxed_weight = source.total_taxed_weight,
                    total_cubic_volume = source.total_cubic_volume,
                    invoices_value = source.invoices_value,
                    manifest_freights_total = source.manifest_freights_total,
                    pick_sequence_code = source.pick_sequence_code,
                    contract_number = source.contract_number,
                    daily_subtotal = source.daily_subtotal,
                    total_cost = source.total_cost,
                    operational_expenses_total = source.operational_expenses_total,
                    inss_value = source.inss_value,
                    sest_senat_value = source.sest_senat_value,
                    ir_value = source.ir_value,
                    paying_total = source.paying_total,
                    creation_user_name = source.creation_user_name,
                    adjustment_user_name = source.adjustment_user_name,
                    metadata = source.metadata,
                    data_extracao = source.data_extracao
            WHEN NOT MATCHED THEN
                INSERT (sequence_code, status, created_at, departured_at, closed_at, finished_at, mdfe_number, mdfe_key, mdfe_status, distribution_pole, classification, vehicle_plate, vehicle_type, vehicle_owner, driver_name, branch_nickname, vehicle_departure_km, closing_km, traveled_km, invoices_count, invoices_volumes, invoices_weight, total_taxed_weight, total_cubic_volume, invoices_value, manifest_freights_total, pick_sequence_code, contract_number, daily_subtotal, total_cost, operational_expenses_total, inss_value, sest_senat_value, ir_value, paying_total, creation_user_name, adjustment_user_name, metadata, data_extracao)
                VALUES (source.sequence_code, source.status, source.created_at, source.departured_at, source.closed_at, source.finished_at, source.mdfe_number, source.mdfe_key, source.mdfe_status, source.distribution_pole, source.classification, source.vehicle_plate, source.vehicle_type, source.vehicle_owner, source.driver_name, source.branch_nickname, source.vehicle_departure_km, source.closing_km, source.traveled_km, source.invoices_count, source.invoices_volumes, source.invoices_weight, source.total_taxed_weight, source.total_cubic_volume, source.invoices_value, source.manifest_freights_total, source.pick_sequence_code, source.contract_number, source.daily_subtotal, source.total_cost, source.operational_expenses_total, source.inss_value, source.sest_senat_value, source.ir_value, source.paying_total, source.creation_user_name, source.adjustment_user_name, source.metadata, source.data_extracao);
            """, NOME_TABELA);

        try (PreparedStatement statement = conexao.prepareStatement(sql)) {
            // Define os parâmetros de forma segura e na ordem correta conforme MERGE SQL
            int paramIndex = 1;
            statement.setObject(paramIndex++, manifesto.getSequenceCode(), Types.BIGINT);
            statement.setString(paramIndex++, truncate(manifesto.getStatus(), 50, "status"));
            // Usar helper methods para tipos especiais (DATETIMEOFFSET)
            if (manifesto.getCreatedAt() != null) {
                statement.setObject(paramIndex++, manifesto.getCreatedAt(), Types.TIMESTAMP_WITH_TIMEZONE);
            } else {
                statement.setNull(paramIndex++, Types.TIMESTAMP_WITH_TIMEZONE);
            }
            if (manifesto.getDeparturedAt() != null) {
                statement.setObject(paramIndex++, manifesto.getDeparturedAt(), Types.TIMESTAMP_WITH_TIMEZONE);
            } else {
                statement.setNull(paramIndex++, Types.TIMESTAMP_WITH_TIMEZONE);
            }
            if (manifesto.getClosedAt() != null) {
                statement.setObject(paramIndex++, manifesto.getClosedAt(), Types.TIMESTAMP_WITH_TIMEZONE);
            } else {
                statement.setNull(paramIndex++, Types.TIMESTAMP_WITH_TIMEZONE);
            }
            if (manifesto.getFinishedAt() != null) {
                statement.setObject(paramIndex++, manifesto.getFinishedAt(), Types.TIMESTAMP_WITH_TIMEZONE);
            } else {
                statement.setNull(paramIndex++, Types.TIMESTAMP_WITH_TIMEZONE);
            }
            statement.setObject(paramIndex++, manifesto.getMdfeNumber(), Types.INTEGER);
            statement.setString(paramIndex++, truncate(manifesto.getMdfeKey(), 100, "mdfe_key"));
            statement.setString(paramIndex++, truncate(manifesto.getMdfeStatus(), 50, "mdfe_status"));
            statement.setString(paramIndex++, truncate(manifesto.getDistributionPole(), 255, "distribution_pole"));
            statement.setString(paramIndex++, truncate(manifesto.getClassification(), 255, "classification"));
            statement.setString(paramIndex++, truncate(manifesto.getVehiclePlate(), 10, "vehicle_plate"));
            statement.setString(paramIndex++, truncate(manifesto.getVehicleType(), 255, "vehicle_type"));
            statement.setString(paramIndex++, truncate(manifesto.getVehicleOwner(), 255, "vehicle_owner"));
            statement.setString(paramIndex++, truncate(manifesto.getDriverName(), 255, "driver_name"));
            statement.setString(paramIndex++, truncate(manifesto.getBranchNickname(), 255, "branch_nickname"));
            statement.setObject(paramIndex++, manifesto.getVehicleDepartureKm(), Types.INTEGER);
            statement.setObject(paramIndex++, manifesto.getClosingKm(), Types.INTEGER);
            statement.setObject(paramIndex++, manifesto.getTraveledKm(), Types.INTEGER);
            statement.setObject(paramIndex++, manifesto.getInvoicesCount(), Types.INTEGER);
            statement.setObject(paramIndex++, manifesto.getInvoicesVolumes(), Types.INTEGER);
            statement.setString(paramIndex++, manifesto.getInvoicesWeight());
            statement.setString(paramIndex++, manifesto.getTotalTaxedWeight());
            statement.setString(paramIndex++, manifesto.getTotalCubicVolume());
            statement.setString(paramIndex++, manifesto.getInvoicesValue());
            statement.setString(paramIndex++, manifesto.getManifestFreightsTotal());
            statement.setObject(paramIndex++, manifesto.getPickSequenceCode(), Types.BIGINT);
            statement.setString(paramIndex++, manifesto.getContractNumber());
            statement.setString(paramIndex++, manifesto.getDailySubtotal());
            setBigDecimalParameter(statement, paramIndex++, manifesto.getTotalCost());
            statement.setString(paramIndex++, manifesto.getOperationalExpensesTotal());
            statement.setString(paramIndex++, manifesto.getInssValue());
            statement.setString(paramIndex++, manifesto.getSestSenatValue());
            statement.setString(paramIndex++, manifesto.getIrValue());
            statement.setString(paramIndex++, manifesto.getPayingTotal());
            statement.setString(paramIndex++, truncate(manifesto.getCreationUserName(), 255, "creation_user_name"));
            statement.setString(paramIndex++, truncate(manifesto.getAdjustmentUserName(), 255, "adjustment_user_name"));
            statement.setString(paramIndex++, manifesto.getMetadata()); // JSON - sem limite, mas pode ser grande
            setInstantParameter(statement, paramIndex++, Instant.now()); // UTC timestamp
            
            // ✅ VALIDAR número de parâmetros
            final int expectedParams = 39;
            if (paramIndex != expectedParams + 1) { // +1 porque paramIndex é 1-based
                throw new SQLException(String.format(
                    "ERRO DE PROGRAMAÇÃO: SQL espera %d parâmetros, mas apenas %d foram setados!",
                    expectedParams, paramIndex - 1));
            }

            final int rowsAffected = statement.executeUpdate();
            
            // ✅ VERIFICAR rows affected
            if (rowsAffected == 0) {
                logger.error("❌ MERGE retornou 0 linhas para manifesto sequence_code={}. " +
                           "Possível violação de constraint ou dados inválidos.", 
                           manifesto.getSequenceCode());
                // Não lançar exceção aqui - deixar o AbstractRepository tratar
                return 0;
            }
            
            if (rowsAffected > 0) {
                logger.debug("✅ Manifesto {} salvo com sucesso: {} linha(s) afetada(s)", 
                            manifesto.getSequenceCode(), rowsAffected);
            }
            
            return rowsAffected;
            
        } catch (SQLException e) {
            logger.error("❌ SQLException ao salvar manifesto sequence_code={}: {} - SQLState: {} - ErrorCode: {}", 
                        manifesto.getSequenceCode(), e.getMessage(), e.getSQLState(), e.getErrorCode(), e);
            
            // Log stacktrace completo para constraint violations (SQLState 23xxx)
            if (e.getSQLState() != null && e.getSQLState().startsWith("23")) {
                logger.error("Constraint violation detectada - stacktrace completo:", e);
            }
            
            // Re-lançar exceção para que o AbstractRepository possa tratar
            throw e;
        }
    }
    
    /**
     * Trunca uma string para o tamanho máximo especificado.
     * Loga warning quando há truncamento para facilitar debug.
     * 
     * @param value Valor a ser truncado
     * @param maxLength Tamanho máximo permitido
     * @param fieldName Nome do campo (para log)
     * @return String truncada ou original se menor que maxLength
     */
    private String truncate(String value, int maxLength, String fieldName) {
        if (value != null && value.length() > maxLength) {
            logger.warn("⚠️ Truncando campo {} de {} para {} chars (sequence_code pode estar próximo): '{}'...", 
                       fieldName, value.length(), maxLength, 
                       value.substring(0, Math.min(50, value.length())));
            return value.substring(0, maxLength);
        }
        return value;
    }
}