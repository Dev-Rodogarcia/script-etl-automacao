package br.com.extrator.api;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
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

import br.com.extrator.db.entity.PageAuditEntity;
import br.com.extrator.db.repository.PageAuditRepository;
import br.com.extrator.api.constantes.ConstantesApiGraphQL;
import br.com.extrator.api.graphql.GraphQLIntervaloHelper;
import br.com.extrator.api.graphql.GraphQLQueries;
import br.com.extrator.util.validacao.ConstantesEntidades;
import br.com.extrator.modelo.graphql.coletas.ColetaNodeDTO;
import br.com.extrator.modelo.graphql.fretes.FreteNodeDTO;
import br.com.extrator.util.configuracao.CarregadorConfig;
import br.com.extrator.util.http.GerenciadorRequisicaoHttp;
import br.com.extrator.util.formatacao.FormatadorData;

/**
 * Cliente especializado para comunicação com a API GraphQL do ESL Cloud
 * Responsável por buscar dados de Coletas através de queries GraphQL
 * com proteções contra loops infinitos e circuit breaker.
 */
public class ClienteApiGraphQL {
    private static final Logger logger = LoggerFactory.getLogger(ClienteApiGraphQL.class);
    
    // PROTEÇÕES CONTRA LOOPS INFINITOS - Replicadas do ClienteApiRest
    // PROBLEMA #7 CORRIGIDO: Valor agora obtido de CarregadorConfig
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
    private final PageAuditRepository pageAuditRepository;
    private String executionUuid;

