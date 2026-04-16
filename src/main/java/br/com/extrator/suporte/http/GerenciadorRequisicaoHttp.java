package br.com.extrator.suporte.http;

/* ==[DOC-FILE]===============================================================
Arquivo : src/main/java/br/com/extrator/suporte/http/GerenciadorRequisicaoHttp.java
Classe  : GerenciadorRequisicaoHttp (class)
Pacote  : br.com.extrator.suporte.http
Modulo  : Suporte - HTTP
Papel   : [DESC PENDENTE]
Conecta com: Sem dependencia interna
Fluxo geral:
1) [PENDENTE]
Estrutura interna:
Metodos: [PENDENTE]
Atributos: [PENDENTE]
[DOC-FILE-END]============================================================== */


import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Instant;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.extrator.suporte.configuracao.ConfigApi;

/**
 * Gerenciador centralizado para requests HTTP com throttling global, retry,
 * backoff exponencial e circuit breaker.
 */
public class GerenciadorRequisicaoHttp {
    private static final Logger logger = LoggerFactory.getLogger(GerenciadorRequisicaoHttp.class);
    private static final long DELAY_HTTP_429_MS = 2000L;

    private static final class CircuitBreakerState {
        private static final int FAILURE_THRESHOLD = 10;
        private static final long RESET_TIMEOUT_MS = 60_000L;

        private final AtomicInteger failureCount = new AtomicInteger(0);
        private final AtomicLong lastFailureTime = new AtomicLong(0);
        private final AtomicBoolean isOpen = new AtomicBoolean(false);

        boolean canExecute(final String escopo) {
            if (!isOpen.get()) {
                return true;
            }

            final long timeSinceFailure = System.currentTimeMillis() - lastFailureTime.get();
            if (timeSinceFailure >= RESET_TIMEOUT_MS) {
                logger.warn("Circuit breaker [{}] tentando HALF-OPEN state apos {}s", escopo, timeSinceFailure / 1000);
                return true;
            }
            return false;
        }

        void recordSuccess(final String escopo) {
            final int previousFailures = failureCount.getAndSet(0);
            if (isOpen.getAndSet(false) || previousFailures > 0) {
                logger.info("Circuit breaker [{}] FECHADO apos sucesso (havia {} falhas)", escopo, previousFailures);
            }
        }

        void recordFailure(final String escopo) {
            final int failures = failureCount.incrementAndGet();
            lastFailureTime.set(System.currentTimeMillis());

            if (failures >= FAILURE_THRESHOLD && !isOpen.get()) {
                isOpen.set(true);
                logger.error(
                    "CIRCUIT BREAKER [{}] ABERTO apos {} falhas consecutivas. Requests bloqueadas por {}s",
                    escopo,
                    failures,
                    RESET_TIMEOUT_MS / 1000
                );
            } else if (failures < FAILURE_THRESHOLD) {
                logger.warn("Falha {}/{} em [{}] - Circuit ainda fechado", failures, FAILURE_THRESHOLD, escopo);
            }
        }

        long getTimeUntilReset() {
            if (!isOpen.get()) {
                return 0;
            }
            final long elapsed = System.currentTimeMillis() - lastFailureTime.get();
            final long remaining = RESET_TIMEOUT_MS - elapsed;
            return Math.max(0, remaining / 1000);
        }
    }

    private static final class Holder {
        private static final GerenciadorRequisicaoHttp INSTANCE = new GerenciadorRequisicaoHttp();
    }

    private final ConcurrentHashMap<String, CircuitBreakerState> circuitBreakers = new ConcurrentHashMap<>();
    private final ReentrantLock lockThrottling = new ReentrantLock(true);
    private final AtomicLong ultimaRequisicaoTimestamp = new AtomicLong(0);

    private final int maxTentativas;
    private final long delayBaseMs;
    private final double multiplicador;
    private final long throttlingMinimoMs;

    public static GerenciadorRequisicaoHttp getInstance() {
        return Holder.INSTANCE;
    }

    private GerenciadorRequisicaoHttp() {
        this.maxTentativas = ConfigApi.obterMaxTentativasRetry();
        this.delayBaseMs = ConfigApi.obterDelayBaseRetry();
        this.multiplicador = ConfigApi.obterMultiplicadorRetry();
        this.throttlingMinimoMs = ConfigApi.obterThrottlingMinimo();

        logger.info(
            "GerenciadorRequisicaoHttp inicializado - maxTentativas={}, delayBaseMs={}, multiplicador={}, throttlingMinimoMs={}",
            maxTentativas,
            delayBaseMs,
            multiplicador,
            throttlingMinimoMs
        );
    }

