package br.com.extrator.aplicacao.validacao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import br.com.extrator.suporte.console.LoggerConsole;

final class ValidacaoSqlBatchExecutor {
    private final LoggerConsole log;
    private final ValidacaoSqlResultPrinter resultPrinter;

    ValidacaoSqlBatchExecutor(final LoggerConsole log) {
        this.log = log;
        this.resultPrinter = new ValidacaoSqlResultPrinter(log);
    }

    void executar(final Connection conn, final String sql, final String scriptName) throws SQLException {
        String sqlProcessado = sql.replaceAll("(?i)^\\s*PRINT\\s+.*$", "");
        final String[] batches = sqlProcessado.split("(?i)^\\s*GO\\s*$", -1);

        for (final String batch : batches) {
            final String batchTrimmed = batch.trim();
            if (batchTrimmed.isEmpty()) {
                continue;
            }

            try (Statement stmt = conn.createStatement()) {
                boolean hasResultSet = stmt.execute(batchTrimmed);
                do {
                    if (hasResultSet) {
                        try (ResultSet rs = stmt.getResultSet()) {
                            resultPrinter.exibirResultado(rs);
                        }
                    }
                    hasResultSet = stmt.getMoreResults();
                } while (hasResultSet || stmt.getUpdateCount() != -1);
            } catch (final SQLException e) {
                log.warn("Erro ao executar batch do script {}: {}", scriptName, e.getMessage());
                final String mensagem = e.getMessage().toLowerCase();
                if (!mensagem.contains("print") && !mensagem.contains("incorrect syntax")) {
                    throw e;
                }
            }
        }
    }
}
