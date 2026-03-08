package br.com.extrator.aplicacao.pipeline;

import java.time.LocalDate;

import br.com.extrator.aplicacao.portas.DataExportGateway;
import br.com.extrator.aplicacao.pipeline.runtime.StepExecutionResult;

public final class DataExportPipelineStep implements PipelineStep {
    private final DataExportGateway gateway;
    private final String entidade;

    public DataExportPipelineStep(final DataExportGateway gateway, final String entidade) {
        this.gateway = gateway;
        this.entidade = entidade;
    }

    @Override
    public StepExecutionResult executar(final LocalDate dataInicio, final LocalDate dataFim) throws Exception {
        return gateway.executar(dataInicio, dataFim, entidade);
    }

    @Override
    public String obterNomeEtapa() {
        return "dataexport:" + entidade;
    }

    @Override
    public String obterNomeEntidade() {
        return entidade;
    }
}


