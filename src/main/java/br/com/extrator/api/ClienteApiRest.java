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
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.extrator.modelo.rest.faturaspagar.FaturaAPagarDTO;
import br.com.extrator.modelo.rest.faturasreceber.FaturaAReceberDTO;
import br.com.extrator.modelo.rest.ocorrencias.OcorrenciaDTO;
import br.com.extrator.util.CarregadorConfig;
import br.com.extrator.util.GerenciadorRequisicaoHttp;

import com.fasterxml.jackson.databind.DeserializationFeature;

/**
 * Classe responsável pela comunicação com a API REST do ESL Cloud
 * Especializada em buscar Faturas e Ocorrências via endpoints REST
 */
public class ClienteApiRest {
    private static final Logger logger = LoggerFactory.getLogger(ClienteApiRest.class);
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
        this.clienteHttp = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.mapeadorJson = new ObjectMapper();
        this.mapeadorJson.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.gerenciadorRequisicao = new GerenciadorRequisicaoHttp();
    }

    /**
     * Busca faturas a RECEBER da API REST.
     * Endpoint sugerido baseado na documentação do projeto.
     * 
     * @param dataReferencia Data de referência para busca (dia de hoje)
     * @return Lista de DTOs de faturas a receber
     */
    public List<FaturaAReceberDTO> buscarFaturasAReceber(final LocalDate dataReferencia) {
        String dataInicioFormatada = formatarDataParaApiRest(dataReferencia);
        return buscarEntidadesTipadas("/api/accounting/credit/billings", dataInicioFormatada, "faturas_a_receber", FaturaAReceberDTO.class);
    }

    /**
     * Busca faturas a PAGAR da API REST.
     * Endpoint sugerido baseado na documentação do projeto.
     * 
     * @param dataReferencia Data de referência para busca (dia de hoje)
     * @return Lista de DTOs de faturas a pagar
     */
    public List<FaturaAPagarDTO> buscarFaturasAPagar(final LocalDate dataReferencia) {
        String dataInicioFormatada = formatarDataParaApiRest(dataReferencia);
        return buscarEntidadesTipadas("/api/accounting/debit/billings", dataInicioFormatada, "faturas_a_pagar", FaturaAPagarDTO.class);
    }

    /**
     * Busca ocorrências da API REST ESL Cloud com paginação
     * 
     * @param dataReferencia Data de referência para busca (dia de hoje)
     * @return Lista de DTOs de ocorrências
     */
    public List<OcorrenciaDTO> buscarOcorrencias(final LocalDate dataReferencia) {
        String dataInicioFormatada = formatarDataParaApiRest(dataReferencia);
        return buscarEntidadesTipadas("/api/invoice_occurrences", dataInicioFormatada, "ocorrencias", OcorrenciaDTO.class);
    }

    /**
     * Busca itens/parcelas de uma fatura a pagar específica.
     * 
     * @param idFatura ID da fatura para buscar os itens
     * @return JSON bruto dos itens da fatura
     */
    public String buscarItensFaturaAPagar(Long idFatura) {
        logger.info("Buscando itens da fatura a pagar ID: {}", idFatura);
        
        try {
            String endpoint = String.format("/api/accounting/debit/billings/%d/installments", idFatura);
            String urlCompleta = urlBase + endpoint;
            
            HttpRequest requisicao = HttpRequest.newBuilder()
                    .uri(URI.create(urlCompleta))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .timeout(this.timeoutRequisicao)
                    .GET()
                    .build();
            
            HttpResponse<String> resposta = gerenciadorRequisicao.executarRequisicao(
                    this.clienteHttp, requisicao, "ItensFaturaAPagar-" + idFatura);
            
            if (resposta.statusCode() == 200) {
                logger.debug("Itens da fatura {} obtidos com sucesso", idFatura);
                return resposta.body();
            } else {
                logger.error("Erro ao buscar itens da fatura {}: HTTP {} - {}", 
                    idFatura, resposta.statusCode(), resposta.body());
                return "{}";
            }
            
        } catch (Exception e) {
            logger.error("Erro ao buscar itens da fatura {}: {}", idFatura, e.getMessage(), e);
            return "{}";
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
    private String formatarDataParaApiRest(LocalDate dataReferencia) {
        ZonedDateTime inicioDodia = dataReferencia.atStartOfDay(ZoneId.of("America/Sao_Paulo"));
        return inicioDodia.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    /**
     * Busca entidades tipadas de um endpoint específico da API REST ESL Cloud com paginação
     * 
     * @param <T>          Tipo da entidade
     * @param endpoint     Endpoint específico (ex: "/api/accounting/credit/billings")
     * @param dataInicio   Data de início para busca (formato ISO: yyyy-MM-dd'T'HH:mm:ss)
     * @param tipoEntidade Tipo da entidade para logs
     * @param classeEntidade Classe da entidade para deserialização
     * @return Lista de entidades tipadas
     */
    public <T> List<T> buscarEntidadesTipadas(final String endpoint, final String dataInicio,
            final String tipoEntidade, final Class<T> classeEntidade) {
        logger.info("Iniciando busca de {} a partir de: {}", endpoint, dataInicio);
        final List<T> entidades = new ArrayList<>();

        String proximoId = null;
        boolean primeiraPagina = true;

        // Validação básica de configuração
        if (urlBase == null || urlBase.isBlank() || token == null || token.isBlank()) {
            logger.error("Configurações inválidas para chamada REST (urlBase/token)");
            return entidades;
        }

        try {
            do {
                // Constrói a URL com os parâmetros adequados
                String url;
                if (primeiraPagina) {
                    // Usa diretamente o timestamp formatado com fuso horário
                    String timestampFormatado = dataInicio;
                    
                    // Log detalhado para diagnóstico
                    logger.info("DIAGNÓSTICO API REST - Timestamp formatado: {}", timestampFormatado);
                    
                    // Codifica o timestamp para URL para evitar problemas com caracteres especiais
                    String timestampCodificado = URLEncoder.encode(timestampFormatado, StandardCharsets.UTF_8);
                    
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

                logger.info("Processadas {} entidades nesta página ({} ms)", entidadesNestaPagina, duracaoMs);

                // CONDIÇÃO DE PARAGEM MELHORADA
                if (entidadesNestaPagina == 0) {
                    proximoId = null; // Força a paragem do loop se não vierem mais dados
                }

            } while (proximoId != null);

        } catch (final IOException e) {
            logger.error("Erro de I/O ou JSON durante a comunicação com a API", e);
            throw new RuntimeException("Erro ao comunicar com a API ESL Cloud", e);
        }

        logger.info("Busca de {} concluída. Total de entidades encontradas: {}", endpoint, entidades.size());
        return entidades;
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

}