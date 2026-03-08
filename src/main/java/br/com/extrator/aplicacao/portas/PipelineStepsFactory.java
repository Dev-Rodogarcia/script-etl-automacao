package br.com.extrator.aplicacao.portas;

import java.util.List;

import br.com.extrator.aplicacao.pipeline.PipelineStep;

public interface PipelineStepsFactory {
    List<PipelineStep> criarStepsFluxoCompleto(boolean incluirFaturasGraphQL, boolean incluirDataQuality);
}
