package br.com.extrator.servicos;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Serviço responsável por capturar e salvar logs do terminal em arquivos
 */
public class LoggingService {
    
    private static final Logger logger = LoggerFactory.getLogger(LoggingService.class);
    private static final String DIRETORIO_LOGS = "logs";
    private static final DateTimeFormatter FORMATTER_ARQUIVO = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final DateTimeFormatter FORMATTER_LOG = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int MAX_LOG_FILES = 20;
    
    private PrintStream originalOut;
    private PrintStream originalErr;
    private ByteArrayOutputStream outputBuffer;
    private ByteArrayOutputStream errorBuffer;
    private PrintStream teeOut;
    private PrintStream teeErr;
    private String nomeOperacao;
    private LocalDateTime inicioOperacao;
    
    public LoggingService() {
        criarDiretorioLogs();
    }
    
    /**
     * Inicia a captura de logs para uma operação específica
     */
    public void iniciarCaptura(String nomeOperacao) {
        this.nomeOperacao = nomeOperacao;
        this.inicioOperacao = LocalDateTime.now();
        
        // Salvar referências originais
        originalOut = System.out;
        originalErr = System.err;
        
        // Criar buffers para capturar a saída
        outputBuffer = new ByteArrayOutputStream();
        errorBuffer = new ByteArrayOutputStream();
        
        // Criar PrintStreams que escrevem tanto no console quanto no buffer
        teeOut = new PrintStream(new TeeOutputStream(originalOut, outputBuffer));
        teeErr = new PrintStream(new TeeOutputStream(originalErr, errorBuffer));
        
        // Redirecionar System.out e System.err
        System.setOut(teeOut);
        System.setErr(teeErr);
        
        System.out.println("=== INÍCIO DA OPERAÇÃO: " + nomeOperacao + " ===");
        System.out.println("Data/Hora: " + inicioOperacao.format(FORMATTER_LOG));
        System.out.println("========================================");
    }
    
    /**
     * Para a captura e salva os logs em arquivo
     */
    public void pararCaptura() {
        if (originalOut == null || originalErr == null) {
            return; // Captura não foi iniciada
        }
        
        LocalDateTime fimOperacao = LocalDateTime.now();
        
        System.out.println("========================================");
        System.out.println("=== FIM DA OPERAÇÃO: " + nomeOperacao + " ===");
        System.out.println("Data/Hora: " + fimOperacao.format(FORMATTER_LOG));
        System.out.println("========================================");
        
        // Restaurar streams originais
        System.setOut(originalOut);
        System.setErr(originalErr);
        
        // Salvar logs em arquivo
        salvarLogsEmArquivo();
        
        // Limpar recursos
        try {
            if (teeOut != null) teeOut.close();
            if (teeErr != null) teeErr.close();
            if (outputBuffer != null) outputBuffer.close();
            if (errorBuffer != null) errorBuffer.close();
        } catch (IOException e) {
            System.err.println("Erro ao fechar recursos de logging: " + e.getMessage());
        }
        
        // Resetar variáveis
        originalOut = null;
        originalErr = null;
        outputBuffer = null;
        errorBuffer = null;
        teeOut = null;
        teeErr = null;
        nomeOperacao = null;
        inicioOperacao = null;
    }
    
    /**
     * Salva os logs capturados em um arquivo
     */
    private void salvarLogsEmArquivo() {
        try {
            String nomeArquivo = gerarNomeArquivoLog();
            Path caminhoArquivo = Paths.get(DIRETORIO_LOGS, nomeArquivo);
            
            StringBuilder conteudoLog = new StringBuilder();
            
            // Cabeçalho do log
            conteudoLog.append("=".repeat(80)).append("\n");
            conteudoLog.append("LOG DA OPERAÇÃO: ").append(nomeOperacao).append("\n");
            conteudoLog.append("Data/Hora de Início: ").append(inicioOperacao.format(FORMATTER_LOG)).append("\n");
            conteudoLog.append("Data/Hora de Fim: ").append(LocalDateTime.now().format(FORMATTER_LOG)).append("\n");
            conteudoLog.append("=".repeat(80)).append("\n\n");
            
            // Saída padrão (System.out)
            if (outputBuffer.size() > 0) {
                conteudoLog.append("=== SAÍDA PADRÃO (System.out) ===\n");
                conteudoLog.append(outputBuffer.toString("UTF-8"));
                conteudoLog.append("\n\n");
            }
            
            // Saída de erro (System.err)
            if (errorBuffer.size() > 0) {
                conteudoLog.append("=== SAÍDA DE ERRO (System.err) ===\n");
                conteudoLog.append(errorBuffer.toString("UTF-8"));
                conteudoLog.append("\n\n");
            }
            
            // Rodapé do log
            conteudoLog.append("=".repeat(80)).append("\n");
            conteudoLog.append("FIM DO LOG\n");
            conteudoLog.append("=".repeat(80)).append("\n");
            
            // Escrever arquivo
            Files.write(caminhoArquivo, conteudoLog.toString().getBytes("UTF-8"));
            
            System.out.println("📄 Log salvo em: " + caminhoArquivo.toAbsolutePath());
            aplicarRetencaoLogs();
            
        } catch (IOException e) {
            System.err.println("❌ Erro ao salvar log em arquivo: " + e.getMessage());
        }
    }
    
