package br.com.extrator.comandos.cli.extracao.daemon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.com.extrator.aplicacao.extracao.ExecutionLockBusyException;
import br.com.extrator.aplicacao.materializacao.FatoMaterializacaoProcedureResultado;
import br.com.extrator.aplicacao.materializacao.FatoMaterializacaoResumo;
import br.com.extrator.comandos.cli.extracao.reconciliacao.LoopReconciliationService;
import br.com.extrator.comandos.cli.extracao.reconciliacao.LoopReconciliationService.ReconciliationSummary;

class LoopDaemonRunHandlerTest {

    @TempDir
    Path tempDir;

    @Test
    void deveExecutarNovoCicloQuandoReceberForceRun() throws Exception {
        final DaemonStateStore stateStore = novoStore();
        final DaemonHistoryWriter historyWriter = novoHistoryWriter();
        final AtomicInteger ciclosExecutados = new AtomicInteger();
        final AtomicInteger chamadaEspera = new AtomicInteger();

        final LoopDaemonRunHandler handler = new LoopDaemonRunHandler(
            stateStore,
            historyWriter,
            () -> ciclosExecutados.incrementAndGet(),
            (inicio, fimExtracao, sucesso, detalheFalha) -> null,
            (proximoCiclo, store) -> chamadaEspera.getAndIncrement() == 0
                ? LoopDaemonRunHandler.WaitResult.FORCE_RUN_REQUESTED
                : LoopDaemonRunHandler.WaitResult.STOP_REQUESTED,
            cicloLog -> () -> { },
            () -> 9876L,
            30L,
            false,
            () -> java.time.Duration.ofSeconds(30)
        );

        handler.executar();

        assertEquals(2, ciclosExecutados.get(), "Force run deve disparar um segundo ciclo imediato");
        assertFalse(Files.exists(stateStore.getPidFile()), "PID deve ser limpo ao finalizar");
        assertFalse(Files.exists(stateStore.getStopFile()), "Stop file deve ser limpo ao finalizar");
        assertFalse(Files.exists(stateStore.getForceRunFile()), "Force run file deve ser limpo ao finalizar");
        assertEquals("STOPPED", stateStore.loadState().getProperty("status"), "Estado final deve ser STOPPED");
    }

    @Test
    void deveMaterializarFatosBiDepoisDeFluxoBemSucedido() throws Exception {
        final DaemonStateStore stateStore = novoStore();
        final DaemonHistoryWriter historyWriter = novoHistoryWriter();
        final AtomicInteger fluxosExecutados = new AtomicInteger();
        final AtomicInteger materializacoes = new AtomicInteger();

        final LoopDaemonRunHandler handler = new LoopDaemonRunHandler(
            stateStore,
            historyWriter,
            () -> fluxosExecutados.incrementAndGet(),
            () -> {
                materializacoes.incrementAndGet();
                return new FatoMaterializacaoResumo(List.of(), Duration.ofMillis(15));
            },
            (inicio, fimExtracao, sucesso, detalheFalha) -> null,
            (proximoCiclo, store) -> LoopDaemonRunHandler.WaitResult.STOP_REQUESTED,
            cicloLog -> () -> { },
            () -> 24680L,
            30L,
            false,
            () -> java.time.Duration.ofSeconds(30)
        );

        handler.executar();

        final Path logCiclo = localizarPrimeiroLogCiclo(tempDir.resolve("daemon").resolve("ciclos"));
        final String conteudo = Files.readString(logCiclo, StandardCharsets.UTF_8);

        assertEquals(1, fluxosExecutados.get());
        assertEquals(1, materializacoes.get());
        assertTrue(conteudo.contains("materializacao_bi"), "Resumo do ciclo deve registrar materializacao BI");
    }

    @Test
    void falhaParcialDeMaterializacaoDeveSerRegistradaComoAlertaDoCiclo() throws Exception {
        final DaemonStateStore stateStore = novoStore();
        final DaemonHistoryWriter historyWriter = novoHistoryWriter();
        final AtomicInteger fluxosExecutados = new AtomicInteger();

        final LoopDaemonRunHandler handler = new LoopDaemonRunHandler(
            stateStore,
            historyWriter,
            () -> fluxosExecutados.incrementAndGet(),
            () -> new FatoMaterializacaoResumo(
                List.of(
                    new FatoMaterializacaoProcedureResultado(
                        "dbo.sp_ok",
                        1,
                        2,
                        LocalDateTime.now(),
                        Duration.ofMillis(10)
                    ),
                    FatoMaterializacaoProcedureResultado.falha(
                        "dbo.sp_bloqueada",
                        new IllegalStateException("lock ocupado"),
                        Duration.ofMillis(5)
                    )
                ),
                Duration.ofMillis(15)
            ),
            (inicio, fimExtracao, sucesso, detalheFalha) -> null,
            (proximoCiclo, store) -> LoopDaemonRunHandler.WaitResult.STOP_REQUESTED,
            cicloLog -> () -> { },
            () -> 24681L,
            30L,
            false,
            () -> java.time.Duration.ofSeconds(30)
        );

        handler.executar();

        final Path logCiclo = localizarPrimeiroLogCiclo(tempDir.resolve("daemon").resolve("ciclos"));
        final String conteudo = Files.readString(logCiclo, StandardCharsets.UTF_8);

        assertEquals(1, fluxosExecutados.get());
        assertTrue(conteudo.contains("alerta de materializacao BI"));
        assertTrue(conteudo.contains("falhas=1"));
        assertTrue(conteudo.contains("procedures_falhas="));
        assertTrue(conteudo.contains("sp_bloqueada"));
    }

