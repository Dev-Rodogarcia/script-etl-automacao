---
context:
  - Documentacao
  - Legado
  - ArquivoHistorico
updated_at: 2026-03-18T00:00:32.1501533-03:00
source_of_truth: docs
classification: legado
related_files:
  - docs/index.md
  - docs/legado/index.md
  - docs/legado/classificacao.md
---
package br.com.extrator.api;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.extrator.modelo.rest.faturaspagar.FaturaAPagarDTO;
import br.com.extrator.modelo.rest.faturasreceber.FaturaAReceberDTO;
import br.com.extrator.modelo.rest.ocorrencias.OcorrenciaDTO;
import br.com.extrator.util.CarregadorConfig;
import br.com.extrator.util.GerenciadorRequisicaoHttp;

/**
 * Classe responsável pela comunicação com a API REST do ESL Cloud
 * Especializada em buscar Faturas e Ocorrências via endpoints REST
 */
public class ClienteApiRest {
    private static final Logger logger = LoggerFactory.getLogger(ClienteApiRest.class);

    // Limites configuráveis para paginação
    private final int maxPaginasPorExecucao;
    private final int intervaloPaginasLog;

    // Circuit Breaker - Controle de falhas consecutivas
    private final Map<String, Integer> contadorFalhasConsecutivas = new HashMap<>();
    private final Set<String> entidadesComCircuitAberto = new HashSet<>();
    private static final int MAX_FALHAS_CONSECUTIVAS = 5;

    private final String urlBase;
    private final String token;
    private final HttpClient clienteHttp;
    private final ObjectMapper mapeadorJson;
    private final Duration timeoutRequisicao;
    private final GerenciadorRequisicaoHttp gerenciadorRequisicao;

    /**
     * Construtor que inicializa o cliente HTTP e carrega as configurações da API
     * REST
     */
    public ClienteApiRest() {
        this.urlBase = CarregadorConfig.obterUrlBaseApi();
        this.token = CarregadorConfig.obterTokenApiRest();
        this.timeoutRequisicao = CarregadorConfig.obterTimeoutApiRest();
        this.maxPaginasPorExecucao = CarregadorConfig.obterLimitePaginasApiRest();
        this.intervaloPaginasLog = 10; // Valor padrão para log de progresso a cada 10 páginas
        this.clienteHttp = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.mapeadorJson = new ObjectMapper();
        this.mapeadorJson.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.gerenciadorRequisicao = new GerenciadorRequisicaoHttp();
    }

