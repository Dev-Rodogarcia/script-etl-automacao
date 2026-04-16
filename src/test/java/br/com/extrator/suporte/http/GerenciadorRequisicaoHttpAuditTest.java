package br.com.extrator.suporte.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

class GerenciadorRequisicaoHttpAuditTest {

    @Test
    void falhaLocalizadaNaoDeveAbrirCircuitoParaEntidadeSaudavel() throws Exception {
        final AtomicInteger chamadasFalhas = new AtomicInteger();
        final AtomicInteger chamadasSaudaveis = new AtomicInteger();
        final HttpServer servidor = HttpServer.create(new InetSocketAddress(0), 0);
        servidor.createContext("/falha", exchange -> {
            exchange.getRequestBody().readAllBytes();
            chamadasFalhas.incrementAndGet();
            final byte[] corpo = "erro".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(503, corpo.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(corpo);
            }
        });
        servidor.createContext("/saudavel", exchange -> {
            exchange.getRequestBody().readAllBytes();
            chamadasSaudaveis.incrementAndGet();
            final byte[] corpo = "ok".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, corpo.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(corpo);
            }
        });
        servidor.start();

        try {
            final GerenciadorRequisicaoHttp gerenciador = novoGerenciador();
            final HttpClient cliente = HttpClient.newHttpClient();
            final String baseUrl = "http://127.0.0.1:" + servidor.getAddress().getPort();
            final HttpRequest requisicaoFalha = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/falha"))
                .timeout(Duration.ofSeconds(1))
                .GET()
                .build();
            final HttpRequest requisicaoSaudavel = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/saudavel"))
                .timeout(Duration.ofSeconds(1))
                .GET()
                .build();

            for (int tentativa = 0; tentativa < 5; tentativa++) {
                org.junit.jupiter.api.Assertions.assertThrows(
                    RuntimeException.class,
                    () -> gerenciador.executarRequisicao(cliente, requisicaoFalha, "entidade-a")
                );
            }

            final HttpResponse<String> respostaSaudavel =
                gerenciador.executarRequisicao(cliente, requisicaoSaudavel, "entidade-b");

            assertEquals(200, respostaSaudavel.statusCode());
            assertEquals("ok", respostaSaudavel.body());
            assertEquals(1, chamadasSaudaveis.get(), "A entidade saudavel deve continuar acessivel com breaker por escopo.");
            assertTrue(chamadasFalhas.get() >= 5, "As falhas devem saturar apenas o escopo problemático.");
        } finally {
            servidor.stop(0);
        }
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
}
