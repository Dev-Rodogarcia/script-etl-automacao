package br.com.extrator.aplicacao.pipeline;

import java.time.LocalDate;

import br.com.extrator.aplicacao.portas.GraphQLGateway;
import br.com.extrator.aplicacao.pipeline.runtime.StepExecutionResult;

public final class GraphQLPipelineStep implements PipelineStep {
    private final GraphQLGateway gateway;
    private final String entidade;

    public GraphQLPipelineStep(final GraphQLGateway gateway, final String entidade) {
        this.gateway = gateway;
        this.entidade = entidade;
    }

    @Override
    public StepExecutionResult executar(final LocalDate dataInicio, final LocalDate dataFim) throws Exception {
        return gateway.executar(dataInicio, dataFim, entidade);
    }

    @Override
    public String obterNomeEtapa() {
        return "graphql:" + entidade;
    }

    @Override
    public String obterNomeEntidade() {
        return entidade;
    }
}


