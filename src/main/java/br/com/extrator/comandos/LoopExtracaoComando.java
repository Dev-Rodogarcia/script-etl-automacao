package br.com.extrator.comandos;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class LoopExtracaoComando implements Comando {
    private volatile boolean running = false;
    private volatile boolean paused = false;
    private volatile boolean executing = false;
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> future;
    private volatile LocalDateTime nextRunAt;
    private final int intervalMinutes = 30;

    @Override
    public void executar(String[] args) throws Exception {
        System.out.println("=".repeat(80));
        System.out.println("🕒 LOOP DE EXTRACAO A CADA 30 MINUTOS");
        System.out.println("=".repeat(80));
        imprimirStatus();
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.println();
                System.out.println("[I] Iniciar  [P] Pausar  [R] Retomar  [S] Parar  [T] Status  [X] Sair");
                System.out.print("Opcao: ");
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
                    System.out.println("Encerrando console de loop.");
                    break;
                } else {
                    System.out.println("Opcao invalida");
                }
            }
        }
    }

    private void iniciar() {
        if (running) {
            if (paused) {
                paused = false;
                System.out.println("Loop retomado e iniciando nova extracao imediata");
                executarCiclo();
            } else {
                System.out.println("Loop ja esta ativo - iniciando nova extracao imediata");
                executarCiclo();
            }
        } else {
            scheduler = Executors.newSingleThreadScheduledExecutor();
            running = true;
            paused = false;
            future = scheduler.scheduleWithFixedDelay(() -> executarCiclo(), 0, intervalMinutes, TimeUnit.MINUTES);
            System.out.println("Loop iniciado (intervalo: 30 minutos)");
            imprimirStatus();
        }
    }

    private void pausar() {
        if (!running) {
            System.out.println("Loop nao esta ativo");
            return;
        }
        if (paused) {
            System.out.println("Loop ja esta pausado");
            return;
        }
        paused = true;
        System.out.println("Loop pausado");
        imprimirStatus();
    }

    private void retomar() {
        if (!running) {
            System.out.println("Loop nao esta ativo");
            return;
        }
        if (!paused) {
            System.out.println("Loop nao esta pausado");
            return;
        }
        paused = false;
        System.out.println("Loop retomado");
        imprimirStatus();
    }

    private void parar() {
        if (!running) {
            System.out.println("Loop nao esta ativo");
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
        System.out.println("Loop parado");
        imprimirStatus();
    }

    private void imprimirStatus() {
        String status = running ? (paused ? "PAUSADO" : "ATIVO") : "INATIVO";
        System.out.println("Status atual: " + status);
        if (nextRunAt != null) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            LocalDateTime agora = LocalDateTime.now();
            Duration restante = Duration.between(agora, nextRunAt);
            long mm = Math.max(0, restante.toMinutes());
            long ss = Math.max(0, restante.minusMinutes(mm).toSeconds());
            System.out.println("Proxima execucao: " + nextRunAt.format(fmt) + " (em " + mm + "m " + ss + "s)");
        } else {
            System.out.println("Proxima execucao: indefinida");
        }
    }

    private void executarCiclo() {
        if (paused) {
            return;
        }
        if (executing) {
            System.out.println("Uma extracao ja esta em execucao");
            return;
        }
        executing = true;
        final LocalDateTime inicio = LocalDateTime.now();
        System.out.println("Iniciando extracao completa...");
        try {
            final br.com.extrator.servicos.LoggingService ls = new br.com.extrator.servicos.LoggingService();
            ls.iniciarCaptura("extracao_dados_loop");
            try {
                new ExecutarFluxoCompletoComando().executar(new String[] { "--fluxo-completo" });
            } finally {
                ls.pararCaptura();
            }
        } catch (Exception e) {
            System.out.println("Falha ao executar extracao: " + e.getMessage());
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
        System.out.println("Extracao concluida em " + fim.format(fmt) + " (duracao " + dm + "m " + ds + "s)" +
                           " | Proxima em " + nextRunAt.format(fmt) +
                           " (em " + mm + "m " + ss + "s)");
        System.out.println("Durante a espera: [N] Nova imediata  [P] Pausar  [S] Parar  [T] Status  [X] Parar/Sair");
    }
}
