package br.com.extrator.auditoria;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Classe responsável por gerar relatórios de auditoria em diferentes formatos.
 * Produz relatórios detalhados sobre a validação dos dados extraídos.
 */
public class AuditoriaRelatorio {
    private static final Logger logger = LoggerFactory.getLogger(AuditoriaRelatorio.class);
    private static final DateTimeFormatter FORMATTER_ARQUIVO = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter FORMATTER_RELATORIO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    
    /**
     * Gera relatório baseado no resultado da auditoria
     * 
     * @param resultado Resultado completo da auditoria
     */
    public void gerarRelatorio(final ResultadoAuditoria resultado) {
        // Exibir resumo no console
        exibirResumoConsole(resultado.getResultadosValidacao());
        
        // Gerar relatório completo em arquivo
        final String diretorioSaida = "relatorios";
        try {
            gerarRelatorioCompleto(resultado.getResultadosValidacao(), diretorioSaida);
        } catch (final IOException e) {
            logger.error("Erro ao gerar relatório: {}", e.getMessage(), e);
        }
    }

    /**
     * Gera um relatório completo de auditoria em formato texto.
     * 
     * @param resultados Lista de resultados de validação das entidades
     * @param diretorioSaida Diretório onde salvar o relatório
     * @return Path do arquivo gerado
     * @throws IOException Se houver erro ao criar o arquivo
     */
    public Path gerarRelatorioCompleto(final List<String> resultados, final String diretorioSaida) throws IOException {
        final String nomeArquivo = String.format("auditoria_completa_%s.txt", 
                                                Instant.now().atZone(ZoneId.systemDefault()).format(FORMATTER_ARQUIVO));
        final Path caminhoArquivo = Paths.get(diretorioSaida, nomeArquivo);
        
        // Criar diretório se não existir
        Files.createDirectories(caminhoArquivo.getParent());
        
        try (final FileWriter writer = new FileWriter(caminhoArquivo.toFile())) {
            escreverCabecalhoRelatorio(writer);
            escreverResumoGeral(writer, resultados);
            escreverDetalhesResultados(writer, resultados);
            escreverRodapeRelatorio(writer);
        }
        
        logger.info("Relatório completo de auditoria gerado: {}", caminhoArquivo);
        return caminhoArquivo;
    }
    
    /**
     * Gera um relatório resumido apenas com problemas encontrados.
     * 
     * @param resultados Lista de resultados de validação das entidades
     * @param diretorioSaida Diretório onde salvar o relatório
     * @return Path do arquivo gerado, ou null se não houver problemas
     * @throws IOException Se houver erro ao criar o arquivo
     */
    public Path gerarRelatorioProblemas(final List<ResultadoValidacaoEntidade> resultados, final String diretorioSaida) throws IOException {
        final List<ResultadoValidacaoEntidade> problemas = resultados.stream()
            .filter(r -> r.getStatus().temProblema())
            .toList();
        
        if (problemas.isEmpty()) {
            logger.info("Nenhum problema encontrado na auditoria. Relatório de problemas não será gerado.");
            return null;
        }
        
        final String nomeArquivo = String.format("auditoria_problemas_%s.txt", 
                                                Instant.now().atZone(ZoneId.systemDefault()).format(FORMATTER_ARQUIVO));
        final Path caminhoArquivo = Paths.get(diretorioSaida, nomeArquivo);
        
        // Criar diretório se não existir
        Files.createDirectories(caminhoArquivo.getParent());
        
        try (final FileWriter writer = new FileWriter(caminhoArquivo.toFile())) {
            writer.write("=".repeat(80) + "\n");
            writer.write("RELATÓRIO DE PROBLEMAS - AUDITORIA ESL CLOUD\n");
            writer.write("=".repeat(80) + "\n");
            writer.write(String.format("Data/Hora: %s\n", Instant.now().atZone(ZoneId.systemDefault()).format(FORMATTER_RELATORIO)));
            writer.write(String.format("Total de problemas encontrados: %d\n\n", problemas.size()));
            
            for (final ResultadoValidacaoEntidade resultado : problemas) {
                escreverDetalheProblema(writer, resultado);
            }
            
            writer.write("\n" + "=".repeat(80) + "\n");
            writer.write("FIM DO RELATÓRIO DE PROBLEMAS\n");
            writer.write("=".repeat(80) + "\n");
        }
        
        logger.info("Relatório de problemas gerado: {}", caminhoArquivo);
        return caminhoArquivo;
    }
    
    /**
     * Exibe um resumo da auditoria no console.
     * 
     * @param resultados Lista de resultados de validação
     */
    public void exibirResumoConsole(final List<String> resultados) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("📊 RESUMO DA AUDITORIA DE DADOS");
        System.out.println("=".repeat(60));
        System.out.println("⏰ Data/Hora: " + Instant.now().atZone(ZoneId.systemDefault()).format(FORMATTER_RELATORIO));
        System.out.println("📋 Total de resultados: " + resultados.size());
        
        final long erros = resultados.stream()
                .filter(r -> r.contains("ERRO") || r.contains("❌"))
                .count();
        final long alertas = resultados.stream()
                .filter(r -> r.contains("ALERTA") || r.contains("⚠️"))
                .count();
        final long sucessos = resultados.stream()
                .filter(r -> r.contains("OK") || r.contains("✅"))
                .count();
        
