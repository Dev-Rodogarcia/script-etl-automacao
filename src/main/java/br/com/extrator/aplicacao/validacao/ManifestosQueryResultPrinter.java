package br.com.extrator.aplicacao.validacao;

import java.sql.ResultSet;
import java.sql.SQLException;

import br.com.extrator.suporte.console.LoggerConsole;

/**
 * Formata e imprime ResultSet de consultas de validacao de manifestos.
 */
final class ManifestosQueryResultPrinter {
    private final LoggerConsole log;

    ManifestosQueryResultPrinter(final LoggerConsole log) {
        this.log = log;
    }

    void exibirResultado(final ResultSet rs) throws SQLException {
        final java.sql.ResultSetMetaData metaData = rs.getMetaData();
        final int columnCount = metaData.getColumnCount();

        for (int i = 1; i <= columnCount; i++) {
            if (i > 1) {
                System.out.print(" | ");
            }
            System.out.print(metaData.getColumnName(i));
        }
        log.console("");

        for (int i = 1; i <= columnCount; i++) {
            if (i > 1) {
                System.out.print("-+-");
            }
            for (int j = 0; j < metaData.getColumnName(i).length(); j++) {
                System.out.print("-");
            }
        }
        log.console("");

        int rowCount = 0;
        while (rs.next()) {
            rowCount++;
            for (int i = 1; i <= columnCount; i++) {
                if (i > 1) {
                    System.out.print(" | ");
                }
                final Object value = rs.getObject(i);
                System.out.print(value != null ? value.toString() : "NULL");
            }
            log.console("");
        }

        if (rowCount == 0) {
            System.out.println("(0 linhas)");
        } else {
            log.console("");
            System.out.println("Total: " + rowCount + " linha(s)");
        }
    }
}
