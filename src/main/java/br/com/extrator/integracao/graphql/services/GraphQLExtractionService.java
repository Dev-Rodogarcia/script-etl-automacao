/* ==[DOC-FILE]===============================================================
Arquivo : src/main/java/br/com/extrator/runners/graphql/services/GraphQLExtractionService.java
Classe  : GraphQLExtractionService (class)
Pacote  : br.com.extrator.integracao.graphql.services
Modulo  : Servico de execucao GraphQL
Papel   : Implementa responsabilidade de graph qlextraction service.

Conecta com:
- ClienteApiGraphQL (api)
- ColetaRepository (db.repository)
- FreteRepository (db.repository)
- FaturaGraphQLRepository (db.repository)
- FaturaPorClienteRepository (db.repository)
- LogExtracaoRepository (db.repository)
- UsuarioSistemaRepository (db.repository)
- ColetaMapper (modelo.graphql.coletas)

Fluxo geral:
1) Coordena extractors da API GraphQL.
2) Controla ordem, limites e logging do processamento.
3) Propaga resultado consolidado para o runner.

Estrutura interna:
Metodos principais:
- GraphQLExtractionService(): realiza operacao relacionada a "graph qlextraction service".
- execute(...3 args): realiza operacao relacionada a "execute".
- shouldExecute(...2 args): realiza operacao relacionada a "should execute".
- shouldExecute(...3 args): realiza operacao relacionada a "should execute".
- extractUsuarios(...3 args): realiza operacao relacionada a "extract usuarios".
- extractColetas(...2 args): realiza operacao relacionada a "extract coletas".
- extractFretes(...2 args): realiza operacao relacionada a "extract fretes".
- extractFaturasGraphQL(...2 args): realiza operacao relacionada a "extract faturas graph ql".
- exibirResumoConsolidado(...2 args): realiza operacao relacionada a "exibir resumo consolidado".
- formatarNumero(...1 args): realiza operacao relacionada a "formatar numero".
Atributos-chave:
- apiClient: cliente de integracao externa.
- logRepository: dependencia de acesso a banco.
- logger: logger da classe para diagnostico.
- log: campo de estado para "log".
- entidadeEspecifica: campo de estado para "entidade especifica".
[DOC-FILE-END]============================================================== */

package br.com.extrator.integracao.graphql.services;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import br.com.extrator.aplicacao.contexto.AplicacaoContexto;
import br.com.extrator.aplicacao.portas.ExecutionAuditPort;
import br.com.extrator.dominio.graphql.coletas.ColetaNodeDTO;
import br.com.extrator.integracao.ClienteApiDataExport;
import br.com.extrator.integracao.ClienteApiGraphQL;
import br.com.extrator.persistencia.repositorio.ColetaRepository;
import br.com.extrator.persistencia.repositorio.FreteRepository;
import br.com.extrator.persistencia.repositorio.FaturaGraphQLRepository;
import br.com.extrator.persistencia.repositorio.FaturaPorClienteRepository;
import br.com.extrator.persistencia.repositorio.LogExtracaoRepository;
import br.com.extrator.persistencia.repositorio.UsuarioSistemaRepository;
import br.com.extrator.integracao.mapeamento.graphql.coletas.ColetaMapper;
import br.com.extrator.integracao.mapeamento.graphql.fretes.FreteMapper;
import br.com.extrator.integracao.mapeamento.graphql.usuarios.UsuarioSistemaMapper;
import br.com.extrator.integracao.comum.ConstantesExtracao;
import br.com.extrator.integracao.comum.EntityExtractor;
import br.com.extrator.integracao.comum.ExtractionHelper;
import br.com.extrator.integracao.comum.ExtractionLogger;
import br.com.extrator.integracao.comum.ExtractionResult;
import br.com.extrator.integracao.graphql.extractors.ColetaExtractor;
import br.com.extrator.integracao.graphql.extractors.FreteExtractor;
import br.com.extrator.integracao.graphql.extractors.FaturaGraphQLExtractor;
import br.com.extrator.integracao.graphql.extractors.UsuarioSistemaExtractor;
import br.com.extrator.plataforma.auditoria.aplicacao.ExecutionAuditRecorder;
import br.com.extrator.plataforma.auditoria.dominio.ExecutionPlanContext;
import br.com.extrator.plataforma.auditoria.dominio.ExecutionWindowPlan;
import br.com.extrator.suporte.concorrencia.ExecutionTimeoutException;
import br.com.extrator.suporte.concorrencia.OperationTimeoutGuard;
import br.com.extrator.suporte.configuracao.ConfigBanco;
import br.com.extrator.suporte.configuracao.ConfigEtl;
import br.com.extrator.suporte.console.LoggerConsole;
import br.com.extrator.suporte.tempo.RelogioSistema;
import br.com.extrator.suporte.validacao.ConstantesEntidades;

