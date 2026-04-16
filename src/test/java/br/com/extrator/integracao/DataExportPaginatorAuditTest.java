package br.com.extrator.integracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sun.net.httpserver.HttpServer;

import br.com.extrator.integracao.constantes.ConstantesApiDataExport.ConfiguracaoEntidade;
import br.com.extrator.persistencia.entidade.PageAuditEntity;
import br.com.extrator.persistencia.repositorio.PageAuditRepository;
import br.com.extrator.suporte.http.GerenciadorRequisicaoHttp;

class DataExportPaginatorAuditTest {

    @Test
    void paginaVaziaAposPaginaParcialSegueComoFimNatural() throws Exception {
        final AtomicInteger chamadas = new AtomicInteger();
        final HttpServer servidor = HttpServer.create(new InetSocketAddress(0), 0);
        servidor.createContext("/api/analytics/reports/8656/data", exchange -> {
            exchange.getRequestBody().readAllBytes();
            final int chamada = chamadas.incrementAndGet();
            final String payload = chamada == 1
                ? "{\"data\":[{\"sequence_number\":1},{\"sequence_number\":2}]}"
                : "{\"data\":[]}";
            final byte[] corpo = payload.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, corpo.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(corpo);
            }
        });
        servidor.start();

        try {
            final ResultadoExtracao<Map<String, Object>> resultado = criarPaginator(servidor).buscarDadosGenericos(
                "exec-audit-dataexport-natural",
                8656,
                "freights",
                "service_at",
                new TypeReference<List<Map<String, Object>>>() {
                },
                Instant.parse("2026-04-10T03:00:00Z"),
                Instant.parse("2026-04-11T03:00:00Z"),
                criarConfigPadrao(),
                false,
                Map.of()
            );

            assertTrue(resultado.isCompleto());
            assertEquals(1, resultado.getPaginasProcessadas());
            assertEquals(2, resultado.getDados().size());
            assertEquals(2, chamadas.get(), "Pagina parcial seguida de vazia deve encerrar sem retentativa extra.");
        } finally {
            servidor.stop(0);
        }
    }

    @Test
    void paginaVaziaAposPaginaCheiaViraIncompletoAposRetentativa() throws Exception {
        final AtomicInteger chamadas = new AtomicInteger();
        final HttpServer servidor = HttpServer.create(new InetSocketAddress(0), 0);
        servidor.createContext("/api/analytics/reports/8656/data", exchange -> {
            exchange.getRequestBody().readAllBytes();
            final int chamada = chamadas.incrementAndGet();
            final String payload = chamada == 1
                ? payloadComIntervalo(1, 100)
                : "{\"data\":[]}";
            final byte[] corpo = payload.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, corpo.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(corpo);
            }
        });
        servidor.start();

        try {
            final ResultadoExtracao<Map<String, Object>> resultado = criarPaginator(servidor).buscarDadosGenericos(
                "exec-audit-dataexport-gap",
                8656,
                "freights",
                "service_at",
                new TypeReference<List<Map<String, Object>>>() {
                },
                Instant.parse("2026-04-10T03:00:00Z"),
                Instant.parse("2026-04-11T03:00:00Z"),
                criarConfigPadrao(),
                false,
                Map.of()
            );

            assertFalse(resultado.isCompleto());
            assertEquals(ResultadoExtracao.MotivoInterrupcao.PAGINA_VAZIA_INESPERADA.getCodigo(), resultado.getMotivoInterrupcao());
            assertEquals(1, resultado.getPaginasProcessadas());
            assertEquals(100, resultado.getDados().size());
            assertEquals(3, chamadas.get(), "Pagina vazia inesperada deve ser repetida uma vez antes do incompleto.");
        } finally {
            servidor.stop(0);
        }
    }

    @Test
    void paginaVaziaAposPaginaAcimaDoPerSegueComoFimNaturalSemRetentativa() throws Exception {
        final AtomicInteger chamadas = new AtomicInteger();
        final HttpServer servidor = HttpServer.create(new InetSocketAddress(0), 0);
        servidor.createContext("/api/analytics/reports/8656/data", exchange -> {
            exchange.getRequestBody().readAllBytes();
            final int chamada = chamadas.incrementAndGet();
            final String payload = chamada == 1
                ? payloadComIntervalo(1, 125)
                : "{\"data\":[]}";
            final byte[] corpo = payload.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, corpo.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(corpo);
            }
        });
        servidor.start();

        try {
            final ResultadoExtracao<Map<String, Object>> resultado = criarPaginator(servidor).buscarDadosGenericos(
                "exec-audit-dataexport-irregular-natural",
                8656,
                "freights",
                "service_at",
                new TypeReference<List<Map<String, Object>>>() {
                },
                Instant.parse("2026-04-10T03:00:00Z"),
                Instant.parse("2026-04-11T03:00:00Z"),
                criarConfigPadrao(),
                false,
                Map.of()
            );

            assertTrue(resultado.isCompleto());
            assertEquals(1, resultado.getPaginasProcessadas());
            assertEquals(125, resultado.getDados().size());
            assertEquals(2, chamadas.get(), "Paginacao irregular com pagina acima do per deve encerrar sem retentativa.");
        } finally {
            servidor.stop(0);
        }
    }

    @Test
    void paginaVaziaAposSequenciaIrregularTambemSegueComoFimNatural() throws Exception {
        final AtomicInteger chamadas = new AtomicInteger();
        final HttpServer servidor = HttpServer.create(new InetSocketAddress(0), 0);
        servidor.createContext("/api/analytics/reports/8656/data", exchange -> {
            exchange.getRequestBody().readAllBytes();
            final int chamada = chamadas.incrementAndGet();
            final String payload = switch (chamada) {
                case 1 -> payloadComIntervalo(1, 125);
                case 2 -> payloadComIntervalo(126, 225);
                default -> "{\"data\":[]}";
            };
            final byte[] corpo = payload.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, corpo.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(corpo);
            }
        });
        servidor.start();

        try {
            final ResultadoExtracao<Map<String, Object>> resultado = criarPaginator(servidor).buscarDadosGenericos(
                "exec-audit-dataexport-irregular-chain",
                8656,
                "freights",
                "service_at",
                new TypeReference<List<Map<String, Object>>>() {
                },
                Instant.parse("2026-04-10T03:00:00Z"),
                Instant.parse("2026-04-11T03:00:00Z"),
                criarConfigPadrao(),
                false,
                Map.of()
            );

            assertTrue(resultado.isCompleto());
            assertEquals(2, resultado.getPaginasProcessadas());
            assertEquals(225, resultado.getDados().size());
            assertEquals(3, chamadas.get(), "Sequencia irregular deve encerrar no vazio seguinte sem retentativa.");
        } finally {
            servidor.stop(0);
        }
    }

    @Test
    void paginaVaziaAposPaginaCheiaPodeSerConfirmadaComoFimNaturalViaContagemEsperada() throws Exception {
        final AtomicInteger chamadas = new AtomicInteger();
        final HttpServer servidor = HttpServer.create(new InetSocketAddress(0), 0);
        servidor.createContext("/api/analytics/reports/8656/data", exchange -> {
            exchange.getRequestBody().readAllBytes();
            final int chamada = chamadas.incrementAndGet();
            final String payload = chamada == 1
                ? payloadComIntervalo(1, 100)
                : "{\"data\":[]}";
            final byte[] corpo = payload.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, corpo.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(corpo);
            }
        });
        servidor.start();

        try {
            final ResultadoExtracao<Map<String, Object>> resultado = criarPaginator(
                servidor,
                (templateId, nomeTabela, campoData, dataInicio, dataFim, tipoAmigavel) -> 100
            ).buscarDadosGenericos(
                "exec-audit-dataexport-count-proof",
                8656,
                "freights",
                "service_at",
                new TypeReference<List<Map<String, Object>>>() {
                },
                Instant.parse("2026-04-10T03:00:00Z"),
                Instant.parse("2026-04-11T03:00:00Z"),
                criarConfigPadrao(),
                false,
                Map.of()
            );

            assertTrue(resultado.isCompleto());
            assertEquals(1, resultado.getPaginasProcessadas());
            assertEquals(100, resultado.getDados().size());
            assertEquals(3, chamadas.get(), "Caso ambiguo deve repetir a pagina uma vez antes de confirmar fim natural.");
        } finally {
            servidor.stop(0);
        }
    }

    private DataExportPaginator criarPaginator(final HttpServer servidor) {
        return criarPaginator(servidor, null);
    }

    private DataExportPaginator criarPaginator(final HttpServer servidor,
                                               final DataExportPaginator.ExpectedCountProbe expectedCountProbe) {
        final DataExportTimeWindowSupport timeWindowSupport =
            new DataExportTimeWindowSupport(ZoneId.of("America/Sao_Paulo"));
        final DataExportRequestBodyFactory requestBodyFactory =
            new DataExportRequestBodyFactory(LoggerFactory.getLogger(DataExportPaginatorAuditTest.class), timeWindowSupport);
        final DataExportPageAuditLogger pageAuditLogger =
            new DataExportPageAuditLogger(new PageAuditRepositoryNoOp());
        final DataExportHttpExecutor httpExecutor = new DataExportHttpExecutor(
            LoggerFactory.getLogger(DataExportPaginatorAuditTest.class),
            HttpClient.newHttpClient(),
            novoGerenciador(),
            new DataExportRequestFactory("token-auditoria"),
            "POST",
            "corpo",
            1L,
            1L,
            0.0d
        );
        final DataExportPaginationSupport paginationSupport = new DataExportPaginationSupport(
            LoggerFactory.getLogger(DataExportPaginatorAuditTest.class),
            5,
            Duration.ofMinutes(10),
            new HashMap<>(),
            new HashSet<>(),
            new HashMap<>()
        );
        return new DataExportPaginator(
            LoggerFactory.getLogger(DataExportPaginatorAuditTest.class),
            "http://127.0.0.1:" + servidor.getAddress().getPort(),
            requestBodyFactory,
            pageAuditLogger,
            httpExecutor,
            1,
            1,
            1,
            paginationSupport,
            timeWindowSupport,
            expectedCountProbe
        );
    }

    private ConfiguracaoEntidade criarConfigPadrao() {
        return new ConfiguracaoEntidade(
            8656,
            "service_at",
            "freights",
            "100",
            Duration.ofSeconds(2),
            "sequence_number asc",
            false
        );
    }

    private String payloadComIntervalo(final int inicio, final int fim) {
        final String registros = IntStream.rangeClosed(inicio, fim)
            .mapToObj(valor -> "{\"sequence_number\":" + valor + "}")
            .collect(Collectors.joining(","));
        return "{\"data\":[" + registros + "]}";
    }

    private GerenciadorRequisicaoHttp novoGerenciador() {
        try {
            final Constructor<GerenciadorRequisicaoHttp> constructor = GerenciadorRequisicaoHttp.class
                .getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Nao foi possivel instanciar GerenciadorRequisicaoHttp para o teste", e);
        }
    }

    private static final class PageAuditRepositoryNoOp extends PageAuditRepository {
        @Override
        public void inserir(final PageAuditEntity a) {
            // Mantem o teste isolado de banco: a auditoria de pagina nao faz parte do cenario validado aqui.
        }
    }
}
