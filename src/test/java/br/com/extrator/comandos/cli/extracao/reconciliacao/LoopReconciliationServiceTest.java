/* ==[DOC-FILE]===============================================================
Arquivo : src/test/java/br/com/extrator/comandos/extracao/reconciliacao/LoopReconciliationServiceTest.java
Classe  : LoopReconciliationServiceTest (class)
Pacote  : br.com.extrator.comandos.cli.extracao.reconciliacao
Modulo  : Teste automatizado
Papel   : Valida comportamento da unidade LoopReconciliationService.

Conecta com:
- Sem dependencia interna explicita (classe isolada ou foco em libs externas).

Fluxo geral:
1) Prepara cenarios e dados de teste.
2) Executa casos para validar comportamento de LoopReconciliationService.
3) Assegura regressao controlada nas regras principais.

Estrutura interna:
Metodos principais:
- setUp(): ajusta valor em estado interno.
- deveExecutarReconciliacaoDiariaDeOntem(): verifica comportamento esperado em teste automatizado.
- deveManterPendenciaQuandoReconciliacaoFalhaEExecutarNaRetentativa(): verifica comportamento esperado em teste automatizado.
- deveRespeitarLimiteMaximoPorCiclo(): verifica comportamento esperado em teste automatizado.
- deveAgendarDiasRetroativosEmFalhaDoCiclo(): verifica comportamento esperado em teste automatizado.
- deveRetornarResumoInativoQuandoFeatureDesativada(): verifica comportamento esperado em teste automatizado.
- salvarEstadoInicial(...2 args): persiste dados em armazenamento.
- carregarEstado(...1 args): realiza operacao relacionada a "carregar estado".
Atributos-chave:
- HOJE: campo de estado para "hoje".
- ONTEM: campo de estado para "ontem".
- stateFile: campo de estado para "state file".
- clock: campo de estado para "clock".
[DOC-FILE-END]============================================================== */