/**
 * Serviço de orquestração para extrações GraphQL.
 * Coordena a execução de todas as entidades GraphQL com logs detalhados e resumos consolidados.
 */
public class GraphQLExtractionService {
    @FunctionalInterface
    private interface TimedExtractionSupplier {
        ExtractionResult executar() throws Exception;
    }

    private record TimedExtractionOutcome(ExtractionResult result, List<String> avisosSeguranca) {
    }

    private record ExecutionDates(LocalDate inicio, LocalDate fim) {
    }
    
    private final ClienteApiGraphQL apiClient;
    private final LogExtracaoRepository logRepository;
    private final ExecutionAuditPort executionAuditPort;
    private final boolean auditoriaEstruturadaAtiva;
    private final ExtractionLogger logger;
    private final LoggerConsole log;
    
    public GraphQLExtractionService() {
        this(true);
    }

    public GraphQLExtractionService(final boolean auditoriaEstruturadaAtiva) {
        this(
            new ClienteApiGraphQL(),
            new LogExtracaoRepository(),
            AplicacaoContexto.executionAuditPort(),
            new ExtractionLogger(GraphQLExtractionService.class),
            LoggerConsole.getLogger(GraphQLExtractionService.class),
            auditoriaEstruturadaAtiva
        );
    }

    protected GraphQLExtractionService(final ClienteApiGraphQL apiClient,
                                       final LogExtracaoRepository logRepository,
                                       final ExecutionAuditPort executionAuditPort,
                                       final ExtractionLogger logger,
                                       final LoggerConsole log) {
        this(apiClient, logRepository, executionAuditPort, logger, log, true);
    }

    protected GraphQLExtractionService(final ClienteApiGraphQL apiClient,
                                       final LogExtracaoRepository logRepository,
                                       final ExecutionAuditPort executionAuditPort,
                                       final ExtractionLogger logger,
                                       final LoggerConsole log,
                                       final boolean auditoriaEstruturadaAtiva) {
        this.apiClient = apiClient;
        this.logRepository = logRepository;
        this.executionAuditPort = executionAuditPort;
        this.auditoriaEstruturadaAtiva = auditoriaEstruturadaAtiva;
        this.logger = logger;
        this.log = log;
        final String pipelineExecutionId =
            br.com.extrator.suporte.observabilidade.ExecutionContext.currentExecutionId();
        if (this.apiClient != null) {
            this.apiClient.setExecutionUuid(
                "n/a".equals(pipelineExecutionId)
                    ? java.util.UUID.randomUUID().toString()
                    : pipelineExecutionId
            );
        }
    }
    
    /**
     * Executa extrações GraphQL baseado nos parâmetros fornecidos.
     * 
     * @param dataInicio Data de início
     * @param dataFim Data de fim
     * @param entidade Nome da entidade específica (null = todas)
     * @throws RuntimeException Se houver falha crítica na extração
     */
    // Referência para a entidade específica (usada na lógica de faturas_graphql)
    private String entidadeEspecifica;
    
