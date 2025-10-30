package br.com.extrator.db.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.extrator.db.entity.LocalizacaoCargaEntity;

/**
 * Repositório para operações de persistência da entidade LocalizacaoCargaEntity.
 * Implementa a arquitetura de persistência híbrida: colunas-chave para indexação
 * e uma coluna de metadados para resiliência e completude dos dados.
 * Utiliza operações MERGE (UPSERT) com a chave de negócio (sequenceNumber).
 */
public class LocalizacaoCargaRepository extends AbstractRepository<LocalizacaoCargaEntity> {
    private static final Logger logger = LoggerFactory.getLogger(LocalizacaoCargaRepository.class);
    private static final String NOME_TABELA = "localizacao_cargas";

    @Override
    protected String getNomeTabela() {
        return NOME_TABELA;
    }

    /**
     * Cria a tabela 'localizacao_cargas' se ela não existir, seguindo o modelo híbrido.
     * A estrutura contém apenas colunas essenciais para busca e uma coluna NVARCHAR(MAX)
     * para armazenar o JSON completo, garantindo resiliência.
     */
    @Override
    protected void criarTabelaSeNaoExistir(final Connection conexao) throws SQLException {
        if (!verificarTabelaExiste(conexao, NOME_TABELA)) {
            logger.info("Criando tabela {} com arquitetura híbrida...", NOME_TABELA);

            final String sql = """
                CREATE TABLE localizacao_cargas (
                    -- Coluna de Chave Primária (Chave de Negócio)
                    sequence_number BIGINT PRIMARY KEY,

                    -- Colunas Essenciais para Indexação e Relatórios
                    service_at DATETIMEOFFSET,
                    status NVARCHAR(50),
                    total_value DECIMAL(18, 2),
                    predicted_delivery_at DATETIMEOFFSET,
                    origin_location_name NVARCHAR(255),
                    destination_location_name NVARCHAR(255),

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
     * Executa a operação MERGE (UPSERT) para inserir ou atualizar um registro de localização no banco.
     * A lógica é segura e baseada na nova arquitetura de Entidade.
     */
    @Override
    protected int executarMerge(final Connection conexao, final LocalizacaoCargaEntity carga) throws SQLException {
        // Para Localização de Cargas, o 'sequence_number' é a chave de negócio primária.
        if (carga.getSequenceNumber() == null) {
            throw new SQLException("Não é possível executar o MERGE para Localização de Carga sem um 'sequence_number'.");
        }

        final String sql = String.format("""
            MERGE %s AS target
            USING (VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?))
                AS source (sequence_number, service_at, status, total_value, predicted_delivery_at, origin_location_name, destination_location_name, metadata, data_extracao)
            ON target.sequence_number = source.sequence_number
            WHEN MATCHED THEN
                UPDATE SET
                    service_at = source.service_at,
                    status = source.status,
                    total_value = source.total_value,
                    predicted_delivery_at = source.predicted_delivery_at,
                    origin_location_name = source.origin_location_name,
                    destination_location_name = source.destination_location_name,
                    metadata = source.metadata,
                    data_extracao = source.data_extracao
            WHEN NOT MATCHED THEN
                INSERT (sequence_number, service_at, status, total_value, predicted_delivery_at, origin_location_name, destination_location_name, metadata, data_extracao)
                VALUES (source.sequence_number, source.service_at, source.status, source.total_value, source.predicted_delivery_at, source.origin_location_name, source.destination_location_name, source.metadata, source.data_extracao);
            """, NOME_TABELA);

        try (PreparedStatement statement = conexao.prepareStatement(sql)) {
            // Define os parâmetros de forma segura e na ordem correta.
            int paramIndex = 1;
            statement.setObject(paramIndex++, carga.getSequenceNumber(), Types.BIGINT);
            statement.setObject(paramIndex++, carga.getServiceAt(), Types.TIMESTAMP_WITH_TIMEZONE);
            statement.setString(paramIndex++, carga.getStatus());
            statement.setBigDecimal(paramIndex++, carga.getTotalValue());
            statement.setObject(paramIndex++, carga.getPredictedDeliveryAt(), Types.TIMESTAMP_WITH_TIMEZONE);
            statement.setString(paramIndex++, carga.getOriginLocationName());
            statement.setString(paramIndex++, carga.getDestinationLocationName());
            statement.setString(paramIndex++, carga.getMetadata());
            statement.setTimestamp(paramIndex++, Timestamp.from(Instant.now())); // UTC timestamp

            final int rowsAffected = statement.executeUpdate();
            logger.debug("MERGE executado para Localização de Carga sequence_number {}: {} linha(s) afetada(s)", carga.getSequenceNumber(), rowsAffected);
            return rowsAffected;
        }
    }
}