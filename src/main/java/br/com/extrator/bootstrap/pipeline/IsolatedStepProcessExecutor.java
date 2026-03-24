package br.com.extrator.bootstrap.pipeline;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import br.com.extrator.suporte.concorrencia.ExecutionTimeoutException;
import br.com.extrator.suporte.configuracao.ConfigEtl;
import br.com.extrator.suporte.observabilidade.ExecutionContext;

public class IsolatedStepProcessExecutor {
    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final String MAIN_CLASS_NAME = "br.com.extrator.bootstrap.Main";
    private static final String CHILD_PROCESS_PROPERTY = "etl.process.isolated.child";

    public ProcessExecutionResult executar(final ApiType apiType,
                                           final LocalDate dataInicio,
                                           final LocalDate dataFim,
                                           final String entidade) throws Exception {
        return executar(apiType, dataInicio, dataFim, entidade, FaultMode.NONE, resolverTimeoutPadrao(apiType, entidade));
    }

    public ProcessExecutionResult executar(final ApiType apiType,
                                           final LocalDate dataInicio,
                                           final LocalDate dataFim,
                                           final String entidade,
                                           final FaultMode faultMode) throws Exception {
        return executar(apiType, dataInicio, dataFim, entidade, faultMode, resolverTimeoutPadrao(apiType, entidade));
    }

    public ProcessExecutionResult executar(final ApiType apiType,
                                           final LocalDate dataInicio,
                                           final LocalDate dataFim,
                                           final String entidade,
                                           final Duration timeout) throws Exception {
        return executar(apiType, dataInicio, dataFim, entidade, FaultMode.NONE, timeout);
    }

