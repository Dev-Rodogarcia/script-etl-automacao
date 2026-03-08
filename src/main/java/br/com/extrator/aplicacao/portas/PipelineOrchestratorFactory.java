package br.com.extrator.aplicacao.portas;

import br.com.extrator.aplicacao.pipeline.PipelineOrchestrator;

@FunctionalInterface
public interface PipelineOrchestratorFactory {
    PipelineOrchestrator criar();
}
