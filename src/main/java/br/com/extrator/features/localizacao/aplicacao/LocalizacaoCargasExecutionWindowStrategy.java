package br.com.extrator.features.localizacao.aplicacao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import br.com.extrator.aplicacao.extracao.ExtracaoPorIntervaloRequest.ModoExecucao;
import br.com.extrator.plataforma.auditoria.aplicacao.FeatureExecutionWindowStrategy;
import br.com.extrator.plataforma.auditoria.dominio.ExecutionWindowPlan;
import br.com.extrator.suporte.validacao.ConstantesEntidades;

public final class LocalizacaoCargasExecutionWindowStrategy implements FeatureExecutionWindowStrategy {
    private static final int REPLAY_DIAS = 90;
    private static final int MICRO_BATCH_DIAS = 2;

    @Override
    public String entidade() {
        return ConstantesEntidades.LOCALIZACAO_CARGAS;
    }

    @Override
    public ExecutionWindowPlan planejar(final LocalDate dataReferenciaFim,
                                        final Optional<LocalDateTime> watermarkConfirmado) {
        return planejar(dataReferenciaFim, watermarkConfirmado, ModoExecucao.INTERVALO);
    }

    @Override
    public ExecutionWindowPlan planejar(final LocalDate dataReferenciaFim,
                                        final Optional<LocalDateTime> watermarkConfirmado,
                                        final ModoExecucao modoExecucao) {
        final int diasConsulta = modoExecucao == ModoExecucao.MICRO_BATCH ? MICRO_BATCH_DIAS : REPLAY_DIAS;
        final boolean expandirAteWatermark = modoExecucao != ModoExecucao.MICRO_BATCH;
        return planejarComJanela(dataReferenciaFim, watermarkConfirmado, diasConsulta, expandirAteWatermark);
    }

    private static ExecutionWindowPlan planejarComJanela(final LocalDate dataReferenciaFim,
                                                         final Optional<LocalDateTime> watermarkConfirmado,
                                                         final int diasConsulta,
                                                         final boolean expandirAteWatermark) {
        final LocalDate consultaMinima = dataReferenciaFim.minusDays(Math.max(1, diasConsulta) - 1L);
        final LocalDate consultaInicio = expandirAteWatermark
            ? watermarkConfirmado
                .map(LocalDateTime::toLocalDate)
                .map(data -> data.isBefore(consultaMinima) ? data : consultaMinima)
                .orElse(consultaMinima)
            : consultaMinima;
        final LocalDateTime confirmacaoFim = dataReferenciaFim.atTime(LocalTime.MAX);
        final LocalDateTime confirmacaoInicio = resolverConfirmacaoInicio(
            watermarkConfirmado,
            consultaInicio,
            confirmacaoFim
        );
        return new ExecutionWindowPlan(
            consultaInicio,
            dataReferenciaFim,
            confirmacaoInicio,
            confirmacaoFim
        );
    }

    private static LocalDateTime resolverConfirmacaoInicio(final Optional<LocalDateTime> watermarkConfirmado,
                                                           final LocalDate consultaInicio,
                                                           final LocalDateTime confirmacaoFim) {
        final LocalDateTime confirmacaoInicio = watermarkConfirmado.orElse(consultaInicio.atStartOfDay());
        return confirmacaoInicio.isAfter(confirmacaoFim) ? confirmacaoFim : confirmacaoInicio;
    }
}
