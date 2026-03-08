package br.com.extrator.persistencia.repositorio;

/**
 * Signals interruption during retry wait for execution history persistence.
 */
public class HistoryPersistenceInterruptedException extends RuntimeException {

    public HistoryPersistenceInterruptedException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
