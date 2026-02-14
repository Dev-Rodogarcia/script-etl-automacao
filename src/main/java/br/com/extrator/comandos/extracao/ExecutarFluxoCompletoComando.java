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
 * Comando responsável por executar o fluxo completo de extração de dados
 * das 3 APIs do ESL Cloud (REST, GraphQL e DataExport).
 */
public class ExecutarFluxoCompletoComando implements Comando {
    // PROBLEMA #9 CORRIGIDO: Usar LoggerConsole para log duplo (arquivo + console)
    private static final LoggerConsole log = LoggerConsole.getLogger(ExecutarFluxoCompletoComando.class);
    private static final String FLAG_SEM_FATURAS_GRAPHQL = "--sem-faturas-graphql";
    
    // Constantes para gravação do timestamp de execução
    private static final String ARQUIVO_ULTIMO_RUN = "last_run.properties";
    private static final String PROPRIEDADE_ULTIMO_RUN = "last_successful_run";
    
    // Número de threads para execução paralela dos runners
    private static final int NUMERO_DE_THREADS = 2;
    
    @Override
    public void executar(final String[] args) throws Exception {
        final boolean incluirFaturasGraphQL = !possuiFlag(args, FLAG_SEM_FATURAS_GRAPHQL);

        // Exibe banner inicial de extração completa
        BannerUtil.exibirBannerExtracaoCompleta();
        
        // Define data de hoje para buscar dados do dia atual
        final LocalDate dataHoje = LocalDate.now();
        
        // PROBLEMA #9 CORRIGIDO: Usar LoggerConsole para log duplo
        log.info("Iniciando processo de extração de dados das 2 APIs do ESL Cloud");
        log.console("\n" + "=".repeat(60));
        log.console("INICIANDO PROCESSO DE EXTRAÇÃO DE DADOS");
        log.console("=".repeat(60));
        log.console("Modo: DADOS DE HOJE");
        log.console("Faturas GraphQL: {}", incluirFaturasGraphQL ? "INCLUIDO" : "DESABILITADO (flag --sem-faturas-graphql)");
        // PROBLEMA 13 CORRIGIDO: Usar FormatadorData em vez de criar formatters inline
        log.console("Data de extração: {} (dados de hoje)", FormatadorData.formatBR(dataHoje));
        log.console("Início: {}", FormatadorData.formatBR(LocalDateTime.now()));
        log.console("=".repeat(60) + "\n");
        
        final LocalDateTime inicioExecucao = LocalDateTime.now();
        boolean validacaoFinalCompleta = true;
        String detalheFalhaValidacao = null;
        
        // ========== EXECUÇÃO PARALELA DOS RUNNERS ==========
        log.info("🔄 Iniciando fluxo ETL em modo paralelo com {} threads", NUMERO_DE_THREADS);
        
        final ExecutorService executor = Executors.newFixedThreadPool(NUMERO_DE_THREADS);
        // Usar LinkedHashMap para manter ordem de inserção e associar explicitamente nome ao Future
        // Isso elimina o risco de desalinhamento entre ordem de submissão e nomes dos runners
        final Map<String, Future<?>> runnersFuturos = new LinkedHashMap<>();
        final List<String> runnersFalhados = new ArrayList<>();
        int totalSucessos = 0;
        int totalFalhas = 0;
        
        try {
            // Submeter todas as tarefas para execução paralela
            log.info("🔄 [1/2] Submetendo API GraphQL para execução...");
            runnersFuturos.put("GraphQL", executor.submit(criarCallableRunner(() -> GraphQLRunner.executar(dataHoje))));
            
            log.info("🔄 [2/2] Submetendo API Data Export para execução...");
            runnersFuturos.put("DataExport", executor.submit(criarCallableRunner(() -> DataExportRunner.executar(dataHoje))));
            
            log.info("⏳ Aguardando conclusão de todos os runners...");
            
            // Aguardar a conclusão e tratar falhas individualmente
            // Centralização da lógica de sucesso/falha aqui
            // Iterar sobre Map.entrySet() para ter acesso simultâneo ao nome e ao Future
            for (final Map.Entry<String, Future<?>> entry : runnersFuturos.entrySet()) {
                final String nomeRunner = entry.getKey();
                final Future<?> futuro = entry.getValue();
                
                try {
                    // .get() é bloqueante - espera a thread daquele runner terminar
                    futuro.get();
                    totalSucessos++;
                    log.info("✅ API {} concluída com sucesso!", nomeRunner);
                } catch (final ExecutionException e) {
                    // Captura exceção, registra erro e continua
                    totalFalhas++;
                    runnersFalhados.add(nomeRunner);
                    
                    final Throwable causa = e.getCause();
                    final String mensagemErro = causa != null ? causa.getMessage() : e.getMessage();
                    log.error("❌ FALHA NO RUNNER {}: {}. O fluxo continuará.", nomeRunner, mensagemErro, e);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    totalFalhas++;
                    runnersFalhados.add(nomeRunner);
                    log.error("❌ Thread interrompida para runner {}: {}", nomeRunner, e.getMessage(), e);
                }
            }
            
            // Resumo da execução dos runners
            log.console("\n" + "=".repeat(60));
            log.info("📊 RESUMO DA EXECUÇÃO DOS RUNNERS (APIs principais)");
            log.console("=".repeat(60));
            log.info("✅ Runners bem-sucedidos: {}/2", totalSucessos);
            if (totalFalhas > 0) {
                log.warn("❌ Runners com falha: {}/2 - {}", totalFalhas, String.join(", ", runnersFalhados));
            }
            log.console("=".repeat(60) + "\n");
            
        } finally {
            executor.shutdown();
            log.debug("ExecutorService encerrado");
        }
        
        if (incluirFaturasGraphQL) {
            // ========== FASE 3: EXTRAÇÃO DE FATURAS GRAPHQL POR ÚLTIMO ==========
            // Motivo: O enriquecimento de faturas_graphql é muito demorado (50+ minutos),
            // então as outras entidades são priorizadas para garantir dados parciais atualizados no BI.
            log.console("\n" + "=".repeat(60));
            log.info("🔄 [FASE 3] EXECUTANDO FATURAS GRAPHQL POR ÚLTIMO");
            log.console("=".repeat(60));
            log.info("ℹ️ Todas as outras entidades já foram extraídas.");
            log.info("ℹ️ Faturas GraphQL é executado por último devido ao processo de enriquecimento demorado.");
            
            try {
                GraphQLRunner.executarFaturasGraphQLPorIntervalo(dataHoje, dataHoje);
                log.info("✅ Faturas GraphQL concluídas com sucesso!");
                totalSucessos++;
            } catch (final Exception e) {
                log.error("❌ Falha na extração de Faturas GraphQL: {}. Dados já extraídos das outras entidades foram preservados.", e.getMessage(), e);
                totalFalhas++;
                runnersFalhados.add("FaturasGraphQL");
            }
            log.console("=".repeat(60) + "\n");
        } else {
            log.console("\n" + "=".repeat(60));
            log.warn("⚠️ [FASE 3] FATURAS GRAPHQL DESABILITADO POR OPÇÃO DO OPERADOR");
            log.info("ℹ️ Flag detectada: {}", FLAG_SEM_FATURAS_GRAPHQL);
            log.console("=".repeat(60) + "\n");
        }

        
        // ========== PASSO B: VALIDAÇÃO DE COMPLETUDE ==========
        log.console("\n" + "=".repeat(60));
        log.info("🔍 INICIANDO VALIDAÇÃO DE COMPLETUDE DOS DADOS");
        log.console("=".repeat(60));
        
        if (totalFalhas > 0) {
            log.warn("⚠️ ATENÇÃO: Runners falhados ({}) - validação pode estar incompleta", String.join(", ", runnersFalhados));
        }
        
        try {
            final CompletudeValidator validator = new CompletudeValidator();
            
            final LocalDate dataReferencia = LocalDate.now();
            log.info("🔄 [1/2] Validando completude (contagem origem x destino) com base nos logs da execução...");
            final Map<String, CompletudeValidator.StatusValidacao> resultadosValidacao =
                validator.validarCompletudePorLogs(dataReferencia);
            if (!incluirFaturasGraphQL) {
                resultadosValidacao.remove(ConstantesEntidades.FATURAS_GRAPHQL);
                log.info("ℹ️ Validação de completude: {} foi desconsiderada por opção do operador.", ConstantesEntidades.FATURAS_GRAPHQL);
            }

            final boolean extracaoCompleta = resultadosValidacao.values().stream()
                .allMatch(status -> status == CompletudeValidator.StatusValidacao.OK);

            if (!extracaoCompleta) {
                resultadosValidacao.forEach((entidade, status) -> {
                    if (status != CompletudeValidator.StatusValidacao.OK) {
                        log.error("INTEGRIDADE_ETL | resultado=FALHA | codigo=COMPLETUDE | entidade={} | status={}", entidade, status);
                    }
                });
            }

            log.info("🔄 [2/2] Executando validação estrita de integridade ETL...");
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
                integridadeValidator.validarExecucao(inicioExecucao, LocalDateTime.now(), entidadesEsperadas);

            if (!resultadoIntegridade.isValido()) {
                resultadoIntegridade.getFalhas().forEach(falha ->
                    log.error("INTEGRIDADE_ETL | resultado=FALHA | detalhe={}", falha)
                );
            }

            validacaoFinalCompleta = extracaoCompleta && resultadoIntegridade.isValido();

            log.console("\n" + "=".repeat(60));
            if (validacaoFinalCompleta) {
                log.info("🎉 EXTRAÇÃO 100% COMPLETA E VALIDADA!");
                log.info("✅ Todos os dados foram extraídos com sucesso!");
            } else {
                detalheFalhaValidacao = "Validação de integridade reprovada (completude/schema/chaves/referencial).";
                log.error("❌ EXTRAÇÃO COM PROBLEMAS - Verificar logs");
                log.error("❌ Carga interrompida por divergência entre origem e destino.");
            }
            log.console("=".repeat(60));
            
        } catch (final Exception e) {
            validacaoFinalCompleta = false;
            detalheFalhaValidacao = "Falha ao executar validações finais: " + e.getMessage();
            log.error("❌ Falha na validação final de integridade: {}", e.getMessage());
            log.debug("Stack trace completo da falha na validação:", e);
        }
            
        // Exibe resumo final
        final LocalDateTime fimExecucao = LocalDateTime.now();
        final long duracaoMinutos = java.time.Duration.between(inicioExecucao, fimExecucao).toMinutes();
        
        if (totalFalhas == 0 && validacaoFinalCompleta) {
            BannerUtil.exibirBannerSucesso();
            log.info("📊 RESUMO DA EXTRAÇÃO");
            log.info("Início: {} | Fim: {} | Duração: {} minutos", 
                FormatadorData.formatBR(inicioExecucao), FormatadorData.formatBR(fimExecucao), duracaoMinutos);
            log.info("✅ Todas as APIs foram processadas com sucesso!");
            gravarDataExecucao();
        } else {
            BannerUtil.exibirBannerErro();
            log.warn("📊 RESUMO DA EXTRAÇÃO (com falhas)");
            log.info("Início: {} | Fim: {} | Duração: {} minutos", 
                FormatadorData.formatBR(inicioExecucao), FormatadorData.formatBR(fimExecucao), duracaoMinutos);
            if (totalFalhas > 0) {
                log.warn("⚠️ Execução com falhas parciais: {}/2 runners OK, falhados: {}", 
                    totalSucessos, String.join(", ", runnersFalhados));
            }
            if (!validacaoFinalCompleta) {
                log.error("❌ Validação final reprovada: {}", detalheFalhaValidacao != null ? detalheFalhaValidacao : "divergência de integridade");
            }
            log.info("Timestamp não gravado devido a falhas parciais");
            if (!validacaoFinalCompleta) {
                throw new RuntimeException(
                    "Fluxo completo interrompido por falha de integridade. " +
                    (detalheFalhaValidacao != null ? detalheFalhaValidacao : "Verifique os logs estruturados de validação.")
                );
            }
            throw new PartialExecutionException(
                "Fluxo completo concluído com falhas parciais. Runners falhados: " + String.join(", ", runnersFalhados)
            );
        }
    }
    