    public void execute(final LocalDate dataInicio, final LocalDate dataFim, final String entidade) {
        this.entidadeEspecifica = entidade;
        final LocalDateTime inicioExecucao = RelogioSistema.agora();
        final List<ExtractionResult> resultados = new ArrayList<>();
        
        log.info("");
        log.info("=".repeat(80));
        log.info("INICIANDO EXTRACOES GRAPHQL");
        log.info("=".repeat(80));
        log.info("Periodo: {} a {}", dataInicio, dataFim != null ? dataFim : dataInicio);
        log.info("Inicio: {}", inicioExecucao);
        log.info("Entidade(s): {}", entidade == null || entidade.isBlank() ? "TODAS" : entidade);
        log.info("Modo de integridade: {}", ConfigEtl.obterModoIntegridadeEtl());
        log.info("");
        
        validarInfraestrutura();
        ExtractionHelper.limparAvisosSeguranca();

        final boolean executarColetas = shouldExecute(entidade, ConstantesEntidades.COLETAS);
        final boolean executarFretes = shouldExecute(entidade, ConstantesEntidades.FRETES);
        final boolean executarFaturasGraphql = shouldExecute(entidade, ConstantesEntidades.FATURAS_GRAPHQL,
            ConstantesEntidades.ALIASES_FATURAS_GRAPHQL);
        final boolean executarUsuariosSistema = shouldExecute(entidade, ConstantesEntidades.USUARIOS_SISTEMA);
        boolean coletasConcluidasComSucesso = !executarColetas;

        // Extrair usuários ANTES de coletas (dependência)
        if (executarColetas) {
            try {
                final ExtractionResult resultUsuarios = executarComTimeout(
                    ConstantesEntidades.USUARIOS_SISTEMA,
                    () -> {
                        final ExecutionDates datas = resolverDatasExecucao(
                            ConstantesEntidades.USUARIOS_SISTEMA,
                            dataInicio,
                            dataFim
                        );
                        return extractUsuarios(datas.inicio(), datas.fim(), false);
                    }
                );
                if (resultUsuarios != null) {
                    resultados.add(resultUsuarios);
                }
            } catch (final Exception e) {
                registrarFalhaEntidade(
                    resultados,
                    ConstantesEntidades.USUARIOS_SISTEMA,
                    "Usuarios do Sistema",
                    e,
                    dataInicio,
                    dataFim
                );
            }
            aplicarDelayEntreEntidades();
        } else if (executarUsuariosSistema) {
            try {
                final ExtractionResult resultUsuarios = executarComTimeout(
                    ConstantesEntidades.USUARIOS_SISTEMA,
                    () -> {
                        final ExecutionDates datas = resolverDatasExecucao(
                            ConstantesEntidades.USUARIOS_SISTEMA,
                            dataInicio,
                            dataFim
                        );
                        return extractUsuarios(datas.inicio(), datas.fim(), true);
                    }
                );
                if (resultUsuarios != null) {
                    resultados.add(resultUsuarios);
                }
            } catch (final Exception e) {
                registrarFalhaEntidade(
                    resultados,
                    ConstantesEntidades.USUARIOS_SISTEMA,
                    "Usuarios do Sistema",
                    e,
                    dataInicio,
                    dataFim
                );
            }
            aplicarDelayEntreEntidades();
        }

        if (executarColetas) {
            try {
                final ExtractionResult result = executarComTimeout(
                    ConstantesEntidades.COLETAS,
                    () -> {
                        final ExecutionDates datas = resolverDatasExecucao(ConstantesEntidades.COLETAS, dataInicio, dataFim);
                        return extractColetas(datas.inicio(), datas.fim());
                    }
                );
                if (result != null) {
                    resultados.add(result);
                    coletasConcluidasComSucesso = ConstantesEntidades.STATUS_COMPLETO.equals(result.getStatus());
                }
            } catch (final Exception e) {
                coletasConcluidasComSucesso = false;
                registrarFalhaEntidade(resultados, ConstantesEntidades.COLETAS, "Coletas", e, dataInicio, dataFim);
            }
            aplicarDelayEntreEntidades();
        }

        if (executarFretes) {
            if (executarColetas && !coletasConcluidasComSucesso) {
                final ExtractionResult bloqueado = registrarBloqueioFretesPorColetas();
                resultados.add(bloqueado);
            } else {
                try {
                    final ExtractionResult result = executarComTimeout(
                        ConstantesEntidades.FRETES,
                        () -> {
                            final ExecutionDates datas =
                                resolverDatasExecucao(ConstantesEntidades.FRETES, dataInicio, dataFim);
                            return extractFretes(datas.inicio(), datas.fim());
                        }
                    );
                    if (result != null) {
                        resultados.add(result);
                    }
                } catch (final Exception e) {
                    registrarFalhaEntidade(resultados, ConstantesEntidades.FRETES, "Fretes", e, dataInicio, dataFim);
                }
            }
            aplicarDelayEntreEntidades();
        }

        // FASE 3: FATURAS_GRAPHQL foi movido para ser executado POR ÚLTIMO
        // A extração de faturas_graphql agora é controlada pelos comandos (ExecutarFluxoCompletoComando
        // e ExecutarExtracaoPorIntervaloComando) que chamam GraphQLRunner.executarFaturasGraphQLPorIntervalo()
        // APÓS todas as outras entidades serem extraídas.
        // 
        // Motivo: O enriquecimento de faturas_graphql é muito demorado (50+ minutos),
        // então as outras entidades são priorizadas para garantir dados parciais atualizados no BI.
        //
        // Se executarFaturasGraphql for true E estivermos em modo de extração específica de faturas_graphql,
        // executamos aqui. Caso contrário, deixamos para o comando orquestrador.
        final boolean isSomenteFaturasGraphQL = entidadeEspecifica != null && 
            (ConstantesEntidades.FATURAS_GRAPHQL.equalsIgnoreCase(entidadeEspecifica) ||
             java.util.Arrays.stream(ConstantesEntidades.ALIASES_FATURAS_GRAPHQL)
                 .anyMatch(alias -> alias.equalsIgnoreCase(entidadeEspecifica)));
        
        if (executarFaturasGraphql && isSomenteFaturasGraphQL) {
            // Extração específica de faturas_graphql foi solicitada explicitamente
            try {
                final ExtractionResult result = executarComTimeout(
                    ConstantesEntidades.FATURAS_GRAPHQL,
                    () -> {
                        final ExecutionDates datas =
                            resolverDatasExecucao(ConstantesEntidades.FATURAS_GRAPHQL, dataInicio, dataFim);
                        return extractFaturasGraphQL(datas.inicio(), datas.fim());
                    }
                );
                if (result != null) {
                    resultados.add(result);
                }
            } catch (final Exception e) {
                registrarFalhaEntidade(
                    resultados,
                    ConstantesEntidades.FATURAS_GRAPHQL,
                    "Faturas GraphQL",
                    e,
                    dataInicio,
                    dataFim
                );
            }
            aplicarDelayEntreEntidades();
        } else if (executarFaturasGraphql) {
            // Faturas GraphQL será executado POR ÚLTIMO pelo comando orquestrador
            log.info("[INFO] Faturas GraphQL sera extraido POR ULTIMO apos todas as outras entidades (FASE 3)");
        }

        // Resumo consolidado final
        exibirResumoConsolidado(resultados, inicioExecucao);

        // Se alguma entidade falhou, propagar falha para o comando não marcar extração como sucesso
        final boolean modoEstrito = ConfigEtl.isModoIntegridadeEstrito();
        final List<String> entidadesComFalha = resultados.stream()
            .filter(r -> deveFalharExecucaoFinal(r, modoEstrito))
            .map(r -> r.getEntityName() + "(" + r.getStatus() + ")")
            .toList();
        if (!entidadesComFalha.isEmpty()) {
            throw new RuntimeException("Extração GraphQL com falhas: " + String.join(", ", entidadesComFalha)
                + ". Verifique os logs. A extração NÃO deve ser considerada concluída com sucesso.");
        }
    }
    
