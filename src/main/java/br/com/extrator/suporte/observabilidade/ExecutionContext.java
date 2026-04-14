package br.com.extrator.suporte.observabilidade;

/* ==[DOC-FILE]===============================================================
Arquivo : src/main/java/br/com/extrator/suporte/observabilidade/ExecutionContext.java
Classe  : ExecutionContext (class)
Pacote  : br.com.extrator.suporte.observabilidade
Modulo  : Suporte - Observabilidade
Papel   : [DESC PENDENTE]
Conecta com: Sem dependencia interna
Fluxo geral:
1) [PENDENTE]
Estrutura interna:
Metodos: [PENDENTE]
Atributos: [PENDENTE]
[DOC-FILE-END]============================================================== */


import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.slf4j.MDC;

/**
 * Tracks execution context (execution id and command) in MDC.
 */
public final class ExecutionContext {

    public static final String MDC_EXECUTION_ID = "etl_execution_id";
    public static final String MDC_COMMAND = "etl_command";
    public static final String MDC_CYCLE_ID = "etl_cycle_id";
    public static final String MDC_RETRY_ATTEMPT = "etl_retry_attempt";
    public static final String MDC_RETRY_MAX_ATTEMPTS = "etl_retry_max_attempts";
    private static final String PROP_PARENT_EXECUTION_ID = "etl.parent.execution.id";
    private static final String PROP_PARENT_COMMAND = "etl.parent.command";
    private static final String PROP_PARENT_CYCLE_ID = "etl.parent.cycle.id";
    private static final String PROP_PARENT_RETRY_ATTEMPT = "etl.parent.retry.attempt";
    private static final String PROP_PARENT_RETRY_MAX_ATTEMPTS = "etl.parent.retry.max_attempts";
    private static final String LOOP_DAEMON_RUN = "--loop-daemon-run";
    private static final String NA = "n/a";

    private ExecutionContext() {
        // utility
    }

    public static String initialize(final String commandName) {
        final String executionId = inheritedValue(PROP_PARENT_EXECUTION_ID, UUID.randomUUID().toString());
        MDC.put(MDC_EXECUTION_ID, executionId);
        MDC.put(MDC_COMMAND, sanitizeCommand(inheritedValue(PROP_PARENT_COMMAND, commandName)));
        final String inheritedCycleId = sanitizeValue(System.getProperty(PROP_PARENT_CYCLE_ID));
        if (!NA.equals(inheritedCycleId)) {
            MDC.put(MDC_CYCLE_ID, inheritedCycleId);
        }
        final Integer inheritedRetryAttempt = parsePositiveInteger(System.getProperty(PROP_PARENT_RETRY_ATTEMPT));
        final Integer inheritedRetryMaxAttempts = parsePositiveInteger(System.getProperty(PROP_PARENT_RETRY_MAX_ATTEMPTS));
        if (inheritedRetryAttempt != null && inheritedRetryMaxAttempts != null) {
            setRetryContext(inheritedRetryAttempt, inheritedRetryMaxAttempts);
        } else {
            clearRetryContext();
        }
        return executionId;
    }

    public static String currentExecutionId() {
        final String value = MDC.get(MDC_EXECUTION_ID);
        return value == null || value.isBlank() ? NA : value;
    }

    public static String currentCommand() {
        final String value = MDC.get(MDC_COMMAND);
        return value == null || value.isBlank() ? NA : value;
    }

    public static void setCycleId(final String cycleId) {
        final String sanitized = sanitizeValue(cycleId);
        if (NA.equals(sanitized)) {
            MDC.remove(MDC_CYCLE_ID);
            return;
        }
        MDC.put(MDC_CYCLE_ID, sanitized);
    }

    public static String currentCycleId() {
        final String value = MDC.get(MDC_CYCLE_ID);
        return value == null || value.isBlank() ? NA : value;
    }

    public static void setRetryContext(final int attempt, final int maxAttempts) {
        final int sanitizedMaxAttempts = Math.max(1, maxAttempts);
        final int sanitizedAttempt = Math.max(1, Math.min(attempt, sanitizedMaxAttempts));
        MDC.put(MDC_RETRY_ATTEMPT, Integer.toString(sanitizedAttempt));
        MDC.put(MDC_RETRY_MAX_ATTEMPTS, Integer.toString(sanitizedMaxAttempts));
    }

    public static int currentRetryAttempt() {
        return parsePositiveInteger(MDC.get(MDC_RETRY_ATTEMPT), 1);
    }

    public static int currentRetryMaxAttempts() {
        return parsePositiveInteger(MDC.get(MDC_RETRY_MAX_ATTEMPTS), 1);
    }

    public static boolean hasRetryContext() {
        return parsePositiveInteger(MDC.get(MDC_RETRY_ATTEMPT)) != null
            && parsePositiveInteger(MDC.get(MDC_RETRY_MAX_ATTEMPTS)) != null;
    }

    public static boolean isRetryFinalAttempt() {
        return currentRetryAttempt() >= currentRetryMaxAttempts();
    }

    public static boolean isRetryIntermediaryAttempt() {
        return currentRetryMaxAttempts() > 1 && currentRetryAttempt() < currentRetryMaxAttempts();
    }

    public static void clearCycleId() {
        MDC.remove(MDC_CYCLE_ID);
    }

    public static void clearRetryContext() {
        MDC.remove(MDC_RETRY_ATTEMPT);
        MDC.remove(MDC_RETRY_MAX_ATTEMPTS);
    }

    public static boolean isLoopDaemonCommand() {
        return LOOP_DAEMON_RUN.equalsIgnoreCase(currentCommand());
    }

    public static Runnable wrapRunnable(final Runnable delegate) {
        if (delegate == null) {
            return () -> {
                // noop
            };
        }

        final Map<String, String> capturedContext = MDC.getCopyOfContextMap();
        return () -> {
            MDC.clear();
            try {
                if (capturedContext != null) {
                    MDC.setContextMap(capturedContext);
                }
                delegate.run();
            } finally {
                MDC.clear();
            }
        };
    }

    public static <V> Callable<V> wrapCallable(final Callable<V> delegate) {
        if (delegate == null) {
            return () -> null;
        }
        final Map<String, String> capturedContext = MDC.getCopyOfContextMap();
        return () -> {
            MDC.clear();
            try {
                if (capturedContext != null) {
                    MDC.setContextMap(capturedContext);
                }
                return delegate.call();
            } finally {
                MDC.clear();
            }
        };
    }

    public static void clear() {
        MDC.clear();
    }

    private static String sanitizeCommand(final String commandName) {
        if (commandName == null || commandName.isBlank()) {
            return NA;
        }
        return commandName.trim().replaceAll("\\s+", "_");
    }

    private static String inheritedValue(final String propertyName, final String fallback) {
        final String inherited = sanitizeValue(System.getProperty(propertyName));
        if (!NA.equals(inherited)) {
            return inherited;
        }
        return fallback;
    }

    private static String sanitizeValue(final String value) {
        if (value == null || value.isBlank()) {
            return NA;
        }
        return value.trim().replaceAll("\\s+", "_");
    }

    private static Integer parsePositiveInteger(final String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            final int parsed = Integer.parseInt(value.trim());
            return parsed > 0 ? parsed : null;
        } catch (final NumberFormatException ignored) {
            return null;
        }
    }

    private static int parsePositiveInteger(final String value, final int fallback) {
        final Integer parsed = parsePositiveInteger(value);
        return parsed == null ? fallback : parsed;
    }
}
