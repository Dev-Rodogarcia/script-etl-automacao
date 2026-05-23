/* ==[DOC-FILE]===============================================================
Arquivo : src/main/java/br/com/extrator/db/repository/LocalizacaoCargaRepository.java
Classe  : LocalizacaoCargaRepository (class)
Pacote  : br.com.extrator.persistencia.repositorio
Modulo  : Repositorio de dados
Papel   : Implementa responsabilidade de localizacao carga repository.

Conecta com:
- LocalizacaoCargaEntity (db.entity)
- ConstantesEntidades (util.validacao)

Fluxo geral:
1) Monta comandos SQL e parametros.
2) Executa operacoes de persistencia/consulta no banco.
3) Converte resultado para entidades de dominio.

Estrutura interna:
Metodos principais:
- getNomeTabela(): expone valor atual do estado interno.
Atributos-chave:
- logger: logger da classe para diagnostico.
- NOME_TABELA: campo de estado para "nome tabela".
[DOC-FILE-END]============================================================== */

package br.com.extrator.persistencia.repositorio;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.extrator.persistencia.entidade.LocalizacaoCargaEntity;
import br.com.extrator.suporte.validacao.ConstantesEntidades;

/**
 * Repositório para operações de persistência da entidade LocalizacaoCargaEntity.
 * Implementa a arquitetura de persistência híbrida: colunas-chave para indexação
 * e uma coluna de metadados para resiliência e completude dos dados.
 * Utiliza operações MERGE (UPSERT) com a chave de negócio (sequenceNumber).
 */
public class LocalizacaoCargaRepository extends AbstractRepository<LocalizacaoCargaEntity> {
    private static final Logger logger = LoggerFactory.getLogger(LocalizacaoCargaRepository.class);
    private static final String NOME_TABELA = ConstantesEntidades.LOCALIZACAO_CARGAS;
    private static final String NOME_TABELA_STAGING = "#stg_localizacao_cargas";

    @Override
    protected String getNomeTabela() {
        return NOME_TABELA;
    }

    @Override
    protected boolean aceitarMergeSemAlteracoesComoSucesso(final LocalizacaoCargaEntity carga) {
        return true;
    }

    @Override
    protected boolean usarStagingPorExecucao() {
        return true;
    }

    @Override
    protected void prepararStagingPorExecucao(final Connection conexao) throws SQLException {
        recriarTabelaTemporariaPorExecucao(conexao, NOME_TABELA_STAGING);
        criarIndicesStaging(conexao);
    }

    @Override
    protected int executarMergeNoDestinoDaExecucao(final Connection conexao,
                                                   final LocalizacaoCargaEntity carga) throws SQLException {
        return executarMergeEmTabela(conexao, carga, validarNomeTabelaTemporaria(NOME_TABELA_STAGING));
    }

    @Override
    protected int promoverStagingPorExecucao(final Connection conexao) throws SQLException {
        final String sql = construirSqlMerge(
            qualificarTabelaDestino(),
            NOME_TABELA_STAGING + " AS source",
            hashOperacionalDiferente()
        );
        try (PreparedStatement statement = conexao.prepareStatement(sql)) {
            return statement.executeUpdate();
        }
    }

    /**
     * Executa a operação MERGE (UPSERT) para inserir ou atualizar um registro de localização no banco.
     * A lógica é segura e baseada na nova arquitetura de Entidade.
     */
    @Override
    protected int executarMerge(final Connection conexao, final LocalizacaoCargaEntity carga) throws SQLException {
        return executarMergeEmTabela(conexao, carga, qualificarTabelaDestino());
    }

    private int executarMergeEmTabela(final Connection conexao,
                                      final LocalizacaoCargaEntity carga,
                                      final String tabelaAlvo) throws SQLException {
        // Para Localização de Cargas, o 'sequence_number' é a chave de negócio primária.
        if (carga.getSequenceNumber() == null) {
            throw new SQLException("Não é possível executar o MERGE para Localização de Carga sem um 'sequence_number'.");
        }

        final String sql = construirSqlMerge(tabelaAlvo, construirSourceClauseValues(), hashOperacionalDiferente());

        try (PreparedStatement statement = conexao.prepareStatement(sql)) {
            // Define os parâmetros de forma segura e na ordem correta conforme MERGE SQL
            int paramIndex = 1;
            setLongParameter(statement, paramIndex++, carga.getSequenceNumber());
            setStringParameter(statement, paramIndex++, carga.getType());
            // Usar helper methods para tipos especiais
            if (carga.getServiceAt() != null) {
                statement.setObject(paramIndex++, carga.getServiceAt(), Types.TIMESTAMP_WITH_TIMEZONE);
            } else {
                statement.setNull(paramIndex++, Types.TIMESTAMP_WITH_TIMEZONE);
            }
            setIntegerParameter(statement, paramIndex++, carga.getInvoicesVolumes());
            setStringParameter(statement, paramIndex++, carga.getTaxedWeight());
            setBigDecimalParameter(statement, paramIndex++, carga.getTaxedWeightDecimal());
            setStringParameter(statement, paramIndex++, carga.getInvoicesValue());
            setBigDecimalParameter(statement, paramIndex++, carga.getInvoicesValueDecimal());
            setBigDecimalParameter(statement, paramIndex++, carga.getTotalValue());
            setStringParameter(statement, paramIndex++, carga.getServiceType());
            setStringParameter(statement, paramIndex++, carga.getBranchNickname());
            if (carga.getPredictedDeliveryAt() != null) {
                statement.setObject(paramIndex++, carga.getPredictedDeliveryAt(), Types.TIMESTAMP_WITH_TIMEZONE);
            } else {
                statement.setNull(paramIndex++, Types.TIMESTAMP_WITH_TIMEZONE);
            }
            setStringParameter(statement, paramIndex++, carga.getDestinationLocationName());
            setStringParameter(statement, paramIndex++, carga.getDestinationBranchNickname());
            setStringParameter(statement, paramIndex++, carga.getClassification());
            setStringParameter(statement, paramIndex++, carga.getStatus());
            setStringParameter(statement, paramIndex++, carga.getStatusNormalized());
            setStringParameter(statement, paramIndex++, carga.getStatusBranchNickname());
            setStringParameter(statement, paramIndex++, carga.getOriginLocationName());
            setStringParameter(statement, paramIndex++, carga.getOriginBranchNickname());
            setStringParameter(statement, paramIndex++, carga.getFitFlnClnNickname());
            setStringParameter(statement, paramIndex++, carga.getMetadata());
            setStringParameter(statement, paramIndex++, carga.getLocalizacaoHash());
            setInstantParameter(statement, paramIndex++, Instant.now()); // UTC timestamp
            
            if (paramIndex != 25) {
                throw new SQLException(String.format("Número incorreto de parâmetros: esperado 24, definido %d", paramIndex - 1));
            }

            final int rowsAffected = statement.executeUpdate();
            logger.debug("MERGE executado para Localização de Carga sequence_number {}: {} linha(s) afetada(s)", carga.getSequenceNumber(), rowsAffected);
            return rowsAffected;
        }
    }

