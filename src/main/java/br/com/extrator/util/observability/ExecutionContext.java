package br.com.extrator.util.observability;

import java.util.Map;
import java.util.UUID;

import org.slf4j.MDC;

/**
 * Tracks execution context (execution id and command) in MDC.
 */
public final class ExecutionContext {

    public static final String MDC_EXECUTION_ID = "etl_execution_id";
    public static final String MDC_COMMAND = "etl_command";
    private static final String NA = "n/a";

    private ExecutionContext() {
        // utility
    }

    public static String initialize(final String commandName) {
        final String executionId = UUID.randomUUID().toString();
        MDC.put(MDC_EXECUTION_ID, executionId);
        MDC.put(MDC_COMMAND, sanitizeCommand(commandName));
        return executionId;
    }

    public static String currentExecutionId() {
        final String value = MDC.get(MDC_EXECUTION_ID);
        return value == null || value.isBlank() ? NA : value;
    }

    public static Runnable wrapRunnable(final Runnable delegate) {
        if (delegate == null) {
            return () -> {
                // noop
            };
        }

        final Map<String, String> capturedContext = MDC.getCopyOfContextMap();
        return () -> {
            final Map<String, String> previous = MDC.getCopyOfContextMap();
            try {
                if (capturedContext == null) {
                    MDC.clear();
                } else {
                    MDC.setContextMap(capturedContext);
                }
                delegate.run();
            } finally {
                if (previous == null) {
                    MDC.clear();
                } else {
                    MDC.setContextMap(previous);
                }
            }
        };
    }

    public static void clear() {
        MDC.remove(MDC_EXECUTION_ID);
        MDC.remove(MDC_COMMAND);
    }

    private static String sanitizeCommand(final String commandName) {
        if (commandName == null || commandName.isBlank()) {
            return NA;
        }
        return commandName.trim().replaceAll("\\s+", "_");
    }
}
