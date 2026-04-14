package br.com.extrator.aplicacao.extracao;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class RecoveryUseCaseTest {

    @Test
    void deveFalharQuandoPeriodoInvalido() {
        final RecoveryUseCase useCase = new RecoveryUseCase();
        assertThrows(
            IllegalArgumentException.class,
            () -> useCase.executarReplay(LocalDate.of(2026, 1, 2), LocalDate.of(2026, 1, 1), null, null, true)
        );
    }

    @Test
    void deveCurtoCircuitarQuandoReplayJaFoiConcluido() {
        final FakeRecoveryReplayGate gate = new FakeRecoveryReplayGate(RecoveryReplayGate.StartResult.ALREADY_COMPLETED);
        final FakeExtracaoPorIntervaloUseCase extracao = new FakeExtracaoPorIntervaloUseCase();
        final RecoveryUseCase useCase = new RecoveryUseCase(
            extracao,
            gate,
            Clock.fixed(Instant.parse("2026-04-13T03:00:00Z"), ZoneOffset.UTC)
        );

        assertDoesNotThrow(() ->
            useCase.executarReplay(LocalDate.of(2026, 4, 10), LocalDate.of(2026, 4, 10), "graphql", "fretes", true)
        );

        assertEquals(0, extracao.execucoes.get());
        assertEquals(0, gate.completedCalls.get());
        assertEquals(0, gate.failedCalls.get());
    }

    @Test
    void deveMarcarReplayComoConcluidoQuandoFluxoExecutaComSucesso() throws Exception {
        final FakeRecoveryReplayGate gate = new FakeRecoveryReplayGate(RecoveryReplayGate.StartResult.STARTED);
        final FakeExtracaoPorIntervaloUseCase extracao = new FakeExtracaoPorIntervaloUseCase();
        final RecoveryUseCase useCase = new RecoveryUseCase(
            extracao,
            gate,
            Clock.fixed(Instant.parse("2026-04-13T03:00:00Z"), ZoneOffset.UTC)
        );

        useCase.executarReplay(LocalDate.of(2026, 4, 10), LocalDate.of(2026, 4, 10), "graphql", "fretes", true);

        assertEquals(1, extracao.execucoes.get());
        assertEquals(1, gate.completedCalls.get());
        assertEquals(0, gate.failedCalls.get());
    }

    @Test
    void deveMarcarReplayComoFalhoQuandoFluxoLancaErro() {
        final FakeRecoveryReplayGate gate = new FakeRecoveryReplayGate(RecoveryReplayGate.StartResult.STARTED);
        final FakeExtracaoPorIntervaloUseCase extracao = new FakeExtracaoPorIntervaloUseCase();
        extracao.throwOnExecute = true;
        final RecoveryUseCase useCase = new RecoveryUseCase(
            extracao,
            gate,
            Clock.fixed(Instant.parse("2026-04-13T03:00:00Z"), ZoneOffset.UTC)
        );

        assertThrows(
            IllegalStateException.class,
            () -> useCase.executarReplay(LocalDate.of(2026, 4, 10), LocalDate.of(2026, 4, 10), "graphql", "fretes", true)
        );

        assertEquals(1, gate.failedCalls.get());
        assertEquals(0, gate.completedCalls.get());
    }

    @Test
    void devePreservarExcecaoOriginalQuandoMarkFailedTambemFalha() {
        final FakeRecoveryReplayGate gate = new FakeRecoveryReplayGate(RecoveryReplayGate.StartResult.STARTED);
        gate.throwOnMarkFailed = true;
        final FakeExtracaoPorIntervaloUseCase extracao = new FakeExtracaoPorIntervaloUseCase();
        extracao.throwOnExecute = true;
        final RecoveryUseCase useCase = new RecoveryUseCase(
            extracao,
            gate,
            Clock.fixed(Instant.parse("2026-04-13T03:00:00Z"), ZoneOffset.UTC)
        );

        final IllegalStateException erro = assertThrows(
            IllegalStateException.class,
            () -> useCase.executarReplay(LocalDate.of(2026, 4, 10), LocalDate.of(2026, 4, 10), "graphql", "fretes", true)
        );

        assertEquals("falha simulada", erro.getMessage());
        assertEquals(1, erro.getSuppressed().length);
        assertSame(gate.markFailedException, erro.getSuppressed()[0]);
    }

    private static final class FakeExtracaoPorIntervaloUseCase extends ExtracaoPorIntervaloUseCase {
        private final AtomicInteger execucoes = new AtomicInteger();
        private boolean throwOnExecute;

        @Override
        public void executar(final ExtracaoPorIntervaloRequest request) {
            execucoes.incrementAndGet();
            if (throwOnExecute) {
                throw new IllegalStateException("falha simulada");
            }
        }
    }

    private static final class FakeRecoveryReplayGate implements RecoveryReplayGate {
        private final StartResult startResult;
        private final AtomicInteger completedCalls = new AtomicInteger();
        private final AtomicInteger failedCalls = new AtomicInteger();
        private boolean throwOnMarkFailed;
        private RuntimeException markFailedException;

        private FakeRecoveryReplayGate(final StartResult startResult) {
            this.startResult = startResult;
        }

        @Override
        public StartResult tryStart(final ReplayAttempt attempt, final java.time.LocalDateTime now) {
            return startResult;
        }

        @Override
        public void markCompleted(final String idempotencyKey,
                                  final String executionUuid,
                                  final java.time.LocalDateTime finishedAt) {
            completedCalls.incrementAndGet();
        }

        @Override
        public void markFailed(final String idempotencyKey,
                               final String executionUuid,
                               final java.time.LocalDateTime finishedAt,
                               final String errorMessage) {
            failedCalls.incrementAndGet();
            if (throwOnMarkFailed) {
                markFailedException = new RuntimeException("falha ao marcar replay como failed");
                throw markFailedException;
            }
        }
    }
}
