package br.com.extrator.plataforma.auditoria.aplicacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.com.extrator.aplicacao.extracao.ExtracaoPorIntervaloRequest.ModoExecucao;
import br.com.extrator.aplicacao.portas.ExecutionAuditPort;
import br.com.extrator.plataforma.auditoria.dominio.ExecutionAuditRecord;
import br.com.extrator.plataforma.auditoria.dominio.ExecutionWindowPlan;
import br.com.extrator.suporte.validacao.ConstantesEntidades;

class ExecutionWindowPlannerTest {
    private static final String PROP_LOOKBACK_INTRADIA = "etl.intradia.lookback.offset.dias";
    private String lookbackAnterior;

    @BeforeEach
    void configurarLookbackIntradia() {
        lookbackAnterior = System.getProperty(PROP_LOOKBACK_INTRADIA);
        System.setProperty(PROP_LOOKBACK_INTRADIA, "7");
    }

    @AfterEach
    void restaurarLookbackIntradia() {
        if (lookbackAnterior == null) {
            System.clearProperty(PROP_LOOKBACK_INTRADIA);
            return;
        }
        System.setProperty(PROP_LOOKBACK_INTRADIA, lookbackAnterior);
    }

    @Test
    void devePlanejarLookbackIntradiaD7InclusivoParaEntidadesCriticas() {
        final ExecutionWindowPlanner planner = new ExecutionWindowPlanner(new StubExecutionAuditPort());

        final ExecutionWindowPlan planoColetas = planner.planejarEntidade(
            ConstantesEntidades.COLETAS,
            LocalDate.of(2026, 3, 25)
        );
        final ExecutionWindowPlan planoManifestos = planner.planejarEntidade(
            ConstantesEntidades.MANIFESTOS,
            LocalDate.of(2026, 3, 25)
        );
        final ExecutionWindowPlan planoCotacoes = planner.planejarEntidade(
            ConstantesEntidades.COTACOES,
            LocalDate.of(2026, 3, 25)
        );

        assertEquals(LocalDate.of(2026, 3, 18), planoColetas.consultaDataInicio());
        assertEquals(LocalDate.of(2026, 3, 25), planoColetas.consultaDataFim());
        assertEquals(LocalDate.of(2026, 3, 18), planoManifestos.consultaDataInicio());
        assertEquals(LocalDate.of(2026, 3, 25), planoManifestos.consultaDataFim());
        assertEquals(LocalDate.of(2026, 3, 18), planoCotacoes.consultaDataInicio());
    }

    @Test
    void deveRespeitarOffsetLookbackIntradiaConfigurado() {
        System.setProperty(PROP_LOOKBACK_INTRADIA, "3");
        final ExecutionWindowPlanner planner = new ExecutionWindowPlanner(new StubExecutionAuditPort());

        final ExecutionWindowPlan plano = planner.planejarEntidade(
            ConstantesEntidades.RASTER_VIAGENS,
            LocalDate.of(2026, 3, 25)
        );

        assertEquals(LocalDate.of(2026, 3, 22), plano.consultaDataInicio());
        assertEquals(LocalDate.of(2026, 3, 25), plano.consultaDataFim());
    }

    @Test
    void devePlanejarFretesComLookbackIntradiaPadronizado() {
        final ExecutionWindowPlanner planner = new ExecutionWindowPlanner(new StubExecutionAuditPort());

        final ExecutionWindowPlan plano = planner.planejarEntidade(
            ConstantesEntidades.FRETES,
            LocalDate.of(2026, 3, 25)
        );

        assertEquals(LocalDate.of(2026, 3, 18), plano.consultaDataInicio());
        assertEquals(LocalDate.of(2026, 3, 25), plano.consultaDataFim());
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
        assertEquals(LocalDate.of(2026, 3, 18), plano.consultaDataInicio());
        assertEquals(LocalDateTime.of(2026, 3, 25, LocalTime.MAX.getHour(), LocalTime.MAX.getMinute(), LocalTime.MAX.getSecond(), LocalTime.MAX.getNano()), plano.confirmacaoFim());
    }

    @Test
    void deveEvitarConfirmacaoInvertidaQuandoWatermarkPassouDoFimDaJanela() {
        final LocalDate dataReferencia = LocalDate.of(2026, 3, 25);
        final LocalDateTime watermark = LocalDateTime.of(2026, 3, 26, 0, 0);
        final LocalDateTime fimDaJanela = dataReferencia.atTime(LocalTime.MAX);
        final ExecutionWindowPlanner planner = new ExecutionWindowPlanner(new StubExecutionAuditPort(watermark));

        final ExecutionWindowPlan planoFretes = planner.planejarEntidade(
            ConstantesEntidades.FRETES,
            dataReferencia
        );
        final ExecutionWindowPlan planoColetas = planner.planejarEntidade(
            ConstantesEntidades.COLETAS,
            dataReferencia
        );

        assertEquals(fimDaJanela, planoFretes.confirmacaoInicio());
        assertEquals(fimDaJanela, planoFretes.confirmacaoFim());
        assertEquals(fimDaJanela, planoColetas.confirmacaoInicio());
        assertEquals(fimDaJanela, planoColetas.confirmacaoFim());
    }

    @Test
    void devePlanejarLocalizacaoCargasComLookbackIntradiaPadronizado() {
        final ExecutionWindowPlanner planner = new ExecutionWindowPlanner(new StubExecutionAuditPort());

        final ExecutionWindowPlan plano = planner.planejarEntidade(
            ConstantesEntidades.LOCALIZACAO_CARGAS,
            LocalDate.of(2026, 3, 25),
            ModoExecucao.INTERVALO
        );

        assertEquals(LocalDate.of(2026, 3, 18), plano.consultaDataInicio());
        assertEquals(LocalDate.of(2026, 3, 25), plano.consultaDataFim());
    }

