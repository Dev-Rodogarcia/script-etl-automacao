package br.com.extrator.aplicacao.validacao;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import br.com.extrator.suporte.console.LoggerConsole;

final class ValidacaoSqlResultPrinter {
    private final LoggerConsole log;

    ValidacaoSqlResultPrinter(final LoggerConsole log) {
        this.log = log;
    }

    void exibirResultado(final ResultSet rs) throws SQLException {
        final ResultSetMetaData metaData = rs.getMetaData();
        final int columnCount = metaData.getColumnCount();

        if (columnCount == 0) {
            return;
        }

        final List<List<String>> rows = new ArrayList<>();
        final List<Integer> columnWidths = new ArrayList<>();

        for (int i = 1; i <= columnCount; i++) {
            columnWidths.add(metaData.getColumnName(i).length());
        }

        while (rs.next()) {
            final List<String> row = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                final Object value = rs.getObject(i);
                final String strValue = value != null ? value.toString() : "NULL";
                row.add(strValue);
                if (strValue.length() > columnWidths.get(i - 1)) {
                    columnWidths.set(i - 1, Math.min(strValue.length(), 100));
                }
            }
            rows.add(row);
        }

        if (rows.isEmpty()) {
            log.console("(Nenhum resultado)");
            return;
        }

        log.console(formatarCabecalho(metaData, columnWidths, columnCount));
        log.console(formatarSeparador(columnWidths, columnCount));

        for (final List<String> row : rows) {
            log.console(formatarLinha(row, columnWidths, columnCount));
        }

        log.console("");
        log.info("Total: {} linha(s)", rows.size());
    }

    private String formatarCabecalho(final ResultSetMetaData metaData,
                                     final List<Integer> columnWidths,
                                     final int columnCount) throws SQLException {
        final StringBuilder header = new StringBuilder();
        for (int i = 1; i <= columnCount; i++) {
            if (i > 1) {
                header.append(" | ");
            }
            header.append(String.format("%-" + columnWidths.get(i - 1) + "s", metaData.getColumnName(i)));
        }
        return header.toString();
    }

    private String formatarSeparador(final List<Integer> columnWidths, final int columnCount) {
        final StringBuilder separator = new StringBuilder();
        for (int i = 0; i < columnCount; i++) {
            if (i > 0) {
                separator.append("-+-");
            }
            separator.append("-".repeat(columnWidths.get(i)));
        }
        return separator.toString();
    }

    private String formatarLinha(final List<String> row, final List<Integer> columnWidths, final int columnCount) {
        final StringBuilder rowStr = new StringBuilder();
        for (int i = 0; i < columnCount; i++) {
            if (i > 0) {
                rowStr.append(" | ");
            }
            String value = row.get(i);
            if (value.length() > columnWidths.get(i)) {
                value = value.substring(0, columnWidths.get(i) - 3) + "...";
            }
            rowStr.append(String.format("%-" + columnWidths.get(i) + "s", value));
        }
        return rowStr.toString();
    }
}