    public ExtractionResult executarSomenteColetasReferencial(final LocalDate dataInicio, final LocalDate dataFim) {
        this.entidadeEspecifica = ConstantesEntidades.COLETAS;
        final LocalDateTime inicioExecucao = RelogioSistema.agora();

        log.info("");
        log.info("=".repeat(80));
        log.info("INICIANDO EXTRACAO GRAPHQL AUXILIAR: COLETAS REFERENCIAL");
        log.info("=".repeat(80));
        log.info("Periodo: {} a {}", dataInicio, dataFim != null ? dataFim : dataInicio);
        log.info("Inicio: {}", inicioExecucao);
        log.info("Modo: AUXILIAR_REFERENCIAL");
        log.info("");

        validarInfraestrutura();
        ExtractionHelper.limparAvisosSeguranca();

        final ExtractionResult resultado;
        try {
            resultado = executarComTimeout(
                ConstantesEntidades.COLETAS_REFERENCIAL,
                ConfigEtl.obterTimeoutEntidadeGraphQLColetasReferencial(),
                () -> extractColetasReferencial(dataInicio, dataFim)
            );
        } catch (final Exception e) {
            if (e instanceof ExecutionTimeoutException) {
                final String aviso = String.format(
                    "Timeout na entidade %s apos %d ms. A extracao auxiliar sera encerrada.",
                    ConstantesEntidades.COLETAS,
                    ConfigEtl.obterTimeoutEntidadeGraphQLColetasReferencial().toMillis()
                );
                ExtractionHelper.appendAvisoSeguranca(aviso);
                log.error("[TIMEOUT] {}", aviso, e);
            }
            throw new RuntimeException("Falha na extracao auxiliar de coletas referencial", e);
        }
        exibirResumoConsolidado(List.of(resultado), inicioExecucao);

        final boolean modoEstrito = ConfigEtl.isModoIntegridadeEstrito();
        final boolean possuiFalha = deveFalharExecucaoFinal(resultado, modoEstrito);
        if (possuiFalha) {
            throw new RuntimeException(
                "Extracao GraphQL auxiliar de coletas com falhas: "
                    + resultado.getEntityName()
                    + "(" + resultado.getStatus() + "). Verifique os logs."
            );
        }

        return resultado;
    }

