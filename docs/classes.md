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
        // Query GraphQL expandida conforme documentação em docs/descobertas-endpoints/coletas.md
        // Query: BuscarColetasExpandidaV2, Tipo: Pick
        String query = """
                query BuscarColetasExpandidaV2($params: PickInput!, $after: String) {
                    pick(params: $params, after: $after, first: 100) {
                        edges {
                            cursor
                            node {
                                id
                                sequenceCode
                                requestDate
                                requestHour
                                serviceDate
                                serviceStartHour
                                finishDate
                                serviceEndHour
                                status
                                requester
                                invoicesVolumes
                                invoicesWeight
                                taxedWeight
                                invoicesValue
                                comments
                                agentId
                                manifestItemPickId
                                vehicleTypeId
                                customer {
                                    id
                                    name
                                }
                                pickAddress {
                                    line1
                                    city {
                                        name
                                        state {
                                            code
                                        }
                                    }
                                }
                                user {
                                    id
                                    name
                                }
                                invoicesCubedWeight
                                cancellationReason
                                cancellationUserId
                                cargoClassificationId
                                costCenterId
                                destroyReason
                                destroyUserId
                                lunchBreakEndHour
                                lunchBreakStartHour
                                notificationEmail
                                notificationPhone
                                pickTypeId
                                pickupLocationId
                                statusUpdatedAt
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
        // Query GraphQL expandida conforme documentação em docs/descobertas-endpoints/fretes.md
        // Query: BuscarFretesExpandidaV3, Tipo: FreightBase
        String query = """
                query BuscarFretesExpandidaV3($params: FreightInput!, $after: String) {
                    freight(params: $params, after: $after, first: 100) {
                        edges {
                            node {
                                id
                                referenceNumber
                                serviceAt
                                total
                                subtotal
                                invoicesValue
                                invoicesTotalVolumes
                                taxedWeight
                                realWeight
                                totalCubicVolume
                                payer {
                                    id
                                    name
                                }
                                sender {
                                    id
                                    name
                                    mainAddress {
                                        city {
                                            name
                                            state {
                                                code
                                            }
                                        }
                                    }
                                }
                                receiver {
                                    id
                                    name
                                    mainAddress {
                                        city {
                                            name
                                            state {
                                                code
                                            }
                                        }
                                    }
                                }
                                corporation {
                                    name
                                }
                                customerPriceTable {
                                    name
                                }
                                freightClassification {
                                    name
                                }
                                costCenter {
                                    name
                                }
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
                                redispatchSubtotal
                                secCatSubtotal
                                serviceDate
                                serviceType
                                status
                                suframaSubtotal
                                tdeSubtotal
                                tollSubtotal
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

import java.util.List;

/**
 * Classe auxiliar para encapsular a resposta paginada de uma query GraphQL.
 * Contém os dados da página atual e as informações de paginação necessárias
 * para continuar buscando as próximas páginas.
 */
public class PaginatedGraphQLResponse<T> {
    
    private final List<T> entidades;
    private final boolean hasNextPage;
    private final String endCursor;
    
    /**
     * Construtor da resposta paginada
     * 
     * @param entidades Lista de entidades da página atual
     * @param hasNextPage Indica se há próxima página disponível
     * @param endCursor Cursor para buscar a próxima página
     */
    public PaginatedGraphQLResponse(List<T> entidades, boolean hasNextPage, String endCursor) {
        this.entidades = entidades;
        this.hasNextPage = hasNextPage;
        this.endCursor = endCursor;
    }
    
    /**
     * @return Lista de entidades da página atual
     */
    public List<T> getEntidades() {
        return entidades;
    }
    
    /**
     * @return true se há próxima página disponível, false caso contrário
     */
    public boolean getHasNextPage() {
        return hasNextPage;
    }
    
    /**
     * @return Cursor para buscar a próxima página
     */
    public String getEndCursor() {
        return endCursor;
    }
}


package br.com.extrator.db.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Entity (Entidade) que representa uma linha na tabela 'fretes' do banco de dados.
 * Contém os campos-chave "promovidos" para acesso rápido e indexação,
 * e uma coluna 'metadata' para armazenar o JSON bruto completo, garantindo
 * 100% de completude e resiliência a futuras mudanças na API.
 */
public class FreteEntity {

    // --- Coluna de Chave Primária ---
    private Long id;

    // --- Colunas Essenciais para Indexação e Relatórios ---
    private OffsetDateTime servicoEm;
    private OffsetDateTime criadoEm;
    private String status;
    private String modal;
    private String tipoFrete;
    private BigDecimal valorTotal;
    private BigDecimal valorNotas;
    private BigDecimal pesoNotas;
    private Long idCorporacao;
    private Long idCidadeDestino;
    private LocalDate dataPrevisaoEntrega;

    // --- Campos Expandidos (22 campos do CSV) ---
    private Long pagadorId;
    private String pagadorNome;
    private Long remetenteId;
    private String remetenteNome;
    private String origemCidade;
    private String origemUf;
    private Long destinatarioId;
    private String destinatarioNome;
    private String destinoCidade;
    private String destinoUf;
    private String filialNome;
    private String numeroNotaFiscal;
    private String tabelaPrecoNome;
    private String classificacaoNome;
    private String centroCustoNome;
    private String usuarioNome;
    private String referenceNumber;
    private Integer invoicesTotalVolumes;
    private BigDecimal taxedWeight;
    private BigDecimal realWeight;
    private BigDecimal totalCubicVolume;
    private BigDecimal subtotal;

    private String chaveCte;
    private Integer numeroCte;
    private Integer serieCte;

    // --- Coluna de Metadados ---
    private String metadata;

    // --- Getters e Setters ---

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public OffsetDateTime getServicoEm() {
        return servicoEm;
    }

    public void setServicoEm(final OffsetDateTime servicoEm) {
        this.servicoEm = servicoEm;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(final OffsetDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public String getModal() {
        return modal;
    }

    public void setModal(final String modal) {
        this.modal = modal;
    }

    public String getTipoFrete() {
        return tipoFrete;
    }

    public void setTipoFrete(final String tipoFrete) {
        this.tipoFrete = tipoFrete;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(final BigDecimal valorTotal) {
        this.valorTotal = valorTotal;
    }

    public BigDecimal getValorNotas() {
        return valorNotas;
    }

    public void setValorNotas(final BigDecimal valorNotas) {
        this.valorNotas = valorNotas;
    }

    public BigDecimal getPesoNotas() {
        return pesoNotas;
    }

    public void setPesoNotas(final BigDecimal pesoNotas) {
        this.pesoNotas = pesoNotas;
    }

    public Long getIdCorporacao() {
        return idCorporacao;
    }

    public void setIdCorporacao(final Long idCorporacao) {
        this.idCorporacao = idCorporacao;
    }

    public Long getIdCidadeDestino() {
        return idCidadeDestino;
    }

    public void setIdCidadeDestino(final Long idCidadeDestino) {
        this.idCidadeDestino = idCidadeDestino;
    }

    public LocalDate getDataPrevisaoEntrega() {
        return dataPrevisaoEntrega;
    }

    public void setDataPrevisaoEntrega(final LocalDate dataPrevisaoEntrega) {
        this.dataPrevisaoEntrega = dataPrevisaoEntrega;
    }

    // --- Getters e Setters para Campos Expandidos ---

    public Long getPagadorId() {
        return pagadorId;
    }

    public void setPagadorId(Long pagadorId) {
        this.pagadorId = pagadorId;
    }

    public String getPagadorNome() {
        return pagadorNome;
    }

    public void setPagadorNome(String pagadorNome) {
        this.pagadorNome = pagadorNome;
    }

    public Long getRemetenteId() {
        return remetenteId;
    }

    public void setRemetenteId(Long remetenteId) {
        this.remetenteId = remetenteId;
    }

    public String getRemetenteNome() {
        return remetenteNome;
    }

    public void setRemetenteNome(String remetenteNome) {
        this.remetenteNome = remetenteNome;
    }

    public String getOrigemCidade() {
        return origemCidade;
    }

    public void setOrigemCidade(String origemCidade) {
        this.origemCidade = origemCidade;
    }

    public String getOrigemUf() {
        return origemUf;
    }

    public void setOrigemUf(String origemUf) {
        this.origemUf = origemUf;
    }

    public Long getDestinatarioId() {
        return destinatarioId;
    }

    public void setDestinatarioId(Long destinatarioId) {
        this.destinatarioId = destinatarioId;
    }

    public String getDestinatarioNome() {
        return destinatarioNome;
    }

    public void setDestinatarioNome(String destinatarioNome) {
        this.destinatarioNome = destinatarioNome;
    }

    public String getDestinoCidade() {
        return destinoCidade;
    }

    public void setDestinoCidade(String destinoCidade) {
        this.destinoCidade = destinoCidade;
    }

    public String getDestinoUf() {
        return destinoUf;
    }

    public void setDestinoUf(String destinoUf) {
        this.destinoUf = destinoUf;
    }

    public String getFilialNome() {
        return filialNome;
    }

    public void setFilialNome(String filialNome) {
        this.filialNome = filialNome;
    }

    public String getNumeroNotaFiscal() {
        return numeroNotaFiscal;
    }

    public void setNumeroNotaFiscal(String numeroNotaFiscal) {
        this.numeroNotaFiscal = numeroNotaFiscal;
    }

    public String getTabelaPrecoNome() {
        return tabelaPrecoNome;
    }

    public void setTabelaPrecoNome(String tabelaPrecoNome) {
        this.tabelaPrecoNome = tabelaPrecoNome;
    }

    public String getClassificacaoNome() {
        return classificacaoNome;
    }

    public void setClassificacaoNome(String classificacaoNome) {
        this.classificacaoNome = classificacaoNome;
    }

    public String getCentroCustoNome() {
        return centroCustoNome;
    }

    public void setCentroCustoNome(String centroCustoNome) {
        this.centroCustoNome = centroCustoNome;
    }

    public String getUsuarioNome() {
        return usuarioNome;
    }

    public void setUsuarioNome(String usuarioNome) {
        this.usuarioNome = usuarioNome;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public Integer getInvoicesTotalVolumes() {
        return invoicesTotalVolumes;
    }

    public void setInvoicesTotalVolumes(Integer invoicesTotalVolumes) {
        this.invoicesTotalVolumes = invoicesTotalVolumes;
    }

    public BigDecimal getTaxedWeight() {
        return taxedWeight;
    }

    public void setTaxedWeight(BigDecimal taxedWeight) {
        this.taxedWeight = taxedWeight;
    }

    public BigDecimal getRealWeight() {
        return realWeight;
    }

    public void setRealWeight(BigDecimal realWeight) {
        this.realWeight = realWeight;
    }

    public BigDecimal getTotalCubicVolume() {
        return totalCubicVolume;
    }

    public void setTotalCubicVolume(BigDecimal totalCubicVolume) {
        this.totalCubicVolume = totalCubicVolume;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public String getChaveCte() {
        return chaveCte;
    }

    public void setChaveCte(final String chaveCte) {
        this.chaveCte = chaveCte;
    }

    public Integer getNumeroCte() {
        return numeroCte;
    }

    public void setNumeroCte(final Integer numeroCte) {
        this.numeroCte = numeroCte;
    }

    public Integer getSerieCte() {
        return serieCte;
    }

    public void setSerieCte(final Integer serieCte) {
        this.serieCte = serieCte;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(final String metadata) {
        this.metadata = metadata;
    }
}


package br.com.extrator.db.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.extrator.db.entity.FreteEntity;

/**
 * Repositório para operações de persistência da entidade FreteEntity.
 * Implementa a arquitetura de persistência híbrida: colunas-chave para indexação
 * e uma coluna de metadados para resiliência e completude dos dados.
 * Utiliza operações MERGE (UPSERT) com a chave primária (id) do frete.
 */
public class FreteRepository extends AbstractRepository<FreteEntity> {
    private static final Logger logger = LoggerFactory.getLogger(FreteRepository.class);
    private static final String NOME_TABELA = "fretes";

    @Override
    protected String getNomeTabela() {
        return NOME_TABELA;
    }

    /**
     * Cria a tabela 'fretes' se ela não existir, seguindo o modelo híbrido.
     * A estrutura contém apenas colunas essenciais para busca e uma coluna NVARCHAR(MAX)
     * para armazenar o JSON completo, garantindo resiliência.
     */
    @Override
    protected void criarTabelaSeNaoExistir(Connection conexao) throws SQLException {
        if (!verificarTabelaExiste(conexao, NOME_TABELA)) {
            logger.info("Criando tabela {} com arquitetura híbrida...", NOME_TABELA);

            String sql = """
                CREATE TABLE fretes (
                    -- Coluna de Chave Primária
                    id BIGINT PRIMARY KEY,

                    -- Colunas Essenciais "Promovidas" para Indexação e Relatórios
                    servico_em DATETIMEOFFSET,
                    criado_em DATETIMEOFFSET,
                    status NVARCHAR(50),
                    modal NVARCHAR(50),
                    tipo_frete NVARCHAR(100),
                    valor_total DECIMAL(18, 2),
                    valor_notas DECIMAL(18, 2),
                    peso_notas DECIMAL(18, 3),
                    id_corporacao BIGINT,
                    id_cidade_destino BIGINT,
                    data_previsao_entrega DATE,

                    -- Campos Expandidos (22 campos do CSV)
                    pagador_id BIGINT,
                    pagador_nome NVARCHAR(255),
                    remetente_id BIGINT,
                    remetente_nome NVARCHAR(255),
                    origem_cidade NVARCHAR(255),
                    origem_uf NVARCHAR(10),
                    destinatario_id BIGINT,
                    destinatario_nome NVARCHAR(255),
                    destino_cidade NVARCHAR(255),
                    destino_uf NVARCHAR(10),
                    filial_nome NVARCHAR(255),
                    numero_nota_fiscal NVARCHAR(100),
                    tabela_preco_nome NVARCHAR(255),
                    classificacao_nome NVARCHAR(255),
                    centro_custo_nome NVARCHAR(255),
                    usuario_nome NVARCHAR(255),
                    reference_number NVARCHAR(100),
                    invoices_total_volumes INT,
                    taxed_weight DECIMAL(18, 3),
                    real_weight DECIMAL(18, 3),
                    total_cubic_volume DECIMAL(18, 3),
                    subtotal DECIMAL(18, 2),

                    -- CT-e (chave, número, série)
                    chave_cte NVARCHAR(100),
                    numero_cte INT,
                    serie_cte INT,

                    -- Coluna de Metadados para Resiliência e Completude
                    metadata NVARCHAR(MAX),

                    -- Coluna de Auditoria
                    data_extracao DATETIME2 DEFAULT GETDATE()
                )
                """;

            executarDDL(conexao, sql);
            logger.info("Tabela {} criada com sucesso.", NOME_TABELA);
        }
        criarViewPowerBISeNaoExistir(conexao);
    }

    private void criarViewPowerBISeNaoExistir(final Connection conexao) throws SQLException {
        final String sqlView = """
            CREATE OR ALTER VIEW dbo.vw_fretes_powerbi AS
            SELECT
                id AS [ID],
                chave_cte AS [Chave CT-e],
                numero_cte AS [Numero CT-e],
                serie_cte AS [Serie CT-e],
                servico_em AS [Data frete],
                valor_total AS [Valor Total do Servico],
                valor_notas AS [Valor NF],
                subtotal AS [Valor Frete],
                invoices_total_volumes AS [Volumes],
                taxed_weight AS [Kg Taxado],
                real_weight AS [Kg Real],
                total_cubic_volume AS [M3],
                pagador_nome AS [Pagador],
                remetente_nome AS [Remetente],
                origem_cidade AS [Origem],
                origem_uf AS [UF Origem],
                destinatario_nome AS [Destinatario],
                destino_cidade AS [Destino],
                destino_uf AS [UF Destino],
                filial_nome AS [Filial],
                tabela_preco_nome AS [Tabela de Preco],
                classificacao_nome AS [Classificacao],
                centro_custo_nome AS [Centro de Custo],
                usuario_nome AS [Usuario],
                numero_nota_fiscal AS [NF],
                status AS [Status],
                modal AS [Modal],
                tipo_frete AS [Tipo Frete],
                data_extracao AS [Data de extracao]
            FROM dbo.fretes;
        """;
        executarDDL(conexao, sqlView);
    }

    /**
     * Executa a operação MERGE (UPSERT) para inserir ou atualizar um frete no banco.
     * A lógica é segura e baseada na nova arquitetura de Entidade.
     */
    @Override
    protected int executarMerge(Connection conexao, FreteEntity frete) throws SQLException {
        // Para Fretes, o 'id' é a única chave confiável para o MERGE.
        if (frete.getId() == null) {
            throw new SQLException("Não é possível executar o MERGE para Frete sem um ID.");
        }

        String sql = String.format("""
            MERGE %s AS target
            USING (VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?))
                AS source (id, servico_em, criado_em, status, modal, tipo_frete, valor_total, valor_notas, peso_notas, id_corporacao, id_cidade_destino, data_previsao_entrega,
                           pagador_id, pagador_nome, remetente_id, remetente_nome, origem_cidade, origem_uf, destinatario_id, destinatario_nome, destino_cidade, destino_uf,
                           filial_nome, numero_nota_fiscal, tabela_preco_nome, classificacao_nome, centro_custo_nome, usuario_nome, reference_number, chave_cte, numero_cte, serie_cte, invoices_total_volumes,
                           taxed_weight, real_weight, total_cubic_volume, subtotal, metadata, data_extracao)
            ON target.id = source.id
            WHEN MATCHED THEN
                UPDATE SET
                    servico_em = source.servico_em,
                    criado_em = source.criado_em,
                    status = source.status,
                    modal = source.modal,
                    tipo_frete = source.tipo_frete,
                    valor_total = source.valor_total,
                    valor_notas = source.valor_notas,
                    peso_notas = source.peso_notas,
                    id_corporacao = source.id_corporacao,
                    id_cidade_destino = source.id_cidade_destino,
                    data_previsao_entrega = source.data_previsao_entrega,
                    pagador_id = source.pagador_id,
                    pagador_nome = source.pagador_nome,
                    remetente_id = source.remetente_id,
                    remetente_nome = source.remetente_nome,
                    origem_cidade = source.origem_cidade,
                    origem_uf = source.origem_uf,
                    destinatario_id = source.destinatario_id,
                    destinatario_nome = source.destinatario_nome,
                    destino_cidade = source.destino_cidade,
                    destino_uf = source.destino_uf,
                    filial_nome = source.filial_nome,
                    numero_nota_fiscal = source.numero_nota_fiscal,
                    tabela_preco_nome = source.tabela_preco_nome,
                    classificacao_nome = source.classificacao_nome,
                    centro_custo_nome = source.centro_custo_nome,
                    usuario_nome = source.usuario_nome,
                    reference_number = source.reference_number,
                    chave_cte = source.chave_cte,
                    numero_cte = source.numero_cte,
                    serie_cte = source.serie_cte,
                    invoices_total_volumes = source.invoices_total_volumes,
                    taxed_weight = source.taxed_weight,
                    real_weight = source.real_weight,
                    total_cubic_volume = source.total_cubic_volume,
                    subtotal = source.subtotal,
                    metadata = source.metadata,
                    data_extracao = source.data_extracao
            WHEN NOT MATCHED THEN
                INSERT (id, servico_em, criado_em, status, modal, tipo_frete, valor_total, valor_notas, peso_notas, id_corporacao, id_cidade_destino, data_previsao_entrega,
                        pagador_id, pagador_nome, remetente_id, remetente_nome, origem_cidade, origem_uf, destinatario_id, destinatario_nome, destino_cidade, destino_uf,
                        filial_nome, numero_nota_fiscal, tabela_preco_nome, classificacao_nome, centro_custo_nome, usuario_nome, reference_number, chave_cte, numero_cte, serie_cte, invoices_total_volumes,
                        taxed_weight, real_weight, total_cubic_volume, subtotal, metadata, data_extracao)
                VALUES (source.id, source.servico_em, source.criado_em, source.status, source.modal, source.tipo_frete, source.valor_total, source.valor_notas, source.peso_notas, source.id_corporacao, source.id_cidade_destino, source.data_previsao_entrega,
                        source.pagador_id, source.pagador_nome, source.remetente_id, source.remetente_nome, source.origem_cidade, source.origem_uf, source.destinatario_id, source.destinatario_nome, source.destino_cidade, source.destino_uf,
                        source.filial_nome, source.numero_nota_fiscal, source.tabela_preco_nome, source.classificacao_nome, source.centro_custo_nome, source.usuario_nome, source.reference_number, source.chave_cte, source.numero_cte, source.serie_cte, source.invoices_total_volumes,
                        source.taxed_weight, source.real_weight, source.total_cubic_volume, source.subtotal, source.metadata, source.data_extracao);
            """, NOME_TABELA);

        try (PreparedStatement statement = conexao.prepareStatement(sql)) {
            // Define os parâmetros de forma segura e na ordem correta.
            int paramIndex = 1;
            statement.setObject(paramIndex++, frete.getId(), Types.BIGINT);
            statement.setObject(paramIndex++, frete.getServicoEm(), Types.TIMESTAMP_WITH_TIMEZONE);
            statement.setObject(paramIndex++, frete.getCriadoEm(), Types.TIMESTAMP_WITH_TIMEZONE);
            statement.setString(paramIndex++, frete.getStatus());
            statement.setString(paramIndex++, frete.getModal());
            statement.setString(paramIndex++, frete.getTipoFrete());
            statement.setBigDecimal(paramIndex++, frete.getValorTotal());
            statement.setBigDecimal(paramIndex++, frete.getValorNotas());
            statement.setBigDecimal(paramIndex++, frete.getPesoNotas());
            statement.setObject(paramIndex++, frete.getIdCorporacao(), Types.BIGINT);
            statement.setObject(paramIndex++, frete.getIdCidadeDestino(), Types.BIGINT);
            statement.setObject(paramIndex++, frete.getDataPrevisaoEntrega(), Types.DATE);
            // Campos expandidos (22 campos do CSV)
            statement.setObject(paramIndex++, frete.getPagadorId(), Types.BIGINT);
            statement.setString(paramIndex++, frete.getPagadorNome());
            statement.setObject(paramIndex++, frete.getRemetenteId(), Types.BIGINT);
            statement.setString(paramIndex++, frete.getRemetenteNome());
            statement.setString(paramIndex++, frete.getOrigemCidade());
            statement.setString(paramIndex++, frete.getOrigemUf());
            statement.setObject(paramIndex++, frete.getDestinatarioId(), Types.BIGINT);
            statement.setString(paramIndex++, frete.getDestinatarioNome());
            statement.setString(paramIndex++, frete.getDestinoCidade());
            statement.setString(paramIndex++, frete.getDestinoUf());
            statement.setString(paramIndex++, frete.getFilialNome());
            statement.setString(paramIndex++, frete.getNumeroNotaFiscal());
            statement.setString(paramIndex++, frete.getTabelaPrecoNome());
            statement.setString(paramIndex++, frete.getClassificacaoNome());
            statement.setString(paramIndex++, frete.getCentroCustoNome());
            statement.setString(paramIndex++, frete.getUsuarioNome());
            statement.setString(paramIndex++, frete.getReferenceNumber());
            statement.setString(paramIndex++, frete.getChaveCte());
            if (frete.getNumeroCte() != null) {
                statement.setObject(paramIndex++, frete.getNumeroCte(), Types.INTEGER);
            } else {
                statement.setNull(paramIndex++, Types.INTEGER);
            }
            if (frete.getSerieCte() != null) {
                statement.setObject(paramIndex++, frete.getSerieCte(), Types.INTEGER);
            } else {
                statement.setNull(paramIndex++, Types.INTEGER);
            }
            statement.setObject(paramIndex++, frete.getInvoicesTotalVolumes(), Types.INTEGER);
            statement.setBigDecimal(paramIndex++, frete.getTaxedWeight());
            statement.setBigDecimal(paramIndex++, frete.getRealWeight());
            statement.setBigDecimal(paramIndex++, frete.getTotalCubicVolume());
            statement.setBigDecimal(paramIndex++, frete.getSubtotal());
            statement.setString(paramIndex++, frete.getMetadata());
            setInstantParameter(statement, paramIndex++, Instant.now()); // UTC timestamp
            
            // Verificar se todos os parâmetros foram definidos (39 parâmetros = paramIndex final = 40)
            if (paramIndex != 40) {
                throw new SQLException(String.format("Número incorreto de parâmetros: esperado 39, definido %d", paramIndex - 1));
            }

            int rowsAffected = statement.executeUpdate();
            logger.debug("MERGE executado para Frete ID {}: {} linha(s) afetada(s)", frete.getId(), rowsAffected);
            return rowsAffected;
        }
    }
}




package br.com.extrator.modelo.graphql.fretes;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO aninhado para representar a Cidade (City) de um Endereço.
 * Conforme documentação em docs/descobertas-endpoints/fretes.md linha 90-96.
 */
public class CityDTO {
    @JsonProperty("name")
    private String name;

    @JsonProperty("state")
    private StateDTO state;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public StateDTO getState() {
        return state;
    }

    public void setState(StateDTO state) {
        this.state = state;
    }
}





package br.com.extrator.modelo.graphql.fretes;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO aninhado para representar a Filial (Corporation) de um Frete.
 * Conforme documentação em docs/descobertas-endpoints/fretes.md linha 152.
 */
public class CorporationDTO {
    @JsonProperty("name")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}




package br.com.extrator.modelo.graphql.fretes;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO aninhado para representar o Centro de Custo (CostCenter) de um Frete.
 * Conforme documentação em docs/descobertas-endpoints/fretes.md linha 172.
 */
public class CostCenterDTO {
    @JsonProperty("name")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

package br.com.extrator.modelo.graphql.fretes;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO aninhado para representar a Tabela de Preço (CustomerPriceTable) de um Frete.
 * Conforme documentação em docs/descobertas-endpoints/fretes.md linha 170.
 */
public class CustomerPriceTableDTO {
    @JsonProperty("name")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

package br.com.extrator.modelo.graphql.fretes;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO aninhado para representar a Classificação (FreightClassification) de um Frete.
 * Conforme documentação em docs/descobertas-endpoints/fretes.md linha 171.
 */
public class FreightClassificationDTO {
    @JsonProperty("name")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

package br.com.extrator.modelo.graphql.fretes;

import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonProperty;

public class FreightInvoiceDTO {
    public static class InvoiceDTO {
        @JsonProperty("number")
        private String number;
        @JsonProperty("series")
        private String series;
        @JsonProperty("key")
        private String key;
        @JsonProperty("value")
        private BigDecimal value;
        @JsonProperty("weight")
        private BigDecimal weight;

        public String getNumber() { return number; }
        public void setNumber(final String number) { this.number = number; }
        public String getSeries() { return series; }
        public void setSeries(final String series) { this.series = series; }
        public String getKey() { return key; }
        public void setKey(final String key) { this.key = key; }
        public BigDecimal getValue() { return value; }
        public void setValue(final BigDecimal value) { this.value = value; }
        public BigDecimal getWeight() { return weight; }
        public void setWeight(final BigDecimal weight) { this.weight = weight; }
    }

    @JsonProperty("invoice")
    private InvoiceDTO invoice;

    public InvoiceDTO getInvoice() { return invoice; }
    public void setInvoice(final InvoiceDTO invoice) { this.invoice = invoice; }
}


package br.com.extrator.modelo.graphql.fretes;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import br.com.extrator.db.entity.FreteEntity;

/**
 * Mapper (Tradutor) que transforma o FreteNodeDTO (dados brutos do GraphQL)
 * em uma FreteEntity (pronta para o banco de dados).
 * Converte tipos de data/hora e preserva 100% dos dados originais
 * na coluna de metadados.
 */
public class FreteMapper {

    private static final Logger logger = LoggerFactory.getLogger(FreteMapper.class);

    private final ObjectMapper objectMapper;

    public FreteMapper() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Converte o DTO de Frete em uma Entidade.
     * @param dto O objeto DTO com os dados do frete.
     * @return Um objeto FreteEntity pronto para ser salvo.
     */
    public FreteEntity toEntity(final FreteNodeDTO dto) {
        if (dto == null) {
            return null;
        }

        final FreteEntity entity = new FreteEntity();

        // 1. Mapeamento dos campos essenciais
        entity.setId(dto.getId());
        entity.setStatus(dto.getStatus());
        entity.setModal(dto.getModal());
        entity.setTipoFrete(dto.getType());
        entity.setValorTotal(dto.getTotalValue());
        entity.setValorNotas(dto.getInvoicesValue());
        entity.setPesoNotas(dto.getInvoicesWeight());
        entity.setIdCorporacao(dto.getCorporationId());
        entity.setIdCidadeDestino(dto.getDestinationCityId());

        // 1.1. Mapeamento dos campos expandidos (22 campos do CSV)
        if (dto.getPayer() != null) {
            entity.setPagadorId(dto.getPayer().getId());
            entity.setPagadorNome(dto.getPayer().getName());
        }

        if (dto.getSender() != null) {
            entity.setRemetenteId(dto.getSender().getId());
            entity.setRemetenteNome(dto.getSender().getName());
            if (dto.getSender().getMainAddress() != null && 
                dto.getSender().getMainAddress().getCity() != null) {
                entity.setOrigemCidade(dto.getSender().getMainAddress().getCity().getName());
                if (dto.getSender().getMainAddress().getCity().getState() != null) {
                    entity.setOrigemUf(dto.getSender().getMainAddress().getCity().getState().getCode());
                }
            }
        }

        if (dto.getReceiver() != null) {
            entity.setDestinatarioId(dto.getReceiver().getId());
            entity.setDestinatarioNome(dto.getReceiver().getName());
            if (dto.getReceiver().getMainAddress() != null && 
                dto.getReceiver().getMainAddress().getCity() != null) {
                entity.setDestinoCidade(dto.getReceiver().getMainAddress().getCity().getName());
                if (dto.getReceiver().getMainAddress().getCity().getState() != null) {
                    entity.setDestinoUf(dto.getReceiver().getMainAddress().getCity().getState().getCode());
                }
            }
        }

        // Mapear campos expandidos adicionais
        if (dto.getCorporation() != null) {
            entity.setFilialNome(dto.getCorporation().getName());
        }

        if (dto.getFreightInvoices() != null && !dto.getFreightInvoices().isEmpty()) {
            final java.util.List<String> numeros = new java.util.ArrayList<>();
            for (final FreightInvoiceDTO fi : dto.getFreightInvoices()) {
                if (fi != null && fi.getInvoice() != null && fi.getInvoice().getNumber() != null) {
                    numeros.add(fi.getInvoice().getNumber());
                }
            }
            if (!numeros.isEmpty()) {
                entity.setNumeroNotaFiscal(String.join(", ", numeros));
            }
        }

        if (dto.getCustomerPriceTable() != null) {
            entity.setTabelaPrecoNome(dto.getCustomerPriceTable().getName());
        }

        if (dto.getFreightClassification() != null) {
            entity.setClassificacaoNome(dto.getFreightClassification().getName());
        }

        if (dto.getCostCenter() != null) {
            entity.setCentroCustoNome(dto.getCostCenter().getName());
        }

        if (dto.getUser() != null) {
            entity.setUsuarioNome(dto.getUser().getName());
        }

        // Mapear campos simples adicionais (22 campos do CSV)
        entity.setReferenceNumber(dto.getReferenceNumber());
        entity.setInvoicesTotalVolumes(dto.getInvoicesTotalVolumes());
        entity.setTaxedWeight(dto.getTaxedWeight());
        entity.setRealWeight(dto.getRealWeight());
        entity.setTotalCubicVolume(dto.getTotalCubicVolume());
        entity.setSubtotal(dto.getSubtotal());

        if (dto.getCte() != null) {
            entity.setChaveCte(dto.getCte().getKey());
            entity.setNumeroCte(dto.getCte().getNumber());
            entity.setSerieCte(dto.getCte().getSeries());
        }

        // 2. Conversão segura de tipos de data e hora
        try {
            if (dto.getServiceAt() != null && !dto.getServiceAt().trim().isEmpty()) {
                entity.setServicoEm(OffsetDateTime.parse(dto.getServiceAt()));
            }
            if (dto.getCreatedAt() != null && !dto.getCreatedAt().trim().isEmpty()) {
                entity.setCriadoEm(OffsetDateTime.parse(dto.getCreatedAt()));
            }
            if (dto.getDeliveryPredictionDate() != null && !dto.getDeliveryPredictionDate().trim().isEmpty()) {
                entity.setDataPrevisaoEntrega(LocalDate.parse(dto.getDeliveryPredictionDate()));
            }
        } catch (final DateTimeParseException e) {
            logger.error("❌ Erro ao converter data para frete ID {}: serviceAt='{}', createdAt='{}', deliveryPredictionDate='{}' - {}", 
                dto.getId(), dto.getServiceAt(), dto.getCreatedAt(), dto.getDeliveryPredictionDate(), e.getMessage());
            logger.debug("Stack trace completo:", e);
        }

        // 3. Empacotamento de todos os metadados
        try {
            final String metadata = objectMapper.writeValueAsString(dto);
            entity.setMetadata(metadata);
        } catch (final JsonProcessingException e) {
            logger.error("❌ CRÍTICO: Falha ao serializar metadados para frete ID {}: {}", 
                dto.getId(), e.getMessage(), e);
            entity.setMetadata(String.format("{\"error\":\"Serialization failed\",\"id\":%d}", dto.getId()));
        }

        return entity;
    }
}


package br.com.extrator.modelo.graphql.fretes;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO (Data Transfer Object) para representar um "node" de Frete,
 * conforme retornado pela API GraphQL. Mapeia os campos essenciais
 * e inclui um contêiner dinâmico para capturar todas as outras
 * propriedades, garantindo resiliência e completude.
 */
public class FreteNodeDTO {

    // --- Campos Essenciais Mapeados ---
    @JsonProperty("id")
    private Long id;

    @JsonProperty("serviceAt")
    private String serviceAt; // Recebe como String para ser convertido para OffsetDateTime

    @JsonProperty("createdAt")
    private String createdAt; // Recebe como String para ser convertido para OffsetDateTime

    @JsonProperty("status")
    private String status;

    @JsonProperty("modal")
    private String modal;

    @JsonProperty("type")
    private String type;

    @JsonProperty("total")
    private BigDecimal totalValue;

    @JsonProperty("invoicesValue")
    private BigDecimal invoicesValue;

    @JsonProperty("invoicesWeight")
    private BigDecimal invoicesWeight;

    @JsonProperty("corporationId")
    private Long corporationId;

    @JsonProperty("destinationCityId")
    private Long destinationCityId;

    @JsonProperty("deliveryPredictionDate")
    private String deliveryPredictionDate; // Recebe como String para ser convertido para LocalDate

    // --- Campos Expandidos (Objetos Aninhados) ---
    @JsonProperty("payer")
    private PayerDTO payer;

    @JsonProperty("sender")
    private SenderDTO sender;

    @JsonProperty("receiver")
    private ReceiverDTO receiver;

    @JsonProperty("corporation")
    private CorporationDTO corporation;

    @JsonProperty("freightInvoices")
    private List<FreightInvoiceDTO> freightInvoices;

    @JsonProperty("customerPriceTable")
    private CustomerPriceTableDTO customerPriceTable;

    @JsonProperty("freightClassification")
    private FreightClassificationDTO freightClassification;

    @JsonProperty("costCenter")
    private CostCenterDTO costCenter;

    @JsonProperty("user")
    private UserDTO user;

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class CteDTO {
        @JsonProperty("key")
        private String key;
        @JsonProperty("number")
        private Integer number;
        @JsonProperty("series")
        private Integer series;

        public String getKey() { return key; }
        public void setKey(final String key) { this.key = key; }
        public Integer getNumber() { return number; }
        public void setNumber(final Integer number) { this.number = number; }
        public Integer getSeries() { return series; }
        public void setSeries(final Integer series) { this.series = series; }
    }

    @JsonProperty("cte")
    private CteDTO cte;

    // --- Campos Adicionais do CSV (22 campos mapeados) ---
    @JsonProperty("referenceNumber")
    private String referenceNumber;

    @JsonProperty("invoicesTotalVolumes")
    private Integer invoicesTotalVolumes;

    @JsonProperty("taxedWeight")
    private BigDecimal taxedWeight;

    @JsonProperty("realWeight")
    private BigDecimal realWeight;

    @JsonProperty("totalCubicVolume")
    private BigDecimal totalCubicVolume;

    @JsonProperty("subtotal")
    private BigDecimal subtotal;

    // --- Contêiner Dinâmico ("Resto") ---
    private final Map<String, Object> otherProperties = new HashMap<>();

    @JsonAnySetter
    public void add(final String key, final Object value) {
        this.otherProperties.put(key, value);
    }

    // --- Getters e Setters ---

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getServiceAt() {
        return serviceAt;
    }

    public void setServiceAt(final String serviceAt) {
        this.serviceAt = serviceAt;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final String createdAt) {
        this.createdAt = createdAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public String getModal() {
        return modal;
    }

    public void setModal(final String modal) {
        this.modal = modal;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public BigDecimal getTotalValue() {
        return totalValue;
    }

    public void setTotalValue(final BigDecimal totalValue) {
        this.totalValue = totalValue;
    }

    public BigDecimal getInvoicesValue() {
        return invoicesValue;
    }

    public void setInvoicesValue(final BigDecimal invoicesValue) {
        this.invoicesValue = invoicesValue;
    }

    public BigDecimal getInvoicesWeight() {
        return invoicesWeight;
    }

    public void setInvoicesWeight(final BigDecimal invoicesWeight) {
        this.invoicesWeight = invoicesWeight;
    }

    public Long getCorporationId() {
        return corporationId;
    }

    public void setCorporationId(final Long corporationId) {
        this.corporationId = corporationId;
    }

    public Long getDestinationCityId() {
        return destinationCityId;
    }

    public void setDestinationCityId(final Long destinationCityId) {
        this.destinationCityId = destinationCityId;
    }

    public String getDeliveryPredictionDate() {
        return deliveryPredictionDate;
    }

    public void setDeliveryPredictionDate(final String deliveryPredictionDate) {
        this.deliveryPredictionDate = deliveryPredictionDate;
    }

    // --- Getters e Setters para Campos Expandidos ---

    public PayerDTO getPayer() {
        return payer;
    }

    public void setPayer(PayerDTO payer) {
        this.payer = payer;
    }

    public SenderDTO getSender() {
        return sender;
    }

    public void setSender(SenderDTO sender) {
        this.sender = sender;
    }

    public ReceiverDTO getReceiver() {
        return receiver;
    }

    public void setReceiver(ReceiverDTO receiver) {
        this.receiver = receiver;
    }

    public CorporationDTO getCorporation() {
        return corporation;
    }

    public void setCorporation(CorporationDTO corporation) {
        this.corporation = corporation;
    }

    public List<FreightInvoiceDTO> getFreightInvoices() {
        return freightInvoices;
    }

    public void setFreightInvoices(List<FreightInvoiceDTO> freightInvoices) {
        this.freightInvoices = freightInvoices;
    }

    public CustomerPriceTableDTO getCustomerPriceTable() {
        return customerPriceTable;
    }

    public void setCustomerPriceTable(CustomerPriceTableDTO customerPriceTable) {
        this.customerPriceTable = customerPriceTable;
    }

    public FreightClassificationDTO getFreightClassification() {
        return freightClassification;
    }

    public void setFreightClassification(FreightClassificationDTO freightClassification) {
        this.freightClassification = freightClassification;
    }

    public CostCenterDTO getCostCenter() {
        return costCenter;
    }

    public void setCostCenter(CostCenterDTO costCenter) {
        this.costCenter = costCenter;
    }

    public UserDTO getUser() {
        return user;
    }

    public void setUser(UserDTO user) {
        this.user = user;
    }

    public CteDTO getCte() { return cte; }
    public void setCte(final CteDTO cte) { this.cte = cte; }

    // --- Getters e Setters para Campos Adicionais do CSV ---

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public Integer getInvoicesTotalVolumes() {
        return invoicesTotalVolumes;
    }

    public void setInvoicesTotalVolumes(Integer invoicesTotalVolumes) {
        this.invoicesTotalVolumes = invoicesTotalVolumes;
    }

    public BigDecimal getTaxedWeight() {
        return taxedWeight;
    }

    public void setTaxedWeight(BigDecimal taxedWeight) {
        this.taxedWeight = taxedWeight;
    }

    public BigDecimal getRealWeight() {
        return realWeight;
    }

    public void setRealWeight(BigDecimal realWeight) {
        this.realWeight = realWeight;
    }

    public BigDecimal getTotalCubicVolume() {
        return totalCubicVolume;
    }

    public void setTotalCubicVolume(BigDecimal totalCubicVolume) {
        this.totalCubicVolume = totalCubicVolume;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    @JsonAnyGetter
    public Map<String, Object> getOtherProperties() {
        return otherProperties;
    }
}



package br.com.extrator.modelo.graphql.fretes;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO aninhado para representar o Endereço Principal (MainAddress) de um Remetente/Destinatário.
 * Conforme documentação em docs/descobertas-endpoints/fretes.md linha 88-97.
 */
public class MainAddressDTO {
    @JsonProperty("city")
    private CityDTO city;

    public CityDTO getCity() {
        return city;
    }

    public void setCity(CityDTO city) {
        this.city = city;
    }
}



package br.com.extrator.modelo.graphql.fretes;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO aninhado para representar o Pagador (Payer) de um Frete.
 * Conforme documentação em docs/descobertas-endpoints/fretes.md linha 81-84.
 */
public class PayerDTO {
    @JsonProperty("id")
    private Long id;

    @JsonProperty("name")
    private String name;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}




package br.com.extrator.modelo.graphql.fretes;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO aninhado para representar o Destinatário (Receiver) de um Frete.
 * Conforme documentação em docs/descobertas-endpoints/fretes.md linha 101-112.
 */
public class ReceiverDTO {
    @JsonProperty("id")
    private Long id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("mainAddress")
    private MainAddressDTO mainAddress;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public MainAddressDTO getMainAddress() {
        return mainAddress;
    }

    public void setMainAddress(MainAddressDTO mainAddress) {
        this.mainAddress = mainAddress;
    }
}




package br.com.extrator.modelo.graphql.fretes;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO aninhado para representar o Remetente (Sender) de um Frete.
 * Conforme documentação em docs/descobertas-endpoints/fretes.md linha 87-98.
 */
public class SenderDTO {
    @JsonProperty("id")
    private Long id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("mainAddress")
    private MainAddressDTO mainAddress;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public MainAddressDTO getMainAddress() {
        return mainAddress;
    }

    public void setMainAddress(MainAddressDTO mainAddress) {
        this.mainAddress = mainAddress;
    }
}



package br.com.extrator.modelo.graphql.fretes;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO aninhado para representar o Estado (State) de uma Cidade.
 * Conforme documentação em docs/descobertas-endpoints/fretes.md linha 93-95.
 */
public class StateDTO {
    @JsonProperty("code")
    private String code;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}




package br.com.extrator.modelo.graphql.fretes;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO aninhado para representar o Usuário (User) de um Frete.
 * Conforme documentação em docs/descobertas-endpoints/fretes.md linha 173.
 */
public class UserDTO {
    @JsonProperty("name")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

