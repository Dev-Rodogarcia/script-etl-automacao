package br.com.extrator.comandos.extracao;

import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.OptionalLong;
import java.util.Properties;

import br.com.extrator.Main;
import br.com.extrator.comandos.base.Comando;

/**
 * Gerencia loop de extracao em segundo plano (daemon).
 */
public class LoopDaemonComando implements Comando {
    private static final Path LOGS_DIR = Paths.get("logs");
    private static final Path PID_FILE = LOGS_DIR.resolve("loop_daemon.pid");
    private static final Path STATE_FILE = LOGS_DIR.resolve("loop_daemon.state");
    private static final Path STOP_FILE = LOGS_DIR.resolve("loop_daemon.stop");
    private static final Path DAEMON_STDOUT_FILE = LOGS_DIR.resolve("loop_daemon_console.log");
    private static final long INTERVALO_MINUTOS = 30L;

    public enum Modo {
        START,
        STOP,
        STATUS,
        RUN
    }

    private final Modo modo;

    public LoopDaemonComando(final Modo modo) {
        this.modo = modo;
    }

    @Override
    public void executar(final String[] args) throws Exception {
        switch (modo) {
            case START -> iniciarDaemon();
            case STOP -> pararDaemon();
            case STATUS -> exibirStatus();
            case RUN -> executarDaemon();
            default -> throw new IllegalStateException("Modo de loop daemon nao suportado: " + modo);
        }
    }

    private void iniciarDaemon() throws Exception {
        garantirDiretorioLogs();
        final OptionalLong pidExistente = lerPidArquivo();
        if (pidExistente.isPresent() && processoEstaVivo(pidExistente.getAsLong())) {
            salvarEstado("RUNNING", pidExistente.getAsLong(), "Loop daemon ja estava em execucao.", null, null);
            System.out.println("Loop daemon ja esta em execucao. PID: " + pidExistente.getAsLong());
            return;
        }

        limparArquivoSeExistir(PID_FILE);
        limparArquivoSeExistir(STOP_FILE);

        final List<String> comando = construirComandoFilho();
        final ProcessBuilder processBuilder = new ProcessBuilder(comando);
        processBuilder.redirectOutput(Redirect.appendTo(DAEMON_STDOUT_FILE.toFile()));
        processBuilder.redirectError(Redirect.appendTo(DAEMON_STDOUT_FILE.toFile()));

        final Process processo = processBuilder.start();
        final long pid = processo.pid();
        Files.writeString(PID_FILE, String.valueOf(pid), StandardCharsets.UTF_8);
        salvarEstado("STARTING", pid, "Processo daemon iniciado.", null, null);

        Thread.sleep(1200L);
        if (!processo.isAlive()) {
            limparArquivoSeExistir(PID_FILE);
            limparArquivoSeExistir(STOP_FILE);
            throw new IllegalStateException("Falha ao iniciar loop daemon. Consulte " + DAEMON_STDOUT_FILE.toAbsolutePath());
        }

        System.out.println("Loop daemon iniciado com sucesso. PID: " + pid);
        System.out.println("Log do daemon: " + DAEMON_STDOUT_FILE.toAbsolutePath());
    }

    private void pararDaemon() throws Exception {
        garantirDiretorioLogs();
        final OptionalLong pidOpt = lerPidArquivo();
        if (pidOpt.isEmpty()) {
            limparArquivoSeExistir(STOP_FILE);
            salvarEstado("STOPPED", -1L, "Loop daemon ja estava parado.", null, null);
            System.out.println("Loop daemon nao estava em execucao.");
            return;
        }

        final long pid = pidOpt.getAsLong();
        Files.writeString(STOP_FILE, "stop@" + LocalDateTime.now(), StandardCharsets.UTF_8);
        salvarEstado("STOPPING", pid, "Solicitado encerramento do loop daemon.", null, null);

        final ProcessHandle processHandle = ProcessHandle.of(pid).orElse(null);
        if (processHandle != null && processHandle.isAlive()) {
            final long limiteMillis = System.currentTimeMillis() + 20_000L;
            while (processHandle.isAlive() && System.currentTimeMillis() < limiteMillis) {
                Thread.sleep(500L);
            }
            if (processHandle.isAlive()) {
                processHandle.destroy();
                Thread.sleep(2000L);
            }
            if (processHandle.isAlive()) {
                processHandle.destroyForcibly();
                Thread.sleep(1000L);
            }
        }

        limparArquivoSeExistir(PID_FILE);
        limparArquivoSeExistir(STOP_FILE);
        salvarEstado("STOPPED", pid, "Loop daemon encerrado por comando de parada.", null, null);
        System.out.println("Loop daemon parado.");
    }

