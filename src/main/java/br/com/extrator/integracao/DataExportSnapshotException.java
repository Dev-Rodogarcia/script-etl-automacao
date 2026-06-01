package br.com.extrator.integracao;

public class DataExportSnapshotException extends RuntimeException {
    public DataExportSnapshotException(final String message) {
        super(message);
    }

    public DataExportSnapshotException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
