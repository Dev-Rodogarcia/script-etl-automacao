package br.com.extrator.runners.common;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import br.com.extrator.api.ResultadoExtracao;
// DataExportEntityExtractor Ã© usado em instanceof e cast (linhas 54, 56, 79) - falso positivo do linter
import br.com.extrator.runners.common.DataExportEntityExtractor;
import br.com.extrator.util.configuracao.CarregadorConfig;
import br.com.extrator.util.console.LoggerConsole;
import br.com.extrator.util.validacao.ConstantesEntidades;

/**
 * Classe utilitÃ¡ria para logging padronizado e detalhado de extraÃ§Ãµes.
 * Fornece logs ricos com mÃ©tricas, estatÃ­sticas e informaÃ§Ãµes de performance.
 */
@SuppressWarnings("unused") // DataExportEntityExtractor Ã© usado em instanceof e cast (linhas 59, 60, 61, 85)
public class ExtractionLogger {
    private final LoggerConsole log;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    
    // ReferÃªncia estÃ¡tica ao tipo para forÃ§ar o linter a reconhecer o import
    private static final Class<?> DATA_EXPORT_EXTRACTOR_TYPE = DataExportEntityExtractor.class;
    
    public ExtractionLogger(final Class<?> clazz) {
        this.log = LoggerConsole.getLogger(clazz);
    }
    