    private void exibirStatus() throws Exception {
        garantirDiretorioLogs();
        final OptionalLong pidOpt = lerPidArquivo();
        final Properties state = carregarEstado();

        final long pid = pidOpt.orElse(-1L);
        final boolean vivo = pid > 0 && processoEstaVivo(pid);
        final String statusEstado = state.getProperty("status", vivo ? "RUNNING" : "STOPPED");
        final String atualizadoEm = state.getProperty("updated_at", "N/A");
        final String detalhe = state.getProperty("detail", "N/A");
        final String ultimoCiclo = state.getProperty("last_run_at", "N/A");
        final String proximoCiclo = state.getProperty("next_run_at", "N/A");

        System.out.println("Status do loop daemon");
        System.out.println("  PID: " + (pid > 0 ? pid : "N/A"));
        System.out.println("  Processo vivo: " + (vivo ? "SIM" : "NAO"));
        System.out.println("  Estado: " + statusEstado);
        System.out.println("  Atualizado em: " + atualizadoEm);
        System.out.println("  Ultimo ciclo: " + ultimoCiclo);
        System.out.println("  Proximo ciclo: " + proximoCiclo);
        System.out.println("  Detalhe: " + detalhe);
        System.out.println("  Log: " + DAEMON_STDOUT_FILE.toAbsolutePath());

        if (pid > 0 && !vivo) {
            salvarEstado("STOPPED", pid, "PID registrado nao esta mais ativo.", ultimoCiclo, proximoCiclo);
            limparArquivoSeExistir(PID_FILE);
        }
    }

