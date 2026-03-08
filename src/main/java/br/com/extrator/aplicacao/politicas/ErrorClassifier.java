package br.com.extrator.aplicacao.politicas;

import java.util.Locale;

public class ErrorClassifier {
    public ErrorTaxonomy classificar(final Throwable throwable) {
        if (throwable == null) {
            return ErrorTaxonomy.TRANSIENT_API_ERROR;
        }
        final Throwable root = rootCause(throwable);
        final String msg = message(root);

        if (root instanceof java.net.http.HttpTimeoutException || msg.contains("timeout")) {
            return ErrorTaxonomy.TIMEOUT;
        }
        if (root instanceof IllegalArgumentException || msg.contains("validation")) {
            return ErrorTaxonomy.PERMANENT_VALIDATION_ERROR;
        }
        if (root.getClass().getName().equals("java.sql.SQLException") && (msg.contains("duplicate") || msg.contains("conflict") || msg.contains("unique"))) {
            return ErrorTaxonomy.DB_CONFLICT;
        }
        if (msg.contains("schema") || msg.contains("column") || msg.contains("field mismatch")) {
            return ErrorTaxonomy.SCHEMA_DRIFT;
        }
        if (msg.contains("quality") || msg.contains("freshness") || msg.contains("referential")) {
            return ErrorTaxonomy.DATA_QUALITY_BREACH;
        }
        return ErrorTaxonomy.TRANSIENT_API_ERROR;
    }

    private Throwable rootCause(final Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private String message(final Throwable t) {
        final String message = t.getMessage();
        return message == null ? "" : message.toLowerCase(Locale.ROOT);
    }
}


