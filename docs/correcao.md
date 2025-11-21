Sim, analisei todas as classes enviadas.

**Situação Atual:**

1.  **DTOs (`FreteNodeDTO`, `CteDTO`, etc.):** ✅ **Corretos.** Você já incluiu as classes aninhadas (`CteDTO`, `FreightInvoiceDTO`) no código enviado. Eles estão prontos para receber os dados.
2.  **Entidade e Repositório:** ✅ **Corretos.**
3.  **Mapper (`FreteMapper`):** ⚠️ **Incompleto.** Ele não estava mapeando os objetos `cte` e `freightInvoices` para os campos planos da entidade (`chaveCte`, `numeroNotaFiscal`).
4.  **Cliente API (`ClienteApiGraphQL`):** ❌ **Incorreto.** O método `buscarFretes` ainda estava usando a query antiga (`BuscarFretesExpandidaV3`) e a lógica de buscar dia-a-dia, ignorando a capacidade de filtro por intervalo da API e, crucialmente, **não solicitando os campos de Chave e NF na query**.

Abaixo estão **apenas as duas classes que precisam de alteração**. Substitua o conteúdo dos arquivos correspondentes no seu projeto.

-----

### 1\. `ClienteApiGraphQL.java` (Corrigido)

**Alterações:**

  * Substituída a query pela **Versão 5.1** (que inclui `cte { key }` e `freightInvoices`).
  * Alterada a lógica de busca para usar o filtro de intervalo (`YYYY-MM-DD - YYYY-MM-DD`) em vez de fazer duas requisições separadas.

<!-- end list -->

