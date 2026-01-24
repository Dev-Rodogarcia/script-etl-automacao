package br.com.extrator.comandos.extracao;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import br.com.extrator.auditoria.servicos.CompletudeValidator;
import br.com.extrator.comandos.base.Comando;
import br.com.extrator.util.console.LoggerConsole;
import br.com.extrator.runners.dataexport.DataExportRunner;
import br.com.extrator.runners.graphql.GraphQLRunner;
import br.com.extrator.util.console.BannerUtil;
import br.com.extrator.util.formatacao.FormatadorData;

/**
 * Comando responsável por executar o fluxo completo de extração de dados
 * das 3 APIs do ESL Cloud (REST, GraphQL e DataExport).
 */
public class ExecutarFluxoCompletoComando implements Comando {
    // PROBLEMA #9 CORRIGIDO: Usar LoggerConsole para log duplo (arquivo + console)
    private static final LoggerConsole log = LoggerConsole.getLogger(ExecutarFluxoCompletoComando.class);
    
    // Constantes para gravação do timestamp de execução
    private static final String ARQUIVO_ULTIMO_RUN = "last_run.properties";
    private static final String PROPRIEDADE_ULTIMO_RUN = "last_successful_run";
    
    // Número de threads para execução paralela dos runners
    private static final int NUMERO_DE_THREADS = 2;
    
    @Override
    public void executar(final String[] args) throws Exception {
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
        // PROBLEMA 13 CORRIGIDO: Usar FormatadorData em vez de criar formatters inline
        log.console("Data de extração: {} (dados de hoje)", FormatadorData.formatBR(dataHoje));
        log.console("Início: {}", FormatadorData.formatBR(LocalDateTime.now()));
        log.console("=".repeat(60) + "\n");
        
        final LocalDateTime inicioExecucao = LocalDateTime.now();
        
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

        
        // ========== PASSO B: VALIDAÇÃO DE COMPLETUDE ==========
        log.console("\n" + "=".repeat(60));
        log.info("🔍 INICIANDO VALIDAÇÃO DE COMPLETUDE DOS DADOS");
        log.console("=".repeat(60));
        
        if (totalFalhas > 0) {
            log.warn("⚠️ ATENÇÃO: Runners falhados ({}) - validação pode estar incompleta", String.join(", ", runnersFalhados));
        }
        
        try {
            final CompletudeValidator validator = new CompletudeValidator();
            
            log.info("🔄 [1/2] Buscando totais nas APIs do ESL Cloud...");
            final LocalDate dataReferencia = LocalDate.now();
            final Optional<Map<String, Integer>> totaisEslCloudOpt = validator.buscarTotaisEslCloud(dataReferencia);
            
            if (totaisEslCloudOpt.isPresent()) {
                final Map<String, Integer> totaisEslCloud = totaisEslCloudOpt.get();
                log.info("✅ Totais obtidos das APIs com sucesso!");
                
                log.info("🔄 [2/2] Validando completude dos dados extraídos...");
                final Map<String, CompletudeValidator.StatusValidacao> resultadosValidacao = validator.validarCompletude(totaisEslCloud, dataReferencia);
                
                final boolean extracaoCompleta = resultadosValidacao.values().stream()
                    .allMatch(status -> status == CompletudeValidator.StatusValidacao.OK);
                
                boolean gapValidationOk = true;
                boolean temporalValidationOk = true;
                
                if (extracaoCompleta) {
                    log.info("🔍 [3/4] Executando validação de gaps (IDs sequenciais)...");
                    final CompletudeValidator.StatusValidacao gapStatus = validator.validarGapsOcorrencias(dataReferencia);
                    gapValidationOk = (gapStatus == CompletudeValidator.StatusValidacao.OK);
                    
                    if (gapValidationOk) {
                        log.info("✅ Validação de gaps: OK");
                    } else {
                        log.warn("⚠️ Validação de gaps detectou problemas: {}", gapStatus);
                    }
                    
                    log.info("🕐 [4/4] Executando validação de janela temporal...");
                    final Map<String, CompletudeValidator.StatusValidacao> temporalResults = validator.validarJanelaTemporal(dataReferencia);
                    temporalValidationOk = temporalResults.values().stream()
                        .allMatch(status -> status == CompletudeValidator.StatusValidacao.OK);
                    
                    if (temporalValidationOk) {
                        log.info("✅ Validação temporal: OK");
                    } else {
                        log.error("❌ Validação temporal detectou registros criados durante extração!");
                    }
                }
                
                final boolean validacaoFinalCompleta = extracaoCompleta && gapValidationOk && temporalValidationOk;
                
                log.console("\n" + "=".repeat(60));
                if (validacaoFinalCompleta) {
                    log.info("🎉 EXTRAÇÃO 100% COMPLETA E VALIDADA!");
                    log.info("✅ Todos os dados foram extraídos com sucesso!");
                } else {
                    log.error("❌ EXTRAÇÃO COM PROBLEMAS - Verificar logs");
                    if (!extracaoCompleta) {
                        log.warn("⚠️ Inconsistências na contagem de registros detectadas.");
                    }
                    if (!gapValidationOk) {
                        log.warn("⚠️ Gaps nos IDs detectados - possível perda de registros.");
                    }
                    if (!temporalValidationOk) {
                        log.error("❌ CRÍTICO: Registros criados durante extração!");
                    }
                }
                log.console("=".repeat(60));
            } else {
                log.info("ℹ️ Continuando sem validação de completude (API indisponível)");
            }
            
        } catch (final Exception e) {
            log.warn("⚠️ Validação de completude falhou: {} - dados foram salvos", e.getMessage());
            log.debug("Stack trace completo da falha na validação:", e);
        }
            
        // Exibe resumo final
        final LocalDateTime fimExecucao = LocalDateTime.now();
        final long duracaoMinutos = java.time.Duration.between(inicioExecucao, fimExecucao).toMinutes();
        
        if (totalFalhas == 0) {
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
            log.warn("⚠️ Execução com falhas parciais: {}/2 runners OK, falhados: {}", 
                totalSucessos, String.join(", ", runnersFalhados));
            log.info("Timestamp não gravado devido a falhas parciais");
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
}
