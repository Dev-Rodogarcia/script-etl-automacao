package br.com.extrator.aplicacao.validacao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import br.com.extrator.suporte.console.LoggerConsole;

final class ManifestosValidationQueries {
    private final LoggerConsole log;

    ManifestosValidationQueries(final LoggerConsole log) {
        this.log = log;
    }

    int contar(final Connection conn, final String sql) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt("total") : 0;
        }
    }

    int contarDesdeUltimaExtracaoComFallback(final Connection conn) {
        try {
            return contar(
                conn,
                "SELECT COUNT(*) as total FROM manifestos "
                    + "WHERE data_extracao >= CAST((SELECT TOP 1 timestamp_fim FROM log_extracoes "
                    + "WHERE entidade = 'manifestos' ORDER BY timestamp_fim DESC) AS DATETIME2)"
            );
        } catch (final SQLException e) {
            log.warn("Erro ao contar registros desde ultima extracao: {}", e.getMessage());
            return -1;
        }
    }

    boolean existeColunaIdentificadorUnico(final Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 """
                    SELECT COUNT(*) as existe
                    FROM INFORMATION_SCHEMA.COLUMNS
                    WHERE TABLE_NAME = 'manifestos' AND COLUMN_NAME = 'identificador_unico'""")) {
            return rs.next() && rs.getInt("existe") > 0;
        }
    }

    int contarDuplicadosChaveComposta(final Connection conn) throws SQLException {
        int total = 0;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 """
                    SELECT sequence_code, identificador_unico, COUNT(*) as quantidade
                    FROM manifestos
                    GROUP BY sequence_code, identificador_unico
                    HAVING COUNT(*) > 1""")) {
            while (rs.next()) {
                total++;
            }
        }
        return total;
    }

    int[] contarPickSequenceCode(final Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 """
                    SELECT
                      SUM(CASE WHEN pick_sequence_code IS NULL THEN 1 ELSE 0 END) as com_null,
                      SUM(CASE WHEN pick_sequence_code IS NOT NULL THEN 1 ELSE 0 END) as com_valor
                    FROM manifestos""")) {
            if (rs.next()) {
                return new int[] { rs.getInt("com_null"), rs.getInt("com_valor") };
            }
        }
        return new int[] { 0, 0 };
    }

    int contarDuplicadosUltimas24h(final Connection conn) throws SQLException {
        int total = 0;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 """
                    SELECT sequence_code, identificador_unico
                    FROM manifestos
                    WHERE data_extracao >= DATEADD(HOUR, -24, GETDATE())
                    GROUP BY sequence_code, identificador_unico
                    HAVING COUNT(*) > 1""")) {
            while (rs.next()) {
                total++;
            }
        }
        return total;
    }
}