    /**
     * Executa uma query GraphQL com paginação automática e proteções contra loops infinitos
     * 
     * @param query Query GraphQL a ser executada
     * @param nomeEntidade Nome da entidade na resposta GraphQL
     * @param variaveis Variáveis da query GraphQL
     * @param tipoClasse Classe para desserialização tipada
     * @return ResultadoExtracao indicando se a extração foi completa ou interrompida
     */
    private <T> ResultadoExtracao<T> executarQueryPaginada(final String query, final String nomeEntidade, final Map<String, Object> variaveis, final Class<T> tipoClasse) {
        final String chaveEntidade = "GraphQL-" + nomeEntidade;
        
        // CIRCUIT BREAKER - Verificar se a entidade está com circuit aberto
        if (entidadesComCircuitAberto.contains(chaveEntidade)) {
            logger.warn("⚠️ CIRCUIT BREAKER ATIVO - Entidade {} temporariamente desabilitada devido a falhas consecutivas", nomeEntidade);
            final java.util.List<T> vazio = new java.util.ArrayList<>();
            return ResultadoExtracao.completo(vazio, 0, 0);
        }
        
        logger.info("🔍 Executando query GraphQL paginada para entidade: {}", nomeEntidade);
        
        final List<T> todasEntidades = new ArrayList<>();
        String cursor = null;
        boolean hasNextPage = true;
        int paginaAtual = 1;
        int totalRegistrosProcessados = 0;
        boolean interrompido = false; // NOVO: Rastrear se foi interrompido
        
        final int limitePaginasGeral = CarregadorConfig.obterLimitePaginasApiGraphQL();
        final String nomeEntidadeFaturasGraphQL = ConstantesApiGraphQL.obterNomeEntidadeApi(ConstantesEntidades.FATURAS_GRAPHQL);
        final int limitePaginas = nomeEntidadeFaturasGraphQL.equals(nomeEntidade)
                ? CarregadorConfig.obterLimitePaginasFaturasGraphQL()
                : limitePaginasGeral;
        final boolean auditar = nomeEntidadeFaturasGraphQL.equals(nomeEntidade);
        final String runUuid = auditar ? java.util.UUID.randomUUID().toString() : null;
        final int perInt = 100;
        java.time.LocalDate janelaInicio = null;
        java.time.LocalDate janelaFim = null;
        try {
            final Object paramsObj = variaveis != null ? variaveis.get("params") : null;
            if (paramsObj instanceof final java.util.Map<?, ?> m) {
                final Object v1 = m.get("issueDate");
                final Object v2 = m.get("dueDate");
                final Object v3 = m.get("originalDueDate");
                final String dataStr = v1 != null ? v1.toString() : (v2 != null ? v2.toString() : (v3 != null ? v3.toString() : null));
                if (dataStr != null && !dataStr.isBlank()) {
                    try {
                        final java.time.LocalDate d = java.time.LocalDate.parse(dataStr);
                        janelaInicio = d;
                        janelaFim = d;
                    } catch (final RuntimeException ignored) {}
                }
            }
        } catch (final RuntimeException ignored) {}

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
                // PROBLEMA #7 CORRIGIDO: Usar valor de CarregadorConfig em vez de constante hardcoded
                final int maxRegistros = CarregadorConfig.obterMaxRegistrosGraphQL();
                if (totalRegistrosProcessados >= maxRegistros) {
                    logger.warn("🚨 PROTEÇÃO ATIVADA - Entidade {}: Limite de {} registros atingido. Interrompendo busca para evitar sobrecarga.", 
                            nomeEntidade, maxRegistros);
                    interrompido = true; // NOVO: Marcar como interrompido
                    break;
                }

                // Log de progresso a cada intervalo definido
                if (paginaAtual % INTERVALO_LOG_PROGRESSO == 0) {
                    logger.info("📊 Progresso GraphQL {}: Página {}, {} registros processados", 
                            nomeEntidade, paginaAtual, totalRegistrosProcessados);
                }

                logger.debug("Executando página {} da query GraphQL para {}", paginaAtual, nomeEntidade);
                
                final Map<String, Object> variaveisComCursor = new java.util.HashMap<>(variaveis);
                if (cursor != null) {
                    variaveisComCursor.put("after", cursor);
                }
                String resumoParams = null;
                try {
                    final Object paramsObj = variaveisComCursor.get("params");
                    if (paramsObj instanceof final java.util.Map<?, ?> m) {
                        final Object v1 = ((java.util.Map<?, ?>) m).get("serviceAt");
                        final Object v2 = ((java.util.Map<?, ?>) m).get("requestDate");
                        final String intervalo = v1 != null ? v1.toString() : (v2 != null ? v2.toString() : "");
                        final String cursorStr = cursor != null ? cursor : "<inicio>";
                        resumoParams = "after=" + cursorStr + " | intervalo=" + intervalo;
                    }
                } catch (final RuntimeException ignored) {
                }
                if (resumoParams != null) {
                    logger.info("Parâmetros da requisição: {}", resumoParams);
                }

                // Executar a query para esta página
                final PaginatedGraphQLResponse<T> resposta = executarQueryGraphQLTipado(query, nomeEntidade, variaveisComCursor, tipoClasse);
                
                // Adicionar entidades desta página ao resultado total
                todasEntidades.addAll(resposta.getEntidades());
                totalRegistrosProcessados += resposta.getEntidades().size();
                
                if (auditar && this.executionUuid != null && runUuid != null) {
                    final PageAuditEntity audit = new PageAuditEntity();
                    audit.setExecutionUuid(this.executionUuid);
                    audit.setRunUuid(runUuid);
                    audit.setTemplateId(ConstantesApiGraphQL.TEMPLATE_ID_AUDIT);
                    audit.setPage(paginaAtual);
                    audit.setPer(perInt);
                    audit.setJanelaInicio(janelaInicio);
                    audit.setJanelaFim(janelaFim);
                    audit.setReqHash(resposta.getReqHash() != null ? resposta.getReqHash() : "");
                    audit.setRespHash(resposta.getRespHash() != null ? resposta.getRespHash() : "");
                    audit.setTotalItens(resposta.getTotalItens());
                    audit.setIdKey("id");
                    Long minNum = null;
                    Long maxNum = null;
                    if (tipoClasse != null && tipoClasse.getName().endsWith("CreditCustomerBillingNodeDTO")) {
                        for (final T it : resposta.getEntidades()) {
                            try {
                                final Long idVal = ((br.com.extrator.modelo.graphql.faturas.CreditCustomerBillingNodeDTO) it).getId();
                                if (idVal != null) {
                                    minNum = (minNum == null || idVal < minNum) ? idVal : minNum;
                                    maxNum = (maxNum == null || idVal > maxNum) ? idVal : maxNum;
                                }
                            } catch (final RuntimeException ignored) {}
                        }
                    }
                    audit.setIdMinNum(minNum);
                    audit.setIdMaxNum(maxNum);
                    audit.setStatusCode(resposta.getStatusCode());
                    audit.setDuracaoMs(resposta.getDuracaoMs());
                    this.pageAuditRepository.inserir(audit);
                }
                
                // Reset do contador de falhas em caso de sucesso
                contadorFalhasConsecutivas.put(chaveEntidade, 0);
                
                // PROTEÇÃO 3: Detectar cursor repetido (loop infinito)
                // Comparar o novo cursor retornado com o cursor que foi usado na requisição atual
                // Se forem iguais E hasNextPage=true, significa que a API não avançou na paginação (loop)
                // Se hasNextPage=false, mesmo com cursor repetido, é válido (última página)
                // MELHORIA: Se a página retornou menos registros que o esperado, pode ser a última página
                final String novoCursor = resposta.getEndCursor();
                if (novoCursor != null && cursor != null && novoCursor.equals(cursor)) {
                    if (resposta.getHasNextPage()) {
                        // Verificar se a página retornou menos registros que o esperado
                        final int registrosEsperados = perInt; // first: 100 (ou outro valor configurado)
                        final int registrosRecebidos = resposta.getEntidades().size();
                        
                        if (registrosRecebidos < registrosEsperados) {
                            // Página incompleta + cursor repetido = provavelmente última página
                            logger.warn("⚠️ Entidade {}: Cursor repetido ({}) mas página incompleta ({} < {}). Tratando como última página válida.", 
                                nomeEntidade, novoCursor, registrosRecebidos, registrosEsperados);
                            // Não interromper - tratar como última página válida
                            break;
                        } else {
                            // Página completa + cursor repetido + hasNextPage=true = loop infinito
                            logger.warn("🚨 PROTEÇÃO ATIVADA - Entidade {}: Cursor repetido detectado ({}). A API retornou o mesmo cursor que foi enviado E indicou hasNextPage=true. Interrompendo busca para evitar loop infinito.", 
                                nomeEntidade, novoCursor);
                            interrompido = true;
                            break;
                        }
                    } else {
                        // Cursor repetido + hasNextPage=false = última página (comportamento válido da API)
                        logger.debug("Entidade {}: Cursor repetido ({}) mas hasNextPage=false. Tratando como última página válida.", 
                                nomeEntidade, novoCursor);
                    }
                }
                
                // PROTEÇÃO 4: Detectar página vazia com hasNextPage = true (possível bug da API)
                if (resposta.getEntidades().isEmpty() && resposta.getHasNextPage()) {
                    logger.warn("🚨 PROTEÇÃO ATIVADA - Entidade {}: Página vazia com hasNextPage=true detectada. Interrompendo busca para evitar loop infinito.", 
                            nomeEntidade);
                    interrompido = true;
                    break;
                }
                
                // Atualizar informações de paginação
                hasNextPage = resposta.getHasNextPage();
                cursor = novoCursor;
                
                logger.debug("✅ Página {} processada: {} entidades encontradas. Próxima página: {} (Total: {})", 
                            paginaAtual, resposta.getEntidades().size(), hasNextPage, totalRegistrosProcessados);
                
                paginaAtual++;
                
                // Não é mais necessário pausar entre requisições - o GerenciadorRequisicaoHttp já controla o throttling
                
            } catch (final RuntimeException e) {
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
        this.pageAuditRepository = new PageAuditRepository();
        this.executionUuid = java.util.UUID.randomUUID().toString();
    }

