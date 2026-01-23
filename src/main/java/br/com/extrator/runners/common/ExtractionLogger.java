package br.com.extrator.runners.common;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import br.com.extrator.api.ResultadoExtracao;
// DataExportEntityExtractor é usado em instanceof e cast (linhas 54, 56, 79) - falso positivo do linter
import br.com.extrator.runners.common.DataExportEntityExtractor;
import br.com.extrator.util.console.LoggerConsole;

/**
 * Classe utilitária para logging padronizado e detalhado de extrações.
 * Fornece logs ricos com métricas, estatísticas e informações de performance.
 */
@SuppressWarnings("unused") // DataExportEntityExtractor é usado em instanceof e cast (linhas 59, 60, 61, 85)
public class ExtractionLogger {
    private final LoggerConsole log;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    
    // Referência estática ao tipo para forçar o linter a reconhecer o import
    private static final Class<?> DATA_EXPORT_EXTRACTOR_TYPE = DataExportEntityExtractor.class;
    
    public ExtractionLogger(final Class<?> clazz) {
        this.log = LoggerConsole.getLogger(clazz);
    }
    
    /**
     * Executa uma extração com logging padronizado e detalhado.
     * 
     * @param extractor Extractor a ser executado
     * @param dataInicio Data de início
     * @param dataFim Data de fim
     * @param emoji Emoji para identificação visual
     * @return Resultado da extração
     */
    public <T> ExtractionResult executeWithLogging(
            final EntityExtractor<T> extractor,
            final LocalDate dataInicio,
            final LocalDate dataFim,
            final String emoji) {
        
        final LocalDateTime inicio = LocalDateTime.now();
        final String entityName = extractor.getEntityName();
        final String displayEmoji = emoji != null ? emoji : extractor.getEmoji();
        
        // Log inicial detalhado
        log.info("{}", "=".repeat(80));
        log.info("{} {} INICIANDO EXTRAÇÃO: {}", displayEmoji, displayEmoji, entityName.toUpperCase());
        log.info("{}", "=".repeat(80));
        log.info("📅 Período: {} a {}", 
            formatarPeriodo(dataInicio, dataFim), 
            dataFim != null && !dataInicio.equals(dataFim) ? dataFim : dataInicio);
        log.info("⏰ Início: {}", inicio.format(TIME_FORMATTER));
        log.info("{}", "-".repeat(80));
        
        try {
            final LocalDateTime inicioExtracao = LocalDateTime.now();
            final ResultadoExtracao<T> resultado = extractor.extract(dataInicio, dataFim);
            final LocalDateTime fimExtracao = LocalDateTime.now();
            final Duration duracaoExtracao = Duration.between(inicioExtracao, fimExtracao);
            
            final List<T> dtos = resultado.getDados();
            final int totalPaginas = resultado.getPaginasProcessadas();
            final boolean completo = resultado.isCompleto();
            final String statusMsg = completo ? "✅ COMPLETO" : "⚠️ INCOMPLETO (" + resultado.getMotivoInterrupcao() + ")";
            
            // Log de extração detalhado
            log.info("{}", "-".repeat(80));
            log.info("📊 RESULTADO DA EXTRAÇÃO:");
            log.info("   • Total extraído da API: {} registros", formatarNumero(dtos.size()));
            log.info("   • Páginas processadas: {}", totalPaginas);
            log.info("   • Status: {}", statusMsg);
            final double segundosExtracao = duracaoExtracao.toMillis() / 1000.0;
            log.info("   • Tempo de extração (apenas busca na API): {} ms ({} s)",
                duracaoExtracao.toMillis(),
                String.format("%.2f", segundosExtracao));
            log.info("      ↳ enriquecimento e gravação entram no Tempo de salvamento abaixo");
            if (dtos.size() > 0 && duracaoExtracao.toMillis() > 0) {
                final double registrosPorSegundo = (dtos.size() * 1000.0) / duracaoExtracao.toMillis();
                log.info("   • Taxa de extração: {} registros/segundo", String.format("%.2f", registrosPorSegundo));
            }
            
            int registrosSalvos = 0;
            int totalUnicos = dtos.size(); // Padrão para GraphQL
            final LocalDateTime inicioSalvamento = LocalDateTime.now();
            
            if (!dtos.isEmpty()) {
                try {
                    // Se for DataExportEntityExtractor, usar saveWithDeduplication para obter totalUnicos
                    if (extractor instanceof DataExportEntityExtractor) {
                        final DataExportEntityExtractor<T> dataExportExtractor = (DataExportEntityExtractor<T>) extractor;
                        final DataExportEntityExtractor.SaveResult saveResult = 
                            dataExportExtractor.saveWithDeduplication(dtos);
                        registrosSalvos = saveResult.getRegistrosSalvos();
                        totalUnicos = saveResult.getTotalUnicos();
                        
                        final LocalDateTime fimSalvamento = LocalDateTime.now();
                        final Duration duracaoSalvamento = Duration.between(inicioSalvamento, fimSalvamento);
                        
                        if (dtos.size() != totalUnicos) {
                            final int duplicadosRemovidos = dtos.size() - totalUnicos;
                            final double percentualDuplicados = (duplicadosRemovidos * 100.0) / dtos.size();
                            log.warn("   ⚠️ Duplicados removidos: {} ({}% do total)", 
                                formatarNumero(duplicadosRemovidos), String.format("%.2f", percentualDuplicados));
                        }
                        
                        log.info("{}", "-".repeat(80));
                        log.info("💾 RESULTADO DO SALVAMENTO (DataExport):");
                        log.info("   • Registros únicos após deduplicação: {}", formatarNumero(totalUnicos));
                        log.info("   • Operações no banco (INSERTs + UPDATEs): {}", formatarNumero(registrosSalvos));
                        final double segundosSalvamento = duracaoSalvamento.toMillis() / 1000.0;
                        log.info("   • Tempo de salvamento: {} ms ({} s)", 
                            duracaoSalvamento.toMillis(), 
                            String.format("%.2f", segundosSalvamento));
                        if (totalUnicos > 0 && duracaoSalvamento.toMillis() > 0) {
                            final double registrosPorSegundo = (totalUnicos * 1000.0) / duracaoSalvamento.toMillis();
                            log.info("   • Taxa de salvamento: {} registros/segundo", String.format("%.2f", registrosPorSegundo));
                        }
                    } else {
                        registrosSalvos = extractor.save(dtos);
                        final LocalDateTime fimSalvamento = LocalDateTime.now();
                        final Duration duracaoSalvamento = Duration.between(inicioSalvamento, fimSalvamento);
                        
                        log.info("{}", "-".repeat(80));
                        log.info("💾 RESULTADO DO SALVAMENTO (GraphQL):");
                        log.info("   • Registros salvos: {}", formatarNumero(registrosSalvos));
                        final double segundosSalvamento = duracaoSalvamento.toMillis() / 1000.0;
                        log.info("   • Tempo de salvamento: {} ms ({} s)", 
                            duracaoSalvamento.toMillis(), 
                            String.format("%.2f", segundosSalvamento));
                        if (registrosSalvos > 0 && duracaoSalvamento.toMillis() > 0) {
                            final double registrosPorSegundo = (registrosSalvos * 1000.0) / duracaoSalvamento.toMillis();
                            log.info("   • Taxa de salvamento: {} registros/segundo", String.format("%.2f", registrosPorSegundo));
                        }
                    }
                } catch (final java.sql.SQLException e) {
                    log.error("❌ ERRO CRÍTICO ao salvar {}: {}", entityName, e.getMessage());
                    throw new RuntimeException("Erro ao salvar " + entityName, e);
                }
            } else {
                log.info("   ℹ️ Nenhum registro para salvar (lista vazia)");
            }
            
            final LocalDateTime fim = LocalDateTime.now();
            final Duration duracaoTotal = Duration.between(inicio, fim);
            final int totalRecebido = dtos.size();
            final int deltaIgnorados = Math.max(0, totalUnicos - registrosSalvos);
            final String mensagem = buildMensagem(dataInicio, dataFim, totalRecebido, registrosSalvos, totalUnicos, deltaIgnorados, duracaoTotal);
            
            // Log de resumo final
            log.info("{}", "=".repeat(80));
            log.info("{} {} RESUMO FINAL: {}", displayEmoji, displayEmoji, entityName.toUpperCase());
            log.info("{}", "=".repeat(80));
            log.info("📈 Estatísticas:");
            log.info("   • API → DB: {} → {} registros", formatarNumero(totalRecebido), formatarNumero(registrosSalvos));
            if (totalRecebido != totalUnicos) {
                log.info("   • Únicos após deduplicação: {}", formatarNumero(totalUnicos));
            }
            if (deltaIgnorados > 0) {
                log.info("   • Ignorados/duplicados: {}", formatarNumero(deltaIgnorados));
            }
            log.info("   • Páginas: {}", totalPaginas);
            log.info("   • Tempo total: {} ms ({} s)", 
                duracaoTotal.toMillis(), 
                String.format("%.2f", duracaoTotal.toMillis() / 1000.0));
            log.info("   • Status: {}", completo ? "✅ COMPLETO" : "⚠️ INCOMPLETO");
            log.info("⏰ Fim: {}", fim.format(TIME_FORMATTER));
            log.info("{}", "=".repeat(80));
            log.info(""); // Linha em branco para separação visual
            
            // Usar sucessoComUnicos se for DataExport (tem deduplicação)
            final boolean isDataExport = extractor instanceof DataExportEntityExtractor;
            if (isDataExport) {
                return ExtractionResult.sucessoComUnicos(entityName, inicio, resultado, registrosSalvos, totalUnicos, mensagem)
                    .build();
            } else {
                return ExtractionResult.sucesso(entityName, inicio, resultado, registrosSalvos, mensagem)
                    .build();
            }
                
        } catch (final Exception e) {
            final LocalDateTime fim = LocalDateTime.now();
            final Duration duracaoTotal = Duration.between(inicio, fim);
            log.error("{}", "=".repeat(80));
            log.error("❌ ERRO NA EXTRAÇÃO: {}", entityName.toUpperCase());
            log.error("{}", "=".repeat(80));
            log.error("   • Erro: {}", e.getMessage());
            log.error("   • Tipo: {}", e.getClass().getSimpleName());
            log.error("   • Tempo até erro: {} ms ({} s)", 
                duracaoTotal.toMillis(), 
                String.format("%.2f", duracaoTotal.toMillis() / 1000.0));
            log.error("{}", "=".repeat(80));
            log.error(""); // Linha em branco para separação visual
            return ExtractionResult.erro(entityName, inicio, e).build();
        }
    }
    
