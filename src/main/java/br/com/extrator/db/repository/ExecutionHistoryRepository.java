package br.com.extrator.db.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.extrator.util.banco.GerenciadorConexao;

/**
 * Repository for sys_execution_history.
 */
public class ExecutionHistoryRepository {

    private static final Logger logger = LoggerFactory.getLogger(ExecutionHistoryRepository.class);

    /**
     * Inserts an execution history record.
     */
    public void inserirHistorico(final LocalDateTime inicio,
                                 final LocalDateTime fim,
                                 final int durationSeconds,
                                 final String status,
                                 final int totalRecords,
                                 final String errorCategory,
                                 final String errorMessage) {
        final String sql = """
            INSERT INTO dbo.sys_execution_history
            (start_time, end_time, duration_seconds, status, total_records, error_category, error_message)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = GerenciadorConexao.obterConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, Timestamp.valueOf(inicio));
            stmt.setTimestamp(2, Timestamp.valueOf(fim));
            stmt.setInt(3, durationSeconds);
            stmt.setString(4, limitar(status, 20));
            stmt.setInt(5, totalRecords);
            stmt.setString(6, limitar(errorCategory, 50));
            stmt.setString(7, limitar(errorMessage, 500));

            stmt.executeUpdate();
        } catch (final SQLException e) {
            logger.error("Falha ao inserir sys_execution_history: {}", e.getMessage(), e);
            throw new RuntimeException("Falha ao inserir sys_execution_history", e);
        }
    }

    /**
     * Calculates the total records processed using log_extracoes within the execution window.
     */
    public int calcularTotalRegistros(final LocalDateTime inicio, final LocalDateTime fim) {
        final String sql = """
            SELECT COALESCE(SUM(registros_extraidos), 0) AS total
            FROM dbo.log_extracoes
            WHERE timestamp_fim >= ? AND timestamp_inicio <= ?
            """;

        try (Connection conn = GerenciadorConexao.obterConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, Timestamp.valueOf(inicio));
            stmt.setTimestamp(2, Timestamp.valueOf(fim));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
        } catch (final SQLException e) {
            logger.warn("Falha ao calcular total de registros: {}", e.getMessage());
        }

        return 0;
    }

    private String limitar(final String valor, final int max) {
        if (valor == null) {
            return null;
        }
        String texto = valor.trim();
        if (texto.length() > max) {
            texto = texto.substring(0, max);
        }
        return texto;
    }
}
