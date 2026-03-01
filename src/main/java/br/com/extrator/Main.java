/* ==[DOC-FILE]===============================================================
Arquivo : src/main/java/br/com/extrator/Main.java
Classe  : Main (class)
Pacote  : br.com.extrator
Modulo  : Orquestrador principal
Papel   : Orquestra a execucao da aplicacao e despacho dos comandos CLI.

Conecta com:
- ExecutionAuditor (auditoria.execucao)
- CommandRegistry (comandos)
- Comando (comandos.base)
- ExibirAjudaComando (comandos.console)
- PartialExecutionException (comandos.extracao)
- ExecutionHistoryRepository (db.repository)
- LoggingService (servicos)

Fluxo geral:
1) Interpreta argumentos e seleciona comando de execucao.
2) Coordena log operacional e persistencia de historico.
3) Centraliza tratamento de erros e codigo de saida.

Estrutura interna:
Metodos principais:
- main(...1 args): ponto de entrada da execucao.
- organizarLogsTxtNaPastaLogs(): realiza operacao relacionada a "organizar logs txt na pasta logs".
- isComandoLongaDuracao(...1 args): retorna estado booleano de controle.
- isComandoSilencioso(...1 args): retorna estado booleano de controle.
- isErroEsperado(...1 args): retorna estado booleano de controle.
Atributos-chave:
- logger: logger da classe para diagnostico.
- COMANDOS: campo de estado para "comandos".
[DOC-FILE-END]============================================================== */

package br.com.extrator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.extrator.auditoria.execucao.ExecutionAuditor;
import br.com.extrator.comandos.CommandRegistry;
import br.com.extrator.comandos.base.Comando;
import br.com.extrator.comandos.console.ExibirAjudaComando;
import br.com.extrator.comandos.extracao.PartialExecutionException;
import br.com.extrator.db.repository.ExecutionHistoryRepository;
import br.com.extrator.servicos.LoggingService;
import br.com.extrator.util.tempo.RelogioSistema;