    private boolean shouldExecute(final String entidade, final String entityName) {
        return entidade == null || entidade.isBlank() || entityName.equalsIgnoreCase(entidade);
    }
    
    private boolean shouldExecute(final String entidade, final String entityName, final String[] aliases) {
        if (entidade == null || entidade.isBlank()) {
            return true;
        }
        if (entityName.equalsIgnoreCase(entidade)) {
            return true;
        }
        return Arrays.stream(aliases).anyMatch(alias -> alias.equalsIgnoreCase(entidade));
    }
    
    
    protected void validarInfraestrutura() {
        ConfigBanco.validarConexaoBancoDados();
        ConfigBanco.validarTabelasEssenciais();
    }

    protected void aplicarDelayEntreEntidades() {
        ExtractionHelper.aplicarDelay();
    }

    protected void registrarLogExtracao(final ExtractionResult result) {
        if (result == null || ConstantesEntidades.isEntidadeAuxiliarLogExtracao(result.getEntityName())) {
            log.info(
                "Log operacional em banco ignorado para entidade auxiliar {}. O resultado permanece apenas nos logs de arquivo.",
                result != null ? result.getEntityName() : "n/a"
            );
            return;
        }
        logRepository.gravarLogExtracao(result.toLogEntity());
        if (auditoriaEstruturadaAtiva) {
            ExecutionAuditRecorder.registrar(executionAuditPort, result);
        }
    }

    protected ExtractionResult extractUsuarios(final LocalDate dataInicio, final LocalDate dataFim, final boolean throwOnError) {
        final UsuarioSistemaExtractor extractor = new UsuarioSistemaExtractor(
            apiClient,
            new UsuarioSistemaRepository(),
            new UsuarioSistemaMapper()
        );
        
        final String motivo = throwOnError ? "" : ConstantesExtracao.MSG_MOTIVO_USUARIOS_COLETAS;
        log.info(ConstantesExtracao.MSG_LOG_EXTRAINDO_COM_MOTIVO, extractor.getEmoji(), "Usuarios do Sistema", motivo);
        
        final ExtractionResult result = logger.executeWithLogging(extractor, dataInicio, dataFim, extractor.getEmoji());
        registrarLogExtracao(result);
        
        if (throwOnError && ConstantesEntidades.STATUS_ERRO_API.equals(result.getStatus())) {
            throw new RuntimeException(String.format(ConstantesExtracao.MSG_ERRO_EXTRACAO, "usuarios do sistema"), result.getErro());
        }
        
        return result;
    }
    
    protected ExtractionResult extractColetas(final LocalDate dataInicio, final LocalDate dataFim) {
        final ColetaExtractor extractor = new ColetaExtractor(
            apiClient,
            new ColetaRepository(),
            new ColetaMapper()
        );
        
        final ExtractionResult result = logger.executeWithLogging(extractor, dataInicio, dataFim, extractor.getEmoji());
        registrarLogExtracao(result);
        
        return result;
    }

