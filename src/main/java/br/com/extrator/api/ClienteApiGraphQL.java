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
import java.util.LinkedHashMap;
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
import br.com.extrator.modelo.graphql.faturas.CreditCustomerBillingNodeDTO;
import br.com.extrator.modelo.graphql.bancos.BankAccountNodeDTO;
import br.com.extrator.util.configuracao.CarregadorConfig;
import br.com.extrator.util.http.GerenciadorRequisicaoHttp;
import br.com.extrator.util.formatacao.FormatadorData;

/**
 * Cliente especializado para comunicaÃ§Ã£o com a API GraphQL do ESL Cloud
 * ResponsÃ¡vel por buscar dados de Coletas atravÃ©s de queries GraphQL
 * com proteÃ§Ãµes contra loops infinitos e circuit breaker.
 */
public class ClienteApiGraphQL {
    private static final Logger logger = LoggerFactory.getLogger(ClienteApiGraphQL.class);
    
    // PROTEÃ‡Ã•ES CONTRA LOOPS INFINITOS - Replicadas do ClienteApiRest
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
    private volatile Set<String> camposPickInputCache;

    /**
     * Executa uma query GraphQL com paginaÃ§Ã£o automÃ¡tica e proteÃ§Ãµes contra loops infinitos
     * 
     * @param query Query GraphQL a ser executada
     * @param nomeEntidade Nome da entidade na resposta GraphQL
     * @param variaveis VariÃ¡veis da query GraphQL
     * @param tipoClasse Classe para desserializaÃ§Ã£o tipada
     * @return ResultadoExtracao indicando se a extraÃ§Ã£o foi completa ou interrompida
     */
    private <T> ResultadoExtracao<T> executarQueryPaginada(final String query, final String nomeEntidade, final Map<String, Object> variaveis, final Class<T> tipoClasse) {
        final String chaveEntidade = "GraphQL-" + nomeEntidade;
        
        // CIRCUIT BREAKER - Verificar se a entidade estÃ¡ com circuit aberto
        if (entidadesComCircuitAberto.contains(chaveEntidade)) {
            logger.warn("âš ï¸ CIRCUIT BREAKER ATIVO - Entidade {} temporariamente desabilitada devido a falhas consecutivas", nomeEntidade);
            final java.util.List<T> vazio = new java.util.ArrayList<>();
            return ResultadoExtracao.incompleto(vazio, ResultadoExtracao.MotivoInterrupcao.CIRCUIT_BREAKER, 0, 0);
        }
        
        logger.info("ðŸ” Executando query GraphQL paginada para entidade: {}", nomeEntidade);
        
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
                // PROTEÃ‡ÃƒO 1: Limite mÃ¡ximo de pÃ¡ginas (agora usa a variÃ¡vel jÃ¡ lida)
                if (paginaAtual > limitePaginas) {
                    logger.warn("ðŸš¨ PROTEÃ‡ÃƒO ATIVADA - Entidade {}: Limite de {} pÃ¡ginas atingido. Interrompendo busca para evitar loop infinito.", 
                            nomeEntidade, limitePaginas);
                    interrompido = true; // NOVO: Marcar como interrompido
                    break;
                }

                // PROTEÃ‡ÃƒO 2: Limite mÃ¡ximo de registros
                // PROBLEMA #7 CORRIGIDO: Usar valor de CarregadorConfig em vez de constante hardcoded
                final int maxRegistros = CarregadorConfig.obterMaxRegistrosGraphQL();
                if (totalRegistrosProcessados >= maxRegistros) {
                    logger.warn("ðŸš¨ PROTEÃ‡ÃƒO ATIVADA - Entidade {}: Limite de {} registros atingido. Interrompendo busca para evitar sobrecarga.", 
                            nomeEntidade, maxRegistros);
                    interrompido = true; // NOVO: Marcar como interrompido
                    break;
                }

                // Log de progresso a cada intervalo definido
                if (paginaAtual % INTERVALO_LOG_PROGRESSO == 0) {
                    logger.info("ðŸ“Š Progresso GraphQL {}: PÃ¡gina {}, {} registros processados", 
                            nomeEntidade, paginaAtual, totalRegistrosProcessados);
                }

                logger.debug("Executando pÃ¡gina {} da query GraphQL para {}", paginaAtual, nomeEntidade);
                
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
                        final Object v3 = ((java.util.Map<?, ?>) m).get("dueDate");
                        final Object v4 = ((java.util.Map<?, ?>) m).get("originalDueDate");
                        final Object v5 = ((java.util.Map<?, ?>) m).get("issueDate");
                        final String intervalo = v1 != null ? v1.toString()
                            : (v2 != null ? v2.toString()
                            : (v3 != null ? v3.toString()
                            : (v4 != null ? v4.toString()
                            : (v5 != null ? v5.toString() : ""))));
                        final String cursorStr = cursor != null ? cursor : "<inicio>";
                        resumoParams = "after=" + cursorStr + " | intervalo=" + intervalo;
                    }
                } catch (final RuntimeException ignored) {
                }
                if (resumoParams != null) {
                    logger.info("ParÃ¢metros da requisiÃ§Ã£o: {}", resumoParams);
                }

                // Executar a query para esta pÃ¡gina
                final PaginatedGraphQLResponse<T> resposta = executarQueryGraphQLTipado(query, nomeEntidade, variaveisComCursor, tipoClasse);
                
                // Adicionar entidades desta pÃ¡gina ao resultado total
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
                
                // PROTEÃ‡ÃƒO 3: Detectar cursor repetido (loop infinito)
                // Comparar o novo cursor retornado com o cursor que foi usado na requisiÃ§Ã£o atual
                // Se forem iguais E hasNextPage=true, significa que a API nÃ£o avanÃ§ou na paginaÃ§Ã£o (loop)
                // Se hasNextPage=false, mesmo com cursor repetido, Ã© vÃ¡lido (Ãºltima pÃ¡gina)
                // MELHORIA: Se a pÃ¡gina retornou menos registros que o esperado, pode ser a Ãºltima pÃ¡gina
                final String novoCursor = resposta.getEndCursor();
                if (novoCursor != null && cursor != null && novoCursor.equals(cursor)) {
                    if (resposta.getHasNextPage()) {
                        // Verificar se a pÃ¡gina retornou menos registros que o esperado
                        final int registrosEsperados = perInt; // first: 100 (ou outro valor configurado)
                        final int registrosRecebidos = resposta.getEntidades().size();
                        
                        if (registrosRecebidos < registrosEsperados) {
                            // PÃ¡gina incompleta + cursor repetido = provavelmente Ãºltima pÃ¡gina
                            logger.warn("âš ï¸ Entidade {}: Cursor repetido ({}) mas pÃ¡gina incompleta ({} < {}). Tratando como Ãºltima pÃ¡gina vÃ¡lida.", 
                                nomeEntidade, novoCursor, registrosRecebidos, registrosEsperados);
                            // NÃ£o interromper - tratar como Ãºltima pÃ¡gina vÃ¡lida
                            break;
                        } else {
                            // PÃ¡gina completa + cursor repetido + hasNextPage=true = loop infinito
                            logger.warn("ðŸš¨ PROTEÃ‡ÃƒO ATIVADA - Entidade {}: Cursor repetido detectado ({}). A API retornou o mesmo cursor que foi enviado E indicou hasNextPage=true. Interrompendo busca para evitar loop infinito.", 
                                nomeEntidade, novoCursor);
                            interrompido = true;
                            break;
                        }
                    } else {
                        // Cursor repetido + hasNextPage=false = Ãºltima pÃ¡gina (comportamento vÃ¡lido da API)
                        logger.debug("Entidade {}: Cursor repetido ({}) mas hasNextPage=false. Tratando como Ãºltima pÃ¡gina vÃ¡lida.", 
                                nomeEntidade, novoCursor);
                    }
                }
                
                // PROTEÃ‡ÃƒO 4: Detectar pÃ¡gina vazia com hasNextPage = true (possÃ­vel bug da API)
                if (resposta.getEntidades().isEmpty() && resposta.getHasNextPage()) {
                    logger.warn("ðŸš¨ PROTEÃ‡ÃƒO ATIVADA - Entidade {}: PÃ¡gina vazia com hasNextPage=true detectada. Interrompendo busca para evitar loop infinito.", 
                            nomeEntidade);
                    interrompido = true;
                    break;
                }
                
                // Atualizar informaÃ§Ãµes de paginaÃ§Ã£o
                hasNextPage = resposta.getHasNextPage();
                cursor = novoCursor;
                
                logger.debug("âœ… PÃ¡gina {} processada: {} entidades encontradas. PrÃ³xima pÃ¡gina: {} (Total: {})", 
                            paginaAtual, resposta.getEntidades().size(), hasNextPage, totalRegistrosProcessados);
                
                paginaAtual++;
                
                // NÃ£o Ã© mais necessÃ¡rio pausar entre requisiÃ§Ãµes - o GerenciadorRequisicaoHttp jÃ¡ controla o throttling
                
            } catch (final RuntimeException e) {
                logger.error("ðŸ’¥ Erro ao executar query GraphQL para entidade {} pÃ¡gina {}: {}", 
                        nomeEntidade, paginaAtual, e.getMessage(), e);
                incrementarContadorFalhas(chaveEntidade, nomeEntidade);
                interrompido = true;
                break;
            }
        }

        // NOVO: Retornar ResultadoExtracao baseado na flag de interrupÃ§Ã£o
        if (interrompido) {
            logger.warn("âš ï¸ Query GraphQL INCOMPLETA - Entidade {}: {} registros extraÃ­dos em {} pÃ¡ginas (INTERROMPIDA por proteÃ§Ãµes)", 
                    nomeEntidade, totalRegistrosProcessados, paginaAtual - 1);
            return ResultadoExtracao.incompleto(todasEntidades, ResultadoExtracao.MotivoInterrupcao.LIMITE_PAGINAS, paginaAtual - 1, totalRegistrosProcessados);
        } else {
            // Log final com resultado claro e diferenciado
            if (todasEntidades.isEmpty()) {
                logger.info("âŒ Query GraphQL concluÃ­da - Entidade {}: Nenhum registro encontrado", nomeEntidade);
            } else {
                logger.info("âœ… Query GraphQL COMPLETA - Entidade {}: {} registros extraÃ­dos em {} pÃ¡ginas (ProteÃ§Ãµes: âœ“ Ativas)", 
                        nomeEntidade, totalRegistrosProcessados, paginaAtual - 1);
            }
            return ResultadoExtracao.completo(todasEntidades, paginaAtual - 1, totalRegistrosProcessados);
        }
    }

    

    /**
     * Construtor da classe ClienteApiGraphQL
     * Inicializa as configuraÃ§Ãµes necessÃ¡rias para comunicaÃ§Ã£o com a API GraphQL
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
        this.gerenciadorRequisicao = GerenciadorRequisicaoHttp.getInstance();
        this.pageAuditRepository = new PageAuditRepository();
        this.executionUuid = java.util.UUID.randomUUID().toString();
    }

    public void setExecutionUuid(final String uuid) {
        this.executionUuid = uuid;
    }
    /**
     * Busca coletas via GraphQL para as Ãºltimas 24h (ontem + hoje).
     * MÃ©todo de conveniÃªncia que delega para buscarColetas(dataInicio, dataFim).
     * 
     * @param dataReferencia Data de referÃªncia para buscar as coletas (LocalDate)
     * @return ResultadoExtracao indicando se a busca foi completa ou interrompida
     */
    public ResultadoExtracao<ColetaNodeDTO> buscarColetas(final LocalDate dataReferencia) {
        final LocalDate diaAnterior = dataReferencia.minusDays(1);
        return buscarColetas(diaAnterior, dataReferencia);
    }





    /**
     * Busca fretes via GraphQL para as Ãºltimas 24 horas a partir de uma data de referÃªncia.
     * MÃ©todo de conveniÃªncia que delega para buscarFretes(dataInicio, dataFim).
     * 
     * @param dataReferencia Data de referÃªncia que representa o FIM do intervalo de busca.
     * @return ResultadoExtracao indicando se a busca foi completa ou interrompida
     */
    public ResultadoExtracao<FreteNodeDTO> buscarFretes(final LocalDate dataReferencia) {
        final LocalDate dataInicio = dataReferencia.minusDays(1);
        return buscarFretes(dataInicio, dataReferencia);
    }

    /**
     * Busca coletas via GraphQL para um intervalo de datas.
     * Utiliza GraphQLIntervaloHelper para iterar dia a dia (API nÃ£o suporta intervalo).
     * 
     * @param dataInicio Data de inÃ­cio do perÃ­odo
     * @param dataFim Data de fim do perÃ­odo
     * @return ResultadoExtracao indicando se a busca foi completa ou interrompida
     */
    public ResultadoExtracao<ColetaNodeDTO> buscarColetas(final LocalDate dataInicio, final LocalDate dataFim) {
        final boolean suportaServiceDate = suportaFiltroPick("serviceDate");
        if (suportaServiceDate) {
            logger.info("ðŸ” Coletas: usando filtros combinados requestDate + serviceDate para reduzir perdas referenciais.");
            return buscarColetasComFiltrosCombinados(dataInicio, dataFim);
        }
        logger.info("â„¹ï¸ Coletas: filtro serviceDate nÃ£o disponÃ­vel no schema atual, usando requestDate.");
        return GraphQLIntervaloHelper.executarPorDia(
            dataInicio,
            dataFim,
            this::buscarColetasDia,
            "Coletas"
        );
    }

    private ResultadoExtracao<ColetaNodeDTO> buscarColetasComFiltrosCombinados(final LocalDate dataInicio, final LocalDate dataFim) {
        final List<ColetaNodeDTO> acumulado = new ArrayList<>();
        int totalPaginas = 0;
        boolean completo = true;

        LocalDate dataAtual = dataInicio;
        while (!dataAtual.isAfter(dataFim)) {
            final ResultadoExtracao<ColetaNodeDTO> porRequestDate = buscarColetasDiaComCampo(dataAtual, "requestDate");
            final ResultadoExtracao<ColetaNodeDTO> porServiceDate = buscarColetasDiaComCampo(dataAtual, "serviceDate");

            acumulado.addAll(porRequestDate.getDados());
            acumulado.addAll(porServiceDate.getDados());
            totalPaginas += porRequestDate.getPaginasProcessadas();
            totalPaginas += porServiceDate.getPaginasProcessadas();

            if (!porRequestDate.isCompleto() || !porServiceDate.isCompleto()) {
                completo = false;
            }
            dataAtual = dataAtual.plusDays(1);
        }

        final List<ColetaNodeDTO> deduplicado = deduplicarColetasPorId(acumulado);
        final int duplicadosRemovidos = acumulado.size() - deduplicado.size();
        if (duplicadosRemovidos > 0) {
            logger.info("â„¹ï¸ Coletas combinadas: {} duplicado(s) removido(s) por id/sequenceCode.", duplicadosRemovidos);
        }

        if (completo) {
            return ResultadoExtracao.completo(deduplicado, totalPaginas, deduplicado.size());
        }
        return ResultadoExtracao.incompleto(
            deduplicado,
            ResultadoExtracao.MotivoInterrupcao.LIMITE_PAGINAS,
            totalPaginas,
            deduplicado.size()
        );
    }

    private List<ColetaNodeDTO> deduplicarColetasPorId(final List<ColetaNodeDTO> coletas) {
        final Map<String, ColetaNodeDTO> unicos = new LinkedHashMap<>();
        for (final ColetaNodeDTO coleta : coletas) {
            if (coleta == null) {
                continue;
            }
            final String chave = coleta.getId() != null && !coleta.getId().isBlank()
                ? coleta.getId()
                : String.valueOf(coleta.getSequenceCode());
            unicos.put(chave, coleta);
        }
        return new ArrayList<>(unicos.values());
    }

    /**
     * Busca coletas para um Ãºnico dia especÃ­fico.
     * MÃ©todo auxiliar usado pelo GraphQLIntervaloHelper.
     * 
     * @param data Data especÃ­fica para buscar coletas
     * @return ResultadoExtracao das coletas do dia
     */
    private ResultadoExtracao<ColetaNodeDTO> buscarColetasDia(final LocalDate data) {
        return buscarColetasDiaComCampo(data, "requestDate");
    }

    private ResultadoExtracao<ColetaNodeDTO> buscarColetasDiaComCampo(final LocalDate data, final String campoData) {
        final String dataFormatada = formatarDataParaApiGraphQL(data);
        final Map<String, Object> variaveis = Map.of(
            "params", Map.of(campoData, dataFormatada)
        );
        return executarQueryPaginada(GraphQLQueries.QUERY_COLETAS, 
            ConstantesApiGraphQL.obterNomeEntidadeApi(ConstantesEntidades.COLETAS), variaveis, ColetaNodeDTO.class);
    }

    private boolean suportaFiltroPick(final String campo) {
        final Set<String> campos = listarCamposInputPick();
        return campos.contains(campo);
    }

    private Set<String> listarCamposInputPick() {
        if (camposPickInputCache != null) {
            return camposPickInputCache;
        }

        final Set<String> campos = new HashSet<>();
        try {
            final ObjectNode corpoJson = mapeadorJson.createObjectNode();
            corpoJson.put("query", GraphQLQueries.INTROSPECTION_PICK_INPUT);
            final String corpoRequisicao = mapeadorJson.writeValueAsString(corpoJson);
            final HttpRequest requisicao = HttpRequest.newBuilder()
                    .uri(URI.create(urlBase + endpointGraphQL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.ofString(corpoRequisicao))
                    .timeout(this.timeoutRequisicao)
                    .build();
            final HttpResponse<String> resposta = gerenciadorRequisicao.executarRequisicao(this.clienteHttp, requisicao, "GraphQL-Introspection-PickInput");
            final JsonNode respostaJson = mapeadorJson.readTree(resposta.body());
            final JsonNode fields = respostaJson.path("data").path("__type").path("inputFields");
            if (fields.isArray()) {
                for (final JsonNode f : fields) {
                    final String nome = f.path("name").asText();
                    if (nome != null && !nome.isBlank()) {
                        campos.add(nome);
                    }
                }
            }
        } catch (final RuntimeException | java.io.IOException e) {
            logger.warn("Falha ao introspectar PickInput. Seguira com requestDate apenas: {}", e.getMessage());
        }

        camposPickInputCache = Set.copyOf(campos);
        return camposPickInputCache;
    }

    /**
     * Busca fretes via GraphQL para um intervalo de datas.
     * API de fretes suporta intervalo diretamente via serviceAt.
     * 
     * @param dataInicio Data de inÃ­cio do perÃ­odo
     * @param dataFim Data de fim do perÃ­odo
     * @return ResultadoExtracao indicando se a busca foi completa ou interrompida
     */
    public ResultadoExtracao<FreteNodeDTO> buscarFretes(final LocalDate dataInicio, final LocalDate dataFim) {
        logger.info("ðŸ” Buscando fretes via GraphQL - PerÃ­odo: {} a {}", dataInicio, dataFim);
        
        // Usar formato "dataInicio - dataFim" no filtro serviceAt (jÃ¡ suportado pela API)
        final String intervaloServiceAt = formatarDataParaApiGraphQL(dataInicio) + " - " + formatarDataParaApiGraphQL(dataFim);
        
        final Map<String, Object> variaveis = Map.of(
            "params", Map.of("serviceAt", intervaloServiceAt)
        );

        return executarQueryPaginada(GraphQLQueries.QUERY_FRETES, 
            ConstantesApiGraphQL.obterNomeEntidadeApi(ConstantesEntidades.FRETES), variaveis, FreteNodeDTO.class);
    }

    /**
     * Busca usuÃ¡rios do sistema (Individual) via GraphQL.
     * NÃ£o utiliza filtro de data, apenas filtra por enabled: true.
     * Utiliza paginaÃ§Ã£o cursor-based para extrair todos os usuÃ¡rios ativos.
     * 
     * @return ResultadoExtracao indicando se a busca foi completa ou interrompida
     */
    public ResultadoExtracao<br.com.extrator.modelo.graphql.usuarios.IndividualNodeDTO> buscarUsuariosSistema() {
        try {
            final Map<String, Object> variaveis = new HashMap<>();
            variaveis.put("params", Map.of("enabled", true));
            
            logger.info("Buscando UsuÃ¡rios do Sistema via GraphQL (enabled: true)");
            return executarQueryPaginada(
                GraphQLQueries.QUERY_USUARIOS_SISTEMA, 
                ConstantesApiGraphQL.obterNomeEntidadeApi(ConstantesEntidades.USUARIOS_SISTEMA), 
                variaveis, 
                br.com.extrator.modelo.graphql.usuarios.IndividualNodeDTO.class
            );
        } catch (final RuntimeException e) {
            logger.warn("Falha ao buscar UsuÃ¡rios do Sistema: {}", e.getMessage());
            final List<br.com.extrator.modelo.graphql.usuarios.IndividualNodeDTO> vazio = new ArrayList<>();
            return ResultadoExtracao.incompleto(vazio, ResultadoExtracao.MotivoInterrupcao.ERRO_API, 0, 0);
        }
    }

    /**
     * Busca NFSe diretamente via GraphQL para enriquecer fretes com metadados.
     * Utiliza paginaÃ§Ã£o e traz campos diretos e o XML bruto.
     */
    public ResultadoExtracao<br.com.extrator.modelo.graphql.fretes.nfse.NfseNodeDTO> buscarNfseDireta(final LocalDate dataReferencia) {
        try {
            final LocalDate dataInicio = dataReferencia.minusDays(1);
            final String intervaloIssuedAt = dataInicio.format(FormatadorData.ISO_DATE) + " - " + dataReferencia.format(FormatadorData.ISO_DATE);
            final Map<String, Object> variaveis = Map.of(
                "params", Map.of("issuedAt", intervaloIssuedAt)
            );
            logger.info("Buscando NFSe via GraphQL - PerÃ­odo: {}", intervaloIssuedAt);
            return executarQueryPaginada(GraphQLQueries.QUERY_NFSE, 
                ConstantesApiGraphQL.obterNomeEntidadeApi("nfse"), variaveis, br.com.extrator.modelo.graphql.fretes.nfse.NfseNodeDTO.class);
        } catch (final RuntimeException e) {
            logger.warn("Falha ao buscar NFSe direta: {}", e.getMessage());
            final List<br.com.extrator.modelo.graphql.fretes.nfse.NfseNodeDTO> vazio = new ArrayList<>();
            return ResultadoExtracao.incompleto(vazio, ResultadoExtracao.MotivoInterrupcao.ERRO_API, 0, 0);
        }
    }

    /**
     * Busca capa de faturas via GraphQL para a data de referÃªncia.
     * Utiliza janela configurÃ¡vel para buscar dias anteriores.
     * 
     * @param dataReferencia Data de referÃªncia (normalmente hoje)
     * @return ResultadoExtracao das faturas encontradas
     */
    public ResultadoExtracao<br.com.extrator.modelo.graphql.faturas.CreditCustomerBillingNodeDTO> buscarCapaFaturas(final LocalDate dataReferencia) {
        final int diasJanela = CarregadorConfig.obterDiasJanelaFaturasGraphQL();
        final LocalDate dataInicio = dataReferencia.minusDays(Math.max(0, diasJanela - 1));
        return buscarCapaFaturas(dataInicio, dataReferencia);
    }

    /**
     * Busca capa de faturas via GraphQL para um intervalo de datas especÃ­fico.
     * Utiliza GraphQLIntervaloHelper para iterar dia a dia (API nÃ£o suporta intervalo).
     * 
     * @param dataInicio Data de inÃ­cio do perÃ­odo
     * @param dataFim Data de fim do perÃ­odo
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
     * Busca capa de faturas para um Ãºnico dia especÃ­fico.
     * MÃ©todo auxiliar usado pelo GraphQLIntervaloHelper.
     * 
     * @param data Data especÃ­fica para buscar faturas
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
     * Lista os campos disponÃ­veis no tipo CreditCustomerBillingInput via introspection.
     * Usado para determinar qual campo de filtro usar (dueDate, issueDate, etc).
     * 
     * @return Lista de nomes de campos disponÃ­veis
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
     * Executa uma query GraphQL de forma genÃ©rica e robusta com desserializaÃ§Ã£o tipada
     * 
     * @param query        A query GraphQL a ser executada
     * @param nomeEntidade Nome da entidade para logs e tratamento de erros
     * @param variaveis    VariÃ¡veis da query GraphQL
     * @param tipoClasse   Classe para desserializaÃ§Ã£o tipada
     * @return Resposta paginada contendo entidades tipadas e informaÃ§Ãµes de paginaÃ§Ã£o
     */
    private <T> PaginatedGraphQLResponse<T> executarQueryGraphQLTipado(final String query, final String nomeEntidade,
            final Map<String, Object> variaveis, final Class<T> tipoClasse) {
        logger.debug("Executando query GraphQL tipada para {} - URL: {}{}, VariÃ¡veis: {}", 
                    nomeEntidade, urlBase, endpointGraphQL, variaveis);
        final List<T> entidades = new ArrayList<>();
        boolean hasNextPage = false;
        String endCursor = null;
        int statusCode;
        int duracaoMs;
        String reqHash;
        String respHash;

        // ValidaÃ§Ã£o bÃ¡sica de configuraÃ§Ã£o
        if (urlBase == null || urlBase.isBlank() || token == null || token.isBlank()) {
            logger.error("ConfiguraÃ§Ãµes invÃ¡lidas para chamada GraphQL (urlBase/token)");
            return new PaginatedGraphQLResponse<>(entidades, false, null, 0, 0, "", "", entidades.size());
        }

        try {
            final long tempoInicio = System.currentTimeMillis();
            // Construir o corpo da requisiÃ§Ã£o GraphQL usando ObjectMapper
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

            // Construir a requisiÃ§Ã£o HTTP
            final HttpRequest requisicao = HttpRequest.newBuilder()
                    .uri(URI.create(urlBase + endpointGraphQL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.ofString(corpoRequisicao))
                    .timeout(this.timeoutRequisicao)
                    .build();

            // Executar a requisiÃ§Ã£o usando o gerenciador central
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

            // Verificar se hÃ¡ erros na resposta GraphQL
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
                logger.warn("Campo '{}' nÃ£o encontrado na resposta GraphQL. Campos disponÃ­veis: {}",
                        nomeEntidade, dados.fieldNames());
                return new PaginatedGraphQLResponse<>(entidades, false, null, statusCode, duracaoMs, reqHash, respHash, entidades.size());
            }

            final JsonNode dadosEntidade = dados.get(nomeEntidade);

            // Verificar se a resposta segue o padrÃ£o paginado com edges/node
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
                
                // Extrair informaÃ§Ãµes de paginaÃ§Ã£o do pageInfo
                if (dadosEntidade.has("pageInfo")) {
                    final JsonNode pageInfo = dadosEntidade.get("pageInfo");
                    if (pageInfo.has("hasNextPage")) {
                        hasNextPage = pageInfo.get("hasNextPage").asBoolean();
                    }
                    if (pageInfo.has("endCursor") && !pageInfo.get("endCursor").isNull()) {
                        endCursor = pageInfo.get("endCursor").asText();
                    }
                    logger.debug("InformaÃ§Ãµes de paginaÃ§Ã£o extraÃ­das - hasNextPage: {}, endCursor: {}", hasNextPage, endCursor);
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

            logger.debug("Query GraphQL tipada concluÃ­da para {}. Total encontrado: {}", nomeEntidade, entidades.size());

        } catch (final JsonProcessingException e) {
            logger.error("Erro de processamento JSON durante execuÃ§Ã£o da query GraphQL para {}: {}", nomeEntidade, e.getMessage(), e);
            return new PaginatedGraphQLResponse<>(entidades, false, null, 0, 0, "", "", entidades.size());
        } catch (final RuntimeException e) {
            logger.error("Erro durante execuÃ§Ã£o da query GraphQL para {}: {}", nomeEntidade, e.getMessage(), e);
            return new PaginatedGraphQLResponse<>(entidades, false, null, 0, 0, "", "", entidades.size());
        }

        return new PaginatedGraphQLResponse<>(entidades, hasNextPage, endCursor, statusCode, duracaoMs, reqHash, respHash, entidades.size());
    }

    /**
     * Valida se as credenciais de acesso Ã  API GraphQL estÃ£o funcionando
     * 
     * @return true se a validaÃ§Ã£o foi bem-sucedida, false caso contrÃ¡rio
     */
    public boolean validarAcessoApi() {
        logger.info("Validando acesso Ã  API GraphQL...");

        try {
            // Query simples para testar a conectividade
            final String queryTeste = "{ __schema { queryType { name } } }";

            // Construir o corpo da requisiÃ§Ã£o GraphQL usando ObjectMapper
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
                    logger.info("âœ… ValidaÃ§Ã£o da API GraphQL bem-sucedida");
                } else {
                    logger.error("âŒ Erro na validaÃ§Ã£o da API GraphQL: {}", respostaJson.get("errors"));
                }

                return sucesso;
            } else {
                logger.error("âŒ Falha na validaÃ§Ã£o da API GraphQL. Status: {}", resposta.statusCode());
                return false;
            }

        } catch (java.io.IOException | InterruptedException e) {
            logger.error("âŒ Erro durante validaÃ§Ã£o da API GraphQL: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Busca dados de enriquecimento de uma fatura especÃ­fica via GraphQL.
     * Executa a query EnriquecerFaturas para obter NÂ° NFS-e, Carteira e InstruÃ§Ã£o Customizada.
     * 
     * @param billingId ID da cobranÃ§a (creditCustomerBilling)
     * @return Optional com CreditCustomerBillingNodeDTO contendo os dados enriquecidos, ou empty se nÃ£o encontrado
     */
    public java.util.Optional<CreditCustomerBillingNodeDTO> enriquecerFatura(final String billingId) {
        if (billingId == null || billingId.isBlank()) {
            logger.warn("âš ï¸ Tentativa de enriquecer fatura com ID nulo ou vazio");
            return java.util.Optional.empty();
        }
        
        try {
            final Map<String, Object> variaveis = Map.of("id", billingId);
            final PaginatedGraphQLResponse<CreditCustomerBillingNodeDTO> resposta = 
                executarQueryGraphQLTipado(
                    GraphQLQueries.QUERY_ENRIQUECER_FATURAS,
                    "creditCustomerBilling",
                    variaveis,
                    CreditCustomerBillingNodeDTO.class
                );
            
            if (resposta.getEntidades() != null && !resposta.getEntidades().isEmpty()) {
                return java.util.Optional.of(resposta.getEntidades().get(0));
            }
            
            return java.util.Optional.empty();
        } catch (final Exception e) {
            logger.error("âŒ Erro ao enriquecer fatura com ID {}: {}", billingId, e.getMessage(), e);
            return java.util.Optional.empty();
        }
    }
    
    /**
     * Enriquece fatura usando o nÃºmero do documento (fallback quando billingId nÃ£o estÃ¡ disponÃ­vel).
     * Usa fit_ant_document do DataExport para buscar a cobranÃ§a no GraphQL.
     * 
     * @param document NÃºmero do documento da fatura (ex: "112025/1-3")
     * @return Optional com os dados de enriquecimento ou empty se nÃ£o encontrado
     */
    public java.util.Optional<CreditCustomerBillingNodeDTO> enriquecerFaturaPorDocumento(final String document) {
        if (document == null || document.isBlank()) {
            logger.warn("âš ï¸ Tentativa de enriquecer fatura com documento nulo ou vazio");
            return java.util.Optional.empty();
        }
        
        try {
            final Map<String, Object> variaveis = Map.of("document", document);
            final PaginatedGraphQLResponse<CreditCustomerBillingNodeDTO> resposta = 
                executarQueryGraphQLTipado(
                    GraphQLQueries.QUERY_ENRIQUECER_FATURAS_POR_DOCUMENTO,
                    "creditCustomerBilling",
                    variaveis,
                    CreditCustomerBillingNodeDTO.class
                );
            
            if (resposta.getEntidades() != null && !resposta.getEntidades().isEmpty()) {
                logger.debug("âœ… Fatura encontrada via documento: {}", document);
                return java.util.Optional.of(resposta.getEntidades().get(0));
            }
            
            logger.debug("âš ï¸ Fatura nÃ£o encontrada via documento: {}", document);
            return java.util.Optional.empty();
        } catch (final Exception e) {
            logger.error("âŒ Erro ao enriquecer fatura por documento {}: {}", document, e.getMessage(), e);
            return java.util.Optional.empty();
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
     * Incrementa o contador de falhas consecutivas e ativa o circuit breaker se necessÃ¡rio.
     * 
     * @param chaveEntidade Chave identificadora da entidade GraphQL
     * @param nomeEntidade Nome amigÃ¡vel da entidade para logs
     */
    private void incrementarContadorFalhas(final String chaveEntidade, final String nomeEntidade) {
        final int falhas = contadorFalhasConsecutivas.getOrDefault(chaveEntidade, 0) + 1;
        contadorFalhasConsecutivas.put(chaveEntidade, falhas);
        
        if (falhas >= MAX_FALHAS_CONSECUTIVAS) {
            entidadesComCircuitAberto.add(chaveEntidade);
            logger.error("ðŸš¨ CIRCUIT BREAKER ATIVADO - Entidade {} ({}): {} falhas consecutivas. Entidade temporariamente desabilitada.", 
                    chaveEntidade, nomeEntidade, falhas);
        } else {
            logger.warn("âš ï¸ Falha {}/{} para entidade {} ({})", falhas, MAX_FALHAS_CONSECUTIVAS, chaveEntidade, nomeEntidade);
        }
    }
    
    /**
     * Busca dados de enriquecimento de uma cobranÃ§a especÃ­fica via GraphQL.
     * Retorna ticketAccountId, NFS-e e mÃ©todo de pagamento da primeira parcela.
     * 
     * @param billingId ID da cobranÃ§a (creditCustomerBilling)
     * @return Optional com CreditCustomerBillingNodeDTO contendo os dados enriquecidos, ou empty se nÃ£o encontrado
     */
    public java.util.Optional<CreditCustomerBillingNodeDTO> buscarDadosCobranca(final Long billingId) {
        if (billingId == null) {
            logger.warn("âš ï¸ Tentativa de buscar dados de cobranÃ§a com ID nulo");
            return java.util.Optional.empty();
        }
        
        try {
            final Map<String, Object> variaveis = Map.of("id", billingId.toString());
            final PaginatedGraphQLResponse<CreditCustomerBillingNodeDTO> resposta = 
                executarQueryGraphQLTipado(
                    GraphQLQueries.QUERY_ENRIQUECER_COBRANCA_NFSE,
                    "creditCustomerBilling",
                    variaveis,
                    CreditCustomerBillingNodeDTO.class
                );
            
            if (resposta.getEntidades() != null && !resposta.getEntidades().isEmpty()) {
                return java.util.Optional.of(resposta.getEntidades().get(0));
            }
            
            return java.util.Optional.empty();
        } catch (final Exception e) {
            logger.error("âŒ Erro ao buscar dados de cobranÃ§a com ID {}: {}", billingId, e.getMessage(), e);
            return java.util.Optional.empty();
        }
    }
    
    /**
     * Busca detalhes de uma conta bancÃ¡ria via GraphQL.
     * Usado para resolver dados do banco via ticketAccountId (cache otimizado).
     * 
     * @param bankAccountId ID da conta bancÃ¡ria (ticketAccountId)
     * @return Optional com BankAccountNodeDTO contendo os detalhes do banco, ou empty se nÃ£o encontrado
     */
    public java.util.Optional<BankAccountNodeDTO> buscarDetalhesBanco(final Integer bankAccountId) {
        if (bankAccountId == null) {
            logger.warn("âš ï¸ Tentativa de buscar detalhes de banco com ID nulo");
            return java.util.Optional.empty();
        }
        
        try {
            final Map<String, Object> variaveis = Map.of("id", bankAccountId);
            final PaginatedGraphQLResponse<BankAccountNodeDTO> resposta = 
                executarQueryGraphQLTipado(
                    GraphQLQueries.QUERY_RESOLVER_CONTA_BANCARIA,
                    "bankAccount",
                    variaveis,
                    BankAccountNodeDTO.class
                );
            
            if (resposta.getEntidades() != null && !resposta.getEntidades().isEmpty()) {
                return java.util.Optional.of(resposta.getEntidades().get(0));
            }
            
            return java.util.Optional.empty();
        } catch (final Exception e) {
            logger.error("âŒ Erro ao buscar detalhes de banco com ID {}: {}", bankAccountId, e.getMessage(), e);
            return java.util.Optional.empty();
        }
    }
}