/**
 * Orquestrador principal dos comandos do extrator.
 */
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final int HISTORICO_MAX_TENTATIVAS = 2;
    private static final long HISTORICO_RETRY_DELAY_MS = 750L;
    private static final Path ARQUIVO_FALLBACK_HISTORICO = Path.of("logs", "execution_history_fallback.ndjson");

    private static final Map<String, Comando> COMANDOS = CommandRegistry.criarMapaComandos();

    public static void main(final String[] args) {
        final String nomeComando = (args.length == 0) ? "--fluxo-completo" : args[0].toLowerCase();
        final String tipoExecucao = nomeComando.startsWith("--") ? nomeComando.substring(2) : nomeComando;

        final boolean comandoLongaDuracao = isComandoLongaDuracao(nomeComando);
        final boolean comandoSilencioso = isComandoSilencioso(nomeComando);
        final boolean capturarLogOperacao = !comandoLongaDuracao && !comandoSilencioso;
        final boolean registrarHistorico = !comandoLongaDuracao && !comandoSilencioso;

        final LoggingService loggingService = capturarLogOperacao ? new LoggingService() : null;
        final LocalDateTime inicioExecucao = RelogioSistema.agora();
        final AtomicReference<String> statusRef = new AtomicReference<>("SUCCESS");
        final AtomicReference<String> errorMessageRef = new AtomicReference<>(null);
        final AtomicReference<String> errorCategoryRef = new AtomicReference<>(null);
        final AtomicBoolean historicoRegistrado = new AtomicBoolean(false);
        final AtomicBoolean finalizadoNormalmente = new AtomicBoolean(false);

        final Runnable persistirHistoricoExecucao = () -> {
            if (!registrarHistorico || !historicoRegistrado.compareAndSet(false, true)) {
                return;
            }

            final LocalDateTime fimExecucao = RelogioSistema.agora();
            final long duracaoSegundos = Duration.between(inicioExecucao, fimExecucao).getSeconds();
            final int duracaoSegundosInt = (int) Math.min(Integer.MAX_VALUE, Math.max(0L, duracaoSegundos));
            int totalRecords = 0;

            try {
                final ExecutionHistoryRepository repo = new ExecutionHistoryRepository();
                totalRecords = repo.calcularTotalRegistros(inicioExecucao, fimExecucao);
            } catch (final RuntimeException t) {
                logger.warn("Falha ao calcular total de registros: {}", t.getMessage());
            }

            ExecutionAuditor.registrarCsv(
                fimExecucao,
                statusRef.get(),
                duracaoSegundosInt,
                totalRecords,
                tipoExecucao,
                errorMessageRef.get()
            );

            RuntimeException ultimoErroPersistencia = null;
            boolean historicoPersistidoNoBanco = false;

            for (int tentativa = 1; tentativa <= HISTORICO_MAX_TENTATIVAS; tentativa++) {
                try {
                    final ExecutionHistoryRepository repo = new ExecutionHistoryRepository();
                    repo.inserirHistorico(
                        inicioExecucao,
                        fimExecucao,
                        duracaoSegundosInt,
                        statusRef.get(),
                        totalRecords,
                        errorCategoryRef.get(),
                        errorMessageRef.get()
                    );
                    logger.info(
                        "EVT_EXEC_HISTORY_DB_SAVE_OK tentativa={} status={} tipo_execucao={} inicio={} fim={} total_records={}",
                        tentativa,
                        statusRef.get(),
                        tipoExecucao,
                        inicioExecucao,
                        fimExecucao,
                        totalRecords
                    );
                    historicoPersistidoNoBanco = true;
                    break;
                } catch (final RuntimeException t) {
                    ultimoErroPersistencia = t;
                    logger.warn(
                        "EVT_EXEC_HISTORY_DB_SAVE_FAIL tentativa={} status={} tipo_execucao={} erro={}",
                        tentativa,
                        statusRef.get(),
                        tipoExecucao,
                        t.getMessage()
                    );

                    if (tentativa < HISTORICO_MAX_TENTATIVAS) {
                        try {
                            Thread.sleep(HISTORICO_RETRY_DELAY_MS);
                        } catch (final InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            logger.warn("Retry de persistencia de historico interrompido: {}", ie.getMessage());
                            break;
                        }
                    }
                }
            }

            if (!historicoPersistidoNoBanco) {
                registrarFallbackHistorico(
                    inicioExecucao,
                    fimExecucao,
                    duracaoSegundosInt,
                    statusRef.get(),
                    totalRecords,
                    tipoExecucao,
                    errorCategoryRef.get(),
                    errorMessageRef.get(),
                    ultimoErroPersistencia
                );
            }
        };

        if (loggingService != null) {
            loggingService.iniciarCaptura("extracao_dados");
            Runtime.getRuntime().addShutdownHook(new Thread(() -> loggingService.pararCaptura(statusRef.get())));
        }

        if (registrarHistorico) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (!finalizadoNormalmente.get()) {
                    statusRef.compareAndSet("SUCCESS", "INTERRUPTED");
                    if (errorCategoryRef.get() == null) {
                        errorCategoryRef.set("ShutdownHook");
                    }
                    if (errorMessageRef.get() == null) {
                        errorMessageRef.set("Execucao encerrada antes da finalizacao completa");
                    }
                }
                persistirHistoricoExecucao.run();
            }, "execution-history-shutdown-hook"));
        }

        if (capturarLogOperacao) {
            organizarLogsTxtNaPastaLogs();
        }

        int exitCode = 0;

        try {
            final Comando comando = COMANDOS.getOrDefault(nomeComando, new ExibirAjudaComando());
            if (!COMANDOS.containsKey(nomeComando)) {
                System.err.println("Comando desconhecido: " + nomeComando);
                System.err.println("Use --ajuda para ver os comandos disponiveis.");
            }
            comando.executar(args);
        } catch (final PartialExecutionException e) {
            statusRef.set("PARTIAL");
            errorMessageRef.set(e.getMessage());
            errorCategoryRef.set(e.getClass().getSimpleName());
            exitCode = 2;
            logger.warn("Execucao concluida com falhas parciais: {}", e.getMessage());
            System.err.println("Execucao parcial: " + e.getMessage());
        } catch (final Error e) {
            logger.error("Erro irrecuperavel durante execucao: {}", e.getMessage(), e);
            throw e;
        } catch (final Exception e) {
            final String mensagem = (e.getMessage() == null || e.getMessage().isBlank())
                ? e.getClass().getSimpleName()
                : e.getMessage();
            statusRef.set("ERROR");
            errorMessageRef.set(mensagem);
            errorCategoryRef.set(e.getClass().getSimpleName());
            exitCode = 1;
            if (!(comandoSilencioso && isErroEsperado(e))) {
                logger.error("Erro durante execucao: {}", mensagem, e);
            }
            System.err.println("Erro durante execucao: " + mensagem);
        } finally {
            persistirHistoricoExecucao.run();
            finalizadoNormalmente.set(true);

            if (loggingService != null) {
                loggingService.pararCaptura(statusRef.get());
            }
        }

        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    private static void organizarLogsTxtNaPastaLogs() {
        LoggingService.organizarLogsTxtNaPastaLogs();
    }

    private static boolean isComandoLongaDuracao(final String nomeComando) {
        return "--loop".equals(nomeComando) || "--loop-daemon-run".equals(nomeComando);
    }

    private static boolean isComandoSilencioso(final String nomeComando) {
        return nomeComando.startsWith("--auth-")
            || "--ajuda".equals(nomeComando)
            || "--help".equals(nomeComando)
            || "--auditar-api".equals(nomeComando)
            || "--loop-daemon-start".equals(nomeComando)
            || "--loop-daemon-stop".equals(nomeComando)
            || "--loop-daemon-status".equals(nomeComando);
    }

    private static boolean isErroEsperado(final Exception e) {
        return e instanceof IllegalArgumentException || e instanceof IllegalStateException;
    }

    private static void registrarFallbackHistorico(final LocalDateTime inicioExecucao,
                                                   final LocalDateTime fimExecucao,
                                                   final int duracaoSegundos,
                                                   final String status,
                                                   final int totalRecords,
                                                   final String tipoExecucao,
                                                   final String categoriaErro,
                                                   final String mensagemErro,
                                                   final RuntimeException erroPersistencia) {
        final String linhaJson = "{"
            + "\"event_code\":\"EVT_EXEC_HISTORY_DB_FALLBACK\","
            + "\"timestamp\":\"" + escapeJson(RelogioSistema.agora().toString()) + "\","
            + "\"inicio_execucao\":\"" + escapeJson(String.valueOf(inicioExecucao)) + "\","
            + "\"fim_execucao\":\"" + escapeJson(String.valueOf(fimExecucao)) + "\","
            + "\"duracao_segundos\":" + duracaoSegundos + ","
            + "\"status\":\"" + escapeJson(status) + "\","
            + "\"total_records\":" + totalRecords + ","
            + "\"tipo_execucao\":\"" + escapeJson(tipoExecucao) + "\","
            + "\"categoria_erro\":\"" + escapeJson(categoriaErro) + "\","
            + "\"mensagem_erro\":\"" + escapeJson(mensagemErro) + "\","
            + "\"erro_persistencia\":\"" + escapeJson(erroPersistencia == null ? null : erroPersistencia.getMessage()) + "\""
            + "}"
            + System.lineSeparator();

        try {
            final Path diretorio = ARQUIVO_FALLBACK_HISTORICO.getParent();
            if (diretorio != null) {
                Files.createDirectories(diretorio);
            }
            Files.writeString(
                ARQUIVO_FALLBACK_HISTORICO,
                linhaJson,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
            );
            logger.error(
                "EVT_EXEC_HISTORY_DB_FALLBACK_WRITTEN arquivo={} status={} tipo_execucao={} erro={}",
                ARQUIVO_FALLBACK_HISTORICO.toAbsolutePath(),
                status,
                tipoExecucao,
                erroPersistencia == null ? "<desconhecido>" : erroPersistencia.getMessage()
            );
        } catch (final IOException io) {
            logger.error(
                "EVT_EXEC_HISTORY_DB_FALLBACK_WRITE_FAIL arquivo={} erro_original={} erro_fallback={}",
                ARQUIVO_FALLBACK_HISTORICO.toAbsolutePath(),
                erroPersistencia == null ? "<desconhecido>" : erroPersistencia.getMessage(),
                io.getMessage(),
                io
            );
        }
    }

    private static String escapeJson(final String valor) {
        if (valor == null) {
            return "";
        }
        return valor
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\r", "\\r")
            .replace("\n", "\\n");
    }
}