```java
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

public class ClienteApiGraphQL {
    private static final Logger logger = LoggerFactory.getLogger(ClienteApiGraphQL.class);
    
    private static final int MAX_REGISTROS_POR_EXECUCAO = 50000;
    private static final int INTERVALO_LOG_PROGRESSO = 50;
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

    public ClienteApiGraphQL() {
        this.urlBase = CarregadorConfig.obterUrlBaseApi();
        this.endpointGraphQL = CarregadorConfig.obterEndpointGraphQL();
        this.token = CarregadorConfig.obterTokenApiGraphQL();
        this.timeoutRequisicao = CarregadorConfig.obterTimeoutApiRest();
        this.clienteHttp = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.mapeadorJson = new ObjectMapper();
        this.gerenciadorRequisicao = new GerenciadorRequisicaoHttp();
    }

    /**
     * Busca fretes via GraphQL usando a Query V5.1 (Full Schema).
     * Utiliza intervalo de datas para trazer dados de ontem e hoje em uma única execução paginada.
     */
    public ResultadoExtracao<FreteNodeDTO> buscarFretes(LocalDate dataReferencia) {
        // Query Validada V5.1 - Traz Chave CT-e e Notas Fiscais
        String query = """
            query BuscarFretesProducaoV5_1($params: FreightInput!, $after: String) {
              freight(params: $params, after: $after, first: 100) {
                edges {
                  node {
                    # --- 1. Identificadores ---
                    id
                    referenceNumber
                    serviceAt
                    createdAt
                    
                    # --- 2. O CAMPO DE JOIN (Chave CT-e) ---
                    cte {
                      key     # <--- AQUI ESTÁ A CHAVE DE 44 DÍGITOS
                      number  # Número do CT-e
                      series  # Série
                    }

                    # --- 3. Valores ---
                    total           # Valor Total Serviço
                    subtotal        # Valor Frete Peso
                    invoicesValue   # Valor da Carga
                    
                    # --- 4. Métricas ---
                    taxedWeight
                    realWeight
                    totalCubicVolume
                    invoicesTotalVolumes
                    invoicesWeight

                    # --- 5. Notas Fiscais (Lista Aninhada) ---
                    freightInvoices {
                      invoice {
                        number
                        series
                        key
                        value
                        weight
                      }
                    }

                    # --- 6. Atores e Endereços ---
                    sender {
                      id
                      name
                      mainAddress { city { name state { code } } }
                    }
                    receiver {
                      id
                      name
                      mainAddress { city { name state { code } } }
                    }
                    payer { 
                        id
                        name 
                    }
                    
                    # --- 7. Classificações ---
                    modal
                    status
                    type
                    deliveryPredictionDate
                    
                    corporation { name }
                    customerPriceTable { name }
                    freightClassification { name }
                    costCenter { name }
                    user { name }
                    
                    destinationCityId
                    corporationId
                  }
                }
                pageInfo {
                  hasNextPage
                  endCursor
                }
              }
            }""";

        // 1. Calcular o intervalo de datas (Ontem até Hoje)
        LocalDate dataInicio = dataReferencia.minusDays(1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        // A API aceita "YYYY-MM-DD - YYYY-MM-DD" para filtrar um range
        String intervaloServiceAt = dataInicio.format(formatter) + " - " + dataReferencia.format(formatter);

        // 2. Construir variáveis
        Map<String, Object> variaveis = Map.of(
            "params", Map.of("serviceAt", intervaloServiceAt)
        );

        logger.info("🔍 Buscando fretes (Schema Completo V5.1) - Período: {}", intervaloServiceAt);
        
        // Executa a paginação
        ResultadoExtracao<FreteNodeDTO> resultado = executarQueryPaginada(query, "freight", variaveis, FreteNodeDTO.class);
        
        if (resultado.getDados().isEmpty()) {
            logger.warn("❌ Sem fretes encontrados para o período {}", intervaloServiceAt);
        } else {
            logger.info("✅ Encontrados {} fretes para o período {}", resultado.getDados().size(), intervaloServiceAt);
        }
        
        return resultado;
    }

    // --- MÉTODOS AUXILIARES MANTIDOS DA SUA CLASSE ORIGINAL ---
    // (Mantive a lógica de paginação, circuit breaker e busca de coletas inalterada)

    public ResultadoExtracao<ColetaNodeDTO> buscarColetas(LocalDate dataReferencia) {
        // Query Coletas mantida conforme seu código original, pois já funciona
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
                                customer { id name }
                                pickAddress { line1 city { name state { code } } }
                                user { id name }
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
                        pageInfo { hasNextPage endCursor }
                    }
                }""";

        LocalDate diaAnterior = dataReferencia.minusDays(1);
        List<ColetaNodeDTO> todasColetas = new ArrayList<>();
        int totalPaginas = 0;
        boolean ambasCompletas = true;

        // Busca Dia 1
        logger.info("🔍 Coletas - Dia 1/2: {}", diaAnterior);
        String dataAnteriorFormatada = formatarDataParaApiGraphQL(diaAnterior);
        Map<String, Object> variaveisDiaAnterior = Map.of("params", Map.of("requestDate", dataAnteriorFormatada));
        ResultadoExtracao<ColetaNodeDTO> resultadoDiaAnterior = executarQueryPaginada(query, "pick", variaveisDiaAnterior, ColetaNodeDTO.class);
        todasColetas.addAll(resultadoDiaAnterior.getDados());
        totalPaginas += resultadoDiaAnterior.getPaginasProcessadas();
        if (!resultadoDiaAnterior.isCompleto()) ambasCompletas = false;

        // Busca Dia 2
        logger.info("🔍 Coletas - Dia 2/2: {}", dataReferencia);
        String dataAtualFormatada = formatarDataParaApiGraphQL(dataReferencia);
        Map<String, Object> variaveisDataAtual = Map.of("params", Map.of("requestDate", dataAtualFormatada));
        ResultadoExtracao<ColetaNodeDTO> resultadoDataAtual = executarQueryPaginada(query, "pick", variaveisDataAtual, ColetaNodeDTO.class);
        todasColetas.addAll(resultadoDataAtual.getDados());
        totalPaginas += resultadoDataAtual.getPaginasProcessadas();
        if (!resultadoDataAtual.isCompleto()) ambasCompletas = false;

        logger.info("✅ Total: {} coletas", todasColetas.size());

        if (ambasCompletas) {
            return ResultadoExtracao.completo(todasColetas, totalPaginas, todasColetas.size());
        } else {
            return ResultadoExtracao.incompleto(todasColetas, ResultadoExtracao.MotivoInterrupcao.LIMITE_PAGINAS, totalPaginas, todasColetas.size());
        }
    }

    private <T> ResultadoExtracao<T> executarQueryPaginada(String query, String nomeEntidade, Map<String, Object> variaveis, Class<T> tipoClasse) {
        String chaveEntidade = "GraphQL-" + nomeEntidade;
        
        if (entidadesComCircuitAberto.contains(chaveEntidade)) {
            logger.warn("⚠️ CIRCUIT BREAKER ATIVO - Entidade {} temporariamente desabilitada", nomeEntidade);
            return ResultadoExtracao.completo(new ArrayList<>(), 0, 0);
        }
        
        List<T> todasEntidades = new ArrayList<>();
        String cursor = null;
        boolean hasNextPage = true;
        int paginaAtual = 1;
        int totalRegistrosProcessados = 0;
        boolean interrompido = false;
        
        final int limitePaginas = CarregadorConfig.obterLimitePaginasApiGraphQL();

        while (hasNextPage) {
            try {
                if (paginaAtual > limitePaginas) {
                    logger.warn("🚨 Limite de {} páginas atingido para {}.", limitePaginas, nomeEntidade);
                    interrompido = true;
                    break;
                }
                if (totalRegistrosProcessados >= MAX_REGISTROS_POR_EXECUCAO) {
                    logger.warn("🚨 Limite de {} registros atingido para {}.", MAX_REGISTROS_POR_EXECUCAO, nomeEntidade);
                    interrompido = true;
                    break;
                }
                if (paginaAtual % INTERVALO_LOG_PROGRESSO == 0) {
                    logger.info("📊 Progresso {}: Pág {}, {} registros", nomeEntidade, paginaAtual, totalRegistrosProcessados);
                }

                Map<String, Object> variaveisComCursor = new java.util.HashMap<>(variaveis);
                if (cursor != null) {
                    variaveisComCursor.put("after", cursor);
                }

                PaginatedGraphQLResponse<T> resposta = executarQueryGraphQLTipado(query, nomeEntidade, variaveisComCursor, tipoClasse);
                
                todasEntidades.addAll(resposta.getEntidades());
                totalRegistrosProcessados += resposta.getEntidades().size();
                contadorFalhasConsecutivas.put(chaveEntidade, 0);
                
                hasNextPage = resposta.getHasNextPage();
                cursor = resposta.getEndCursor();
                paginaAtual++;
                
            } catch (Exception e) {
                logger.error("💥 Erro na query GraphQL {} pág {}: {}", nomeEntidade, paginaAtual, e.getMessage());
                incrementarContadorFalhas(chaveEntidade, nomeEntidade);
                break;
            }
        }

        if (interrompido) {
            return ResultadoExtracao.incompleto(todasEntidades, ResultadoExtracao.MotivoInterrupcao.LIMITE_PAGINAS, paginaAtual - 1, totalRegistrosProcessados);
        } else {
            return ResultadoExtracao.completo(todasEntidades, paginaAtual - 1, totalRegistrosProcessados);
        }
    }

    private <T> PaginatedGraphQLResponse<T> executarQueryGraphQLTipado(String query, String nomeEntidade, Map<String, Object> variaveis, Class<T> tipoClasse) {
        List<T> entidades = new ArrayList<>();
        boolean hasNextPage = false;
        String endCursor = null;

        try {
            ObjectNode corpoJson = mapeadorJson.createObjectNode();
            corpoJson.put("query", query);
            if (variaveis != null && !variaveis.isEmpty()) {
                corpoJson.set("variables", mapeadorJson.valueToTree(variaveis));
            }
            final String corpoRequisicao = mapeadorJson.writeValueAsString(corpoJson);

            HttpRequest requisicao = HttpRequest.newBuilder()
                    .uri(URI.create(urlBase + endpointGraphQL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.ofString(corpoRequisicao))
                    .timeout(this.timeoutRequisicao)
                    .build();

            HttpResponse<String> resposta = gerenciadorRequisicao.executarRequisicao(this.clienteHttp, requisicao, "GraphQL-" + nomeEntidade);
            JsonNode respostaJson = mapeadorJson.readTree(resposta.body());

            if (respostaJson.has("errors")) {
                logger.error("Erros GraphQL {}: {}", nomeEntidade, respostaJson.get("errors"));
                return new PaginatedGraphQLResponse<>(entidades, false, null);
            }

            if (respostaJson.has("data") && respostaJson.get("data").has(nomeEntidade)) {
                JsonNode dadosEntidade = respostaJson.get("data").get(nomeEntidade);
                
                if (dadosEntidade.has("edges") && dadosEntidade.get("edges").isArray()) {
                    for (JsonNode edge : dadosEntidade.get("edges")) {
                        if (edge.has("node")) {
                            try {
                                T entidade = mapeadorJson.treeToValue(edge.get("node"), tipoClasse);
                                entidades.add(entidade);
                            } catch (Exception e) {
                                logger.warn("Erro deserialização {}: {}", nomeEntidade, e.getMessage());
                            }
                        }
                    }
                }
                
                if (dadosEntidade.has("pageInfo")) {
                    JsonNode pageInfo = dadosEntidade.get("pageInfo");
                    if (pageInfo.has("hasNextPage")) hasNextPage = pageInfo.get("hasNextPage").asBoolean();
                    if (pageInfo.has("endCursor") && !pageInfo.get("endCursor").isNull()) endCursor = pageInfo.get("endCursor").asText();
                }
            }
        } catch (Exception e) {
            logger.error("Erro fatal query {}: {}", nomeEntidade, e.getMessage());
        }

        return new PaginatedGraphQLResponse<>(entidades, hasNextPage, endCursor);
    }

    public boolean validarAcessoApi() {
        try {
            String queryTeste = "{ __schema { queryType { name } } }";
            ObjectNode corpoJson = mapeadorJson.createObjectNode();
            corpoJson.put("query", queryTeste);
            HttpRequest requisicao = HttpRequest.newBuilder()
                    .uri(URI.create(urlBase + endpointGraphQL))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapeadorJson.writeValueAsString(corpoJson)))
                    .build();
            HttpResponse<String> resposta = clienteHttp.send(requisicao, HttpResponse.BodyHandlers.ofString());
            return resposta.statusCode() == 200 && !mapeadorJson.readTree(resposta.body()).has("errors");
        } catch (Exception e) {
            logger.error("Erro validação API: {}", e.getMessage());
            return false;
        }
    }

    private String formatarDataParaApiGraphQL(LocalDate data) {
        return data.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    private void incrementarContadorFalhas(String chaveEntidade, String nomeEntidade) {
        int falhas = contadorFalhasConsecutivas.getOrDefault(chaveEntidade, 0) + 1;
        contadorFalhasConsecutivas.put(chaveEntidade, falhas);
        if (falhas >= MAX_FALHAS_CONSECUTIVAS) {
            entidadesComCircuitAberto.add(chaveEntidade);
            logger.error("🚨 CIRCUIT BREAKER {} ({}) ativado.", chaveEntidade, nomeEntidade);
        }
    }
}
```

