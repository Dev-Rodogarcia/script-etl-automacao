package br.com.extrator.integracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.extrator.dominio.dataexport.contasapagar.ContasAPagarDTO;

class ClienteApiDataExportTest {

    @Test
    void deveListarSegmentosDiariosParaContasAPagar() {
        assertEquals(
            List.of(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 2), LocalDate.of(2026, 4, 3)),
            ClienteApiDataExport.listarSegmentosDiarios(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 3))
        );
    }

    @Test
    void deveConsolidarContasAPagarMantendoRegistroMaisFrescoPorCreatedAt() {
        final ContasAPagarDTO antiga = conta("100", "2026-04-01", "2026-04-01T08:00:00-03:00", "2026-04-01", "2026-04-01");
        final ContasAPagarDTO recente = conta("100", "2026-04-01", "2026-04-01T09:00:00-03:00", "2026-04-01", "2026-04-01");

        final List<ContasAPagarDTO> consolidadas = ClienteApiDataExport.consolidarContasAPagarPorSequenceCode(
            List.of(antiga, recente)
        );

        assertEquals(1, consolidadas.size());
        assertEquals("2026-04-01T09:00:00-03:00", consolidadas.get(0).getCreatedAt());
    }

    @Test
    void deveConsolidarResultadosSegmentadosPreservandoContagemBruta() {
        final ResultadoExtracao<ContasAPagarDTO> segmentoUm = ResultadoExtracao.completo(
            List.of(conta("100", "2026-04-01", "2026-04-01T08:00:00-03:00", null, null)),
            2,
            1
        );
        final ResultadoExtracao<ContasAPagarDTO> segmentoDois = ResultadoExtracao.incompleto(
            List.of(conta("100", "2026-04-01", "2026-04-01T09:00:00-03:00", null, null)),
            ResultadoExtracao.MotivoInterrupcao.LACUNA_PAGINACAO_422,
            3,
            1
        );

        final ResultadoExtracao<ContasAPagarDTO> consolidado = ClienteApiDataExport.consolidarResultadosContasAPagar(
            List.of(segmentoUm, segmentoDois)
        );

        assertFalse(consolidado.isCompleto());
        assertEquals(5, consolidado.getPaginasProcessadas());
        assertEquals(2, consolidado.getRegistrosExtraidos());
        assertEquals(1, consolidado.getDados().size());
        assertEquals("2026-04-01T09:00:00-03:00", consolidado.getDados().get(0).getCreatedAt());
    }

    private static ContasAPagarDTO conta(final String sequenceCode,
                                         final String issueDate,
                                         final String createdAt,
                                         final String transactionDate,
                                         final String liquidationDate) {
        final ContasAPagarDTO dto = new ContasAPagarDTO();
        dto.setSequenceCode(sequenceCode);
        dto.setIssueDate(issueDate);
        dto.setCreatedAt(createdAt);
        dto.setTransactionDate(transactionDate);
        dto.setLiquidationDate(liquidationDate);
        return dto;
    }
}
