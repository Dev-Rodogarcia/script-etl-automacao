package br.com.extrator.persistencia.repositorio;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.extrator.aplicacao.expurgo.EntityReconciliationSpec;
import br.com.extrator.aplicacao.expurgo.OrphanReconciliationStore;
import br.com.extrator.suporte.banco.GerenciadorConexao;

public class OrphanReconciliationRepository implements OrphanReconciliationStore {
    private static final Logger logger = LoggerFactory.getLogger(OrphanReconciliationRepository.class);
    private static final int LOCK_TIMEOUT_MS = 30_000;

    @Override
    public Set<String> buscarChavesAtivas(final EntityReconciliationSpec spec,
                                          final LocalDate dataInicio,
                                          final LocalDate dataFim) throws SQLException {
        final String tableName = validarTabelaQualificada(spec.tableName());
        final String keyExpression = spec.dbKeyExpression();
        final String temporalExpression = spec.dbTemporalExpression();
        final String sql = """
            SELECT DISTINCT %1$s AS chave
              FROM %2$s AS base
             WHERE COALESCE(base.excluido_na_origem, 0) = 0
               AND %1$s IS NOT NULL
               AND %3$s >= ?
               AND %3$s < DATEADD(day, 1, ?)
             ORDER BY chave
            """.formatted(keyExpression, tableName, temporalExpression);

        final Set<String> keys = new LinkedHashSet<>();
        try (Connection connection = GerenciadorConexao.obterConexao();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDate(1, Date.valueOf(dataInicio));
            statement.setDate(2, Date.valueOf(dataFim));
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    final String key = rs.getString("chave");
                    if (key != null && !key.isBlank()) {
                        keys.add(key.trim());
                    }
                }
            }
        }

        logger.info(
            "Chaves ativas carregadas do banco | entidade={} | periodo={}..{} | total={}",
            spec.entityName(),
            dataInicio,
            dataFim,
            keys.size()
        );
        return keys;
    }

    @Override
    public int marcarOrfaos(final EntityReconciliationSpec spec,
                            final List<String> orphanKeys,
                            final int batchSize) throws SQLException {
        if (orphanKeys == null || orphanKeys.isEmpty()) {
            return 0;
        }

        final String tableName = validarTabelaQualificada(spec.tableName());
        final String keyExpression = spec.dbKeyExpression();
        final int tamanhoLote = Math.max(1, batchSize);
        int totalAtualizado = 0;

        try (Connection connection = GerenciadorConexao.obterConexao()) {
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            connection.setAutoCommit(false);
            configurarLockTimeout(connection);

            for (int inicio = 0; inicio < orphanKeys.size(); inicio += tamanhoLote) {
                final int fim = Math.min(orphanKeys.size(), inicio + tamanhoLote);
                final List<String> lote = new ArrayList<>(orphanKeys.subList(inicio, fim));
                try {
                    final int atualizados = executarUpdateLote(connection, tableName, keyExpression, lote);
                    connection.commit();
                    totalAtualizado += atualizados;
                    logger.info(
                        "Lote de expurgo logico confirmado | entidade={} | lote={}..{} | chaves={} | atualizados={}",
                        spec.entityName(),
                        inicio + 1,
                        fim,
                        lote.size(),
                        atualizados
                    );
                } catch (final SQLException e) {
                    rollbackSilencioso(connection);
                    throw e;
                }
            }
        }

        return totalAtualizado;
    }

    private int executarUpdateLote(final Connection connection,
                                   final String tableName,
                                   final String keyExpression,
                                   final List<String> lote) throws SQLException {
        final String placeholders = String.join(",", Collections.nCopies(lote.size(), "?"));
        final String sql = """
            UPDATE %1$s
               SET excluido_na_origem = 1,
                   data_exclusao_origem = GETDATE()
             WHERE excluido_na_origem = 0
               AND %2$s IN (%3$s)
            """.formatted(tableName, keyExpression, placeholders);
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            for (final String key : lote) {
                statement.setString(index++, key);
            }
            return statement.executeUpdate();
        }
    }

    private void configurarLockTimeout(final Connection connection) {
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET LOCK_TIMEOUT " + LOCK_TIMEOUT_MS);
        } catch (final SQLException e) {
            logger.warn("Nao foi possivel configurar LOCK_TIMEOUT no expurgo logico: {}", e.getMessage());
        }
    }

    private void rollbackSilencioso(final Connection connection) {
        try {
            connection.rollback();
        } catch (final SQLException rollbackEx) {
            logger.error("Falha ao executar rollback do lote de expurgo logico: {}", rollbackEx.getMessage(), rollbackEx);
        }
    }

    private String validarTabelaQualificada(final String tableName) throws SQLException {
        if (tableName == null || !tableName.matches("^dbo\\.[a-zA-Z0-9_]+$")) {
            throw new SQLException("Nome de tabela invalido para expurgo logico: " + tableName);
        }
        return tableName;
    }
}
