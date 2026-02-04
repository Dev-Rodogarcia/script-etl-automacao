package br.com.extrator.util;

/**
 * Utilitário centralizado para operações de thread (ex.: pausas).
 * Evita uso direto de {@link Thread#sleep(long)} espalhado pelo código.
 *
 * @author Sistema de Extração ESL Cloud
 * @version 1.0
 */
public final class ThreadUtil {

    /**
     * Aguarda o tempo especificado (delega para {@link Thread#sleep(long)}).
     *
     * @param millis tempo em milissegundos
     * @throws InterruptedException se a thread for interrompida durante a espera
     */
    public static void aguardar(final long millis) throws InterruptedException {
        Thread.sleep(millis);
    }

    private ThreadUtil() {
        // Impede instanciação
    }
}
