package br.com.extrator.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilitário para limpar todas as tabelas do banco de dados.
 * Remove dados com timestamp incorreto após correção do bug de timezone.
 */
public class LimpadorTabelas {
    private static final Logger logger = LoggerFactory.getLogger(LimpadorTabelas.class);
    
    public static void main(String[] args) {
        logger.info("🧹 Iniciando limpeza das tabelas...");
        
        try (Connection conexao = GerenciadorConexao.obterConexao()) {
            List<String> tabelas = obterListaTabelas(conexao);
            
            logger.info("📋 Encontradas {} tabelas para limpeza", tabelas.size());
            
            for (String tabela : tabelas) {
                limparTabela(conexao, tabela);
            }
            
            logger.info("✅ Limpeza concluída com sucesso!");
            
        } catch (SQLException e) {
            logger.error("❌ Erro durante a limpeza das tabelas: {}", e.getMessage(), e);
            System.exit(1);
        }
    }
    
    private static List<String> obterListaTabelas(Connection conexao) throws SQLException {
        List<String> tabelas = new ArrayList<>();
        
        String sql = "SELECT table_name FROM information_schema.tables " +
                    "WHERE table_schema = DATABASE() " +
                    "AND table_type = 'BASE TABLE' " +
                    "AND table_name NOT LIKE 'sys_%'";
        
        try (PreparedStatement stmt = conexao.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                String nomeTabela = rs.getString("table_name");
                tabelas.add(nomeTabela);
            }
        }
        
        return tabelas;
    }
    
    private static void limparTabela(Connection conexao, String nomeTabela) {
        try {
            String sql = "TRUNCATE TABLE " + nomeTabela;
            
            try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
                stmt.executeUpdate();
                logger.info("🗑️  Tabela {} limpa com sucesso", nomeTabela);
            }
            
        } catch (SQLException e) {
            logger.warn("⚠️  Não foi possível limpar a tabela {}: {}", nomeTabela, e.getMessage());
        }
    }
}