    @Test
    void deveTratarFalhaDeIntegridadeSemInterromperLoop() throws Exception {
        final DaemonStateStore stateStore = novoStore();
        final DaemonHistoryWriter historyWriter = novoHistoryWriter();

        final LoopDaemonRunHandler handler = new LoopDaemonRunHandler(
            stateStore,
            historyWriter,
            () -> {
                throw new RuntimeException(LoopDaemonHandlerSupport.MENSAGEM_FALHA_INTEGRIDADE + " em teste");
            },
            (inicio, fimExtracao, sucesso, detalheFalha) -> null,
            (proximoCiclo, store) -> LoopDaemonRunHandler.WaitResult.STOP_REQUESTED,
            cicloLog -> () -> { },
            () -> 4321L,
            30L,
            false,
            () -> java.time.Duration.ofSeconds(30)
        );

        handler.executar();

        final Path logCiclo = localizarPrimeiroLogCiclo(tempDir.resolve("daemon").resolve("ciclos"));
        final String conteudo = Files.readString(logCiclo, StandardCharsets.UTF_8);
        assertTrue(
            conteudo.contains("Ciclo concluido com alerta de integridade"),
            "Resumo do ciclo deve registrar alerta de integridade"
        );
    }

    @Test
    void deveClassificarStatusComFalhaDeReconciliacao() {
        final LoopReconciliationService service = new LoopReconciliationService(
            tempDir.resolve("reconciliacao.state"),
            Clock.systemDefaultZone(),
            true,
            1,
            0,
            (data, api, entidade) -> CompletableFuture.failedFuture(new IllegalStateException("falha simulada"))
        );

        final ReconciliationSummary resumoComFalha = service.processarPosCiclo(
            LocalDateTime.now().minusMinutes(10),
            LocalDateTime.now(),
            true,
            null
        );

        assertEquals(
            "WAITING_NEXT_CYCLE_WITH_ERROR",
            LoopDaemonRunHandler.determinarStatusDaemon(true, resumoComFalha)
        );
        assertEquals(
            "WAITING_NEXT_CYCLE_WITH_ERROR",
            LoopDaemonRunHandler.determinarStatusDaemon(true, true, null)
        );
        assertEquals("WAITING_NEXT_CYCLE", LoopDaemonRunHandler.determinarStatusDaemon(true, null));
    }

    @Test
    void deveEncerrarCicloQuandoFluxoExcedeTimeoutGlobal() throws Exception {
        final DaemonStateStore stateStore = novoStore();
        final DaemonHistoryWriter historyWriter = novoHistoryWriter();

        final LoopDaemonRunHandler handler = new LoopDaemonRunHandler(
            stateStore,
            historyWriter,
            () -> {
                while (true) {
                    try {
                        Thread.sleep(1_000L);
                    } catch (final InterruptedException ignored) {
                        // Simula fluxo travado que ignora interrupcao.
                    }
                }
            },
            (inicio, fimExtracao, sucesso, detalheFalha) -> null,
            (proximoCiclo, store) -> LoopDaemonRunHandler.WaitResult.STOP_REQUESTED,
            cicloLog -> () -> { },
            () -> 2468L,
            30L,
            false,
            () -> java.time.Duration.ofMillis(200)
        );

        final long inicioMs = System.currentTimeMillis();
        handler.executar();
        final long duracaoMs = System.currentTimeMillis() - inicioMs;

        final Path logCiclo = localizarPrimeiroLogCiclo(tempDir.resolve("daemon").resolve("ciclos"));
        final String conteudo = Files.readString(logCiclo, StandardCharsets.UTF_8);

        assertTrue(duracaoMs < 4_000, "Watchdog global deve impedir hang indefinido do daemon");
        assertTrue(conteudo.toLowerCase().contains("timeout"), "Resumo do ciclo deve registrar timeout global");
    }