    private void executarDaemon() throws Exception {
        garantirDiretorioLogs();
        limparArquivoSeExistir(STOP_FILE);
        final long pid = ProcessHandle.current().pid();
        Files.writeString(PID_FILE, String.valueOf(pid), StandardCharsets.UTF_8);
        salvarEstado("RUNNING", pid, "Daemon iniciado e aguardando ciclos.", null, null);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                limparArquivoSeExistir(PID_FILE);
                limparArquivoSeExistir(STOP_FILE);
                salvarEstado("STOPPED", pid, "Daemon finalizado.", null, null);
            } catch (final Exception ignored) {
                // no-op
            }
        }));

        while (true) {
            if (deveParar()) {
                salvarEstado("STOPPED", pid, "Sinal de parada detectado antes do ciclo.", null, null);
                break;
            }

            final LocalDateTime inicio = LocalDateTime.now();
            salvarEstado("RUNNING", pid, "Executando ciclo de extracao.", inicio.toString(), null);

            boolean sucesso = true;
            String detalhe = "Ciclo concluido com sucesso.";
            try {
                new ExecutarFluxoCompletoComando().executar(new String[] {"--fluxo-completo"});
            } catch (final Exception e) {
                sucesso = false;
                detalhe = "Falha no ciclo: " + resumirMensagem(e.getMessage());
            }

            final LocalDateTime fim = LocalDateTime.now();
            final LocalDateTime proximo = fim.plusMinutes(INTERVALO_MINUTOS);
            final String status = sucesso ? "WAITING_NEXT_CYCLE" : "WAITING_NEXT_CYCLE_WITH_ERROR";
            salvarEstado(status, pid, detalhe, fim.toString(), proximo.toString());

            final boolean paradaSolicitada = aguardarProximoCicloComParada(proximo);
            if (paradaSolicitada) {
                salvarEstado("STOPPED", pid, "Sinal de parada detectado durante espera.", fim.toString(), null);
                break;
            }
        }

        limparArquivoSeExistir(PID_FILE);
        limparArquivoSeExistir(STOP_FILE);
    }

    private boolean aguardarProximoCicloComParada(final LocalDateTime proximo) throws InterruptedException {
        while (LocalDateTime.now().isBefore(proximo)) {
            if (deveParar()) {
                return true;
            }
            Thread.sleep(1000L);
        }
        return false;
    }

    private boolean deveParar() {
        return Files.exists(STOP_FILE);
    }

    private List<String> construirComandoFilho() throws URISyntaxException {
        final List<String> comando = new ArrayList<>();
        comando.add(resolverExecutavelJava());

        final Path jarAtual = resolverJarAtual();
        if (jarAtual != null && Files.exists(jarAtual)) {
            comando.add("-jar");
            comando.add(jarAtual.toString());
            comando.add("--loop-daemon-run");
            return comando;
        }

        final String classpath = System.getProperty("java.class.path");
        if (classpath == null || classpath.isBlank()) {
            throw new IllegalStateException("Nao foi possivel resolver classpath para iniciar loop daemon.");
        }
        comando.add("-cp");
        comando.add(classpath);
        comando.add(Main.class.getName());
        comando.add("--loop-daemon-run");
        return comando;
    }

    private Path resolverJarAtual() throws URISyntaxException {
        final String sunCommand = System.getProperty("sun.java.command", "");
        if (sunCommand != null && !sunCommand.isBlank()) {
            final String primeiroToken = extrairPrimeiroToken(sunCommand);
            if (primeiroToken.toLowerCase(Locale.ROOT).endsWith(".jar")) {
                return Paths.get(primeiroToken).toAbsolutePath().normalize();
            }
        }

        final String codeSource = Main.class.getProtectionDomain().getCodeSource().getLocation().toString();
        final int idx = codeSource.toLowerCase(Locale.ROOT).indexOf(".jar");
        if (idx >= 0) {
            String pathJar = codeSource.substring(0, idx + 4);
            if (pathJar.startsWith("jar:")) {
                pathJar = pathJar.substring(4);
            }
            if (pathJar.startsWith("nested:")) {
                pathJar = pathJar.substring(7);
            }
            if (pathJar.startsWith("file:")) {
                return Paths.get(new java.net.URI(pathJar)).toAbsolutePath().normalize();
            }
            return Paths.get(pathJar).toAbsolutePath().normalize();
        }
        return null;
    }

    private String extrairPrimeiroToken(final String comandoCompleto) {
        final String valor = comandoCompleto.trim();
        if (valor.isEmpty()) {
            return valor;
        }
        if (valor.startsWith("\"")) {
            final int fim = valor.indexOf('"', 1);
            if (fim > 1) {
                return valor.substring(1, fim);
            }
            return valor.substring(1);
        }
        final int espaco = valor.indexOf(' ');
        return espaco > 0 ? valor.substring(0, espaco) : valor;
    }

    private String resolverExecutavelJava() {
        final String javaHome = System.getProperty("java.home");
        final boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
        final String nomeExecutavel = windows ? "java.exe" : "java";
        final Path executavel = Paths.get(javaHome, "bin", nomeExecutavel).toAbsolutePath().normalize();
        return executavel.toString();
    }

    private OptionalLong lerPidArquivo() {
        if (!Files.exists(PID_FILE)) {
            return OptionalLong.empty();
        }
        try {
            final String conteudo = Files.readString(PID_FILE, StandardCharsets.UTF_8).trim();
            if (conteudo.isEmpty()) {
                return OptionalLong.empty();
            }
            return OptionalLong.of(Long.parseLong(conteudo));
        } catch (final Exception e) {
            return OptionalLong.empty();
        }
    }

    private Properties carregarEstado() {
        final Properties p = new Properties();
        if (!Files.exists(STATE_FILE)) {
            return p;
        }
        try (var in = Files.newInputStream(STATE_FILE)) {
            p.load(in);
        } catch (final IOException e) {
            // Mantem estado vazio em caso de falha de leitura.
        }
        return p;
    }

    private void salvarEstado(
        final String status,
        final long pid,
        final String detalhe,
        final String lastRunAt,
        final String nextRunAt
    ) {
        final Properties p = new Properties();
        p.setProperty("status", status);
        p.setProperty("pid", pid > 0 ? String.valueOf(pid) : "");
        p.setProperty("detail", detalhe == null ? "" : detalhe);
        p.setProperty("updated_at", LocalDateTime.now().toString());
        p.setProperty("last_run_at", lastRunAt == null ? "" : lastRunAt);
        p.setProperty("next_run_at", nextRunAt == null ? "" : nextRunAt);

        try (var out = Files.newOutputStream(STATE_FILE)) {
            p.store(out, "loop-daemon-state");
        } catch (final IOException e) {
            throw new RuntimeException("Falha ao salvar estado do loop daemon.", e);
        }
    }

    private boolean processoEstaVivo(final long pid) {
        return ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false);
    }

    private void garantirDiretorioLogs() throws IOException {
        if (!Files.exists(LOGS_DIR)) {
            Files.createDirectories(LOGS_DIR);
        }
    }

    private void limparArquivoSeExistir(final Path path) throws IOException {
        if (Files.exists(path)) {
            Files.delete(path);
        }
    }

    private String resumirMensagem(final String msg) {
        if (msg == null || msg.isBlank()) {
            return "Sem detalhes.";
        }
        final String limpa = msg.replace('\n', ' ').replace('\r', ' ').trim();
        return limpa.length() > 240 ? limpa.substring(0, 240) + "..." : limpa;
    }
}
