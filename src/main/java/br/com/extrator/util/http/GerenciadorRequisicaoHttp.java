package br.com.extrator.util.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.extrator.util.ThreadUtil;
import br.com.extrator.util.configuracao.CarregadorConfig;

import java.io.IOException;
import java.nio.charset.Charset;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Gerenciador centralizado para requisições HTTP com throttling, retry e backoff exponencial.
 * Implementa as regras mandatórias da API:
 * - Rate Limit: 2 segundos entre requisições (GLOBAL - compartilhado entre todas as threads)
 * - Tratamento de HTTP 429: espera 2 segundos e retenta
 * - Tratamento de erros 5xx: backoff exponencial
 * - Circuit Breaker: Protege contra avalanche de requisições em falha total da API
 * 
 * SINGLETON THREAD-SAFE: Usa padrão Bill Pugh (Holder) para garantir:
 * - Lazy-loading (instância criada apenas quando necessário)
 * - Thread-safety sem synchronized
 * - Throttling GLOBAL (todas as threads respeitam o mesmo intervalo de 2s)
 * 
 * CORREÇÃO CRÍTICA #8: Circuit Breaker implementado para proteger contra falhas em cascata
 * 
 * @author Sistema de Extração ESL Cloud
 * @version 3.0 - Com Circuit Breaker
 */
public class GerenciadorRequisicaoHttp {
    private static final Logger logger = LoggerFactory.getLogger(GerenciadorRequisicaoHttp.class);
    
    // ========== CIRCUIT BREAKER (CORREÇÃO CRÍTICA #8) ==========
    private static class CircuitBreakerState {
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private final AtomicLong lastFailureTime = new AtomicLong(0);
        private final AtomicBoolean isOpen = new AtomicBoolean(false);
        
        // Threshold: 10 falhas consecutivas abrem o circuit breaker
        private static final int FAILURE_THRESHOLD = 10;
        // Timeout: Circuit breaker tenta reset após 60 segundos
        private static final long RESET_TIMEOUT_MS = 60_000L;
        
        /**
         * Verifica se o circuit breaker está aberto e se deve tentar reset.
         * @return true se pode executar requisição, false se circuit está aberto
         */
        boolean canExecute() {
            if (!isOpen.get()) {
                return true; // Circuit fechado - OK para executar
            }
            
            // Circuit aberto - verificar se passou o timeout para tentar reset
            long timeSinceFailure = System.currentTimeMillis() - lastFailureTime.get();
            if (timeSinceFailure >= RESET_TIMEOUT_MS) {
                logger.warn("🔄 Circuit breaker tentando HALF-OPEN state (reset após {}s)", 
                    timeSinceFailure / 1000);
                // Entra em half-open state - permite uma tentativa
                return true;
            }
            
            // Circuit ainda aberto - não pode executar
            return false;
        }
        
        /**
         * Registra sucesso - reseta contador de falhas e fecha circuit.
         */
        void recordSuccess() {
            int previousFailures = failureCount.getAndSet(0);
            if (isOpen.getAndSet(false) || previousFailures > 0) {
                logger.info("✅ Circuit breaker FECHADO após sucesso (havia {} falhas)", previousFailures);
            }
        }
        
        /**
         * Registra falha - incrementa contador e abre circuit se atingir threshold.
         */
        void recordFailure() {
            int failures = failureCount.incrementAndGet();
            lastFailureTime.set(System.currentTimeMillis());
            
            if (failures >= FAILURE_THRESHOLD && !isOpen.get()) {
                isOpen.set(true);
                logger.error("🚨 CIRCUIT BREAKER ABERTO após {} falhas consecutivas! " +
                    "Requisições bloqueadas por {}s", failures, RESET_TIMEOUT_MS / 1000);
            } else if (failures < FAILURE_THRESHOLD) {
                logger.warn("⚠️ Falha {}/{} - Circuit ainda fechado", failures, FAILURE_THRESHOLD);
            }
        }
        
        /**
         * Obtém tempo restante até reset (em segundos).
         */
        long getTimeUntilReset() {
            if (!isOpen.get()) {
                return 0;
            }
            long elapsed = System.currentTimeMillis() - lastFailureTime.get();
            long remaining = RESET_TIMEOUT_MS - elapsed;
            return Math.max(0, remaining / 1000);
        }
    }
    
    private final CircuitBreakerState circuitBreaker = new CircuitBreakerState();
    // ==========================================================
    
