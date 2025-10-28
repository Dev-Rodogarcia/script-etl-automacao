package br.com.extrator.api;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
 */
public class ClienteApiGraphQL {
    private static final Logger logger = LoggerFactory.getLogger(ClienteApiGraphQL.class);
    private final String urlBase;
    private final String endpointGraphQL;
    private final String token;
    private final HttpClient clienteHttp;
    private final ObjectMapper mapeadorJson;
    private final GerenciadorRequisicaoHttp gerenciadorRequisicao;
    private final Duration timeoutRequisicao;

    /**
     * Executa uma query GraphQL com paginação automática
     * 
     * @param query Query GraphQL a ser executada
     * @param nomeEntidade Nome da entidade na resposta GraphQL
     * @param variaveis Variáveis da query GraphQL
     * @param tipoClasse Classe para desserialização tipada
     * @return Lista completa de entidades de todas as páginas
     */
    private <T> List<T> executarQueryPaginada(String query, String nomeEntidade, Map<String, Object> variaveis, Class<T> tipoClasse) {
        List<T> todasEntidades = new ArrayList<>();
        String cursor = null;
        boolean hasNextPage = true;
        int paginaAtual = 1;

        while (hasNextPage) {
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
            
            // Atualizar informações de paginação
            hasNextPage = resposta.getHasNextPage();
            cursor = resposta.getEndCursor();
            
            logger.debug("Página {} processada: {} entidades encontradas. Próxima página: {}", 
                        paginaAtual, resposta.getEntidades().size(), hasNextPage);
            
            paginaAtual++;
            
            // Não é mais necessário pausar entre requisições - o GerenciadorRequisicaoHttp já controla o throttling
        }

        logger.info("Paginação concluída para {}. Total de páginas: {}, Total de entidades: {}", 
                   nomeEntidade, paginaAtual - 1, todasEntidades.size());
        
        return todasEntidades;
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
     * Busca coletas via GraphQL para uma data específica
     * 
     * @param dataReferencia Data de referência para buscar as coletas (LocalDate)
     * @return Lista de coletas encontradas
     */
    public List<ColetaNodeDTO> buscarColetas(LocalDate dataReferencia) {
        String query = """
                query BuscarColetas($params: PickInput!, $after: String) {
                    pick(params: $params, after: $after, first: 100) {
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

        // Construir variáveis usando exclusivamente requestDate
        String dataFormatada = formatarDataParaApiGraphQL(dataReferencia);
        Map<String, Object> variaveis = Map.of(
            "params", Map.of("requestDate", dataFormatada)
        );

        logger.debug("Executando query GraphQL para coletas - URL: {}{}, Variáveis: {}", 
                    urlBase, endpointGraphQL, variaveis);

        return executarQueryPaginada(query, "pick", variaveis, ColetaNodeDTO.class);
    }





    /**
     * Busca fretes via GraphQL para uma data específica
     * 
     * @param dataReferencia Data de referência para buscar os fretes (LocalDate)
     * @return Lista de fretes encontradas
     */
    public List<FreteNodeDTO> buscarFretes(LocalDate dataReferencia) {
        // Query GraphQL para fretes
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

        // Construir variáveis usando serviceAt e corporationId
        String dataFormatada = formatarDataParaApiGraphQL(dataReferencia);
        int corporationIdInt = Integer.parseInt(CarregadorConfig.obterCorporationId());
        
        Map<String, Object> variaveis = Map.of(
            "params", Map.of(
                "serviceAt", dataFormatada,
                "corporationId", corporationIdInt
            )
        );

        logger.debug("Executando query GraphQL para fretes - URL: {}{}, Variáveis: {}", 
                    urlBase, endpointGraphQL, variaveis);

        return executarQueryPaginada(query, "freight", variaveis, FreteNodeDTO.class);
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
}