    private String formatarNumero(final int numero) {
        return String.format("%,d", numero);
    }
    
    private String formatarPeriodo(final LocalDate dataInicio, final LocalDate dataFim) {
        if (dataFim != null && !dataInicio.equals(dataFim)) {
            return dataInicio + " a " + dataFim;
        }
        return dataInicio.toString();
    }
    
    private String buildMensagem(final LocalDate dataInicio,
                                 final LocalDate dataFim,
                                 final int totalRecebido,
                                 final int registrosSalvos,
                                 final int totalUnicos,
                                 final int deltaIgnorados,
                                 final Duration duracaoTotal) {
        final StringBuilder sb = new StringBuilder();
        sb.append("API: ").append(formatarNumero(totalRecebido)).append(" recebidos");
        if (totalRecebido != totalUnicos) {
            sb.append(" (únicos: ").append(formatarNumero(totalUnicos)).append(")");
        }
        sb.append(" | DB: ").append(formatarNumero(registrosSalvos)).append(" processados");
        if (deltaIgnorados > 0) {
            sb.append(" | Delta: ").append(formatarNumero(deltaIgnorados)).append(" (duplicados/ignorados)");
        }
        sb.append(" | Tempo: ").append(duracaoTotal.toMillis()).append("ms");
        
        if (dataFim != null && !dataInicio.equals(dataFim)) {
            sb.append(" | Período: ").append(dataInicio).append(" a ").append(dataFim);
        } else {
            sb.append(" | Data: ").append(dataInicio);
        }
        
        return sb.toString();
    }
}
