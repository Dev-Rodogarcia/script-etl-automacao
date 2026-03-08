package br.com.extrator.observabilidade.servicos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

final class IntegridadeEtlSqlSupport {

    boolean tabelaExiste(final Connection conexao, final String tabela) throws SQLException {
        final String sql = """
            SELECT COUNT(*)
            FROM INFORMATION_SCHEMA.TABLES
            WHERE TABLE_SCHEMA = 'dbo' AND TABLE_NAME = ?
            """;
        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setString(1, tabela);
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    boolean colunaExiste(final Connection conexao, final String tabela, final String coluna) throws SQLException {
        final String sql = """
            SELECT COUNT(*)
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = 'dbo' AND TABLE_NAME = ? AND COLUMN_NAME = ?
            """;
        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setString(1, tabela);
            stmt.setString(2, coluna);
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    int executarCount(final Connection conexao,
                      final String sql,
                      final LocalDateTime inicioExecucao,
                      final LocalDateTime fimExecucao) throws SQLException {
        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(inicioExecucao));
            stmt.setTimestamp(2, Timestamp.valueOf(fimExecucao));
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    List<Long> executarListaLong(final Connection conexao,
                                 final String sql,
                                 final LocalDateTime inicioExecucao,
                                 final LocalDateTime fimExecucao) throws SQLException {
        final List<Long> valores = new ArrayList<>();
        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(inicioExecucao));
            stmt.setTimestamp(2, Timestamp.valueOf(fimExecucao));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    final long valor = rs.getLong(1);
                    if (!rs.wasNull()) {
                        valores.add(valor);
                    }
                }
            }
        }
        return valores;
    }
}
