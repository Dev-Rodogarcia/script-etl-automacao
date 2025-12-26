package br.com.extrator.comandos.validacao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.extrator.comandos.base.Comando;
import br.com.extrator.util.banco.GerenciadorConexao;

/**
 * Comando para verificar o timezone do SQL Server e diagnosticar problemas de timestamp
 */
public class VerificarTimezoneComando implements Comando {
    
    private static final Logger logger = LoggerFactory.getLogger(VerificarTimezoneComando.class);
    
    @Override
    public void executar(String[] args) {
        logger.info("🕐 Iniciando verificação de timezone do SQL Server...");
        
        try (Connection conn = GerenciadorConexao.obterConexao()) {
            verificarTimezone(conn);
        } catch (SQLException e) {
            logger.error("❌ Erro ao verificar timezone: {}", e.getMessage(), e);
        }
    }
    
    private void verificarTimezone(Connection conn) throws SQLException {
        String query = """
            SELECT
                SYSDATETIMEOFFSET() as horario_com_timezone,
                GETDATE() as horario_sem_timezone,
                CURRENT_TIMEZONE() as timezone_servidor,
                GETUTCDATE() as horario_utc
            """;
        
        try (PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                logger.info("📊 DIAGNÓSTICO DE TIMEZONE DO SQL SERVER:");
                logger.info("   🕐 Horário com timezone: {}", rs.getString("horario_com_timezone"));
                logger.info("   🕐 GETDATE() (local): {}", rs.getString("horario_sem_timezone"));
                logger.info("   🕐 GETUTCDATE() (UTC): {}", rs.getString("horario_utc"));
                logger.info("   🌍 Timezone do servidor: {}", rs.getString("timezone_servidor"));
                
                String timezone = rs.getString("timezone_servidor");
                if (!"E. South America Standard Time".equals(timezone)) {
                    logger.warn("⚠️  PROBLEMA DETECTADO!");
                    logger.warn("   Timezone atual: {}", timezone);
                    logger.warn("   Timezone esperado: E. South America Standard Time");
                    logger.warn("   SOLUÇÃO: Usar Timestamp.from(Instant.now()) no Java em vez de GETDATE()");
                } else {
                    logger.info("✅ Timezone está correto para Brasília");
                }
            }
        }
    }
    

}