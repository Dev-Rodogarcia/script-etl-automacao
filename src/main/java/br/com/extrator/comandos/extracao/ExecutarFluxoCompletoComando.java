package br.com.extrator.comandos.extracao;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import br.com.extrator.auditoria.servicos.CompletudeValidator;
import br.com.extrator.auditoria.servicos.IntegridadeEtlValidator;
import br.com.extrator.comandos.base.Comando;
import br.com.extrator.util.console.LoggerConsole;
import br.com.extrator.runners.dataexport.DataExportRunner;
import br.com.extrator.runners.graphql.GraphQLRunner;
import br.com.extrator.util.console.BannerUtil;
import br.com.extrator.util.formatacao.FormatadorData;
import br.com.extrator.util.validacao.ConstantesEntidades;

/**
 * Comando responsÃ¡vel por executar o fluxo completo de extraÃ§Ã£o de dados
 * das 3 APIs do ESL Cloud (REST, GraphQL e DataExport).
 */
public class ExecutarFluxoCompletoComando implements Comando {
    // PROBLEMA #9 CORRIGIDO: Usar LoggerConsole para log duplo (arquivo + console)
    private static final LoggerConsole log = LoggerConsole.getLogger(ExecutarFluxoCompletoComando.class);
    private static final String FLAG_SEM_FATURAS_GRAPHQL = "--sem-faturas-graphql";
    private static final String FLAG_MODO_LOOP_DAEMON = "--modo-loop-daemon";
    
    // Constantes para gravaÃ§Ã£o do timestamp de execuÃ§Ã£o
    private static final String ARQUIVO_ULTIMO_RUN = "last_run.properties";
    private static final String PROPRIEDADE_ULTIMO_RUN = "last_successful_run";
    
    // NÃºmero de threads para execuÃ§Ã£o paralela dos runners
    private static final int NUMERO_DE_THREADS = 2;
    
