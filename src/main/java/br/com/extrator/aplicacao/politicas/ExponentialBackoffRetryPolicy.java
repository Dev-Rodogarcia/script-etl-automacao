package br.com.extrator.aplicacao.politicas;

import java.time.Duration;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.extrator.aplicacao.portas.ClockPort;

public class ExponentialBackoffRetryPolicy implements RetryPolicy {
    private static final Logger logger = LoggerFactory.getLogger(ExponentialBackoffRetryPolicy.class);

    private final int maxTentativas;
    private final long delayBaseMs;
    private final double multiplicador;
    private final double jitter;
    private final ClockPort clock;
    private final Random random;

    public ExponentialBackoffRetryPolicy(
        final int maxTentativas,
        final long delayBaseMs,
        final double multiplicador,
        final double jitter,
        final ClockPort clock
    ) {
        this.maxTentativas = Math.max(1, maxTentativas);
        this.delayBaseMs = Math.max(0L, delayBaseMs);
        this.multiplicador = Math.max(1.0d, multiplicador);
        this.jitter = Math.max(0.0d, jitter);
        this.clock = clock;
        this.random = new Random();
    }

    @Override
    public <T> T executar(final CheckedSupplier<T> supplier, final String operationName) throws Exception {
        Exception ultimoErro = null;
        for (int tentativa = 1; tentativa <= maxTentativas; tentativa++) {
            try {
                return supplier.get();
            } catch (Exception e) {
                ultimoErro = e;
                if (tentativa >= maxTentativas) {
                    throw e;
                }
                final long delay = calcularDelay(tentativa);
                logger.warn(
                    "Retry operation={} tentativa={}/{} delay_ms={} erro={}",
                    operationName,
                    tentativa,
                    maxTentativas,
                    delay,
                    e.getMessage()
                );
                try {
                    clock.dormir(Duration.ofMillis(delay));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Retry interrompido para operacao " + operationName, ie);
                }
            }
        }
        throw ultimoErro == null ? new IllegalStateException("Falha sem erro detalhado") : ultimoErro;
    }

    private long calcularDelay(final int tentativa) {
        final double exponencial = delayBaseMs * Math.pow(multiplicador, Math.max(0, tentativa - 1));
        final long base = Math.round(exponencial);
        if (jitter <= 0.0d || base <= 0L) {
            return base;
        }
        final long adicional = Math.round(base * jitter * random.nextDouble());
        return base + adicional;
    }
}