    protected ExtractionResult extractColetasReferencial(final LocalDate dataInicio, final LocalDate dataFim) {
        final ColetaExtractor extractor = new ColetaExtractor(
            apiClient,
            new ColetaRepository(),
            new ColetaMapper()
        );
        final EntityExtractor<ColetaNodeDTO> extractorAuxiliar = new EntityExtractor<>() {
            @Override
            public br.com.extrator.integracao.ResultadoExtracao<ColetaNodeDTO> extract(final LocalDate inicio,
                                                                                        final LocalDate fim) {
                return extractor.extract(inicio, fim);
            }

            @Override
            public int save(final List<ColetaNodeDTO> dtos) throws java.sql.SQLException {
                return extractor.save(dtos);
            }

            @Override
            public SaveMetrics saveWithMetrics(final List<ColetaNodeDTO> dtos) throws java.sql.SQLException {
                return extractor.saveWithMetrics(dtos);
            }

            @Override
            public String getEntityName() {
                return ConstantesEntidades.COLETAS_REFERENCIAL;
            }

            @Override
            public String getEmoji() {
                return extractor.getEmoji();
            }

            @Override
            public boolean permiteConcluirComInvalidosAuditados() {
                return extractor.permiteConcluirComInvalidosAuditados();
            }
        };

        final ExtractionResult result =
            logger.executeWithLogging(extractorAuxiliar, dataInicio, dataFim, extractorAuxiliar.getEmoji());
        registrarLogExtracao(result);
        return result;
    }

    protected ExtractionResult extractFretes(final LocalDate dataInicio, final LocalDate dataFim) {
        final ClienteApiDataExport indicadoresApiClient = new ClienteApiDataExport();
        final String pipelineExecutionId =
            br.com.extrator.suporte.observabilidade.ExecutionContext.currentExecutionId();
        indicadoresApiClient.setExecutionUuid(
            "n/a".equals(pipelineExecutionId)
                ? java.util.UUID.randomUUID().toString()
                : pipelineExecutionId
        );
        final FreteExtractor extractor = new FreteExtractor(
            apiClient,
            new FreteRepository(),
            new FreteMapper(),
            indicadoresApiClient::buscarFretesIndicadores
        );
        
        final ExtractionResult result = logger.executeWithLogging(extractor, dataInicio, dataFim, extractor.getEmoji());
        registrarLogExtracao(result);
        return result;
    }
    
    protected ExtractionResult extractFaturasGraphQL(final LocalDate dataInicio, final LocalDate dataFim) {
        final FaturaGraphQLExtractor extractor = new FaturaGraphQLExtractor(
            apiClient,
            new FaturaGraphQLRepository(),
            new FaturaPorClienteRepository(),
            new FreteRepository(),
            log
        );
        
        final ExtractionResult result = logger.executeWithLogging(extractor, dataInicio, dataFim, extractor.getEmoji());
        registrarLogExtracao(result);
        
        return result;
    }

