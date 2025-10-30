package br.com.extrator.comandos;

import java.time.Instant;
import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.extrator.auditoria.AuditoriaService;
import br.com.extrator.auditoria.ResultadoAuditoria;

/**
 * Comando responsável por executar auditoria de dados do sistema.
 */
public class ExecutarAuditoriaComando implements Comando {
    private static final Logger logger = LoggerFactory.getLogger(ExecutarAuditoriaComando.class);
    
    @Override
    public void executar(String[] args) throws Exception {
        System.out.println("📋 Executando auditoria de dados...");
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
                    
                    System.out.printf("📅 Executando auditoria para período: %s até %s%n", 
                                    dataInicioLocal, dataFimLocal);
                    
                    resultado = auditoriaService.executarAuditoriaPorPeriodo(dataInicio, dataFim);
                    
                } catch (final Exception e) {
                    System.err.println("❌ ERRO: Formato de data inválido. Use: YYYY-MM-DD YYYY-MM-DD");
                    System.err.println("Exemplo: --auditoria --periodo 2024-01-01 2024-01-31");
                    return;
                }
            } else {
                // Executar auditoria completa (padrão - últimas 24 horas)
                System.out.println("📅 Executando auditoria completa (últimas 24 horas)");
                resultado = auditoriaService.executarAuditoriaCompleta();
            }
            
            if (resultado != null && resultado.isSucesso()) {
                System.out.println("✅ Auditoria concluída com sucesso!");
            } else {
                System.out.println("⚠️  Auditoria concluída com alertas. Verifique os relatórios gerados.");
            }
            
        } catch (final Exception e) {
            logger.error("Erro na auditoria de dados: {}", e.getMessage(), e);
            System.err.println("❌ ERRO na auditoria: " + e.getMessage());
            throw e; // Re-propaga para tratamento de alto nível
        }
    }
}