    /**
     * Cria um Callable que executa uma tarefa que pode lançar Exception.
     * A exceção será capturada pelo Future.get() no loop de verificação.
     * 
     * @param tarefa Tarefa a ser executada (que pode lançar Exception)
     * @return Callable que executa a tarefa
     */
    private Callable<Void> criarCallableRunner(final ExecutavelComExcecao tarefa) {
        return () -> {
            tarefa.executar();
            return null;
        };
    }
    
    /**
     * Interface funcional para tarefas que podem lançar Exception.
     * Usada para simplificar a criação de Callables.
     */
    @FunctionalInterface
    private interface ExecutavelComExcecao {
        void executar() throws Exception;
    }
    
    /**
     * Grava timestamp da execução bem-sucedida.
     */
    private void gravarDataExecucao() {
        try {
            final Properties props = new Properties();
            props.setProperty(PROPRIEDADE_ULTIMO_RUN, LocalDateTime.now().toString());
            
            try (final FileOutputStream fos = new FileOutputStream(ARQUIVO_ULTIMO_RUN)) {
                props.store(fos, "Última execução bem-sucedida do sistema de extração");
            }
            
            log.debug("Timestamp de execução gravado com sucesso");
        } catch (final IOException e) {
            log.warn("Não foi possível gravar timestamp de execução: {}", e.getMessage());
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
}
