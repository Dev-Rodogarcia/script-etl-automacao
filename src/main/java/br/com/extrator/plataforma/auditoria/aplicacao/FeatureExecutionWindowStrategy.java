package br.com.extrator.plataforma.auditoria.aplicacao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import br.com.extrator.aplicacao.extracao.ExtracaoPorIntervaloRequest.ModoExecucao;
import br.com.extrator.plataforma.auditoria.dominio.ExecutionWindowPlan;

public interface FeatureExecutionWindowStrategy {

    String entidade();

    ExecutionWindowPlan planejar(LocalDate dataReferenciaFim, Optional<LocalDateTime> watermarkConfirmado);

    default ExecutionWindowPlan planejar(final LocalDate dataReferenciaFim,
                                         final Optional<LocalDateTime> watermarkConfirmado,
                                         final ModoExecucao modoExecucao) {
        return planejar(dataReferenciaFim, watermarkConfirmado);
    }
}
