package br.com.extrator.bootstrap.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.extrator.aplicacao.pipeline.PipelineOrchestrator;
import br.com.extrator.aplicacao.pipeline.PipelineReport;
import br.com.extrator.aplicacao.pipeline.PipelineStep;
import br.com.extrator.aplicacao.pipeline.runtime.StepExecutionResult;
import br.com.extrator.aplicacao.pipeline.runtime.StepStatus;
import br.com.extrator.aplicacao.portas.ClockPort;
import br.com.extrator.aplicacao.portas.ConfigPort;
import br.com.extrator.aplicacao.portas.ExtractionLoggerPort;
import br.com.extrator.observabilidade.pipeline.InMemoryPipelineMetrics;

class PipelineCompositionRootAuditTest {

    @Test
    void deveAbortarStepCorePorPadraoQuandoConfiguracaoNaoSobrescreveFailureMode() {
        final PipelineCompositionRoot root = new PipelineCompositionRoot(
            new ConfigPortPadrao(),
            new FixedClock(LocalDateTime.of(2026, 4, 14, 10, 0)),
            new NoOpExtractionLogger(),
            new InMemoryPipelineMetrics()
        );
        final PipelineOrchestrator orchestrator = root.criarOrquestrador();

        final List<PipelineStep> steps = List.of(
            falha("graphql-core", "graphql"),
            sucesso("dataexport-core", "dataexport")
        );

        final PipelineReport report = orchestrator.executar(LocalDate.of(2026, 4, 14), LocalDate.of(2026, 4, 14), steps);

        assertTrue(report.isAborted());
        assertEquals("graphql-core", report.getAbortedBy());
        assertEquals(1, report.getResultados().size());
        assertEquals(StepStatus.FAILED, report.getResultados().get(0).getStatus());
        assertFalse(report.getResultados().isEmpty());
    }

    private PipelineStep falha(final String nomeEtapa, final String entidade) {
        return new PipelineStep() {
            @Override
            public StepExecutionResult executar(final LocalDate dataInicio, final LocalDate dataFim) {
                throw new IllegalStateException("falha forcada");
            }

            @Override
            public String obterNomeEtapa() {
                return nomeEtapa;
            }

            @Override
            public String obterNomeEntidade() {
                return entidade;
            }
        };
    }

    private PipelineStep sucesso(final String nomeEtapa, final String entidade) {
        return new PipelineStep() {
            @Override
            public StepExecutionResult executar(final LocalDate dataInicio, final LocalDate dataFim) {
                final LocalDateTime agora = LocalDateTime.now();
                return StepExecutionResult.builder(nomeEtapa, entidade)
                    .status(StepStatus.SUCCESS)
                    .startedAt(agora)
                    .finishedAt(agora.plusSeconds(1))
                    .build();
            }

            @Override
            public String obterNomeEtapa() {
                return nomeEtapa;
            }

            @Override
            public String obterNomeEntidade() {
                return entidade;
            }
        };
    }

    private static final class ConfigPortPadrao implements ConfigPort {
        @Override
        public String obterTexto(final String key, final String defaultValue) {
            return defaultValue;
        }

        @Override
        public int obterInteiro(final String key, final int defaultValue) {
            return defaultValue;
        }

        @Override
        public long obterLongo(final String key, final long defaultValue) {
            return defaultValue;
        }

        @Override
        public double obterDecimal(final String key, final double defaultValue) {
            return defaultValue;
        }

        @Override
        public boolean obterBooleano(final String key, final boolean defaultValue) {
            return defaultValue;
        }
    }

    private static final class NoOpExtractionLogger implements ExtractionLoggerPort {
        @Override
        public void logarEstruturado(final String eventName, final java.util.Map<String, Object> fields) {
            // no-op
        }
    }

    private static final class FixedClock implements ClockPort {
        private LocalDateTime atual;

        private FixedClock(final LocalDateTime atual) {
            this.atual = atual;
        }

        @Override
        public LocalDate hoje() {
            return atual.toLocalDate();
        }

        @Override
        public LocalDateTime agora() {
            return atual;
        }

        @Override
        public void dormir(final Duration duration) {
            atual = atual.plus(duration);
        }
    }
}