    public ProcessExecutionResult executar(final ApiType apiType,
                                           final LocalDate dataInicio,
                                           final LocalDate dataFim,
                                           final String entidade,
                                           final FaultMode faultMode,
                                           final Duration timeout) throws Exception {
        final Path logFile = criarArquivoLog(apiType, entidade, faultMode);
        final List<String> comando = construirComando(apiType, dataInicio, dataFim, entidade, faultMode);
        final ProcessBuilder processBuilder = new ProcessBuilder(comando);
        processBuilder.directory(Path.of(System.getProperty("user.dir")).toFile());
        processBuilder.redirectOutput(logFile.toFile());
        processBuilder.redirectError(logFile.toFile());

        final Process process = processBuilder.start();
        final Duration timeoutAplicado = timeout == null || timeout.isNegative() || timeout.isZero()
            ? resolverTimeoutPadrao(apiType, entidade)
            : timeout;
        final long deadlineNanos = System.nanoTime() + timeoutAplicado.toNanos();
        try {
            while (true) {
                try {
                    final long restanteNanos = deadlineNanos - System.nanoTime();
                    if (restanteNanos <= 0L) {
                        destruirProcesso(process);
                        throw new ExecutionTimeoutException(
                            "Processo isolado "
                                + apiType.name().toLowerCase(Locale.ROOT)
                                + " excedeu timeout de "
                                + timeoutAplicado.toMillis()
                                + " ms. Ultimas linhas: "
                                + lerTail(logFile)
                        );
                    }
                    final long esperaAtualMs = Math.max(
                        1L,
                        Math.min(250L, java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(restanteNanos))
                    );
                    if (process.waitFor(esperaAtualMs, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                        break;
                    }
                } catch (final InterruptedException e) {
                    destruirProcesso(process);
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
            final int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new IllegalStateException(
                    "Processo isolado " + apiType.name().toLowerCase(Locale.ROOT)
                        + " falhou com exit_code=" + exitCode
                        + ". Ultimas linhas: " + lerTail(logFile)
                );
            }
            return new ProcessExecutionResult(process.pid(), logFile);
        } finally {
            if (process.isAlive()) {
                destruirProcesso(process);
            }
        }
    }

    protected List<String> construirComando(final ApiType apiType,
                                            final LocalDate dataInicio,
                                            final LocalDate dataFim,
                                            final String entidade,
                                            final FaultMode faultMode) throws URISyntaxException {
        final List<String> comando = new ArrayList<>();
        comando.add(resolverExecutavelJava());
        comando.add("-Dfile.encoding=UTF-8");
        comando.add("-Dsun.stdout.encoding=UTF-8");
        comando.add("-Dsun.stderr.encoding=UTF-8");
        comando.add("-Dextrator.logger.console.mirror=false");
        comando.add("-Detl.process.isolation.enabled=false");
        comando.add("-D" + CHILD_PROCESS_PROPERTY + "=true");
        comando.add("-Detl.parent.execution.id=" + ExecutionContext.currentExecutionId());
        comando.add("-Detl.parent.command=" + ExecutionContext.currentCommand());
        comando.add("-Detl.parent.cycle.id=" + ExecutionContext.currentCycleId());

        final Path jarAtual = resolverJarAtual();
        if (jarAtual != null && Files.exists(jarAtual)) {
            comando.add("-jar");
            comando.add(jarAtual.toString());
        } else {
            final String classpath = montarClasspathExecucao();
            if (classpath == null || classpath.isBlank()) {
                throw new IllegalStateException("Nao foi possivel resolver classpath para execucao isolada.");
            }
            comando.add("-cp");
            comando.add(classpath);
            comando.add(MAIN_CLASS_NAME);
        }

        comando.add("--executar-step-isolado");
        comando.add(apiType.name().toLowerCase(Locale.ROOT));
        comando.add(dataInicio.toString());
        comando.add(dataFim.toString());
        comando.add(entidade == null || entidade.isBlank() ? "all" : entidade);
        if (faultMode != null && faultMode != FaultMode.NONE) {
            comando.add("--fault");
            comando.add(faultMode.cliValue());
        }
        return comando;
    }

    private String montarClasspathExecucao() throws URISyntaxException {
        final List<String> entradas = new ArrayList<>();
        final Path codeSource = resolverCodeSourceAtual();
        if (codeSource != null && Files.exists(codeSource)) {
            entradas.add(codeSource.toString());
        }

        entradas.addAll(resolverClasspathDoClassLoader(Thread.currentThread().getContextClassLoader()));

        final String runtimeClasspath = java.lang.management.ManagementFactory.getRuntimeMXBean().getClassPath();
        if (runtimeClasspath != null && !runtimeClasspath.isBlank()) {
            entradas.add(runtimeClasspath);
        }

        final String classpathAtual = System.getProperty("java.class.path");
        if (classpathAtual != null && !classpathAtual.isBlank() && !classpathAtual.equals(runtimeClasspath)) {
            entradas.add(classpathAtual);
        }

        return String.join(java.io.File.pathSeparator, entradas.stream().distinct().toList());
    }

    private List<String> resolverClasspathDoClassLoader(final ClassLoader classLoader) {
        final List<String> entradas = new ArrayList<>();
        ClassLoader atual = classLoader;
        while (atual != null) {
            try {
                final java.lang.reflect.Method metodo = atual.getClass().getMethod("getURLs");
                final Object retorno = metodo.invoke(atual);
                if (retorno instanceof URL[] urls) {
                    for (final URL url : urls) {
                        if (url == null || !"file".equalsIgnoreCase(url.getProtocol())) {
                            continue;
                        }
                        entradas.add(Path.of(url.toURI()).toAbsolutePath().normalize().toString());
                    }
                }
            } catch (final ReflectiveOperationException | IllegalArgumentException | URISyntaxException ignored) {
                // Sem suporte a getURLs neste classloader; segue para o pai.
            }
            atual = atual.getParent();
        }
        return entradas;
    }

    private Path criarArquivoLog(final ApiType apiType,
                                 final String entidade,
                                 final FaultMode faultMode) throws IOException {
        final Path dir = Path.of("logs", "isolated_steps");
        Files.createDirectories(dir);
        final String nomeEntidade = entidade == null || entidade.isBlank()
            ? "all"
            : entidade.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        final String nomeFault = faultMode == null ? FaultMode.NONE.cliValue() : faultMode.cliValue();
        return dir.resolve(
            "isolated_step_"
                + apiType.name().toLowerCase(Locale.ROOT)
                + "_"
                + nomeEntidade
                + "_"
                + nomeFault
                + "_"
                + FILE_TS.format(LocalDateTime.now())
                + ".log"
        );
    }

    private void destruirProcesso(final Process process) {
        process.descendants().forEach(ProcessHandle::destroyForcibly);
        process.destroy();
        try {
            if (!process.waitFor(ConfigEtl.obterTimeoutDestruicaoProcessoIsoladoMs(), java.util.concurrent.TimeUnit.MILLISECONDS)) {
                process.descendants().forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly();
                process.waitFor(ConfigEtl.obterTimeoutDestruicaoProcessoIsoladoMs(), java.util.concurrent.TimeUnit.MILLISECONDS);
            }
        } catch (final InterruptedException e) {
            process.descendants().forEach(ProcessHandle::destroyForcibly);
            process.destroyForcibly();
            Thread.currentThread().interrupt();
        }
    }

    private String lerTail(final Path logFile) {
        try {
            final List<String> linhas = Files.readAllLines(logFile, StandardCharsets.UTF_8);
            final int inicio = Math.max(0, linhas.size() - 8);
            return String.join(" | ", linhas.subList(inicio, linhas.size()));
        } catch (final IOException e) {
            return "log_indisponivel=" + logFile.toAbsolutePath();
        }
    }

    private Path resolverJarAtual() throws URISyntaxException {
        final String sunCommand = System.getProperty("sun.java.command", "");
        if (sunCommand != null && !sunCommand.isBlank()) {
            final String primeiroToken = extrairPrimeiroToken(sunCommand);
            if (primeiroToken.toLowerCase(Locale.ROOT).endsWith(".jar")) {
                return resolverPathJar(primeiroToken);
            }
        }

        final String codeSource = IsolatedStepProcessExecutor.class.getProtectionDomain().getCodeSource().getLocation().toString();
        final int idx = codeSource.toLowerCase(Locale.ROOT).indexOf(".jar");
        if (idx >= 0) {
            String pathJar = codeSource.substring(0, idx + 4);
            if (pathJar.startsWith("jar:")) {
                pathJar = pathJar.substring(4);
            }
            if (pathJar.startsWith("nested:")) {
                pathJar = pathJar.substring(7);
            }
            return resolverPathJar(pathJar);
        }
        return null;
    }

    private Path resolverCodeSourceAtual() throws URISyntaxException {
        final String codeSource = IsolatedStepProcessExecutor.class.getProtectionDomain().getCodeSource().getLocation().toString();
        if (codeSource == null || codeSource.isBlank()) {
            return null;
        }
        if (codeSource.startsWith("file:")) {
            return Path.of(new java.net.URI(codeSource)).toAbsolutePath().normalize();
        }
        return Path.of(codeSource).toAbsolutePath().normalize();
    }

    private Path resolverPathJar(final String pathJar) throws URISyntaxException {
        if (pathJar == null || pathJar.isBlank()) {
            return null;
        }
        if (pathJar.startsWith("file:")) {
            return Path.of(new java.net.URI(pathJar)).toAbsolutePath().normalize();
        }

        String caminho = URLDecoder.decode(pathJar, StandardCharsets.UTF_8);
        if (caminho.length() >= 3
            && caminho.charAt(0) == '/'
            && Character.isLetter(caminho.charAt(1))
            && caminho.charAt(2) == ':') {
            caminho = caminho.substring(1);
        }
        return Path.of(caminho).toAbsolutePath().normalize();
    }

    private String extrairPrimeiroToken(final String comandoCompleto) {
        final String valor = comandoCompleto == null ? "" : comandoCompleto.trim();
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
        return Path.of(javaHome, "bin", nomeExecutavel).toAbsolutePath().normalize().toString();
    }

    private Duration resolverTimeoutPadrao(final ApiType apiType, final String entidade) {
        if (apiType == ApiType.DATAEXPORT) {
            return ConfigEtl.obterTimeoutStepDataExport();
        }
        if (entidade != null && "faturas_graphql".equalsIgnoreCase(entidade)) {
            return ConfigEtl.obterTimeoutStepFaturasGraphQL();
        }
        return ConfigEtl.obterTimeoutStepGraphQL();
    }

    public enum ApiType {
        GRAPHQL,
        DATAEXPORT
    }

    public enum FaultMode {
        NONE("none"),
        HANG_IGNORE_INTERRUPT("hang_ignore_interrupt"),
        ERROR("error");

        private final String cliValue;

        FaultMode(final String cliValue) {
            this.cliValue = cliValue;
        }

        public String cliValue() {
            return cliValue;
        }

        public static FaultMode fromCliValue(final String value) {
            if (value == null || value.isBlank()) {
                return NONE;
            }
            final String normalized = value.trim().toLowerCase(Locale.ROOT);
            return switch (normalized) {
                case "hang_ignore_interrupt" -> HANG_IGNORE_INTERRUPT;
                case "error" -> ERROR;
                default -> NONE;
            };
        }
    }

    public record ProcessExecutionResult(long pid, Path logFile) {
    }
}
