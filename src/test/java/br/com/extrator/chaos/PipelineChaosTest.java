package br.com.extrator.chaos;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.net.http.HttpTimeoutException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import br.com.extrator.aplicacao.portas.ClockPort;
import br.com.extrator.aplicacao.portas.ExtractionLoggerPort;
import br.com.extrator.aplicacao.pipeline.PipelineOrchestrator;
import br.com.extrator.aplicacao.pipeline.PipelineReport;
import br.com.extrator.aplicacao.pipeline.PipelineStep;
import br.com.extrator.aplicacao.pipeline.runtime.StepExecutionResult;
import br.com.extrator.aplicacao.pipeline.runtime.StepStatus;
import br.com.extrator.observabilidade.pipeline.InMemoryPipelineMetrics;
import br.com.extrator.aplicacao.politicas.CircuitBreaker;
import br.com.extrator.aplicacao.politicas.ErrorClassifier;
import br.com.extrator.aplicacao.politicas.ExponentialBackoffRetryPolicy;
import br.com.extrator.aplicacao.politicas.FailureMode;

class PipelineChaosTest {

    @Test
    void deveAbrirCircuitBreakerAposFalhasConsecutivas() {
        final MutableClock clock = new MutableClock(LocalDateTime.of(2026, 1, 1, 0, 0));
        final PipelineOrchestrator orchestrator = new PipelineOrchestrator(
            new ExponentialBackoffRetryPolicy(2, 10L, 2.0d, 0.0d, clock),
            (entidade, taxonomy) -> FailureMode.CONTINUE_WITH_ALERT,
            new CircuitBreaker(1, Duration.ofSeconds(120), clock),
            new ErrorClassifier(),
            noOpLogger(),
            new InMemoryPipelineMetrics()
        );

        final PipelineStep timeoutStep = new PipelineStep() {
            @Override
            public StepExecutionResult executar(final LocalDate dataInicio, final LocalDate dataFim) throws Exception {
                throw new HttpTimeoutException("api timeout");
            }

            @Override
            public String obterNomeEtapa() {
                return "graphql:coletas";
            }

            @Override
            public String obterNomeEntidade() {
                return "coletas";
            }
        };

        final PipelineReport first = orchestrator.executar(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2), List.of(timeoutStep));
        assertEquals(StepStatus.FAILED, first.getResultados().get(0).getStatus());

        final PipelineReport second = orchestrator.executar(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2), List.of(timeoutStep));
        assertEquals(StepStatus.SKIPPED, second.getResultados().get(0).getStatus());
    }

    @Test
    void deveContinuarExecutandoCiclosMesmoComTimeoutsIntermitentesNoCore() {
        final MutableClock clock = new MutableClock(LocalDateTime.of(2026, 1, 1, 0, 0));
        final PipelineOrchestrator orchestrator = new PipelineOrchestrator(
            new ExponentialBackoffRetryPolicy(1, 10L, 1.0d, 0.0d, clock),
            (entidade, taxonomy) -> FailureMode.CONTINUE_WITH_ALERT,
            new CircuitBreaker(5, Duration.ofSeconds(120), clock),
            new ErrorClassifier(),
            noOpLogger(),
            new InMemoryPipelineMetrics()
        );
        final AtomicInteger chamadas = new AtomicInteger();

        final PipelineStep graphqlOscilante = new PipelineStep() {
            @Override
            public StepExecutionResult executar(final LocalDate dataInicio, final LocalDate dataFim) throws Exception {
                final int tentativa = chamadas.incrementAndGet();
                if (tentativa % 2 == 0) {
                    Thread.sleep(250L);
                    return StepExecutionResult.builder("graphql:core", "graphql")
                        .status(StepStatus.SUCCESS)
                        .startedAt(LocalDateTime.now())
                        .finishedAt(LocalDateTime.now().plusNanos(250_000_000L))
                        .build();
                }
                Thread.sleep(1_000L);
                return StepExecutionResult.builder("graphql:core", "graphql")
                    .status(StepStatus.SUCCESS)
                    .startedAt(LocalDateTime.now())
                    .finishedAt(LocalDateTime.now().plusSeconds(1))
                    .build();
            }

            @Override
            public String obterNomeEtapa() {
                return "graphql:core";
            }

            @Override
            public String obterNomeEntidade() {
                return "graphql";
            }

            @Override
            public Duration obterTimeoutExecucao() {
                return Duration.ofMillis(150);
            }
        };

        final PipelineStep dataExportSaudavel = new PipelineStep() {
            @Override
            public StepExecutionResult executar(final LocalDate dataInicio, final LocalDate dataFim) {
                return StepExecutionResult.builder("dataexport:core", "dataexport")
                    .status(StepStatus.SUCCESS)
                    .startedAt(LocalDateTime.now())
                    .finishedAt(LocalDateTime.now().plusNanos(50_000_000L))
                    .build();
            }

            @Override
            public String obterNomeEtapa() {
                return "dataexport:core";
            }

            @Override
            public String obterNomeEntidade() {
                return "dataexport";
            }
        };

        for (int i = 0; i < 4; i++) {
            final long inicioMs = System.currentTimeMillis();
            final PipelineReport report = orchestrator.executar(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 2),
                List.of(graphqlOscilante, dataExportSaudavel)
            );
            final long duracaoMs = System.currentTimeMillis() - inicioMs;

            assertEquals(2, report.getResultados().size());
            assertEquals(StepStatus.SUCCESS, report.getResultados().get(1).getStatus());
            assertEquals(false, report.isAborted());
            assertEquals(true, duracaoMs < 2_000);
        }
    }

    private ExtractionLoggerPort noOpLogger() {
        return (eventName, fields) -> { };
    }

    private static final class MutableClock implements ClockPort {
        private LocalDateTime now;

        private MutableClock(final LocalDateTime now) {
            this.now = now;
        }

        @Override
        public LocalDate hoje() {
            return now.toLocalDate();
        }

        @Override
        public LocalDateTime agora() {
            return now;
        }

        @Override
        public void dormir(final Duration duration) {
            now = now.plus(duration);
        }
    }
}

