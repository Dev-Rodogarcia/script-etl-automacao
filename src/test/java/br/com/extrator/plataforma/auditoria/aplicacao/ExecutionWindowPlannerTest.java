package br.com.extrator.plataforma.auditoria.aplicacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import br.com.extrator.aplicacao.portas.ExecutionAuditPort;
import br.com.extrator.plataforma.auditoria.dominio.ExecutionAuditRecord;
import br.com.extrator.plataforma.auditoria.dominio.ExecutionWindowPlan;
import br.com.extrator.suporte.validacao.ConstantesEntidades;

class ExecutionWindowPlannerTest {

    @Test
    void devePlanejarJanelaComOverlapDe48hParaEntidadesCriticas() {
        final ExecutionWindowPlanner planner = new ExecutionWindowPlanner(new StubExecutionAuditPort());

        final ExecutionWindowPlan planoColetas = planner.planejarEntidade(
            ConstantesEntidades.COLETAS,
            LocalDate.of(2026, 3, 25)
        );
        final ExecutionWindowPlan planoManifestos = planner.planejarEntidade(
            ConstantesEntidades.MANIFESTOS,
            LocalDate.of(2026, 3, 25)
        );

        assertEquals(LocalDate.of(2026, 3, 24), planoColetas.consultaDataInicio());
        assertEquals(LocalDate.of(2026, 3, 25), planoColetas.consultaDataFim());
        assertEquals(LocalDate.of(2026, 3, 24), planoManifestos.consultaDataInicio());
        assertEquals(LocalDate.of(2026, 3, 25), planoManifestos.consultaDataFim());
    }

    @Test
    void deveUsarWatermarkConfirmadoComoInicioDeConfirmacao() {
        final LocalDateTime watermark = LocalDateTime.of(2026, 3, 24, 5, 30);
        final ExecutionWindowPlanner planner = new ExecutionWindowPlanner(new StubExecutionAuditPort(watermark));

        final ExecutionWindowPlan plano = planner.planejarEntidade(
            ConstantesEntidades.FRETES,
            LocalDate.of(2026, 3, 25)
        );

        assertEquals(watermark, plano.confirmacaoInicio());
        assertEquals(LocalDateTime.of(2026, 3, 25, LocalTime.MAX.getHour(), LocalTime.MAX.getMinute(), LocalTime.MAX.getSecond(), LocalTime.MAX.getNano()), plano.confirmacaoFim());
    }

    @Test
    void naoDeveIncluirFaturasGraphqlQuandoFlagEstiverDesabilitada() {
        final ExecutionWindowPlanner planner = new ExecutionWindowPlanner(new StubExecutionAuditPort());

        final var planos = planner.planejarFluxoCompleto(LocalDate.of(2026, 3, 25), false);

        assertFalse(planos.containsKey(ConstantesEntidades.FATURAS_GRAPHQL));
    }

    private static final class StubExecutionAuditPort implements ExecutionAuditPort {
        private final LocalDateTime watermark;

        private StubExecutionAuditPort() {
            this(null);
        }

        private StubExecutionAuditPort(final LocalDateTime watermark) {
            this.watermark = watermark;
        }

        @Override
        public void registrarResultado(final ExecutionAuditRecord record) {
            // no-op
        }

        @Override
        public Optional<ExecutionAuditRecord> buscarResultado(final String executionUuid, final String entidade) {
            return Optional.empty();
        }

        @Override
        public List<ExecutionAuditRecord> listarResultados(final String executionUuid) {
            return List.of();
        }

        @Override
        public Optional<LocalDateTime> buscarWatermarkConfirmado(final String entidade) {
            return Optional.ofNullable(watermark);
        }

        @Override
        public void atualizarWatermarkConfirmado(final String entidade, final LocalDateTime watermarkConfirmado) {
            // no-op
        }
    }
}
