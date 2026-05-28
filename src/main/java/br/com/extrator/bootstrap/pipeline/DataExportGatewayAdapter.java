/* ==[DOC-FILE]===============================================================
Arquivo : src/main/java/br/com/extrator/bootstrap/pipeline/DataExportGatewayAdapter.java
Classe  : DataExportGatewayAdapter (class)
Pacote  : br.com.extrator.bootstrap.pipeline
Modulo  : Bootstrap - Wiring

Papel   : Adapter que implementa DataExportGateway, invocando o DataExportExtractionService
          e retornando um StepExecutionResult padronizado para o pipeline.

Conecta com:
- DataExportGateway (aplicacao.portas) — interface de porta implementada
- DataExportExtractionService (integracao.dataexport.services) — servico de extracao DataExport
- StepExecutionResult (aplicacao.pipeline.runtime) — resultado padronizado de step
- StepStatus (aplicacao.pipeline.runtime) — enumeracao de status de step

Fluxo geral:
1) Recebe dataInicio, dataFim e nome de entidade (pode ser null/"all").
2) Normaliza o filtro de entidade (null significa todas as entidades).
3) Instancia DataExportExtractionService e invoca executar().
4) Constroi e retorna StepExecutionResult com status SUCCESS.

Estrutura interna:
Metodos principais:
- executar(dataInicio, dataFim, entidade): executa extracao DataExport e retorna resultado.
- normalizeEntityFilter(entidade): normaliza o nome da entidade para null quando representa "todas".
[DOC-FILE-END]============================================================== */
package br.com.extrator.bootstrap.pipeline;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import br.com.extrator.integracao.dataexport.services.DataExportExtractionService;
import br.com.extrator.integracao.dataexport.services.DataExportExtractionService.ExecutionStatus;
import br.com.extrator.integracao.dataexport.services.DataExportExtractionService.ExecutionSummary;
import br.com.extrator.aplicacao.portas.DataExportGateway;
import br.com.extrator.aplicacao.pipeline.runtime.StepExecutionResult;
import br.com.extrator.aplicacao.pipeline.runtime.StepStatus;
import br.com.extrator.bootstrap.pipeline.IsolatedStepProcessExecutor.ApiType;
import br.com.extrator.suporte.configuracao.ConfigEtl;
import br.com.extrator.suporte.observabilidade.ExecutionContext;

public final class DataExportGatewayAdapter implements DataExportGateway {
    private static final String ISOLATED_RESULT_PREFIX = "ISOLATED_STEP_RESULT api=dataexport";

    private final DataExportExtractionService service;
    private final IsolatedStepProcessExecutor isolatedExecutor;

    public DataExportGatewayAdapter() {
        this(new DataExportExtractionService(), new IsolatedStepProcessExecutor());
    }

    DataExportGatewayAdapter(final DataExportExtractionService service,
                             final IsolatedStepProcessExecutor isolatedExecutor) {
        this.service = service;
        this.isolatedExecutor = isolatedExecutor;
    }

    @Override
    public StepExecutionResult executar(final LocalDate dataInicio, final LocalDate dataFim, final String entidade) throws Exception {
        final LocalDateTime inicio = LocalDateTime.now();
        final String filtroEntidade = normalizeEntityFilter(entidade);
        final boolean forcarIsolamentoNoDaemon = ExecutionContext.isLoopDaemonCommand();
        final boolean usarIsolamento = !ConfigEtl.isProcessoFilhoIsolado()
            && (ConfigEtl.isIsolamentoProcessoAtivo() || forcarIsolamentoNoDaemon);
        final Long childPid;
        final Path childLogFile;
        final DataExportStepSummary resumo;
        if (usarIsolamento) {
            final IsolatedStepProcessExecutor.ProcessExecutionResult processResult =
                isolatedExecutor.executar(ApiType.DATAEXPORT, dataInicio, dataFim, filtroEntidade, resolverTimeoutIsolado(filtroEntidade));
            childPid = processResult.pid();
            childLogFile = processResult.logFile();
            resumo = lerResumoProcessoIsolado(childLogFile)
                .orElseGet(DataExportStepSummary::success);
        } else {
            resumo = DataExportStepSummary.from(service.executar(dataInicio, dataFim, filtroEntidade));
            childPid = null;
            childLogFile = null;
        }
        final String entidadeExecucao = filtroEntidade == null ? "dataexport" : filtroEntidade;
        final boolean parcial = resumo.isPartialSuccess();
        return StepExecutionResult.builder("dataexport:" + entidadeExecucao, entidadeExecucao)
            .status(parcial ? StepStatus.DEGRADED : StepStatus.SUCCESS)
            .startedAt(inicio)
            .finishedAt(LocalDateTime.now())
            .message(criarMensagemResultado(resumo))
            .metadata("source", "dataexport")
            .metadata("execution_status", resumo.status())
            .metadata("failure_mode", parcial ? "DEGRADED" : null)
            .metadata("failed_entities", resumo.failedEntities())
            .metadata("execution_mode", usarIsolamento ? "isolated_process" : "in_process")
            .metadata("forced_by_daemon", forcarIsolamentoNoDaemon)
            .metadata("child_pid", childPid)
            .metadata("child_log_file", childLogFile == null ? null : childLogFile.toString())
            .build();
    }

