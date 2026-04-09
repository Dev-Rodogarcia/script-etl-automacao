package br.com.extrator.integracao;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import br.com.extrator.integracao.constantes.ConstantesApiDataExport;
import br.com.extrator.suporte.validacao.ConstantesEntidades;

class DataExportRequestBodyFactoryTest {

    @Test
    void deveAplicarFiltrosExtrasEmSearchNestedDeContasAPagar() {
        final DataExportRequestBodyFactory factory = new DataExportRequestBodyFactory(
            LoggerFactory.getLogger(DataExportRequestBodyFactoryTest.class),
            DataExportTimeWindowSupport.createConfigured()
        );
        final String corpo = factory.construirCorpoRequisicao(
            "accounting_debits",
            "issue_date",
            Instant.parse("2026-04-01T03:00:00Z"),
            Instant.parse("2026-04-02T02:59:59Z"),
            1,
            ConstantesApiDataExport.obterConfiguracao(ConstantesEntidades.CONTAS_A_PAGAR),
            Map.of("created_at", "2026-04-01 00:00 - 2026-04-01 23:59")
        );

        assertTrue(corpo.contains("\"issue_date\""));
        assertTrue(corpo.contains("\"created_at\":\"2026-04-01 00:00 - 2026-04-01 23:59\""));
    }

    @Test
    void naoDeveInjetarCreatedAtVazioQuandoNaoHouverFiltroExtra() {
        final DataExportRequestBodyFactory factory = new DataExportRequestBodyFactory(
            LoggerFactory.getLogger(DataExportRequestBodyFactoryTest.class),
            DataExportTimeWindowSupport.createConfigured()
        );
        final String corpo = factory.construirCorpoRequisicao(
            "accounting_debits",
            "issue_date",
            Instant.parse("2026-04-01T03:00:00Z"),
            Instant.parse("2026-04-02T02:59:59Z"),
            1,
            ConstantesApiDataExport.obterConfiguracao(ConstantesEntidades.CONTAS_A_PAGAR)
        );

        assertFalse(corpo.contains("\"created_at\":\"\""));
    }
}
