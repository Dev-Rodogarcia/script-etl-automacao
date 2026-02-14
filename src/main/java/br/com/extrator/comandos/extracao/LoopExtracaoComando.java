package br.com.extrator.comandos.extracao;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import br.com.extrator.comandos.base.Comando;
import br.com.extrator.util.console.LoggerConsole;

public class LoopExtracaoComando implements Comando {
    // PROBLEMA #9: Console interativo - usa LoggerConsole para manter log duplo
    private static final LoggerConsole log = LoggerConsole.getLogger(LoopExtracaoComando.class);
    private volatile boolean running = false;
    private volatile boolean paused = false;
    private volatile boolean executing = false;
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> future;
    private volatile LocalDateTime nextRunAt;
    private final int intervalMinutes = 30;

    @Override
    public void executar(String[] args) throws Exception {
        log.console("=".repeat(80));
        log.info("🕒 LOOP DE EXTRACAO A CADA 30 MINUTOS");
        log.info("   Se uma tabela/entidade falhar, será reextraída na próxima execução (em 30 min).");
        log.console("=".repeat(80));
        imprimirStatus();
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                log.console("");
                log.console("[I] Iniciar  [P] Pausar  [R] Retomar  [S] Parar  [T] Status  [X] Sair");
                System.out.print("Opcao: "); // System.out.print para prompt inline
                String opcao = scanner.nextLine().trim();
                if ("I".equalsIgnoreCase(opcao)) {
                    iniciar();
                } else if ("N".equalsIgnoreCase(opcao)) {
                    executarCiclo();
                } else if ("P".equalsIgnoreCase(opcao)) {
                    pausar();
                } else if ("R".equalsIgnoreCase(opcao)) {
                    retomar();
                } else if ("S".equalsIgnoreCase(opcao)) {
                    parar();
                } else if ("T".equalsIgnoreCase(opcao)) {
                    imprimirStatus();
                } else if ("X".equalsIgnoreCase(opcao)) {
                    parar();
                    log.info("Encerrando console de loop.");
                    break;
                } else {
                    log.warn("Opcao invalida");
                }
            }
        }
    }

    private void iniciar() {
        if (running) {
            if (paused) {
                paused = false;
                log.info("Loop retomado e iniciando nova extracao imediata");
                executarCiclo();
            } else {
                log.info("Loop ja esta ativo - iniciando nova extracao imediata");
                executarCiclo();
            }
        } else {
            scheduler = Executors.newSingleThreadScheduledExecutor();
            running = true;
            paused = false;
            future = scheduler.scheduleWithFixedDelay(() -> executarCiclo(), 0, intervalMinutes, TimeUnit.MINUTES);
            log.info("Loop iniciado (intervalo: 30 minutos)");
            imprimirStatus();
        }
    }

    private void pausar() {
        if (!running) {
            log.warn("Loop nao esta ativo");
            return;
        }
        if (paused) {
            log.warn("Loop ja esta pausado");
            return;
        }
        paused = true;
        log.info("Loop pausado");
        imprimirStatus();
    }

    private void retomar() {
        if (!running) {
            log.warn("Loop nao esta ativo");
            return;
        }
        if (!paused) {
            log.warn("Loop nao esta pausado");
            return;
        }
        paused = false;
        log.info("Loop retomado");
        imprimirStatus();
    }

    private void parar() {
        if (!running) {
            log.warn("Loop nao esta ativo");
            return;
        }
        running = false;
        paused = false;
        if (future != null) {
            future.cancel(true);
        }
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        log.info("Loop parado");
        imprimirStatus();
    }

    private void imprimirStatus() {
        String status = running ? (paused ? "PAUSADO" : "ATIVO") : "INATIVO";
        log.info("Status atual: {}", status);
        if (nextRunAt != null) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            LocalDateTime agora = LocalDateTime.now();
            Duration restante = Duration.between(agora, nextRunAt);
            long mm = Math.max(0, restante.toMinutes());
            long ss = Math.max(0, restante.minusMinutes(mm).toSeconds());
            log.info("Proxima execucao: {} (em {}m {}s)", nextRunAt.format(fmt), mm, ss);
        } else {
            log.info("Proxima execucao: indefinida");
        }
    }

    private void executarCiclo() {
        if (paused) {
            return;
        }
        if (executing) {
            log.warn("Uma extracao ja esta em execucao");
            return;
        }
        executing = true;
        final LocalDateTime inicio = LocalDateTime.now();
        log.info("Iniciando extracao completa...");
        try {
            final br.com.extrator.servicos.LoggingService ls = new br.com.extrator.servicos.LoggingService();
            ls.iniciarCaptura("extracao_dados_loop");
            String statusExecucao = "SUCCESS";
            try {
                new ExecutarFluxoCompletoComando().executar(new String[] { "--fluxo-completo" });
            } catch (Exception e) {
                statusExecucao = "ERROR";
                throw e;
            } finally {
                ls.pararCaptura(statusExecucao);
            }
        } catch (Exception e) {
            log.error("Falha ao executar extracao: {}", e.getMessage());
        } finally {
            executing = false;
        }
        LocalDateTime fim = LocalDateTime.now();
        Duration duracao = Duration.between(inicio, fim);
        long dm = Math.max(0, duracao.toMinutes());
        long ds = Math.max(0, duracao.minusMinutes(dm).toSeconds());
        nextRunAt = fim.plusMinutes(intervalMinutes);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        Duration restante = Duration.between(fim, nextRunAt);
        long mm = Math.max(0, restante.toMinutes());
        long ss = Math.max(0, restante.minusMinutes(mm).toSeconds());
        log.info("Extracao concluida em {} (duracao {}m {}s) | Proxima em {} (em {}m {}s)", 
            fim.format(fmt), dm, ds, nextRunAt.format(fmt), mm, ss);
        log.console("Durante a espera: [N] Nova imediata  [P] Pausar  [S] Parar  [T] Status  [X] Parar/Sair");
    }
}
