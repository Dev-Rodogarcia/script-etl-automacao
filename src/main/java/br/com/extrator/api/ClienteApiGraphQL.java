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

        while (hasNextPage) {
            try {
                // PROTEÇÃO 1: Limite máximo de páginas
                int limitePaginas = CarregadorConfig.obterLimitePaginasApiGraphQL();
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
     * Busca coletas via GraphQL para uma data específica
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

        // Construir variáveis usando exclusivamente requestDate
        String dataFormatada = formatarDataParaApiGraphQL(dataReferencia);
        Map<String, Object> variaveis = Map.of(
            "params", Map.of("requestDate", dataFormatada)
        );

        logger.info("Buscando coletas via GraphQL - Data: {}", dataFormatada);

        return executarQueryPaginada(query, "pick", variaveis, ColetaNodeDTO.class);
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