    @Test
    void devePararEmIntervencaoManualAposTresAlertasConsecutivos() throws Exception {
        final DaemonStateStore stateStore = novoStore();
        final DaemonHistoryWriter historyWriter = novoHistoryWriter();
        final AtomicInteger ciclosExecutados = new AtomicInteger();

        final String propriedadeAnterior = System.getProperty("loop.daemon.max_consecutive_alert_cycles");
        try {
            System.setProperty("loop.daemon.max_consecutive_alert_cycles", "3");
            final LoopDaemonRunHandler handler = new LoopDaemonRunHandler(
                stateStore,
                historyWriter,
                () -> {
                    ciclosExecutados.incrementAndGet();
                    throw new RuntimeException(LoopDaemonHandlerSupport.MENSAGEM_FALHA_INTEGRIDADE + " em serie");
                },
                (inicio, fimExtracao, sucesso, detalheFalha) -> null,
                (proximoCiclo, store) -> LoopDaemonRunHandler.WaitResult.TIME_ELAPSED,
                cicloLog -> () -> { },
                () -> 97531L,
                30L,
                false,
                () -> java.time.Duration.ofSeconds(30)
            );

            handler.executar();

            final var estado = stateStore.loadState();
            assertEquals(3, ciclosExecutados.get());
            assertEquals("WAITING_MANUAL_INTERVENTION", estado.getProperty("status"));
            assertEquals("3", estado.getProperty("consecutive_alert_cycles"));
            assertEquals("3", estado.getProperty("consecutive_non_success_cycles"));
        } finally {
            if (propriedadeAnterior == null) {
                System.clearProperty("loop.daemon.max_consecutive_alert_cycles");
            } else {
                System.setProperty("loop.daemon.max_consecutive_alert_cycles", propriedadeAnterior);
            }
        }
    }

    @Test
    void lockOcupadoNaoDeveContarComoFalhaDegradanteDoDaemon() throws Exception {
        final DaemonStateStore stateStore = novoStore();
        final DaemonHistoryWriter historyWriter = novoHistoryWriter();
        final AtomicInteger reconciliacoes = new AtomicInteger();

        final String propriedadeAnterior = System.getProperty("loop.daemon.max_consecutive_alert_cycles");
        try {
            System.setProperty("loop.daemon.max_consecutive_alert_cycles", "1");
            final LoopDaemonRunHandler handler = new LoopDaemonRunHandler(
                stateStore,
                historyWriter,
                () -> {
                    throw new ExecutionLockBusyException("etl-global-execution", -1);
                },
                (inicio, fimExtracao, sucesso, detalheFalha) -> {
                    reconciliacoes.incrementAndGet();
                    return null;
                },
                (proximoCiclo, store) -> LoopDaemonRunHandler.WaitResult.STOP_REQUESTED,
                cicloLog -> () -> { },
                () -> 8642L,
                30L,
                false,
                () -> java.time.Duration.ofSeconds(30)
            );

            handler.executar();

            final var estado = stateStore.loadState();
            final Path logCiclo = localizarPrimeiroLogCiclo(tempDir.resolve("daemon").resolve("ciclos"));
            final String conteudo = Files.readString(logCiclo, StandardCharsets.UTF_8);

            assertEquals("STOPPED", estado.getProperty("status"));
            assertEquals("0", estado.getProperty("consecutive_alert_cycles"));
            assertEquals("0", estado.getProperty("consecutive_non_success_cycles"));
            assertEquals(0, reconciliacoes.get(), "Reconciliacao nao deve rodar enquanto o lock global esta ocupado");
            assertTrue(conteudo.contains("Ciclo pulado porque outra execucao esta segurando o lock global"));
        } finally {
            if (propriedadeAnterior == null) {
                System.clearProperty("loop.daemon.max_consecutive_alert_cycles");
            } else {
                System.setProperty("loop.daemon.max_consecutive_alert_cycles", propriedadeAnterior);
            }
        }
    }

    private DaemonStateStore novoStore() {
        return new DaemonStateStore(
            tempDir.resolve("daemon"),
            tempDir.resolve("daemon").resolve("loop_daemon.state"),
            tempDir.resolve("daemon").resolve("loop_daemon.pid"),
            tempDir.resolve("daemon").resolve("loop_daemon.stop"),
            tempDir.resolve("daemon").resolve("loop_daemon.force_run")
        );
    }

    private DaemonHistoryWriter novoHistoryWriter() {
        return new DaemonHistoryWriter(
            tempDir.resolve("daemon"),
            tempDir.resolve("daemon").resolve("ciclos"),
            tempDir.resolve("daemon").resolve("history"),
            tempDir.resolve("daemon").resolve("reconciliacao"),
            "extrator.loop.reconciliacao.history.dir",
            false
        );
    }

    private Path localizarPrimeiroLogCiclo(final Path cyclesDir) throws Exception {
        try (var stream = Files.walk(cyclesDir)) {
            final List<Path> logs = stream
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith(".log"))
                .toList();
            assertFalse(logs.isEmpty(), "Deve existir pelo menos um log de ciclo");
            return logs.get(0);
        }
    }
}
