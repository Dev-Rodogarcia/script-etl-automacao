package br.com.extrator.api;

import java.io.ByteArrayInputStream;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
    private final int templateIdManifestos;
    private final int templateIdLocalizacaoCarga;
    private final int templateIdCotacoes;
    
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
        
        logger.info("Cliente da API Data Export inicializado com sucesso");
        logger.debug("URL base configurada: {}", urlBase);
    }
    
    /**
     * Busca dados de manifestos da API Data Export usando fluxo assíncrono (arquivo XLSX).
     * Este método solicita a geração de um arquivo Excel, aguarda sua conclusão e processa o conteúdo.
     * 
     * @param dataInicio Data de início para filtro no formato ISO (YYYY-MM-DD)
     * @param modoTeste Se true, limita os resultados para teste
     * @return Lista de entidades dinâmicas representando os manifestos
     */
    public List<EntidadeDinamica> buscarManifestos(String dataInicio, boolean modoTeste) {
        return buscarManifestosSincrono(dataInicio, modoTeste);
    }
    
    /**
     * Busca dados de manifestos usando fluxo síncrono (resposta JSON direta).
     * Este método faz uma requisição GET direta para o endpoint /data e recebe os dados como JSON.
     * 
     * @param dataInicio Data de início para filtro no formato ISO (YYYY-MM-DD)
     * @param modoTeste Se true, limita os resultados para teste
     * @return Lista de entidades dinâmicas representando os manifestos
     */
    public List<EntidadeDinamica> buscarManifestosSincrono(String dataInicio, boolean modoTeste) {
        logger.info("Iniciando busca de manifestos (fluxo síncrono - resposta JSON) com data de início: {} (modo teste: {})", dataInicio, modoTeste);
        
        List<EntidadeDinamica> resultados = buscarDadosSincronos(
            templateIdManifestos,
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
     * Busca dados de manifestos usando fluxo assíncrono (geração e download de arquivo XLSX).
     * 
     * @param dataInicio Data de início para filtro no formato ISO (YYYY-MM-DD)
     * @param modoTeste Se true, limita os resultados para teste
     * @return Lista de entidades dinâmicas representando os manifestos
     */
    public List<EntidadeDinamica> buscarManifestosAssincrono(String dataInicio, boolean modoTeste) {
        logger.info("Iniciando busca de manifestos (fluxo assíncrono - arquivo XLSX) com data de início: {} (modo teste: {})", dataInicio, modoTeste);
        
        List<EntidadeDinamica> resultados = buscarDadosAssincronos(
            templateIdManifestos,
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
     * Busca dados de localização de carga da API Data Export usando fluxo síncrono (resposta JSON).
     * Este método faz uma requisição GET direta e recebe os dados como JSON.
     * 
     * @param dataInicio Data de início para filtro no formato ISO (YYYY-MM-DD)
     * @param modoTeste Se true, limita os resultados para teste
     * @return Lista de entidades dinâmicas representando a localização de carga
     */
    public List<EntidadeDinamica> buscarLocalizacaoCarga(String dataInicio, boolean modoTeste) {
        return buscarLocalizacaoCargaSincrono(dataInicio, modoTeste);
    }
    
    /**
     * Busca dados de localização de carga usando fluxo síncrono (resposta JSON direta).
     * 
     * @param dataInicio Data de início para filtro no formato ISO (YYYY-MM-DD)
     * @param modoTeste Se true, limita os resultados para teste
     * @return Lista de entidades dinâmicas representando a localização de carga
     */
    public List<EntidadeDinamica> buscarLocalizacaoCargaSincrono(String dataInicio, boolean modoTeste) {
        logger.info("Iniciando busca de localização de carga (fluxo síncrono - resposta JSON) com data de início: {} (modo teste: {})", dataInicio, modoTeste);
        
        List<EntidadeDinamica> resultados = buscarDadosSincronos(
            templateIdLocalizacaoCarga,
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
     * Busca dados de cotações da API Data Export usando fluxo síncrono (resposta JSON).
     * Este método faz uma requisição GET direta e recebe os dados como JSON.
     * 
     * @param dataInicio Data de início para filtro no formato ISO (YYYY-MM-DD)
     * @param modoTeste Se true, limita os resultados para teste
     * @return Lista de entidades dinâmicas representando as cotações
     */
    public List<EntidadeDinamica> buscarCotacoes(String dataInicio, boolean modoTeste) {
        return buscarCotacoesSincrono(dataInicio, modoTeste);
    }
    
    /**
     * Busca dados de cotações usando fluxo síncrono (resposta JSON direta).
     * 
     * @param dataInicio Data de início para filtro no formato ISO (YYYY-MM-DD)
     * @param modoTeste Se true, limita os resultados para teste
     * @return Lista de entidades dinâmicas representando as cotações
     */
    public List<EntidadeDinamica> buscarCotacoesSincrono(String dataInicio, boolean modoTeste) {
        logger.info("Iniciando busca de cotações (fluxo síncrono - resposta JSON) com data de início: {} (modo teste: {})", dataInicio, modoTeste);
        
        List<EntidadeDinamica> resultados = buscarDadosSincronos(
            templateIdCotacoes,
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
     * Implementa o fluxo síncrono da API ESL Cloud Data Export.
     * 
     * FLUXO SÍNCRONO:
     * - Método: GET com corpo JSON
     * - URL: .../api/analytics/reports/{id}/data (sufixo /data é obrigatório)
     * - Resposta: JSON direto com os dados
     * - Uso: Para dados menores que podem ser retornados imediatamente
     * 
     * @param templateId ID numérico do template
     * @param tabelaFiltro Nome da tabela para filtro
     * @param campoFiltro Nome do campo para filtro de data
     * @param dataInicio Data de início do período
     * @param dataFim Data de fim do período
     * @param tipoEntidade Tipo da entidade para o objeto EntidadeDinamica
     * @return Lista de entidades dinâmicas extraídas do JSON de resposta
     */
    private List<EntidadeDinamica> buscarDadosSincronos(int templateId, String tabelaFiltro, 
            String campoFiltro, String dataInicio, String dataFim, String tipoEntidade) {
        
        logger.info("Executando busca síncrona - Template: {}, Tabela: {}, Campo: {}, Período: {} a {}", 
                templateId, tabelaFiltro, campoFiltro, dataInicio, dataFim);
        
        try {
            // Constrói a URL final para fluxo síncrono (obrigatório incluir /data)
            String urlFinal = urlBase + "/api/analytics/reports/" + templateId + "/data";
            logger.debug("URL da requisição síncrona: {}", urlFinal);
            
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
            // Garante que as datas estejam no formato YYYY-MM-DD (sem hora)
            String dataInicioFormatada = formatarDataParaApi(dataInicio);
            String dataFimFormatada = formatarDataParaApi(dataFim);
            
            // Constrói o objeto JSON usando ObjectMapper
            var searchObject = objectMapper.createObjectNode();
            var tabelaObject = objectMapper.createObjectNode();
            
            // Formata o período de datas no formato exato esperado pela API
            String periodoFiltro = dataInicioFormatada + " - " + dataFimFormatada;
            tabelaObject.put(campo, periodoFiltro);
            searchObject.set(tabela, tabelaObject);
            
            var rootObject = objectMapper.createObjectNode();
            rootObject.set("search", searchObject);
            
            logger.debug("Corpo da requisição construído com período: {}", periodoFiltro);
            return objectMapper.writeValueAsString(rootObject);
            
        } catch (IOException e) {
            logger.error("Erro de I/O ao construir corpo da requisição: {}", e.getMessage(), e);
            // Fallback para construção manual com datas formatadas
            String dataInicioFormatada = formatarDataParaApi(dataInicio);
            String dataFimFormatada = formatarDataParaApi(dataFim);
            return String.format("{ \"search\": { \"%s\": { \"%s\": \"%s - %s\" } } }", 
                    tabela, campo, dataInicioFormatada, dataFimFormatada);
        } catch (RuntimeException e) {
            logger.error("Erro de runtime ao construir corpo da requisição: {}", e.getMessage(), e);
            // Fallback para construção manual com datas formatadas
            String dataInicioFormatada = formatarDataParaApi(dataInicio);
            String dataFimFormatada = formatarDataParaApi(dataFim);
            return String.format("{ \"search\": { \"%s\": { \"%s\": \"%s - %s\" } } }", 
                    tabela, campo, dataInicioFormatada, dataFimFormatada);
        }
    }
    
    /**
     * Formata uma data para o formato exato esperado pela API ESL Cloud (YYYY-MM-DD).
     * Remove qualquer informação de hora, minutos, segundos ou milissegundos.
     * 
     * @param data Data em qualquer formato (ISO, com hora, etc.)
     * @return Data formatada como YYYY-MM-DD
     */
    private String formatarDataParaApi(String data) {
        try {
            // Remove qualquer parte de hora se existir (tudo após 'T')
            String apenasData = data.split("T")[0];
            
            // Valida se está no formato correto YYYY-MM-DD
            LocalDate.parse(apenasData, DateTimeFormatter.ISO_LOCAL_DATE);
            
            return apenasData;
            
        } catch (Exception e) {
            logger.warn("Erro ao formatar data '{}': {}. Tentando extrair apenas a parte da data", data, e.getMessage());
            
            // Fallback: tenta extrair apenas os primeiros 10 caracteres (YYYY-MM-DD)
            if (data != null && data.length() >= 10) {
                String tentativa = data.substring(0, 10);
                try {
                    LocalDate.parse(tentativa, DateTimeFormatter.ISO_LOCAL_DATE);
                    return tentativa;
                } catch (Exception ex) {
                    logger.error("Não foi possível formatar a data '{}'. Usando data atual", data);
                    return LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
                }
            } else {
                logger.error("Data inválida '{}'. Usando data atual", data);
                return LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            }
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
     * Implementa o fluxo assíncrono da API ESL Cloud Data Export.
     * 
     * FLUXO ASSÍNCRONO:
     * - Passo 1: POST para .../api/analytics/reports/{id}/export (solicita geração)
     * - Passo 2: GET para .../api/analytics/report_files/{file_id} (consulta status)
     * - Passo 3: GET para URL de download (baixa arquivo XLSX)
     * - Passo 4: Processa arquivo Excel usando Apache POI
     * - Uso: Para grandes volumes de dados que precisam ser gerados como arquivo
     * 
     * @param templateId ID do template do relatório
     * @param tabelaFiltro Nome da tabela para filtro
     * @param campoFiltro Nome do campo para filtro de data
     * @param dataInicio Data de início do período
     * @param dataFim Data de fim do período
     * @param tipoEntidade Tipo da entidade para logging
     * @return Lista de entidades processadas do arquivo Excel
     */
    private List<EntidadeDinamica> buscarDadosAssincronos(int templateId, String tabelaFiltro, 
            String campoFiltro, String dataInicio, String dataFim, String tipoEntidade) {
        
        logger.info("Executando busca assíncrona - Template: {}, Tabela: {}, Campo: {}, Período: {} a {}", 
                templateId, tabelaFiltro, campoFiltro, dataInicio, dataFim);
        
        try {
            // Passo 1: Solicitar geração do relatório
            String idArquivo = solicitarGeracaoRelatorio(templateId, tabelaFiltro, campoFiltro, dataInicio, dataFim);
            if (idArquivo == null) {
                logger.error("Falha ao solicitar geração do relatório para template {}", templateId);
                return new ArrayList<>();
            }
            
            // Passo 2: Aguardar conclusão e obter URL de download
            String urlDownload = aguardarConclusaoRelatorio(idArquivo);
            if (urlDownload == null) {
                logger.error("Falha ao obter URL de download para arquivo {}", idArquivo);
                return new ArrayList<>();
            }
            
            // Passo 3: Baixar e processar o arquivo
            return baixarEProcessarArquivo(urlDownload, tipoEntidade);
            
        } catch (Exception e) {
            logger.error("Erro inesperado no fluxo assíncrono para template {}: {}", templateId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Solicita a geração de um relatório via API assíncrona.
     * 
     * @param templateId ID do template
     * @param tabelaFiltro Nome da tabela para filtro
     * @param campoFiltro Nome do campo para filtro de data
     * @param dataInicio Data de início do período
     * @param dataFim Data de fim do período
     * @return ID do arquivo gerado ou null em caso de erro
     */
    private String solicitarGeracaoRelatorio(int templateId, String tabelaFiltro, 
            String campoFiltro, String dataInicio, String dataFim) {
        
        try {
            // Constrói a URL para solicitação de export
            String urlExport = urlBase + "/api/analytics/reports/" + templateId + "/export";
            logger.debug("URL de solicitação de export: {}", urlExport);
            
            // Constrói o corpo da requisição (mesmo formato do fluxo síncrono)
            String corpoRequisicao = construirCorpoRequisicaoSincrona(tabelaFiltro, campoFiltro, dataInicio, dataFim);
            logger.debug("Corpo da requisição de export: {}", corpoRequisicao);
            
            // Cria a requisição HTTP POST
            final String urlFinalParaLambda = urlExport;
            final String corpoRequisicaoFinal = corpoRequisicao;
            java.util.function.Supplier<HttpRequest> fornecedorRequisicao = () -> HttpRequest.newBuilder()
                    .uri(URI.create(urlFinalParaLambda))
                    .POST(HttpRequest.BodyPublishers.ofString(corpoRequisicaoFinal))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(60))
                    .build();
            
            // Executa a requisição
            logger.debug("Enviando requisição POST para export do template {}", templateId);
            HttpResponse<String> response = br.com.extrator.util.UtilitarioHttpRetry.executarComRetry(
                    httpClient, 
                    fornecedorRequisicao, 
                    "DataExport-Export-" + templateId);
            
            int statusCode = response.statusCode();
            logger.debug("Status da resposta de export: {}", statusCode);
            
            if (statusCode == 200 || statusCode == 201 || statusCode == 202) {
                String responseBody = response.body();
                logger.debug("Resposta de export recebida: {}", responseBody);
                
                JsonNode rootNode = objectMapper.readTree(responseBody);
                String idArquivo = null;
                if (rootNode.has("id")) {
                    idArquivo = rootNode.get("id").asText();
                } else if (rootNode.path("data").has("id")) {
                    idArquivo = rootNode.path("data").path("id").asText();
                } else if (rootNode.has("report_file_id")) {
                    idArquivo = rootNode.get("report_file_id").asText();
                }
                if (idArquivo != null && !idArquivo.isBlank()) {
                    logger.info("Solicitação de export aceita. ID do arquivo: {}", idArquivo);
                    return idArquivo;
                } else {
                    logger.error("Resposta de export não contém ID do arquivo: {}", responseBody);
                    return null;
                }
            } else {
                logger.error("Erro na solicitação de export para template {}. Status: {}, Resposta: {}", 
                        templateId, statusCode, response.body());
                return null;
            }
            
        } catch (IOException e) {
            logger.error("Erro de I/O ao solicitar export do template {}: {}", templateId, e.getMessage(), e);
            return null;
        } catch (InterruptedException e) {
            logger.error("Requisição de export interrompida para template {}: {}", templateId, e.getMessage());
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {
            logger.error("Erro inesperado ao solicitar export do template {}: {}", templateId, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Aguarda a conclusão da geração do relatório e retorna a URL de download.
     * 
     * @param idArquivo ID do arquivo sendo gerado
     * @return URL de download ou null em caso de erro/timeout
     */
    private String aguardarConclusaoRelatorio(String idArquivo) {
        final int MAX_TENTATIVAS = 30; // Máximo 5 minutos (30 * 10 segundos)
        final int INTERVALO_SEGUNDOS = 10;
        
        logger.info("Aguardando conclusão do relatório. ID do arquivo: {}", idArquivo);
        
        for (int tentativa = 1; tentativa <= MAX_TENTATIVAS; tentativa++) {
            try {
                // Constrói a URL para verificar status
                String urlStatus = urlBase + "/api/analytics/report_files/" + idArquivo;
                logger.debug("Verificando status (tentativa {}/{}): {}", tentativa, MAX_TENTATIVAS, urlStatus);
                
                // Cria a requisição HTTP GET
                final String urlFinalParaLambda = urlStatus;
                java.util.function.Supplier<HttpRequest> fornecedorRequisicao = () -> HttpRequest.newBuilder()
                        .uri(URI.create(urlFinalParaLambda))
                        .GET()
                        .header("Authorization", "Bearer " + token)
                        .header("Accept", "application/json")
                        .timeout(Duration.ofSeconds(30))
                        .build();
                
                // Executa a requisição
                HttpResponse<String> response = br.com.extrator.util.UtilitarioHttpRetry.executarComRetry(
                        httpClient, 
                        fornecedorRequisicao, 
                        "DataExport-Status-" + idArquivo);
                
                int statusCode = response.statusCode();
                
                if (statusCode == 200) {
                    String responseBody = response.body();
                    JsonNode rootNode = objectMapper.readTree(responseBody);
                    
                    if (rootNode.has("status")) {
                        String status = rootNode.get("status").asText();
                        logger.debug("Status do arquivo {}: {}", idArquivo, status);
                        
                        // Sucesso: aceitar múltiplas variações de status
                        if ("generated".equalsIgnoreCase(status) ||
                            "completed".equalsIgnoreCase(status) ||
                            "done".equalsIgnoreCase(status) ||
                            "ready".equalsIgnoreCase(status)) {
                            String urlDownload = null;
                            if (rootNode.has("download_url")) {
                                urlDownload = rootNode.get("download_url").asText();
                            } else if (rootNode.has("downloadUrl")) {
                                urlDownload = rootNode.get("downloadUrl").asText();
                            } else if (rootNode.path("data").has("download_url")) {
                                urlDownload = rootNode.path("data").path("download_url").asText();
                            }
                            if (urlDownload != null && !urlDownload.isBlank()) {
                                logger.info("Relatório concluído! URL de download: {}", urlDownload);
                                return urlDownload;
                            } else {
                                logger.error("Arquivo concluído mas sem URL de download: {}", responseBody);
                                return null;
                            }
                        }
                        
                        if ("failed".equalsIgnoreCase(status) || "error".equalsIgnoreCase(status)) {
                            logger.error("Geração do relatório falhou. Status: {}, Resposta: {}", status, responseBody);
                            return null;
                        }
                        
                        // Status ainda em processamento
                        logger.debug("Relatório ainda em processamento. Status: {}", status);
                    } else {
                        logger.warn("Resposta de status não contém campo 'status': {}", responseBody);
                    }
                } else {
                    logger.warn("Erro ao verificar status do arquivo {}. Status HTTP: {}, Resposta: {}", 
                            idArquivo, statusCode, response.body());
                }
                
                // Aguarda antes da próxima tentativa (exceto na última)
                if (tentativa < MAX_TENTATIVAS) {
                    logger.debug("Aguardando {} segundos antes da próxima verificação...", INTERVALO_SEGUNDOS);
                    LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(INTERVALO_SEGUNDOS));
                }
                
            } catch (IOException e) {
                logger.error("Erro de I/O ao verificar status do arquivo {}: {}", idArquivo, e.getMessage(), e);
            } catch (InterruptedException e) {
                logger.error("Verificação de status interrompida para arquivo {}: {}", idArquivo, e.getMessage());
                Thread.currentThread().interrupt();
                return null;
            } catch (Exception e) {
                logger.error("Erro inesperado ao verificar status do arquivo {}: {}", idArquivo, e.getMessage(), e);
            }
        }
        
        logger.error("Timeout ao aguardar conclusão do relatório. ID do arquivo: {}", idArquivo);
        return null;
    }
    
    /**
     * Baixa o arquivo do relatório e processa seu conteúdo.
     * 
     * @param urlDownload URL para download do arquivo
     * @param tipoEntidade Tipo da entidade para os objetos EntidadeDinamica
     * @return Lista de entidades dinâmicas processadas
     */
    private List<EntidadeDinamica> baixarEProcessarArquivo(String urlDownload, String tipoEntidade) {
        try {
            logger.info("Baixando arquivo do relatório: {}", urlDownload);
            
            // Cria a requisição HTTP GET para download
            final String urlFinalParaLambda = urlDownload;
            java.util.function.Supplier<HttpRequest> fornecedorRequisicao = () -> HttpRequest.newBuilder()
                    .uri(URI.create(urlFinalParaLambda))
                    .GET()
                    .header("Authorization", "Bearer " + token)
                    .timeout(Duration.ofSeconds(120)) // Timeout maior para download
                    .build();
            
            // Executa o download como bytes (não como String para arquivos binários)
            HttpResponse<byte[]> response = br.com.extrator.util.UtilitarioHttpRetry.executarComRetryBytes(
                    httpClient, 
                    fornecedorRequisicao, 
                    "DataExport-Download");
            
            int statusCode = response.statusCode();
            
            if (statusCode == 200) {
                byte[] conteudoArquivo = response.body();
                logger.info("Arquivo baixado com sucesso. Tamanho: {} bytes", conteudoArquivo.length);
                
                // Processa o arquivo XLSX usando Apache POI
                return processarArquivoExcel(conteudoArquivo, tipoEntidade);
            } else {
                logger.error("Erro ao baixar arquivo. Status: {}", statusCode);
                return new ArrayList<>();
            }
            
        } catch (IOException e) {
            logger.error("Erro de I/O ao baixar arquivo: {}", e.getMessage(), e);
            return new ArrayList<>();
        } catch (InterruptedException e) {
            logger.error("Download interrompido: {}", e.getMessage());
            Thread.currentThread().interrupt();
            return new ArrayList<>();
        } catch (Exception e) {
            logger.error("Erro inesperado ao baixar arquivo: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
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
        } catch (Exception e) {
            logger.warn("Template ID inválido para '{}'. Usando padrão {}. Detalhes: {}", propKey, padrao, e.getMessage());
        }
        return padrao;
    }
    
    /**
     * Processa um arquivo Excel (XLSX) baixado da API e converte para lista de EntidadeDinamica.
     * 
     * @param dadosArquivo Os bytes do arquivo XLSX baixado
     * @param nomeTabela Nome da tabela para logs
     * @return Lista de EntidadeDinamica com os dados do arquivo
     */
    private List<EntidadeDinamica> processarArquivoExcel(byte[] dadosArquivo, String nomeTabela) {
        List<EntidadeDinamica> resultados = new ArrayList<>();
        
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(dadosArquivo);
             Workbook workbook = new XSSFWorkbook(inputStream)) {
            
            // Obtém a primeira planilha
            Sheet sheet = workbook.getSheetAt(0);
            
            // Verifica se a planilha tem dados
            if (sheet.getPhysicalNumberOfRows() == 0) {
                logger.warn("Planilha vazia para tabela: {}", nomeTabela);
                return resultados;
            }
            
            // Obtém os cabeçalhos da primeira linha
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                logger.warn("Linha de cabeçalho não encontrada para tabela: {}", nomeTabela);
                return resultados;
            }
            
            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(getCellValueAsString(cell));
            }
            
            logger.info("Cabeçalhos encontrados para {}: {}", nomeTabela, headers);
            
            // Processa as linhas de dados (a partir da linha 1)
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) continue;
                
                EntidadeDinamica entidade = new EntidadeDinamica();
                
                // Processa cada célula da linha
                for (int cellIndex = 0; cellIndex < headers.size(); cellIndex++) {
                    Cell cell = row.getCell(cellIndex);
                    String columnName = headers.get(cellIndex);
                    String cellValue = getCellValueAsString(cell);
                    
                    entidade.adicionarCampo(columnName, cellValue);
                }
                
                resultados.add(entidade);
            }
            
            logger.info("Processadas {} linhas de dados para tabela: {}", resultados.size(), nomeTabela);
            
        } catch (IOException e) {
            logger.error("Erro ao processar arquivo Excel para tabela {}: {}", nomeTabela, e.getMessage(), e);
            throw new RuntimeException("Falha ao processar arquivo Excel", e);
        }
        
        return resultados;
    }
    
    /**
     * Converte o valor de uma célula Excel para String, tratando diferentes tipos de dados.
     * 
     * @param cell A célula do Excel
     * @return O valor da célula como String
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                // Verifica se é uma data
                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    // Para números, remove decimais desnecessários
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue == Math.floor(numericValue)) {
                        return String.valueOf((long) numericValue);
                    } else {
                        return String.valueOf(numericValue);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                // Para fórmulas, tenta obter o valor calculado
                try {
                    return getCellValueAsString(cell.getCachedFormulaResultType(), cell);
                } catch (Exception e) {
                    return cell.getCellFormula();
                }
            case BLANK:
            case _NONE:
            default:
                return "";
        }
    }
    
    /**
     * Método auxiliar para obter valor de célula com fórmula.
     */
    private String getCellValueAsString(org.apache.poi.ss.usermodel.CellType cellType, Cell cell) {
        return switch (cellType) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getDateCellValue().toString();
                } else {
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue == Math.floor(numericValue)) {
                        yield String.valueOf((long) numericValue);
                    } else {
                        yield String.valueOf(numericValue);
                    }
                }
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }
}