    // ========== SINGLETON (Bill Pugh Holder Pattern) ==========
    private static class Holder {
        private static final GerenciadorRequisicaoHttp INSTANCE = new GerenciadorRequisicaoHttp();
    }
    
    /**
     * Obtém a instância única do GerenciadorRequisicaoHttp.
     * Thread-safe e lazy-loaded (criado apenas na primeira chamada).
     * 
     * @return Instância singleton do gerenciador
     */
    public static GerenciadorRequisicaoHttp getInstance() {
        return Holder.INSTANCE;
    }
    // ==========================================================
    
    // Fair lock para garantir ordem FIFO no throttling (evita starvation)
    private final ReentrantLock lockThrottling = new ReentrantLock(true);
    
    // Controle de throttling thread-safe (timestamp da última requisição)
    private final AtomicLong ultimaRequisicaoTimestamp = new AtomicLong(0);
    
    // Configurações de retry
    private final int maxTentativas;
    private final long delayBaseMs;
    private final double multiplicador;
    
    // Configuração de throttling
    private final long throttlingMinimoMs;
    
    // Constantes da API
    private static final long DELAY_HTTP_429_MS = 2000L; // 2 segundos para 429
    
    /**
     * Construtor privado (Singleton).
     * Use getInstance() para obter a instância.
     */
    private GerenciadorRequisicaoHttp() {
        this.maxTentativas = CarregadorConfig.obterMaxTentativasRetry();
        this.delayBaseMs = CarregadorConfig.obterDelayBaseRetry();
        this.multiplicador = CarregadorConfig.obterMultiplicadorRetry();
        this.throttlingMinimoMs = CarregadorConfig.obterThrottlingMinimo();
        
        logger.info("🔒 GerenciadorRequisicaoHttp (SINGLETON) inicializado - Max tentativas: {}, Delay base: {}ms, Multiplicador: {}, Throttling mínimo: {}ms", 
                   maxTentativas, delayBaseMs, multiplicador, throttlingMinimoMs);
    }
    
