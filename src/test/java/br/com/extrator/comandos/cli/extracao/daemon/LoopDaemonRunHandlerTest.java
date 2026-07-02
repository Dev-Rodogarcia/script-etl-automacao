package br.com.extrator.comandos.cli.extracao.daemon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

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
            LoopDaemonHandlerSupport.INTERVALO_MINUTOS_FALLBACK,
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
    void deveAgendarProximoCicloComIntervaloDeSessentaMinutos() throws Exception {
        final DaemonStateStore stateStore = novoStore();
        final DaemonHistoryWriter historyWriter = novoHistoryWriter();
        final AtomicReference<LocalDateTime> proximoCapturado = new AtomicReference<>();

        final LoopDaemonRunHandler handler = new LoopDaemonRunHandler(
            stateStore,
            historyWriter,
            () -> { },
            (inicio, fimExtracao, sucesso, detalheFalha) -> null,
            (proximoCiclo, store) -> {
                proximoCapturado.set(proximoCiclo);
                return LoopDaemonRunHandler.WaitResult.STOP_REQUESTED;
            },
            cicloLog -> () -> { },
            () -> 9877L,
            LoopDaemonHandlerSupport.INTERVALO_MINUTOS_FALLBACK,
            false,
            () -> java.time.Duration.ofSeconds(30)
        );

        final LocalDateTime antes = LocalDateTime.now();
        handler.executar();
        final LocalDateTime depois = LocalDateTime.now();

        assertFalse(
            proximoCapturado.get().isBefore(antes.plusMinutes(60)),
            "Proximo ciclo deve respeitar o fallback de 60 minutos"
        );
        assertTrue(
            proximoCapturado.get().isBefore(depois.plusMinutes(60).plusSeconds(2)),
            "Proximo ciclo deve ser calculado a partir do fim do ciclo atual"
        );
    }

    @Test
    void deveExecutarFechamentoMensalNoDiaPrimeiroAntesDoFluxoIntradiaERegistrarSucesso() throws Exception {
        final DaemonStateStore stateStore = novoStore();
        final DaemonHistoryWriter historyWriter = novoHistoryWriter();
        final Path fechamentoState = tempDir.resolve("daemon").resolve("fechamento_mensal.state");
        final List<String> ordem = new ArrayList<>();
        final AtomicReference<LocalDate> dataInicioCapturada = new AtomicReference<>();
        final AtomicReference<LocalDate> dataFimCapturada = new AtomicReference<>();

        final LoopDaemonRunHandler handler = new LoopDaemonRunHandler(
            stateStore,
            historyWriter,
            () -> ordem.add("intradia"),
            () -> null,
            (inicio, fimExtracao, sucesso, detalheFalha) -> null,
            (proximoCiclo, store) -> LoopDaemonRunHandler.WaitResult.STOP_REQUESTED,
            cicloLog -> () -> { },
            () -> 13579L,
            LoopDaemonHandlerSupport.INTERVALO_MINUTOS_FALLBACK,
            false,
            () -> java.time.Duration.ofSeconds(30),
            (dataInicio, dataFim) -> {
                ordem.add("mensal");
                dataInicioCapturada.set(dataInicio);
                dataFimCapturada.set(dataFim);
            },
            () -> LocalDate.of(2026, 7, 1),
            fechamentoState
        );

        handler.executar();

        final Properties estadoFechamento = carregarProperties(fechamentoState);
        assertEquals(List.of("mensal", "intradia"), ordem);
        assertEquals(LocalDate.of(2026, 6, 1), dataInicioCapturada.get());
        assertEquals(LocalDate.of(2026, 6, 30), dataFimCapturada.get());
        assertEquals("2026-06", estadoFechamento.getProperty("last_attempted_competencia"));
        assertEquals("SUCCESS", estadoFechamento.getProperty("status"));
    }

    @Test
    void deveIgnorarFechamentoMensalJaMarcadoComoSuccessOuRunningNaMesmaCompetencia() throws Exception {
        for (final String statusJaRegistrado : List.of("SUCCESS", "RUNNING")) {
            final Path baseDir = tempDir.resolve("daemon-" + statusJaRegistrado.toLowerCase());
            final DaemonStateStore stateStore = novoStore(baseDir);
            final DaemonHistoryWriter historyWriter = novoHistoryWriter(baseDir);
            final Path fechamentoState = baseDir.resolve("fechamento_mensal.state");
            final Properties estadoInicial = new Properties();
            estadoInicial.setProperty("last_attempted_competencia", "2026-06");
            estadoInicial.setProperty("status", statusJaRegistrado);
            salvarProperties(fechamentoState, estadoInicial);

            final AtomicInteger fechamentoExecutado = new AtomicInteger();
            final AtomicInteger intradiaExecutada = new AtomicInteger();

            final LoopDaemonRunHandler handler = new LoopDaemonRunHandler(
                stateStore,
                historyWriter,
                intradiaExecutada::incrementAndGet,
                () -> null,
                (inicio, fimExtracao, sucesso, detalheFalha) -> null,
                (proximoCiclo, store) -> LoopDaemonRunHandler.WaitResult.STOP_REQUESTED,
                cicloLog -> () -> { },
                () -> 13580L,
                LoopDaemonHandlerSupport.INTERVALO_MINUTOS_FALLBACK,
                false,
                () -> java.time.Duration.ofSeconds(30),
                (dataInicio, dataFim) -> fechamentoExecutado.incrementAndGet(),
                () -> LocalDate.of(2026, 7, 1),
                fechamentoState
            );

            handler.executar();

            final Properties estadoFinal = carregarProperties(fechamentoState);
            assertEquals(0, fechamentoExecutado.get(), "Nao deve repetir fechamento " + statusJaRegistrado);
            assertEquals(1, intradiaExecutada.get(), "Loop intradia deve continuar normalmente");
            assertEquals(statusJaRegistrado, estadoFinal.getProperty("status"));
        }
    }

    @Test
    void falhaNoFechamentoMensalDeveRegistrarErrorEContinuarFluxoIntradia() throws Exception {
        final DaemonStateStore stateStore = novoStore();
        final DaemonHistoryWriter historyWriter = novoHistoryWriter();
        final Path fechamentoState = tempDir.resolve("daemon").resolve("fechamento_mensal.state");
        final AtomicInteger intradiaExecutada = new AtomicInteger();

        final LoopDaemonRunHandler handler = new LoopDaemonRunHandler(
            stateStore,
            historyWriter,
            intradiaExecutada::incrementAndGet,
            () -> null,
            (inicio, fimExtracao, sucesso, detalheFalha) -> null,
            (proximoCiclo, store) -> LoopDaemonRunHandler.WaitResult.STOP_REQUESTED,
            cicloLog -> () -> { },
            () -> 13581L,
            LoopDaemonHandlerSupport.INTERVALO_MINUTOS_FALLBACK,
            false,
            () -> java.time.Duration.ofSeconds(30),
            (dataInicio, dataFim) -> {
                throw new IllegalStateException("falha mensal simulada");
            },
            () -> LocalDate.of(2026, 7, 1),
            fechamentoState
        );

        handler.executar();

        final Properties estadoFechamento = carregarProperties(fechamentoState);
        assertEquals(1, intradiaExecutada.get());
        assertEquals("2026-06", estadoFechamento.getProperty("last_attempted_competencia"));
        assertEquals("ERROR", estadoFechamento.getProperty("status"));
        assertTrue(estadoFechamento.getProperty("last_error").contains("falha mensal simulada"));
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
            LoopDaemonHandlerSupport.INTERVALO_MINUTOS_FALLBACK,
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
            LoopDaemonHandlerSupport.INTERVALO_MINUTOS_FALLBACK,
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
            LoopDaemonHandlerSupport.INTERVALO_MINUTOS_FALLBACK,
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
            LoopDaemonHandlerSupport.INTERVALO_MINUTOS_FALLBACK,
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
                LoopDaemonHandlerSupport.INTERVALO_MINUTOS_FALLBACK,
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
                LoopDaemonHandlerSupport.INTERVALO_MINUTOS_FALLBACK,
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
        return novoStore(tempDir.resolve("daemon"));
    }

    private DaemonStateStore novoStore(final Path baseDir) {
        return new DaemonStateStore(
            baseDir,
            baseDir.resolve("loop_daemon.state"),
            baseDir.resolve("loop_daemon.pid"),
            baseDir.resolve("loop_daemon.stop"),
            baseDir.resolve("loop_daemon.force_run")
        );
    }

    private DaemonHistoryWriter novoHistoryWriter() {
        return novoHistoryWriter(tempDir.resolve("daemon"));
    }

    private DaemonHistoryWriter novoHistoryWriter(final Path baseDir) {
        return new DaemonHistoryWriter(
            baseDir,
            baseDir.resolve("ciclos"),
            baseDir.resolve("history"),
            baseDir.resolve("reconciliacao"),
            "extrator.loop.reconciliacao.history.dir",
            false
        );
    }

    private Properties carregarProperties(final Path path) throws Exception {
        final Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        return properties;
    }

    private void salvarProperties(final Path path, final Properties properties) throws Exception {
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        try (var writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            properties.store(writer, "test-state");
        }
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
