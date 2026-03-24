package br.com.extrator.aplicacao.validacao;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ValidacaoEtlResilienciaUseCaseTest {

    @TempDir
    Path tempDir;

    @Test
    void deveGerarRelatorioDeResilienciaComStatusFinalAprovado() throws Exception {
        final ValidacaoEtlResilienciaUseCase useCase = new ValidacaoEtlResilienciaUseCase(tempDir.resolve("logs"));

        final ValidacaoEtlResilienciaUseCase.ReportFiles files = useCase.executar(
            new ValidacaoEtlResilienciaRequest(
                true,
                4,
                Duration.ofSeconds(10),
                4,
                1234L,
                true
            )
        );

        assertTrue(Files.exists(files.json()));
        assertTrue(Files.exists(files.markdown()));

        final String markdown = Files.readString(files.markdown(), StandardCharsets.UTF_8);
        assertTrue(markdown.contains("SISTEMA RESILIENTE"));
        assertTrue(markdown.contains("AUTO_CHAOS_LOOP"));
        assertTrue(markdown.contains("WATCHDOG_GLOBAL"));
    }
}
