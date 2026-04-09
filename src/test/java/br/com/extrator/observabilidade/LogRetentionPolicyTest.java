package br.com.extrator.observabilidade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LogRetentionPolicyTest {

    @TempDir
    Path tempDir;

    @Test
    void deveManterSomenteOsVinteArquivosMaisRecentes() throws Exception {
        for (int i = 1; i <= 25; i++) {
            final Path arquivo = tempDir.resolve(String.format("log_%02d.log", i));
            Files.writeString(arquivo, "log-" + i);
            Files.setLastModifiedTime(arquivo, FileTime.from(Instant.parse("2026-04-09T10:00:00Z").plusSeconds(i)));
        }

        LogRetentionPolicy.retainRecentFiles(tempDir, 20, path -> LogRetentionPolicy.hasExtension(path, ".log"));

        try (var stream = Files.list(tempDir)) {
            final List<String> nomes = stream
                .filter(Files::isRegularFile)
                .map(path -> path.getFileName().toString())
                .sorted()
                .toList();
            assertEquals(20, nomes.size(), "A pasta deve manter no maximo 20 logs");
            assertFalse(nomes.contains("log_01.log"), "O log mais antigo deve ser removido");
            assertFalse(nomes.contains("log_05.log"), "Os cinco logs mais antigos devem ser removidos");
            assertTrue(nomes.contains("log_25.log"), "O log mais recente deve permanecer");
        }
    }

    @Test
    void deveAplicarRetencaoRecursivaERemoverPastasDeCicloVazias() throws Exception {
        final Path diaAntigo = tempDir.resolve("2026-04-07");
        final Path diaIntermediario = tempDir.resolve("2026-04-08");
        final Path diaAtual = tempDir.resolve("2026-04-09");
        Files.createDirectories(diaAntigo);
        Files.createDirectories(diaIntermediario);
        Files.createDirectories(diaAtual);

        criarLog(diaAntigo.resolve("extracao_daemon_2026-04-07_09-00-00.log"), 1);
        for (int i = 0; i < 10; i++) {
            criarLog(diaIntermediario.resolve(String.format("extracao_daemon_2026-04-08_10-%02d-00.log", i)), i + 10);
            criarLog(diaAtual.resolve(String.format("extracao_daemon_2026-04-09_11-%02d-00.log", i)), i + 20);
        }

        LogRetentionPolicy.retainRecentFilesRecursively(
            tempDir,
            20,
            path -> LogRetentionPolicy.hasExtension(path, ".log")
        );

        try (var stream = Files.walk(tempDir)) {
            final List<Path> arquivos = stream
                .filter(Files::isRegularFile)
                .sorted(Comparator.naturalOrder())
                .toList();
            assertEquals(20, arquivos.size(), "A arvore deve manter no maximo 20 logs");
        }
        assertFalse(Files.exists(diaAntigo), "Pastas de ciclo esvaziadas devem ser removidas");
        assertTrue(Files.exists(diaAtual), "Pastas com logs recentes devem permanecer");
    }

    private void criarLog(final Path arquivo, final int secondsOffset) throws Exception {
        Files.writeString(arquivo, arquivo.getFileName().toString());
        Files.setLastModifiedTime(
            arquivo,
            FileTime.from(Instant.parse("2026-04-09T10:00:00Z").plusSeconds(secondsOffset))
        );
    }
}
