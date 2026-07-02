package br.com.extrator.plataforma.auditoria.aplicacao;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import br.com.extrator.aplicacao.extracao.ExtracaoPorIntervaloRequest.ModoExecucao;
import br.com.extrator.aplicacao.portas.ExecutionAuditPort;
import br.com.extrator.features.usuarios.aplicacao.UsuariosExecutionWindowStrategy;
import br.com.extrator.plataforma.auditoria.dominio.ExecutionWindowPlan;
import br.com.extrator.suporte.configuracao.ConfigEtl;
import br.com.extrator.suporte.validacao.ConstantesEntidades;

public final class ExecutionWindowPlanner {
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
        final int intradiaLookbackOffsetDias = ConfigEtl.obterIntradiaLookbackOffsetDias();
        registrarLookbackIntradia(registradas, ConstantesEntidades.COLETAS, intradiaLookbackOffsetDias);
        registrarLookbackIntradia(registradas, ConstantesEntidades.MANIFESTOS, intradiaLookbackOffsetDias);
        registrarLookbackIntradia(registradas, ConstantesEntidades.FRETES, intradiaLookbackOffsetDias);
        registrarLookbackIntradia(registradas, ConstantesEntidades.COTACOES, intradiaLookbackOffsetDias);
        registrarLookbackIntradia(registradas, ConstantesEntidades.LOCALIZACAO_CARGAS, intradiaLookbackOffsetDias);
        registrarLookbackIntradia(registradas, ConstantesEntidades.FATURAS_POR_CLIENTE, intradiaLookbackOffsetDias);
        registrarLookbackIntradia(registradas, ConstantesEntidades.CONTAS_A_PAGAR, intradiaLookbackOffsetDias);
        registrarLookbackIntradia(registradas, ConstantesEntidades.INVENTARIO, intradiaLookbackOffsetDias);
        registrarLookbackIntradia(registradas, ConstantesEntidades.SINISTROS, intradiaLookbackOffsetDias);
        registrarLookbackIntradia(registradas, ConstantesEntidades.RASTER_VIAGENS, intradiaLookbackOffsetDias);

        registradas.put(ConstantesEntidades.USUARIOS_SISTEMA, new UsuariosExecutionWindowStrategy());
        return Map.copyOf(registradas);
    }

    private void registrarLookbackIntradia(final Map<String, FeatureExecutionWindowStrategy> registradas,
                                           final String entidade,
                                           final int lookbackOffsetDias) {
        registradas.put(entidade, new IntradiaLookbackExecutionWindowStrategy(entidade, lookbackOffsetDias));
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
            ConstantesEntidades.FATURAS_POR_CLIENTE,
            ConstantesEntidades.RASTER_VIAGENS
        );
    }

    private static final class IntradiaLookbackExecutionWindowStrategy implements FeatureExecutionWindowStrategy {
        private final String entidade;
        private final int lookbackOffsetDias;

        private IntradiaLookbackExecutionWindowStrategy(final String entidade,
                                                       final int lookbackOffsetDias) {
            this.entidade = entidade;
            this.lookbackOffsetDias = Math.max(0, lookbackOffsetDias);
        }

        @Override
        public String entidade() {
            return entidade;
        }

        @Override
        public ExecutionWindowPlan planejar(final LocalDate dataReferenciaFim,
                                            final Optional<java.time.LocalDateTime> watermarkConfirmado) {
            final LocalDate consultaInicio = dataReferenciaFim.minusDays(lookbackOffsetDias);
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
