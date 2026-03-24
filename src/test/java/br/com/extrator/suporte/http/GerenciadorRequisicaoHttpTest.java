package br.com.extrator.suporte.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

class GerenciadorRequisicaoHttpTest {

    @Test
    void deveAbortarSocketPenduradoDentroDoTimeoutConfigurado() throws Exception {
        final GerenciadorRequisicaoHttp gerenciador = GerenciadorRequisicaoHttp.getInstance();
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            final CountDownLatch conexaoAceita = new CountDownLatch(1);
            final CountDownLatch liberarSocket = new CountDownLatch(1);
            final Thread servidor = new Thread(() -> {
                try (Socket ignored = serverSocket.accept()) {
                    conexaoAceita.countDown();
                    liberarSocket.await(5, TimeUnit.SECONDS);
                } catch (final Exception ignored) {
                    // no-op
                }
            });
            servidor.setDaemon(true);
            servidor.start();

            final HttpClient cliente = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(200))
                .build();
            final HttpRequest requisicao = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + serverSocket.getLocalPort() + "/hang"))
                .timeout(Duration.ofMillis(150))
                .GET()
                .build();

            final long inicioMs = System.currentTimeMillis();
            final RuntimeException erro = assertThrows(
                RuntimeException.class,
                () -> gerenciador.executarRequisicaoEstrita(cliente, requisicao, "diagnostico-hang")
            );
            final long duracaoMs = System.currentTimeMillis() - inicioMs;

            assertTrue(conexaoAceita.await(1, TimeUnit.SECONDS), "Servidor de teste deve aceitar a conexao");
            assertTrue(duracaoMs < 1_500, "Timeout HTTP deve encerrar a chamada sem hang prolongado");
            assertInstanceOf(HttpTimeoutException.class, causaRaiz(erro));
            liberarSocket.countDown();
        }
    }

    @Test
    void deveRetentarFalhaIntermitenteERecuperarNaTentativaSeguinte() throws Exception {
        final GerenciadorRequisicaoHttp gerenciador = GerenciadorRequisicaoHttp.getInstance();
        final AtomicInteger chamadas = new AtomicInteger();
        final HttpServer servidor = HttpServer.create(new InetSocketAddress(0), 0);
        servidor.createContext("/retry", exchange -> {
            final int tentativa = chamadas.incrementAndGet();
            final byte[] corpo = (tentativa == 1 ? "temporario" : "ok").getBytes(java.nio.charset.StandardCharsets.UTF_8);
            final int status = tentativa == 1 ? 503 : 200;
            exchange.sendResponseHeaders(status, corpo.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(corpo);
            }
        });
        servidor.start();
        try {
            final HttpClient cliente = HttpClient.newHttpClient();
            final HttpRequest requisicao = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + servidor.getAddress().getPort() + "/retry"))
                .timeout(Duration.ofSeconds(1))
                .GET()
                .build();

            final HttpResponse<String> resposta = gerenciador.executarRequisicao(cliente, requisicao, "diagnostico-retry");

            assertEquals(200, resposta.statusCode());
            assertEquals("ok", resposta.body());
            assertEquals(2, chamadas.get(), "Falha intermitente deve ser recuperada com retry limitado");
        } finally {
            servidor.stop(0);
        }
    }

    private Throwable causaRaiz(final Throwable erro) {
        Throwable atual = erro;
        while (atual.getCause() != null && atual.getCause() != atual) {
            atual = atual.getCause();
        }
        return atual;
    }
}
