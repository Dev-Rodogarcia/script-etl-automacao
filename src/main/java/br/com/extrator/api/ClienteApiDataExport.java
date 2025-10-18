package br.com.extrator.api;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.extrator.modelo.EntidadeDinamica;
import br.com.extrator.util.CarregadorConfig;

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
    
    // Constantes para os IDs dos templates validados
    private static final int TEMPLATE_ID_MANIFESTOS = 6399;
    private static final int TEMPLATE_ID_LOCALIZACAO_CARGA = 8656;
    private static final int TEMPLATE_ID_COTACOES = 6906;
    private static final int TEMPLATE_ID_COLETAS = 6908;
    
    // Constantes para nomes das tabelas e campos de filtro
    private static final String TABELA_MANIFESTOS = "manifests";
    private static final String CAMPO_MANIFESTOS = "service_date";
    
    private static final String TABELA_LOCALIZACAO_CARGA = "freights";
    private static final String CAMPO_LOCALIZACAO_CARGA = "service_at";
    
    private static final String TABELA_COTACOES = "quotes";
    private static final String CAMPO_COTACOES = "requested_at";
    
    private static final String TABELA_COLETAS = "picks";
    private static final String CAMPO_COLETAS = "request_date";
    
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
        
        // Valida configurações obrigatórias
        if (urlBase == null || urlBase.trim().isEmpty()) {
            throw new IllegalStateException("URL base da API não configurada");
        }
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalStateException("Token da API Data Export não configurado");
        }
        
        logger.info("Cliente da API Data Export inicializado com sucesso");
        logger.debug("URL base configurada: {}", urlBase);
    }
    
    /**
     * Busca dados de manifestos da API Data Export.
     * 
     * @param dataInicio Data de início para filtro no formato ISO (YYYY-MM-DD)
     * @param modoTeste Se true, limita os resultados para teste
     * @return Lista de entidades dinâmicas representando os manifestos
     */
    public List<EntidadeDinamica> buscarManifestos(String dataInicio, boolean modoTeste) {
        logger.info("Iniciando busca de manifestos com data de início: {} (modo teste: {})", dataInicio, modoTeste);
        
        List<EntidadeDinamica> resultados = buscarDadosSincronos(
            TEMPLATE_ID_MANIFESTOS,
            TABELA_MANIFESTOS,
            CAMPO_MANIFESTOS,
            dataInicio,
            calcularDataFim(dataInicio),
            "manifestos"
        );
        
        // Se modo teste estiver ativo, limita os resultados
        if (modoTeste && resultados.size() > 100) {
            logger.info("Modo teste ativo: limitando resultados de {} para 100 registros", resultados.size());
            return resultados.subList(0, 100);
        }
        
        return resultados;
    }
    
    /**
     * Busca dados de localização de carga da API Data Export.
     * 
     * @param dataInicio Data de início para filtro no formato ISO (YYYY-MM-DD)
     * @param modoTeste Se true, limita os resultados para teste
     * @return Lista de entidades dinâmicas representando a localização de carga
     */
    public List<EntidadeDinamica> buscarLocalizacaoCarga(String dataInicio, boolean modoTeste) {
        logger.info("Iniciando busca de localização de carga com data de início: {} (modo teste: {})", dataInicio, modoTeste);
        
        List<EntidadeDinamica> resultados = buscarDadosSincronos(
            TEMPLATE_ID_LOCALIZACAO_CARGA,
            TABELA_LOCALIZACAO_CARGA,
            CAMPO_LOCALIZACAO_CARGA,
            dataInicio,
            calcularDataFim(dataInicio),
            "localizacao_carga"
        );
        
        // Se modo teste estiver ativo, limita os resultados
        if (modoTeste && resultados.size() > 100) {
            logger.info("Modo teste ativo: limitando resultados de {} para 100 registros", resultados.size());
            return resultados.subList(0, 100);
        }
        
        return resultados;
    }
    
    /**
     * Busca dados de cotações da API Data Export.
     * 
     * @param dataInicio Data de início para filtro no formato ISO (YYYY-MM-DD)
     * @param modoTeste Se true, limita os resultados para teste
     * @return Lista de entidades dinâmicas representando as cotações
     */
    public List<EntidadeDinamica> buscarCotacoes(String dataInicio, boolean modoTeste) {
        logger.info("Iniciando busca de cotações com data de início: {} (modo teste: {})", dataInicio, modoTeste);
        
        List<EntidadeDinamica> resultados = buscarDadosSincronos(
            TEMPLATE_ID_COTACOES,
            TABELA_COTACOES,
            CAMPO_COTACOES,
            dataInicio,
            calcularDataFim(dataInicio),
            "cotacoes"
        );
        
        // Se modo teste estiver ativo, limita os resultados
        if (modoTeste && resultados.size() > 100) {
            logger.info("Modo teste ativo: limitando resultados de {} para 100 registros", resultados.size());
            return resultados.subList(0, 100);
        }
        
        return resultados;
    }
    
    /**
     * Busca dados de coletas da API Data Export.
     * 
     * @param dataInicio Data de início para filtro no formato ISO (YYYY-MM-DD)
     * @param modoTeste Se true, limita os resultados para teste
     * @return Lista de entidades dinâmicas representando as coletas
     */
    public List<EntidadeDinamica> buscarColetas(String dataInicio, boolean modoTeste) {
        logger.info("Iniciando busca de coletas com data de início: {} (modo teste: {})", dataInicio, modoTeste);
        
        List<EntidadeDinamica> resultados = buscarDadosSincronos(
            TEMPLATE_ID_COLETAS,
            TABELA_COLETAS,
            CAMPO_COLETAS,
            dataInicio,
            calcularDataFim(dataInicio),
            "coletas"
        );
        
        // Se modo teste estiver ativo, limita os resultados
        if (modoTeste && resultados.size() > 100) {
            logger.info("Modo teste ativo: limitando resultados de {} para 100 registros", resultados.size());
            return resultados.subList(0, 100);
        }
        
        return resultados;
    }
    
    /**
     * Método principal que implementa o fluxo síncrono de extração de dados.
     * Realiza uma requisição GET com corpo JSON para o endpoint específico do template.
     * 
     * @param templateId ID numérico do template
     * @param tabelaFiltro Nome da tabela para filtro
     * @param campoFiltro Nome do campo para filtro de data
     * @param dataInicio Data de início do período
     * @param dataFim Data de fim do período
     * @param tipoEntidade Tipo da entidade para o objeto EntidadeDinamica
     * @return Lista de entidades dinâmicas extraídas
     */
    private List<EntidadeDinamica> buscarDadosSincronos(int templateId, String tabelaFiltro, 
            String campoFiltro, String dataInicio, String dataFim, String tipoEntidade) {
        
        logger.info("Executando busca síncrona - Template: {}, Tabela: {}, Campo: {}, Período: {} a {}", 
                templateId, tabelaFiltro, campoFiltro, dataInicio, dataFim);
        
        try {
            // Constrói a URL final
            String urlFinal = urlBase + "/api/analytics/reports/" + templateId + "/data";
            logger.debug("URL da requisição: {}", urlFinal);
            
            // Constrói o corpo da requisição JSON
            String corpoRequisicao = construirCorpoRequisicaoSincrona(tabelaFiltro, campoFiltro, dataInicio, dataFim);
            logger.debug("Corpo da requisição: {}", corpoRequisicao);
            
            // Cria a requisição HTTP GET com corpo usando Supplier para UtilitarioHttpRetry
            final String urlFinalParaLambda = urlFinal; // Variável final para uso na lambda
            final String corpoRequisicaoFinal = corpoRequisicao; // Variável final para uso na lambda
            java.util.function.Supplier<HttpRequest> fornecedorRequisicao = () -> HttpRequest.newBuilder()
                    .uri(URI.create(urlFinalParaLambda))
                    .method("GET", HttpRequest.BodyPublishers.ofString(corpoRequisicaoFinal))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(60))
                    .build();
            
            // Executa a requisição usando UtilitarioHttpRetry para garantir throttling proativo
            logger.debug("Enviando requisição GET com corpo para template {} usando UtilitarioHttpRetry", templateId);
            HttpResponse<String> response = br.com.extrator.util.UtilitarioHttpRetry.executarComRetry(
                    httpClient, 
                    fornecedorRequisicao, 
                    "DataExport-Template-" + templateId);
            
            // Verifica o status da resposta
            int statusCode = response.statusCode();
            logger.debug("Status da resposta: {}", statusCode);
            
            if (statusCode == 200) {
                String responseBody = response.body();
                logger.debug("Resposta recebida com sucesso. Tamanho: {} caracteres", responseBody.length());
                
                // Processa a resposta JSON e retorna as entidades
                return processarRespostaJson(responseBody, tipoEntidade);
            } else {
                logger.error("Erro na requisição para template {}. Status: {}, Resposta: {}", 
                        templateId, statusCode, response.body());
                return new ArrayList<>();
            }
            
        } catch (IOException e) {
            logger.error("Erro de I/O ao buscar dados do template {}: {}", templateId, e.getMessage(), e);
            return new ArrayList<>();
        } catch (InterruptedException e) {
            logger.error("Requisição interrompida para template {}: {}", templateId, e.getMessage());
            Thread.currentThread().interrupt();
            return new ArrayList<>();
        } catch (Exception e) {
            logger.error("Erro inesperado ao buscar dados do template {}: {}", templateId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Constrói o corpo JSON da requisição síncrona com os filtros apropriados.
     * 
     * @param tabela Nome da tabela para filtro
     * @param campo Nome do campo para filtro de data
     * @param dataInicio Data de início do período
     * @param dataFim Data de fim do período
     * @return String JSON formatada para o corpo da requisição
     */
    private String construirCorpoRequisicaoSincrona(String tabela, String campo, String dataInicio, String dataFim) {
        try {
            // Constrói o objeto JSON usando ObjectMapper
            var searchObject = objectMapper.createObjectNode();
            var tabelaObject = objectMapper.createObjectNode();
            
            // Formata o período de datas
            String periodoFiltro = dataInicio + " - " + dataFim;
            tabelaObject.put(campo, periodoFiltro);
            searchObject.set(tabela, tabelaObject);
            
            var rootObject = objectMapper.createObjectNode();
            rootObject.set("search", searchObject);
            
            return objectMapper.writeValueAsString(rootObject);
            
        } catch (IOException e) {
            logger.error("Erro de I/O ao construir corpo da requisição: {}", e.getMessage(), e);
            // Fallback para construção manual
            return String.format("{ \"search\": { \"%s\": { \"%s\": \"%s - %s\" } } }", 
                    tabela, campo, dataInicio, dataFim);
        } catch (RuntimeException e) {
            logger.error("Erro de runtime ao construir corpo da requisição: {}", e.getMessage(), e);
            // Fallback para construção manual
            return String.format("{ \"search\": { \"%s\": { \"%s\": \"%s - %s\" } } }", 
                    tabela, campo, dataInicio, dataFim);
        }
    }
    
    /**
     * Processa a resposta JSON da API e converte em lista de EntidadeDinamica.
     * 
     * @param responseBody Corpo da resposta JSON
     * @param tipoEntidade Tipo da entidade para os objetos EntidadeDinamica
     * @return Lista de entidades dinâmicas processadas
     */
    private List<EntidadeDinamica> processarRespostaJson(String responseBody, String tipoEntidade) {
        List<EntidadeDinamica> entidades = new ArrayList<>();
        
        try {
            JsonNode rootNode = objectMapper.readTree(responseBody);
            logger.debug("JSON parseado com sucesso");
            
            // A resposta da API Data Export pode ter diferentes estruturas
            // Tentamos encontrar um array de dados
            JsonNode dadosArray = null;
            
            // Verifica se a resposta é diretamente um array
            if (rootNode.isArray()) {
                dadosArray = rootNode;
            } else if (rootNode.has("data") && rootNode.get("data").isArray()) {
                dadosArray = rootNode.get("data");
            } else if (rootNode.has("results") && rootNode.get("results").isArray()) {
                dadosArray = rootNode.get("results");
            } else {
                logger.warn("Estrutura de resposta não reconhecida para tipo {}", tipoEntidade);
                logger.debug("Estrutura da resposta: {}", rootNode.toString());
            }
            
            if (dadosArray != null && dadosArray.isArray()) {
                logger.debug("Processando array com {} elementos", dadosArray.size());
                
                for (JsonNode itemNode : dadosArray) {
                    try {
                        // Cria uma nova entidade dinâmica
                        EntidadeDinamica entidade = new EntidadeDinamica(tipoEntidade);
                        
                        // Processa cada campo do JSON
                        itemNode.properties().forEach(campo -> {
                            String nomeCampo = campo.getKey();
                            JsonNode valorCampo = campo.getValue();
                            
                            // Converte o valor do campo para o tipo apropriado
                            Object valor;
                            if (valorCampo.isTextual()) {
                                valor = valorCampo.asText();
                            } else if (valorCampo.isNumber()) {
                                valor = valorCampo.isInt() ? valorCampo.asInt() : valorCampo.asDouble();
                            } else if (valorCampo.isBoolean()) {
                                valor = valorCampo.asBoolean();
                            } else if (valorCampo.isNull()) {
                                valor = null;
                            } else {
                                // Para objetos complexos, converte para string
                                valor = valorCampo.toString();
                            }
                            
                            entidade.adicionarCampo(nomeCampo, valor);
                        });
                        
                        entidades.add(entidade);
                        
                    } catch (Exception e) {
                        logger.warn("Erro ao processar item individual do tipo {}: {}", tipoEntidade, e.getMessage());
                    }
                }
                
                logger.info("Processamento concluído. {} entidades do tipo {} extraídas", entidades.size(), tipoEntidade);
            } else {
                logger.warn("Nenhum array de dados encontrado na resposta para tipo {}", tipoEntidade);
            }
            
        } catch (IOException e) {
            logger.error("Erro ao fazer parse do JSON para tipo {}: {}", tipoEntidade, e.getMessage(), e);
        } catch (RuntimeException e) {
            logger.error("Erro de runtime ao processar resposta para tipo {}: {}", tipoEntidade, e.getMessage(), e);
        }
        
        return entidades;
    }
    
    /**
     * Calcula a data de fim baseada na data de início.
     * Adiciona 30 dias à data de início para evitar consultas muito longas.
     * 
     * @param dataInicio Data de início no formato ISO (YYYY-MM-DD)
     * @return Data de fim no formato ISO (YYYY-MM-DD)
     */
    private String calcularDataFim(String dataInicio) {
        try {
            // Código corrigido e mais robusto
            String apenasData = dataInicio.split("T")[0]; // Pega apenas a parte antes do 'T'
            LocalDate inicio = LocalDate.parse(apenasData, DateTimeFormatter.ISO_LOCAL_DATE);
            
            // Adiciona 30 dias
            LocalDate fim = inicio.plusDays(30);
            
            // Retorna no formato ISO
            return fim.format(DateTimeFormatter.ISO_LOCAL_DATE);
            
        } catch (Exception e) {
            logger.warn("Erro ao calcular data fim para '{}': {}. Usando data atual + 30 dias", dataInicio, e.getMessage());
            
            // Fallback: usa data atual + 30 dias
            LocalDate fim = LocalDate.now().plusDays(30);
            return fim.format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
    }
}