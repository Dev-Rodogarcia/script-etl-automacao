package br.com.extrator.auditoria;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Classe responsável por validar a completude e integridade dos dados
 * extraídos das APIs do ESL Cloud.
 * 
 * Verifica se os dados foram extraídos corretamente, identifica lacunas
 * e inconsistências nos dados armazenados.
 */
public class AuditoriaValidator {
    private static final Logger logger = LoggerFactory.getLogger(AuditoriaValidator.class);
    
    // Mapeamento das entidades e suas colunas de data relevantes
    private static final Map<String, String> COLUNAS_DATA_ENTIDADES = Map.of(
        "cotacoes", "requested_at",
        "coletas", "service_date", 
        "faturas_a_pagar", "due_date",
        "faturas_a_receber", "due_date",
        "fretes", "servico_em",
        "manifestos", "created_at",
        "ocorrencias", "occurrence_at",
        "localizacao_cargas", "service_at"
    );
    
    /**
     * Valida uma entidade específica verificando completude dos dados.
     * 
     * @param conexao Conexão com o banco de dados
     * @param nomeEntidade Nome da entidade a ser validada
     * @param dataInicio Data de início do período
     * @param dataFim Data de fim do período
     * @return ResultadoValidacaoEntidade com o resultado da validação
     */
    public ResultadoValidacaoEntidade validarEntidade(final Connection conexao, final String nomeEntidade, 
                                                     final LocalDateTime dataInicio, final LocalDateTime dataFim) {
        final ResultadoValidacaoEntidade resultado = new ResultadoValidacaoEntidade();
        resultado.setNomeEntidade(nomeEntidade);
        resultado.setDataInicio(dataInicio);
        resultado.setDataFim(dataFim);
        
        try {
            // Verificar se a tabela existe
            if (!verificarExistenciaTabela(conexao, nomeEntidade)) {
                resultado.setErro("Tabela não encontrada: " + nomeEntidade);
                resultado.setStatus(StatusValidacao.ERRO);
                return resultado;
            }
            
            // Contar registros no período
            final long totalRegistros = contarRegistrosPorPeriodo(conexao, nomeEntidade, dataInicio, dataFim);
            resultado.setTotalRegistros(totalRegistros);
            
            // Contar registros das últimas 24 horas
            final LocalDateTime agora = LocalDateTime.now();
            final LocalDateTime inicio24h = agora.minusHours(24);
            final long registros24h = contarRegistrosPorPeriodo(conexao, nomeEntidade, inicio24h, agora);
            resultado.setRegistrosUltimas24h(registros24h);
            
            // Verificar registros com dados nulos críticos
            final long registrosComNulos = contarRegistrosComNulos(conexao, nomeEntidade);
            resultado.setRegistrosComNulos(registrosComNulos);
            
            // Verificar último registro extraído
            final LocalDateTime ultimaExtracao = obterDataUltimaExtracao(conexao, nomeEntidade);
            resultado.setUltimaExtracao(ultimaExtracao);
            
            // Determinar status da validação
            determinarStatusValidacao(resultado);
            
            logger.debug("Validação da entidade {} concluída. Status: {}, Registros: {}", 
                        nomeEntidade, resultado.getStatus(), totalRegistros);
            
        } catch (final SQLException e) {
            logger.error("Erro ao validar entidade {}: {}", nomeEntidade, e.getMessage(), e);
            resultado.setErro("Erro SQL: " + e.getMessage());
            resultado.setStatus(StatusValidacao.ERRO);
        }
        
        return resultado;
    }
    