    /**
     * Executa uma extraÃ§Ã£o com logging padronizado e detalhado.
     * 
     * @param extractor Extractor a ser executado
     * @param dataInicio Data de inÃ­cio
     * @param dataFim Data de fim
     * @param emoji Emoji para identificaÃ§Ã£o visual
     * @return Resultado da extraÃ§Ã£o
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
        log.info("{} {} INICIANDO EXTRAÃ‡ÃƒO: {}", displayEmoji, displayEmoji, entityName.toUpperCase());
        log.info("{}", "=".repeat(80));
        log.info("ðŸ“… PerÃ­odo: {} a {}", 
            formatarPeriodo(dataInicio, dataFim), 
            dataFim != null && !dataInicio.equals(dataFim) ? dataFim : dataInicio);
        log.info("â° InÃ­cio: {}", inicio.format(TIME_FORMATTER));
        log.info("{}", "-".repeat(80));

        int registrosExtraidosAteFalha = 0;
        int paginasProcessadasAteFalha = 0;
        
        try {
            final LocalDateTime inicioExtracao = LocalDateTime.now();
            final ResultadoExtracao<T> resultado = extractor.extract(dataInicio, dataFim);
            final LocalDateTime fimExtracao = LocalDateTime.now();
            final Duration duracaoExtracao = Duration.between(inicioExtracao, fimExtracao);
            
            final List<T> dtos = resultado.getDados();
            final int totalPaginas = resultado.getPaginasProcessadas();
            final boolean completo = resultado.isCompleto();
            final String statusMsg = completo ? "âœ… COMPLETO" : "âš ï¸ INCOMPLETO (" + resultado.getMotivoInterrupcao() + ")";
            registrosExtraidosAteFalha = resultado.getRegistrosExtraidos();
            paginasProcessadasAteFalha = totalPaginas;
            
            // Log de extraÃ§Ã£o detalhado
            log.info("{}", "-".repeat(80));
            log.info("ðŸ“Š RESULTADO DA EXTRAÃ‡ÃƒO:");
            log.info("   â€¢ Total extraÃ­do da API: {} registros", formatarNumero(dtos.size()));
            log.info("   â€¢ PÃ¡ginas processadas: {}", totalPaginas);
            log.info("   â€¢ Status: {}", statusMsg);
            final double segundosExtracao = duracaoExtracao.toMillis() / 1000.0;
            log.info("   â€¢ Tempo de extraÃ§Ã£o (apenas busca na API): {} ms ({} s)",
                duracaoExtracao.toMillis(),
                String.format("%.2f", segundosExtracao));
            log.info("      â†³ enriquecimento e gravaÃ§Ã£o entram no Tempo de salvamento abaixo");
            if (dtos.size() > 0 && duracaoExtracao.toMillis() > 0) {
                final double registrosPorSegundo = (dtos.size() * 1000.0) / duracaoExtracao.toMillis();
                log.info("   â€¢ Taxa de extraÃ§Ã£o: {} registros/segundo", String.format("%.2f", registrosPorSegundo));
            }
            
            int registrosSalvos = 0;
            int totalUnicos = dtos.size(); // PadrÃ£o para GraphQL
            int registrosInvalidos = 0;
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
                        registrosInvalidos = saveResult.getRegistrosInvalidos();
                        
                        final LocalDateTime fimSalvamento = LocalDateTime.now();
                        final Duration duracaoSalvamento = Duration.between(inicioSalvamento, fimSalvamento);
                        
                        if (dtos.size() != totalUnicos) {
                            final int duplicadosRemovidos = dtos.size() - totalUnicos;
                            final double percentualDuplicados = (duplicadosRemovidos * 100.0) / dtos.size();
                            log.warn("   âš ï¸ Duplicados removidos: {} ({}% do total)", 
                                formatarNumero(duplicadosRemovidos), String.format("%.2f", percentualDuplicados));
                        }
                        
                        log.info("{}", "-".repeat(80));
                        log.info("ðŸ’¾ RESULTADO DO SALVAMENTO (DataExport):");
                        log.info("   â€¢ Registros Ãºnicos apÃ³s deduplicaÃ§Ã£o: {}", formatarNumero(totalUnicos));
                        log.info("   â€¢ OperaÃ§Ãµes no banco (INSERTs + UPDATEs): {}", formatarNumero(registrosSalvos));
                        final double segundosSalvamento = duracaoSalvamento.toMillis() / 1000.0;
                        log.info("   â€¢ Tempo de salvamento: {} ms ({} s)", 
                            duracaoSalvamento.toMillis(), 
                            String.format("%.2f", segundosSalvamento));
                        if (totalUnicos > 0 && duracaoSalvamento.toMillis() > 0) {
                            final double registrosPorSegundo = (totalUnicos * 1000.0) / duracaoSalvamento.toMillis();
                            log.info("   â€¢ Taxa de salvamento: {} registros/segundo", String.format("%.2f", registrosPorSegundo));
                        }
                        if (registrosInvalidos > 0) {
                            log.warn("   âš ï¸ Registros invÃ¡lidos descartados: {}", formatarNumero(registrosInvalidos));
                        }
                    } else {
                        registrosSalvos = extractor.save(dtos);
                        if (ConstantesEntidades.USUARIOS_SISTEMA.equals(entityName)) {
                            // usuarios_sistema deduplica por user_id dentro do extractor antes do MERGE.
                            // Para consistÃªncia de status/log, "totalUnicos" deve refletir a cardinalidade deduplicada.
                            totalUnicos = registrosSalvos;
                        }
                        final LocalDateTime fimSalvamento = LocalDateTime.now();
                        final Duration duracaoSalvamento = Duration.between(inicioSalvamento, fimSalvamento);
                        
                        log.info("{}", "-".repeat(80));
                        log.info("ðŸ’¾ RESULTADO DO SALVAMENTO (GraphQL):");
                        log.info("   â€¢ Registros salvos: {}", formatarNumero(registrosSalvos));
                        final double segundosSalvamento = duracaoSalvamento.toMillis() / 1000.0;
                        log.info("   â€¢ Tempo de salvamento: {} ms ({} s)", 
                            duracaoSalvamento.toMillis(), 
                            String.format("%.2f", segundosSalvamento));
                        if (registrosSalvos > 0 && duracaoSalvamento.toMillis() > 0) {
                            final double registrosPorSegundo = (registrosSalvos * 1000.0) / duracaoSalvamento.toMillis();
                            log.info("   â€¢ Taxa de salvamento: {} registros/segundo", String.format("%.2f", registrosPorSegundo));
                        }
                    }
                } catch (final java.sql.SQLException e) {
                    log.error("âŒ ERRO CRÃTICO ao salvar {}: {}", entityName, e.getMessage());
                    throw new RuntimeException("Erro ao salvar " + entityName, e);
                }
            } else {
                log.info("   â„¹ï¸ Nenhum registro para salvar (lista vazia)");
            }
            
            final LocalDateTime fim = LocalDateTime.now();
            final Duration duracaoTotal = Duration.between(inicio, fim);
            final int totalRecebido = dtos.size();
            final int deltaIgnorados = Math.max(0, totalUnicos - registrosSalvos);
            final boolean salvamentoConsistente = registrosSalvos == totalUnicos;
            final boolean invalidosDentroTolerancia = isInvalidosDentroTolerancia(registrosInvalidos, totalRecebido);
            final String statusFinal = determinarStatusFinal(resultado, salvamentoConsistente, invalidosDentroTolerancia);
            final String motivoStatus = determinarMotivoStatus(
                resultado,
                salvamentoConsistente,
                invalidosDentroTolerancia,
                registrosInvalidos
            );
            final String mensagem = buildMensagem(
                dataInicio,
                dataFim,
                totalRecebido,
                registrosSalvos,
                totalUnicos,
                deltaIgnorados,
                registrosInvalidos,
                duracaoTotal,
                statusFinal,
                motivoStatus
            );

            if (!salvamentoConsistente) {
                log.error("âŒ DivergÃªncia de carga detectada em {}: Ãºnicos={} | salvos={}",
                    entityName, formatarNumero(totalUnicos), formatarNumero(registrosSalvos));
            }
            if (registrosInvalidos > 0 && !invalidosDentroTolerancia) {
                log.error("❌ Registros inválidos descartados em {}: {}", entityName, formatarNumero(registrosInvalidos));
            } else if (registrosInvalidos > 0) {
                final double percentualInvalidos = (registrosInvalidos * 100.0) / Math.max(1, totalRecebido);
                log.warn("⚠️ Registros inválidos descartados em {} dentro da tolerância operacional: {} ({}%)",
                    entityName,
                    formatarNumero(registrosInvalidos),
                    String.format("%.2f", percentualInvalidos));
            }
            log.info("   - ETL_DIAG status_code={} | reason_code={} | api_count={} | unique_count={} | db_upserts={} | invalid_count={} | pages={}",
                statusFinal,
                motivoStatus,
                formatarNumero(totalRecebido),
                formatarNumero(totalUnicos),
                formatarNumero(registrosSalvos),
                formatarNumero(registrosInvalidos),
                formatarNumero(totalPaginas));
            
            // Log de resumo final
            log.info("{}", "=".repeat(80));
            log.info("{} {} RESUMO FINAL: {}", displayEmoji, displayEmoji, entityName.toUpperCase());
            log.info("{}", "=".repeat(80));
            log.info("ðŸ“ˆ EstatÃ­sticas:");
            log.info("   â€¢ API â†’ DB: {} â†’ {} registros", formatarNumero(totalRecebido), formatarNumero(registrosSalvos));
            if (totalRecebido != totalUnicos) {
                log.info("   â€¢ Ãšnicos apÃ³s deduplicaÃ§Ã£o: {}", formatarNumero(totalUnicos));
            }
            if (deltaIgnorados > 0) {
                log.info("   â€¢ Ignorados/duplicados: {}", formatarNumero(deltaIgnorados));
            }
            log.info("   â€¢ PÃ¡ginas: {}", totalPaginas);
            log.info("   â€¢ Tempo total: {} ms ({} s)", 
                duracaoTotal.toMillis(), 
                String.format("%.2f", duracaoTotal.toMillis() / 1000.0));
            if (registrosInvalidos > 0) {
                log.info("   â€¢ Registros invÃ¡lidos descartados: {}", formatarNumero(registrosInvalidos));
            }
            log.info("   â€¢ Status: {}", formatarStatusHumano(statusFinal));
            log.info("â° Fim: {}", fim.format(TIME_FORMATTER));
            log.info("{}", "=".repeat(80));
            log.info(""); // Linha em branco para separaÃ§Ã£o visual
            
            // Usar sucessoComUnicos se for DataExport (tem deduplicaÃ§Ã£o)
            final boolean isDataExport = extractor instanceof DataExportEntityExtractor;
            if (isDataExport) {
                return ExtractionResult.sucessoComUnicos(entityName, inicio, resultado, registrosSalvos, totalUnicos, mensagem)
                    .status(statusFinal)
                    .build();
            } else {
                return ExtractionResult.sucesso(entityName, inicio, resultado, registrosSalvos, mensagem)
                    .status(statusFinal)
                    .build();
            }
                
        } catch (final Exception e) {
            final LocalDateTime fim = LocalDateTime.now();
            final Duration duracaoTotal = Duration.between(inicio, fim);
            log.error("{}", "=".repeat(80));
            log.error("âŒ ERRO NA EXTRAÃ‡ÃƒO: {}", entityName.toUpperCase());
            log.error("{}", "=".repeat(80));
            log.error("   â€¢ Erro: {}", e.getMessage());
            log.error("   â€¢ Tipo: {}", e.getClass().getSimpleName());
            log.error("   â€¢ Tempo atÃ© erro: {} ms ({} s)", 
                duracaoTotal.toMillis(), 
                String.format("%.2f", duracaoTotal.toMillis() / 1000.0));
            if (registrosExtraidosAteFalha > 0 || paginasProcessadasAteFalha > 0) {
                log.error("   â€¢ Progresso antes da falha: {} registros da API, {} pÃ¡ginas",
                    formatarNumero(registrosExtraidosAteFalha),
                    formatarNumero(paginasProcessadasAteFalha));
            }
            log.error("{}", "=".repeat(80));
            log.error(""); // Linha em branco para separaÃ§Ã£o visual
            return ExtractionResult.erroComParcial(
                entityName,
                inicio,
                e,
                registrosExtraidosAteFalha,
                paginasProcessadasAteFalha
            ).build();
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
                                 final int registrosInvalidos,
                                 final Duration duracaoTotal,
                                 final String statusCode,
                                 final String reasonCode) {
        final StringBuilder sb = new StringBuilder();
        sb.append("API: ").append(formatarNumero(totalRecebido)).append(" recebidos");
        if (totalRecebido != totalUnicos) {
            sb.append(" (Ãºnicos: ").append(formatarNumero(totalUnicos)).append(")");
        }
        sb.append(" | DB: ").append(formatarNumero(registrosSalvos)).append(" processados");
        if (deltaIgnorados > 0) {
            sb.append(" | Delta: ").append(formatarNumero(deltaIgnorados)).append(" (duplicados/ignorados)");
        }
        if (registrosInvalidos > 0) {
            sb.append(" | InvÃ¡lidos descartados: ").append(formatarNumero(registrosInvalidos));
        }
        sb.append(" | Tempo: ").append(duracaoTotal.toMillis()).append("ms");
        
        if (dataFim != null && !dataInicio.equals(dataFim)) {
            sb.append(" | PerÃ­odo: ").append(dataInicio).append(" a ").append(dataFim);
        } else {
            sb.append(" | Data: ").append(dataInicio);
        }
        sb.append(" | status_code=").append(statusCode);
        sb.append(" | reason_code=").append(reasonCode);
        sb.append(" | api_count=").append(totalRecebido);
        sb.append(" | unique_count=").append(totalUnicos);
        sb.append(" | db_upserts=").append(registrosSalvos);
        sb.append(" | invalid_count=").append(registrosInvalidos);
        
        return sb.toString();
    }

    private String determinarStatusFinal(final ResultadoExtracao<?> resultado,
                                         final boolean salvamentoConsistente,
                                         final boolean invalidosDentroTolerancia) {
        if (!resultado.isCompleto()) {
            final String motivo = resultado.getMotivoInterrupcao();
            if (ResultadoExtracao.MotivoInterrupcao.ERRO_API.getCodigo().equals(motivo)
                || ResultadoExtracao.MotivoInterrupcao.CIRCUIT_BREAKER.getCodigo().equals(motivo)) {
                return ConstantesEntidades.STATUS_ERRO_API;
            }
            return ConstantesEntidades.STATUS_INCOMPLETO_LIMITE;
        }
        if (!salvamentoConsistente) {
            return ConstantesEntidades.STATUS_INCOMPLETO_DB;
        }
        if (!invalidosDentroTolerancia) {
            return ConstantesEntidades.STATUS_INCOMPLETO_DADOS;
        }
        return ConstantesEntidades.STATUS_COMPLETO;
    }

    private String determinarMotivoStatus(final ResultadoExtracao<?> resultado,
                                          final boolean salvamentoConsistente,
                                          final boolean invalidosDentroTolerancia,
                                          final int registrosInvalidos) {
        if (!resultado.isCompleto()) {
            final String motivo = resultado.getMotivoInterrupcao();
            return motivo != null && !motivo.isBlank()
                ? motivo
                : ResultadoExtracao.MotivoInterrupcao.LIMITE_PAGINAS.getCodigo();
        }
        if (!salvamentoConsistente) {
            return "DIVERGENCIA_SALVAMENTO";
        }
        if (!invalidosDentroTolerancia) {
            return "DADOS_INVALIDOS_ORIGEM";
        }
        if (registrosInvalidos > 0) {
            return "INVALIDOS_TOLERADOS";
        }
        return "OK";
    }

    private boolean isInvalidosDentroTolerancia(final int registrosInvalidos, final int totalRecebido) {
        if (registrosInvalidos <= 0) {
            return true;
        }

        final int limiteAbsoluto = CarregadorConfig.obterMaxInvalidosToleradosPorEntidade();
        final double limitePercentual = CarregadorConfig.obterPercentualMaxInvalidosToleradosPorEntidade();
        final double percentualInvalidos = (registrosInvalidos * 100.0) / Math.max(1, totalRecebido);

        return registrosInvalidos <= limiteAbsoluto && percentualInvalidos <= limitePercentual;
    }

    private String formatarStatusHumano(final String statusCode) {
        if (ConstantesEntidades.STATUS_COMPLETO.equals(statusCode)) {
            return "âœ… COMPLETO";
        }
        return "âš ï¸ " + statusCode;
    }
}



