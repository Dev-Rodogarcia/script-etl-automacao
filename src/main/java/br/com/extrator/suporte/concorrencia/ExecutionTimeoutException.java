package br.com.extrator.suporte.concorrencia;

public class ExecutionTimeoutException extends Exception {
    public ExecutionTimeoutException(final String message) {
        super(message);
    }

    public ExecutionTimeoutException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