    /**
     * Verifica se existem dados recentes (últimas 24 horas) em todas as entidades.
     * 
     * @param conexao Conexão com o banco de dados
     * @param dataInicio Data de início do período
     * @param dataFim Data de fim do período
     * @return true se existem dados recentes, false caso contrário
     */
    public boolean verificarExistenciaDadosRecentes(final Connection conexao, final LocalDateTime dataInicio, final LocalDateTime dataFim) {
        try {
            final List<String> entidades = List.of(
                "cotacoes", "coletas", "faturas_a_pagar", "faturas_a_receber", 
                "fretes", "manifestos", "ocorrencias", "localizacao_cargas"
            );
            
            for (final String entidade : entidades) {
                if (verificarExistenciaTabela(conexao, entidade)) {
                    final long registros = contarRegistrosPorPeriodo(conexao, entidade, dataInicio, dataFim);
                    if (registros > 0) {
                        return true; // Pelo menos uma entidade tem dados recentes
                    }
                }
            }
            
            return false; // Nenhuma entidade tem dados recentes
            
        } catch (final SQLException e) {
            logger.error("Erro ao verificar existência de dados recentes: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Verifica se uma tabela existe no banco de dados.
     */
    private boolean verificarExistenciaTabela(final Connection conexao, final String nomeTabela) throws SQLException {
        final String sql = """
            SELECT COUNT(*)
            FROM INFORMATION_SCHEMA.TABLES
            WHERE TABLE_NAME = ? AND TABLE_SCHEMA = 'dbo'
            """;
        
        try (final PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setString(1, nomeTabela);
            try (final ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }
    
    /**
     * Conta registros de uma entidade em um período específico.
     */
    private long contarRegistrosPorPeriodo(final Connection conexao, final String nomeEntidade, 
                                          final LocalDateTime dataInicio, final LocalDateTime dataFim) throws SQLException {
        final String colunaData = COLUNAS_DATA_ENTIDADES.get(nomeEntidade);
        if (colunaData == null) {
            // Se não temos coluna de data específica, usar data_extracao
            return contarRegistrosPorDataExtracao(conexao, nomeEntidade, dataInicio, dataFim);
        }
        
        final String sql = String.format("""
            SELECT COUNT(*)
            FROM %s
            WHERE %s >= ? AND %s <= ?
            """, nomeEntidade, colunaData, colunaData);
        
        try (final PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setString(1, dataInicio.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            stmt.setString(2, dataFim.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            try (final ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        }
    }
    
    /**
     * Conta registros por data de extração (fallback quando não há coluna de data específica).
     */
    private long contarRegistrosPorDataExtracao(final Connection conexao, final String nomeEntidade, 
                                               final LocalDateTime dataInicio, final LocalDateTime dataFim) throws SQLException {
        final String sql = String.format("""
            SELECT COUNT(*)
            FROM %s
            WHERE data_extracao >= ? AND data_extracao <= ?
            """, nomeEntidade);
        
        try (final PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setString(1, dataInicio.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            stmt.setString(2, dataFim.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            try (final ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        }
    }
    
    /**
     * Conta registros com campos críticos nulos.
     */
    private long contarRegistrosComNulos(final Connection conexao, final String nomeEntidade) throws SQLException {
        // Verificar campos críticos específicos por entidade
        final Map<String, String> camposCriticos = Map.of(
            "cotacoes", "sequence_code IS NULL OR total_value IS NULL",
            "coletas", "id IS NULL",
            "faturas_a_pagar", "id IS NULL OR document_number IS NULL",
            "faturas_a_receber", "id IS NULL OR document_number IS NULL",
            "fretes", "id IS NULL",
            "manifestos", "sequence_code IS NULL",
            "ocorrencias", "id IS NULL",
            "localizacao_cargas", "sequence_number IS NULL"
        );
        
        final String condicaoNulos = camposCriticos.getOrDefault(nomeEntidade, "id IS NULL");
        final String sql = String.format("SELECT COUNT(*) FROM %s WHERE %s", nomeEntidade, condicaoNulos);
        
        try (final PreparedStatement stmt = conexao.prepareStatement(sql);
             final ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0;
        }
    }
    
    /**
     * Obtém a data da última extração de dados.
     */
    private LocalDateTime obterDataUltimaExtracao(final Connection conexao, final String nomeEntidade) throws SQLException {
        final String sql = String.format("SELECT MAX(data_extracao) FROM %s", nomeEntidade);
        
        try (final PreparedStatement stmt = conexao.prepareStatement(sql);
             final ResultSet rs = stmt.executeQuery()) {
            if (rs.next() && rs.getTimestamp(1) != null) {
                return rs.getTimestamp(1).toLocalDateTime();
            }
            return null;
        }
    }
    
    /**
     * Determina o status da validação com base nos resultados obtidos.
     */
    private void determinarStatusValidacao(final ResultadoValidacaoEntidade resultado) {
        if (resultado.getErro() != null) {
            resultado.setStatus(StatusValidacao.ERRO);
            return;
        }
        
        // Verificar se há dados recentes (últimas 24 horas)
        if (resultado.getRegistrosUltimas24h() == 0) {
            resultado.setStatus(StatusValidacao.ALERTA);
            resultado.adicionarObservacao("Nenhum registro encontrado nas últimas 24 horas");
            return;
        }
        
        // Verificar se há muitos registros com nulos
        final double percentualNulos = resultado.getTotalRegistros() > 0 ? 
            (double) resultado.getRegistrosComNulos() / resultado.getTotalRegistros() * 100 : 0;
        
        if (percentualNulos > 10) { // Mais de 10% com nulos é preocupante
            resultado.setStatus(StatusValidacao.ALERTA);
            resultado.adicionarObservacao(String.format("%.1f%% dos registros possuem campos críticos nulos", percentualNulos));
            return;
        }
        
        // Verificar se a última extração é muito antiga (mais de 25 horas)
        if (resultado.getUltimaExtracao() != null) {
            final long horasDesdeUltimaExtracao = java.time.Duration.between(resultado.getUltimaExtracao(), LocalDateTime.now()).toHours();
            if (horasDesdeUltimaExtracao > 25) {
                resultado.setStatus(StatusValidacao.ALERTA);
                resultado.adicionarObservacao(String.format("Última extração há %d horas", horasDesdeUltimaExtracao));
                return;
            }
        }
        
        // Se chegou até aqui, está tudo OK
        resultado.setStatus(StatusValidacao.OK);
    }
}