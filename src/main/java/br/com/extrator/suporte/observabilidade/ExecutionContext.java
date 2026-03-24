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
    private static final String PROP_PARENT_EXECUTION_ID = "etl.parent.execution.id";
    private static final String PROP_PARENT_COMMAND = "etl.parent.command";
    private static final String PROP_PARENT_CYCLE_ID = "etl.parent.cycle.id";
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

    public static void clearCycleId() {
        MDC.remove(MDC_CYCLE_ID);
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
}