package br.com.extrator.comandos.cli.extracao.reconciliacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LoopReconciliationServiceTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 2, 20);
    private static final LocalDate ONTEM = HOJE.minusDays(1);

    @TempDir
    Path tempDir;

    private Path stateFile;
    private Clock clock;

    @BeforeEach
    void setUp() {
        this.stateFile = tempDir.resolve("loop_reconciliation.state");
        this.clock = Clock.fixed(
            LocalDateTime.of(2026, 2, 20, 10, 0).atZone(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault()
        );
    }

    @Test
    void deveExecutarReconciliacaoDiariaDeOntem() {
        final List<String> execucoes = new ArrayList<>();
        final LoopReconciliationService service = new LoopReconciliationService(
            stateFile,
            clock,
            true,
            20,
            1,
            (data, api, entidade) -> {
                execucoes.add(data + "|" + api + "|" + entidade);
                return sucesso();
            }
        );

        final var resumo = service.processarPosCiclo(
            LocalDateTime.of(2026, 2, 20, 9, 0),
            LocalDateTime.of(2026, 2, 20, 9, 30),
            true,
            null
        );

        assertTrue(resumo.isAtivo());
        assertEquals(11, resumo.getReconciliacoesExecutadas());
        assertEquals(0, resumo.getFalhas());
        assertEquals(11, execucoes.size());
        assertTrue(execucoes.stream().allMatch(execucao -> execucao.startsWith(ONTEM + "|")));
        assertTrue(execucoes.stream().noneMatch(execucao -> execucao.contains("|null")));
        assertTrue(execucoes.contains(ONTEM + "|graphql|fretes"));
        assertTrue(execucoes.contains(ONTEM + "|dataexport|localizacao_cargas"));
        assertTrue(resumo.getPendenciasRestantes().isEmpty());
        assertTrue(resumo.isAgendouReconciliacaoDiaria());
        assertFalse(resumo.isPendenciaPorFalha());

        final Properties estado = carregarEstado(stateFile);
        assertEquals(ONTEM.toString(), estado.getProperty("last_daily_scheduled_date"));
        assertEquals(ONTEM.toString(), estado.getProperty("last_successful_reconciliation_date"));
        assertEquals("", estado.getProperty("pending_dates"));
    }

    @Test
    void deveManterPendenciaQuandoReconciliacaoFalhaEExecutarNaRetentativa() {
        salvarEstadoInicial(ONTEM.toString(), "");

        final AtomicInteger tentativas = new AtomicInteger(0);
        final LoopReconciliationService service = new LoopReconciliationService(
            stateFile,
            clock,
            true,
            1,
            0,
            (data, api, entidade) -> {
                if (tentativas.incrementAndGet() == 1) {
                    return falha("falha simulada");
                }
                return sucesso();
            }
        );

        final var primeiroResumo = service.processarPosCiclo(
            LocalDateTime.of(2026, 2, 20, 10, 0),
            LocalDateTime.of(2026, 2, 20, 10, 30),
            false,
            "Fluxo completo concluido com falhas parciais. Runners falhados: DataExport/localizacao_cargas"
        );

        assertEquals(0, primeiroResumo.getReconciliacoesExecutadas());
        assertEquals(1, primeiroResumo.getFalhas());
        assertEquals(List.of(HOJE), primeiroResumo.getPendenciasRestantes());
        assertTrue(primeiroResumo.isPendenciaPorFalha());

        final var segundoResumo = service.processarPosCiclo(
            LocalDateTime.of(2026, 2, 20, 11, 0),
            LocalDateTime.of(2026, 2, 20, 11, 30),
            true,
            null
        );

        assertEquals(1, segundoResumo.getReconciliacoesExecutadas());
        assertEquals(0, segundoResumo.getFalhas());
        assertTrue(segundoResumo.getPendenciasRestantes().isEmpty());

        final Properties estado = carregarEstado(stateFile);
        assertEquals("", estado.getProperty("pending_dates"));
        assertEquals(HOJE.toString(), estado.getProperty("last_successful_reconciliation_date"));
    }

    @Test
    void deveRespeitarLimiteMaximoPorCiclo() {
        salvarEstadoInicial(
            ONTEM.toString(),
            "",
            "2026-02-17|dataexport|manifestos,2026-02-18|dataexport|manifestos,2026-02-19|dataexport|manifestos"
        );

        final List<LocalDate> datasExecutadas = new ArrayList<>();
        final LoopReconciliationService service = new LoopReconciliationService(
            stateFile,
            clock,
            true,
            2,
            0,
            (data, api, entidade) -> {
                datasExecutadas.add(data);
                return sucesso();
            }
        );

        final var resumo = service.processarPosCiclo(
            LocalDateTime.of(2026, 2, 20, 12, 0),
            LocalDateTime.of(2026, 2, 20, 12, 30),
            true,
            null
        );

        assertEquals(List.of(LocalDate.of(2026, 2, 17), LocalDate.of(2026, 2, 18)), datasExecutadas);
        assertEquals(2, resumo.getReconciliacoesExecutadas());
        assertEquals(0, resumo.getFalhas());
        assertEquals(List.of(LocalDate.of(2026, 2, 19)), resumo.getPendenciasRestantes());
    }

    @Test
    void deveAgendarDiasRetroativosEmFalhaDoCiclo() {
        salvarEstadoInicial(ONTEM.toString(), "");

        final LoopReconciliationService service = new LoopReconciliationService(
            stateFile,
            clock,
            true,
            1,
            2,
            (data, api, entidade) -> falha("falha simulada")
        );

        final var resumo = service.processarPosCiclo(
            LocalDateTime.of(2026, 2, 20, 0, 5),
            LocalDateTime.of(2026, 2, 20, 0, 35),
            false,
            "Fluxo completo concluido com falhas parciais. Runners falhados: DataExport/localizacao_cargas"
        );

        assertEquals(0, resumo.getReconciliacoesExecutadas());
        assertEquals(1, resumo.getFalhas());
        assertTrue(resumo.isPendenciaPorFalha());
        assertEquals(
            List.of(LocalDate.of(2026, 2, 18), LocalDate.of(2026, 2, 19), LocalDate.of(2026, 2, 20)),
            resumo.getPendenciasRestantes()
        );
    }

    @Test
    void naoDeveAgendarPendenciaGenericaQuandoFalhaNaoIdentificaEntidade() {
        salvarEstadoInicial(ONTEM.toString(), "");

        final AtomicInteger execucoes = new AtomicInteger(0);
        final LoopReconciliationService service = new LoopReconciliationService(
            stateFile,
            clock,
            true,
            1,
            2,
            (data, api, entidade) -> {
                execucoes.incrementAndGet();
                return sucesso();
            }
        );

        final var resumo = service.processarPosCiclo(
            LocalDateTime.of(2026, 2, 20, 0, 5),
            LocalDateTime.of(2026, 2, 20, 0, 35),
            false,
            "timeout HTTP 429 sem runner falhado identificavel"
        );

        assertEquals(0, execucoes.get());
        assertEquals(0, resumo.getReconciliacoesExecutadas());
        assertEquals(0, resumo.getFalhas());
        assertFalse(resumo.isPendenciaPorFalha());
        assertTrue(resumo.getPendenciasRestantes().isEmpty());

        final Properties estado = carregarEstado(stateFile);
        assertEquals("", estado.getProperty("pending_targets"));
        assertEquals("", estado.getProperty("pending_dates"));
    }

    @Test
    void naoDeveAgendarRunnerAgregadoSemEntidade() {
        salvarEstadoInicial(ONTEM.toString(), "");

        final AtomicInteger execucoes = new AtomicInteger(0);
        final LoopReconciliationService service = new LoopReconciliationService(
            stateFile,
            clock,
            true,
            1,
            0,
            (data, api, entidade) -> {
                execucoes.incrementAndGet();
                return sucesso();
            }
        );

        final var resumo = service.processarPosCiclo(
            LocalDateTime.of(2026, 2, 20, 6, 0),
            LocalDateTime.of(2026, 2, 20, 6, 15),
            false,
            "Fluxo completo concluido com falhas parciais. Runners falhados: DataExport"
        );

        assertEquals(0, execucoes.get());
        assertFalse(resumo.isPendenciaPorFalha());
        assertTrue(resumo.getPendenciasRestantes().isEmpty());
    }

    @Test
    void deveRetornarResumoInativoQuandoFeatureDesativada() {
        final LoopReconciliationService service = new LoopReconciliationService(
            stateFile,
            clock,
            false,
            2,
            1,
            (data, api, entidade) -> {
                throw new IllegalStateException("nao deveria executar");
            }
        );

        final var resumo = service.processarPosCiclo(
            LocalDateTime.of(2026, 2, 20, 9, 0),
            LocalDateTime.of(2026, 2, 20, 9, 30),
            false,
            null
        );

        assertFalse(resumo.isAtivo());
        assertEquals(0, resumo.getReconciliacoesExecutadas());
        assertEquals(0, resumo.getFalhas());
        assertTrue(resumo.getPendenciasRestantes().isEmpty());
        assertFalse(Files.exists(stateFile));
    }

    @Test
    void deveAgendarReconciliacaoSegmentadaQuandoFalhaInformarRunnerEspecifico() {
        salvarEstadoInicial(ONTEM.toString(), "");

        final List<String> execucoes = new ArrayList<>();
        final LoopReconciliationService service = new LoopReconciliationService(
            stateFile,
            clock,
            true,
            1,
            0,
            (data, api, entidade) -> {
                execucoes.add(data + "|" + api + "|" + entidade);
                return falha("falha segmentada");
            }
        );

        final var resumo = service.processarPosCiclo(
            LocalDateTime.of(2026, 2, 20, 6, 0),
            LocalDateTime.of(2026, 2, 20, 6, 15),
            false,
            "Fluxo completo concluido com falhas parciais. Runners falhados: GraphQL/fretes"
        );

        assertEquals(0, resumo.getReconciliacoesExecutadas());
        assertEquals(1, resumo.getFalhas());
        assertEquals(List.of(HOJE), resumo.getPendenciasRestantes());
        assertEquals(List.of("2026-02-20|graphql|fretes"), execucoes);

        final Properties estado = carregarEstado(stateFile);
        assertEquals("2026-02-20|graphql|fretes", estado.getProperty("pending_targets"));
        assertEquals(HOJE.toString(), estado.getProperty("pending_dates"));
    }

    @Test
    void deveAguardarCompletionStageAntesDeConcluirReconciliacao() throws Exception {
        salvarEstadoInicial(ONTEM.toString(), "", "2026-02-19|dataexport|manifestos");

        final CountDownLatch chamadaExecutor = new CountDownLatch(1);
        final CompletableFuture<Void> execucaoEmAndamento = new CompletableFuture<>();
        final LoopReconciliationService service = new LoopReconciliationService(
            stateFile,
            clock,
            true,
            1,
            0,
            (data, api, entidade) -> {
                chamadaExecutor.countDown();
                return execucaoEmAndamento;
            }
        );

        final ExecutorService worker = Executors.newSingleThreadExecutor();
        try {
            final Future<LoopReconciliationService.ReconciliationSummary> resultado =
                worker.submit(() -> service.processarPosCiclo(
                    LocalDateTime.of(2026, 2, 20, 10, 0),
                    LocalDateTime.of(2026, 2, 20, 10, 30),
                    true,
                    null
                ));

            assertTrue(chamadaExecutor.await(1, TimeUnit.SECONDS), "Executor de reconciliacao deve ser chamado");
            assertFalse(resultado.isDone(), "Loop nao deve concluir enquanto o CompletionStage estiver pendente");

            execucaoEmAndamento.complete(null);

            final var resumo = resultado.get(1, TimeUnit.SECONDS);
            assertEquals(1, resumo.getReconciliacoesExecutadas());
            assertEquals(0, resumo.getFalhas());
            assertTrue(resumo.getPendenciasRestantes().isEmpty());
            assertEquals("", carregarEstado(stateFile).getProperty("pending_targets"));
        } finally {
            execucaoEmAndamento.complete(null);
            worker.shutdownNow();
        }
    }

    private static CompletableFuture<Void> sucesso() {
        return CompletableFuture.completedFuture(null);
    }

    private static CompletableFuture<Void> falha(final String mensagem) {
        return CompletableFuture.failedFuture(new IllegalStateException(mensagem));
    }

    private void salvarEstadoInicial(final String lastDailyScheduledDate, final String pendingDates) {
        salvarEstadoInicial(lastDailyScheduledDate, pendingDates, "");
    }

    private void salvarEstadoInicial(final String lastDailyScheduledDate,
                                     final String pendingDates,
                                     final String pendingTargets) {
        final Properties properties = new Properties();
        properties.setProperty("last_daily_scheduled_date", lastDailyScheduledDate == null ? "" : lastDailyScheduledDate);
        properties.setProperty("last_successful_reconciliation_date", "");
        properties.setProperty("pending_dates", pendingDates == null ? "" : pendingDates);
        properties.setProperty("pending_targets", pendingTargets == null ? "" : pendingTargets);
        properties.setProperty("last_error", "");
        properties.setProperty("updated_at", "2026-02-20T00:00:00");

        try {
            final Path parent = stateFile.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            try (OutputStream out = Files.newOutputStream(stateFile)) {
                properties.store(out, "test-state");
            }
        } catch (final IOException e) {
            throw new RuntimeException("Falha ao preparar estado inicial para teste", e);
        }
    }

    private Properties carregarEstado(final Path path) {
        final Properties properties = new Properties();
        try (var in = Files.newInputStream(path)) {
            properties.load(in);
        } catch (final IOException e) {
            throw new RuntimeException("Falha ao carregar estado para teste", e);
        }
        return properties;
    }
}