    @Test
    void deveManterLocalizacaoCargasEmD7MesmoComWatermarkAntigo() {
        final LocalDateTime watermark = LocalDateTime.of(2026, 1, 1, 5, 30);
        final ExecutionWindowPlanner planner = new ExecutionWindowPlanner(new StubExecutionAuditPort(watermark));

        final ExecutionWindowPlan plano = planner.planejarEntidade(
            ConstantesEntidades.LOCALIZACAO_CARGAS,
            LocalDate.of(2026, 3, 25),
            ModoExecucao.MICRO_BATCH
        );

        assertEquals(LocalDate.of(2026, 3, 18), plano.consultaDataInicio());
        assertEquals(LocalDate.of(2026, 3, 25), plano.consultaDataFim());
        assertEquals(watermark, plano.confirmacaoInicio());
    }

    @Test
    void deveIncluirInventarioESinistrosNoFluxoCompleto() {
        final ExecutionWindowPlanner planner = new ExecutionWindowPlanner(new StubExecutionAuditPort());

        final var planos = planner.planejarFluxoCompleto(LocalDate.of(2026, 3, 25));

        assertTrue(planos.containsKey(ConstantesEntidades.INVENTARIO));
        assertTrue(planos.containsKey(ConstantesEntidades.SINISTROS));
        assertTrue(planos.containsKey(ConstantesEntidades.CONTAS_A_PAGAR));
        assertTrue(planos.containsKey(ConstantesEntidades.FATURAS_POR_CLIENTE));
        assertTrue(planos.containsKey(ConstantesEntidades.RASTER_VIAGENS));
        assertJanelaD7(planos.get(ConstantesEntidades.COLETAS));
        assertJanelaD7(planos.get(ConstantesEntidades.FRETES));
        assertJanelaD7(planos.get(ConstantesEntidades.MANIFESTOS));
        assertJanelaD7(planos.get(ConstantesEntidades.COTACOES));
        assertJanelaD7(planos.get(ConstantesEntidades.LOCALIZACAO_CARGAS));
        assertJanelaD7(planos.get(ConstantesEntidades.INVENTARIO));
        assertJanelaD7(planos.get(ConstantesEntidades.SINISTROS));
        assertJanelaD7(planos.get(ConstantesEntidades.CONTAS_A_PAGAR));
        assertJanelaD7(planos.get(ConstantesEntidades.FATURAS_POR_CLIENTE));
        assertJanelaD7(planos.get(ConstantesEntidades.RASTER_VIAGENS));
    }

    @Test
    void deveLimitarUsuariosANoventaDiasQuandoNaoHaWatermark() {
        final ExecutionWindowPlanner planner = new ExecutionWindowPlanner(new StubExecutionAuditPort());

        final ExecutionWindowPlan plano = planner.planejarEntidade(
            ConstantesEntidades.USUARIOS_SISTEMA,
            LocalDate.of(2026, 6, 3)
        );

        assertEquals(LocalDate.of(2026, 3, 5), plano.consultaDataInicio());
        assertEquals(LocalDate.of(2026, 6, 3), plano.consultaDataFim());
        assertEquals(LocalDateTime.of(2026, 3, 5, 0, 0), plano.confirmacaoInicio());
        assertEquals(LocalDateTime.of(2026, 6, 3, LocalTime.MAX.getHour(), LocalTime.MAX.getMinute(), LocalTime.MAX.getSecond(), LocalTime.MAX.getNano()), plano.confirmacaoFim());
    }

    @Test
    void devePlanejarUsuariosAPartirDoWatermarkConfirmado() {
        final LocalDateTime watermark = LocalDateTime.of(2026, 6, 2, 10, 45);
        final ExecutionWindowPlanner planner = new ExecutionWindowPlanner(new StubExecutionAuditPort(watermark));

        final ExecutionWindowPlan plano = planner.planejarEntidade(
            ConstantesEntidades.USUARIOS_SISTEMA,
            LocalDate.of(2026, 6, 3)
        );

        assertEquals(LocalDate.of(2026, 6, 2), plano.consultaDataInicio());
        assertEquals(watermark, plano.confirmacaoInicio());
        assertEquals(LocalDate.of(2026, 6, 3), plano.consultaDataFim());
    }

    @Test
    void deveManterConsultaD7MesmoComWatermarkMaisAntigo() {
        final LocalDateTime watermark = LocalDateTime.of(2026, 3, 10, 5, 30);
        final ExecutionWindowPlanner planner = new ExecutionWindowPlanner(new StubExecutionAuditPort(watermark));

        final ExecutionWindowPlan plano = planner.planejarEntidade(
            ConstantesEntidades.COLETAS,
            LocalDate.of(2026, 3, 25)
        );

        assertEquals(LocalDate.of(2026, 3, 18), plano.consultaDataInicio());
        assertEquals(watermark, plano.confirmacaoInicio());
    }

    @Test
    void deveFalharQuandoEntidadeNaoPossuirStrategyRegistrada() {
        final ExecutionWindowPlanner planner = new ExecutionWindowPlanner(new StubExecutionAuditPort());

        assertThrows(
            IllegalArgumentException.class,
            () -> planner.planejarEntidade("entidade_desconhecida", LocalDate.of(2026, 3, 25))
        );
    }

    private static void assertJanelaD7(final ExecutionWindowPlan plano) {
        assertEquals(LocalDate.of(2026, 3, 18), plano.consultaDataInicio());
        assertEquals(LocalDate.of(2026, 3, 25), plano.consultaDataFim());
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
