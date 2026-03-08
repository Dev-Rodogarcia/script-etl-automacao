package br.com.extrator.aplicacao.extracao;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.extrator.aplicacao.contexto.AplicacaoContexto;
import br.com.extrator.aplicacao.pipeline.GraphQLPipelineStep;
import br.com.extrator.aplicacao.pipeline.PipelineOrchestrator;
import br.com.extrator.aplicacao.pipeline.PipelineReport;
import br.com.extrator.aplicacao.pipeline.PipelineStep;
import br.com.extrator.aplicacao.pipeline.runtime.StepExecutionResult;
import br.com.extrator.aplicacao.pipeline.runtime.StepStatus;
import br.com.extrator.suporte.validacao.ConstantesEntidades;

public class PreBackfillReferencialColetasUseCase {

    private static final Logger log = LoggerFactory.getLogger(PreBackfillReferencialColetasUseCase.class);

    public PreBackfillReferencialColetasUseCase() {
    }

    public void executar(final LocalDate dataInicio, final LocalDate dataFim) {
        final LocalDate inicioEfetivo = resolverInicioEfetivo(dataInicio);

        final PipelineOrchestrator orchestrator = AplicacaoContexto.orchestratorFactory().criar();
        final List<PipelineStep> steps = List.of(
            new GraphQLPipelineStep(AplicacaoContexto.graphQLGateway(), ConstantesEntidades.COLETAS)
        );
        final PipelineReport report = orchestrator.executar(inicioEfetivo, dataFim, steps);
        final List<String> falhas = coletarFalhas(report);

        if (!falhas.isEmpty()) {
            throw new IllegalStateException(String.join(" | ", falhas));
        }
    }

    private LocalDate resolverInicioEfetivo(final LocalDate inicioEstatico) {
        final Optional<LocalDate> dataOrfao = AplicacaoContexto.manifestoOrfaoQueryPort()
            .buscarDataMaisAntigaManifestoOrfao();
        if (dataOrfao.isPresent() && dataOrfao.get().isBefore(inicioEstatico)) {
            log.info(
                "PRE-BACKFILL | janela_dinamica=true | orfao_mais_antigo={} | inicio_estatico={} | usando inicio_dinamico={}",
                dataOrfao.get(), inicioEstatico, dataOrfao.get()
            );
            return dataOrfao.get();
        }
        log.info(
            "PRE-BACKFILL | janela_dinamica=false | inicio_estatico={} | motivo={}",
            inicioEstatico,
            dataOrfao.isEmpty() ? "sem_orfaos_no_banco" : "orfao_dentro_da_janela_estatica"
        );
        return inicioEstatico;
    }

    private List<String> coletarFalhas(final PipelineReport report) {
        final List<String> falhas = new ArrayList<>();

        for (final StepExecutionResult result : report.getResultados()) {
            if (result.getStatus() == StepStatus.SUCCESS || result.getStatus() == StepStatus.DEGRADED) {
                continue;
            }

            final String nomeEtapa = result.obterNomeEtapa() == null ? "desconhecido" : result.obterNomeEtapa();
            final String mensagem = result.getMessage() == null ? "falha sem mensagem" : result.getMessage();
            falhas.add(nomeEtapa + ": " + mensagem);
        }

        if (report.isAborted()) {
            final String abortadoPor = report.getAbortedBy() == null ? "desconhecido" : report.getAbortedBy();
            falhas.add("pipeline abortado por " + abortadoPor);
        }

        return falhas;
    }
}
