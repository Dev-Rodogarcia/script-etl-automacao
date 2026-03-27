package br.com.extrator.features.manifestos.aplicacao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import br.com.extrator.plataforma.auditoria.aplicacao.FeatureExecutionWindowStrategy;
import br.com.extrator.plataforma.auditoria.dominio.ExecutionWindowPlan;
import br.com.extrator.suporte.validacao.ConstantesEntidades;

public final class ManifestosExecutionWindowStrategy implements FeatureExecutionWindowStrategy {

    @Override
    public String entidade() {
        return ConstantesEntidades.MANIFESTOS;
    }

    @Override
    public ExecutionWindowPlan planejar(final LocalDate dataReferenciaFim,
                                        final Optional<LocalDateTime> watermarkConfirmado) {
        final LocalDate consultaInicio = dataReferenciaFim.minusDays(1);
        final LocalDateTime confirmacaoInicio = watermarkConfirmado.orElse(consultaInicio.atStartOfDay());
        return new ExecutionWindowPlan(
            consultaInicio,
            dataReferenciaFim,
            confirmacaoInicio,
            dataReferenciaFim.atTime(LocalTime.MAX)
        );
    }
}