    public void setExecutionUuid(final String uuid) {
        this.executionUuid = uuid;
    }
    /**
     * Busca coletas via GraphQL para as últimas 24h (ontem + hoje).
     * Método de conveniência que delega para buscarColetas(dataInicio, dataFim).
     * 
     * @param dataReferencia Data de referência para buscar as coletas (LocalDate)
     * @return ResultadoExtracao indicando se a busca foi completa ou interrompida
     */
    public ResultadoExtracao<ColetaNodeDTO> buscarColetas(final LocalDate dataReferencia) {
        final LocalDate diaAnterior = dataReferencia.minusDays(1);
        return buscarColetas(diaAnterior, dataReferencia);
    }





    /**
     * Busca fretes via GraphQL para as últimas 24 horas a partir de uma data de referência.
     * Método de conveniência que delega para buscarFretes(dataInicio, dataFim).
     * 
     * @param dataReferencia Data de referência que representa o FIM do intervalo de busca.
     * @return ResultadoExtracao indicando se a busca foi completa ou interrompida
     */
    public ResultadoExtracao<FreteNodeDTO> buscarFretes(final LocalDate dataReferencia) {
        final LocalDate dataInicio = dataReferencia.minusDays(1);
        return buscarFretes(dataInicio, dataReferencia);
    }