        System.out.println("✅ Sucessos: " + sucessos);
        System.out.println("⚠️  Alertas: " + alertas);
        System.out.println("❌ Erros: " + erros);
        
        if (erros == 0 && alertas == 0) {
            System.out.println("✅ Status: AUDITORIA APROVADA");
        } else if (erros > 0) {
            System.out.println("🚨 Status: AUDITORIA COM ERROS CRÍTICOS");
        } else {
            System.out.println("⚠️  Status: AUDITORIA COM ALERTAS");
        }
        
        System.out.println("=".repeat(60));
        
        // Exibir alguns resultados como exemplo
        if (!resultados.isEmpty()) {
            System.out.println("\n📝 Primeiros resultados:");
            resultados.stream()
                    .limit(5)
                    .forEach(r -> System.out.println("  • " + r));
            
            if (resultados.size() > 5) {
                System.out.println("  ... e mais " + (resultados.size() - 5) + " resultados");
            }
        }
        
        System.out.println();
    }
    
    /**
     * Escreve o cabeçalho do relatório.
     */
    private void escreverCabecalhoRelatorio(final FileWriter writer) throws IOException {
        writer.write("=".repeat(80) + "\n");
        writer.write("RELATÓRIO COMPLETO DE AUDITORIA - ESL CLOUD DATA EXTRACTOR\n");
        writer.write("=".repeat(80) + "\n");
        writer.write(String.format("Data/Hora da Auditoria: %s\n", Instant.now().atZone(ZoneId.systemDefault()).format(FORMATTER_RELATORIO)));
        writer.write("Sistema: ESL Cloud Data Extraction\n");
        writer.write("Versão: 1.0-SNAPSHOT\n\n");
    }
    
    /**
     * Escreve o resumo geral da auditoria.
     */
    private void escreverResumoGeral(final FileWriter writer, final List<String> resultados) throws IOException {
        writer.write("RESUMO GERAL\n");
        writer.write("-".repeat(40) + "\n");
        writer.write("Data/Hora da Auditoria: " + Instant.now().atZone(ZoneId.systemDefault()).format(FORMATTER_RELATORIO) + "\n");
        writer.write("Total de Resultados: " + resultados.size() + "\n");
        
        final long erros = resultados.stream()
                .filter(r -> r.contains("ERRO") || r.contains("❌"))
                .count();
        final long alertas = resultados.stream()
                .filter(r -> r.contains("ALERTA") || r.contains("⚠️"))
                .count();
        final long sucessos = resultados.stream()
                .filter(r -> r.contains("OK") || r.contains("✅"))
                .count();
        
        writer.write("Sucessos: " + sucessos + "\n");
        writer.write("Alertas: " + alertas + "\n");
        writer.write("Erros: " + erros + "\n");
        writer.write("\n");
    }
    
    /**
     * Escreve os detalhes de cada entidade.
     */
    private void escreverDetalhesResultados(final FileWriter writer, final List<String> resultados) throws IOException {
        writer.write("DETALHES DOS RESULTADOS\n");
        writer.write("-".repeat(40) + "\n\n");
        
        for (int i = 0; i < resultados.size(); i++) {
            writer.write(String.format("Resultado %d:\n", i + 1));
            writer.write(resultados.get(i) + "\n\n");
        }
    }
    
    /**
     * Escreve o detalhe de um problema específico.
     */
    private void escreverDetalheProblema(final FileWriter writer, final ResultadoValidacaoEntidade resultado) throws IOException {
        writer.write(String.format("🔍 ENTIDADE: %s\n", resultado.getNomeEntidade().toUpperCase()));
        writer.write(String.format("   Status: %s\n", resultado.getStatus().getDescricaoCompleta()));
        
        if (resultado.getErro() != null) {
            writer.write(String.format("   Erro: %s\n", resultado.getErro()));
        }
        
        if (!resultado.getObservacoes().isEmpty()) {
            writer.write("   Observações:\n");
            for (final String obs : resultado.getObservacoes()) {
                writer.write(String.format("     - %s\n", obs));
            }
        }
        
        writer.write(String.format("   Registros: %,d (últimas 24h: %,d)\n", 
                                  resultado.getTotalRegistros(), resultado.getRegistrosUltimas24h()));
        
        if (resultado.getUltimaExtracao() != null) {
            writer.write(String.format("   Última extração: %s\n", 
                resultado.getUltimaExtracao().atZone(ZoneId.systemDefault()).format(FORMATTER_RELATORIO)));
        }
        
        writer.write("\n" + "-".repeat(60) + "\n\n");
    }
    
    /**
     * Escreve o rodapé do relatório.
     */
    private void escreverRodapeRelatorio(final FileWriter writer) throws IOException {
        writer.write("=".repeat(80) + "\n");
        writer.write("FIM DO RELATÓRIO DE AUDITORIA\n");
        writer.write(String.format("Gerado em: %s\n", Instant.now().atZone(ZoneId.systemDefault()).format(FORMATTER_RELATORIO)));
        writer.write("=".repeat(80) + "\n");
    }
}
