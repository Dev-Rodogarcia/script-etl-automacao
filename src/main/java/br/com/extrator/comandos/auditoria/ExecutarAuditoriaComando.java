package br.com.extrator.comandos.auditoria;

import java.time.Instant;
import java.time.LocalDate;

import br.com.extrator.auditoria.modelos.ResultadoAuditoria;
import br.com.extrator.auditoria.servicos.AuditoriaService;
import br.com.extrator.comandos.base.Comando;
import br.com.extrator.util.console.LoggerConsole;

/**
 * Comando responsável por executar auditoria de dados do sistema.
 */
public class ExecutarAuditoriaComando implements Comando {
    // PROBLEMA #9 CORRIGIDO: Usar LoggerConsole para log duplo
    private static final LoggerConsole log = LoggerConsole.getLogger(ExecutarAuditoriaComando.class);
    
    @Override
    public void executar(String[] args) throws Exception {
        log.info("📋 Executando auditoria de dados...");
        try {
            final AuditoriaService auditoriaService = new AuditoriaService();
            ResultadoAuditoria resultado;
            
            // Verificar se foi especificado um período customizado
            if (args.length >= 4 && "--periodo".equals(args[1])) {
                try {
                    final LocalDate dataInicioLocal = LocalDate.parse(args[2]);
                    final LocalDate dataFimLocal = LocalDate.parse(args[3]);
                    
                    // Converter para Instant (início do dia e fim do dia em UTC)
                    final Instant dataInicio = dataInicioLocal.atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
                    final Instant dataFim = dataFimLocal.atTime(23, 59, 59).toInstant(java.time.ZoneOffset.UTC);
                    
                    log.info("📅 Executando auditoria para período: {} até {}", dataInicioLocal, dataFimLocal);
                    
                    resultado = auditoriaService.executarAuditoriaPorPeriodo(dataInicio, dataFim);
                    
                } catch (final Exception e) {
                    log.error("❌ ERRO: Formato de data inválido. Use: YYYY-MM-DD YYYY-MM-DD");
                    log.error("Exemplo: --auditoria --periodo 2024-01-01 2024-01-31");
                    return;
                }
            } else {
                // Executar auditoria completa (padrão - últimas 24 horas)
                log.info("📅 Executando auditoria completa (últimas 24 horas)");
                resultado = auditoriaService.executarAuditoriaCompleta();
            }
            
            if (resultado != null && resultado.isSucesso()) {
                log.info("✅ Auditoria concluída com sucesso!");
            } else {
                log.warn("⚠️ Auditoria concluída com alertas. Verifique os relatórios gerados.");
            }
            
        } catch (final Exception e) {
            log.error("❌ ERRO na auditoria: {}", e.getMessage(), e);
            throw e;
        }
    }
}