package br.com.extrator.persistencia.repositorio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class FretePruneGuardrailEvaluatorTest {

    @Test
    void deveBloquearVolumeZeradoQuandoHistoricoIndicaJanelaPovoada() {
        final FretePruneGuardrailEvaluator evaluator = new FretePruneGuardrailEvaluator(0.30d, 100);

        final FretePruneGuardrailEvaluator.Decision decision = evaluator.evaluate(List.of(847, 860, 835, 852), 0);

        assertFalse(decision.allowDeletion());
        assertEquals(FretePruneGuardrailEvaluator.BlockReason.ZERO_WITH_BASELINE, decision.blockReason());
        assertEquals(852, decision.baselineMedian());
    }

    @Test
    void deveBloquearQuedaAbruptaDeVolume() {
        final FretePruneGuardrailEvaluator evaluator = new FretePruneGuardrailEvaluator(0.30d, 100);

        final FretePruneGuardrailEvaluator.Decision decision = evaluator.evaluate(List.of(847, 860, 835, 852), 120);

        assertFalse(decision.allowDeletion());
        assertEquals(FretePruneGuardrailEvaluator.BlockReason.SHARP_DROP, decision.blockReason());
    }

    @Test
    void devePermitirQuandoVolumeAtualAindaECompativelComHistorico() {
        final FretePruneGuardrailEvaluator evaluator = new FretePruneGuardrailEvaluator(0.30d, 100);

        final FretePruneGuardrailEvaluator.Decision decision = evaluator.evaluate(List.of(847, 860, 835, 852), 810);

        assertTrue(decision.allowDeletion());
        assertNull(decision.blockReason());
    }
}