    /**
     * Exibe resumo consolidado de todas as extrações GraphQL executadas.
     */
    private void exibirResumoConsolidado(final List<ExtractionResult> resultados, final LocalDateTime inicioExecucao) {
        final LocalDateTime fimExecucao = RelogioSistema.agora();
        final Duration duracaoTotal = Duration.between(inicioExecucao, fimExecucao);
        
        log.info("");
        log.info("=".repeat(80));
        log.info("RESUMO CONSOLIDADO GRAPHQL");
        log.info("=".repeat(80));
        
        final int totalEntidades = resultados.size();
        int entidadesComSucesso = 0;
        int entidadesIncompletas = 0;
        int entidadesComErro = 0;
        int totalRegistrosExtraidos = 0;
        int totalRegistrosSalvos = 0;
        int totalPaginas = 0;
        
        for (final ExtractionResult result : resultados) {
            if (ConstantesEntidades.STATUS_COMPLETO.equals(result.getStatus())) {
                entidadesComSucesso++;
            } else if (ConstantesEntidades.STATUS_ERRO_API.equals(result.getStatus())) {
                entidadesComErro++;
            } else {
                entidadesIncompletas++;
            }
            totalRegistrosExtraidos += result.getRegistrosExtraidos();
            totalRegistrosSalvos += result.getRegistrosSalvos();
            totalPaginas += result.getPaginasProcessadas();
        }
        
        log.info("Estatisticas Gerais:");
        log.info("   - Entidades processadas: {}", totalEntidades);
        log.info("   - [OK] Sucessos: {}", entidadesComSucesso);
        if (entidadesIncompletas > 0) {
            log.info("   - [AVISO] Incompletas: {}", entidadesIncompletas);
        }
        if (entidadesComErro > 0) {
            log.info("   - [ERRO] Erros: {}", entidadesComErro);
        }
        log.info("");
        log.info("Volumes:");
        log.info("   - Total extraido da API: {} registros", formatarNumero(totalRegistrosExtraidos));
        log.info("   - Total salvo no banco: {} registros", formatarNumero(totalRegistrosSalvos));
        log.info("   - Total de paginas: {}", formatarNumero(totalPaginas));
        log.info("");
        log.info("Performance:");
        log.info("   - Tempo total: {} ms ({} s)", 
            duracaoTotal.toMillis(), 
            String.format("%.2f", duracaoTotal.toMillis() / 1000.0));
        if (totalRegistrosSalvos > 0 && duracaoTotal.toMillis() > 0) {
            final double registrosPorSegundo = (totalRegistrosSalvos * 1000.0) / duracaoTotal.toMillis();
            log.info("   - Taxa media: {} registros/segundo", String.format("%.2f", registrosPorSegundo));
        }
        log.info("");
        log.info("Detalhamento por Entidade:");
        for (int i = 0; i < resultados.size(); i++) {
            final ExtractionResult result = resultados.get(i);
            final String statusIcon;
            if (ConstantesEntidades.STATUS_COMPLETO.equals(result.getStatus())) {
                statusIcon = "[OK]";
            } else if (ConstantesEntidades.STATUS_ERRO_API.equals(result.getStatus())) {
                statusIcon = "[ERRO]";
            } else {
                statusIcon = "[AVISO]";
            }
            log.info("   {}. {} {}: {} registros salvos | {} paginas | {}",
                i + 1,
                statusIcon,
                result.getEntityName(),
                formatarNumero(result.getRegistrosSalvos()),
                result.getPaginasProcessadas(),
                result.getStatus());
        }

        // EVENTOS / OBSERVAÇÕES: timeouts, entidades com erro (ficam gravados no log)
        final List<String> eventos = new ArrayList<>(ExtractionHelper.drenarAvisosSeguranca());
        for (final ExtractionResult r : resultados) {
            if (!ConstantesEntidades.STATUS_COMPLETO.equals(r.getStatus())) {
                eventos.add("Entidade " + r.getEntityName() + " nao concluiu como COMPLETO (status="
                    + r.getStatus() + "). Sera reextraida na proxima execucao.");
            }
        }
        if (!eventos.isEmpty()) {
            log.info("");
            log.info("[AVISO] EVENTOS / OBSERVACOES (registrado para auditoria):");
            for (final String ev : eventos) {
                log.info("   - {}", ev);
            }
        }

        log.info("");
        log.info("Fim: {}", fimExecucao);
        log.info("=".repeat(80));
        if (entidadesComErro > 0 || entidadesIncompletas > 0) {
            final String resumoFalhas = "[AVISO] EXTRACOES GRAPHQL COM NAO CONFORMIDADES "
                + "(incompletas=" + entidadesIncompletas + ", erros=" + entidadesComErro + ")";
            log.info(resumoFalhas);
        } else {
            log.info("[OK] EXTRACOES GRAPHQL CONCLUIDAS");
        }
        log.info("=".repeat(80));
        log.info("");
    }
    
    public void executar(final LocalDate dataInicio, final LocalDate dataFim, final String entidade) {
        execute(dataInicio, dataFim, entidade);
    }

    private ExtractionResult executarComTimeout(final String entidade,
                                                final TimedExtractionSupplier supplier) throws Exception {
        return executarComTimeout(
            entidade,
            ConfigEtl.obterTimeoutEntidadeGraphQL(entidade),
            supplier
        );
    }

    private ExtractionResult executarComTimeout(final String operacao,
                                                final Duration timeout,
                                                final TimedExtractionSupplier supplier) throws Exception {
        final TimedExtractionOutcome outcome = OperationTimeoutGuard.executar(
            "graphql:" + operacao,
            timeout,
            () -> new TimedExtractionOutcome(
                supplier.executar(),
                ExtractionHelper.drenarAvisosSeguranca()
            )
        );
        outcome.avisosSeguranca().forEach(ExtractionHelper::appendAvisoSeguranca);
        return outcome.result();
    }

