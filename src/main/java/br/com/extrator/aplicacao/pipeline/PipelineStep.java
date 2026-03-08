package br.com.extrator.aplicacao.pipeline;

import java.time.LocalDate;

import br.com.extrator.aplicacao.pipeline.runtime.StepExecutionResult;

public interface PipelineStep {
    StepExecutionResult executar(LocalDate dataInicio, LocalDate dataFim) throws Exception;

    String obterNomeEtapa();

    String obterNomeEntidade();
}


