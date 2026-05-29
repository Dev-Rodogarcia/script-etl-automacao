package br.com.extrator.plataforma.auditoria.aplicacao;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import br.com.extrator.aplicacao.extracao.ExtracaoPorIntervaloRequest.ModoExecucao;
import br.com.extrator.aplicacao.portas.ExecutionAuditPort;
import br.com.extrator.features.fretes.aplicacao.FretesExecutionWindowStrategy;
import br.com.extrator.features.localizacao.aplicacao.LocalizacaoCargasExecutionWindowStrategy;
import br.com.extrator.plataforma.auditoria.dominio.ExecutionWindowPlan;
import br.com.extrator.suporte.validacao.ConstantesEntidades;

public final class ExecutionWindowPlanner {
    private static final int REPLAY_MINIMO_DIAS = 7;
    private final ExecutionAuditPort executionAuditPort;
    private final Map<String, FeatureExecutionWindowStrategy> strategies;

    public ExecutionWindowPlanner(final ExecutionAuditPort executionAuditPort) {
        this.executionAuditPort = executionAuditPort;
        this.strategies = registrarStrategies();
    }

    public Map<String, ExecutionWindowPlan> planejarFluxoCompleto(final LocalDate dataReferenciaFim) {
        return planejarFluxoCompleto(dataReferenciaFim, ModoExecucao.INTERVALO);
    }

    public Map<String, ExecutionWindowPlan> planejarFluxoCompleto(final LocalDate dataReferenciaFim,
                                                                  final ModoExecucao modoExecucao) {
        final Map<String, ExecutionWindowPlan> planos = new LinkedHashMap<>();
        for (final String entidade : entidadesPadrao()) {
            planos.put(entidade, planejarEntidade(entidade, dataReferenciaFim, modoExecucao));
        }
        return Map.copyOf(planos);
    }

    public ExecutionWindowPlan planejarEntidade(final String entidade, final LocalDate dataReferenciaFim) {
        return planejarEntidade(entidade, dataReferenciaFim, ModoExecucao.INTERVALO);
    }

    public ExecutionWindowPlan planejarEntidade(final String entidade,
                                                final LocalDate dataReferenciaFim,
                                                final ModoExecucao modoExecucao) {
        final FeatureExecutionWindowStrategy strategy = strategies.get(entidade);
        if (strategy == null) {
            throw new IllegalArgumentException("Nenhuma strategy de janela registrada para a entidade '" + entidade + "'.");
        }
        return strategy.planejar(
            dataReferenciaFim,
            executionAuditPort.buscarWatermarkConfirmado(entidade),
            modoExecucao
        );
    }

    private Map<String, FeatureExecutionWindowStrategy> registrarStrategies() {
        final Map<String, FeatureExecutionWindowStrategy> registradas = new LinkedHashMap<>();
        registrarReplay(registradas, ConstantesEntidades.COLETAS);
        registrarReplay(registradas, ConstantesEntidades.MANIFESTOS);
        registradas.put(ConstantesEntidades.FRETES, new FretesExecutionWindowStrategy());
        registrarReplay(registradas, ConstantesEntidades.COTACOES);
        registradas.put(ConstantesEntidades.LOCALIZACAO_CARGAS, new LocalizacaoCargasExecutionWindowStrategy());
        registrarReplay(registradas, ConstantesEntidades.FATURAS_POR_CLIENTE);

        registrarJanelaDiaria(registradas, ConstantesEntidades.USUARIOS_SISTEMA);
        registrarJanelaDiaria(registradas, ConstantesEntidades.CONTAS_A_PAGAR);
        registrarJanelaDiaria(registradas, ConstantesEntidades.INVENTARIO);
        registrarJanelaDiaria(registradas, ConstantesEntidades.SINISTROS);
        return Map.copyOf(registradas);
    }

    private void registrarReplay(final Map<String, FeatureExecutionWindowStrategy> registradas,
                                 final String entidade) {
        registradas.put(entidade, new RegisteredExecutionWindowStrategy(entidade, REPLAY_MINIMO_DIAS, true));
    }

    private void registrarJanelaDiaria(final Map<String, FeatureExecutionWindowStrategy> registradas,
                                       final String entidade) {
        registradas.put(entidade, new RegisteredExecutionWindowStrategy(entidade, 2, true));
    }

    private List<String> entidadesPadrao() {
        return List.of(
            ConstantesEntidades.USUARIOS_SISTEMA,
            ConstantesEntidades.COLETAS,
            ConstantesEntidades.FRETES,
            ConstantesEntidades.MANIFESTOS,
            ConstantesEntidades.COTACOES,
            ConstantesEntidades.LOCALIZACAO_CARGAS,
            ConstantesEntidades.INVENTARIO,
            ConstantesEntidades.SINISTROS,
            ConstantesEntidades.CONTAS_A_PAGAR,
            ConstantesEntidades.FATURAS_POR_CLIENTE
        );
    }

    private static final class RegisteredExecutionWindowStrategy implements FeatureExecutionWindowStrategy {
        private final String entidade;
        private final int consultaDiasMinimos;
        private final boolean expandirConsultaAteWatermark;

        private RegisteredExecutionWindowStrategy(final String entidade,
                                                  final int consultaDiasMinimos,
                                                  final boolean expandirConsultaAteWatermark) {
            this.entidade = entidade;
            this.consultaDiasMinimos = Math.max(1, consultaDiasMinimos);
            this.expandirConsultaAteWatermark = expandirConsultaAteWatermark;
        }

        @Override
        public String entidade() {
            return entidade;
        }

        @Override
        public ExecutionWindowPlan planejar(final LocalDate dataReferenciaFim,
                                            final Optional<java.time.LocalDateTime> watermarkConfirmado) {
            final LocalDate consultaMinima = dataReferenciaFim.minusDays(consultaDiasMinimos - 1L);
            final LocalDate consultaInicio = expandirConsultaAteWatermark
                ? watermarkConfirmado
                    .map(java.time.LocalDateTime::toLocalDate)
                    .map(data -> data.isBefore(consultaMinima) ? data : consultaMinima)
                    .orElse(consultaMinima)
                : consultaMinima;
            final java.time.LocalDateTime confirmacaoFim = dataReferenciaFim.atTime(LocalTime.MAX);
            final java.time.LocalDateTime confirmacaoInicio = resolverConfirmacaoInicio(
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

        private static java.time.LocalDateTime resolverConfirmacaoInicio(
            final Optional<java.time.LocalDateTime> watermarkConfirmado,
            final LocalDate consultaInicio,
            final java.time.LocalDateTime confirmacaoFim
        ) {
            final java.time.LocalDateTime confirmacaoInicio =
                watermarkConfirmado.orElse(consultaInicio.atStartOfDay());
            return confirmacaoInicio.isAfter(confirmacaoFim) ? confirmacaoFim : confirmacaoInicio;
        }
    }
}
