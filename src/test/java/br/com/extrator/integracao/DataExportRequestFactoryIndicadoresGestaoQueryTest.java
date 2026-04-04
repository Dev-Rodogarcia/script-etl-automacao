package br.com.extrator.integracao;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.http.HttpRequest;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.junit.jupiter.api.Test;

class DataExportRequestFactoryIndicadoresGestaoQueryTest {

    @Test
    void deveCodificarManifestosEmModoGetComQueryString() {
        final DataExportRequestFactory factory = new DataExportRequestFactory("token-teste");
        final String corpoJson = """
            {
              "search": {
                "manifests": {
                  "service_date": "2026-03-01 - 2026-03-10"
                }
              },
              "page": "1",
              "per": "100",
              "order_by": "sequence_code asc"
            }
            """;

        final HttpRequest request = factory.construirRequisicao(
            "https://exemplo.test/api/analytics/reports/6399/data",
            corpoJson,
            Duration.ofSeconds(30),
            "GET",
            "application/json",
            true
        );

        final String query = URLDecoder.decode(request.uri().getRawQuery(), StandardCharsets.UTF_8);
        assertTrue(query.contains("search[manifests][service_date]=2026-03-01 - 2026-03-10"));
        assertTrue(query.contains("page=1"));
        assertTrue(query.contains("per=100"));
        assertTrue(query.contains("order_by=sequence_code asc"));
    }

    @Test
    void devePreservarMultiplosFiltrosDeDataNoModoGetComQueryString() {
        final DataExportRequestFactory factory = new DataExportRequestFactory("token-teste");
        final String corpoJson = """
            {
              "search": {
                "quotes": {
                  "requested_at": "2026-03-01 - 2026-03-10",
                  "updated_at": "2026-03-01 - 2026-03-10"
                }
              },
              "page": "1",
              "per": "100",
              "order_by": "sequence_code asc"
            }
            """;

        final HttpRequest request = factory.construirRequisicao(
            "https://exemplo.test/api/analytics/reports/6906/data",
            corpoJson,
            Duration.ofSeconds(30),
            "GET",
            "application/json",
            true
        );

        final String query = URLDecoder.decode(request.uri().getRawQuery(), StandardCharsets.UTF_8);
        assertTrue(query.contains("search[quotes][requested_at]=2026-03-01 - 2026-03-10"));
        assertTrue(query.contains("search[quotes][updated_at]=2026-03-01 - 2026-03-10"));
    }
}
