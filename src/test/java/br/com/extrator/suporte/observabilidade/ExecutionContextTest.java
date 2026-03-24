package br.com.extrator.suporte.observabilidade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ExecutionContextTest {

    @AfterEach
    void limparContexto() {
        ExecutionContext.clear();
    }

    @Test
    void deveInicializarContextoComIdExecucao() {
        final String executionId = ExecutionContext.initialize("--fluxo-completo");

        assertNotNull(executionId);
        assertEquals(executionId, ExecutionContext.currentExecutionId());
        assertNotEquals("n/a", ExecutionContext.currentExecutionId());
    }

    @Test
    void devePropagarMdcParaRunnableEncapsulado() throws InterruptedException {
        final String executionId = ExecutionContext.initialize("--loop");
        ExecutionContext.setCycleId("cycle-123");
        final AtomicReference<String> executionIdThread = new AtomicReference<>();
        final AtomicReference<String> cycleIdThread = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);

        final Runnable wrapped = ExecutionContext.wrapRunnable(() -> {
            executionIdThread.set(ExecutionContext.currentExecutionId());
            cycleIdThread.set(ExecutionContext.currentCycleId());
            latch.countDown();
        });

        final Thread t = new Thread(wrapped, "execution-context-test");
        t.start();

        final boolean executou = latch.await(3, TimeUnit.SECONDS);
        if (!executou) {
            throw new AssertionError("Runnable encapsulado nao executou dentro do timeout.");
        }

        assertEquals(executionId, executionIdThread.get());
        assertEquals("cycle-123", cycleIdThread.get());
    }

    @Test
    void deveInicializarContextoComValoresHerdadosDoProcessoPai() {
        System.setProperty("etl.parent.execution.id", "exec-pai");
        System.setProperty("etl.parent.command", "--loop-daemon-run");
        System.setProperty("etl.parent.cycle.id", "cycle-pai");
        try {
            final String executionId = ExecutionContext.initialize(null);

            assertEquals("exec-pai", executionId);
            assertEquals("exec-pai", ExecutionContext.currentExecutionId());
            assertEquals("--loop-daemon-run", ExecutionContext.currentCommand());
            assertEquals("cycle-pai", ExecutionContext.currentCycleId());
            assertTrue(ExecutionContext.isLoopDaemonCommand());
        } finally {
            System.clearProperty("etl.parent.execution.id");
            System.clearProperty("etl.parent.command");
            System.clearProperty("etl.parent.cycle.id");
        }
    }
}
