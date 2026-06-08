package br.com.extrator.aplicacao.materializacao;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FatoMaterializacaoScheduler {
    private static final Logger logger = LoggerFactory.getLogger(FatoMaterializacaoScheduler.class);

    private final FatoMaterializacaoJob job;
    private final ScheduledExecutorService executor;
    private final List<String> procedures;
    private final Duration intervalo;
    private final Duration atrasoInicial;
    private final int timeoutSegundos;
    private final AtomicBoolean encerrando = new AtomicBoolean(false);

    public FatoMaterializacaoScheduler(final FatoMaterializacaoJob job,
                                       final List<String> procedures,
                                       final Duration intervalo,
                                       final Duration atrasoInicial,
                                       final int timeoutSegundos) {
        this.job = job;
        this.procedures = List.copyOf(procedures);
        this.intervalo = intervalo;
        this.atrasoInicial = atrasoInicial;
        this.timeoutSegundos = timeoutSegundos;
        this.executor = Executors.newSingleThreadScheduledExecutor(threadFactory());
    }

    public void iniciarEBloquear() throws InterruptedException {
        final CountDownLatch parada = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            parar();
            parada.countDown();
        }, "fato-materializacao-shutdown"));

        logger.info(
            "Scheduler de materializacao de fatos BI iniciado | intervalo_min={} | atraso_inicial_seg={} | procedures={}",
            intervalo.toMinutes(),
            atrasoInicial.toSeconds(),
            procedures
        );
        executor.scheduleWithFixedDelay(
            this::executarComProtecao,
            atrasoInicial.toSeconds(),
            intervalo.toSeconds(),
            TimeUnit.SECONDS
        );
        parada.await();
    }

    public void parar() {
        if (!encerrando.compareAndSet(false, true)) {
            return;
        }
        logger.info("Encerrando scheduler de materializacao de fatos BI.");
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }

    private void executarComProtecao() {
        try {
            job.executar(procedures, timeoutSegundos);
        } catch (final Exception e) {
            logger.error("Falha na materializacao agendada de fatos BI: {}", e.getMessage(), e);
        }
    }

    private ThreadFactory threadFactory() {
        return runnable -> {
            final Thread thread = new Thread(runnable, "fato-materializacao-scheduler");
            thread.setDaemon(false);
            return thread;
        };
    }
}