    private java.time.Duration resolverTimeoutIsolado(final String filtroEntidade) {
        if (filtroEntidade == null || filtroEntidade.isBlank()) {
            return ConfigEtl.obterTimeoutStepDataExport();
        }
        return ConfigEtl.obterTimeoutEntidadeDataExport(filtroEntidade);
    }

    private String criarMensagemResultado(final DataExportStepSummary resumo) {
        if (resumo.isPartialSuccess()) {
            return "DataExport concluido com status PARTIAL_SUCCESS; falhas: "
                + String.join(", ", resumo.failedEntities());
        }
        return "DataExport executado com sucesso";
    }

    private Optional<DataExportStepSummary> lerResumoProcessoIsolado(final Path childLogFile) {
        if (childLogFile == null || !Files.exists(childLogFile)) {
            return Optional.empty();
        }
        try {
            final List<String> linhas = Files.readAllLines(childLogFile, StandardCharsets.UTF_8);
            for (int i = linhas.size() - 1; i >= 0; i--) {
                final String linha = linhas.get(i);
                if (linha != null && linha.contains(ISOLATED_RESULT_PREFIX)) {
                    return Optional.of(parseResumoIsolado(linha));
                }
            }
        } catch (final IOException ignored) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    private DataExportStepSummary parseResumoIsolado(final String linha) {
        final String status = extrairValor(linha, "status").orElse(ExecutionStatus.SUCCESS.name());
        final List<String> entidadesFalhas = extrairValor(linha, "failed_entities")
            .map(valor -> valor.isBlank()
                ? List.<String>of()
                : List.of(valor.split(",")).stream()
                    .map(String::trim)
                    .filter(item -> !item.isBlank())
                    .toList())
            .orElse(List.of());
        return new DataExportStepSummary(status, entidadesFalhas);
    }

    private Optional<String> extrairValor(final String linha, final String chave) {
        final String marcador = chave + "=";
        final int inicio = linha.indexOf(marcador);
        if (inicio < 0) {
            return Optional.empty();
        }
        final int valorInicio = inicio + marcador.length();
        final int valorFim = linha.indexOf(' ', valorInicio);
        return Optional.of((valorFim < 0 ? linha.substring(valorInicio) : linha.substring(valorInicio, valorFim)).trim());
    }

    private String normalizeEntityFilter(final String entidade) {
        if (entidade == null) {
            return null;
        }
        final String normalizada = entidade.trim().toLowerCase(Locale.ROOT);
        if (normalizada.isBlank()
            || "all".equals(normalizada)
            || "todas".equals(normalizada)
            || "dataexport".equals(normalizada)) {
            return null;
        }
        return normalizada;
    }

    private record DataExportStepSummary(String status, List<String> failedEntities) {
        static DataExportStepSummary from(final ExecutionSummary summary) {
            if (summary == null) {
                return success();
            }
            return new DataExportStepSummary(summary.status().name(), List.copyOf(summary.entidadesComFalha()));
        }

        static DataExportStepSummary success() {
            return new DataExportStepSummary(ExecutionStatus.SUCCESS.name(), List.of());
        }

        boolean isPartialSuccess() {
            return ExecutionStatus.PARTIAL_SUCCESS.name().equals(status);
        }
    }
}