    private void aguardarComTratamentoInterrupcao(final long delayMs, final String contexto) {
        try {
            Thread.sleep(delayMs);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread interrompida durante " + contexto, e);
        }
    }

    private boolean deveRetentar(final int statusCode) {
        if (statusCode == 404 || statusCode == 401 || statusCode == 403) {
            return false;
        }
        if (statusCode == 429) {
            return true;
        }
        if (statusCode == 500 || statusCode == 502 || statusCode == 503) {
            return true;
        }
        if (statusCode >= 500 && statusCode <= 599) {
            return true;
        }
        if (statusCode >= 400 && statusCode <= 499) {
            return false;
        }
        return false;
    }

    public HttpResponse<String> executarRequisicao(final HttpClient cliente,
                                                   final HttpRequest requisicao,
                                                   final String tipoEntidade) {
        return executarRequisicaoInterna(
            cliente,
            requisicao,
            tipoEntidade,
            HttpResponse.BodyHandlers.ofString(),
            null,
            true
        );
    }

    public HttpResponse<String> executarRequisicaoEstrita(final HttpClient cliente,
                                                          final HttpRequest requisicao,
                                                          final String tipoEntidade) {
        return executarRequisicaoEstrita(cliente, requisicao, tipoEntidade, Collections.emptySet());
    }

    public HttpResponse<String> executarRequisicaoEstrita(final HttpClient cliente,
                                                          final HttpRequest requisicao,
                                                          final String tipoEntidade,
                                                          final Set<Integer> statusPermitidos) {
        final HttpResponse<String> resposta = executarRequisicao(cliente, requisicao, tipoEntidade);
        validarRespostaEstrita(resposta, tipoEntidade, statusPermitidos);
        return resposta;
    }

    public HttpResponse<String> executarRequisicaoComCharset(final HttpClient cliente,
                                                             final HttpRequest requisicao,
                                                             final String tipoEntidade,
                                                             final Charset charset) {
        return executarRequisicaoInterna(
            cliente,
            requisicao,
            tipoEntidade,
            HttpResponse.BodyHandlers.ofString(charset),
            charset,
            false
        );
    }

    public HttpResponse<String> executarRequisicaoComCharsetEstrita(final HttpClient cliente,
                                                                    final HttpRequest requisicao,
                                                                    final String tipoEntidade,
                                                                    final Charset charset) {
        return executarRequisicaoComCharsetEstrita(
            cliente,
            requisicao,
            tipoEntidade,
            charset,
            Collections.emptySet()
        );
    }

    public HttpResponse<String> executarRequisicaoComCharsetEstrita(final HttpClient cliente,
                                                                    final HttpRequest requisicao,
                                                                    final String tipoEntidade,
                                                                    final Charset charset,
                                                                    final Set<Integer> statusPermitidos) {
        final HttpResponse<String> resposta = executarRequisicaoComCharset(cliente, requisicao, tipoEntidade, charset);
        validarRespostaEstrita(resposta, tipoEntidade, statusPermitidos);
        return resposta;
    }

    private HttpResponse<String> executarRequisicaoInterna(final HttpClient cliente,
                                                           final HttpRequest requisicao,
                                                           final String tipoEntidade,
                                                           final HttpResponse.BodyHandler<String> bodyHandler,
                                                           final Charset charset,
                                                           final boolean detalheProtecao) {
        validarCircuitBreaker(tipoEntidade, detalheProtecao);
        aplicarThrottling();
        Exception ultimaFalha = null;
        final String escopoBreaker = normalizarEscopoCircuitBreaker(tipoEntidade);

        for (int tentativa = 1; tentativa <= maxTentativas; tentativa++) {
            try {
                registrarTentativa(tipoEntidade, tentativa, charset);
                final HttpResponse<String> resposta = executarComTimeout(cliente, requisicao, bodyHandler, tipoEntidade);
                final int statusCode = resposta.statusCode();

                if (statusCode >= 200 && statusCode < 300) {
                    obterCircuitBreaker(escopoBreaker).recordSuccess(escopoBreaker);
                    logger.debug("Request bem-sucedida para {} - status={}", descreverTipoEntidade(tipoEntidade), statusCode);
                    return resposta;
                }

                if (!deveRetentar(statusCode)) {
                    registrarErroDefinitivo(tipoEntidade, statusCode, resposta.body());
                    return resposta;
                }

                tratarStatusRetentavel(escopoBreaker, tipoEntidade, tentativa, resposta);
            } catch (final HttpTimeoutException e) {
                ultimaFalha = e;
                tratarTimeout(escopoBreaker, tipoEntidade, tentativa, e);
            } catch (final IOException e) {
                ultimaFalha = e;
                tratarIOException(escopoBreaker, tipoEntidade, tentativa, e);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Thread interrompida durante requisicao", e);
            }
        }

        final String mensagemFalha = String.format(
            "Request para %s falhou apos %d tentativas. Verifique conectividade e configuracoes da API.",
            descreverTipoEntidade(tipoEntidade),
            maxTentativas
        );
        logger.error(mensagemFalha);
        throw new RuntimeException(mensagemFalha, ultimaFalha);
    }

