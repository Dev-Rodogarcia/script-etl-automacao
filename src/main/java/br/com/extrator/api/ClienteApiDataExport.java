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
import br.com.extrator.modelo.dataexport.contasapagar.ContasAPagarDTO;
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
    private static final int TEMPLATE_ID_CONTAS_APAGAR = 8636;
    private static final int TEMPLATE_ID_FATURAS_POR_CLIENTE = 4924;

    // Campos de data corretos para cada template (descobertos via Postman)
    private static final String CAMPO_MANIFESTOS = "service_date";
    private static final String CAMPO_COTACOES = "requested_at";
    private static final String CAMPO_LOCALIZACAO_CARGA = "service_at";
    private static final String CAMPO_CONTAS_APAGAR = "issue_date";
    private static final String CAMPO_FATURAS_POR_CLIENTE = "service_at";
    
    // Constantes para nomes de// Nomes das tabelas conforme API DataExport
    private static final String TABELA_MANIFESTOS = "manifests";
    private static final String TABELA_COTACOES = "quotes";
    private static final String TABELA_LOCALIZACAO_CARGA = "freights";
    private static final String TABELA_CONTAS_APAGAR = "accounting_debits";
    private static final String TABELA_FATURAS_POR_CLIENTE = "freights";

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
     * Busca dados de Faturas a Pagar (Contas a Pagar) da API Data Export (últimas 24h).
     */
    public ResultadoExtracao<ContasAPagarDTO> buscarContasAPagar() {
        logger.info("Buscando Faturas a Pagar da API DataExport (últimas 24h)");
        Instant agora = Instant.now();
        Instant ontem = agora.minusSeconds(24 * 60 * 60);
        return buscarDadosGenericos(
            TEMPLATE_ID_CONTAS_APAGAR,
            TABELA_CONTAS_APAGAR,
            CAMPO_CONTAS_APAGAR,
            new TypeReference<List<ContasAPagarDTO>>() {},
            ontem,
            agora
        );
    }

    /**
     * Busca dados de Faturas por Cliente da API Data Export (últimas 24h).
     */
    public ResultadoExtracao<br.com.extrator.modelo.dataexport.faturaporcliente.FaturaPorClienteDTO> buscarFaturasPorCliente() {
        logger.info("Buscando Faturas por Cliente da API DataExport (últimas 24h)");
        Instant agora = Instant.now();
        Instant ontem = agora.minusSeconds(24 * 60 * 60);
        return buscarDadosGenericos(
            TEMPLATE_ID_FATURAS_POR_CLIENTE,
            TABELA_FATURAS_POR_CLIENTE,
            CAMPO_FATURAS_POR_CLIENTE,
            new com.fasterxml.jackson.core.type.TypeReference<java.util.List<br.com.extrator.modelo.dataexport.faturaporcliente.FaturaPorClienteDTO>>() {},
            ontem,
            agora
        );
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
        
        // Obter valor de 'per' e timeout adequado
        String valorPer = obterValorPerPorTemplate(templateId);
        Duration timeout = obterTimeoutPorTemplate(templateId);
        
        logger.info("═══════════════════════════════════════════════════════");
        logger.info("INICIANDO EXTRAÇÃO: Template {} - {}", templateId, tipoAmigavel);
        logger.info("Período: {} até {}", 
                dataInicio.atZone(java.time.ZoneOffset.UTC).toLocalDate(), 
                dataFim.atZone(java.time.ZoneOffset.UTC).toLocalDate());
        logger.info("Valor 'per': {}", valorPer);
        logger.info("Timeout: {} segundos", timeout.getSeconds());
        logger.info("═══════════════════════════════════════════════════════");

        List<T> resultadosFinais = new ArrayList<>();
        int paginaAtual = 1;
        int totalPaginas = 0;
        int totalRegistrosProcessados = 0;
        boolean interrompido = false;
        int limitePaginas = CarregadorConfig.obterLimitePaginasApiDataExport();

        try {
            while (true) {
                // PROTEÇÃO 1: Limite máximo de páginas
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

                // Log início da página
                logger.info("→ Requisitando página {}...", paginaAtual);

                // URL base limpa sem parâmetros de query (filtros e paginação vão no corpo JSON)
                String url = String.format("%s/api/analytics/reports/%d/data", urlBase, templateId);

                // Constrói o corpo JSON com search, page, per conforme formato do Postman
                String corpoJson = construirCorpoRequisicao(templateId, nomeTabela, campoData, dataInicio, dataFim, paginaAtual, valorPer);

                logger.debug("URL: {} | Corpo: {}", url, corpoJson);

                HttpRequest requisicao = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Authorization", "Bearer " + token)
                        .header("Content-Type", "application/json")
                        .timeout(timeout) // Timeout específico por template
                        .method("GET", HttpRequest.BodyPublishers.ofString(corpoJson))
                        .build();

                // Executar requisição com medição de tempo
                long tempoInicio = System.currentTimeMillis();
                HttpResponse<String> resposta = gerenciadorRequisicao.executarRequisicao(this.httpClient, requisicao, 
                        "DataExport-Template-" + templateId + "-Page-" + paginaAtual);
                long duracaoMs = System.currentTimeMillis() - tempoInicio;

                // Verificar resposta
                if (resposta == null) {
                    logger.error("❌ Erro: resposta nula na página {}", paginaAtual);
                    incrementarContadorFalhas(chaveTemplate, tipoAmigavel);
                    throw new RuntimeException("Resposta nula na paginação - página " + paginaAtual);
                }

                logger.info("← Resposta recebida: Status {}, Tempo: {}ms", resposta.statusCode(), duracaoMs);

                if (resposta.statusCode() != 200) {
                    logger.error("❌ Erro HTTP {} na página {}: {}", 
                            resposta.statusCode(), paginaAtual, resposta.body());
                    incrementarContadorFalhas(chaveTemplate, tipoAmigavel);
                    throw new RuntimeException("Erro HTTP " + resposta.statusCode() + " na página " + paginaAtual);
                }

                // Parse da resposta
                List<T> registrosPagina;
                try {
                    JsonNode raizJson = objectMapper.readTree(resposta.body());
                    JsonNode dadosNode = raizJson.has("data") ? raizJson.get("data") : raizJson;

                    if (dadosNode != null && dadosNode.isArray()) {
                        if (dadosNode.size() == 0) {
                            // Array vazio - fim da paginação
                            logger.info("■ Fim da paginação (página vazia)");
                            totalPaginas = paginaAtual - 1;
                            break;
                        }
                        registrosPagina = objectMapper.convertValue(dadosNode, typeReference);
                    } else {
                        // Resposta não é um array - tratar como vazio
                        logger.warn("⚠️ Resposta não é um array válido na página {}. Tratando como vazio.", paginaAtual);
                        totalPaginas = paginaAtual - 1;
                        break;
                    }
                } catch (JsonProcessingException e) {
                    logger.error("❌ Erro ao parsear JSON da página {}: {}", paginaAtual, e.getMessage());
                    incrementarContadorFalhas(chaveTemplate, tipoAmigavel);
                    throw new RuntimeException("Erro ao parsear página " + paginaAtual, e);
                }

                logger.info("✓ Página {}: {} registros parseados", paginaAtual, registrosPagina.size());

                

                // Adicionar registros
                resultadosFinais.addAll(registrosPagina);
                totalRegistrosProcessados += registrosPagina.size();
                
                // Reset do contador de falhas em caso de sucesso
                contadorFalhasConsecutivas.put(chaveTemplate, 0);
                
                logger.info("↑ Total acumulado: {} registros", totalRegistrosProcessados);

                // Log de progresso a cada intervalo definido
                if (paginaAtual % INTERVALO_LOG_PROGRESSO == 0) {
                    logger.info("⏳ Progresso: {} páginas processadas, {} registros", 
                            paginaAtual, totalRegistrosProcessados);
                }

                // Próxima página
                paginaAtual++;
            }

            // Reset circuit breaker em caso de sucesso
            contadorFalhasConsecutivas.put(chaveTemplate, 0);

            logger.info("═══════════════════════════════════════════════════════");
            logger.info("✅ EXTRAÇÃO CONCLUÍDA: {} registros em {} páginas", 
                    totalRegistrosProcessados, totalPaginas > 0 ? totalPaginas : (paginaAtual - 1));
            logger.info("═══════════════════════════════════════════════════════");

            // Retornar ResultadoExtracao
            if (interrompido) {
                return ResultadoExtracao.incompleto(resultadosFinais, ResultadoExtracao.MotivoInterrupcao.LIMITE_PAGINAS, 
                        totalPaginas > 0 ? totalPaginas : (paginaAtual - 1), totalRegistrosProcessados);
            } else {
                return ResultadoExtracao.completo(resultadosFinais, 
                        totalPaginas > 0 ? totalPaginas : (paginaAtual - 1), totalRegistrosProcessados);
            }

        } catch (RuntimeException e) {
            logger.error("❌ ERRO CRÍTICO na extração de {}: {}", tipoAmigavel, e.getMessage(), e);
            incrementarContadorFalhas(chaveTemplate, tipoAmigavel);
            throw new RuntimeException("Falha na extração de " + tipoAmigavel, e);
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
     * Formato: {"search": {"nomeTabela": {"campoData": "yyyy-MM-dd - yyyy-MM-dd"}}, "page": "1", "per": "1000|10000"}
     * 
     * @param templateId ID do template para determinar o valor de 'per'
     * @param nomeTabela Nome da tabela para o campo search
     * @param campoData Nome do campo de data específico do template
     * @param dataInicio Data de início do filtro
     * @param dataFim Data de fim do filtro
     * @param pagina Número da página atual
     * @param valorPer Valor de 'per' (registros por página)
     * @return String JSON formatada para o corpo da requisição
     */
    private String construirCorpoRequisicao(int templateId, String nomeTabela, String campoData, 
            Instant dataInicio, Instant dataFim, int pagina, String valorPer) {
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
            if (templateId == TEMPLATE_ID_CONTAS_APAGAR) {
                ObjectNode searchNested = objectMapper.createObjectNode();
                searchNested.put(campoData, range);
                searchNested.put("created_at", "");
                search.set(nomeTabela, searchNested);
            } else {
                table.put(campoData, range);
                search.set(nomeTabela, table);
            }

            corpo.set("search", search);
            corpo.put("page", String.valueOf(pagina));
            corpo.put("per", valorPer);

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
     * Obtém contagem via CSV para Faturas a Pagar.
     */
    public int obterContagemContasAPagar(final LocalDate dataReferencia) {
        return obterContagemGenericaCsv(
            TEMPLATE_ID_CONTAS_APAGAR,
            TABELA_CONTAS_APAGAR,
            CAMPO_CONTAS_APAGAR,
            dataReferencia,
            "faturas a pagar"
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
     * Obtém o valor de 'per' (registros por página) baseado no template ID.
     * Conforme documentação em docs/descobertas-endpoints/:
     * - Template 6906 (Cotações): per: "1000"
     * - Template 6399 (Manifestos): per: "10000"
     * - Template 8656 (Localização de Carga): per: "10000"
     * 
     * @param templateId ID do template
     * @return String com o valor de 'per' apropriado
     */
    private String obterValorPerPorTemplate(int templateId) {
        return switch (templateId) {
            case 6906 -> "1000";   // Cotações
            case 6399 -> "10000";  // Manifestos
            case 8656 -> "10000";  // Localização de Carga
            case 8636 -> "100";    // Faturas a Pagar
            default -> "100";      // Valor padrão para templates desconhecidos
        };
    }

    /**
     * Obtém o timeout adequado baseado no template ID.
     * Manifestos podem retornar páginas grandes, então precisam de timeout maior.
     * 
     * @param templateId ID do template
     * @return Duration com o timeout apropriado
     */
    private Duration obterTimeoutPorTemplate(int templateId) {
        // Timeout padrão da configuração
        Duration timeoutPadrao = this.timeoutRequisicao;
        
        // Timeout aumentado para manifestos (podem ter páginas muito grandes)
        return switch (templateId) {
            case 6399 -> Duration.ofSeconds(120);  // Manifestos: 120 segundos
            case 8656 -> Duration.ofSeconds(90);   // Localização: 90 segundos
            case 6906 -> Duration.ofSeconds(60);   // Cotações: 60 segundos
            case 8636 -> Duration.ofSeconds(60);   // Faturas a Pagar: 60 segundos
            default -> timeoutPadrao;              // Usar timeout padrão
        };
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