    private String construirSqlMerge(final String tabelaAlvo,
                                     final String sourceClause,
                                     final String freshnessGuard) {
        return """
            MERGE %s WITH (HOLDLOCK) AS target
            USING %s
            ON target.sequence_number = source.sequence_number
            WHEN MATCHED AND %s THEN
                UPDATE SET
                    type = source.type,
                    service_at = source.service_at,
                    invoices_volumes = source.invoices_volumes,
                    taxed_weight = source.taxed_weight,
                    taxed_weight_decimal = source.taxed_weight_decimal,
                    invoices_value = source.invoices_value,
                    invoices_value_decimal = source.invoices_value_decimal,
                    total_value = source.total_value,
                    service_type = source.service_type,
                    branch_nickname = source.branch_nickname,
                    predicted_delivery_at = source.predicted_delivery_at,
                    destination_location_name = source.destination_location_name,
                    destination_branch_nickname = source.destination_branch_nickname,
                    classification = source.classification,
                    status = source.status,
                    status_normalized = source.status_normalized,
                    status_branch_nickname = source.status_branch_nickname,
                    origin_location_name = source.origin_location_name,
                    origin_branch_nickname = source.origin_branch_nickname,
                    fit_fln_cln_nickname = source.fit_fln_cln_nickname,
                    metadata = source.metadata,
                    localizacao_hash = source.localizacao_hash,
                    data_extracao = source.data_extracao
            WHEN NOT MATCHED THEN
                INSERT (sequence_number, type, service_at, invoices_volumes, taxed_weight, taxed_weight_decimal, invoices_value, invoices_value_decimal, total_value, service_type, branch_nickname, predicted_delivery_at, destination_location_name, destination_branch_nickname, classification, status, status_normalized, status_branch_nickname, origin_location_name, origin_branch_nickname, fit_fln_cln_nickname, metadata, localizacao_hash, data_extracao)
                VALUES (source.sequence_number, source.type, source.service_at, source.invoices_volumes, source.taxed_weight, source.taxed_weight_decimal, source.invoices_value, source.invoices_value_decimal, source.total_value, source.service_type, source.branch_nickname, source.predicted_delivery_at, source.destination_location_name, source.destination_branch_nickname, source.classification, source.status, source.status_normalized, source.status_branch_nickname, source.origin_location_name, source.origin_branch_nickname, source.fit_fln_cln_nickname, source.metadata, source.localizacao_hash, source.data_extracao);
            """.formatted(tabelaAlvo, sourceClause, freshnessGuard);
    }

    private String construirSourceClauseValues() {
        return """
            (VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?))
                AS source (sequence_number, type, service_at, invoices_volumes, taxed_weight, taxed_weight_decimal, invoices_value, invoices_value_decimal, total_value, service_type, branch_nickname, predicted_delivery_at, destination_location_name, destination_branch_nickname, classification, status, status_normalized, status_branch_nickname, origin_location_name, origin_branch_nickname, fit_fln_cln_nickname, metadata, localizacao_hash, data_extracao)
            """;
    }

    private String hashOperacionalDiferente() {
        return """
            (
                target.localizacao_hash IS NULL
                OR source.localizacao_hash IS NULL
                OR target.localizacao_hash <> source.localizacao_hash
            )
            """;
    }

    private void criarIndicesStaging(final Connection conexao) throws SQLException {
        try (Statement statement = conexao.createStatement()) {
            statement.execute("""
                CREATE UNIQUE CLUSTERED INDEX CX_stg_localizacao_sequence
                ON #stg_localizacao_cargas(sequence_number)
                """);
            statement.execute("""
                CREATE NONCLUSTERED INDEX IX_stg_localizacao_hash
                ON #stg_localizacao_cargas(sequence_number, localizacao_hash)
                INCLUDE (status, status_normalized, status_branch_nickname, fit_fln_cln_nickname, destination_branch_nickname, predicted_delivery_at, service_at)
                """);
        }
    }
}