    private HttpResponse<String> executarComTimeout(final HttpClient cliente,
                                                    final HttpRequest requisicao,
                                                    final HttpResponse.BodyHandler<String> bodyHandler,
                                                    final String tipoEntidade)
        throws IOException, InterruptedException {
        final Duration timeout = requisicao.timeout().orElseGet(ConfigApi::obterTimeoutApiRest);
        final long timeoutMs = Math.max(1L, timeout.toMillis());
        final Future<HttpResponse<String>> future = cliente.sendAsync(requisicao, bodyHandler);
        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (final TimeoutException e) {
            future.cancel(true);
            throw new HttpTimeoutException(
                "Timeout aguardando resposta de " + descreverTipoEntidade(tipoEntidade) + " apos " + timeoutMs + " ms"
            );
        } catch (final ExecutionException e) {
            future.cancel(true);
            final Throwable causa = e.getCause() == null ? e : e.getCause();
            if (causa instanceof HttpTimeoutException httpTimeoutException) {
                throw httpTimeoutException;
            }
            if (causa instanceof IOException ioException) {
                throw ioException;
            }
            if (causa instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new RuntimeException(
                "Falha inesperada durante requisicao HTTP para " + descreverTipoEntidade(tipoEntidade),
                causa
            );
        }
    }

    private void validarCircuitBreaker(final String tipoEntidade, final boolean detalheProtecao) {
        final String escopoBreaker = normalizarEscopoCircuitBreaker(tipoEntidade);
        final CircuitBreakerState circuitBreaker = obterCircuitBreaker(escopoBreaker);
        if (!circuitBreaker.canExecute(escopoBreaker)) {
            final long timeUntilReset = circuitBreaker.getTimeUntilReset();
            final String mensagem;
            if (detalheProtecao) {
                mensagem = String.format(
                    "CIRCUIT BREAKER ABERTO - API indisponivel para %s. Sistema em protecao. Aguarde %d segundos para nova tentativa.",
                    tipoEntidade != null ? tipoEntidade : "requisicao",
                    timeUntilReset
                );
            } else {
                mensagem = String.format(
                    "CIRCUIT BREAKER ABERTO - API indisponivel para %s. Aguarde %d segundos.",
                    tipoEntidade != null ? tipoEntidade : "requisicao",
                    timeUntilReset
                );
            }
            logger.error(mensagem);
            throw new RuntimeException(mensagem);
        }
    }

    private void registrarTentativa(final String tipoEntidade, final int tentativa, final Charset charset) {
        if (charset == null) {
            logger.debug(
                "Executando requisicao para {} - tentativa {}/{}",
                descreverTipoEntidade(tipoEntidade),
                tentativa,
                maxTentativas
            );
            return;
        }

        logger.debug(
            "Executando requisicao (charset={}) para {} - tentativa {}/{}",
            charset.displayName(),
            descreverTipoEntidade(tipoEntidade),
            tentativa,
            maxTentativas
        );
    }

    private void registrarErroDefinitivo(final String tipoEntidade, final int statusCode, final String corpoResposta) {
        if (statusCode == 404) {
            logger.debug(
                "HTTP 404 para {} (esperado). Resposta: {}",
                descreverTipoEntidade(tipoEntidade),
                resumirCorpo(corpoResposta)
            );
            return;
        }

        logger.error(
            "Erro definitivo na requisicao para {} - HTTP {} (nao sera retentado). Resposta: {}",
            descreverTipoEntidade(tipoEntidade),
            statusCode,
            resumirCorpo(corpoResposta)
        );
    }

    private void tratarStatusRetentavel(final String escopoBreaker,
                                        final String tipoEntidade,
                                        final int tentativa,
                                        final HttpResponse<String> resposta) {
        final int statusCode = resposta.statusCode();
        final String corpoResposta = resposta.body();
        obterCircuitBreaker(escopoBreaker).recordFailure(escopoBreaker);

        if (statusCode == 429) {
            final OptionalLong retryAfterMs = extrairRetryAfterMillis(resposta);
            final long delayMs = calcularDelayRateLimit(tentativa, retryAfterMs);
            logger.warn(
                "Rate limit atingido (HTTP 429) para {} - tentativa {}/{}. Aguardando {} ms{}...",
                descreverTipoEntidade(tipoEntidade),
                tentativa,
                maxTentativas,
                delayMs,
                retryAfterMs.isPresent() ? " (Retry-After considerado)" : ""
            );
            if (tentativa < maxTentativas) {
                aguardarComTratamentoInterrupcao(delayMs, "retry apos HTTP 429");
            }
            return;
        }

        final String respostaResumida = resumirCorpo(corpoResposta);
        if (tentativa < maxTentativas) {
            logger.warn(
                "Erro de servidor (HTTP {}) para {} - tentativa {}/{}. Resposta: {}",
                statusCode,
                descreverTipoEntidade(tipoEntidade),
                tentativa,
                maxTentativas,
                respostaResumida
            );
        } else {
            logger.error(
                "Erro de servidor (HTTP {}) para {} - tentativa final {}/{}. Resposta: {}",
                statusCode,
                descreverTipoEntidade(tipoEntidade),
                tentativa,
                maxTentativas,
                respostaResumida
            );
        }

        aguardarComBackoffSeNecessario(tentativa, "backoff exponencial");
    }

    private void tratarTimeout(final String escopoBreaker,
                               final String tipoEntidade,
                               final int tentativa,
                               final HttpTimeoutException e) {
        throwIfInterrupted("timeout HTTP para " + descreverTipoEntidade(tipoEntidade));
        obterCircuitBreaker(escopoBreaker).recordFailure(escopoBreaker);
        if (tentativa < maxTentativas) {
            logger.warn(
                "Timeout na requisicao para {} - tentativa {}/{}",
                descreverTipoEntidade(tipoEntidade),
                tentativa,
                maxTentativas
            );
        } else {
            logger.error(
                "Timeout na requisicao para {} - tentativa final {}/{}",
                descreverTipoEntidade(tipoEntidade),
                tentativa,
                maxTentativas,
                e
            );
        }

        aguardarComBackoffSeNecessario(tentativa, "retry apos timeout");
    }

    private void tratarIOException(final String escopoBreaker,
                                   final String tipoEntidade,
                                   final int tentativa,
                                   final IOException e) {
        throwIfInterrupted("IOException para " + descreverTipoEntidade(tipoEntidade));
        obterCircuitBreaker(escopoBreaker).recordFailure(escopoBreaker);
        if (tentativa < maxTentativas) {
            logger.warn(
                "IOException na requisicao para {} - tentativa {}/{}: {}",
                descreverTipoEntidade(tipoEntidade),
                tentativa,
                maxTentativas,
                e.getMessage()
            );
        } else {
            logger.error(
                "IOException na requisicao para {} - tentativa final {}/{}: {}",
                descreverTipoEntidade(tipoEntidade),
                tentativa,
                maxTentativas,
                e.getMessage()
            );
        }

        aguardarComBackoffSeNecessario(tentativa, "retry apos IOException");
    }

    private void aplicarThrottling() {
        lockThrottling.lock();
        try {
            throwIfInterrupted("throttling global");
            final long agora = System.currentTimeMillis();
            final long ultimaRequisicao = ultimaRequisicaoTimestamp.get();
            final long tempoDecorrido = agora - ultimaRequisicao;

            if (tempoDecorrido < throttlingMinimoMs) {
                final long tempoEspera = throttlingMinimoMs - tempoDecorrido;
                logger.debug(
                    "Throttling global aplicado - espera={}ms, limite={}ms, decorrido={}ms",
                    tempoEspera,
                    throttlingMinimoMs,
                    tempoDecorrido
                );
                aguardarComTratamentoInterrupcao(tempoEspera, "throttling global");
            } else {
                logger.debug("Throttling global OK - decorrido={}ms, limite={}ms", tempoDecorrido, throttlingMinimoMs);
            }

            ultimaRequisicaoTimestamp.set(System.currentTimeMillis());
        } finally {
            lockThrottling.unlock();
        }
    }

    private void aguardarComBackoffSeNecessario(final int tentativa, final String contexto) {
        if (tentativa < maxTentativas) {
            final long delayMs = calcularDelayBackoffExponencial(tentativa);
            logger.info("Aguardando {}ms antes da proxima tentativa ({})...", delayMs, contexto);
            aguardarComTratamentoInterrupcao(delayMs, contexto);
        }
    }

    private long calcularDelayBackoffExponencial(final int tentativa) {
        final double delay = delayBaseMs * Math.pow(multiplicador, tentativa - 1);
        return Math.round(delay);
    }

    private long calcularDelayRateLimit(final int tentativa, final OptionalLong retryAfterMs) {
        long delay = Math.max(DELAY_HTTP_429_MS, calcularDelayBackoffExponencial(tentativa));
        if (retryAfterMs != null && retryAfterMs.isPresent()) {
            delay = Math.max(delay, retryAfterMs.getAsLong());
        }
        return adicionarJitter(delay);
    }

    private long adicionarJitter(final long delayBase) {
        final long base = Math.max(0L, delayBase);
        if (base == 0L) {
            return 0L;
        }
        final long jitterMax = Math.max(250L, base / 4L);
        return base + ThreadLocalRandom.current().nextLong(0L, jitterMax + 1L);
    }

    private OptionalLong extrairRetryAfterMillis(final HttpResponse<?> resposta) {
        if (resposta == null) {
            return OptionalLong.empty();
        }
        final String retryAfter = resposta.headers().firstValue("Retry-After").orElse(null);
        if (retryAfter == null || retryAfter.isBlank()) {
            return OptionalLong.empty();
        }

        final String valor = retryAfter.trim();
        try {
            final long segundos = Long.parseLong(valor);
            return OptionalLong.of(Math.max(0L, segundos) * 1_000L);
        } catch (final NumberFormatException ignored) {
            // Tenta parsear HTTP-date.
        }

        try {
            final long millis = Duration.between(Instant.now(), ZonedDateTime.parse(valor).toInstant()).toMillis();
            return OptionalLong.of(Math.max(0L, millis));
        } catch (final RuntimeException ignored) {
            logger.debug("Retry-After invalido para {}: {}", resposta.uri(), valor);
            return OptionalLong.empty();
        }
    }

    private void validarRespostaEstrita(final HttpResponse<String> resposta,
                                        final String tipoEntidade,
                                        final Set<Integer> statusPermitidos) {
        if (resposta == null) {
            final String mensagem = String.format(
                "Resposta HTTP nula para %s.",
                tipoEntidade != null ? tipoEntidade : "API"
            );
            logger.error(mensagem);
            throw new IllegalStateException(mensagem);
        }

        final int statusCode = resposta.statusCode();
        if (statusCode >= 200 && statusCode < 300) {
            return;
        }

        final Set<Integer> permitidos = statusPermitidos == null ? Collections.emptySet() : statusPermitidos;
        if (permitidos.contains(statusCode)) {
            logger.debug("Status HTTP {} permitido explicitamente para {}.", statusCode, tipoEntidade);
            return;
        }

        final String mensagem = String.format(
            "Erro HTTP nao-sucesso para %s: status=%d, resposta=%s",
            tipoEntidade != null ? tipoEntidade : "API",
            statusCode,
            resumirResposta(resposta)
        );
        logger.error(mensagem);
        throw new IllegalStateException(mensagem);
    }

    private String resumirResposta(final HttpResponse<String> resposta) {
        if (resposta == null || resposta.body() == null) {
            return "<sem-corpo>";
        }
        return resumirCorpo(resposta.body());
    }

    private String resumirCorpo(final String corpo) {
        if (corpo == null) {
            return "<sem-corpo>";
        }
        return corpo.length() > 200 ? corpo.substring(0, 200) + "..." : corpo;
    }

    private String descreverTipoEntidade(final String tipoEntidade) {
        return tipoEntidade != null ? tipoEntidade : "API";
    }

    private CircuitBreakerState obterCircuitBreaker(final String escopoBreaker) {
        return circuitBreakers.computeIfAbsent(escopoBreaker, ignored -> new CircuitBreakerState());
    }

    private String normalizarEscopoCircuitBreaker(final String tipoEntidade) {
        return (tipoEntidade == null || tipoEntidade.isBlank()) ? "API" : tipoEntidade.trim();
    }

    private void throwIfInterrupted(final String contexto) {
        if (!Thread.currentThread().isInterrupted()) {
            return;
        }
        throw new RuntimeException("Thread interrompida durante " + contexto);
    }
}
