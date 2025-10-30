package br.com.extrator.db.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.extrator.db.entity.LogExtracaoEntity;
import br.com.extrator.db.entity.LogExtracaoEntity.StatusExtracao;
import br.com.extrator.util.GerenciadorConexao;

/**
 * Repository para gerenciar logs de extração
 */
public class LogExtracaoRepository {
    
    private static final Logger logger = LoggerFactory.getLogger(LogExtracaoRepository.class);
    
    /**
     * Grava um novo log de extração
     */
    public void gravarLogExtracao(LogExtracaoEntity logExtracao) {
        String sql = """
            INSERT INTO log_extracoes
            (entidade, timestamp_inicio, timestamp_fim, status_final, registros_extraidos, paginas_processadas, mensagem)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (Connection conn = GerenciadorConexao.obterConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, logExtracao.getEntidade());
            stmt.setTimestamp(2, Timestamp.valueOf(logExtracao.getTimestampInicio()));
            stmt.setTimestamp(3, Timestamp.valueOf(logExtracao.getTimestampFim()));
            stmt.setString(4, logExtracao.getStatusFinal().getValor());
            stmt.setInt(5, logExtracao.getRegistrosExtraidos());
            stmt.setInt(6, logExtracao.getPaginasProcessadas());
            stmt.setString(7, logExtracao.getMensagem());
            
            int linhasAfetadas = stmt.executeUpdate();
            
            if (linhasAfetadas > 0) {
                logger.debug("Log de extração gravado: entidade={}, status={}, registros={}", 
                    logExtracao.getEntidade(), logExtracao.getStatusFinal(), logExtracao.getRegistrosExtraidos());
            }
            
        } catch (SQLException e) {
            logger.error("Erro ao gravar log de extração para entidade {}: {}", 
                logExtracao.getEntidade(), e.getMessage(), e);
            throw new RuntimeException("Falha ao gravar log de extração", e);
        }
    }
    
    /**
     * Busca o último log de extração para uma entidade
     */
    public Optional<LogExtracaoEntity> buscarUltimoLogPorEntidade(String entidade) {
        String sql = """
            SELECT TOP 1 id, entidade, timestamp_inicio, timestamp_fim, status_final,
                   registros_extraidos, paginas_processadas, mensagem
            FROM log_extracoes
            WHERE entidade = ?
            ORDER BY timestamp_fim DESC
            """;
        
        try (Connection conn = GerenciadorConexao.obterConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, entidade);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    LogExtracaoEntity log = new LogExtracaoEntity();
                    log.setId(rs.getLong("id"));
                    log.setEntidade(rs.getString("entidade"));
                    log.setTimestampInicio(rs.getTimestamp("timestamp_inicio").toLocalDateTime());
                    log.setTimestampFim(rs.getTimestamp("timestamp_fim").toLocalDateTime());
                    log.setStatusFinal(StatusExtracao.fromString(rs.getString("status_final")));
                    log.setRegistrosExtraidos(rs.getInt("registros_extraidos"));
                    log.setPaginasProcessadas(rs.getInt("paginas_processadas"));
                    log.setMensagem(rs.getString("mensagem"));
                    
                    return Optional.of(log);
                }
            }
            
        } catch (SQLException e) {
            logger.error("Erro ao buscar último log para entidade {}: {}", entidade, e.getMessage(), e);
        }
        
        return Optional.empty();
    }
    
    /**
     * Verifica se a tabela log_extracoes existe
     */
    public boolean tabelaExiste() {
        String sql = """
            SELECT COUNT(*)
            FROM INFORMATION_SCHEMA.TABLES
            WHERE TABLE_NAME = 'log_extracoes'
            """;
        
        try (Connection conn = GerenciadorConexao.obterConexao();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            
        } catch (SQLException e) {
            logger.error("Erro ao verificar existência da tabela log_extracoes: {}", e.getMessage(), e);
        }
        
        return false;
    }
    
    /**
     * Cria a tabela log_extracoes se não existir
     */
    public void criarTabelaSeNaoExistir() {
        if (tabelaExiste()) {
            logger.debug("Tabela log_extracoes já existe");
            return;
        }
        
        String sql = """
            CREATE TABLE log_extracoes (
                id BIGINT IDENTITY PRIMARY KEY,
                entidade NVARCHAR(50) NOT NULL,
                timestamp_inicio DATETIME2 NOT NULL,
                timestamp_fim DATETIME2 NOT NULL,
                status_final NVARCHAR(20) NOT NULL,
                registros_extraidos INT NOT NULL,
                paginas_processadas INT NOT NULL,
                mensagem NVARCHAR(MAX)
            );
            
            CREATE INDEX idx_entidade_timestamp ON log_extracoes (entidade, timestamp_fim DESC);
            """;
        
        try (Connection conn = GerenciadorConexao.obterConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.executeUpdate();
            logger.info("✅ Tabela log_extracoes criada com sucesso");
            
        } catch (SQLException e) {
            logger.error("❌ Erro ao criar tabela log_extracoes: {}", e.getMessage(), e);
            throw new RuntimeException("Falha ao criar tabela log_extracoes", e);
        }
    }
}