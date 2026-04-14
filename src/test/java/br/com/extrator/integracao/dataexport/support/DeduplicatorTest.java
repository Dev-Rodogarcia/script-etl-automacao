package br.com.extrator.integracao.dataexport.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

import br.com.extrator.persistencia.entidade.ContasAPagarDataExportEntity;

class DeduplicatorTest {

    @Test
    void deveManterContaMaisFrescaPorDataCriacao() {
        final ContasAPagarDataExportEntity antiga = conta(100L);
        antiga.setDataCriacao(OffsetDateTime.parse("2026-04-10T08:00:00-03:00"));

        final ContasAPagarDataExportEntity recente = conta(100L);
        recente.setDataCriacao(OffsetDateTime.parse("2026-04-10T09:00:00-03:00"));

        final var resultado = Deduplicator.deduplicarFaturasAPagar(java.util.List.of(antiga, recente));

        assertEquals(1, resultado.size());
        assertEquals(OffsetDateTime.parse("2026-04-10T09:00:00-03:00"), resultado.get(0).getDataCriacao());
    }

    @Test
    void deveUsarDataTransacaoComoFallbackQuandoDataCriacaoNaoExiste() {
        final ContasAPagarDataExportEntity antiga = conta(200L);
        antiga.setDataTransacao(LocalDate.of(2026, 4, 10));

        final ContasAPagarDataExportEntity recente = conta(200L);
        recente.setDataTransacao(LocalDate.of(2026, 4, 11));

        final var resultado = Deduplicator.deduplicarFaturasAPagar(java.util.List.of(recente, antiga));

        assertEquals(1, resultado.size());
        assertEquals(LocalDate.of(2026, 4, 11), resultado.get(0).getDataTransacao());
    }

    private ContasAPagarDataExportEntity conta(final Long sequenceCode) {
        final ContasAPagarDataExportEntity entity = new ContasAPagarDataExportEntity();
        entity.setSequenceCode(sequenceCode);
        entity.setDataExtracao(LocalDateTime.of(2026, 4, 13, 10, 0));
        return entity;
    }
}
