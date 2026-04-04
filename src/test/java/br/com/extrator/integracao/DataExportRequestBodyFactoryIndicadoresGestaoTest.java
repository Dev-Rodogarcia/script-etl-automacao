package br.com.extrator.integracao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import br.com.extrator.integracao.constantes.ConstantesApiDataExport.ConfiguracaoEntidade;
import br.com.extrator.suporte.mapeamento.MapperUtil;

class DataExportRequestBodyFactoryIndicadoresGestaoTest {

    @Test
    void deveConstruirCorpoCompativelComInventarioUsandoStartedAt() throws Exception {
        final DataExportTimeWindowSupport support = new DataExportTimeWindowSupport(ZoneId.of("America/Sao_Paulo"));
        final DataExportRequestBodyFactory factory = new DataExportRequestBodyFactory(
            LoggerFactory.getLogger(DataExportRequestBodyFactoryIndicadoresGestaoTest.class),
            support
        );
        final ConfiguracaoEntidade config = new ConfiguracaoEntidade(
            10633,
            "started_at",
            "check_in_orders",
            "100",
            Duration.ofSeconds(60),
            "sequence_code asc",
            false
        );

        final String corpo = factory.construirCorpoRequisicao(
            "check_in_orders",
            "started_at",
            support.inicioDoDia(LocalDate.of(2026, 3, 1)),
            support.fimDoDia(LocalDate.of(2026, 3, 31)),
            1,
            config
        );

        final JsonNode raiz = MapperUtil.sharedJson().readTree(corpo);
        assertEquals("2026-03-01 - 2026-03-31", raiz.path("search").path("check_in_orders").path("started_at").asText());
        assertEquals("100", raiz.path("per").asText());
        assertEquals("sequence_code asc", raiz.path("order_by").asText());
    }

    @Test
    void deveConstruirCorpoCompativelComSinistrosUsandoOpeningAtDate() throws Exception {
        final DataExportTimeWindowSupport support = new DataExportTimeWindowSupport(ZoneId.of("America/Sao_Paulo"));
        final DataExportRequestBodyFactory factory = new DataExportRequestBodyFactory(
            LoggerFactory.getLogger(DataExportRequestBodyFactoryIndicadoresGestaoTest.class),
            support
        );
        final ConfiguracaoEntidade config = new ConfiguracaoEntidade(
            6392,
            "opening_at_date",
            "insurance_claims",
            "100",
            Duration.ofSeconds(60),
            "sequence_code asc",
            false
        );

        final String corpo = factory.construirCorpoRequisicao(
            "insurance_claims",
            "opening_at_date",
            support.inicioDoDia(LocalDate.of(2026, 3, 1)),
            support.fimDoDia(LocalDate.of(2026, 3, 31)),
            2,
            config
        );

        final JsonNode raiz = MapperUtil.sharedJson().readTree(corpo);
        assertEquals("2026-03-01 - 2026-03-31", raiz.path("search").path("insurance_claims").path("opening_at_date").asText());
        assertEquals("2", raiz.path("page").asText());
    }
}
