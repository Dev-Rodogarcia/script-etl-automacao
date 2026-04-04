package br.com.extrator.plataforma.auditoria.aplicacao;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import br.com.extrator.aplicacao.portas.ExecutionAuditPort;
import br.com.extrator.features.coletas.aplicacao.ColetasExecutionWindowStrategy;
import br.com.extrator.features.fretes.aplicacao.FretesExecutionWindowStrategy;
import br.com.extrator.features.manifestos.aplicacao.ManifestosExecutionWindowStrategy;
import br.com.extrator.features.usuarios.aplicacao.UsuariosExecutionWindowStrategy;
import br.com.extrator.plataforma.auditoria.dominio.ExecutionWindowPlan;
import br.com.extrator.suporte.validacao.ConstantesEntidades;

public final class ExecutionWindowPlanner {
    private final ExecutionAuditPort executionAuditPort;
    private final Map<String, FeatureExecutionWindowStrategy> strategies;

    public ExecutionWindowPlanner(final ExecutionAuditPort executionAuditPort) {
        this.executionAuditPort = executionAuditPort;
        this.strategies = Map.of(
            ConstantesEntidades.COLETAS, new ColetasExecutionWindowStrategy(),
            ConstantesEntidades.MANIFESTOS, new ManifestosExecutionWindowStrategy(),
            ConstantesEntidades.FRETES, new FretesExecutionWindowStrategy(),
            ConstantesEntidades.USUARIOS_SISTEMA, new UsuariosExecutionWindowStrategy()
        );
    }

    public Map<String, ExecutionWindowPlan> planejarFluxoCompleto(final LocalDate dataReferenciaFim,
                                                                  final boolean incluirFaturasGraphQL) {
        final Map<String, ExecutionWindowPlan> planos = new LinkedHashMap<>();
        for (final String entidade : entidadesPadrao(incluirFaturasGraphQL)) {
            planos.put(entidade, planejarEntidade(entidade, dataReferenciaFim));
        }
        return Map.copyOf(planos);
    }

    public ExecutionWindowPlan planejarEntidade(final String entidade, final LocalDate dataReferenciaFim) {
        final FeatureExecutionWindowStrategy strategy = strategies.get(entidade);
        if (strategy == null) {
            return janelaPadrao(dataReferenciaFim);
        }
        return strategy.planejar(
            dataReferenciaFim,
            executionAuditPort.buscarWatermarkConfirmado(entidade)
        );
    }

    private ExecutionWindowPlan janelaPadrao(final LocalDate dataReferenciaFim) {
        final LocalDate consultaInicio = dataReferenciaFim.minusDays(1);
        return new ExecutionWindowPlan(
            consultaInicio,
            dataReferenciaFim,
            consultaInicio.atStartOfDay(),
            dataReferenciaFim.atTime(LocalTime.MAX)
        );
    }

    private List<String> entidadesPadrao(final boolean incluirFaturasGraphQL) {
        final List<String> entidades = new java.util.ArrayList<>(List.of(
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
        ));
        if (incluirFaturasGraphQL) {
            entidades.add(ConstantesEntidades.FATURAS_GRAPHQL);
        }
        return List.copyOf(entidades);
    }
}
