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
 * PROBLEMA 5 CORRIGIDO: Retry seletivo implementado
 */
public class GerenciadorRequisicaoHttp {
    private static final Logger logger = LoggerFactory.getLogger(GerenciadorRequisicaoHttp.class);
    
    // Controle de throttling thread-safe
    private final AtomicLong ultimaRequisicaoTimestamp = new AtomicLong(0);
    
    // Configurações de retry
    private final int maxTentativas;
    private final long delayBaseMs;
    private final double multiplicador;
    
    // Configuração de throttling
    private final long throttlingMinimoMs;
    
    // Constantes da API
    private static final long DELAY_HTTP_429_MS = 2000L; // 2 segundos para 429
    
    public GerenciadorRequisicaoHttp() {
        this.maxTentativas = CarregadorConfig.obterMaxTentativasRetry();
        this.delayBaseMs = CarregadorConfig.obterDelayBaseRetry();
        this.multiplicador = CarregadorConfig.obterMultiplicadorRetry();
        this.throttlingMinimoMs = CarregadorConfig.obterThrottlingMinimo();
        
        logger.info("GerenciadorRequisicaoHttp inicializado - Max tentativas: {}, Delay base: {}ms, Multiplicador: {}, Throttling mínimo: {}ms", 
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
            Thread.sleep(delayMs);
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
     * Executa uma requisição HTTP com throttling, retry e backoff exponencial.
     * PROBLEMA 5 CORRIGIDO: Implementa retry seletivo baseado no código de status
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
                
                // PROBLEMA 5: Rate Limit (429) - Espera fixa de 2 segundos
                if (statusCode == 429) {
                    logger.warn("⚠️ Rate limit atingido (HTTP 429) para {} - Tentativa {}/{}. Aguardando {} segundos...", 
                              tipoEntidade != null ? tipoEntidade : "API", tentativa, maxTentativas, DELAY_HTTP_429_MS / 1000);
                    
                    if (tentativa < maxTentativas) {
                        aguardarComTratamentoInterrupcao(DELAY_HTTP_429_MS, "retry após HTTP 429");
                    }
                }
                // PROBLEMA 5: Erros de servidor (500, 502, 503, outros 5xx) - Backoff exponencial
                else if (statusCode >= 500 && statusCode <= 599) {
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
                logger.error("✗ Timeout na requisição para {} - Tentativa {}/{}", 
                           tipoEntidade != null ? tipoEntidade : "API", tentativa, maxTentativas, e);
                
                if (tentativa < maxTentativas) {
                    long delayMs = calcularDelayBackoffExponencial(tentativa);
                    logger.info("🕒 Aguardando {}ms antes da próxima tentativa após timeout...", delayMs);
                    aguardarComTratamentoInterrupcao(delayMs, "retry após timeout");
                }
                
            } catch (IOException e) {
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
     * Aplica throttling para respeitar o rate limit da API.
     * Garante que haja pelo menos o intervalo mínimo configurado entre requisições.
     */
    private void aplicarThrottling() {
        long agora = System.currentTimeMillis();
        long ultimaRequisicao = ultimaRequisicaoTimestamp.get();
        long tempoDecorrido = agora - ultimaRequisicao;
        
        if (tempoDecorrido < throttlingMinimoMs) {
            long tempoEspera = throttlingMinimoMs - tempoDecorrido;
            logger.debug("🕒 Throttling aplicado - Espera: {}ms | Limite configurado: {}ms | Tempo decorrido: {}ms", 
                        tempoEspera, throttlingMinimoMs, tempoDecorrido);
            
            aguardarComTratamentoInterrupcao(tempoEspera, "throttling");
        } else {
            logger.debug("✅ Throttling OK - Tempo decorrido: {}ms | Limite: {}ms", 
                        tempoDecorrido, throttlingMinimoMs);
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