    private ExtractionResult registrarBloqueioFretesPorColetas() {
        final LocalDateTime inicio = RelogioSistema.agora();
        final String mensagem =
            "Fretes nao executado porque Coletas nao concluiu com sucesso no mesmo ciclo. O ciclo sera finalizado como falho.";
        log.error("[ERRO] {}", mensagem);
        ExtractionHelper.appendAvisoSeguranca(mensagem);
        final ExtractionResult result = ExtractionResult.bloqueadoPorDependencia(
            ConstantesEntidades.FRETES,
            inicio,
            ConstantesEntidades.COLETAS,
            mensagem
        ).build();
        registrarLogExtracao(result);
        return result;
    }

    private void registrarFalhaEntidade(final List<ExtractionResult> resultados,
                                        final String entidade,
                                        final String descricao,
                                        final Exception erro,
                                        final LocalDate dataInicio,
                                        final LocalDate dataFim) {
        if (erro instanceof ExecutionTimeoutException) {
            final long timeoutMs = ConfigEtl.obterTimeoutEntidadeGraphQL(entidade).toMillis();
            final String aviso = String.format(
                "Timeout na entidade %s apos %d ms. A execucao seguiu para a proxima entidade.",
                entidade,
                timeoutMs
            );
            ExtractionHelper.appendAvisoSeguranca(aviso);
            log.error("[TIMEOUT] {}", aviso, erro);
        } else {
            log.error(
                "[ERRO] Erro ao extrair {}: {}. Indo para a proxima entidade; sera reextraida na proxima execucao.",
                descricao,
                erro.getMessage(),
                erro
            );
        }
        final ExecutionDates datas = resolverDatasExecucao(entidade, dataInicio, dataFim);
        final ExecutionWindowPlan plano =
            ExecutionPlanContext.getPlano(entidade).orElseGet(() -> criarJanelaPadrao(datas.inicio(), datas.fim()));
        final ExtractionResult erroResult = ExtractionResult.erro(entidade, RelogioSistema.agora(), erro)
            .janelaConsultaInicio(plano.consultaInicioDateTime())
            .janelaConsultaFim(plano.consultaFimDateTime())
            .janelaConfirmacaoInicio(plano.confirmacaoInicio())
            .janelaConfirmacaoFim(plano.confirmacaoFim())
            .build();
        registrarLogExtracao(erroResult);
        resultados.add(erroResult);
    }

    private String formatarNumero(final int numero) {
        return String.format("%,d", numero);
    }

    private ExecutionDates resolverDatasExecucao(final String entidade,
                                                 final LocalDate dataInicio,
                                                 final LocalDate dataFim) {
        final ExecutionWindowPlan plano =
            ExecutionPlanContext.getPlano(entidade).orElseGet(() -> criarJanelaPadrao(dataInicio, dataFim));
        return new ExecutionDates(plano.consultaDataInicio(), plano.consultaDataFim());
    }

    private ExecutionWindowPlan criarJanelaPadrao(final LocalDate dataInicio, final LocalDate dataFim) {
        final LocalDate inicio = dataInicio != null ? dataInicio : RelogioSistema.hoje().minusDays(1);
        final LocalDate fim = dataFim != null ? dataFim : inicio;
        return new ExecutionWindowPlan(
            inicio,
            fim,
            inicio.atStartOfDay(),
            fim.atTime(java.time.LocalTime.MAX)
        );
    }

    private boolean deveFalharExecucaoFinal(final ExtractionResult result, final boolean modoEstrito) {
        if (result == null) {
            return false;
        }
        if (ConstantesEntidades.STATUS_COMPLETO.equals(result.getStatus())) {
            return false;
        }
        if (modoEstrito) {
            return true;
        }
        return isEntidadeCriticaParaCompletude(result.getEntityName())
            || ConstantesEntidades.STATUS_ERRO_API.equals(result.getStatus());
    }

    private boolean isEntidadeCriticaParaCompletude(final String entidade) {
        if (entidade == null || entidade.isBlank()) {
            return false;
        }
        return List.of(
            ConstantesEntidades.USUARIOS_SISTEMA,
            ConstantesEntidades.COLETAS,
            ConstantesEntidades.FRETES,
            ConstantesEntidades.FATURAS_GRAPHQL
        ).contains(entidade);
    }
}