    /**
     * Busca coletas via GraphQL para um intervalo de datas.
     * Utiliza GraphQLIntervaloHelper para iterar dia a dia (API não suporta intervalo).
     * 
     * @param dataInicio Data de início do período
     * @param dataFim Data de fim do período
     * @return ResultadoExtracao indicando se a busca foi completa ou interrompida
     */
    public ResultadoExtracao<ColetaNodeDTO> buscarColetas(final LocalDate dataInicio, final LocalDate dataFim) {
        return GraphQLIntervaloHelper.executarPorDia(
            dataInicio,
            dataFim,
            this::buscarColetasDia,
            "Coletas"
        );
    }
    
    /**
     * Busca coletas para um único dia específico.
     * Método auxiliar usado pelo GraphQLIntervaloHelper.
     * 
     * @param data Data específica para buscar coletas
     * @return ResultadoExtracao das coletas do dia
     */
    private ResultadoExtracao<ColetaNodeDTO> buscarColetasDia(final LocalDate data) {
        final String dataFormatada = formatarDataParaApiGraphQL(data);
        final Map<String, Object> variaveis = Map.of(
            "params", Map.of("requestDate", dataFormatada)
        );
        return executarQueryPaginada(GraphQLQueries.QUERY_COLETAS, 
            ConstantesApiGraphQL.obterNomeEntidadeApi(ConstantesEntidades.COLETAS), variaveis, ColetaNodeDTO.class);
    }

    /**
     * Busca fretes via GraphQL para um intervalo de datas.
     * API de fretes suporta intervalo diretamente via serviceAt.
     * 
     * @param dataInicio Data de início do período
     * @param dataFim Data de fim do período
     * @return ResultadoExtracao indicando se a busca foi completa ou interrompida
     */
    public ResultadoExtracao<FreteNodeDTO> buscarFretes(final LocalDate dataInicio, final LocalDate dataFim) {
        logger.info("🔍 Buscando fretes via GraphQL - Período: {} a {}", dataInicio, dataFim);
        
        // Usar formato "dataInicio - dataFim" no filtro serviceAt (já suportado pela API)
        final String intervaloServiceAt = formatarDataParaApiGraphQL(dataInicio) + " - " + formatarDataParaApiGraphQL(dataFim);
        
        final Map<String, Object> variaveis = Map.of(
            "params", Map.of("serviceAt", intervaloServiceAt)
        );

        return executarQueryPaginada(GraphQLQueries.QUERY_FRETES, 
            ConstantesApiGraphQL.obterNomeEntidadeApi(ConstantesEntidades.FRETES), variaveis, FreteNodeDTO.class);
    }

