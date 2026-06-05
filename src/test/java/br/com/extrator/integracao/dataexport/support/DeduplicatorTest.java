package br.com.extrator.integracao.dataexport.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.extrator.persistencia.entidade.ContasAPagarDataExportEntity;
import br.com.extrator.persistencia.entidade.ManifestoEntity;

class DeduplicatorTest {

    @Test
    void devePreservarMetricasFinanceirasOperacionaisAoDeduplicarManifestosComMesmoTimestamp() {
        final ManifestoEntity completo = manifesto(69533L);
        completo.setKm(new BigDecimal("460.00"));
        completo.setTotalCost(new BigDecimal("1234.56"));
        completo.setManifestFreightsTotal(new BigDecimal("3456.78"));
        completo.setTotalTaxedWeight(new BigDecimal("987.654"));
        completo.setCapacidadeKg(new BigDecimal("12000.00"));
        completo.setVehicleWeightCapacity(new BigDecimal("12000.00"));
        completo.setManifestItemsCount(12);
        completo.setFinalizedManifestItemsCount(11);

        final ManifestoEntity esparso = manifesto(69533L);
        esparso.setKm(BigDecimal.ZERO);
        esparso.setTotalCost(BigDecimal.ZERO);
        esparso.setManifestFreightsTotal(BigDecimal.ZERO);
        esparso.setTotalTaxedWeight(BigDecimal.ZERO);
        esparso.setCapacidadeKg(BigDecimal.ZERO);
        esparso.setVehicleWeightCapacity(BigDecimal.ZERO);
        esparso.setManifestItemsCount(0);
        esparso.setFinalizedManifestItemsCount(0);

        final var resultado = Deduplicator.deduplicarManifestos(List.of(completo, esparso));

        assertEquals(1, resultado.size());
        final ManifestoEntity consolidado = resultado.get(0);
        assertEquals(new BigDecimal("460.00"), consolidado.getKm());
        assertEquals(new BigDecimal("1234.56"), consolidado.getTotalCost());
        assertEquals(new BigDecimal("3456.78"), consolidado.getManifestFreightsTotal());
        assertEquals(new BigDecimal("987.654"), consolidado.getTotalTaxedWeight());
        assertEquals(new BigDecimal("12000.00"), consolidado.getCapacidadeKg());
        assertEquals(12, consolidado.getManifestItemsCount());
        assertEquals(11, consolidado.getFinalizedManifestItemsCount());
    }

    @Test
    void deveManterStatusMaisRecenteECompletarMetricasDoDuplicadoAnterior() {
        final ManifestoEntity anteriorCompleto = manifesto(69538L);
        anteriorCompleto.setCreatedAt(OffsetDateTime.parse("2026-05-01T08:00:00-03:00"));
        anteriorCompleto.setStatus("em_transito");
        anteriorCompleto.setKm(new BigDecimal("380.50"));
        anteriorCompleto.setTotalCost(new BigDecimal("900.00"));
        anteriorCompleto.setManifestFreightsTotal(new BigDecimal("2100.00"));
        anteriorCompleto.setTotalTaxedWeight(new BigDecimal("700.000"));
        anteriorCompleto.setCapacidadeKg(new BigDecimal("9000.00"));
        anteriorCompleto.setManifestItemsCount(9);
        anteriorCompleto.setFinalizedManifestItemsCount(8);

        final ManifestoEntity recenteEsparso = manifesto(69538L);
        recenteEsparso.setCreatedAt(OffsetDateTime.parse("2026-05-01T09:00:00-03:00"));
        recenteEsparso.setStatus("encerrado");

        final var resultado = Deduplicator.deduplicarManifestos(List.of(anteriorCompleto, recenteEsparso));

        assertEquals(1, resultado.size());
        final ManifestoEntity consolidado = resultado.get(0);
        assertEquals("encerrado", consolidado.getStatus());
        assertEquals(new BigDecimal("380.50"), consolidado.getKm());
        assertEquals(new BigDecimal("900.00"), consolidado.getTotalCost());
        assertEquals(new BigDecimal("2100.00"), consolidado.getManifestFreightsTotal());
        assertEquals(new BigDecimal("700.000"), consolidado.getTotalTaxedWeight());
        assertEquals(new BigDecimal("9000.00"), consolidado.getCapacidadeKg());
        assertEquals(9, consolidado.getManifestItemsCount());
        assertEquals(8, consolidado.getFinalizedManifestItemsCount());
    }

    @Test
    void devePreservarManifestosSemPickNemMdfeQuandoIdentificadorForDiferente() {
        final ManifestoEntity primeiro = manifestoSemPickNemMdfe(60962L, "hash-nota-a");
        final ManifestoEntity segundo = manifestoSemPickNemMdfe(60962L, "hash-nota-b");

        final var resultado = Deduplicator.deduplicarManifestos(List.of(primeiro, segundo));

        assertEquals(2, resultado.size());
    }

    @Test
    void deveDeduplicarManifestosSemPickNemMdfeQuandoIdentificadorForIgual() {
        final ManifestoEntity primeiro = manifestoSemPickNemMdfe(60962L, "hash-nota-a");
        primeiro.setCreatedAt(OffsetDateTime.parse("2026-05-01T08:00:00-03:00"));
        final ManifestoEntity segundo = manifestoSemPickNemMdfe(60962L, "hash-nota-a");
        segundo.setCreatedAt(OffsetDateTime.parse("2026-05-01T09:00:00-03:00"));
        segundo.setStatus("encerrado");

        final var resultado = Deduplicator.deduplicarManifestos(List.of(primeiro, segundo));

        assertEquals(1, resultado.size());
        assertEquals("encerrado", resultado.get(0).getStatus());
    }

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

    private ManifestoEntity manifesto(final Long sequenceCode) {
        final ManifestoEntity entity = new ManifestoEntity();
        entity.setSequenceCode(sequenceCode);
        entity.setPickSequenceCode(1000L);
        entity.setMdfeNumber(3896);
        entity.setCreatedAt(OffsetDateTime.parse("2026-05-01T08:00:00-03:00"));
        return entity;
    }

    private ManifestoEntity manifestoSemPickNemMdfe(final Long sequenceCode, final String identificadorUnico) {
        final ManifestoEntity entity = new ManifestoEntity();
        entity.setSequenceCode(sequenceCode);
        entity.setIdentificadorUnico(identificadorUnico);
        entity.setCreatedAt(OffsetDateTime.parse("2026-05-01T08:00:00-03:00"));
        return entity;
    }
}
