package br.com.extrator.integracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.extrator.dominio.graphql.coletas.ColetaNodeDTO;

class GraphQLColetaSupportTest {

    @Test
    void deveFalharQuandoNaoHouverIdNemSequenceCode() {
        final ColetaNodeDTO coleta = new ColetaNodeDTO();

        assertThrows(IllegalStateException.class, () -> GraphQLColetaSupport.resolverChaveDeduplicacao(coleta));
    }

    @Test
    void deveUsarSequenceCodeQuandoIdNaoExistir() {
        final ColetaNodeDTO coleta = new ColetaNodeDTO();
        coleta.setSequenceCode(123L);

        assertEquals("SEQ:123", GraphQLColetaSupport.resolverChaveDeduplicacao(coleta));
    }

    @Test
    void devePreferirRegistroMaisFrescoPorStatusUpdatedAt() {
        final ColetaNodeDTO antiga = coleta("pick-1", 100L, "2026-04-01", "2026-04-02", "2026-04-01", "2026-04-03T08:00:00");
        final ColetaNodeDTO recente = coleta("pick-1", 100L, "2026-04-01", "2026-04-02", "2026-04-01", "2026-04-03T09:00:00");

        final List<ColetaNodeDTO> deduplicadas = GraphQLColetaSupport.deduplicarColetasPorId(List.of(antiga, recente));

        assertEquals(1, deduplicadas.size());
        assertEquals("2026-04-03T09:00:00", deduplicadas.get(0).getStatusUpdatedAt());
    }

    @Test
    void deveUsarFinishDateQuandoStatusUpdatedAtNaoExistir() {
        final ColetaNodeDTO antiga = coleta("pick-1", 100L, "2026-04-01", "2026-04-02", "2026-04-01", null);
        final ColetaNodeDTO recente = coleta("pick-1", 100L, "2026-04-01", "2026-04-03", "2026-04-01", null);

        final List<ColetaNodeDTO> deduplicadas = GraphQLColetaSupport.deduplicarColetasPorId(List.of(recente, antiga));

        assertEquals(1, deduplicadas.size());
        assertEquals("2026-04-03", deduplicadas.get(0).getFinishDate());
    }

    @Test
    void deveUsarRequestDateComoUltimoCriterioDeFrescor() {
        final ColetaNodeDTO antiga = coleta(null, 100L, "2026-04-01", null, null, null);
        final ColetaNodeDTO recente = coleta(null, 100L, "2026-04-05", null, null, null);

        final List<ColetaNodeDTO> deduplicadas = GraphQLColetaSupport.deduplicarColetasPorId(List.of(antiga, recente));

        assertEquals(1, deduplicadas.size());
        assertEquals("2026-04-05", deduplicadas.get(0).getRequestDate());
    }

    private static ColetaNodeDTO coleta(final String id,
                                        final Long sequenceCode,
                                        final String requestDate,
                                        final String finishDate,
                                        final String serviceDate,
                                        final String statusUpdatedAt) {
        final ColetaNodeDTO coleta = new ColetaNodeDTO();
        coleta.setId(id);
        coleta.setSequenceCode(sequenceCode);
        coleta.setRequestDate(requestDate);
        coleta.setFinishDate(finishDate);
        coleta.setServiceDate(serviceDate);
        coleta.setStatusUpdatedAt(statusUpdatedAt);
        return coleta;
    }
}
