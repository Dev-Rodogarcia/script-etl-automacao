/* ==[DOC-FILE]===============================================================
Arquivo : src/main/java/br/com/extrator/aplicacao/politicas/ErrorClassifier.java
Classe  : ErrorClassifier (class)
Pacote  : br.com.extrator.aplicacao.politicas
Modulo  : Politicas - Resiliencia

Papel   : Classifica erros em taxonomia (TIMEOUT, VALIDATION, DB_CONFLICT, SCHEMA_DRIFT, DATA_QUALITY_BREACH, TRANSIENT_API_ERROR).

Conecta com:
- ErrorTaxonomy (enum de classificacao)

Fluxo geral:
1) classificar() recebe Throwable e analisa chain de causas.
2) Detecta tipo por excecao ou keywords em mensagem (timeout, validation, schema, quality, etc).
3) Retorna ErrorTaxonomy para uso em retry/fallback policies.

Estrutura interna:
Metodos principais:
- classificar(Throwable): classifica erro em categoria.
- rootCause(Throwable): percorre chain ate causa raiz.
- message(Throwable): extrai mensagem lowercase para pattern matching.
Regras de classificacao:
- HttpTimeoutException ou "timeout" => TIMEOUT.
- IllegalArgumentException ou "validation" => PERMANENT_VALIDATION_ERROR.
- SQLException (duplicate/unique/conflict) => DB_CONFLICT.
- "schema" ou "column" => SCHEMA_DRIFT.
- "quality" ou "freshness" => DATA_QUALITY_BREACH.
- Else => TRANSIENT_API_ERROR (default).
[DOC-FILE-END]============================================================== */
package br.com.extrator.aplicacao.politicas;

import java.net.http.HttpTimeoutException;
import java.util.Locale;
import java.util.concurrent.TimeoutException;

import br.com.extrator.suporte.concorrencia.ExecutionTimeoutException;

public class ErrorClassifier {
    public ErrorTaxonomy classificar(final Throwable throwable) {
        if (throwable == null) {
            return ErrorTaxonomy.TRANSIENT_API_ERROR;
        }
        final Throwable root = rootCause(throwable);
        final String msg = messages(throwable);

        if (causedBy(throwable, ExecutionTimeoutException.class, HttpTimeoutException.class, TimeoutException.class)
            || contemIndicadorTimeout(msg)) {
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

    private boolean causedBy(final Throwable throwable, final Class<?>... tipos) {
        Throwable current = throwable;
        while (current != null) {
            for (final Class<?> tipo : tipos) {
                if (tipo.isInstance(current)) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean contemIndicadorTimeout(final String msg) {
        return msg.contains("timeout")
            || msg.contains("timed out")
            || msg.contains("excedeu timeout")
            || msg.contains("timeout ao executar");
    }

    private String messages(final Throwable throwable) {
        final StringBuilder builder = new StringBuilder();
        Throwable current = throwable;
        while (current != null) {
            final String message = current.getMessage();
            if (message != null && !message.isBlank()) {
                if (builder.length() > 0) {
                    builder.append(' ');
                }
                builder.append(message.toLowerCase(Locale.ROOT));
            }
            current = current.getCause();
        }
        return builder.toString();
    }
}

