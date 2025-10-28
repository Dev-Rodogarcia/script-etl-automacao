package br.com.extrator.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Gerenciador centralizado para requisições HTTP com throttling, retry e backoff exponencial.
 * Implementa as regras mandatórias da API:
 * - Rate Limit: 2 segundos entre requisições
 * - Tratamento de HTTP 429: espera 2 segundos e retenta
 * - Tratamento de erros 5xx: backoff exponencial
 */
public class GerenciadorRequisicaoHttp {
    private static final Logger logger = LoggerFactory.getLogger(GerenciadorRequisicaoHttp.class);
    
    // Controle de throttling thread-safe
    private final AtomicLong ultimaRequisicaoTimestamp = new AtomicLong(0);
    
    // Configurações de retry
    private final int maxTentativas;
    private final long delayBaseMs;
    private final double multiplicador;
    
    // Constantes da API
    private static final long THROTTLING_MINIMO_MS = 2000L; // 2 segundos
    private static final long DELAY_HTTP_429_MS = 2000L; // 2 segundos para 429
    
    public GerenciadorRequisicaoHttp() {
        this.maxTentativas = CarregadorConfig.obterMaxTentativasRetry();
        this.delayBaseMs = CarregadorConfig.obterDelayBaseRetry();
        this.multiplicador = CarregadorConfig.obterMultiplicadorRetry();
        
        logger.info("GerenciadorRequisicaoHttp inicializado - Max tentativas: {}, Delay base: {}ms, Multiplicador: {}", 
                   maxTentativas, delayBaseMs, multiplicador);
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
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread interrompida durante " + contexto, e);
        }
    }
    
    /**
     * Executa uma requisição HTTP com throttling, retry e backoff exponencial.
     * 
     * @param cliente Cliente HTTP configurado
     * @param requisicao Requisição HTTP a ser enviada
     * @param tipoEntidade Tipo de entidade para logs (opcional)
     * @return HttpResponse com a resposta da API
     * @throws RuntimeException Se a requisição falhar após todas as tentativas
     */
    public HttpResponse<String> executarRequisicao(HttpClient cliente, HttpRequest requisicao, String tipoEntidade) {
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
                    logger.debug("Requisição bem-sucedida para {} - Status: {}", 
                               tipoEntidade != null ? tipoEntidade : "API", statusCode);
                    return resposta;
                }
                
                // Rate Limit (429) - Espera fixa de 2 segundos
                if (statusCode == 429) {
                    logger.warn("Rate limit atingido (HTTP 429) para {} - Tentativa {}/{}. Aguardando {} segundos...", 
                              tipoEntidade != null ? tipoEntidade : "API", tentativa, maxTentativas, DELAY_HTTP_429_MS / 1000);
                    
                    if (tentativa < maxTentativas) {
                        aguardarComTratamentoInterrupcao(DELAY_HTTP_429_MS, "retry após HTTP 429");
                    }
                }
                // Erros de servidor (5xx) - Backoff exponencial
                else if (statusCode >= 500 && statusCode <= 599) {
                    logger.error("Erro de servidor (HTTP {}) para {} - Tentativa {}/{}. Resposta: {}", 
                               statusCode, tipoEntidade != null ? tipoEntidade : "API", tentativa, maxTentativas, 
                               resposta.body().length() > 200 ? resposta.body().substring(0, 200) + "..." : resposta.body());
                    
                    if (tentativa < maxTentativas) {
                        long delayMs = calcularDelayBackoffExponencial(tentativa);
                        logger.info("Aguardando {}ms antes da próxima tentativa...", delayMs);
                        aguardarComTratamentoInterrupcao(delayMs, "backoff exponencial");
                    }
                }
                // Outros erros (4xx, etc.) - Não recuperáveis
                else {
                    String mensagemErro = String.format(
                        "Erro não recuperável na requisição para %s - HTTP %d. Resposta: %s",
                        tipoEntidade != null ? tipoEntidade : "API",
                        statusCode,
                        resposta.body().length() > 200 ? resposta.body().substring(0, 200) + "..." : resposta.body()
                    );
                    logger.error(mensagemErro);
                    throw new RuntimeException(mensagemErro);
                }
                
            } catch (HttpTimeoutException e) {
                logger.error("Timeout na requisição para {} - Tentativa {}/{}", 
                           tipoEntidade != null ? tipoEntidade : "API", tentativa, maxTentativas, e);
                
                if (tentativa < maxTentativas) {
                    long delayMs = calcularDelayBackoffExponencial(tentativa);
                    logger.info("Aguardando {}ms antes da próxima tentativa após timeout...", delayMs);
                    aguardarComTratamentoInterrupcao(delayMs, "retry após timeout");
                }
                
            } catch (IOException | InterruptedException e) {
                logger.error("Exceção na requisição para {} - Tentativa {}/{}", 
                           tipoEntidade != null ? tipoEntidade : "API", tentativa, maxTentativas, e);
                
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Thread interrompida durante requisição", e);
                }
                
                if (tentativa < maxTentativas) {
                    long delayMs = calcularDelayBackoffExponencial(tentativa);
                    logger.info("Aguardando {}ms antes da próxima tentativa após exceção...", delayMs);
                    aguardarComTratamentoInterrupcao(delayMs, "retry após exceção");
                }
            }
        }
        
        // Se chegou aqui, todas as tentativas falharam
        String mensagemFalha = String.format(
            "Requisição para %s falhou após %d tentativas. Verifique conectividade e configurações da API.",
            tipoEntidade != null ? tipoEntidade : "API",
            maxTentativas
        );
        logger.error(mensagemFalha);
        throw new RuntimeException(mensagemFalha);
    }
    
    /**
     * Aplica throttling garantindo intervalo mínimo de 2 segundos entre requisições.
     */
    private void aplicarThrottling() {
        long agora = System.currentTimeMillis();
        long ultimaRequisicao = ultimaRequisicaoTimestamp.get();
        long tempoDecorrido = agora - ultimaRequisicao;
        
        if (tempoDecorrido < THROTTLING_MINIMO_MS) {
            long tempoEspera = THROTTLING_MINIMO_MS - tempoDecorrido;
            logger.debug("Aplicando throttling - Aguardando {}ms para respeitar rate limit", tempoEspera);
            
            aguardarComTratamentoInterrupcao(tempoEspera, "throttling");
        }
        
        // Atualiza timestamp da última requisição
        ultimaRequisicaoTimestamp.set(System.currentTimeMillis());
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