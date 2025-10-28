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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.extrator.modelo.dataexport.localizacaocarga.LocalizacaoCargaDTO;
import br.com.extrator.modelo.dataexport.cotacao.CotacaoDTO;
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
    
    // Constantes para template IDs da API Data Export
    private static final int TEMPLATE_ID_MANIFESTOS = 6399;
    private static final int TEMPLATE_ID_LOCALIZACAO_CARGA = 8656;
    private static final int TEMPLATE_ID_COTACOES = 6906;
    
    // Constantes para nomes das tabelas e campos de filtro
    private static final String TABELA_MANIFESTOS = "manifests";
    private static final String CAMPO_MANIFESTOS = "service_date";
    
    private static final String TABELA_LOCALIZACAO_CARGA = "freights";
    private static final String CAMPO_LOCALIZACAO_CARGA = "service_at";
    
    private static final String TABELA_COTACOES = "quotes";
    private static final String CAMPO_COTACOES = "requested_at";
    
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
            TEMPLATE_ID_MANIFESTOS
        );
        this.templateIdLocalizacaoCarga = carregarTemplateId(
            "API_DATAEXPORT_TEMPLATE_LOCALIZACAO",
            "api.dataexport.template.localizacao",
            TEMPLATE_ID_LOCALIZACAO_CARGA
        );
        this.templateIdCotacoes = carregarTemplateId(
            "API_DATAEXPORT_TEMPLATE_COTACOES",
            "api.dataexport.template.cotacoes",
            TEMPLATE_ID_COTACOES
        );
        logger.debug(
            "Template IDs configurados: manifestos={}, localizacao={}, cotacoes={}",
            templateIdManifestos, templateIdLocalizacaoCarga, templateIdCotacoes
        );
        
        // Inicializa o gerenciador de requisições HTTP
        this.gerenciadorRequisicao = new GerenciadorRequisicaoHttp();
        
        logger.info("Cliente da API Data Export inicializado com sucesso");
        logger.debug("URL base configurada: {}", urlBase);
    }
    
    /**
     * Busca dados de manifestos da API Data Export usando fluxo síncrono (resposta JSON).
     * 
     * @param dataReferencia Data de referência para busca (dia de hoje)
     * @return Lista de manifestos
     */
    public List<ManifestoDTO> buscarManifestos(LocalDate dataReferencia) {
        logger.info("Buscando manifestos da API DataExport para data: {}", dataReferencia);
        return buscarDadosGenericos(
            templateIdManifestos,
            TABELA_MANIFESTOS,
            CAMPO_MANIFESTOS,
            dataReferencia,
            ManifestoDTO.class
        );
    }
    
    /**
     * Busca dados de cotações da API Data Export usando fluxo síncrono (resposta JSON).
     * 
     * @param dataReferencia Data de referência para busca (dia de hoje)
     * @return Lista de cotações
     */
    public List<CotacaoDTO> buscarCotacoes(LocalDate dataReferencia) {
        logger.info("Buscando cotações da API DataExport para data: {}", dataReferencia);
        return buscarDadosGenericos(
            templateIdCotacoes,
            TABELA_COTACOES,
            CAMPO_COTACOES,
            dataReferencia,
            CotacaoDTO.class
        );
    }
    
    /**
     * Busca dados de localização de carga da API Data Export usando fluxo síncrono (resposta JSON).
     * 
     * @param dataReferencia Data de referência para busca (dia de hoje)
     * @return Lista de localizações de carga
     */
    public List<LocalizacaoCargaDTO> buscarLocalizacaoCarga(LocalDate dataReferencia) {
        logger.info("Buscando localização de carga da API DataExport para data: {}", dataReferencia);
        return buscarDadosGenericos(
            templateIdLocalizacaoCarga,
            TABELA_LOCALIZACAO_CARGA,
            CAMPO_LOCALIZACAO_CARGA,
            dataReferencia,
            LocalizacaoCargaDTO.class
        );
    }
    
    /**
     * Busca dados de manifestos usando fluxo síncrono (resposta JSON direta).
     * Este método faz uma requisição GET direta para o endpoint /data e recebe os dados como JSON.
     * 
     * @param dataInicio Data de início para filtro no formato ISO (YYYY-MM-DD)
     * @return Lista de manifestos
     */
    public List<ManifestoDTO> buscarManifestosSincrono(String dataInicio) {
        logger.info("Iniciando busca de manifestos (fluxo síncrono - resposta JSON) com data de início: {}", dataInicio);
        
        return buscarDadosGenericos(
            templateIdManifestos,
            TABELA_MANIFESTOS,
            CAMPO_MANIFESTOS,
            LocalDate.parse(dataInicio),
            ManifestoDTO.class
        );
    }
    
    /**
     * Método genérico consolidado para buscar dados da API Data Export.
     * Implementa o fluxo síncrono com formatação correta de data para buscar dados do dia especificado.
     * 
     * @param templateId ID do template do relatório
     * @param tabelaFiltro Nome da tabela para filtro
     * @param campoFiltro Nome do campo para filtro de data
     * @param dataReferencia Data de referência (dia de hoje)
     * @param tipoClasse Classe para desserialização tipada
     * @return Lista de entidades tipadas extraídas do JSON de resposta
     */
    private <T> List<T> buscarDadosGenericos(int templateId, String tabelaFiltro, 
            String campoFiltro, LocalDate dataReferencia, Class<T> tipoClasse) {
        
        // Formatar data para API Data Export (YYYY-MM-DD)
        String dataFormatada = formatarDataParaApiDataExport(dataReferencia);
        
        logger.info("Executando busca genérica - Template: {}, Tabela: {}, Campo: {}, Data: {}, Tipo: {}", 
                templateId, tabelaFiltro, campoFiltro, dataFormatada, tipoClasse.getSimpleName());
        
        try {
            // Constrói a URL final para fluxo síncrono (obrigatório incluir /data)
            String urlFinal = urlBase + "/api/analytics/reports/" + templateId + "/data";
            logger.debug("URL da requisição síncrona: {}", urlFinal);
            
            // Constrói o corpo da requisição JSON usando a mesma data para início e fim
            String corpoRequisicao = construirCorpoRequisicaoSincrona(tabelaFiltro, campoFiltro, dataFormatada, dataFormatada);
            logger.debug("Endpoint: {}, Payload: {}", urlFinal, corpoRequisicao);
            
            // Constrói a requisição HTTP
            final String urlFinalParaLambda = urlFinal; // Variável final para uso na lambda
            final String corpoRequisicaoFinal = corpoRequisicao; // Variável final para uso na lambda
            HttpRequest requisicao = HttpRequest.newBuilder()
                    .uri(URI.create(urlFinalParaLambda))
                    .method("GET", HttpRequest.BodyPublishers.ofString(corpoRequisicaoFinal))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .timeout(this.timeoutRequisicao)
                    .build();
            
            // Executa a requisição usando GerenciadorRequisicaoHttp para garantir throttling e retry
            logger.debug("Enviando requisição GET com corpo para template {} usando GerenciadorRequisicaoHttp", templateId);
            HttpResponse<String> response = gerenciadorRequisicao.executarRequisicao(
                    httpClient, 
                    requisicao, 
                    "DataExport-Template-" + templateId);
            
            // Verifica o status da resposta
            int statusCode = response.statusCode();
            logger.debug("Status da resposta: {}", statusCode);
            
            if (statusCode == 200) {
                String responseBody = response.body();
                logger.debug("Resposta recebida com sucesso. Tamanho: {} caracteres", responseBody.length());
                
                // Processa a resposta JSON e retorna as entidades tipadas
                List<T> entidades = processarRespostaJsonTipada(responseBody, tipoClasse);
                
                // Log de diagnóstico para respostas vazias
                if (entidades.isEmpty()) {
                    logger.warn("API retornou status 200 mas nenhuma entidade foi processada. Resposta completa: {}", responseBody);
                }
                
                return entidades;
            } else {
                logger.error("Erro na requisição para template {}. Status: {}, Resposta: {}", 
                        templateId, statusCode, response.body());
                return new ArrayList<>();
            }
            
        } catch (Exception e) {
            logger.error("Erro inesperado ao buscar dados do template {}: {}", templateId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Formatar LocalDate para o formato esperado pela API Data Export (YYYY-MM-DD).
     * 
     * @param data Data a ser formatada
     * @return String no formato YYYY-MM-DD
     */
    private String formatarDataParaApiDataExport(LocalDate data) {
        return data.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    /**
     * Processa a resposta JSON da API Data Export e deserializa para entidades tipadas.
     * 
     * @param responseBody Corpo da resposta JSON
     * @param tipoClasse Classe para desserialização tipada
     * @return Lista de entidades tipadas
     */
    private <T> List<T> processarRespostaJsonTipada(String responseBody, Class<T> tipoClasse) {
        List<T> entidades = new ArrayList<>();
        
        try {
            logger.debug("Iniciando processamento da resposta JSON para tipo {}", tipoClasse.getSimpleName());
            
            // Parse do JSON de resposta
            JsonNode rootNode = objectMapper.readTree(responseBody);
            
            // Verifica se há dados na resposta
            if (rootNode == null || !rootNode.has("data")) {
                logger.warn("Resposta JSON não contém campo 'data'");
                return entidades;
            }
            
            JsonNode dataNode = rootNode.get("data");
            if (!dataNode.isArray()) {
                logger.warn("Campo 'data' não é um array");
                return entidades;
            }
            
            // Processa cada item do array de dados
            for (JsonNode itemNode : dataNode) {
                try {
                    // Deserializa diretamente para a classe tipada usando Jackson
                    T entidade = objectMapper.treeToValue(itemNode, tipoClasse);
                    entidades.add(entidade);
                } catch (JsonProcessingException | IllegalArgumentException e) {
                    logger.warn("Erro ao deserializar item para {}: {}", tipoClasse.getSimpleName(), e.getMessage());
                }
            }
            
            logger.info("Processamento concluído. {} entidades {} extraídas", entidades.size(), tipoClasse.getSimpleName());
            
        } catch (IOException e) {
            logger.error("Erro ao processar resposta JSON para tipo {}: {}", tipoClasse.getSimpleName(), e.getMessage(), e);
        }
        
        return entidades;
    }

    /**
     * Processa um arquivo Excel e deserializa para entidades tipadas.
     * 
     * @param dadosArquivo Dados binários do arquivo Excel
     * @param tipoClasse Classe para desserialização tipada
    /**
     * Constrói o corpo da requisição JSON para o fluxo síncrono.
     * 
     * @param tabelaFiltro Nome da tabela para filtro
     * @param campoFiltro Nome do campo para filtro de data
     * @param dataInicio Data de início do período
     * @param dataFim Data de fim do período
     * @return String JSON do corpo da requisição
     */
    private String construirCorpoRequisicaoSincrona(String tabelaFiltro, String campoFiltro, 
            String dataInicio, String dataFim) {
        try {
            var objectNode = objectMapper.createObjectNode();
            var filtersArray = objectMapper.createArrayNode();
            
            var filterObject = objectMapper.createObjectNode();
            filterObject.put("table", tabelaFiltro);
            filterObject.put("field", campoFiltro);
            filterObject.put("operator", "between");
            
            var valuesArray = objectMapper.createArrayNode();
            valuesArray.add(dataInicio);
            valuesArray.add(dataFim);
            
            filterObject.set("values", valuesArray);
            filtersArray.add(filterObject);
            objectNode.set("filters", filtersArray);
            
            // Log de diagnóstico para mostrar o formato exato enviado para a API
            logger.debug("Filtro construído: tabela={}, campo={}, intervalo=[{} - {}]", 
                    tabelaFiltro, campoFiltro, dataInicio, dataFim);
            
            return objectMapper.writeValueAsString(objectNode);
            
        } catch (JsonProcessingException e) {
            logger.error("Erro ao construir corpo da requisição: {}", e.getMessage(), e);
            return "{}";
        }
    }

    /**
     * Carrega o ID de template permitindo sobrescrita por variável de ambiente ou propriedade.
     * Prioriza variável de ambiente. Se inválido, retorna valor padrão.
     *
     * @param envName nome da variável de ambiente
     * @param propKey chave da propriedade no arquivo de configuração
     * @param padrao valor padrão em caso de ausência/erro
     * @return id de template resolvido
     */
    private int carregarTemplateId(String envName, String propKey, int padrao) {
        try {
            String valorEnv = System.getenv(envName);
            String valor = (valorEnv != null && !valorEnv.trim().isEmpty())
                ? valorEnv
                : CarregadorConfig.obterPropriedade(propKey);
            if (valor != null && !valor.trim().isEmpty()) {
                return Integer.parseInt(valor.trim());
            }
        } catch (NumberFormatException | SecurityException e) {
            logger.warn("Template ID inválido para '{}'. Usando padrão {}. Detalhes: {}", propKey, padrao, e.getMessage());
        }
        return padrao;
    }
    

}