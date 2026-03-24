package br.com.extrator.suporte.concorrencia;

public class ThreadLeakDetectedException extends ExecutionTimeoutException {
    private final ThreadLeakDetector.LeakReport leakReport;

    public ThreadLeakDetectedException(final String message,
                                       final Throwable cause,
                                       final ThreadLeakDetector.LeakReport leakReport) {
        super(message, cause);
        this.leakReport = leakReport;
    }

    public ThreadLeakDetector.LeakReport getLeakReport() {
        return leakReport;
    }
}
