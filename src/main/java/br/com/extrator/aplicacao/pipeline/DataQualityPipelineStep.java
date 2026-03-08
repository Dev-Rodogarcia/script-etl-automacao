/* ==[DOC-FILE]===============================================================
Arquivo : src/main/java/br/com/extrator/aplicacao/pipeline/DataQualityPipelineStep.java
Classe  : DataQualityPipelineStep (class)
Pacote  : br.com.extrator.aplicacao.pipeline
Modulo  : Pipeline - Aplicacao

Papel   : Step de pipeline que executa validacao de qualidade dos dados extraidos (5 checks).

Conecta com:
- DataQualityService (delegacao para validacao)
- PipelineStep (interface que implementa)

Fluxo geral:
1) executar(dataInicio, dataFim) delega a qualityService.avaliar().
2) Retorna StepExecutionResult com status (SUCCESS | FAILED) e metadata (checks_total, checks_failed).
3) Se aprovado: SUCCESS; senao: FAILED com ErrorTaxonomy.DATA_QUALITY_BREACH.

Estrutura interna:
Atributos-chave:
- qualityService: DataQualityService (validador).
- entidades: List<String> (entidades a validar).
Metodos principais:
- executar(): executa checks de qualidade.
- obterNomeEtapa(), obterNomeEntidade(): identificacao.
[DOC-FILE-END]============================================================== */
package br.com.extrator.aplicacao.pipeline;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import br.com.extrator.observabilidade.quality.DataQualityReport;
import br.com.extrator.observabilidade.quality.DataQualityService;
import br.com.extrator.aplicacao.pipeline.runtime.StepExecutionResult;
import br.com.extrator.aplicacao.pipeline.runtime.StepStatus;
import br.com.extrator.aplicacao.politicas.ErrorTaxonomy;

public final class DataQualityPipelineStep implements PipelineStep {
    private final DataQualityService qualityService;
    private final List<String> entidades;

    public DataQualityPipelineStep(final DataQualityService qualityService, final List<String> entidades) {
        this.qualityService = qualityService;
        this.entidades = entidades;
    }

    @Override
    public StepExecutionResult executar(final LocalDate dataInicio, final LocalDate dataFim) {
        final LocalDateTime inicio = LocalDateTime.now();
        final DataQualityReport report = qualityService.avaliar(dataInicio, dataFim, entidades);
        final boolean ok = report.isAprovado();
        final StepExecutionResult.Builder builder = StepExecutionResult.builder(obterNomeEtapa(), obterNomeEntidade())
            .status(ok ? StepStatus.SUCCESS : StepStatus.FAILED)
            .startedAt(inicio)
            .finishedAt(LocalDateTime.now())
            .message("checks_failed=" + report.totalFalhas())
            .metadata("checks_total", report.obterResultados().size())
            .metadata("checks_failed", report.totalFalhas());
        if (!ok) {
            builder.errorTaxonomy(ErrorTaxonomy.DATA_QUALITY_BREACH);
        }
        return builder.build();
    }

    @Override
    public String obterNomeEtapa() {
        return "quality:checks";
    }

    @Override
    public String obterNomeEntidade() {
        return "quality";
    }
}