    /**
     * Busca usuários do sistema (Individual) via GraphQL.
     * Não utiliza filtro de data, apenas filtra por enabled: true.
     * Utiliza paginação cursor-based para extrair todos os usuários ativos.
     * 
     * @return ResultadoExtracao indicando se a busca foi completa ou interrompida
     */
    public ResultadoExtracao<br.com.extrator.modelo.graphql.usuarios.IndividualNodeDTO> buscarUsuariosSistema() {
        try {
            final Map<String, Object> variaveis = new HashMap<>();
            variaveis.put("params", Map.of("enabled", true));
            
            logger.info("Buscando Usuários do Sistema via GraphQL (enabled: true)");
            return executarQueryPaginada(
                GraphQLQueries.QUERY_USUARIOS_SISTEMA, 
                ConstantesApiGraphQL.obterNomeEntidadeApi(ConstantesEntidades.USUARIOS_SISTEMA), 
                variaveis, 
                br.com.extrator.modelo.graphql.usuarios.IndividualNodeDTO.class
            );
        } catch (final RuntimeException e) {
            logger.warn("Falha ao buscar Usuários do Sistema: {}", e.getMessage());
            final List<br.com.extrator.modelo.graphql.usuarios.IndividualNodeDTO> vazio = new ArrayList<>();
            return ResultadoExtracao.completo(vazio, 0, 0);
        }
    }

    /**
     * Busca NFSe diretamente via GraphQL para enriquecer fretes com metadados.
     * Utiliza paginação e traz campos diretos e o XML bruto.
     */
    public ResultadoExtracao<br.com.extrator.modelo.graphql.fretes.nfse.NfseNodeDTO> buscarNfseDireta(final LocalDate dataReferencia) {
        try {
            final LocalDate dataInicio = dataReferencia.minusDays(1);
            final String intervaloIssuedAt = dataInicio.format(FormatadorData.ISO_DATE) + " - " + dataReferencia.format(FormatadorData.ISO_DATE);
            final Map<String, Object> variaveis = Map.of(
                "params", Map.of("issuedAt", intervaloIssuedAt)
            );
            logger.info("Buscando NFSe via GraphQL - Período: {}", intervaloIssuedAt);
            return executarQueryPaginada(GraphQLQueries.QUERY_NFSE, 
                ConstantesApiGraphQL.obterNomeEntidadeApi("nfse"), variaveis, br.com.extrator.modelo.graphql.fretes.nfse.NfseNodeDTO.class);
        } catch (final RuntimeException e) {
            logger.warn("Falha ao buscar NFSe direta: {}", e.getMessage());
            final List<br.com.extrator.modelo.graphql.fretes.nfse.NfseNodeDTO> vazio = new ArrayList<>();
            return ResultadoExtracao.completo(vazio, 0, 0);
        }
    }

    /**
     * Busca capa de faturas via GraphQL para a data de referência.
     * Utiliza janela configurável para buscar dias anteriores.
     * 
     * @param dataReferencia Data de referência (normalmente hoje)
     * @return ResultadoExtracao das faturas encontradas
     */
    public ResultadoExtracao<br.com.extrator.modelo.graphql.faturas.CreditCustomerBillingNodeDTO> buscarCapaFaturas(final LocalDate dataReferencia) {
        final int diasJanela = CarregadorConfig.obterDiasJanelaFaturasGraphQL();
        final LocalDate dataInicio = dataReferencia.minusDays(Math.max(0, diasJanela - 1));
        return buscarCapaFaturas(dataInicio, dataReferencia);
    }