    @Override
    public void executar(final String[] args) throws Exception {
        final boolean incluirFaturasGraphQL = !possuiFlag(args, FLAG_SEM_FATURAS_GRAPHQL);
        final boolean modoLoopDaemon = possuiFlag(args, FLAG_MODO_LOOP_DAEMON);

        // Exibe banner inicial de extraÃ§Ã£o completa
        BannerUtil.exibirBannerExtracaoCompleta();
        
        // Define data de hoje para buscar dados do dia atual
        final LocalDate dataHoje = LocalDate.now();
        
        // PROBLEMA #9 CORRIGIDO: Usar LoggerConsole para log duplo
        log.info("Iniciando processo de extraÃ§Ã£o de dados das 2 APIs do ESL Cloud");
        log.console("\n" + "=".repeat(60));
        log.console("INICIANDO PROCESSO DE EXTRAÃ‡ÃƒO DE DADOS");
        log.console("=".repeat(60));
        log.console("Modo: DADOS DE HOJE");
        if (modoLoopDaemon) {
            log.console("Contexto: LOOP DAEMON (integridade final nao bloqueante)");
        }
        log.console("Faturas GraphQL: {}", incluirFaturasGraphQL ? "INCLUIDO" : "DESABILITADO (flag --sem-faturas-graphql)");
        // PROBLEMA 13 CORRIGIDO: Usar FormatadorData em vez de criar formatters inline
        log.console("Data de extraÃ§Ã£o: {} (dados de hoje)", FormatadorData.formatBR(dataHoje));
        log.console("InÃ­cio: {}", FormatadorData.formatBR(LocalDateTime.now()));
        log.console("=".repeat(60) + "\n");
        
        final LocalDateTime inicioExecucao = LocalDateTime.now();
        boolean validacaoFinalCompleta = true;
        String detalheFalhaValidacao = null;
        int completudeEntidadesTotal = -1;
        int completudeEntidadesNaoOk = -1;
        int integridadeFalhas = -1;
        
        // ========== EXECUÃ‡ÃƒO PARALELA DOS RUNNERS ==========
        log.info("ðŸ”„ Iniciando fluxo ETL em modo paralelo com {} threads", NUMERO_DE_THREADS);
        
        final ExecutorService executor = Executors.newFixedThreadPool(NUMERO_DE_THREADS);
        // Usar LinkedHashMap para manter ordem de inserÃ§Ã£o e associar explicitamente nome ao Future
        // Isso elimina o risco de desalinhamento entre ordem de submissÃ£o e nomes dos runners
        final Map<String, Future<?>> runnersFuturos = new LinkedHashMap<>();
        final List<String> runnersFalhados = new ArrayList<>();
        int totalSucessos = 0;
        int totalFalhas = 0;
        
        try {
            // Submeter todas as tarefas para execuÃ§Ã£o paralela
            log.info("ðŸ”„ [1/2] Submetendo API GraphQL para execuÃ§Ã£o...");
            runnersFuturos.put("GraphQL", executor.submit(criarCallableRunner(() -> GraphQLRunner.executar(dataHoje))));
            
            log.info("ðŸ”„ [2/2] Submetendo API Data Export para execuÃ§Ã£o...");
            runnersFuturos.put("DataExport", executor.submit(criarCallableRunner(() -> DataExportRunner.executar(dataHoje))));
            
            log.info("â³ Aguardando conclusÃ£o de todos os runners...");
            
            // Aguardar a conclusÃ£o e tratar falhas individualmente
            // CentralizaÃ§Ã£o da lÃ³gica de sucesso/falha aqui
            // Iterar sobre Map.entrySet() para ter acesso simultÃ¢neo ao nome e ao Future
            for (final Map.Entry<String, Future<?>> entry : runnersFuturos.entrySet()) {
                final String nomeRunner = entry.getKey();
                final Future<?> futuro = entry.getValue();
                
                try {
                    // .get() Ã© bloqueante - espera a thread daquele runner terminar
                    futuro.get();
                    totalSucessos++;
                    log.info("âœ… API {} concluÃ­da com sucesso!", nomeRunner);
                } catch (final ExecutionException e) {
                    // Captura exceÃ§Ã£o, registra erro e continua
                    totalFalhas++;
                    runnersFalhados.add(nomeRunner);
                    
                    final Throwable causa = e.getCause();
                    final String mensagemErro = causa != null ? causa.getMessage() : e.getMessage();
                    log.error("âŒ FALHA NO RUNNER {}: {}. O fluxo continuarÃ¡.", nomeRunner, mensagemErro, e);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    totalFalhas++;
                    runnersFalhados.add(nomeRunner);
                    log.error("âŒ Thread interrompida para runner {}: {}", nomeRunner, e.getMessage(), e);
                }
            }
            
            // Resumo da execuÃ§Ã£o dos runners
            log.console("\n" + "=".repeat(60));
            log.info("ðŸ“Š RESUMO DA EXECUÃ‡ÃƒO DOS RUNNERS (APIs principais)");
            log.console("=".repeat(60));
            log.info("âœ… Runners bem-sucedidos: {}/2", totalSucessos);
            if (totalFalhas > 0) {
                log.warn("âŒ Runners com falha: {}/2 - {}", totalFalhas, String.join(", ", runnersFalhados));
            }
            log.console("=".repeat(60) + "\n");
            
        } finally {
            executor.shutdown();
            log.debug("ExecutorService encerrado");
        }
        
        if (incluirFaturasGraphQL) {
            // ========== FASE 3: EXTRAÃ‡ÃƒO DE FATURAS GRAPHQL POR ÃšLTIMO ==========
            // Motivo: O enriquecimento de faturas_graphql Ã© muito demorado (50+ minutos),
            // entÃ£o as outras entidades sÃ£o priorizadas para garantir dados parciais atualizados no BI.
            log.console("\n" + "=".repeat(60));
            log.info("ðŸ”„ [FASE 3] EXECUTANDO FATURAS GRAPHQL POR ÃšLTIMO");
            log.console("=".repeat(60));
            log.info("â„¹ï¸ Todas as outras entidades jÃ¡ foram extraÃ­das.");
            log.info("â„¹ï¸ Faturas GraphQL Ã© executado por Ãºltimo devido ao processo de enriquecimento demorado.");
            
            try {
                GraphQLRunner.executarFaturasGraphQLPorIntervalo(dataHoje, dataHoje);
                log.info("âœ… Faturas GraphQL concluÃ­das com sucesso!");
                totalSucessos++;
            } catch (final Exception e) {
                log.error("âŒ Falha na extraÃ§Ã£o de Faturas GraphQL: {}. Dados jÃ¡ extraÃ­dos das outras entidades foram preservados.", e.getMessage(), e);
                totalFalhas++;
                runnersFalhados.add("FaturasGraphQL");
            }
            log.console("=".repeat(60) + "\n");
        } else {
            log.console("\n" + "=".repeat(60));
            log.warn("âš ï¸ [FASE 3] FATURAS GRAPHQL DESABILITADO POR OPÃ‡ÃƒO DO OPERADOR");
            log.info("â„¹ï¸ Flag detectada: {}", FLAG_SEM_FATURAS_GRAPHQL);
            log.console("=".repeat(60) + "\n");
        }

        
        // ========== PASSO B: VALIDAÃ‡ÃƒO DE COMPLETUDE ==========
        log.console("\n" + "=".repeat(60));
        log.info("ðŸ” INICIANDO VALIDAÃ‡ÃƒO DE COMPLETUDE DOS DADOS");
        log.console("=".repeat(60));
        
        if (totalFalhas > 0) {
            log.warn("âš ï¸ ATENÃ‡ÃƒO: Runners falhados ({}) - validaÃ§Ã£o pode estar incompleta", String.join(", ", runnersFalhados));
        }
        
        try {
            final CompletudeValidator validator = new CompletudeValidator();
            
            final LocalDate dataReferencia = LocalDate.now();
            log.info("ðŸ”„ [1/2] Validando completude (contagem origem x destino) com base nos logs da execuÃ§Ã£o...");
            final Map<String, CompletudeValidator.StatusValidacao> resultadosValidacao =
                validator.validarCompletudePorLogs(dataReferencia);
            if (!incluirFaturasGraphQL) {
                resultadosValidacao.remove(ConstantesEntidades.FATURAS_GRAPHQL);
                log.info("â„¹ï¸ ValidaÃ§Ã£o de completude: {} foi desconsiderada por opÃ§Ã£o do operador.", ConstantesEntidades.FATURAS_GRAPHQL);
            }

            final boolean extracaoCompleta = resultadosValidacao.values().stream()
                .allMatch(status -> status == CompletudeValidator.StatusValidacao.OK);
            completudeEntidadesTotal = resultadosValidacao.size();
            completudeEntidadesNaoOk = (int) resultadosValidacao.values().stream()
                .filter(status -> status != CompletudeValidator.StatusValidacao.OK)
                .count();

            if (!extracaoCompleta) {
                resultadosValidacao.forEach((entidade, status) -> {
                    if (status != CompletudeValidator.StatusValidacao.OK) {
                        if (modoLoopDaemon) {
                            log.warn("INTEGRIDADE_ETL | resultado=ALERTA_LOOP | codigo=COMPLETUDE | entidade={} | status={}", entidade, status);
                        } else {
                            log.error("INTEGRIDADE_ETL | resultado=FALHA | codigo=COMPLETUDE | entidade={} | status={}", entidade, status);
                        }
                    }
                });
            }

            log.info("ðŸ”„ [2/2] Executando validaÃ§Ã£o estrita de integridade ETL...");
            final IntegridadeEtlValidator integridadeValidator = new IntegridadeEtlValidator();
            final Set<String> entidadesEsperadas = new LinkedHashSet<>(List.of(
                ConstantesEntidades.USUARIOS_SISTEMA,
                ConstantesEntidades.COLETAS,
                ConstantesEntidades.FRETES,
                ConstantesEntidades.MANIFESTOS,
                ConstantesEntidades.COTACOES,
                ConstantesEntidades.LOCALIZACAO_CARGAS,
                ConstantesEntidades.CONTAS_A_PAGAR,
                ConstantesEntidades.FATURAS_POR_CLIENTE,
                ConstantesEntidades.FATURAS_GRAPHQL
            ));
            if (!incluirFaturasGraphQL) {
                entidadesEsperadas.remove(ConstantesEntidades.FATURAS_GRAPHQL);
            }

            final IntegridadeEtlValidator.ResultadoValidacao resultadoIntegridade =
                integridadeValidator.validarExecucao(inicioExecucao, LocalDateTime.now(), entidadesEsperadas, modoLoopDaemon);

            if (!resultadoIntegridade.isValido()) {
                if (modoLoopDaemon) {
                    resultadoIntegridade.getFalhas().forEach(falha ->
                        log.warn("INTEGRIDADE_ETL | resultado=ALERTA_LOOP | detalhe={}", falha)
                    );
                } else {
                    resultadoIntegridade.getFalhas().forEach(falha ->
                        log.error("INTEGRIDADE_ETL | resultado=FALHA | detalhe={}", falha)
                    );
                }
            }
            integridadeFalhas = resultadoIntegridade.getFalhas().size();

            validacaoFinalCompleta = extracaoCompleta && resultadoIntegridade.isValido();

            log.console("\n" + "=".repeat(60));
            if (validacaoFinalCompleta) {
                log.info("ðŸŽ‰ EXTRAÃ‡ÃƒO 100% COMPLETA E VALIDADA!");
                log.info("âœ… Todos os dados foram extraÃ­dos com sucesso!");
            } else {
                detalheFalhaValidacao = "Validacao de integridade reprovada (completude/schema/chaves/referencial).";
                if (modoLoopDaemon) {
                    log.warn("EXTRACAO CONCLUIDA COM ALERTA DE INTEGRIDADE (modo loop daemon)");
                    log.warn("Carga nao foi interrompida; o loop seguira no proximo ciclo.");
                } else {
                    log.error("EXTRACAO COM PROBLEMAS - Verificar logs");
                    log.error("Carga interrompida por divergencia entre origem e destino.");
                }
            }
            log.console("=".repeat(60));
            
        } catch (final Exception e) {
            validacaoFinalCompleta = false;
            detalheFalhaValidacao = "Falha ao executar validaÃ§Ãµes finais: " + e.getMessage();
            log.error("âŒ Falha na validaÃ§Ã£o final de integridade: {}", e.getMessage());
            log.debug("Stack trace completo da falha na validaÃ§Ã£o:", e);
        }
            
        // Exibe resumo final
        final LocalDateTime fimExecucao = LocalDateTime.now();
        final long duracaoMinutos = java.time.Duration.between(inicioExecucao, fimExecucao).toMinutes();
        final long duracaoSegundos = java.time.Duration.between(inicioExecucao, fimExecucao).getSeconds();
        final boolean falhaSomenteValidacao = totalFalhas == 0 && !validacaoFinalCompleta;
        final String statusExecutivo = determinarStatusExecutivo(totalFalhas, validacaoFinalCompleta, modoLoopDaemon, falhaSomenteValidacao);

        log.info(
            "RESUMO_EXECUTIVO | status={} | inicio={} | fim={} | duracao_seg={} | duracao_min={} | runners_ok={} | runners_falha={} | validacao_final={} | completude_total={} | completude_nao_ok={} | integridade_falhas={} | modo_loop_daemon={} | faturas_graphql={}",
            statusExecutivo,
            FormatadorData.formatBR(inicioExecucao),
            FormatadorData.formatBR(fimExecucao),
            duracaoSegundos,
            duracaoMinutos,
            totalSucessos,
            totalFalhas,
            validacaoFinalCompleta,
            completudeEntidadesTotal,
            completudeEntidadesNaoOk,
            integridadeFalhas,
            modoLoopDaemon,
            incluirFaturasGraphQL
        );
        if (!runnersFalhados.isEmpty()) {
            log.warn("RESUMO_EXECUTIVO | runners_falhados={}", String.join(", ", runnersFalhados));
        }
        if (detalheFalhaValidacao != null && !detalheFalhaValidacao.isBlank()) {
            log.warn("RESUMO_EXECUTIVO | detalhe_validacao={}", detalheFalhaValidacao);
        }

        if (totalFalhas == 0 && validacaoFinalCompleta) {
            BannerUtil.exibirBannerSucesso();
            log.info("RESUMO DA EXTRACAO");
            log.info("Inicio: {} | Fim: {} | Duracao: {} minutos", 
                FormatadorData.formatBR(inicioExecucao), FormatadorData.formatBR(fimExecucao), duracaoMinutos);
            log.info("Todas as APIs foram processadas com sucesso.");
            gravarDataExecucao();
        } else if (modoLoopDaemon && falhaSomenteValidacao) {
            BannerUtil.exibirBannerSucesso();
            log.warn("RESUMO DA EXTRACAO (com alerta de integridade no loop)");
            log.info("Inicio: {} | Fim: {} | Duracao: {} minutos", 
                FormatadorData.formatBR(inicioExecucao), FormatadorData.formatBR(fimExecucao), duracaoMinutos);
            log.warn("Validacao final reprovada: {}", detalheFalhaValidacao != null ? detalheFalhaValidacao : "divergencia de integridade");
            log.info("Timestamp nao gravado devido a alerta de integridade (modo loop daemon)");
        } else {
            BannerUtil.exibirBannerErro();
            log.warn("ðŸ“Š RESUMO DA EXTRAÃ‡ÃƒO (com falhas)");
            log.info("InÃ­cio: {} | Fim: {} | DuraÃ§Ã£o: {} minutos", 
                FormatadorData.formatBR(inicioExecucao), FormatadorData.formatBR(fimExecucao), duracaoMinutos);
            if (totalFalhas > 0) {
                log.warn("âš ï¸ ExecuÃ§Ã£o com falhas parciais: {}/2 runners OK, falhados: {}", 
                    totalSucessos, String.join(", ", runnersFalhados));
            }
            if (!validacaoFinalCompleta) {
                log.error("âŒ ValidaÃ§Ã£o final reprovada: {}", detalheFalhaValidacao != null ? detalheFalhaValidacao : "divergÃªncia de integridade");
            }
            log.info("Timestamp nÃ£o gravado devido a falhas parciais");
            if (!validacaoFinalCompleta) {
                throw new RuntimeException(
                    "Fluxo completo interrompido por falha de integridade. " +
                    (detalheFalhaValidacao != null ? detalheFalhaValidacao : "Verifique os logs estruturados de validaÃ§Ã£o.")
                );
            }
            throw new PartialExecutionException(
                "Fluxo completo concluÃ­do com falhas parciais. Runners falhados: " + String.join(", ", runnersFalhados)
            );
        }
    }
    