    /**
     * Gera o nome do arquivo de log baseado na operação e timestamp
     */
    private String gerarNomeArquivoLog() {
        String timestamp = inicioOperacao.format(FORMATTER_ARQUIVO);
        String operacaoLimpa = nomeOperacao.toLowerCase()
                .replaceAll("[^a-z0-9]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        
        return String.format("%s_%s.log", operacaoLimpa, timestamp);
    }
    
    /**
     * Cria o diretório de logs se não existir
     */
    private void criarDiretorioLogs() {
        try {
            Path diretorio = Paths.get(DIRETORIO_LOGS);
            if (!Files.exists(diretorio)) {
                Files.createDirectories(diretorio);
                System.out.println("📁 Diretório de logs criado: " + diretorio.toAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("❌ Erro ao criar diretório de logs: " + e.getMessage());
        }
    }
    
    /**
     * Aplica retenção máxima de arquivos .log na pasta de logs.
     * Mantém apenas os MAX_LOG_FILES mais recentes, removendo os demais (mais antigos).
     */
    private static void aplicarRetencaoLogs() {
        try {
            final File pastaLogs = new File(DIRETORIO_LOGS);
            if (!pastaLogs.exists()) {
                return;
            }
            final File[] arquivosLog = pastaLogs.listFiles((dir, name) -> name.toLowerCase().endsWith(".log"));
            if (arquivosLog == null) {
                return;
            }
            if (arquivosLog.length <= MAX_LOG_FILES) {
                return;
            }
            java.util.Arrays.sort(arquivosLog, (a, b) -> Long.compare(a.lastModified(), b.lastModified()));
            final int excedente = arquivosLog.length - MAX_LOG_FILES;
            for (int i = 0; i < excedente; i++) {
                try {
                    Files.deleteIfExists(arquivosLog[i].toPath());
                    logger.debug("🧹 Log antigo removido por retenção: {}", arquivosLog[i].getName());
                } catch (final IOException | SecurityException e) {
                    logger.warn("Falha ao remover log antigo {}: {}", arquivosLog[i].getName(), e.getMessage());
                }
            }
        } catch (final SecurityException e) {
            logger.warn("Não foi possível aplicar retenção de logs: {}", e.getMessage());
        }
    }
    
    /**
     * Classe interna para implementar um OutputStream que escreve em dois destinos
     */
    private static class TeeOutputStream extends OutputStream {
        private final OutputStream out1;
        private final OutputStream out2;
        
        public TeeOutputStream(OutputStream out1, OutputStream out2) {
            this.out1 = out1;
            this.out2 = out2;
        }
        
        @Override
        public void write(int b) throws IOException {
            out1.write(b);
            out2.write(b);
        }
        
        @Override
        public void write(byte[] b) throws IOException {
            out1.write(b);
            out2.write(b);
        }
        
        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            out1.write(b, off, len);
            out2.write(b, off, len);
        }
        
        @Override
        public void flush() throws IOException {
            out1.flush();
            out2.flush();
        }
        
        @Override
        public void close() throws IOException {
            try { out1.flush(); } catch (IOException ignored) {}
            try { out2.flush(); } catch (IOException ignored) {}
        }
    }
    
    /**
     * Organiza arquivos de log .txt na pasta logs.
     * Move arquivos .txt da raiz do projeto para a pasta logs, exceto README.txt.
     */
    public static void organizarLogsTxtNaPastaLogs() {
        try {
            final File pastaRaiz = new File(".");
            final File pastaLogs = new File(DIRETORIO_LOGS);
            
            if (!pastaLogs.exists()) {
                pastaLogs.mkdirs();
            }
            
            final File[] arquivosTxt = pastaRaiz.listFiles((dir, name) -> 
                name.toLowerCase().endsWith(".txt") && !"README.txt".equals(name));
            
            if (arquivosTxt != null) {
                for (final File arquivo : arquivosTxt) {
                    final File destino = new File(pastaLogs, arquivo.getName());
                    if (arquivo.renameTo(destino)) {
                        logger.debug("Log movido: {} -> logs/{}", arquivo.getName(), arquivo.getName());
                    }
                }
            }
        } catch (final SecurityException e) {
            logger.warn("Não foi possível organizar logs .txt: {}", e.getMessage());
        }
    }
}
