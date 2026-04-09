package br.com.extrator.aplicacao.extracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import br.com.extrator.plataforma.auditoria.dominio.ExecutionAuditRecord;
import br.com.extrator.plataforma.auditoria.dominio.ExecutionWindowPlan;
import br.com.extrator.suporte.validacao.ConstantesEntidades;

class FluxoCompletoUseCaseTest {

    @Test
    void deveConfirmarWatermarkApenasParaEntidadesComStatusConfirmavelEApiCompleta() {
        final Map<String, ExecutionWindowPlan> planos = Map.of(
            ConstantesEntidades.COLETAS, plano(LocalDate.of(2026, 4, 1)),
            ConstantesEntidades.FRETES, plano(LocalDate.of(2026, 4, 1)),
            ConstantesEntidades.MANIFESTOS, plano(LocalDate.of(2026, 4, 1))
        );

        final List<ExecutionAuditRecord> auditoria = List.of(
            audit(ConstantesEntidades.COLETAS, "COMPLETO", true, LocalDateTime.of(2026, 4, 1, 10, 0)),
            audit(ConstantesEntidades.FRETES, "INCOMPLETO_DB", true, LocalDateTime.of(2026, 4, 1, 10, 0)),
            audit(ConstantesEntidades.MANIFESTOS, "RECONCILIADO", true, LocalDateTime.of(2026, 4, 1, 11, 0))
        );

        final Set<String> confirmadas = FluxoCompletoUseCase.resolverEntidadesComWatermarkConfirmado(planos, auditoria);

        assertEquals(Set.of(ConstantesEntidades.COLETAS, ConstantesEntidades.MANIFESTOS), confirmadas);
        assertFalse(confirmadas.contains(ConstantesEntidades.FRETES));
    }

    @Test
    void deveIgnorarEntidadeQuandoApiNaoEstiverCompleta() {
        final Map<String, ExecutionWindowPlan> planos = Map.of(
            ConstantesEntidades.COLETAS, plano(LocalDate.of(2026, 4, 1))
        );

        final Set<String> confirmadas = FluxoCompletoUseCase.resolverEntidadesComWatermarkConfirmado(
            planos,
            List.of(audit(ConstantesEntidades.COLETAS, "COMPLETO", false, LocalDateTime.of(2026, 4, 1, 10, 0)))
        );

        assertTrue(confirmadas.isEmpty());
    }

    private static ExecutionWindowPlan plano(final LocalDate dataFim) {
        return new ExecutionWindowPlan(
            dataFim.minusDays(1),
            dataFim,
            dataFim.minusDays(1).atStartOfDay(),
            dataFim.atTime(LocalTime.MAX)
        );
    }

    private static ExecutionAuditRecord audit(final String entidade,
                                              final String status,
                                              final boolean apiCompleta,
                                              final LocalDateTime finishedAt) {
        return new ExecutionAuditRecord(
            "exec-1",
            entidade,
            finishedAt.minusHours(1),
            finishedAt,
            finishedAt.minusHours(1),
            finishedAt,
            status,
            10,
            10,
            10,
            10,
            apiCompleta,
            null,
            1,
            0,
            0,
            finishedAt.minusHours(1),
            finishedAt,
            "--fluxo-completo",
            "cycle-1",
            null
        );
    }
}