    /**
     * Cria um Callable que executa uma tarefa que pode lanÃ§ar Exception.
     * A exceÃ§Ã£o serÃ¡ capturada pelo Future.get() no loop de verificaÃ§Ã£o.
     * 
     * @param tarefa Tarefa a ser executada (que pode lanÃ§ar Exception)
     * @return Callable que executa a tarefa
     */
    private Callable<Void> criarCallableRunner(final ExecutavelComExcecao tarefa) {
        return () -> {
            tarefa.executar();
            return null;
        };
    }
    
    /**
     * Interface funcional para tarefas que podem lanÃ§ar Exception.
     * Usada para simplificar a criaÃ§Ã£o de Callables.
     */
    @FunctionalInterface
    private interface ExecutavelComExcecao {
        void executar() throws Exception;
    }
    
    /**
     * Grava timestamp da execuÃ§Ã£o bem-sucedida.
     */
    private void gravarDataExecucao() {
        try {
            final Properties props = new Properties();
            props.setProperty(PROPRIEDADE_ULTIMO_RUN, LocalDateTime.now().toString());
            
            try (final FileOutputStream fos = new FileOutputStream(ARQUIVO_ULTIMO_RUN)) {
                props.store(fos, "Ãšltima execuÃ§Ã£o bem-sucedida do sistema de extraÃ§Ã£o");
            }
            
            log.debug("Timestamp de execuÃ§Ã£o gravado com sucesso");
        } catch (final IOException e) {
            log.warn("NÃ£o foi possÃ­vel gravar timestamp de execuÃ§Ã£o: {}", e.getMessage());
        }
    }

    private boolean possuiFlag(final String[] args, final String flag) {
        if (args == null || flag == null) {
            return false;
        }
        for (final String arg : args) {
            if (flag.equalsIgnoreCase(arg)) {
                return true;
            }
        }
        return false;
    }

    private String determinarStatusExecutivo(
        final int totalFalhasRunners,
        final boolean validacaoFinalCompleta,
        final boolean modoLoopDaemon,
        final boolean falhaSomenteValidacao
    ) {
        if (totalFalhasRunners == 0 && validacaoFinalCompleta) {
            return "SUCCESS";
        }
        if (modoLoopDaemon && falhaSomenteValidacao) {
            return "SUCCESS_WITH_ALERT";
        }
        if (totalFalhasRunners > 0 && validacaoFinalCompleta) {
            return "PARTIAL";
        }
        return "ERROR";
    }
}