    /**
     * Busca capa de faturas via GraphQL para um intervalo de datas específico.
     * Utiliza GraphQLIntervaloHelper para iterar dia a dia (API não suporta intervalo).
     * 
     * @param dataInicio Data de início do período
     * @param dataFim Data de fim do período
     * @return ResultadoExtracao indicando se a busca foi completa ou interrompida
     */
    public ResultadoExtracao<br.com.extrator.modelo.graphql.faturas.CreditCustomerBillingNodeDTO> buscarCapaFaturas(final LocalDate dataInicio, final LocalDate dataFim) {
        return GraphQLIntervaloHelper.executarPorDia(
            dataInicio,
            dataFim,
            this::buscarCapaFaturasDia,
            "Capa Faturas"
        );
    }
    
    /**
     * Busca capa de faturas para um único dia específico.
     * Método auxiliar usado pelo GraphQLIntervaloHelper.
     * 
     * @param data Data específica para buscar faturas
     * @return ResultadoExtracao das faturas do dia
     */
    private ResultadoExtracao<br.com.extrator.modelo.graphql.faturas.CreditCustomerBillingNodeDTO> buscarCapaFaturasDia(final LocalDate data) {
        final List<String> campos = listarCamposInputCreditCustomerBilling();
        String campoFiltro = "dueDate";
        if (campos != null && !campos.isEmpty()) {
            if (campos.contains("dueDate")) {
                campoFiltro = "dueDate";
            } else if (campos.contains("originalDueDate")) {
                campoFiltro = "originalDueDate";
            } else if (campos.contains("issueDate")) {
                campoFiltro = "issueDate";
            }
        }
        
        final String dataStr = data.format(FormatadorData.ISO_DATE);
        final Map<String, Object> params = new HashMap<>();
        params.put(campoFiltro, dataStr);
        
        final String corpId = CarregadorConfig.obterCorporationId();
        if (corpId != null && !corpId.isBlank()) {
            if (campos != null && campos.contains("corporationId")) {
                params.put("corporationId", corpId);
            } else if (campos != null && campos.contains("corporation")) {
                params.put("corporation", Map.of("id", corpId));
            }
        }
        
        final Map<String, Object> variaveis = Map.of("params", params);
        return executarQueryPaginada(GraphQLQueries.QUERY_FATURAS, 
            ConstantesApiGraphQL.obterNomeEntidadeApi(ConstantesEntidades.FATURAS_GRAPHQL), variaveis, 
            br.com.extrator.modelo.graphql.faturas.CreditCustomerBillingNodeDTO.class);
    }