-----

### 2\. `FreteMapper.java` (Corrigido)

**Alterações:**

  * Adicionada a lógica para extrair `dto.getCte().getKey()` para o campo `chaveCte` da entidade.
  * Adicionada a lógica para percorrer a lista `dto.getFreightInvoices()`, extrair os números e concatenar na String `numeroNotaFiscal` (ex: "123, 456").

<!-- end list -->

```java
package br.com.extrator.modelo.graphql.fretes;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import br.com.extrator.db.entity.FreteEntity;

public class FreteMapper {

    private static final Logger logger = LoggerFactory.getLogger(FreteMapper.class);
    private final ObjectMapper objectMapper;

    public FreteMapper() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Converte o DTO de Frete em uma Entidade.
     * ATUALIZADO: Agora mapeia corretamente o objeto aninhado CTE e a lista de NFs.
     */
    public FreteEntity toEntity(final FreteNodeDTO dto) {
        if (dto == null) {
            return null;
        }

        final FreteEntity entity = new FreteEntity();

        // --- 1. Mapeamento dos campos essenciais e diretos ---
        entity.setId(dto.getId());
        entity.setStatus(dto.getStatus());
        entity.setModal(dto.getModal());
        entity.setTipoFrete(dto.getType());
        entity.setValorTotal(dto.getTotalValue());
        entity.setSubtotal(dto.getSubtotal()); // Valor Frete Peso
        entity.setValorNotas(dto.getInvoicesValue());
        entity.setPesoNotas(dto.getInvoicesWeight());
        entity.setIdCorporacao(dto.getCorporationId());
        entity.setIdCidadeDestino(dto.getDestinationCityId());
        
        // Campos físicos e referência
        entity.setReferenceNumber(dto.getReferenceNumber());
        entity.setInvoicesTotalVolumes(dto.getInvoicesTotalVolumes());
        entity.setTaxedWeight(dto.getTaxedWeight());
        entity.setRealWeight(dto.getRealWeight());
        entity.setTotalCubicVolume(dto.getTotalCubicVolume());

        // --- 2. Mapeamento CRÍTICO: Dados do CT-e (Join) ---
        // Se o frete foi emitido, este objeto virá preenchido na query V5.1
        if (dto.getCte() != null) {
            entity.setChaveCte(dto.getCte().getKey());     // Chave de 44 dígitos
            entity.setNumeroCte(dto.getCte().getNumber()); // Número Sequencial
            entity.setSerieCte(dto.getCte().getSeries());  // Série
        }

        // --- 3. Mapeamento CRÍTICO: Lista de Notas Fiscais ---
        // Transforma a lista de objetos FreightInvoice em uma string única para o banco
        if (dto.getFreightInvoices() != null && !dto.getFreightInvoices().isEmpty()) {
            try {
                String notasConcatenadas = dto.getFreightInvoices().stream()
                    .filter(fi -> fi != null && fi.getInvoice() != null && fi.getInvoice().getNumber() != null)
                    .map(fi -> fi.getInvoice().getNumber())
                    .distinct() // Remove duplicatas se houver
                    .collect(Collectors.joining(", "));
                
                if (!notasConcatenadas.isEmpty()) {
                    entity.setNumeroNotaFiscal(notasConcatenadas);
                }
            } catch (Exception e) {
                logger.warn("Erro ao processar NFs para frete ID {}: {}", dto.getId(), e.getMessage());
            }
        }

        // --- 4. Mapeamento dos Atores (Pagador, Remetente, Destinatário) ---
        if (dto.getPayer() != null) {
            entity.setPagadorId(dto.getPayer().getId());
            entity.setPagadorNome(dto.getPayer().getName());
        }

        if (dto.getSender() != null) {
            entity.setRemetenteId(dto.getSender().getId());
            entity.setRemetenteNome(dto.getSender().getName());
            if (dto.getSender().getMainAddress() != null && dto.getSender().getMainAddress().getCity() != null) {
                entity.setOrigemCidade(dto.getSender().getMainAddress().getCity().getName());
                if (dto.getSender().getMainAddress().getCity().getState() != null) {
                    entity.setOrigemUf(dto.getSender().getMainAddress().getCity().getState().getCode());
                }
            }
        }

        if (dto.getReceiver() != null) {
            entity.setDestinatarioId(dto.getReceiver().getId());
            entity.setDestinatarioNome(dto.getReceiver().getName());
            if (dto.getReceiver().getMainAddress() != null && dto.getReceiver().getMainAddress().getCity() != null) {
                entity.setDestinoCidade(dto.getReceiver().getMainAddress().getCity().getName());
                if (dto.getReceiver().getMainAddress().getCity().getState() != null) {
                    entity.setDestinoUf(dto.getReceiver().getMainAddress().getCity().getState().getCode());
                }
            }
        }

        // --- 5. Classificadores e Objetos Relacionais ---
        if (dto.getCorporation() != null) entity.setFilialNome(dto.getCorporation().getName());
        if (dto.getCustomerPriceTable() != null) entity.setTabelaPrecoNome(dto.getCustomerPriceTable().getName());
        if (dto.getFreightClassification() != null) entity.setClassificacaoNome(dto.getFreightClassification().getName());
        if (dto.getCostCenter() != null) entity.setCentroCustoNome(dto.getCostCenter().getName());
        if (dto.getUser() != null) entity.setUsuarioNome(dto.getUser().getName());

        // --- 6. Conversão Segura de Datas ---
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
        } catch (DateTimeParseException e) {
            logger.error("❌ Erro parse data Frete ID {}: {}", dto.getId(), e.getMessage());
        }

        // --- 7. Metadados Completos (JSON Bruto para Resiliência) ---
        try {
            entity.setMetadata(objectMapper.writeValueAsString(dto));
        } catch (JsonProcessingException e) {
            logger.error("❌ Falha ao serializar metadados para frete ID {}: {}", dto.getId(), e.getMessage());
            entity.setMetadata(String.format("{\"error\":\"Serialization failed\",\"id\":%d}", dto.getId()));
        }

        return entity;
    }
}
```