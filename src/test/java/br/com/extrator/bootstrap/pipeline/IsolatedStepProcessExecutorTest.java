package br.com.extrator.bootstrap.pipeline;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URISyntaxException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.extrator.suporte.concorrencia.ExecutionTimeoutException;

class IsolatedStepProcessExecutorTest {

    @Test
    void deveEncerrarProcessoFilhoTravadoQuandoTimeoutExpira() {
        final IsolatedStepProcessExecutor executor = new HangingCommandExecutor();
        final long inicioMs = System.currentTimeMillis();

        final ExecutionTimeoutException erro = assertThrows(
            ExecutionTimeoutException.class,
            () -> executor.executar(
                IsolatedStepProcessExecutor.ApiType.GRAPHQL,
                LocalDate.of(2026, 3, 18),
                LocalDate.of(2026, 3, 18),
                "all",
                Duration.ofMillis(1_500L)
            )
        );

        final long duracaoMs = System.currentTimeMillis() - inicioMs;
        assertTrue(duracaoMs < 10_000L, "Processo filho travado deve ser abortado em tempo finito");
        assertTrue(erro.getMessage().contains("timeout") || erro.getMessage().contains("excedeu"));
    }

    private static final class HangingCommandExecutor extends IsolatedStepProcessExecutor {
        @Override
        protected List<String> construirComando(final ApiType apiType,
                                                final LocalDate dataInicio,
                                                final LocalDate dataFim,
                                                final String entidade,
                                                final FaultMode faultMode) throws URISyntaxException {
            return List.of(
                "powershell",
                "-NoProfile",
                "-Command",
                "while ($true) { Start-Sleep -Milliseconds 200 }"
            );
        }
    }
}
