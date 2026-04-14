package br.com.extrator.aplicacao.extracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

class ReferentialBackfillBacklogServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void deveMesclarJanelasPendentesSemPerderExtremos() {
        final ReferentialBackfillBacklogService service = novoService();

        service.mergePending(LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 20), "primeira");
        service.mergePending(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 15), "segunda");

        final var backlog = service.loadPending();
        assertTrue(backlog.isPresent());
        assertEquals(LocalDate.of(2026, 1, 1), backlog.get().inicio());
        assertEquals(LocalDate.of(2026, 1, 20), backlog.get().fim());
        assertEquals("segunda", backlog.get().reason());
    }

    @Test
    void deveAvancarBacklogAteLimparArquivoQuandoConsumido() {
        final Path stateFile = tempDir.resolve("coletas_backfill.properties");
        final ReferentialBackfillBacklogService service = novoService(stateFile);

        service.mergePending(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 5), "teste");
        service.markProcessedUntil(LocalDate.of(2026, 1, 3));

        final var parcial = service.loadPending();
        assertTrue(parcial.isPresent());
        assertEquals(LocalDate.of(2026, 1, 4), parcial.get().inicio());
        assertEquals(LocalDate.of(2026, 1, 5), parcial.get().fim());

        service.markProcessedUntil(LocalDate.of(2026, 1, 5));

        assertTrue(service.loadPending().isEmpty());
        assertFalse(Files.exists(stateFile));
    }

    private ReferentialBackfillBacklogService novoService() {
        return novoService(tempDir.resolve("coletas_backfill.properties"));
    }

    private ReferentialBackfillBacklogService novoService(final Path stateFile) {
        return new ReferentialBackfillBacklogService(
            stateFile,
            Clock.fixed(Instant.parse("2026-04-13T03:00:00Z"), ZoneOffset.UTC),
            LoggerFactory.getLogger(ReferentialBackfillBacklogServiceTest.class)
        );
    }
}
