package br.com.extrator.comandos;

import br.com.extrator.util.GerenciadorConexao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Comando para verificar os timestamps no banco de dados
 */
public class VerificarTimestampsComando implements Comando {
    private static final Logger logger = LoggerFactory.getLogger(VerificarTimestampsComando.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);

    @Override
    public void executar(String[] args) throws Exception {
        logger.info("🔍 Verificando timestamps no banco de dados...");
        
        try (Connection conexao = GerenciadorConexao.obterConexao()) {
            verificarTimestampsTabela(conexao, "fretes");
            verificarTimestampsTabela(conexao, "coletas");
            verificarTimestampsTabela(conexao, "cotacoes");
        }
        
        logger.info("✅ Verificação de timestamps concluída!");
    }
    
    private void verificarTimestampsTabela(Connection conexao, String nomeTabela) {
        try {
            // Verifica se a tabela existe
            String sqlExiste = "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = ?";
            try (PreparedStatement stmt = conexao.prepareStatement(sqlExiste)) {
                stmt.setString(1, nomeTabela);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) == 0) {
                        logger.info("📋 Tabela {} não existe", nomeTabela);
                        return;
                    }
                }
            }
            
            // Verifica registros e timestamps
            String sql = "SELECT COUNT(*) as total, " +
                        "MIN(data_extracao) as min_timestamp, " +
                        "MAX(data_extracao) as max_timestamp " +
                        "FROM " + nomeTabela;
            
            try (PreparedStatement stmt = conexao.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                
                if (rs.next()) {
                    int total = rs.getInt("total");
                    logger.info("📋 Tabela {}: {} registros", nomeTabela, total);
                    
                    if (total > 0) {
                        java.sql.Timestamp minTs = rs.getTimestamp("min_timestamp");
                        java.sql.Timestamp maxTs = rs.getTimestamp("max_timestamp");
                        
                        if (minTs != null && maxTs != null) {
                            Instant minInstant = minTs.toInstant();
                            Instant maxInstant = maxTs.toInstant();
                            
                            logger.info("   📅 Timestamp mais antigo: {}", FORMATTER.format(minInstant));
                            logger.info("   📅 Timestamp mais recente: {}", FORMATTER.format(maxInstant));
                            
                            // Verifica se os timestamps são recentes (últimas 24h)
                            Instant agora = Instant.now();
                            Instant inicio24h = agora.minusSeconds(24 * 60 * 60);
                            
                            if (maxInstant.isAfter(inicio24h)) {
                                logger.info("   ✅ Timestamps estão atualizados (últimas 24h)");
                            } else {
                                logger.warn("   ⚠️ Timestamps podem estar desatualizados");
                            }
                        }
                    }
                }
            }
            
        } catch (SQLException e) {
            logger.error("❌ Erro ao verificar tabela {}: {}", nomeTabela, e.getMessage());
        }
    }
}