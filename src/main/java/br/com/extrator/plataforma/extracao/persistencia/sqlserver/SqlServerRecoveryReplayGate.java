package br.com.extrator.plataforma.extracao.persistencia.sqlserver;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Locale;

import br.com.extrator.aplicacao.extracao.RecoveryReplayGate;
import br.com.extrator.suporte.banco.GerenciadorConexao;
import br.com.extrator.suporte.configuracao.ConfigEtl;

public final class SqlServerRecoveryReplayGate implements RecoveryReplayGate {
    private static final String TABLE_NAME = "dbo.sys_replay_idempotency";

    @Override
    public StartResult tryStart(final ReplayAttempt attempt, final LocalDateTime now) throws Exception {
        if (attempt == null) {
            throw new IllegalArgumentException("attempt nao pode ser null");
        }

        try (Connection connection = GerenciadorConexao.obterConexao()) {
            final boolean autoCommitOriginal = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                ensureTableExists(connection);
                acquireLock(connection, attempt.idempotencyKey());

                final GateState existing = loadStateForUpdate(connection, attempt.idempotencyKey());
                if (existing != null && !isExpired(existing, now)) {
                    if ("COMPLETED".equals(existing.status())) {
                        connection.commit();
                        return StartResult.ALREADY_COMPLETED;
                    }
                    if ("STARTED".equals(existing.status())) {
                        connection.commit();
                        return StartResult.ALREADY_RUNNING;
                    }
                }

                upsertStarted(connection, attempt, now);
                connection.commit();
                return StartResult.STARTED;
            } catch (final SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                try {
                    releaseLock(connection, attempt.idempotencyKey());
                } finally {
                    connection.setAutoCommit(autoCommitOriginal);
                }
            }
        }
    }

    @Override
    public void markCompleted(final String idempotencyKey,
                              final String executionUuid,
                              final LocalDateTime finishedAt) throws Exception {
        updateFinalStatus(idempotencyKey, executionUuid, finishedAt, "COMPLETED", null);
    }

    @Override
    public void markFailed(final String idempotencyKey,
                           final String executionUuid,
                           final LocalDateTime finishedAt,
                           final String errorMessage) throws Exception {
        updateFinalStatus(idempotencyKey, executionUuid, finishedAt, "FAILED", errorMessage);
    }

    private void updateFinalStatus(final String idempotencyKey,
                                   final String executionUuid,
                                   final LocalDateTime finishedAt,
                                   final String status,
                                   final String errorMessage) throws Exception {
        try (Connection connection = GerenciadorConexao.obterConexao()) {
            ensureTableExists(connection);
            final String sql = """
                UPDATE dbo.sys_replay_idempotency
                   SET status = ?,
                       execution_uuid = ?,
                       finished_at = ?,
                       expires_at = ?,
                       last_error = ?,
                       updated_at = SYSDATETIME()
                 WHERE idempotency_key = ?
                   AND execution_uuid = ?
                   AND status = 'STARTED'
                """;
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, status);
                ps.setString(2, executionUuid);
                ps.setTimestamp(3, toTimestamp(finishedAt));
                ps.setTimestamp(4, toTimestamp(finishedAt.plus(ConfigEtl.obterRecoveryReplayIdempotencyTtl())));
                ps.setString(5, sanitizeError(errorMessage));
                ps.setString(6, idempotencyKey);
                ps.setString(7, executionUuid);
                final int rowsAffected = ps.executeUpdate();
                validarAtualizacaoStatusFinal(rowsAffected, idempotencyKey, executionUuid, status);
            }
        }
    }

    static void validarAtualizacaoStatusFinal(final int rowsAffected,
                                              final String idempotencyKey,
                                              final String executionUuid,
                                              final String statusPretendido) throws SQLException {
        if (rowsAffected == 1) {
            return;
        }
        throw new SQLException(
            "Gate de replay nao confirmou atualizacao final com ownership por execution_uuid. rows_affected="
                + rowsAffected
                + " | idempotency_key="
                + idempotencyKey
                + " | execution_uuid="
                + executionUuid
                + " | status_pretendido="
                + statusPretendido
        );
    }

    private void upsertStarted(final Connection connection,
                               final ReplayAttempt attempt,
                               final LocalDateTime now) throws SQLException {
        final LocalDateTime expiresAt = now.plus(ConfigEtl.obterRecoveryReplayIdempotencyTtl());
        final String sql = """
            MERGE dbo.sys_replay_idempotency AS target
            USING (VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?))
                AS source (
                    idempotency_key, api, entidade, data_inicio, data_fim, modo,
                    execution_uuid, started_at, expires_at
                )
            ON target.idempotency_key = source.idempotency_key
            WHEN MATCHED THEN
                UPDATE SET
                    api = source.api,
                    entidade = source.entidade,
                    data_inicio = source.data_inicio,
                    data_fim = source.data_fim,
                    modo = source.modo,
                    status = 'STARTED',
                    execution_uuid = source.execution_uuid,
                    started_at = source.started_at,
                    finished_at = NULL,
                    expires_at = source.expires_at,
                    last_error = NULL,
                    updated_at = SYSDATETIME()
            WHEN NOT MATCHED THEN
                INSERT (
                    idempotency_key, api, entidade, data_inicio, data_fim, modo,
                    status, execution_uuid, started_at, finished_at, expires_at, last_error
                )
                VALUES (
                    source.idempotency_key, source.api, source.entidade, source.data_inicio, source.data_fim, source.modo,
                    'STARTED', source.execution_uuid, source.started_at, NULL, source.expires_at, NULL
                );
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, attempt.idempotencyKey());
            ps.setString(2, attempt.api());
            ps.setString(3, attempt.entidade());
            ps.setObject(4, attempt.dataInicio());
            ps.setObject(5, attempt.dataFim());
            ps.setString(6, attempt.modo());
            ps.setString(7, attempt.executionUuid());
            ps.setTimestamp(8, toTimestamp(now));
            ps.setTimestamp(9, toTimestamp(expiresAt));
            ps.executeUpdate();
        }
    }

    private GateState loadStateForUpdate(final Connection connection, final String idempotencyKey) throws SQLException {
        final String sql = """
            SELECT status, expires_at
              FROM dbo.sys_replay_idempotency WITH (UPDLOCK, HOLDLOCK)
             WHERE idempotency_key = ?
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, idempotencyKey);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new GateState(
                    normalizarStatus(rs.getString("status")),
                    toLocalDateTime(rs.getTimestamp("expires_at"))
                );
            }
        }
    }

    private void ensureTableExists(final Connection connection) throws SQLException {
        final String sql = "SELECT CASE WHEN OBJECT_ID(?, 'U') IS NULL THEN 0 ELSE 1 END";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, TABLE_NAME);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                if (rs.getInt(1) == 0) {
                    throw new SQLException("Tabela " + TABLE_NAME + " ausente para gate de idempotencia de recovery.");
                }
            }
        }
    }

    private void acquireLock(final Connection connection, final String idempotencyKey) throws SQLException {
        final String sql = """
            DECLARE @resultado INT;
            EXEC @resultado = sp_getapplock
                @Resource = ?,
                @LockMode = 'Exclusive',
                @LockOwner = 'Session',
                @LockTimeout = ?;
            SELECT @resultado;
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, lockResource(idempotencyKey));
            ps.setInt(2, ConfigEtl.obterTimeoutLockReplayMs());
            try (ResultSet rs = ps.executeQuery()) {
                final int resultado = rs.next() ? rs.getInt(1) : -999;
                if (resultado < 0) {
                    throw new SQLException("Nao foi possivel adquirir lock de replay para " + idempotencyKey + ". Codigo=" + resultado);
                }
            }
        }
    }

    private void releaseLock(final Connection connection, final String idempotencyKey) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
            "EXEC sp_releaseapplock @Resource = ?, @LockOwner = 'Session'"
        )) {
            ps.setString(1, lockResource(idempotencyKey));
            ps.execute();
        }
    }

    private boolean isExpired(final GateState state, final LocalDateTime now) {
        return state.expiresAt() != null && state.expiresAt().isBefore(now);
    }

    private String lockResource(final String idempotencyKey) {
        return "replay-idempotency:" + idempotencyKey;
    }

    private String sanitizeError(final String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            return null;
        }
        final String sanitized = errorMessage.replaceAll("\\s+", " ").trim();
        return sanitized.length() > 1000 ? sanitized.substring(0, 1000) : sanitized;
    }

    private String normalizarStatus(final String status) {
        return status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
    }

    private Timestamp toTimestamp(final LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    private LocalDateTime toLocalDateTime(final Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }

    private record GateState(String status, LocalDateTime expiresAt) {
    }
}
