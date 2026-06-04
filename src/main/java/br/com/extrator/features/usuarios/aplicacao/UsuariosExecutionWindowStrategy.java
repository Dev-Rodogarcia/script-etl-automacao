package br.com.extrator.features.usuarios.aplicacao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import br.com.extrator.plataforma.auditoria.aplicacao.FeatureExecutionWindowStrategy;
import br.com.extrator.plataforma.auditoria.dominio.ExecutionWindowPlan;
import br.com.extrator.suporte.tempo.RelogioSistema;
import br.com.extrator.suporte.validacao.ConstantesEntidades;

public final class UsuariosExecutionWindowStrategy implements FeatureExecutionWindowStrategy {
    public static final int FALLBACK_DIAS_CARGA_INICIAL = 90;

    @Override
    public String entidade() {
        return ConstantesEntidades.USUARIOS_SISTEMA;
    }

    @Override
    public ExecutionWindowPlan planejar(final LocalDate dataReferenciaFim,
                                        final Optional<LocalDateTime> watermarkConfirmado) {
        final LocalDate dataFim = dataReferenciaFim == null ? RelogioSistema.hoje() : dataReferenciaFim;
        final LocalDateTime confirmacaoFim = resolverConfirmacaoFim(dataFim);
        final LocalDate consultaInicio = watermarkConfirmado
            .map(LocalDateTime::toLocalDate)
            .orElse(dataFim.minusDays(FALLBACK_DIAS_CARGA_INICIAL));
        final LocalDateTime confirmacaoInicio = resolverConfirmacaoInicio(
            watermarkConfirmado.orElse(consultaInicio.atStartOfDay()),
            confirmacaoFim
        );
        return new ExecutionWindowPlan(
            consultaInicio,
            dataFim,
            confirmacaoInicio,
            confirmacaoFim
        );
    }

    private LocalDateTime resolverConfirmacaoFim(final LocalDate dataFim) {
        final LocalDateTime agora = RelogioSistema.agora();
        if (dataFim == null || !dataFim.isBefore(agora.toLocalDate())) {
            return agora;
        }
        return dataFim.atTime(LocalTime.MAX);
    }

    private LocalDateTime resolverConfirmacaoInicio(final LocalDateTime candidato,
                                                    final LocalDateTime confirmacaoFim) {
        if (candidato == null) {
            return confirmacaoFim;
        }
        return candidato.isAfter(confirmacaoFim) ? confirmacaoFim : candidato;
    }
}