    /**
     * Lista os campos disponíveis no tipo CreditCustomerBillingInput via introspection.
     * Usado para determinar qual campo de filtro usar (dueDate, issueDate, etc).
     * 
     * @return Lista de nomes de campos disponíveis
     */
    private List<String> listarCamposInputCreditCustomerBilling() {
        try {
            final ObjectNode corpoJson = mapeadorJson.createObjectNode();
            corpoJson.put("query", GraphQLQueries.INTROSPECTION_CREDIT_CUSTOMER_BILLING);
            final String corpoRequisicao = mapeadorJson.writeValueAsString(corpoJson);
            final HttpRequest requisicao = HttpRequest.newBuilder()
                    .uri(URI.create(urlBase + endpointGraphQL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.ofString(corpoRequisicao))
                    .timeout(this.timeoutRequisicao)
                    .build();
            final HttpResponse<String> resposta = gerenciadorRequisicao.executarRequisicao(this.clienteHttp, requisicao, "GraphQL-Introspection");
            final JsonNode respostaJson = mapeadorJson.readTree(resposta.body());
            final List<String> campos = new ArrayList<>();
            final JsonNode fields = respostaJson.path("data").path("__type").path("inputFields");
            if (fields.isArray()) {
                for (final JsonNode f : fields) {
                    final String nome = f.path("name").asText();
                    if (nome != null && !nome.isBlank()) {
                        campos.add(nome);
                    }
                }
            }
            return campos;
        } catch (final java.io.IOException e) {
            return java.util.Collections.emptyList();
        }
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
    private <T> PaginatedGraphQLResponse<T> executarQueryGraphQLTipado(final String query, final String nomeEntidade,
            final Map<String, Object> variaveis, final Class<T> tipoClasse) {
        logger.debug("Executando query GraphQL tipada para {} - URL: {}{}, Variáveis: {}", 
                    nomeEntidade, urlBase, endpointGraphQL, variaveis);
        final List<T> entidades = new ArrayList<>();
        boolean hasNextPage = false;
        String endCursor = null;
        int statusCode;
        int duracaoMs;
        String reqHash;
        String respHash;

        // Validação básica de configuração
        if (urlBase == null || urlBase.isBlank() || token == null || token.isBlank()) {
            logger.error("Configurações inválidas para chamada GraphQL (urlBase/token)");
            return new PaginatedGraphQLResponse<>(entidades, false, null, 0, 0, "", "", entidades.size());
        }

        try {
            final long tempoInicio = System.currentTimeMillis();
            // Construir o corpo da requisição GraphQL usando ObjectMapper
            final ObjectNode corpoJson = mapeadorJson.createObjectNode();
            corpoJson.put("query", query);
            if (variaveis != null && !variaveis.isEmpty()) {
                corpoJson.set("variables", mapeadorJson.valueToTree(variaveis));
            }
            final String corpoRequisicao = mapeadorJson.writeValueAsString(corpoJson);
            try {
                final byte[] d = java.security.MessageDigest.getInstance("SHA-256").digest(corpoRequisicao.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                final StringBuilder sb = new StringBuilder(d.length * 2);
                for (final byte b : d) sb.append(String.format("%02x", b));
                reqHash = sb.toString();
            } catch (final java.security.NoSuchAlgorithmException ex) {
                reqHash = "";
            }

            // Construir a requisição HTTP
            final HttpRequest requisicao = HttpRequest.newBuilder()
                    .uri(URI.create(urlBase + endpointGraphQL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.ofString(corpoRequisicao))
                    .timeout(this.timeoutRequisicao)
                    .build();

            // Executar a requisição usando o gerenciador central
            final HttpResponse<String> resposta = gerenciadorRequisicao.executarRequisicao(this.clienteHttp, requisicao, "GraphQL-" + nomeEntidade);
            statusCode = resposta != null ? resposta.statusCode() : 0;
            duracaoMs = (int) (System.currentTimeMillis() - tempoInicio);
            try {
                final byte[] d = java.security.MessageDigest.getInstance("SHA-256").digest((resposta != null ? resposta.body() : "").getBytes(java.nio.charset.StandardCharsets.UTF_8));
                final StringBuilder sb = new StringBuilder(d.length * 2);
                for (final byte b : d) sb.append(String.format("%02x", b));
                respHash = sb.toString();
            } catch (final java.security.NoSuchAlgorithmException ex) {
                respHash = "";
            }

            // Parsear a resposta JSON
            if (resposta == null || resposta.body() == null) {
                logger.warn("Resposta GraphQL nula para {}", nomeEntidade);
                return new PaginatedGraphQLResponse<>(entidades, false, null, statusCode, duracaoMs, reqHash, respHash, entidades.size());
            }
            final JsonNode respostaJson = mapeadorJson.readTree(resposta.body());

            // Verificar se há erros na resposta GraphQL
            if (respostaJson.has("errors")) {
                final JsonNode erros = respostaJson.get("errors");
                logger.error("Erros na query GraphQL para {}: {}", nomeEntidade, erros.toString());
                return new PaginatedGraphQLResponse<>(entidades, false, null, statusCode, duracaoMs, reqHash, respHash, entidades.size());
            }

            // Extrair os dados da resposta
            if (!respostaJson.has("data")) {
                logger.warn("Resposta GraphQL sem campo 'data' para {}", nomeEntidade);
                return new PaginatedGraphQLResponse<>(entidades, false, null, statusCode, duracaoMs, reqHash, respHash, entidades.size());
            }

            final JsonNode dados = respostaJson.get("data");
            if (!dados.has(nomeEntidade)) {
                logger.warn("Campo '{}' não encontrado na resposta GraphQL. Campos disponíveis: {}",
                        nomeEntidade, dados.fieldNames());
                return new PaginatedGraphQLResponse<>(entidades, false, null, statusCode, duracaoMs, reqHash, respHash, entidades.size());
            }

            final JsonNode dadosEntidade = dados.get(nomeEntidade);

            // Verificar se a resposta segue o padrão paginado com edges/node
            if (dadosEntidade.has("edges")) {
                logger.debug("Processando resposta paginada com edges/node para {}", nomeEntidade);
                final JsonNode edges = dadosEntidade.get("edges");

                if (edges.isArray()) {
                    for (final JsonNode edge : edges) {
                        if (edge.has("node")) {
                            final JsonNode node = edge.get("node");
                            try {
                                // Deserializa diretamente para a classe tipada usando Jackson
                                final T entidade = mapeadorJson.treeToValue(node, tipoClasse);
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
                    final JsonNode pageInfo = dadosEntidade.get("pageInfo");
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
                    for (final JsonNode item : dadosEntidade) {
                        try {
                            // Deserializa diretamente para a classe tipada usando Jackson
                            final T entidade = mapeadorJson.treeToValue(item, tipoClasse);
                            entidades.add(entidade);
                        } catch (JsonProcessingException | IllegalArgumentException e) {
                            logger.warn("Erro ao deserializar item de {} para {}: {}", 
                                      nomeEntidade, tipoClasse.getSimpleName(), e.getMessage());
                        }
                    }
                }
            }

            logger.debug("Query GraphQL tipada concluída para {}. Total encontrado: {}", nomeEntidade, entidades.size());

        } catch (final JsonProcessingException e) {
            logger.error("Erro de processamento JSON durante execução da query GraphQL para {}: {}", nomeEntidade, e.getMessage(), e);
            return new PaginatedGraphQLResponse<>(entidades, false, null, 0, 0, "", "", entidades.size());
        } catch (final RuntimeException e) {
            logger.error("Erro durante execução da query GraphQL para {}: {}", nomeEntidade, e.getMessage(), e);
            return new PaginatedGraphQLResponse<>(entidades, false, null, 0, 0, "", "", entidades.size());
        }

        return new PaginatedGraphQLResponse<>(entidades, hasNextPage, endCursor, statusCode, duracaoMs, reqHash, respHash, entidades.size());
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
            final String queryTeste = "{ __schema { queryType { name } } }";

            // Construir o corpo da requisição GraphQL usando ObjectMapper
            final ObjectNode corpoJson = mapeadorJson.createObjectNode();
            corpoJson.put("query", queryTeste);
            final String corpoRequisicao = mapeadorJson.writeValueAsString(corpoJson);

            final String url = urlBase + endpointGraphQL;
            final HttpRequest requisicao = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(corpoRequisicao))
                    .build();

            final HttpResponse<String> resposta = clienteHttp.send(requisicao, HttpResponse.BodyHandlers.ofString());

            if (resposta.statusCode() == 200) {
                final JsonNode respostaJson = mapeadorJson.readTree(resposta.body());
                final boolean sucesso = !respostaJson.has("errors");

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
    private String formatarDataParaApiGraphQL(final LocalDate data) {
        return data.format(FormatadorData.ISO_DATE);
    }

    /**
     * Incrementa o contador de falhas consecutivas e ativa o circuit breaker se necessário.
     * 
     * @param chaveEntidade Chave identificadora da entidade GraphQL
     * @param nomeEntidade Nome amigável da entidade para logs
     */
    private void incrementarContadorFalhas(final String chaveEntidade, final String nomeEntidade) {
        final int falhas = contadorFalhasConsecutivas.getOrDefault(chaveEntidade, 0) + 1;
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