    /**
     * Método auxiliar para aguardar com tratamento adequado de interrupção
     * 
     * @param delayMs Tempo de espera em milissegundos
     * @param contexto Contexto da operação para logging
     * @throws RuntimeException Se a thread for interrompida
     */
    private void aguardarComTratamentoInterrupcao(long delayMs, String contexto) {
        try {
            ThreadUtil.aguardar(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread interrompida durante " + contexto, e);
        }
    }
    
    /**
     * PROBLEMA 5 CORRIGIDO: Verifica se um código de status HTTP deve ser retentado
     * 
     * @param statusCode Código de status HTTP
     * @return true se deve retentar, false caso contrário
     */
    private boolean deveRetentar(int statusCode) {
        // PROBLEMA 5: 404, 401, 403 são erros definitivos - NÃO retente
        if (statusCode == 404 || statusCode == 401 || statusCode == 403) {
            return false;
        }
        
        // PROBLEMA 5: 500, 502, 503 devem ser retentados com backoff exponencial
        if (statusCode == 500 || statusCode == 502 || statusCode == 503) {
            return true;
        }
        
        // PROBLEMA 5: 429 deve ser retentado com delay fixo de 2s
        if (statusCode == 429) {
            return true;
        }
        
        // Outros códigos 5xx também podem ser retentados
        if (statusCode >= 500 && statusCode <= 599) {
            return true;
        }
        
        // Outros códigos 4xx são erros definitivos
        if (statusCode >= 400 && statusCode <= 499) {
            return false;
        }
        
        // Códigos 2xx e 3xx são sucessos/redirecionamentos
        return false;
    }
    
    /**
     * Executa uma requisição HTTP com throttling, retry, backoff exponencial e circuit breaker.
     * 
     * CORREÇÃO CRÍTICA #8: Circuit breaker protege contra avalanche de requisições.
     * PROBLEMA 5 CORRIGIDO: Implementa retry seletivo baseado no código de status.
     * 
     * @param cliente Cliente HTTP configurado
     * @param requisicao Requisição HTTP a ser enviada
     * @param tipoEntidade Tipo de entidade para logs (opcional)
     * @return HttpResponse com a resposta da API
     * @throws RuntimeException Se a requisição falhar após todas as tentativas ou circuit breaker aberto
     */
    public HttpResponse<String> executarRequisicao(HttpClient cliente, HttpRequest requisicao, String tipoEntidade) {
        // ✅ CORREÇÃO CRÍTICA #8: Verificar circuit breaker ANTES de tentar
        if (!circuitBreaker.canExecute()) {
            long timeUntilReset = circuitBreaker.getTimeUntilReset();
            String mensagem = String.format(
                "🚨 CIRCUIT BREAKER ABERTO - API indisponível para %s. " +
                "Sistema em proteção. Aguarde %d segundos para nova tentativa.",
                tipoEntidade != null ? tipoEntidade : "requisição",
                timeUntilReset
            );
            logger.error(mensagem);
            throw new RuntimeException(mensagem);
        }
        
        // Aplicar throttling antes da primeira tentativa
        aplicarThrottling();
        
        for (int tentativa = 1; tentativa <= maxTentativas; tentativa++) {
            try {
                logger.debug("Executando requisição para {} - Tentativa {}/{}", 
                           tipoEntidade != null ? tipoEntidade : "API", tentativa, maxTentativas);
                
                HttpResponse<String> resposta = cliente.send(requisicao, HttpResponse.BodyHandlers.ofString());
                int statusCode = resposta.statusCode();
                
                // Sucesso (200-299)
                if (statusCode >= 200 && statusCode < 300) {
                    // ✅ CORREÇÃO CRÍTICA #8: Registrar sucesso no circuit breaker
                    circuitBreaker.recordSuccess();
                    
                    logger.debug("✓ Requisição bem-sucedida para {} - Status: {}", 
                               tipoEntidade != null ? tipoEntidade : "API", statusCode);
                    return resposta;
                }
                
                // PROBLEMA 5 CORRIGIDO: Verifica se deve retentar baseado no código de status
                if (!deveRetentar(statusCode)) {
                    // HTTP 404 é esperado para faturas sem itens - usa DEBUG
                    if (statusCode == 404) {
                        logger.debug("ℹ️ HTTP 404 para {} (esperado - recurso não encontrado). Resposta: {}",
                            tipoEntidade != null ? tipoEntidade : "API",
                            resposta.body().length() > 200 ? resposta.body().substring(0, 200) + "..." : resposta.body()
                        );
                    }
                    // Outros erros definitivos (401, 403) usam ERROR
                    else {
                        String mensagemErro = String.format(
                            "✗ Erro definitivo na requisição para %s - HTTP %d (não será retentado). Resposta: %s",
                            tipoEntidade != null ? tipoEntidade : "API",
                            statusCode,
                            resposta.body().length() > 200 ? resposta.body().substring(0, 200) + "..." : resposta.body()
                        );
                        logger.error(mensagemErro);
                    }
                    return resposta; // Retorna a resposta com erro ao invés de lançar exceção
                }
                
                // ✅ CORREÇÃO CRÍTICA #8: Registrar falha no circuit breaker para erros retentáveis
                
                // PROBLEMA 5: Rate Limit (429) - Espera fixa de 2 segundos
                if (statusCode == 429) {
                    circuitBreaker.recordFailure(); // ✅ Registrar falha
                    
                    logger.warn("⚠️ Rate limit atingido (HTTP 429) para {} - Tentativa {}/{}. Aguardando {} segundos...", 
                              tipoEntidade != null ? tipoEntidade : "API", tentativa, maxTentativas, DELAY_HTTP_429_MS / 1000);
                    
                    if (tentativa < maxTentativas) {
                        aguardarComTratamentoInterrupcao(DELAY_HTTP_429_MS, "retry após HTTP 429");
                    }
                }
                // PROBLEMA 5: Erros de servidor (500, 502, 503, outros 5xx) - Backoff exponencial
                else if (statusCode >= 500 && statusCode <= 599) {
                    circuitBreaker.recordFailure(); // ✅ Registrar falha
                    
                    logger.error("✗ Erro de servidor (HTTP {}) para {} - Tentativa {}/{}. Resposta: {}", 
                               statusCode, tipoEntidade != null ? tipoEntidade : "API", tentativa, maxTentativas, 
                               resposta.body().length() > 200 ? resposta.body().substring(0, 200) + "..." : resposta.body());
                    
                    if (tentativa < maxTentativas) {
                        long delayMs = calcularDelayBackoffExponencial(tentativa);
                        logger.info("🕒 Aguardando {}ms antes da próxima tentativa (backoff exponencial)...", delayMs);
                        aguardarComTratamentoInterrupcao(delayMs, "backoff exponencial");
                    }
                }
                
            } catch (HttpTimeoutException e) {
                circuitBreaker.recordFailure(); // ✅ Registrar falha no circuit breaker
                
                logger.error("✗ Timeout na requisição para {} - Tentativa {}/{}", 
                           tipoEntidade != null ? tipoEntidade : "API", tentativa, maxTentativas, e);
                
                if (tentativa < maxTentativas) {
                    long delayMs = calcularDelayBackoffExponencial(tentativa);
                    logger.info("🕒 Aguardando {}ms antes da próxima tentativa após timeout...", delayMs);
                    aguardarComTratamentoInterrupcao(delayMs, "retry após timeout");
                }
                
            } catch (IOException e) {
                circuitBreaker.recordFailure(); // ✅ Registrar falha no circuit breaker
                
                // PROBLEMA 5: IOException deve ser retentado normalmente
                logger.error("✗ IOException na requisição para {} - Tentativa {}/{}: {}", 
                           tipoEntidade != null ? tipoEntidade : "API", tentativa, maxTentativas, e.getMessage());
                
                if (tentativa < maxTentativas) {
                    long delayMs = calcularDelayBackoffExponencial(tentativa);
                    logger.info("🕒 Aguardando {}ms antes da próxima tentativa após IOException...", delayMs);
                    aguardarComTratamentoInterrupcao(delayMs, "retry após IOException");
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Thread interrompida durante requisição", e);
            }
        }
        
        // Se chegou aqui, todas as tentativas falharam
        String mensagemFalha = String.format(
            "✗ Requisição para %s falhou após %d tentativas. Verifique conectividade e configurações da API.",
            tipoEntidade != null ? tipoEntidade : "API",
            maxTentativas
        );
        logger.error(mensagemFalha);
        throw new RuntimeException(mensagemFalha);
    }

    /**
     * Variante que permite especificar o charset da resposta como texto.
     * Útil para downloads CSV que podem vir em ISO-8859-1/Windows-1252.
     * 
     * CORREÇÃO CRÍTICA #8: Inclui circuit breaker (mesma lógica do método principal)
     */
    public HttpResponse<String> executarRequisicaoComCharset(HttpClient cliente, HttpRequest requisicao, String tipoEntidade, Charset charset) {
        // ✅ CORREÇÃO CRÍTICA #8: Verificar circuit breaker
        if (!circuitBreaker.canExecute()) {
            long timeUntilReset = circuitBreaker.getTimeUntilReset();
            String mensagem = String.format(
                "🚨 CIRCUIT BREAKER ABERTO - API indisponível para %s. Aguarde %d segundos.",
                tipoEntidade != null ? tipoEntidade : "requisição", timeUntilReset
            );
            logger.error(mensagem);
            throw new RuntimeException(mensagem);
        }
        
        aplicarThrottling();
        for (int tentativa = 1; tentativa <= maxTentativas; tentativa++) {
            try {
                logger.debug("Executando requisição (charset={}) para {} - Tentativa {}/{}",
                        charset.displayName(), tipoEntidade != null ? tipoEntidade : "API", tentativa, maxTentativas);
                HttpResponse<String> resposta = cliente.send(requisicao, HttpResponse.BodyHandlers.ofString(charset));
                int statusCode = resposta.statusCode();
                if (statusCode >= 200 && statusCode < 300) {
                    circuitBreaker.recordSuccess(); // ✅ Registrar sucesso
                    logger.debug("✓ Requisição bem-sucedida para {} - Status: {}",
                            tipoEntidade != null ? tipoEntidade : "API", statusCode);
                    return resposta;
                }
                if (!deveRetentar(statusCode)) {
                    if (statusCode == 404) {
                        logger.debug("ℹ️ HTTP 404 para {} (esperado). Resposta: {}",
                                tipoEntidade != null ? tipoEntidade : "API",
                                resposta.body().length() > 200 ? resposta.body().substring(0, 200) + "..." : resposta.body());
                    } else {
                        String mensagemErro = String.format(
                                "✗ Erro definitivo na requisição para %s - HTTP %d (não será retentado). Resposta: %s",
                                tipoEntidade != null ? tipoEntidade : "API",
                                statusCode,
                                resposta.body().length() > 200 ? resposta.body().substring(0, 200) + "..." : resposta.body());
                        logger.error(mensagemErro);
                    }
                    return resposta;
                }
                if (statusCode == 429) {
                    circuitBreaker.recordFailure(); // ✅ Registrar falha
                    logger.warn("⚠️ Rate limit (429) para {} - Tentativa {}/{}. Aguardando {}s...",
                            tipoEntidade != null ? tipoEntidade : "API", tentativa, maxTentativas, DELAY_HTTP_429_MS / 1000);
                    if (tentativa < maxTentativas) {
                        aguardarComTratamentoInterrupcao(DELAY_HTTP_429_MS, "retry após HTTP 429");
                    }
                } else if (statusCode >= 500 && statusCode <= 599) {
                    circuitBreaker.recordFailure(); // ✅ Registrar falha
                    logger.error("✗ Erro de servidor (HTTP {}) para {} - Tentativa {}/{}. Resposta: {}",
                            statusCode, tipoEntidade != null ? tipoEntidade : "API", tentativa, maxTentativas,
                            resposta.body().length() > 200 ? resposta.body().substring(0, 200) + "..." : resposta.body());
                    if (tentativa < maxTentativas) {
                        long delayMs = calcularDelayBackoffExponencial(tentativa);
                        logger.info("🕒 Aguardando {}ms antes da próxima tentativa (backoff exponencial)...", delayMs);
                        aguardarComTratamentoInterrupcao(delayMs, "backoff exponencial");
                    }
                }
            } catch (HttpTimeoutException e) {
                circuitBreaker.recordFailure(); // ✅ Registrar falha
                logger.error("✗ Timeout na requisição para {} - Tentativa {}/{}",
                        tipoEntidade != null ? tipoEntidade : "API", tentativa, maxTentativas, e);
                if (tentativa < maxTentativas) {
                    long delayMs = calcularDelayBackoffExponencial(tentativa);
                    logger.info("🕒 Aguardando {}ms antes da próxima tentativa após timeout...", delayMs);
                    aguardarComTratamentoInterrupcao(delayMs, "retry após timeout");
                }
            } catch (IOException e) {
                circuitBreaker.recordFailure(); // ✅ Registrar falha
                logger.error("✗ IOException na requisição para {} - Tentativa {}/{}: {}",
                        tipoEntidade != null ? tipoEntidade : "API", tentativa, maxTentativas, e.getMessage());
                if (tentativa < maxTentativas) {
                    long delayMs = calcularDelayBackoffExponencial(tentativa);
                    logger.info("🕒 Aguardando {}ms antes da próxima tentativa após IOException...", delayMs);
                    aguardarComTratamentoInterrupcao(delayMs, "retry após IOException");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Thread interrompida durante requisição", e);
            }
        }
        String mensagemFalha = String.format(
                "✗ Requisição para %s falhou após %d tentativas. Verifique conectividade e configurações da API.",
                tipoEntidade != null ? tipoEntidade : "API",
                maxTentativas);
        logger.error(mensagemFalha);
        throw new RuntimeException(mensagemFalha);
    }
    
    /**
     * Aplica throttling GLOBAL para respeitar o rate limit da API.
     * Garante que haja pelo menos o intervalo mínimo configurado entre requisições.
     * 
     * IMPORTANTE: Usa ReentrantLock fair=true para garantir:
     * - Ordem FIFO (First In, First Out) entre threads concorrentes
     * - Evita starvation (nenhuma thread fica esperando indefinidamente)
     * - Throttling é GLOBAL (todas as threads respeitam o mesmo intervalo)
     */
    private void aplicarThrottling() {
        lockThrottling.lock();
        try {
            long agora = System.currentTimeMillis();
            long ultimaRequisicao = ultimaRequisicaoTimestamp.get();
            long tempoDecorrido = agora - ultimaRequisicao;
            
            if (tempoDecorrido < throttlingMinimoMs) {
                long tempoEspera = throttlingMinimoMs - tempoDecorrido;
                logger.debug("🕒 Throttling GLOBAL aplicado - Espera: {}ms | Limite configurado: {}ms | Tempo decorrido: {}ms", 
                            tempoEspera, throttlingMinimoMs, tempoDecorrido);
                
                aguardarComTratamentoInterrupcao(tempoEspera, "throttling global");
            } else {
                logger.debug("✅ Throttling GLOBAL OK - Tempo decorrido: {}ms | Limite: {}ms", 
                            tempoDecorrido, throttlingMinimoMs);
            }
            
            // Atualiza timestamp APÓS o throttling (garante que próxima requisição respeitará o intervalo)
            ultimaRequisicaoTimestamp.set(System.currentTimeMillis());
        } finally {
            lockThrottling.unlock();
        }
    }
    
    /**
     * Calcula o delay para backoff exponencial.
     * 
     * @param tentativa Número da tentativa atual (1-based)
     * @return Delay em milissegundos
     */
    private long calcularDelayBackoffExponencial(int tentativa) {
        // Fórmula: delayBase * (multiplicador ^ (tentativa - 1))
        double delay = delayBaseMs * Math.pow(multiplicador, tentativa - 1);
        return Math.round(delay);
    }
}
