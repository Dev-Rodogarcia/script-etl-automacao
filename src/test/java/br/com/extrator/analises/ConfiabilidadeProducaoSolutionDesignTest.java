package br.com.extrator.analises;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ConfiabilidadeProducaoSolutionDesignTest {

    @Test
    void pruneGuardrailDeveBloquearJanelaZeradaQuandoHistoricoIndicaVolumeEsperado() {
        final PruneGuardrail guardrail = new PruneGuardrail(0.30d, 100);

        final PruneDecision decision = guardrail.evaluate(
            List.of(847, 860, 835, 852),
            0,
            true
        );

        assertFalse(decision.allowed());
        assertEquals(PruneBlockReason.ZERO_WITH_BASELINE, decision.reason());
    }

    @Test
    void pruneGuardrailDeveBloquearQuedaAbruptaMesmoComExtracaoMarcadaComoCompleta() {
        final PruneGuardrail guardrail = new PruneGuardrail(0.30d, 100);

        final PruneDecision decision = guardrail.evaluate(
            List.of(847, 860, 835, 852),
            120,
            true
        );

        assertFalse(decision.allowed());
        assertEquals(PruneBlockReason.SHARP_DROP, decision.reason());
    }

    @Test
    void pruneGuardrailDevePermitirPruneQuandoVolumeAtualPermaneceProximoDaLinhaDeBase() {
        final PruneGuardrail guardrail = new PruneGuardrail(0.30d, 100);

        final PruneDecision decision = guardrail.evaluate(
            List.of(847, 860, 835, 852),
            810,
            true
        );

        assertTrue(decision.allowed());
        assertNull(decision.reason());
    }

    @Test
    void replayGateDeveCurtoCircuitarChaveJaConcluidaDentroDaJanela() {
        final ReplayIdempotencyGate gate = new ReplayIdempotencyGate(Duration.ofHours(12));

        final Instant now = Instant.parse("2026-04-12T12:00:00Z");
        assertEquals(GateResult.STARTED, gate.tryStart("key-1", now));
        gate.markCompleted("key-1", now.plusSeconds(30));

        assertEquals(GateResult.ALREADY_COMPLETED, gate.tryStart("key-1", now.plusSeconds(60)));
    }

    @Test
    void replayGateDeveEvitarReexecucaoParalelaQuandoRunAnteriorAindaEstaEmStarted() {
        final ReplayIdempotencyGate gate = new ReplayIdempotencyGate(Duration.ofHours(12));

        final Instant now = Instant.parse("2026-04-12T12:00:00Z");
        assertEquals(GateResult.STARTED, gate.tryStart("key-1", now));
        assertEquals(GateResult.ALREADY_RUNNING, gate.tryStart("key-1", now.plusSeconds(5)));
    }

    @Test
    void replayGateDevePermitirNovaTentativaDepoisDeFalha() {
        final ReplayIdempotencyGate gate = new ReplayIdempotencyGate(Duration.ofHours(12));

        final Instant now = Instant.parse("2026-04-12T12:00:00Z");
        assertEquals(GateResult.STARTED, gate.tryStart("key-1", now));
        gate.markFailed("key-1", now.plusSeconds(30));

        assertEquals(GateResult.STARTED, gate.tryStart("key-1", now.plusSeconds(60)));
    }

    @Test
    void replayGateDevePermitirRecuperacaoDeStartedExpirado() {
        final ReplayIdempotencyGate gate = new ReplayIdempotencyGate(Duration.ofMinutes(30));

        final Instant now = Instant.parse("2026-04-12T12:00:00Z");
        assertEquals(GateResult.STARTED, gate.tryStart("key-1", now));

        assertEquals(GateResult.STARTED, gate.tryStart("key-1", now.plus(Duration.ofMinutes(45))));
    }

    @Test
    void daemonAlertPolicyDeveEscalonarDepoisDeTresCiclosConsecutivosComAlertaDeIntegridade() {
        final DaemonAlertEscalationPolicy policy = new DaemonAlertEscalationPolicy(3);

        assertFalse(policy.register(CycleOutcome.SUCCESS));
        assertFalse(policy.register(CycleOutcome.SUCCESS_WITH_ALERT));
        assertFalse(policy.register(CycleOutcome.SUCCESS_WITH_ALERT));
        assertTrue(policy.register(CycleOutcome.SUCCESS_WITH_ALERT));
    }

    @Test
    void daemonAlertPolicyDeveResetarContadorQuandoUmCicloSaudavelInterrompeASequencia() {
        final DaemonAlertEscalationPolicy policy = new DaemonAlertEscalationPolicy(3);

        assertFalse(policy.register(CycleOutcome.SUCCESS_WITH_ALERT));
        assertFalse(policy.register(CycleOutcome.SUCCESS));
        assertFalse(policy.register(CycleOutcome.SUCCESS_WITH_ALERT));
        assertFalse(policy.register(CycleOutcome.SUCCESS_WITH_ALERT));
        assertTrue(policy.register(CycleOutcome.SUCCESS_WITH_ALERT));
    }

    @Test
    void scopedOverrideDeveRestaurarValorAnteriorMesmoQuandoBlocoFalha() {
        final Map<String, String> props = new HashMap<>();
        props.put("timeout", "120000");

        try (ScopedOverride ignored = new ScopedOverride(props, Map.of("timeout", "1800000", "prune", "true"))) {
            assertEquals("1800000", props.get("timeout"));
            assertEquals("true", props.get("prune"));
        }

        assertEquals("120000", props.get("timeout"));
        assertNull(props.get("prune"));
    }

    @Test
    void fkActivationGateDeveBloquearAtivacaoQuandoHaOrfaos() {
        final ForeignKeyActivationGate gate = new ForeignKeyActivationGate();

        assertFalse(gate.canActivate(1, 0));
        assertFalse(gate.canActivate(0, 5));
        assertTrue(gate.canActivate(0, 0));
    }

    private enum PruneBlockReason {
        EXTRACTION_INCOMPLETE,
        ZERO_WITH_BASELINE,
        SHARP_DROP
    }

    private record PruneDecision(boolean allowed, PruneBlockReason reason) {}

    private static final class PruneGuardrail {
        private final double minimumRatio;
        private final int minimumBaseline;

        private PruneGuardrail(final double minimumRatio, final int minimumBaseline) {
            this.minimumRatio = minimumRatio;
            this.minimumBaseline = minimumBaseline;
        }

        private PruneDecision evaluate(final List<Integer> historicalVolumes,
                                       final int currentVolume,
                                       final boolean extractionComplete) {
            if (!extractionComplete) {
                return new PruneDecision(false, PruneBlockReason.EXTRACTION_INCOMPLETE);
            }

            final int baseline = median(historicalVolumes);
            if (baseline >= minimumBaseline && currentVolume == 0) {
                return new PruneDecision(false, PruneBlockReason.ZERO_WITH_BASELINE);
            }

            if (baseline >= minimumBaseline) {
                final double ratio = currentVolume / (double) baseline;
                if (ratio < minimumRatio) {
                    return new PruneDecision(false, PruneBlockReason.SHARP_DROP);
                }
            }

            return new PruneDecision(true, null);
        }

        private int median(final List<Integer> values) {
            final List<Integer> filtered = new ArrayList<>(values);
            filtered.sort(Comparator.naturalOrder());
            if (filtered.isEmpty()) {
                return 0;
            }
            return filtered.get(filtered.size() / 2);
        }
    }

    private enum GateStatus {
        STARTED,
        COMPLETED,
        FAILED
    }

    private enum GateResult {
        STARTED,
        ALREADY_RUNNING,
        ALREADY_COMPLETED
    }

    private record GateEntry(GateStatus status, Instant updatedAt) {}

    private static final class ReplayIdempotencyGate {
        private final Duration ttl;
        private final Map<String, GateEntry> entries = new HashMap<>();

        private ReplayIdempotencyGate(final Duration ttl) {
            this.ttl = ttl;
        }

        private GateResult tryStart(final String key, final Instant now) {
            final GateEntry existing = entries.get(key);
            if (existing == null || existing.updatedAt().plus(ttl).isBefore(now) || existing.status() == GateStatus.FAILED) {
                entries.put(key, new GateEntry(GateStatus.STARTED, now));
                return GateResult.STARTED;
            }

            if (existing.status() == GateStatus.COMPLETED) {
                return GateResult.ALREADY_COMPLETED;
            }

            return GateResult.ALREADY_RUNNING;
        }

        private void markCompleted(final String key, final Instant now) {
            entries.put(key, new GateEntry(GateStatus.COMPLETED, now));
        }

        private void markFailed(final String key, final Instant now) {
            entries.put(key, new GateEntry(GateStatus.FAILED, now));
        }
    }

    private enum CycleOutcome {
        SUCCESS,
        SUCCESS_WITH_ALERT,
        ERROR
    }

    private static final class DaemonAlertEscalationPolicy {
        private final int threshold;
        private int consecutiveAlertLikeCycles;

        private DaemonAlertEscalationPolicy(final int threshold) {
            this.threshold = threshold;
        }

        private boolean register(final CycleOutcome outcome) {
            if (outcome == CycleOutcome.SUCCESS_WITH_ALERT || outcome == CycleOutcome.ERROR) {
                consecutiveAlertLikeCycles++;
            } else {
                consecutiveAlertLikeCycles = 0;
            }
            return consecutiveAlertLikeCycles >= threshold;
        }
    }

    private static final class ScopedOverride implements AutoCloseable {
        private final Map<String, String> target;
        private final Map<String, String> previousValues = new HashMap<>();

        private ScopedOverride(final Map<String, String> target, final Map<String, String> overrides) {
            this.target = target;
            overrides.forEach((key, value) -> {
                previousValues.put(key, target.get(key));
                target.put(key, value);
            });
        }

        @Override
        public void close() {
            previousValues.forEach((key, previous) -> {
                if (previous == null) {
                    target.remove(key);
                } else {
                    target.put(key, previous);
                }
            });
        }
    }

    private static final class ForeignKeyActivationGate {
        private boolean canActivate(final long orphanRows, final long orphanKeys) {
            return orphanRows == 0 && orphanKeys == 0;
        }
    }
}