    /**
     * Busca faturas a RECEBER da API REST.
     * Utiliza janela móvel de 24 horas para capturar todos os dados
     * 
     * @param dataReferencia Data de referência para busca (não utilizada - mantida
     *                       para compatibilidade)
     * @return Lista de DTOs de faturas a receber
     */
    public ResultadoExtracao<FaturaAReceberDTO> buscarFaturasAReceber(final LocalDate dataReferencia) {
        // CORREÇÃO: Usa janela móvel de 24h em vez de meia-noite para capturar 100% dos
        // dados
        final ZonedDateTime agora = ZonedDateTime.now(ZoneId.of("America/Sao_Paulo"));
        final ZonedDateTime dataInicio = agora.minusHours(24);
        final String dataInicioFormatada = dataInicio.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        logger.info("Buscando faturas a receber com janela móvel de 24h: {} até {}",
                dataInicio.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                agora.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

        return buscarEntidadesComResultado("/api/accounting/credit/billings", dataInicioFormatada, "faturas_a_receber",
                FaturaAReceberDTO.class);
    }

    /**
     * Busca faturas a PAGAR da API REST.
     * Utiliza janela móvel de 24 horas para capturar todos os dados
     * 
     * @param dataReferencia Data de referência para busca (não utilizada - mantida
     *                       para compatibilidade)
     * @return Lista de DTOs de faturas a pagar
     */
    public ResultadoExtracao<FaturaAPagarDTO> buscarFaturasAPagar(final LocalDate dataReferencia) {
        // CORREÇÃO: Usa janela móvel de 24h em vez de meia-noite para capturar 100% dos
        // dados
        final ZonedDateTime agora = ZonedDateTime.now(ZoneId.of("America/Sao_Paulo"));
        final ZonedDateTime dataInicio = agora.minusHours(24);
        final String dataInicioFormatada = dataInicio.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        logger.info("Buscando faturas a pagar com janela móvel de 24h: {} até {}",
                dataInicio.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                agora.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

        return buscarEntidadesComResultado("/api/accounting/debit/billings", dataInicioFormatada, "faturas_a_pagar",
                FaturaAPagarDTO.class);
    }

    /**
     * Busca ocorrências da API REST ESL Cloud com paginação
     * Utiliza janela móvel de 24 horas para capturar todos os dados
     * 
     * @param dataReferencia Data de referência para busca (não utilizada - mantida
     *                       para compatibilidade)
     * @return Lista de DTOs de ocorrências
     */
    public ResultadoExtracao<OcorrenciaDTO> buscarOcorrencias(final LocalDate dataReferencia) {
        // CORREÇÃO: Usa janela móvel de 24h em vez de meia-noite para capturar 100% dos
        // dados
        final ZonedDateTime agora = ZonedDateTime.now(ZoneId.of("America/Sao_Paulo"));
        final ZonedDateTime dataInicio = agora.minusHours(24);
        final String dataInicioFormatada = dataInicio.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        logger.info("Buscando ocorrências com janela móvel de 24h: {} até {}",
                dataInicio.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                agora.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

        return buscarEntidadesComResultado("/api/invoice_occurrences", dataInicioFormatada, "ocorrencias",
                OcorrenciaDTO.class);
    }

    /**
     * Busca itens/parcelas de uma fatura a pagar.
     * Inclui delay intencional de 2.2s para respeitar rate limit da API.
     * 
     * @param idFatura ID da fatura para buscar os itens
     * @return JSON bruto dos itens da fatura
     */
    public String buscarItensFaturaAPagar(final Long idFatura) {
        logger.info("Buscando itens da fatura a pagar ID: {}", idFatura);

        try {
            // Delay CRÍTICO: respeita rate limit de 2s da API + margem de segurança
            aplicarDelayRateLimit("fatura-" + idFatura);

            final String endpoint = String.format("/api/accounting/debit/billings/%d/installments", idFatura);
            final String urlCompleta = urlBase + endpoint;

            final HttpRequest requisicao = HttpRequest.newBuilder()
                    .uri(URI.create(urlCompleta))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .timeout(this.timeoutRequisicao)
                    .GET()
                    .build();

            final HttpResponse<String> resposta = gerenciadorRequisicao.executarRequisicao(
                    this.clienteHttp, requisicao, "ItensFaturaAPagar-" + idFatura);

            if (resposta.statusCode() == 200) {
                logger.debug("Itens da fatura {} obtidos com sucesso", idFatura);
                return resposta.body();
            } else {
                logger.error("Erro ao buscar itens da fatura {}: HTTP {} - {}",
                        idFatura, resposta.statusCode(), resposta.body());
                return "{}";
            }

        } catch (final Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("HTTP 404")) {
                logger.debug("Fatura {} não possui itens (HTTP 404 - esperado)", idFatura);
                return "{}";
            } else {
                logger.error("Erro ao buscar itens da fatura {}: {}", idFatura, e.getMessage(), e);
                return "{}";
            }
        }
    }

    /**
     * Formata um LocalDate para o formato esperado pela API REST.
     * Converte para início do dia no fuso horário do Brasil (America/Sao_Paulo)
     * e formata como ISO 8601 completo com offset de timezone.
     * 
     * @param dataReferencia Data de referência
     * @return String formatada para API REST (ex: 2025-10-27T00:00:00-03:00)
     */
    private String formatarDataParaApiRest(final LocalDate dataReferencia) {
        final ZonedDateTime inicioDodia = dataReferencia.atStartOfDay(ZoneId.of("America/Sao_Paulo"));
        return inicioDodia.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    /**
     * Busca entidades tipadas com resultado detalhado incluindo informações de
     * paginação
     * 
     * @param <T>            Tipo da entidade
     * @param endpoint       Endpoint da API
     * @param dataInicio     Data de início formatada
     * @param tipoEntidade   Nome do tipo de entidade para logs
     * @param classeEntidade Classe da entidade
     * @return ResultadoExtracao com dados e informações de paginação
     */
    public <T> ResultadoExtracao<T> buscarEntidadesComResultado(final String endpoint, final String dataInicio,
            final String tipoEntidade, final Class<T> classeEntidade) {
        logger.info("Iniciando busca de {} a partir de: {}", endpoint, dataInicio);

        // Circuit Breaker - Verifica se o circuit está aberto para esta entidade
        if (entidadesComCircuitAberto.contains(tipoEntidade)) {
            logger.warn("Circuit Breaker ABERTO para {}. Retornando resultado vazio.", tipoEntidade);
            return ResultadoExtracao.completo(new ArrayList<>(), 0, 0);
        }

        final List<T> entidades = new ArrayList<>();

        String proximoId = null;
        boolean primeiraPagina = true;
        int paginasProcessadas = 0;
        boolean interrompidoPorLimite = false;

        // Proteção inteligente contra loops infinitos
        int paginasVaziasConsecutivas = 0;
        final int MAX_PAGINAS_VAZIAS_CONSECUTIVAS = 10;

        // Validação básica de configuração
        if (urlBase == null || urlBase.isBlank() || token == null || token.isBlank()) {
            logger.error("Configurações inválidas para chamada REST (urlBase/token)");
            return ResultadoExtracao.completo(entidades, paginasProcessadas, entidades.size());
        }

        try {
            do {
                // Delay CRÍTICO: respeita rate limit de 2s da API + margem de segurança
                aplicarDelayRateLimit(tipoEntidade);

                // Verifica limite de páginas
                if (paginasProcessadas >= maxPaginasPorExecucao) {
                    logger.warn("Limite de páginas atingido para {}: {} páginas processadas. Interrompendo extração.",
                            tipoEntidade, paginasProcessadas);
                    interrompidoPorLimite = true;
                    break;
                }

                // Constrói a URL com os parâmetros adequados
                String url;
                if (primeiraPagina) {
                    // Usa diretamente o timestamp formatado com fuso horário
                    final String timestampFormatado = dataInicio;

                    // Log detalhado para diagnóstico
                    logger.info("DIAGNÓSTICO API REST - Timestamp formatado: {}", timestampFormatado);

                    // Codifica o timestamp para URL para evitar problemas com caracteres especiais
                    final String timestampCodificado = URLEncoder.encode(timestampFormatado, StandardCharsets.UTF_8);

                    logger.debug("Endpoint: {}, Timestamp codificado para URL: {}", endpoint, timestampCodificado);

                    url = urlBase + endpoint + "?since=" + timestampCodificado;

                    // OTIMIZAÇÃO: Aumenta o tamanho da página para reduzir chamadas de rede
                    // Especialmente importante para FATURAS_A_RECEBER que tem alto volume
                    url += "&per=100";
                    primeiraPagina = false;
                } else {
                    url = urlBase + endpoint + "?start=" + proximoId;
                    // OTIMIZAÇÃO: Mantém o tamanho da página otimizado para páginas subsequentes
                    url += "&per=100";
                }

                // Log da URL completa para depuração
                logger.debug("Fazendo requisição HTTP REST para {}: {}", tipoEntidade, url);

                final long inicioMs = System.currentTimeMillis();

                // Constrói a requisição HTTP com timeout configurável
                final HttpRequest requisicao = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Authorization", "Bearer " + token)
                        .header("Accept", "application/json")
                        .GET()
                        .timeout(this.timeoutRequisicao)
                        .build();

                // Executa a requisição usando o gerenciador
                final HttpResponse<String> resposta = gerenciadorRequisicao.executarRequisicao(
                        this.clienteHttp, requisicao, tipoEntidade);

                final long duracaoMs = System.currentTimeMillis() - inicioMs;

                // Verifica se a resposta da requisição é válida
                if (resposta == null) {
                    logger.error(
                            "Erro irrecuperável: a resposta da requisição é nula para {} após todas as tentativas.",
                            tipoEntidade);
                    throw new RuntimeException("Falha na requisição: resposta é null após todas as tentativas.");
                }

                // Verifica se a resposta foi bem-sucedida
                if (resposta.statusCode() != 200) {
                    final String mensagemErro = criarMensagemErroDetalhada(resposta.statusCode(), tipoEntidade,
                            endpoint);
                    logger.error("Erro ao buscar {}. Código de status: {}, ({} ms) Body: {}", tipoEntidade,
                            resposta.statusCode(), duracaoMs, resposta.body());
                    throw new RuntimeException(mensagemErro);
                }

                // Processa a resposta JSON
                final JsonNode raizJson = mapeadorJson.readTree(resposta.body());
                final JsonNode dadosJson = raizJson.get("data");
                final JsonNode paginacaoJson = raizJson.get("paging");

                // Extrai o próximo ID para paginação
                proximoId = paginacaoJson != null && paginacaoJson.has("next_id")
                        && !paginacaoJson.get("next_id").isNull()
                                ? paginacaoJson.get("next_id").asText()
                                : null;

                // Converte os dados JSON em objetos tipados
                int entidadesNestaPagina = 0; // Nova variável para contar
                if (dadosJson != null && dadosJson.isArray()) {
                    entidadesNestaPagina = dadosJson.size(); // Conta quantos itens vieram
                    for (final JsonNode entidadeJson : dadosJson) {
                        try {
                            // Deserializa diretamente para a classe tipada
                            final T entidade = mapeadorJson.treeToValue(entidadeJson, classeEntidade);
                            entidades.add(entidade);
                        } catch (final JsonProcessingException | IllegalArgumentException e) {
                            logger.warn("Erro ao processar {}: {}", tipoEntidade, e.getMessage());
                        }
                    }
                }

                paginasProcessadas++;

                // Log de progresso a cada intervalo configurado
                if (paginasProcessadas % intervaloPaginasLog == 0) {
                    logger.info("Progresso {}: {} páginas processadas, {} entidades coletadas",
                            tipoEntidade, paginasProcessadas, entidades.size());
                }

                logger.info("Processadas {} entidades nesta página ({} ms)", entidadesNestaPagina, duracaoMs);

                // PROTEÇÃO INTELIGENTE: Respeita next_id=null, mas tolera gaps
                if (entidadesNestaPagina == 0) {
                    // CRÍTICO: Se API retornou next_id=null, NÃO HÁ MAIS PÁGINAS!
                    if (proximoId == null) {
                        logger.info("✅ Fim natural da paginação - API retornou next_id=null em página vazia");
                        break; // Sai do loop imediatamente
                    }

                    // Se next_id EXISTE mas página vazia, pode ser gap - continua com proteção
                    paginasVaziasConsecutivas++;
                    logger.warn(
                            "⚠️ Página {} vazia ({}/{} consecutivas) mas next_id existe: {}. Continuando (possível gap de IDs)...",
                            paginasProcessadas, paginasVaziasConsecutivas,
                            MAX_PAGINAS_VAZIAS_CONSECUTIVAS, proximoId);

                    if (paginasVaziasConsecutivas >= MAX_PAGINAS_VAZIAS_CONSECUTIVAS) {
                        logger.error(
                                "🚨 PROTEÇÃO: {} páginas vazias consecutivas com next_id. Possível loop infinito - Parando.",
                                MAX_PAGINAS_VAZIAS_CONSECUTIVAS);
                        break;
                    }
                } else {
                    // Reset contador quando encontra dados
                    paginasVaziasConsecutivas = 0;
                }

            } while (proximoId != null);

        } catch (final IOException e) {
            logger.error("Erro de I/O ou JSON durante a comunicação com a API", e);

            // Circuit Breaker - Incrementa contador de falhas
            incrementarFalhasConsecutivas(tipoEntidade);

            throw new RuntimeException("Erro ao comunicar com a API ESL Cloud", e);
        } catch (final RuntimeException e) {
            logger.error("Erro durante a busca de {}: {}", tipoEntidade, e.getMessage());

            // Circuit Breaker - Incrementa contador de falhas
            incrementarFalhasConsecutivas(tipoEntidade);

            throw e;
        }

        // Circuit Breaker - Reset contador de falhas em caso de sucesso
        resetarFalhasConsecutivas(tipoEntidade);

        logger.info("Busca de {} concluída. Páginas processadas: {}, Total de entidades encontradas: {}",
                endpoint, paginasProcessadas, entidades.size());

        // Retorna resultado apropriado baseado se foi interrompido ou não
        if (interrompidoPorLimite) {
            return ResultadoExtracao.incompleto(entidades,
                    ResultadoExtracao.MotivoInterrupcao.LIMITE_PAGINAS,
                    paginasProcessadas, entidades.size());
        } else {
            return ResultadoExtracao.completo(entidades, paginasProcessadas, entidades.size());
        }
    }

    /**
     * Valida se as credenciais de acesso à API ESL estão funcionando
     * 
     * @return true se a validação foi bem-sucedida, false caso contrário
     */
    public boolean validarAcessoApi() {
        logger.info("Validando acesso à API ESL Cloud...");

        // Lista de endpoints para testar (do mais específico para o mais geral)
        final String[] endpointsParaTestar = {
                "/api/v1/invoices?limit=1",
                "/api/invoices?limit=1",
                "/invoices?limit=1",
                "/api/v1/invoice",
                "/api/invoice",
                "/api/v1",
                "/api",
                "/"
        };

        for (final String endpoint : endpointsParaTestar) {
            try {
                final String url = urlBase + endpoint;
                logger.info("Testando endpoint: {}", url);

                final HttpRequest requisicao = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Authorization", "Bearer " + token)
                        .header("Accept", "application/json")
                        .header("Content-Type", "application/json")
                        .GET()
                        .build();

                final HttpResponse<String> resposta = clienteHttp.send(requisicao,
                        HttpResponse.BodyHandlers.ofString());

                logger.info("Resposta do endpoint {}: Status={}, Body_Length={}",
                        endpoint, resposta.statusCode(), resposta.body().length());

                // Verifica se a resposta foi bem-sucedida
                switch (resposta.statusCode()) {
                    case 200 -> {
                        logger.info("✅ Validação bem-sucedida! Endpoint funcional: {}", endpoint);
                        return true;
                    }
                    case 401 -> {
                        logger.error("❌ Erro de autenticação! Token inválido ou expirado. Endpoint: {}", endpoint);
                        return false;
                    }
                    case 403 -> {
                        logger.error("❌ Erro de autorização! Token sem permissões. Endpoint: {}", endpoint);
                        return false;
                    }
                    case 404 -> {
                        logger.warn("⚠️  Endpoint não encontrado: {} (Testando próximo...)", endpoint);
                        // Continua testando outros endpoints
                    }
                    case 405 -> {
                        logger.warn("⚠️  Método não permitido: {} (Endpoint existe, mas GET não é suportado)",
                                endpoint);
                        // Ainda assim, significa que a API está acessível
                        logger.info("✅ API acessível (endpoint existe): {}", endpoint);
                        return true;
                    }
                    default -> logger.warn("⚠️  Resposta inesperada do endpoint {}: Status={}", endpoint,
                            resposta.statusCode());
                }

            } catch (IOException | InterruptedException e) {
                logger.warn("⚠️  Erro ao testar endpoint {}: {}", endpoint, e.getMessage());
                // Continua testando outros endpoints
            }
        }

        logger.error("❌ Nenhum endpoint válido encontrado. Verifique a URL base e o token.");
        return false;
    }

    /**
     * Cria uma mensagem de erro detalhada baseada no código de status HTTP
     * 
     * @param statusCode   Código de status HTTP
     * @param tipoEntidade Tipo da entidade sendo buscada
     * @param endpoint     Endpoint que falhou
     * @return Mensagem de erro detalhada
     */
    private String criarMensagemErroDetalhada(final int statusCode, final String tipoEntidade, final String endpoint) {
        return switch (statusCode) {
            case 401 -> String.format("""
                    ❌ ERRO DE AUTENTICAÇÃO (HTTP 401)
                    Endpoint: %s
                    Problema: Token de acesso inválido, expirado ou sem permissões para acessar '%s'
                    Soluções:
                      • Verifique se o token no config.properties está correto
                      • Confirme se o token não expirou
                      • Solicite permissões de leitura para o endpoint '%s' à equipe da plataforma
                      • Consulte a documentação da API para verificar os endpoints disponíveis""",
                    endpoint, tipoEntidade, endpoint);

            case 403 -> String.format("""
                    ❌ ERRO DE AUTORIZAÇÃO (HTTP 403)
                    Endpoint: %s
                    Problema: Token válido mas sem permissões suficientes para acessar '%s'
                    Soluções:
                      • Solicite permissões de leitura para o endpoint '%s' à equipe da plataforma
                      • Verifique se sua conta tem acesso aos dados de '%s'""",
                    endpoint, tipoEntidade, endpoint, tipoEntidade);

            case 404 -> String.format("""
                    ❌ ERRO DE ENDPOINT NÃO ENCONTRADO (HTTP 404)
                    Endpoint: %s
                    Problema: O endpoint solicitado não existe ou foi movido
                    Soluções:
                      • Verifique se a URL base no config.properties está correta
                      • Confirme se o endpoint '%s' existe na documentação da API
                      • Verifique se não há erros de digitação no endpoint""",
                    endpoint, endpoint);

            case 500 -> String.format("""
                    ❌ ERRO INTERNO DO SERVIDOR (HTTP 500)
                    Endpoint: %s
                    Problema: Erro interno no servidor da API
                    Soluções:
                      • Verifique se os parâmetros enviados estão no formato correto
                      • Tente novamente em alguns minutos
                      • Entre em contato com o suporte técnico se o problema persistir""",
                    endpoint);

            case 406 -> String.format("""
                    ❌ ERRO DE FORMATO NÃO ACEITÁVEL (HTTP 406)
                    Endpoint: %s
                    Problema: Formato dos dados enviados não é aceito pela API
                    Soluções:
                      • Verifique se a data está no formato correto (yyyy-MM-ddTHH:mm:ss)
                      • Confirme se os headers da requisição estão corretos
                      • Verifique a documentação da API para o formato esperado""",
                    endpoint);

            default -> String.format("""
                    ❌ ERRO HTTP %d
                    Endpoint: %s
                    Problema: Erro inesperado ao buscar '%s'
                    Solução: Verifique os logs para mais detalhes e consulte a documentação da API""",
                    statusCode, endpoint, tipoEntidade);
        };
    }

    /**
     * Incrementa o contador de falhas consecutivas para uma entidade.
     * Se atingir o limite máximo, abre o circuit breaker.
     * 
     * @param tipoEntidade Tipo da entidade
     */
    private void incrementarFalhasConsecutivas(final String tipoEntidade) {
        int falhasAtuais = contadorFalhasConsecutivas.getOrDefault(tipoEntidade, 0);
        falhasAtuais++;
        contadorFalhasConsecutivas.put(tipoEntidade, falhasAtuais);

        logger.warn("Falha #{} para entidade '{}' (limite: {})",
                falhasAtuais, tipoEntidade, MAX_FALHAS_CONSECUTIVAS);

        if (falhasAtuais >= MAX_FALHAS_CONSECUTIVAS) {
            entidadesComCircuitAberto.add(tipoEntidade);
            logger.error("Circuit Breaker ABERTO para '{}' após {} falhas consecutivas",
                    tipoEntidade, falhasAtuais);
        }
    }

    /**
     * Reseta o contador de falhas consecutivas para uma entidade.
     * Remove a entidade do conjunto de circuits abertos.
     * 
     * @param tipoEntidade Tipo da entidade
     */
    private void resetarFalhasConsecutivas(final String tipoEntidade) {
        if (contadorFalhasConsecutivas.containsKey(tipoEntidade)) {
            contadorFalhasConsecutivas.remove(tipoEntidade);
            entidadesComCircuitAberto.remove(tipoEntidade);
            logger.debug("Circuit Breaker RESETADO para '{}'", tipoEntidade);
        }
    }

    /**
     * Obtém a contagem total de ocorrências para uma data de referência específica
     * 
     * @param dataReferencia Data de referência para filtrar as ocorrências
     * @return Número total de ocorrências encontradas
     * @throws RuntimeException se houver erro na requisição ou resposta inválida
     */
    public int obterContagemOcorrencias(final LocalDate dataReferencia) {
        final String dataInicio = formatarDataParaApiRest(dataReferencia);
        final String endpoint = "occurrences";

        try {
            // Constrói a URL com filtros de data e per_page=1 para otimização
            final String url = String.format("%s/%s?created_at_start=%s&per_page=1",
                    urlBase, endpoint, URLEncoder.encode(dataInicio, StandardCharsets.UTF_8));

            logger.info("Obtendo contagem de ocorrências para data: {} (URL: {})", dataReferencia, url);

            // Cria a requisição HTTP
            final HttpRequest requisicao = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/json")
                    .timeout(timeoutRequisicao)
                    .GET()
                    .build();

            // Executa a requisição
            final long inicioMs = System.currentTimeMillis();
            final HttpResponse<String> resposta = gerenciadorRequisicao.executarRequisicao(
                    this.clienteHttp, requisicao, "contagem-ocorrencias");
            final long duracaoMs = System.currentTimeMillis() - inicioMs;

            // Verifica se a resposta é válida
            if (resposta == null) {
                logger.error("Erro: resposta nula ao obter contagem de ocorrências");
                throw new RuntimeException("Falha na requisição: resposta é null");
            }

            if (resposta.statusCode() != 200) {
                final String mensagemErro = String.format("Erro ao obter contagem de ocorrências. Status: %d",
                        resposta.statusCode());
                logger.error("{} ({} ms) Body: {}", mensagemErro, duracaoMs, resposta.body());
                throw new RuntimeException(mensagemErro);
            }

            // Processa a resposta JSON para extrair meta.total_count
            final JsonNode raizJson = mapeadorJson.readTree(resposta.body());
            final JsonNode metaJson = raizJson.get("meta");

            if (metaJson == null || !metaJson.has("total_count")) {
                logger.error("Estrutura JSON inválida: campo 'meta.total_count' não encontrado na resposta");
                throw new RuntimeException("Resposta da API não contém campo 'meta.total_count'");
            }

            final int totalCount = metaJson.get("total_count").asInt();
            logger.info("Contagem de ocorrências obtida com sucesso: {} registros ({} ms)", totalCount, duracaoMs);

            return totalCount;

        } catch (final IOException e) {
            logger.error("Erro de I/O ao obter contagem de ocorrências", e);
            incrementarFalhasConsecutivas("contagem-ocorrencias");
            throw new RuntimeException("Erro ao comunicar com a API para contagem de ocorrências", e);
        } catch (final RuntimeException e) {
            logger.error("Erro ao obter contagem de ocorrências: {}", e.getMessage());
            incrementarFalhasConsecutivas("contagem-ocorrencias");
            throw e;
        }
    }

    /**
     * Obtém a contagem total de faturas a receber para uma data de referência
     * específica
     * 
     * @param dataReferencia Data de referência para filtrar as faturas
     * @return Número total de faturas a receber encontradas
     * @throws RuntimeException se houver erro na requisição ou resposta inválida
     */
    public int obterContagemFaturasAReceber(final LocalDate dataReferencia) {
        final String dataInicio = formatarDataParaApiRest(dataReferencia);
        final String endpoint = "receivable_invoices";

        try {
            // Constrói a URL com filtros de data e per_page=1 para otimização
            final String url = String.format("%s/%s?created_at_start=%s&per_page=1",
                    urlBase, endpoint, URLEncoder.encode(dataInicio, StandardCharsets.UTF_8));

            logger.info("Obtendo contagem de faturas a receber para data: {} (URL: {})", dataReferencia, url);

            // Cria a requisição HTTP
            final HttpRequest requisicao = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/json")
                    .timeout(timeoutRequisicao)
                    .GET()
                    .build();

            // Executa a requisição
            final long inicioMs = System.currentTimeMillis();
            final HttpResponse<String> resposta = gerenciadorRequisicao.executarRequisicao(
                    this.clienteHttp, requisicao, "contagem-faturas-receber");
            final long duracaoMs = System.currentTimeMillis() - inicioMs;

            // Verifica se a resposta é válida
            if (resposta == null) {
                logger.error("Erro: resposta nula ao obter contagem de faturas a receber");
                throw new RuntimeException("Falha na requisição: resposta é null");
            }

            if (resposta.statusCode() != 200) {
                final String mensagemErro = String.format("Erro ao obter contagem de faturas a receber. Status: %d",
                        resposta.statusCode());
                logger.error("{} ({} ms) Body: {}", mensagemErro, duracaoMs, resposta.body());
                throw new RuntimeException(mensagemErro);
            }

            // Processa a resposta JSON para extrair meta.total_count
            final JsonNode raizJson = mapeadorJson.readTree(resposta.body());
            final JsonNode metaJson = raizJson.get("meta");

            if (metaJson == null || !metaJson.has("total_count")) {
                logger.error("Estrutura JSON inválida: campo 'meta.total_count' não encontrado na resposta");
                throw new RuntimeException("Resposta da API não contém campo 'meta.total_count'");
            }

            final int totalCount = metaJson.get("total_count").asInt();
            logger.info("Contagem de faturas a receber obtida com sucesso: {} registros ({} ms)", totalCount,
                    duracaoMs);

            return totalCount;

        } catch (final IOException e) {
            logger.error("Erro de I/O ao obter contagem de faturas a receber", e);
            incrementarFalhasConsecutivas("contagem-faturas-receber");
            throw new RuntimeException("Erro ao comunicar com a API para contagem de faturas a receber", e);
        } catch (final RuntimeException e) {
            logger.error("Erro ao obter contagem de faturas a receber: {}", e.getMessage());
            incrementarFalhasConsecutivas("contagem-faturas-receber");
            throw e;
        }
    }

    /**
     * Obtém a contagem total de faturas a pagar para uma data de referência
     * específica
     * 
     * @param dataReferencia Data de referência para filtrar as faturas
     * @return Número total de faturas a pagar encontradas
     * @throws RuntimeException se houver erro na requisição ou resposta inválida
     */
    public int obterContagemFaturasAPagar(final LocalDate dataReferencia) {
        final String dataInicio = formatarDataParaApiRest(dataReferencia);
        final String endpoint = "payable_invoices";

        try {
            // Constrói a URL com filtros de data e per_page=1 para otimização
            final String url = String.format("%s/%s?created_at_start=%s&per_page=1",
                    urlBase, endpoint, URLEncoder.encode(dataInicio, StandardCharsets.UTF_8));

            logger.info("Obtendo contagem de faturas a pagar para data: {} (URL: {})", dataReferencia, url);

            // Cria a requisição HTTP
            final HttpRequest requisicao = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/json")
                    .timeout(timeoutRequisicao)
                    .GET()
                    .build();

            // Executa a requisição
            final long inicioMs = System.currentTimeMillis();
            final HttpResponse<String> resposta = gerenciadorRequisicao.executarRequisicao(
                    this.clienteHttp, requisicao, "contagem-faturas-pagar");
            final long duracaoMs = System.currentTimeMillis() - inicioMs;

            // Verifica se a resposta é válida
            if (resposta == null) {
                logger.error("Erro: resposta nula ao obter contagem de faturas a pagar");
                throw new RuntimeException("Falha na requisição: resposta é null");
            }

            if (resposta.statusCode() != 200) {
                final String mensagemErro = String.format("Erro ao obter contagem de faturas a pagar. Status: %d",
                        resposta.statusCode());
                logger.error("{} ({} ms) Body: {}", mensagemErro, duracaoMs, resposta.body());
                throw new RuntimeException(mensagemErro);
            }

            // Processa a resposta JSON para extrair meta.total_count
            final JsonNode raizJson = mapeadorJson.readTree(resposta.body());
            final JsonNode metaJson = raizJson.get("meta");

            if (metaJson == null || !metaJson.has("total_count")) {
                logger.error("Estrutura JSON inválida: campo 'meta.total_count' não encontrado na resposta");
                throw new RuntimeException("Resposta da API não contém campo 'meta.total_count'");
            }

            final int totalCount = metaJson.get("total_count").asInt();
            logger.info("Contagem de faturas a pagar obtida com sucesso: {} registros ({} ms)", totalCount, duracaoMs);

            return totalCount;

        } catch (final IOException e) {
            logger.error("Erro de I/O ao obter contagem de faturas a pagar", e);
            incrementarFalhasConsecutivas("contagem-faturas-pagar");
            throw new RuntimeException("Erro ao comunicar com a API para contagem de faturas a pagar", e);
        } catch (final RuntimeException e) {
            logger.error("Erro ao obter contagem de faturas a pagar: {}", e.getMessage());
            incrementarFalhasConsecutivas("contagem-faturas-pagar");
            throw e;
        }
    }

    /**
     * Aplica delay crítico para respeitar rate limit da API.
     * Este método encapsula o Thread.sleep para suprimir warnings específicos do
     * IDE.
     * 
     * @param tipoEntidade tipo da entidade sendo processada (para logs)
     */
    @SuppressWarnings({ "java:S2925", "squid:S2142", "ThreadSleep", "SleepWhileHoldingLock",
            "CallToNativeMethodWhileLocked" })
    private void aplicarDelayRateLimit(final String tipoEntidade) {
        try {
            Thread.sleep(2200); // 2.2s = rate limit 2s + margem de segurança
            logger.debug("Delay de 2.2s aplicado antes de buscar {}", tipoEntidade);
        } catch (final InterruptedException ie) {
            Thread.currentThread().interrupt();
            logger.warn("Thread interrompida durante delay para {}", tipoEntidade);
        }
    }
}










package br.com.extrator.api;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import br.com.extrator.modelo.graphql.coletas.ColetaNodeDTO;
import br.com.extrator.modelo.graphql.fretes.FreteNodeDTO;
import br.com.extrator.util.CarregadorConfig;
import br.com.extrator.util.GerenciadorRequisicaoHttp;

/**
 * Cliente especializado para comunicação com a API GraphQL do ESL Cloud
 * Responsável por buscar dados de Coletas através de queries GraphQL
 * com proteções contra loops infinitos e circuit breaker.
 */
public class ClienteApiGraphQL {
    private static final Logger logger = LoggerFactory.getLogger(ClienteApiGraphQL.class);
    
    // PROTEÇÕES CONTRA LOOPS INFINITOS - Replicadas do ClienteApiRest
    private static final int MAX_REGISTROS_POR_EXECUCAO = 50000;
    private static final int INTERVALO_LOG_PROGRESSO = 50;
    
    // CIRCUIT BREAKER - Controle de falhas consecutivas
    private static final int MAX_FALHAS_CONSECUTIVAS = 5;
    private final Map<String, Integer> contadorFalhasConsecutivas = new HashMap<>();
    private final Set<String> entidadesComCircuitAberto = new HashSet<>();
    
    private final String urlBase;
    private final String endpointGraphQL;
    private final String token;
    private final HttpClient clienteHttp;
    private final ObjectMapper mapeadorJson;
    private final GerenciadorRequisicaoHttp gerenciadorRequisicao;
    private final Duration timeoutRequisicao;

    /**
     * Executa uma query GraphQL com paginação automática e proteções contra loops infinitos
     * 
     * @param query Query GraphQL a ser executada
     * @param nomeEntidade Nome da entidade na resposta GraphQL
     * @param variaveis Variáveis da query GraphQL
     * @param tipoClasse Classe para desserialização tipada
     * @return ResultadoExtracao indicando se a extração foi completa ou interrompida
     */
    private <T> ResultadoExtracao<T> executarQueryPaginada(String query, String nomeEntidade, Map<String, Object> variaveis, Class<T> tipoClasse) {
        String chaveEntidade = "GraphQL-" + nomeEntidade;
        
        // CIRCUIT BREAKER - Verificar se a entidade está com circuit aberto
        if (entidadesComCircuitAberto.contains(chaveEntidade)) {
            logger.warn("⚠️ CIRCUIT BREAKER ATIVO - Entidade {} temporariamente desabilitada devido a falhas consecutivas", nomeEntidade);
            return ResultadoExtracao.completo(new ArrayList<>(), 0, 0);
        }
        
        logger.info("🔍 Executando query GraphQL paginada para entidade: {}", nomeEntidade);
        
        List<T> todasEntidades = new ArrayList<>();
        String cursor = null;
        boolean hasNextPage = true;
        int paginaAtual = 1;
        int totalRegistrosProcessados = 0;
        boolean interrompido = false; // NOVO: Rastrear se foi interrompido
        
        // ✅ LER A CONFIGURAÇÃO UMA VEZ ANTES DO LOOP
        final int limitePaginas = CarregadorConfig.obterLimitePaginasApiGraphQL();

        while (hasNextPage) {
            try {
                // PROTEÇÃO 1: Limite máximo de páginas (agora usa a variável já lida)
                if (paginaAtual > limitePaginas) {
                    logger.warn("🚨 PROTEÇÃO ATIVADA - Entidade {}: Limite de {} páginas atingido. Interrompendo busca para evitar loop infinito.", 
                            nomeEntidade, limitePaginas);
                    interrompido = true; // NOVO: Marcar como interrompido
                    break;
                }

                // PROTEÇÃO 2: Limite máximo de registros
                if (totalRegistrosProcessados >= MAX_REGISTROS_POR_EXECUCAO) {
                    logger.warn("🚨 PROTEÇÃO ATIVADA - Entidade {}: Limite de {} registros atingido. Interrompendo busca para evitar sobrecarga.", 
                            nomeEntidade, MAX_REGISTROS_POR_EXECUCAO);
                    interrompido = true; // NOVO: Marcar como interrompido
                    break;
                }

                // Log de progresso a cada intervalo definido
                if (paginaAtual % INTERVALO_LOG_PROGRESSO == 0) {
                    logger.info("📊 Progresso GraphQL {}: Página {}, {} registros processados", 
                            nomeEntidade, paginaAtual, totalRegistrosProcessados);
                }

                logger.debug("Executando página {} da query GraphQL para {}", paginaAtual, nomeEntidade);
                
                // Adicionar cursor às variáveis se não for a primeira página
                Map<String, Object> variaveisComCursor = new java.util.HashMap<>(variaveis);
                if (cursor != null) {
                    variaveisComCursor.put("after", cursor);
                }

                // Executar a query para esta página
                PaginatedGraphQLResponse<T> resposta = executarQueryGraphQLTipado(query, nomeEntidade, variaveisComCursor, tipoClasse);
                
                // Adicionar entidades desta página ao resultado total
                todasEntidades.addAll(resposta.getEntidades());
                totalRegistrosProcessados += resposta.getEntidades().size();
                
                // Reset do contador de falhas em caso de sucesso
                contadorFalhasConsecutivas.put(chaveEntidade, 0);
                
                // Atualizar informações de paginação
                hasNextPage = resposta.getHasNextPage();
                cursor = resposta.getEndCursor();
                
                logger.debug("✅ Página {} processada: {} entidades encontradas. Próxima página: {} (Total: {})", 
                            paginaAtual, resposta.getEntidades().size(), hasNextPage, totalRegistrosProcessados);
                
                paginaAtual++;
                
                // Não é mais necessário pausar entre requisições - o GerenciadorRequisicaoHttp já controla o throttling
                
            } catch (Exception e) {
                logger.error("💥 Erro ao executar query GraphQL para entidade {} página {}: {}", 
                        nomeEntidade, paginaAtual, e.getMessage(), e);
                incrementarContadorFalhas(chaveEntidade, nomeEntidade);
                break;
            }
        }

        // NOVO: Retornar ResultadoExtracao baseado na flag de interrupção
        if (interrompido) {
            logger.warn("⚠️ Query GraphQL INCOMPLETA - Entidade {}: {} registros extraídos em {} páginas (INTERROMPIDA por proteções)", 
                    nomeEntidade, totalRegistrosProcessados, paginaAtual - 1);
            return ResultadoExtracao.incompleto(todasEntidades, ResultadoExtracao.MotivoInterrupcao.LIMITE_PAGINAS, paginaAtual - 1, totalRegistrosProcessados);
        } else {
            // Log final com resultado claro e diferenciado
            if (todasEntidades.isEmpty()) {
                logger.info("❌ Query GraphQL concluída - Entidade {}: Nenhum registro encontrado", nomeEntidade);
            } else {
                logger.info("✅ Query GraphQL COMPLETA - Entidade {}: {} registros extraídos em {} páginas (Proteções: ✓ Ativas)", 
                        nomeEntidade, totalRegistrosProcessados, paginaAtual - 1);
            }
            return ResultadoExtracao.completo(todasEntidades, paginaAtual - 1, totalRegistrosProcessados);
        }
    }

    /**
     * Obtém a contagem total de fretes para uma data de referência específica
     * Usa uma query GraphQL otimizada que solicita apenas o totalCount
     * 
     * @param dataReferencia Data de referência para filtrar os fretes
     * @return Número total de fretes encontrados
     * @throws RuntimeException se houver erro na requisição ou resposta inválida
     */
    public int obterContagemFretes(final LocalDate dataReferencia) {
        // Query GraphQL otimizada que solicita apenas totalCount
        String query = """
                query ContagemFretes($params: FreightInput!) {
                    freight(params: $params) {
                        totalCount
                    }
                }""";

        try {
            // Calcular o intervalo de 24 horas (mesmo padrão do buscarFretes)
            LocalDate dataInicio = dataReferencia.minusDays(1);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            String intervaloServiceAt = dataInicio.format(formatter) + " - " + dataReferencia.format(formatter);

            // Construir variáveis usando o intervalo de datas
            Map<String, Object> variaveis = Map.of(
                "params", Map.of("serviceAt", intervaloServiceAt)
            );

            logger.info("Obtendo contagem de fretes via GraphQL para período: {}", intervaloServiceAt);

            // Construir o corpo da requisição GraphQL
            ObjectNode corpoJson = mapeadorJson.createObjectNode();
            corpoJson.put("query", query);
            corpoJson.set("variables", mapeadorJson.valueToTree(variaveis));
            final String corpoRequisicao = mapeadorJson.writeValueAsString(corpoJson);

            // Construir a requisição HTTP
            HttpRequest requisicao = HttpRequest.newBuilder()
                    .uri(URI.create(urlBase + endpointGraphQL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.ofString(corpoRequisicao))
                    .timeout(this.timeoutRequisicao)
                    .build();

            // Executar a requisição
            final long inicioMs = System.currentTimeMillis();
            HttpResponse<String> resposta = gerenciadorRequisicao.executarRequisicao(
                    this.clienteHttp, requisicao, "contagem-fretes-graphql");
            final long duracaoMs = System.currentTimeMillis() - inicioMs;

            // Verificar se a resposta é válida
            if (resposta == null) {
                logger.error("Erro: resposta nula ao obter contagem de fretes via GraphQL");
                throw new RuntimeException("Falha na requisição GraphQL: resposta é null");
            }

            if (resposta.statusCode() != 200) {
                final String mensagemErro = String.format("Erro ao obter contagem de fretes via GraphQL. Status: %d", 
                    resposta.statusCode());
                logger.error("{} ({} ms) Body: {}", mensagemErro, duracaoMs, resposta.body());
                throw new RuntimeException(mensagemErro);
            }

            // Parsear a resposta JSON
            JsonNode respostaJson = mapeadorJson.readTree(resposta.body());

            // Verificar se há erros na resposta GraphQL
            if (respostaJson.has("errors")) {
                JsonNode erros = respostaJson.get("errors");
                logger.error("Erros na query GraphQL de contagem de fretes: {}", erros.toString());
                throw new RuntimeException("Erro na query GraphQL: " + erros.toString());
            }

            // Extrair totalCount da resposta
            if (!respostaJson.has("data") || !respostaJson.get("data").has("freight") || 
                !respostaJson.get("data").get("freight").has("totalCount")) {
                logger.error("Estrutura JSON inválida: campo 'data.freight.totalCount' não encontrado na resposta GraphQL");
                throw new RuntimeException("Resposta GraphQL não contém campo 'data.freight.totalCount'");
            }

            final int totalCount = respostaJson.get("data").get("freight").get("totalCount").asInt();
            logger.info("Contagem de fretes obtida com sucesso via GraphQL: {} registros ({} ms)", totalCount, duracaoMs);

            return totalCount;

        } catch (final JsonProcessingException e) {
            logger.error("Erro de processamento JSON ao obter contagem de fretes via GraphQL", e);
            incrementarContadorFalhas("contagem-fretes-graphql", "contagem-fretes");
            throw new RuntimeException("Erro ao processar JSON na contagem de fretes via GraphQL", e);
        } catch (final RuntimeException e) {
            logger.error("Erro ao obter contagem de fretes via GraphQL: {}", e.getMessage());
            incrementarContadorFalhas("contagem-fretes-graphql", "contagem-fretes");
            throw e;
        }
    }

    /**
     * Obtém a contagem total de coletas para uma data de referência específica
     * Usa uma query GraphQL otimizada que solicita apenas o totalCount
     * 
     * @param dataReferencia Data de referência para filtrar as coletas
     * @return Número total de coletas encontradas
     * @throws RuntimeException se houver erro na requisição ou resposta inválida
     */
    public int obterContagemColetas(final LocalDate dataReferencia) {
        // Query GraphQL otimizada que solicita apenas totalCount
        String query = """
                query ContagemColetas($params: PickInput!) {
                    pick(params: $params) {
                        totalCount
                    }
                }""";

        try {
            // Construir variáveis usando exclusivamente requestDate (mesmo padrão do buscarColetas)
            String dataFormatada = formatarDataParaApiGraphQL(dataReferencia);
            Map<String, Object> variaveis = Map.of(
                "params", Map.of("requestDate", dataFormatada)
            );

            logger.info("Obtendo contagem de coletas via GraphQL para data: {}", dataFormatada);

            // Construir o corpo da requisição GraphQL
            ObjectNode corpoJson = mapeadorJson.createObjectNode();
            corpoJson.put("query", query);
            corpoJson.set("variables", mapeadorJson.valueToTree(variaveis));
            final String corpoRequisicao = mapeadorJson.writeValueAsString(corpoJson);

            // Construir a requisição HTTP
            HttpRequest requisicao = HttpRequest.newBuilder()
                    .uri(URI.create(urlBase + endpointGraphQL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.ofString(corpoRequisicao))
                    .timeout(this.timeoutRequisicao)
                    .build();

            // Executar a requisição
            final long inicioMs = System.currentTimeMillis();
            HttpResponse<String> resposta = gerenciadorRequisicao.executarRequisicao(
                    this.clienteHttp, requisicao, "contagem-coletas-graphql");
            final long duracaoMs = System.currentTimeMillis() - inicioMs;

            // Verificar se a resposta é válida
            if (resposta == null) {
                logger.error("Erro: resposta nula ao obter contagem de coletas via GraphQL");
                throw new RuntimeException("Falha na requisição GraphQL: resposta é null");
            }

            if (resposta.statusCode() != 200) {
                final String mensagemErro = String.format("Erro ao obter contagem de coletas via GraphQL. Status: %d", 
                    resposta.statusCode());
                logger.error("{} ({} ms) Body: {}", mensagemErro, duracaoMs, resposta.body());
                throw new RuntimeException(mensagemErro);
            }

            // Parsear a resposta JSON
            JsonNode respostaJson = mapeadorJson.readTree(resposta.body());

            // Verificar se há erros na resposta GraphQL
            if (respostaJson.has("errors")) {
                JsonNode erros = respostaJson.get("errors");
                logger.error("Erros na query GraphQL de contagem de coletas: {}", erros.toString());
                throw new RuntimeException("Erro na query GraphQL: " + erros.toString());
            }

            // Extrair totalCount da resposta
            if (!respostaJson.has("data") || !respostaJson.get("data").has("pick") || 
                !respostaJson.get("data").get("pick").has("totalCount")) {
                logger.error("Estrutura JSON inválida: campo 'data.pick.totalCount' não encontrado na resposta GraphQL");
                throw new RuntimeException("Resposta GraphQL não contém campo 'data.pick.totalCount'");
            }

            final int totalCount = respostaJson.get("data").get("pick").get("totalCount").asInt();
            logger.info("Contagem de coletas obtida com sucesso via GraphQL: {} registros ({} ms)", totalCount, duracaoMs);

            return totalCount;

        } catch (final JsonProcessingException e) {
            logger.error("Erro de processamento JSON ao obter contagem de coletas via GraphQL", e);
            incrementarContadorFalhas("contagem-coletas-graphql", "contagem-coletas");
            throw new RuntimeException("Erro ao processar JSON na contagem de coletas via GraphQL", e);
        } catch (final RuntimeException e) {
            logger.error("Erro ao obter contagem de coletas via GraphQL: {}", e.getMessage());
            incrementarContadorFalhas("contagem-coletas-graphql", "contagem-coletas");
            throw e;
        }
    }

    /**
     * Construtor da classe ClienteApiGraphQL
     * Inicializa as configurações necessárias para comunicação com a API GraphQL
     */
    public ClienteApiGraphQL() {
        this.urlBase = CarregadorConfig.obterUrlBaseApi();
        this.endpointGraphQL = CarregadorConfig.obterEndpointGraphQL();
        this.token = CarregadorConfig.obterTokenApiGraphQL();
        this.timeoutRequisicao = CarregadorConfig.obterTimeoutApiRest();
        this.clienteHttp = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.mapeadorJson = new ObjectMapper();
        this.gerenciadorRequisicao = new GerenciadorRequisicaoHttp();
    }

    /**
     * Busca coletas via GraphQL para as últimas 24h (ontem + hoje)
     * API GraphQL de coletas (PickInput) SÓ aceita 1 data específica em requestDate, não aceita intervalo.
     * Para obter coletas das últimas 24h, precisa buscar 2 dias separadamente.
     * 
     * @param dataReferencia Data de referência para buscar as coletas (LocalDate)
     * @return ResultadoExtracao indicando se a busca foi completa ou interrompida
     */
    public ResultadoExtracao<ColetaNodeDTO> buscarColetas(LocalDate dataReferencia) {
        String query = """
                query BuscarColetas($params: PickInput!, $after: String) {
                    pick(params: $params, after: $after, first: 100) {
                        edges {
                            cursor
                            node {
                                id
                                agentId
                                cancellationReason
                                cancellationUserId
                                cargoClassificationId
                                comments
                                costCenterId
                                destroyReason
                                destroyUserId
                                invoicesCubedWeight
                                invoicesValue
                                invoicesVolumes
                                invoicesWeight
                                lunchBreakEndHour
                                lunchBreakStartHour
                                notificationEmail
                                notificationPhone
                                pickTypeId
                                pickupLocationId
                                requestDate
                                requestHour
                                requester
                                sequenceCode
                                serviceDate
                                serviceEndHour
                                serviceStartHour
                                status
                                statusUpdatedAt
                                taxedWeight
                                vehicleTypeId
                            }
                        }
                        pageInfo {
                            hasNextPage
                            endCursor
                        }
                    }
                }""";

        // Calcular dia anterior (ontem)
        LocalDate diaAnterior = dataReferencia.minusDays(1);
        
        // Lista consolidada para armazenar todas as coletas
        List<ColetaNodeDTO> todasColetas = new ArrayList<>();
        int totalPaginas = 0;
        boolean ambasCompletas = true;

        // 1. Buscar coletas do dia anterior (ontem)
        logger.info("🔍 Coletas - Dia 1/2: {}", diaAnterior);
        String dataAnteriorFormatada = formatarDataParaApiGraphQL(diaAnterior);
        Map<String, Object> variaveisDiaAnterior = Map.of(
            "params", Map.of("requestDate", dataAnteriorFormatada)
        );
        
        ResultadoExtracao<ColetaNodeDTO> resultadoDiaAnterior = executarQueryPaginada(query, "pick", variaveisDiaAnterior, ColetaNodeDTO.class);
        todasColetas.addAll(resultadoDiaAnterior.getDados());
        totalPaginas += resultadoDiaAnterior.getPaginasProcessadas();
        
        if (resultadoDiaAnterior.isCompleto()) {
            logger.info("✅ Dia 1/2: {} coletas", resultadoDiaAnterior.getDados().size());
        } else {
            logger.warn("⚠️ Dia 1/2: {} coletas (INCOMPLETO)", resultadoDiaAnterior.getDados().size());
            ambasCompletas = false;
        }

        // 2. Buscar coletas do dia atual (hoje)
        logger.info("🔍 Coletas - Dia 2/2: {}", dataReferencia);
        String dataAtualFormatada = formatarDataParaApiGraphQL(dataReferencia);
        Map<String, Object> variaveisDataAtual = Map.of(
            "params", Map.of("requestDate", dataAtualFormatada)
        );
        
        ResultadoExtracao<ColetaNodeDTO> resultadoDataAtual = executarQueryPaginada(query, "pick", variaveisDataAtual, ColetaNodeDTO.class);
        todasColetas.addAll(resultadoDataAtual.getDados());
        totalPaginas += resultadoDataAtual.getPaginasProcessadas();
        
        if (resultadoDataAtual.isCompleto()) {
            logger.info("✅ Dia 2/2: {} coletas", resultadoDataAtual.getDados().size());
        } else {
            logger.warn("⚠️ Dia 2/2: {} coletas (INCOMPLETO)", resultadoDataAtual.getDados().size());
            ambasCompletas = false;
        }

        // 3. Consolidar resultados
        logger.info("✅ Total: {} coletas", todasColetas.size());

        // 4. Retornar resultado consolidado
        if (ambasCompletas) {
            return ResultadoExtracao.completo(todasColetas, totalPaginas, todasColetas.size());
        } else {
            // Se qualquer uma das buscas foi incompleta, marcar o resultado como incompleto
            ResultadoExtracao.MotivoInterrupcao motivo = ResultadoExtracao.MotivoInterrupcao.LIMITE_PAGINAS;
            return ResultadoExtracao.incompleto(todasColetas, motivo, totalPaginas, todasColetas.size());
        }
    }





    /**
     * Busca fretes via GraphQL para as últimas 24 horas a partir de uma data de referência.
     * 
     * @param dataReferencia Data de referência que representa o FIM do intervalo de busca.
     * @return ResultadoExtracao indicando se a busca foi completa ou interrompida
     */
    public ResultadoExtracao<FreteNodeDTO> buscarFretes(LocalDate dataReferencia) {
        // A query GraphQL permanece a mesma, pois já está correta.
        String query = """
                query BuscarFretes($params: FreightInput!, $after: String) {
                    freight(params: $params, after: $after, first: 100) {
                        edges {
                            cursor
                            node {
                                id
                                accountingCreditId
                                accountingCreditInstallmentId
                                adValoremSubtotal
                                additionalsSubtotal
                                admFeeSubtotal
                                calculationType
                                collectSubtotal
                                comments
                                corporationId
                                costCenterId
                                createdAt
                                cubagesCubedWeight
                                customerPriceTableId
                                deliveryDeadlineInDays
                                deliveryPredictionDate
                                deliveryPredictionHour
                                deliveryRegionId
                                deliverySubtotal
                                destinationCityId
                                dispatchSubtotal
                                draftEmissionAt
                                emergencySubtotal
                                emissionType
                                finishedAt
                                freightClassificationId
                                freightCubagesCount
                                freightInvoicesCount
                                freightWeightSubtotal
                                globalized
                                globalizedType
                                grisSubtotal
                                insuranceAccountableType
                                insuranceEnabled
                                insuranceId
                                insuredValue
                                invoicesTotalVolumes
                                invoicesValue
                                invoicesWeight
                                itrSubtotal
                                km
                                modal
                                modalCte
                                nfseNumber
                                nfseSeries
                                otherFees
                                paymentAccountableType
                                paymentType
                                previousDocumentType
                                priceTableAccountableType
                                productsValue
                                realWeight
                                redispatchSubtotal
                                referenceNumber
                                secCatSubtotal
                                serviceAt
                                serviceDate
                                serviceType
                                status
                                subtotal
                                suframaSubtotal
                                taxedWeight
                                tdeSubtotal
                                tollSubtotal
                                total
                                totalCubicVolume
                                trtSubtotal
                                type
                            }
                        }
                        pageInfo {
                            hasNextPage
                            endCursor
                        }
                    }
                }""";

        // --- INÍCIO DAS MUDANÇAS ---

        // 1. Calcular o intervalo de 24 horas
        LocalDate dataInicio = dataReferencia.minusDays(1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String intervaloServiceAt = dataInicio.format(formatter) + " - " + dataReferencia.format(formatter);

        // 2. Construir variáveis SEM o corporationId, usando o intervalo de datas
        Map<String, Object> variaveis = Map.of(
            "params", Map.of("serviceAt", intervaloServiceAt)
        );

        // 3. Atualizar os logs para refletir a nova busca
        logger.info("🔍 Buscando fretes via GraphQL - Período: {}", intervaloServiceAt);
        logger.debug("Executando query GraphQL para fretes - URL: {}{}, Variáveis: {}", 
                    urlBase, endpointGraphQL, variaveis);

        ResultadoExtracao<FreteNodeDTO> resultado = executarQueryPaginada(query, "freight", variaveis, FreteNodeDTO.class);
        
        // 4. Atualizar o log de resultado
        if (resultado.getDados().isEmpty()) {
            logger.warn("❌ Sem fretes encontrados para o período {}", intervaloServiceAt);
        } else {
            logger.info("✅ Encontrados {} fretes para o período {}", resultado.getDados().size(), intervaloServiceAt);
        }
        
        // --- FIM DAS MUDANÇAS ---
        
        return resultado;
    }

    /**
     * Executa uma query GraphQL de forma genérica e robusta com desserialização tipada
     * 
     * @param query        A query GraphQL a ser executada
     * @param nomeEntidade Nome da entidade para logs e tratamento de erros
     * @param variaveis    Variáveis da query GraphQL
     * @param tipoClasse   Classe para desserialização tipada
     * @return Resposta paginada contendo entidades tipadas e informações de paginação
     */
    private <T> PaginatedGraphQLResponse<T> executarQueryGraphQLTipado(String query, String nomeEntidade,
            Map<String, Object> variaveis, Class<T> tipoClasse) {
        logger.debug("Executando query GraphQL tipada para {} - URL: {}{}, Variáveis: {}", 
                    nomeEntidade, urlBase, endpointGraphQL, variaveis);
        List<T> entidades = new ArrayList<>();
        boolean hasNextPage = false;
        String endCursor = null;

        // Validação básica de configuração
        if (urlBase == null || urlBase.isBlank() || token == null || token.isBlank()) {
            logger.error("Configurações inválidas para chamada GraphQL (urlBase/token)");
            return new PaginatedGraphQLResponse<>(entidades, false, null);
        }

        try {
            // Construir o corpo da requisição GraphQL usando ObjectMapper
            ObjectNode corpoJson = mapeadorJson.createObjectNode();
            corpoJson.put("query", query);
            if (variaveis != null && !variaveis.isEmpty()) {
                corpoJson.set("variables", mapeadorJson.valueToTree(variaveis));
            }
            final String corpoRequisicao = mapeadorJson.writeValueAsString(corpoJson);

            // Construir a requisição HTTP
            HttpRequest requisicao = HttpRequest.newBuilder()
                    .uri(URI.create(urlBase + endpointGraphQL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.ofString(corpoRequisicao))
                    .timeout(this.timeoutRequisicao)
                    .build();

            // Executar a requisição usando o gerenciador central
            HttpResponse<String> resposta = gerenciadorRequisicao.executarRequisicao(this.clienteHttp, requisicao, "GraphQL-" + nomeEntidade);

            // Parsear a resposta JSON
            JsonNode respostaJson = mapeadorJson.readTree(resposta.body());

            // Verificar se há erros na resposta GraphQL
            if (respostaJson.has("errors")) {
                JsonNode erros = respostaJson.get("errors");
                logger.error("Erros na query GraphQL para {}: {}", nomeEntidade, erros.toString());
                return new PaginatedGraphQLResponse<>(entidades, false, null);
            }

            // Extrair os dados da resposta
            if (!respostaJson.has("data")) {
                logger.warn("Resposta GraphQL sem campo 'data' para {}", nomeEntidade);
                return new PaginatedGraphQLResponse<>(entidades, false, null);
            }

            JsonNode dados = respostaJson.get("data");
            if (!dados.has(nomeEntidade)) {
                logger.warn("Campo '{}' não encontrado na resposta GraphQL. Campos disponíveis: {}",
                        nomeEntidade, dados.fieldNames());
                return new PaginatedGraphQLResponse<>(entidades, false, null);
            }

            JsonNode dadosEntidade = dados.get(nomeEntidade);

            // Verificar se a resposta segue o padrão paginado com edges/node
            if (dadosEntidade.has("edges")) {
                logger.debug("Processando resposta paginada com edges/node para {}", nomeEntidade);
                JsonNode edges = dadosEntidade.get("edges");

                if (edges.isArray()) {
                    for (JsonNode edge : edges) {
                        if (edge.has("node")) {
                            JsonNode node = edge.get("node");
                            try {
                                // Deserializa diretamente para a classe tipada usando Jackson
                                T entidade = mapeadorJson.treeToValue(node, tipoClasse);
                                entidades.add(entidade);
                            } catch (JsonProcessingException | IllegalArgumentException e) {
                                logger.warn("Erro ao deserializar node de {} para {}: {}", 
                                          nomeEntidade, tipoClasse.getSimpleName(), e.getMessage());
                            }
                        }
                    }
                }
                
                // Extrair informações de paginação do pageInfo
                if (dadosEntidade.has("pageInfo")) {
                    JsonNode pageInfo = dadosEntidade.get("pageInfo");
                    if (pageInfo.has("hasNextPage")) {
                        hasNextPage = pageInfo.get("hasNextPage").asBoolean();
                    }
                    if (pageInfo.has("endCursor") && !pageInfo.get("endCursor").isNull()) {
                        endCursor = pageInfo.get("endCursor").asText();
                    }
                    logger.debug("Informações de paginação extraídas - hasNextPage: {}, endCursor: {}", hasNextPage, endCursor);
                }
            } else {
                // Processar resposta no formato antigo (array direto) para compatibilidade
                logger.debug("Processando resposta no formato antigo (array direto) para {}", nomeEntidade);

                if (dadosEntidade.isArray()) {
                    for (JsonNode item : dadosEntidade) {
                        try {
                            // Deserializa diretamente para a classe tipada usando Jackson
                            T entidade = mapeadorJson.treeToValue(item, tipoClasse);
                            entidades.add(entidade);
                        } catch (JsonProcessingException | IllegalArgumentException e) {
                            logger.warn("Erro ao deserializar item de {} para {}: {}", 
                                      nomeEntidade, tipoClasse.getSimpleName(), e.getMessage());
                        }
                    }
                }
            }

            logger.debug("Query GraphQL tipada concluída para {}. Total encontrado: {}", nomeEntidade, entidades.size());

        } catch (JsonProcessingException e) {
            logger.error("Erro de processamento JSON durante execução da query GraphQL para {}: {}", nomeEntidade, e.getMessage(), e);
        } catch (RuntimeException e) {
            logger.error("Erro durante execução da query GraphQL para {}: {}", nomeEntidade, e.getMessage(), e);
        }

        return new PaginatedGraphQLResponse<>(entidades, hasNextPage, endCursor);
    }

    /**
     * Valida se as credenciais de acesso à API GraphQL estão funcionando
     * 
     * @return true se a validação foi bem-sucedida, false caso contrário
     */
    public boolean validarAcessoApi() {
        logger.info("Validando acesso à API GraphQL...");

        try {
            // Query simples para testar a conectividade
            String queryTeste = "{ __schema { queryType { name } } }";

            // Construir o corpo da requisição GraphQL usando ObjectMapper
            ObjectNode corpoJson = mapeadorJson.createObjectNode();
            corpoJson.put("query", queryTeste);
            String corpoRequisicao = mapeadorJson.writeValueAsString(corpoJson);

            String url = urlBase + endpointGraphQL;
            HttpRequest requisicao = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(corpoRequisicao))
                    .build();

            HttpResponse<String> resposta = clienteHttp.send(requisicao, HttpResponse.BodyHandlers.ofString());

            if (resposta.statusCode() == 200) {
                JsonNode respostaJson = mapeadorJson.readTree(resposta.body());
                boolean sucesso = !respostaJson.has("errors");

                if (sucesso) {
                    logger.info("✅ Validação da API GraphQL bem-sucedida");
                } else {
                    logger.error("❌ Erro na validação da API GraphQL: {}", respostaJson.get("errors"));
                }

                return sucesso;
            } else {
                logger.error("❌ Falha na validação da API GraphQL. Status: {}", resposta.statusCode());
                return false;
            }

        } catch (java.io.IOException | InterruptedException e) {
            logger.error("❌ Erro durante validação da API GraphQL: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Formatar LocalDate para o formato esperado pela API GraphQL (YYYY-MM-DD).
     * 
     * @param data A data a ser formatada
     * @return String no formato YYYY-MM-DD
     */
    private String formatarDataParaApiGraphQL(LocalDate data) {
        return data.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    /**
     * Incrementa o contador de falhas consecutivas e ativa o circuit breaker se necessário.
     * 
     * @param chaveEntidade Chave identificadora da entidade GraphQL
     * @param nomeEntidade Nome amigável da entidade para logs
     */
    private void incrementarContadorFalhas(String chaveEntidade, String nomeEntidade) {
        int falhas = contadorFalhasConsecutivas.getOrDefault(chaveEntidade, 0) + 1;
        contadorFalhasConsecutivas.put(chaveEntidade, falhas);
        
        if (falhas >= MAX_FALHAS_CONSECUTIVAS) {
            entidadesComCircuitAberto.add(chaveEntidade);
            logger.error("🚨 CIRCUIT BREAKER ATIVADO - Entidade {} ({}): {} falhas consecutivas. Entidade temporariamente desabilitada.", 
                    chaveEntidade, nomeEntidade, falhas);
        } else {
            logger.warn("⚠️ Falha {}/{} para entidade {} ({})", falhas, MAX_FALHAS_CONSECUTIVAS, chaveEntidade, nomeEntidade);
        }
    }
}






package br.com.extrator.api;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import br.com.extrator.modelo.dataexport.cotacao.CotacaoDTO;
import br.com.extrator.modelo.dataexport.localizacaocarga.LocalizacaoCargaDTO;
import br.com.extrator.modelo.dataexport.manifestos.ManifestoDTO;
import br.com.extrator.util.CarregadorConfig;
import br.com.extrator.util.GerenciadorRequisicaoHttp;

/**
 * Cliente para extração de dados da API Data Export do ESL Cloud.
 * 
 * @author Sistema de Extração ESL Cloud
 * @version 2.0
 */
public class ClienteApiDataExport {
    private static final Logger logger = LoggerFactory.getLogger(ClienteApiDataExport.class);

    // Atributos da classe
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String urlBase;
    private final String token;
    private final int templateIdManifestos;
    private final int templateIdLocalizacaoCarga;
    private final int templateIdCotacoes;
    private final GerenciadorRequisicaoHttp gerenciadorRequisicao;
    private final Duration timeoutRequisicao;

    // PROTEÇÕES CONTRA LOOPS INFINITOS - Replicadas do ClienteApiRest
    private static final int MAX_REGISTROS_POR_EXECUCAO = 10000;
    private static final int INTERVALO_LOG_PROGRESSO = 10; // A cada 10 páginas

    // CIRCUIT BREAKER
    private final Map<String, Integer> contadorFalhasConsecutivas = new HashMap<>();
    private final Set<String> templatesComCircuitAberto = new HashSet<>();
    private static final int MAX_FALHAS_CONSECUTIVAS = 5;

    // Template IDs padrão para cada tipo de dados
    private static final int TEMPLATE_ID_MANIFESTOS = 6399;
    private static final int TEMPLATE_ID_LOCALIZACAO_CARGA = 8656;
    private static final int TEMPLATE_ID_COTACOES = 6906;

    // Campos de data corretos para cada template (descobertos via Postman)
    private static final String CAMPO_MANIFESTOS = "service_date";
    private static final String CAMPO_COTACOES = "requested_at";
    private static final String CAMPO_LOCALIZACAO_CARGA = "service_at";
    
    // Constantes para nomes de// Nomes das tabelas conforme API DataExport
    private static final String TABELA_MANIFESTOS = "manifests";
    private static final String TABELA_COTACOES = "quotes";
    private static final String TABELA_LOCALIZACAO_CARGA = "freights";

    /**
     * Construtor que inicializa o cliente da API Data Export.
     * Carrega as configurações necessárias e inicializa os componentes HTTP.
     */
    public ClienteApiDataExport() {
        logger.info("Inicializando cliente da API Data Export");

        // Inicializa HttpClient e ObjectMapper
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper();

        // Carrega configurações usando CarregadorConfig
        this.urlBase = CarregadorConfig.obterUrlBaseApi();
        this.token = CarregadorConfig.obterTokenApiDataExport();
        this.timeoutRequisicao = CarregadorConfig.obterTimeoutApiRest();

        // Valida configurações obrigatórias
        if (urlBase == null || urlBase.trim().isEmpty()) {
            throw new IllegalStateException("URL base da API não configurada");
        }
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalStateException("Token da API Data Export não configurado");
        }

        // Inicializa IDs de template permitindo sobrescrita via env/properties
        this.templateIdManifestos = carregarTemplateId(
                "API_DATAEXPORT_TEMPLATE_MANIFESTOS",
                "api.dataexport.template.manifestos",
                TEMPLATE_ID_MANIFESTOS);
        this.templateIdLocalizacaoCarga = carregarTemplateId(
                "API_DATAEXPORT_TEMPLATE_LOCALIZACAO",
                "api.dataexport.template.localizacao",
                TEMPLATE_ID_LOCALIZACAO_CARGA);
        this.templateIdCotacoes = carregarTemplateId(
                "API_DATAEXPORT_TEMPLATE_COTACOES",
                "api.dataexport.template.cotacoes",
                TEMPLATE_ID_COTACOES);
        logger.debug(
                "Template IDs configurados: manifestos={}, localizacao={}, cotacoes={}",
                templateIdManifestos, templateIdLocalizacaoCarga, templateIdCotacoes);

        // Inicializa o gerenciador de requisições HTTP
        this.gerenciadorRequisicao = new GerenciadorRequisicaoHttp();

        logger.info("Cliente da API Data Export inicializado com sucesso");
        logger.debug("URL base configurada: {}", urlBase);
    }

    /**
     * Busca dados de manifestos da API Data Export usando fluxo síncrono (resposta
     * JSON).
     * 
     * @return ResultadoExtracao indicando se a busca foi completa ou interrompida
     */
    public ResultadoExtracao<ManifestoDTO> buscarManifestos() {
        logger.info("Buscando manifestos da API DataExport (últimas 24h)");
        Instant agora = Instant.now();
        Instant ontem = agora.minusSeconds(24 * 60 * 60);
        return buscarDadosGenericos(templateIdManifestos, TABELA_MANIFESTOS, CAMPO_MANIFESTOS,
                new TypeReference<List<ManifestoDTO>>() {}, ontem, agora);
    }

    /**
     * Busca dados de cotações da API Data Export usando fluxo síncrono (resposta
     * JSON).
     * 
     * @return ResultadoExtracao indicando se a busca foi completa ou interrompida
     */
    public ResultadoExtracao<CotacaoDTO> buscarCotacoes() {
        logger.info("Buscando cotações da API DataExport (últimas 24h)");
        Instant agora = Instant.now();
        Instant ontem = agora.minusSeconds(24 * 60 * 60);
        return buscarDadosGenericos(templateIdCotacoes, TABELA_COTACOES, CAMPO_COTACOES,
                new TypeReference<List<CotacaoDTO>>() {}, ontem, agora);
    }

    /**
     * Busca dados de localização de carga da API Data Export usando fluxo síncrono
     * (resposta JSON).
     * 
     * @return ResultadoExtracao indicando se a busca foi completa ou interrompida
     */
    public ResultadoExtracao<LocalizacaoCargaDTO> buscarLocalizacaoCarga() {
        logger.info("Buscando localização de carga da API DataExport (últimas 24h)");
        Instant agora = Instant.now();
        Instant ontem = agora.minusSeconds(24 * 60 * 60);
        return buscarDadosGenericos(templateIdLocalizacaoCarga, TABELA_LOCALIZACAO_CARGA, CAMPO_LOCALIZACAO_CARGA,
                new TypeReference<List<LocalizacaoCargaDTO>>() {}, ontem, agora);
    }

    /**
     * Método genérico para buscar dados de qualquer template da API Data Export
     * com proteções contra loops infinitos e circuit breaker.
     * 
     * @param templateId   ID do template na API Data Export
     * @param nomeTabela   Nome da tabela para filtros
     * @param campoData    Campo de data para filtros
     * @param typeReference Referência de tipo para desserialização
     * @param dataInicio   Data de início do período
     * @param dataFim      Data de fim do período
     * @return ResultadoExtracao indicando se a busca foi completa ou interrompida
     */
    private <T> ResultadoExtracao<T> buscarDadosGenericos(int templateId, String nomeTabela, String campoData,
            TypeReference<List<T>> typeReference, Instant dataInicio, Instant dataFim) {
        
        // Determina o nome amigável do tipo de dados baseado na tabela
        String tipoAmigavel = obterNomeAmigavelTipo(nomeTabela);
        String chaveTemplate = "Template-" + templateId;
        
        // CIRCUIT BREAKER - Verificar se o template está com circuit aberto
        if (templatesComCircuitAberto.contains(chaveTemplate)) {
            logger.warn("⚠️ CIRCUIT BREAKER ATIVO - Template {} ({}) temporariamente desabilitado devido a falhas consecutivas", 
                    templateId, tipoAmigavel);
            return ResultadoExtracao.completo(new ArrayList<>(), 0, 0);
        }
        
        logger.info("🔍 Executando busca genérica - Template: {} ({}), Período: {} a {}", 
                templateId, tipoAmigavel, 
                dataInicio.atZone(java.time.ZoneOffset.UTC).toLocalDate(), 
                dataFim.atZone(java.time.ZoneOffset.UTC).toLocalDate());

        List<T> resultadosFinais = new ArrayList<>();
        int paginaAtual = 1;
        boolean haMaisPaginas;
        int totalRegistrosProcessados = 0;
        boolean interrompido = false; // Rastrear se a extração foi interrompida

        do {
            try {
                // PROTEÇÃO 1: Limite máximo de páginas
                int limitePaginas = CarregadorConfig.obterLimitePaginasApiDataExport();
                if (paginaAtual > limitePaginas) {
                    logger.warn("🚨 PROTEÇÃO ATIVADA - Template {} ({}): Limite de {} páginas atingido. Interrompendo busca para evitar loop infinito.", 
                            templateId, tipoAmigavel, limitePaginas);
                    interrompido = true;
                    break;
                }

                // PROTEÇÃO 2: Limite máximo de registros
                if (totalRegistrosProcessados >= MAX_REGISTROS_POR_EXECUCAO) {
                    logger.warn("🚨 PROTEÇÃO ATIVADA - Template {} ({}): Limite de {} registros atingido. Interrompendo busca para evitar sobrecarga.", 
                            templateId, tipoAmigavel, MAX_REGISTROS_POR_EXECUCAO);
                    interrompido = true;
                    break;
                }

                // Log de progresso a cada intervalo definido
                if (paginaAtual % INTERVALO_LOG_PROGRESSO == 0) {
                    logger.info("📊 Progresso Template {} ({}): Página {}, {} registros processados", 
                            templateId, tipoAmigavel, paginaAtual, totalRegistrosProcessados);
                }

                // URL base limpa sem parâmetros de query (filtros e paginação vão no corpo JSON)
                String url = String.format("%s/api/analytics/reports/%d/data", urlBase, templateId);

                // Constrói o corpo JSON com search, page, per conforme formato do Postman
                String corpoJson = construirCorpoRequisicao(nomeTabela, campoData, dataInicio, dataFim, paginaAtual);

                logger.debug("Enviando requisição para URL: {} com corpo: {}", url, corpoJson);

                HttpRequest requisicao = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Authorization", "Bearer " + token)
                        .header("Content-Type", "application/json")
                        .timeout(this.timeoutRequisicao)
                        .method("GET", HttpRequest.BodyPublishers.ofString(corpoJson)) // GET com corpo JSON
                        .build();

                HttpResponse<String> resposta = gerenciadorRequisicao.executarRequisicao(this.httpClient, requisicao, 
                        "DataExport-Template-" + templateId + "-Page-" + paginaAtual);

                List<T> resultadosDaPagina = Collections.emptyList();
                if (resposta.statusCode() == 200) {
                    JsonNode raizJson = objectMapper.readTree(resposta.body());
                    JsonNode dadosNode = raizJson.has("data") ? raizJson.get("data") : raizJson;

                    if (dadosNode != null && dadosNode.isArray() && dadosNode.size() > 0) {
                        resultadosDaPagina = objectMapper.convertValue(dadosNode, typeReference);
                        resultadosFinais.addAll(resultadosDaPagina);
                        totalRegistrosProcessados += resultadosDaPagina.size();
                        
                        // Reset do contador de falhas em caso de sucesso
                        contadorFalhasConsecutivas.put(chaveTemplate, 0);
                        
                        logger.debug("✅ Página {} processada: {} registros (Total: {})", 
                                paginaAtual, resultadosDaPagina.size(), totalRegistrosProcessados);
                    } else {
                        logger.info("📄 Página {}: sem dados, fim da paginação", paginaAtual);
                    }
                } else {
                    logger.error("❌ A requisição para a página {} do template {} falhou com status {}. Body: {}", 
                            paginaAtual, templateId, resposta.statusCode(), resposta.body());
                    
                    // Incrementar contador de falhas
                    incrementarContadorFalhas(chaveTemplate, tipoAmigavel);
                    break;
                }

                haMaisPaginas = !resultadosDaPagina.isEmpty();
                paginaAtual++;

            } catch (java.io.IOException e) {
                logger.error("💥 Erro de I/O ou processamento JSON ao buscar dados do template {} página {}: {}", 
                        templateId, paginaAtual, e.getMessage(), e);
                incrementarContadorFalhas(chaveTemplate, tipoAmigavel);
                break;
            } catch (IllegalArgumentException e) {
                logger.error("💥 Argumento inválido ao buscar dados do template {} página {}: {}", 
                        templateId, paginaAtual, e.getMessage(), e);
                incrementarContadorFalhas(chaveTemplate, tipoAmigavel);
                break;
            } catch (RuntimeException e) {
                logger.error("💥 Erro de execução ao buscar dados do template {} página {}: {}", 
                        templateId, paginaAtual, e.getMessage(), e);
                incrementarContadorFalhas(chaveTemplate, tipoAmigavel);
                break;
            }
        } while (haMaisPaginas);

        // Log final com resultado claro e diferenciado
        if (resultadosFinais.isEmpty()) {
            logger.info("❌ Busca concluída - Template {}: Nenhum {} encontrado no período especificado", 
                    templateId, tipoAmigavel.toLowerCase());
        } else {
            String statusExtracao = interrompido ? "INCOMPLETA" : "COMPLETA";
            logger.info("✅ Busca {} - Template {}: {} {} extraídos em {} páginas (Proteções: ✓ Ativas)", 
                    statusExtracao, templateId, totalRegistrosProcessados, tipoAmigavel.toLowerCase(), paginaAtual - 1);
        }
        
        // Retornar ResultadoExtracao baseado no status da interrupção
        if (interrompido) {
            return ResultadoExtracao.incompleto(resultadosFinais, ResultadoExtracao.MotivoInterrupcao.LIMITE_PAGINAS, 
                    paginaAtual - 1, totalRegistrosProcessados);
        } else {
            return ResultadoExtracao.completo(resultadosFinais, paginaAtual - 1, totalRegistrosProcessados);
        }
    }

    /**
     * Incrementa o contador de falhas consecutivas e ativa o circuit breaker se necessário.
     * 
     * @param chaveTemplate Chave identificadora do template
     * @param tipoAmigavel Nome amigável do tipo para logs
     */
    private void incrementarContadorFalhas(String chaveTemplate, String tipoAmigavel) {
        int falhas = contadorFalhasConsecutivas.getOrDefault(chaveTemplate, 0) + 1;
        contadorFalhasConsecutivas.put(chaveTemplate, falhas);
        
        if (falhas >= MAX_FALHAS_CONSECUTIVAS) {
            templatesComCircuitAberto.add(chaveTemplate);
            logger.error("🚨 CIRCUIT BREAKER ATIVADO - Template {} ({}): {} falhas consecutivas. Template temporariamente desabilitado.", 
                    chaveTemplate, tipoAmigavel, falhas);
        } else {
            logger.warn("⚠️ Falha {}/{} para template {} ({})", falhas, MAX_FALHAS_CONSECUTIVAS, chaveTemplate, tipoAmigavel);
        }
    }

    /**
     * Determina o nome amigável do tipo de dados baseado no nome da tabela.
     * 
     * @param nomeTabela Nome da tabela da API
     * @return Nome amigável para logs
     */
    private String obterNomeAmigavelTipo(String nomeTabela) {
        return switch (nomeTabela) {
            case TABELA_MANIFESTOS -> "Manifestos";
            case TABELA_COTACOES -> "Cotações";
            case TABELA_LOCALIZACAO_CARGA -> "Localizações de Carga";
            default -> "Dados";
        };
    }

    /**
     * Carrega o ID do template a partir de variáveis de ambiente ou propriedades do sistema.
     * 
     * @param envName Nome da variável de ambiente
     * @param propKey Chave da propriedade do sistema
     * @param padrao  Valor padrão caso não seja encontrado
     * @return ID do template configurado ou valor padrão
     */
    private int carregarTemplateId(String envName, String propKey, int padrao) {
        // Tenta primeiro obter da variável de ambiente
        String valorEnv = System.getenv(envName);
        if (valorEnv != null && !valorEnv.trim().isEmpty()) {
            try {
                return Integer.parseInt(valorEnv.trim());
            } catch (NumberFormatException e) {
                logger.warn("Valor inválido na variável de ambiente {}: '{}'. Tentando arquivo de configuração.",
                        envName, valorEnv);
            }
        }

        // Fallback para o arquivo config.properties usando CarregadorConfig
        String valorProp = CarregadorConfig.obterPropriedade(propKey);
        if (valorProp != null && !valorProp.trim().isEmpty()) {
            try {
                return Integer.parseInt(valorProp.trim());
            } catch (NumberFormatException e) {
                logger.warn("Valor inválido na propriedade {}: '{}'. Usando valor padrão {}.",
                        propKey, valorProp, padrao);
            }
        }

        logger.info("Template ID não configurado (env: {}, prop: {}). Usando valor padrão: {}",
                envName, propKey, padrao);
        return padrao;
    }

    /**
     * Constrói o corpo JSON da requisição conforme formato esperado pela API DataExport.
     * Formato: {"search": {"nomeTabela": {"campoData": "yyyy-MM-dd - yyyy-MM-dd"}}, "page": "1", "per": "100"}
     * 
     * @param nomeTabela Nome da tabela para o campo search
     * @param campoData Nome do campo de data específico do template
     * @param dataInicio Data de início do filtro
     * @param dataFim Data de fim do filtro
     * @param pagina Número da página atual
     * @return String JSON formatada para o corpo da requisição
     */
    private String construirCorpoRequisicao(String nomeTabela, String campoData, 
            Instant dataInicio, Instant dataFim, int pagina) {
        try {
            ObjectNode corpo = objectMapper.createObjectNode();
            ObjectNode search = objectMapper.createObjectNode();
            ObjectNode table = objectMapper.createObjectNode();

            // Formata as datas no formato yyyy-MM-dd - yyyy-MM-dd
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            String dataInicioStr = dataInicio.atZone(java.time.ZoneOffset.UTC).toLocalDate().format(fmt);
            String dataFimStr = dataFim.atZone(java.time.ZoneOffset.UTC).toLocalDate().format(fmt);
            String range = dataInicioStr + " - " + dataFimStr;

            // Constrói a estrutura JSON conforme formato do Postman
            table.put(campoData, range);
            search.set(nomeTabela, table);

            corpo.set("search", search);
            corpo.put("page", String.valueOf(pagina));
            corpo.put("per", "100");

            String corpoJson = objectMapper.writeValueAsString(corpo);
            logger.debug("Corpo JSON construído: {}", corpoJson);
            return corpoJson;
            
        } catch (JsonProcessingException e) {
            logger.error("Erro ao construir corpo da requisição: {}", e.getMessage(), e);
            return "{}";
        }
    }

    /**
     * Obtém a contagem total de manifestos para uma data de referência específica
     * Baixa o CSV e conta as linhas de forma eficiente usando NIO
     * 
     * @param dataReferencia Data de referência para filtrar os manifestos
     * @return Número total de manifestos encontrados
     * @throws RuntimeException se houver erro no download ou processamento do CSV
     */
    public int obterContagemManifestos(final LocalDate dataReferencia) {
        return obterContagemGenericaCsv(
            templateIdManifestos, 
            TABELA_MANIFESTOS, 
            CAMPO_MANIFESTOS, 
            dataReferencia, 
            "manifestos"
        );
    }

    /**
     * Obtém a contagem total de cotações para uma data de referência específica
     * Baixa o CSV e conta as linhas de forma eficiente usando NIO
     * 
     * @param dataReferencia Data de referência para filtrar as cotações
     * @return Número total de cotações encontradas
     * @throws RuntimeException se houver erro no download ou processamento do CSV
     */
    public int obterContagemCotacoes(final LocalDate dataReferencia) {
        return obterContagemGenericaCsv(
            templateIdCotacoes, 
            TABELA_COTACOES, 
            CAMPO_COTACOES, 
            dataReferencia, 
            "cotações"
        );
    }

    /**
     * Obtém a contagem total de localizações de carga para uma data de referência específica
     * Baixa o CSV e conta as linhas de forma eficiente usando NIO
     * 
     * @param dataReferencia Data de referência para filtrar as localizações
     * @return Número total de localizações de carga encontradas
     * @throws RuntimeException se houver erro no download ou processamento do CSV
     */
    public int obterContagemLocalizacoesCarga(final LocalDate dataReferencia) {
        return obterContagemGenericaCsv(
            templateIdLocalizacaoCarga, 
            TABELA_LOCALIZACAO_CARGA, 
            CAMPO_LOCALIZACAO_CARGA, 
            dataReferencia, 
            "localizações de carga"
        );
    }

    /**
     * Método genérico para obter contagem de registros via download e contagem de CSV
     * Implementa a estratégia recomendada na documentação: baixar CSV e contar linhas
     * 
     * @param templateId ID do template para a requisição
     * @param nomeTabela Nome da tabela para filtros
     * @param campoData Campo de data para filtros
     * @param dataReferencia Data de referência para filtros
     * @param tipoAmigavel Nome amigável do tipo de dados para logs
     * @return Número total de registros encontrados
     * @throws RuntimeException se houver erro no download ou processamento
     */
    private int obterContagemGenericaCsv(int templateId, String nomeTabela, String campoData, 
            LocalDate dataReferencia, String tipoAmigavel) {
        
        String chaveTemplate = "Template-" + templateId;
        
        // CIRCUIT BREAKER - Verificar se o template está com circuit aberto
        if (templatesComCircuitAberto.contains(chaveTemplate)) {
            logger.warn("⚠️ CIRCUIT BREAKER ATIVO - Template {} ({}) temporariamente desabilitado para contagem", 
                    templateId, tipoAmigavel);
            return 0;
        }

        logger.info("🔢 Obtendo contagem de {} via CSV - Template: {}, Data: {}", 
                tipoAmigavel, templateId, dataReferencia);

        Path arquivoTemporario = null;
        try {
            // Converter LocalDate para Instant (início e fim do dia)
            Instant dataInicio = dataReferencia.atStartOfDay().atZone(java.time.ZoneOffset.UTC).toInstant();
            Instant dataFim = dataReferencia.plusDays(1).atStartOfDay().atZone(java.time.ZoneOffset.UTC).toInstant();

            // URL para download do CSV
            String url = String.format("%s/api/analytics/reports/%d/data", urlBase, templateId);

            // Construir corpo da requisição com per=1 para otimização (apenas primeira página)
            String corpoJson = construirCorpoRequisicaoCsv(nomeTabela, campoData, dataInicio, dataFim);

            logger.debug("Baixando CSV para contagem via URL: {} com corpo: {}", url, corpoJson);

            HttpRequest requisicao = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .header("Accept", "text/csv") // Solicitar formato CSV
                    .timeout(this.timeoutRequisicao)
                    .method("GET", HttpRequest.BodyPublishers.ofString(corpoJson))
                    .build();

            final long inicioMs = System.currentTimeMillis();
            HttpResponse<String> resposta = gerenciadorRequisicao.executarRequisicao(
                    this.httpClient, requisicao, "contagem-csv-" + tipoAmigavel.replace(" ", "-"));
            final long duracaoMs = System.currentTimeMillis() - inicioMs;

            if (resposta == null) {
                logger.error("Erro: resposta nula ao baixar CSV para contagem de {}", tipoAmigavel);
                throw new RuntimeException("Falha na requisição CSV: resposta é null");
            }

            if (resposta.statusCode() != 200) {
                final String mensagemErro = String.format("Erro ao baixar CSV para contagem de %s. Status: %d", 
                    tipoAmigavel, resposta.statusCode());
                logger.error("{} ({} ms) Body: {}", mensagemErro, duracaoMs, resposta.body());
                incrementarContadorFalhas(chaveTemplate, tipoAmigavel);
                throw new RuntimeException(mensagemErro);
            }

            // Criar arquivo temporário no diretório temporário do sistema
            arquivoTemporario = Files.createTempFile("contagem-" + tipoAmigavel.replace(" ", "-"), ".csv");
            
            // Escrever conteúdo CSV no arquivo temporário
            Files.write(arquivoTemporario, resposta.body().getBytes());

            // Contar linhas usando NIO de forma eficiente (sem carregar tudo na memória)
            long totalLinhas;
            try (var linhas = Files.lines(arquivoTemporario)) {
                totalLinhas = linhas.count();
            }

            // Subtrair 1 para desconsiderar o cabeçalho
            final int contagem = Math.max(0, (int) (totalLinhas - 1));

            // Reset do contador de falhas em caso de sucesso
            contadorFalhasConsecutivas.put(chaveTemplate, 0);

            logger.info("✅ Contagem de {} obtida com sucesso via CSV: {} registros ({} ms)", 
                    tipoAmigavel, contagem, duracaoMs);

            return contagem;

        } catch (final IOException e) {
            logger.error("Erro de I/O ao obter contagem de {} via CSV: {}", tipoAmigavel, e.getMessage(), e);
            incrementarContadorFalhas(chaveTemplate, tipoAmigavel);
            throw new RuntimeException("Erro de I/O ao processar contagem de " + tipoAmigavel + " via CSV", e);
        } catch (final RuntimeException e) {
            logger.error("Erro de runtime ao obter contagem de {} via CSV: {}", tipoAmigavel, e.getMessage(), e);
            incrementarContadorFalhas(chaveTemplate, tipoAmigavel);
            throw e; // Re-lançar RuntimeException sem encapsular
        } catch (final Exception e) {
            logger.error("Erro inesperado ao obter contagem de {} via CSV: {}", tipoAmigavel, e.getMessage(), e);
            incrementarContadorFalhas(chaveTemplate, tipoAmigavel);
            throw new RuntimeException("Erro inesperado ao processar contagem de " + tipoAmigavel + " via CSV", e);
        } finally {
            // Garantir que o arquivo temporário seja deletado
            if (arquivoTemporario != null) {
                try {
                    Files.deleteIfExists(arquivoTemporario);
                    logger.debug("Arquivo temporário deletado: {}", arquivoTemporario);
                } catch (final IOException e) {
                    logger.warn("Não foi possível deletar arquivo temporário {}: {}", 
                            arquivoTemporario, e.getMessage());
                } catch (final SecurityException e) {
                    logger.warn("Sem permissão para deletar arquivo temporário {}: {}", 
                            arquivoTemporario, e.getMessage());
                }
            }
        }
    }

    /**
     * Constrói o corpo da requisição JSON para contagem via CSV
     * Similar ao método original, mas otimizado para contagem
     * 
     * @param nomeTabela Nome da tabela para filtros
     * @param campoData Campo de data para filtros
     * @param dataInicio Data de início do período
     * @param dataFim Data de fim do período
     * @return String JSON do corpo da requisição
     */
    private String construirCorpoRequisicaoCsv(String nomeTabela, String campoData, 
            Instant dataInicio, Instant dataFim) {
        try {
            ObjectNode corpo = objectMapper.createObjectNode();
            ObjectNode search = objectMapper.createObjectNode();
            ObjectNode table = objectMapper.createObjectNode();

            // Formatar as datas no formato yyyy-MM-dd - yyyy-MM-dd
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            String dataInicioStr = dataInicio.atZone(java.time.ZoneOffset.UTC).toLocalDate().format(fmt);
            String dataFimStr = dataFim.atZone(java.time.ZoneOffset.UTC).toLocalDate().format(fmt);
            String range = dataInicioStr + " - " + dataFimStr;

            // Construir a estrutura JSON
            table.put(campoData, range);
            search.set(nomeTabela, table);

            corpo.set("search", search);
            corpo.put("page", "1"); // Apenas primeira página para contagem
            corpo.put("per", "10000"); // Máximo possível para obter todos os registros

            String corpoJson = objectMapper.writeValueAsString(corpo);
            logger.debug("Corpo JSON para contagem CSV construído: {}", corpoJson);
            return corpoJson;
            
        } catch (JsonProcessingException e) {
            logger.error("Erro ao construir corpo da requisição para contagem CSV: {}", e.getMessage(), e);
            return "{}";
        }
    }

}