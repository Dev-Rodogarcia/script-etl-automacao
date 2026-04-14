package br.com.extrator.comandos.cli.interno;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

import br.com.extrator.bootstrap.pipeline.IsolatedStepProcessExecutor.ApiType;
import br.com.extrator.bootstrap.pipeline.IsolatedStepProcessExecutor.FaultMode;
import br.com.extrator.comandos.cli.base.Comando;
import br.com.extrator.integracao.dataexport.services.DataExportExtractionService;
import br.com.extrator.integracao.graphql.services.GraphQLExtractionService;
import br.com.extrator.suporte.banco.SqlServerExecutionLockManager;
import br.com.extrator.suporte.configuracao.ConfigEtl;

public class ExecutarStepIsoladoComando implements Comando {
    private static final String EXECUTION_LOCK_RESOURCE = "etl-global-execution";

    @Override
    public void executar(final String[] args) throws Exception {
        if (args == null || args.length < 5) {
            throw new IllegalArgumentException(
                "Uso: --executar-step-isolado <graphql|dataexport> <yyyy-mm-dd> <yyyy-mm-dd> <entidade|all> [--fault modo]"
            );
        }
        validarOrigemDaExecucao();

        final ApiType apiType = resolverApi(args[1]);
        final LocalDate dataInicio = parseDate(args[2], "dataInicio");
        final LocalDate dataFim = parseDate(args[3], "dataFim");
        final String entidade = normalizarEntidade(args[4]);
        final FaultMode faultMode = resolverFault(args);

        executarFaultSeNecessario(faultMode);
        if (ConfigEtl.isProcessoFilhoIsolado()) {
            executarStep(apiType, dataInicio, dataFim, entidade);
            return;
        }
        try (AutoCloseable ignored = new SqlServerExecutionLockManager().acquire(EXECUTION_LOCK_RESOURCE)) {
            executarStep(apiType, dataInicio, dataFim, entidade);
        }
    }

    private ApiType resolverApi(final String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Tipo de API nao informado para execucao isolada.");
        }
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "graphql" -> ApiType.GRAPHQL;
            case "dataexport" -> ApiType.DATAEXPORT;
            default -> throw new IllegalArgumentException("Tipo de API invalido para execucao isolada: " + raw);
        };
    }

    private LocalDate parseDate(final String raw, final String fieldName) {
        try {
            return LocalDate.parse(raw, DateTimeFormatter.ISO_DATE);
        } catch (final DateTimeParseException e) {
            throw new IllegalArgumentException("Data invalida para " + fieldName + ": " + raw, e);
        }
    }

    private String normalizarEntidade(final String raw) {
        if (raw == null || raw.isBlank() || "all".equalsIgnoreCase(raw) || "todas".equalsIgnoreCase(raw)) {
            return null;
        }
        return raw.trim();
    }

    private FaultMode resolverFault(final String[] args) {
        for (int i = 5; i < args.length; i++) {
            if ("--fault".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                return FaultMode.fromCliValue(args[i + 1]);
            }
        }
        return FaultMode.NONE;
    }

    private void executarFaultSeNecessario(final FaultMode faultMode) throws InterruptedException {
        if (faultMode == null || faultMode == FaultMode.NONE) {
            return;
        }
        if (faultMode == FaultMode.ERROR) {
            throw new IllegalStateException("Falha simulada no processo isolado.");
        }
        if (faultMode == FaultMode.HANG_IGNORE_INTERRUPT) {
            while (true) {
                try {
                    Thread.sleep(1_000L);
                } catch (final InterruptedException ignored) {
                    // Simula tarefa externa nao cooperativa; o pai deve matar o processo.
                }
            }
        }
    }

    private void validarOrigemDaExecucao() {
        if (ConfigEtl.isProcessoFilhoIsolado() || ConfigEtl.isExecucaoManualStepIsoladoPermitida()) {
            return;
        }
        throw new IllegalStateException(
            "O comando --executar-step-isolado e interno e nao pode ser executado manualmente sem ETL_PROCESS_ISOLATED_MANUAL_ALLOW=true."
        );
    }

    private void executarStep(final ApiType apiType,
                              final LocalDate dataInicio,
                              final LocalDate dataFim,
                              final String entidade) {
        switch (apiType) {
            case GRAPHQL -> new GraphQLExtractionService().executar(dataInicio, dataFim, entidade);
            case DATAEXPORT -> new DataExportExtractionService().executar(dataInicio, dataFim, entidade);
            default -> throw new IllegalArgumentException("API isolada nao suportada: " + apiType);
        }
    }
}
