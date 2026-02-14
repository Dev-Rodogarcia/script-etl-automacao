package br.com.extrator.auditoria.validacao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.extrator.auditoria.enums.StatusValidacao;
import br.com.extrator.auditoria.modelos.ResultadoValidacaoEntidade;
import br.com.extrator.db.entity.LogExtracaoEntity;
import br.com.extrator.db.entity.LogExtracaoEntity.StatusExtracao;
import br.com.extrator.db.repository.LogExtracaoRepository;
import br.com.extrator.util.validacao.ConstantesEntidades;

/**
 * Classe responsÃ¡vel por validar a completude e integridade dos dados
 * extraÃ­dos das APIs do ESL Cloud.
 * 
 * Verifica se os dados foram extraÃ­dos corretamente, identifica lacunas
 * e inconsistÃªncias nos dados armazenados.
 * 
 * VERSÃƒO CORRIGIDA: Agora confia no log_extracoes quando a extraÃ§Ã£o foi COMPLETA,
 * eliminando falsos-positivos causados por dados de mÃºltiplas extraÃ§Ãµes.
 */
public class AuditoriaValidator {
    private static final Logger logger = LoggerFactory.getLogger(AuditoriaValidator.class);
    
    // Cache para validaÃ§Ãµes de colunas (evita consultas repetidas)
    private final Map<String, Boolean> cacheValidacaoColunas = new HashMap<>();
    
    // Repository para consultar logs de extraÃ§Ã£o
    private final LogExtracaoRepository logExtracaoRepository;
    
    /**
     * Construtor que inicializa o repository de logs de extraÃ§Ã£o.
     */
    public AuditoriaValidator() {
        this.logExtracaoRepository = new LogExtracaoRepository();
        // NOTA: As tabelas devem ser criadas via scripts SQL da pasta database/
        // O LogExtracaoRepository nÃ£o cria mais tabelas automaticamente
    }
    
    /**
     * Consulta o log de extraÃ§Ã£o mais recente para uma entidade
     * 
     * @param nomeEntidade Nome da entidade a ser consultada
     * @param dataInicio Data de inÃ­cio do perÃ­odo de interesse
     * @return LogExtracaoEntity da Ãºltima extraÃ§Ã£o ou null se nÃ£o encontrada
     */
    private LogExtracaoEntity consultarLogExtracao(final String nomeEntidade, final Instant dataInicio) {
        try {
            logger.debug("Consultando log de extraÃ§Ã£o para entidade: {}", nomeEntidade);
            
            final Optional<LogExtracaoEntity> ultimoLog = logExtracaoRepository.buscarUltimoLogPorEntidade(nomeEntidade);
            
            if (ultimoLog.isPresent()) {
                final LogExtracaoEntity log = ultimoLog.get();
                
                // Verificar se o log Ã© recente (dentro do perÃ­odo de interesse)
                final LocalDateTime dataInicioLocal = LocalDateTime.ofInstant(dataInicio, ZoneOffset.UTC);
                if (log.getTimestampFim().isAfter(dataInicioLocal)) {
                    logger.info("Log de extraÃ§Ã£o encontrado para {}: Status={}, Registros={}, PÃ¡ginas={}", 
                        nomeEntidade, log.getStatusFinal(), log.getRegistrosExtraidos(), log.getPaginasProcessadas());
                    return log;
                } else {
                    logger.debug("Log de extraÃ§Ã£o encontrado para {} mas Ã© anterior ao perÃ­odo de interesse", nomeEntidade);
                }
            } else {
                logger.debug("Nenhum log de extraÃ§Ã£o encontrado para {}", nomeEntidade);
            }
            
        } catch (final Exception e) {
            logger.error("Erro ao consultar log de extraÃ§Ã£o para {}: {}", nomeEntidade, e.getMessage(), e);
        }
        
        return null;
    }
    
    /**
     * Valida uma entidade especÃ­fica verificando completude dos dados.
     * 
     * VERSÃƒO CORRIGIDA: Agora confia no log_extracoes quando status = COMPLETO.
     * NÃ£o compara com contagem do banco para evitar falsos-positivos causados
     * por dados de mÃºltiplas extraÃ§Ãµes.
     * 
     * @param conexao ConexÃ£o com o banco de dados
     * @param nomeEntidade Nome da entidade a ser validada
     * @param dataInicio Data de inÃ­cio do perÃ­odo
     * @param dataFim Data de fim do perÃ­odo
     * @return ResultadoValidacaoEntidade com o resultado da validaÃ§Ã£o
     */
    public ResultadoValidacaoEntidade validarEntidade(final Connection conexao, final String nomeEntidade, 
                                                     final Instant dataInicio, final Instant dataFim) {
        logger.info("ðŸ” Auditando {}...", nomeEntidade);
        logger.debug("ParÃ¢metros: dataInicio={}, dataFim={}", dataInicio, dataFim);
        
        final ResultadoValidacaoEntidade resultado = new ResultadoValidacaoEntidade();
        resultado.setNomeEntidade(nomeEntidade);
        resultado.setDataInicio(dataInicio);
        resultado.setDataFim(dataFim);
        final String nomeTabela = mapearNomeTabela(nomeEntidade);
        
        // PASSO 1: Consultar log de extraÃ§Ãµes primeiro
        final LogExtracaoEntity logExtracao = consultarLogExtracao(nomeEntidade, dataInicio);
        if (logExtracao != null) {
             logger.info("ðŸ“Š Log de extraÃ§Ã£o encontrado para {}: status={}, registros={}, pÃ¡ginas={}", 
                 nomeEntidade, logExtracao.getStatusFinal(), logExtracao.getRegistrosExtraidos(), logExtracao.getPaginasProcessadas());
             
             // Se a extraÃ§Ã£o foi interrompida ou teve erro, ajustar expectativas
             switch (logExtracao.getStatusFinal()) {
                 case INCOMPLETO_LIMITE -> {
                     logger.warn("âš ï¸ ExtraÃ§Ã£o de {} foi interrompida por limite. Detalhes: {}", nomeEntidade, logExtracao.getMensagem());
                     resultado.adicionarObservacao("ExtraÃ§Ã£o interrompida por limite: " + logExtracao.getMensagem());
                 }
                 case INCOMPLETO_DADOS -> {
                     logger.warn("âš ï¸ ExtraÃ§Ã£o de {} concluiu com dados invÃ¡lidos descartados. Detalhes: {}", nomeEntidade, logExtracao.getMensagem());
                     resultado.adicionarObservacao("ExtraÃ§Ã£o com dados invÃ¡lidos na origem: " + logExtracao.getMensagem());
                 }
                 case INCOMPLETO_DB -> {
                     logger.warn("âš ï¸ ExtraÃ§Ã£o de {} concluiu com divergÃªncia de salvamento. Detalhes: {}", nomeEntidade, logExtracao.getMensagem());
                     resultado.adicionarObservacao("ExtraÃ§Ã£o com divergÃªncia de persistÃªncia: " + logExtracao.getMensagem());
                 }
                 case INCOMPLETO -> {
                     logger.warn("âš ï¸ ExtraÃ§Ã£o de {} ficou incompleta sem categoria especÃ­fica. Detalhes: {}", nomeEntidade, logExtracao.getMensagem());
                     resultado.adicionarObservacao("ExtraÃ§Ã£o incompleta (status legado): " + logExtracao.getMensagem());
                 }
                 case ERRO_API -> {
                     logger.warn("âŒ ExtraÃ§Ã£o de {} teve erro de API. Detalhes: {}", nomeEntidade, logExtracao.getMensagem());
                     resultado.adicionarObservacao("Erro na extraÃ§Ã£o: " + logExtracao.getMensagem());
                 }
                 case COMPLETO -> {
                     logger.info("âœ… ExtraÃ§Ã£o de {} foi completada com sucesso", nomeEntidade);
                     resultado.adicionarObservacao("ExtraÃ§Ã£o completada com sucesso");
                 }
                 default -> logger.debug("Status de extraÃ§Ã£o nÃ£o reconhecido: {}", logExtracao.getStatusFinal());
             }
         } else {
             logger.warn("âš ï¸ Nenhum log de extraÃ§Ã£o encontrado para {} no perÃ­odo especificado", nomeEntidade);
             resultado.adicionarObservacao("Nenhum log de extraÃ§Ã£o encontrado para o perÃ­odo");
         }
        
        try {
            // Verificar se a tabela existe (NÃƒO criar - schema deve ser gerenciado via scripts SQL)
            if (!verificarExistenciaTabela(conexao, nomeTabela)) {
                final String erro = String.format(
                    "Tabela '%s' nÃ£o encontrada. Execute os scripts SQL da pasta 'database/' antes de rodar a aplicaÃ§Ã£o. " +
                    "Veja database/README.md para instruÃ§Ãµes.",
                    nomeEntidade
                );
                logger.error("âŒ {}", erro);
                resultado.setErro(erro);
                resultado.setStatus(StatusValidacao.ERRO);
                return resultado;
            }
            
            // Validar se a coluna data_extracao existe
            if (!validarColunaExiste(conexao, nomeTabela, "data_extracao")) {
                final String erro = "Coluna 'data_extracao' nÃ£o encontrada na tabela: " + nomeEntidade;
                logger.error("âŒ {}", erro);
                resultado.setErro(erro);
                resultado.setStatus(StatusValidacao.ERRO);
                return resultado;
            }
            
            // âœ… CORREÃ‡ÃƒO: Comparar dados do banco com dados do log_extracoes
            if (logExtracao != null && logExtracao.getStatusFinal() == StatusExtracao.COMPLETO) {
                // Comparar: usar registros_extraidos do log como "esperado" e contar no banco
                final int registrosEsperados = logExtracao.getRegistrosExtraidos();
                
                // Contar registros das Ãºltimas 24 horas (janela mais ampla e confiÃ¡vel)
                final Instant agora = Instant.now();
                final Instant inicio24h = agora.minusSeconds(24 * 60 * 60);
                final long registros24h = contarRegistrosPorDataExtracao(conexao, nomeTabela, inicio24h, agora, resultado);
                
                // Usar registros das Ãºltimas 24h como base de comparaÃ§Ã£o
                // Isso Ã© mais confiÃ¡vel que usar a janela exata do log, pois data_extracao
                // pode ter timestamps diferentes do timestamp_inicio/fim do log
                resultado.setTotalRegistros(registros24h);
                resultado.setRegistrosUltimas24h(registros24h);
                resultado.setRegistrosEsperadosApi(registrosEsperados);
                resultado.setDiferencaRegistros((int) (registros24h - registrosEsperados));
                
                if (registrosEsperados > 0) {
                    resultado.setPercentualCompletude((registros24h * 100.0) / registrosEsperados);
                }
                
                resultado.setColunaUtilizada("log_extracoes (comparaÃ§Ã£o banco vs log - Ãºltimas 24h)");
                
                logger.info("âœ… Comparando banco vs log para {}: {} registros no banco (24h), {} esperados do log", 
                    nomeEntidade, registros24h, registrosEsperados);
                
            } else {
                // Se nÃ£o tem log ou foi incompleto, fazer contagem tradicional no banco
                final long totalRegistros = contarRegistrosPorDataExtracao(conexao, nomeTabela, dataInicio, dataFim, resultado);
                resultado.setTotalRegistros(totalRegistros);
                
                // Contar registros das Ãºltimas 24 horas
                final Instant agora = Instant.now();
                final Instant inicio24h = agora.minusSeconds(24 * 60 * 60);
                final long registros24h = contarRegistrosPorDataExtracao(conexao, nomeTabela, inicio24h, agora, null);
                resultado.setRegistrosUltimas24h(registros24h);
                
                logger.debug("Contagem do banco para {} (tabela: {}): {} registros", nomeEntidade, nomeTabela, totalRegistros);
            }
            
            // Verificar registros com dados nulos crÃ­ticos
            final long registrosComNulos = contarRegistrosComNulos(conexao, nomeTabela);
            resultado.setRegistrosComNulos(registrosComNulos);
            
            // Verificar Ãºltimo registro extraÃ­do
            final Instant ultimaExtracao = obterDataUltimaExtracao(conexao, nomeTabela);
            resultado.setUltimaExtracao(ultimaExtracao);
            
            // Se retornou 0 registros, investigar causa raiz
            if (resultado.getTotalRegistros() == 0) {
                investigarCausaRaizZeroRegistros(conexao, nomeTabela, resultado);
            }
            
            // Determinar status da validaÃ§Ã£o
            determinarStatusValidacao(resultado, logExtracao);
            
            logger.info("âœ“ {}: {} registros, coluna: {}", 
                nomeEntidade, resultado.getTotalRegistros(), resultado.getColunaUtilizada());
            
        } catch (final SQLException e) {
            logger.error("âŒ Erro SQL ao validar entidade {}: {}", nomeEntidade, e.getMessage(), e);
            resultado.setErro("Erro SQL: " + e.getMessage());
            resultado.setStatus(StatusValidacao.ERRO);
        } catch (final Exception e) {
            logger.error("âŒ Erro inesperado ao validar entidade {}: {}", nomeEntidade, e.getMessage(), e);
            resultado.setErro("Erro inesperado: " + e.getMessage());
            resultado.setStatus(StatusValidacao.ERRO);
        }
        
        return resultado;
    }
    
    /**
     * Verifica se existem dados recentes (Ãºltimas 24 horas) em todas as entidades.
     * 
     * @param conexao ConexÃ£o com o banco de dados
     * @param dataInicio Data de inÃ­cio do perÃ­odo
     * @param dataFim Data de fim do perÃ­odo
     * @return true se existem dados recentes, false caso contrÃ¡rio
     */
    public boolean verificarExistenciaDadosRecentes(final Connection conexao, final Instant dataInicio, final Instant dataFim) {
        try {
            final List<String> entidades = List.of(
                ConstantesEntidades.COTACOES, ConstantesEntidades.COLETAS, ConstantesEntidades.CONTAS_A_PAGAR, ConstantesEntidades.FATURAS_POR_CLIENTE,
                ConstantesEntidades.FRETES, ConstantesEntidades.FATURAS_GRAPHQL, ConstantesEntidades.MANIFESTOS, ConstantesEntidades.LOCALIZACAO_CARGAS
            );
            
            for (final String entidade : entidades) {
                if (verificarExistenciaTabela(conexao, entidade)) {
                    final long registros = contarRegistrosPorDataExtracao(conexao, entidade, dataInicio, dataFim, null);
                    if (registros > 0) {
                        return true; // Pelo menos uma entidade tem dados recentes
                    }
                }
            }
            
            return false; // Nenhuma entidade tem dados recentes
            
        } catch (final SQLException e) {
            logger.error("Erro ao verificar existÃªncia de dados recentes: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Verifica se todas as tabelas necessÃ¡rias existem.
     * 
     * âš ï¸ IMPORTANTE: Em produÃ§Ã£o, as tabelas devem ser criadas via scripts SQL versionados (pasta database/).
     * Este mÃ©todo apenas verifica a existÃªncia, nÃ£o cria tabelas.
     * 
     * @param conexao ConexÃ£o com o banco de dados
     * @throws SQLException Se alguma tabela nÃ£o existir
     */
    public void verificarTodasTabelasExistem(final Connection conexao) throws SQLException {
        final List<String> entidades = List.of(
            ConstantesEntidades.COTACOES, ConstantesEntidades.COLETAS, 
            ConstantesEntidades.CONTAS_A_PAGAR, ConstantesEntidades.FATURAS_POR_CLIENTE,
            ConstantesEntidades.FRETES, ConstantesEntidades.FATURAS_GRAPHQL, 
            ConstantesEntidades.MANIFESTOS, ConstantesEntidades.LOCALIZACAO_CARGAS
        );
        
        logger.info("ðŸ” Verificando se todas as tabelas existem...");
        final List<String> tabelasFaltando = new ArrayList<>();
        
        for (final String entidade : entidades) {
            final String nomeTabela = mapearNomeEntidadeParaTabela(entidade);
            if (!verificarExistenciaTabela(conexao, nomeTabela)) {
                tabelasFaltando.add(entidade);
                logger.error("âŒ Tabela '{}' nÃ£o encontrada para entidade '{}'", nomeTabela, entidade);
            } else {
                logger.debug("âœ… Tabela '{}' existe", nomeTabela);
            }
        }
        
        if (!tabelasFaltando.isEmpty()) {
            final String mensagem = String.format(
                "As seguintes tabelas nÃ£o existem: %s. Execute os scripts SQL da pasta 'database/' antes de rodar a aplicaÃ§Ã£o. " +
                "Veja database/README.md para instruÃ§Ãµes.",
                String.join(", ", tabelasFaltando)
            );
            logger.error("âŒ {}", mensagem);
            throw new SQLException(mensagem);
        }
        
        logger.info("âœ… Todas as tabelas verificadas e existem no banco de dados");
    }
    
    /**
     * âš ï¸ DEPRECATED: Use verificarTodasTabelasExistem() em vez deste mÃ©todo.
     * 
     * @deprecated Em produÃ§Ã£o, as tabelas devem ser criadas via scripts SQL versionados (pasta database/).
     */
    @Deprecated
    public void criarTodasTabelasSeNaoExistirem(final Connection conexao) {
        try {
            verificarTodasTabelasExistem(conexao);
        } catch (final SQLException e) {
            logger.error("âŒ Erro ao verificar tabelas: {}", e.getMessage());
            // NÃ£o lanÃ§ar exceÃ§Ã£o para manter compatibilidade com cÃ³digo legado
        }
    }
    
    /**
     * Mapeia o nome da entidade para o nome da tabela no banco.
     */
    private String mapearNomeEntidadeParaTabela(final String nomeEntidade) {
        // Os nomes das entidades jÃ¡ correspondem aos nomes das tabelas
        return nomeEntidade;
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
     * Valida se uma coluna existe em uma tabela especÃ­fica usando cache para performance.
     * 
     * @param conexao ConexÃ£o com o banco de dados
     * @param nomeTabela Nome da tabela
     * @param nomeColuna Nome da coluna
     * @return true se a coluna existe, false caso contrÃ¡rio
     */
    private boolean validarColunaExiste(final Connection conexao, final String nomeTabela, final String nomeColuna) throws SQLException {
        final String chaveCache = nomeTabela + "." + nomeColuna;
        
        // Verifica se jÃ¡ temos o resultado no cache
        if (cacheValidacaoColunas.containsKey(chaveCache)) {
            final boolean existe = cacheValidacaoColunas.get(chaveCache);
            logger.debug("Cache hit para coluna {}.{}: {}", nomeTabela, nomeColuna, existe);
            return existe;
        }
        
        // Consulta INFORMATION_SCHEMA para verificar se a coluna existe
        final String sql = """
            SELECT COUNT(*)
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_NAME = ? AND COLUMN_NAME = ? AND TABLE_SCHEMA = 'dbo'
            """;
        
        logger.debug("Validando existÃªncia da coluna {}.{}", nomeTabela, nomeColuna);
        
        try (final PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setString(1, nomeTabela);
            stmt.setString(2, nomeColuna);
            try (final ResultSet rs = stmt.executeQuery()) {
                final boolean existe = rs.next() && rs.getInt(1) > 0;
                
                // Armazena no cache para futuras consultas
                cacheValidacaoColunas.put(chaveCache, existe);
                
                logger.debug("Coluna {}.{} existe: {}", nomeTabela, nomeColuna, existe);
                return existe;
            }
        }
    }
    
    /**
     * Conta registros por data de extraÃ§Ã£o (mÃ©todo auxiliar para casos sem log).
     * Usa CAST para garantir compatibilidade de timezone e >= < para evitar duplicatas.
     * 
     * âš ï¸ ESPECIAL: Para CONTAS_A_PAGAR, usa issue_date ao invÃ©s de data_extracao,
     * pois a API busca por issue_date nas Ãºltimas 24h.
     */
    private long contarRegistrosPorDataExtracao(final Connection conexao, final String nomeEntidade, 
                                               final Instant dataInicio, final Instant dataFim,
                                               final ResultadoValidacaoEntidade resultado) throws SQLException {
        final String nomeTabela = mapearNomeTabela(nomeEntidade);
        final String sql;
        
        // âš ï¸ CONTAS_A_PAGAR usa issue_date ao invÃ©s de data_extracao (mesma lÃ³gica da API)
        if (ConstantesEntidades.CONTAS_A_PAGAR.equals(nomeEntidade)) {
            // API busca por issue_date nas Ãºltimas 24h (desde ontem atÃ© hoje)
            // Usar CAST para garantir comparaÃ§Ã£o apenas por data (sem hora)
            sql = String.format("""
                SELECT COUNT(*)
                FROM %s
                WHERE issue_date >= CAST(DATEADD(day, -1, GETDATE()) AS DATE) 
                  AND issue_date <= CAST(GETDATE() AS DATE)
                """, nomeTabela);
            
            logger.debug("Query executada (CONTAS_A_PAGAR usando issue_date): {}", sql);
            
            if (resultado != null) {
                resultado.setColunaUtilizada("issue_date (contagem banco - Ãºltimas 24h)");
                resultado.setQueryExecutada(sql);
            }
            
            try (final PreparedStatement stmt = conexao.prepareStatement(sql);
                 final ResultSet rs = stmt.executeQuery()) {
                final long count = rs.next() ? rs.getLong(1) : 0;
                logger.debug("Resultado: {} registros encontrados", count);
                return count;
            }
        } else {
            // Para outras entidades, usar data_extracao normalmente
            sql = String.format("""
                SELECT COUNT(*)
                FROM %s
                WHERE data_extracao >= CAST(? AS DATETIME2) AND data_extracao < CAST(? AS DATETIME2)
                """, nomeTabela);
            
            logger.debug("Query executada: {}", sql);
            logger.debug("ParÃ¢metros: dataInicio={}, dataFim={}", dataInicio, dataFim);
            
            if (resultado != null) {
                resultado.setColunaUtilizada("data_extracao (contagem banco)");
                resultado.setQueryExecutada(sql);
            }
            
            try (final PreparedStatement stmt = conexao.prepareStatement(sql)) {
                // Converter Instant para Timestamp para compatibilidade com JDBC
                stmt.setTimestamp(1, Timestamp.from(dataInicio));
                stmt.setTimestamp(2, Timestamp.from(dataFim));
                try (final ResultSet rs = stmt.executeQuery()) {
                    final long count = rs.next() ? rs.getLong(1) : 0;
                    logger.debug("Resultado: {} registros encontrados", count);
                    return count;
                }
            }
        }
    }
    
    /**
     * Investiga a causa raiz quando uma entidade retorna 0 registros.
     */
    private void investigarCausaRaizZeroRegistros(final Connection conexao, final String nomeEntidade, 
                                                 final ResultadoValidacaoEntidade resultado) throws SQLException {
        // Verificar se a tabela tem registros em geral
        final String sqlTotal = String.format("SELECT COUNT(*) FROM %s", nomeEntidade);
        
        try (final PreparedStatement stmt = conexao.prepareStatement(sqlTotal);
             final ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                final long totalGeral = rs.getLong(1);
                if (totalGeral == 0) {
                    resultado.adicionarObservacao("Tabela estÃ¡ vazia");
                    logger.warn("âš ï¸ Tabela {} estÃ¡ completamente vazia", nomeEntidade);
                } else {
                    resultado.adicionarObservacao(String.format("Tabela tem %d registros mas nenhum no perÃ­odo especificado", totalGeral));
                    logger.warn("âš ï¸ Tabela {} tem {} registros mas nenhum no perÃ­odo auditado", nomeEntidade, totalGeral);
                }
            }
        }
    }
    
    /**
     * Conta registros com campos crÃ­ticos nulos.
     */
    private long contarRegistrosComNulos(final Connection conexao, final String nomeEntidade) throws SQLException {
        // Verificar campos crÃ­ticos especÃ­ficos por entidade
        final Map<String, String> camposCriticos = Map.of(
            ConstantesEntidades.COTACOES, "sequence_code IS NULL OR total_value IS NULL",
            ConstantesEntidades.COLETAS, "id IS NULL",
            ConstantesEntidades.CONTAS_A_PAGAR, "sequence_code IS NULL OR document_number IS NULL",
            ConstantesEntidades.FATURAS_POR_CLIENTE, "unique_id IS NULL OR numero_fatura IS NULL",
            ConstantesEntidades.FRETES, "id IS NULL",
            ConstantesEntidades.MANIFESTOS, "sequence_code IS NULL",
            ConstantesEntidades.LOCALIZACAO_CARGAS, "sequence_number IS NULL"
        );
        
        final String condicaoNulos = camposCriticos.getOrDefault(nomeEntidade, "id IS NULL");
        final String sql = String.format("SELECT COUNT(*) FROM %s WHERE %s", nomeEntidade, condicaoNulos);
        
        try (final PreparedStatement stmt = conexao.prepareStatement(sql);
             final ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0;
        }
    }
    
    /**
     * ObtÃ©m a data da Ãºltima extraÃ§Ã£o para uma entidade especÃ­fica.
     */
    private Instant obterDataUltimaExtracao(final Connection conexao, final String nomeEntidade) throws SQLException {
        final String sql = String.format("SELECT MAX(data_extracao) FROM %s", nomeEntidade);
        
        logger.debug("Obtendo Ãºltima extraÃ§Ã£o para {}: {}", nomeEntidade, sql);
        
        try (final PreparedStatement stmt = conexao.prepareStatement(sql);
             final ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                final Timestamp timestamp = rs.getTimestamp(1);
                if (timestamp != null) {
                    final Instant dataUltimaExtracao = timestamp.toInstant();
                    logger.debug("Ãšltima extraÃ§Ã£o para {}: {}", nomeEntidade, dataUltimaExtracao);
                    return dataUltimaExtracao;
                }
            }
            logger.debug("Nenhuma extraÃ§Ã£o encontrada para {}", nomeEntidade);
            return null;
        }
    }
    
    /**
     * Determina o status de validaÃ§Ã£o baseado nos dados coletados e no log de extraÃ§Ãµes.
     * 
     * VERSÃƒO CORRIGIDA: Agora confia no log_extracoes quando status = COMPLETO.
     * NÃ£o valida integridade banco vs log para evitar falsos-positivos.
     * 
     * @param resultado Resultado da validaÃ§Ã£o a ser analisado
     * @param logExtracao Log da extraÃ§Ã£o (pode ser null)
     */
    private void determinarStatusValidacao(final ResultadoValidacaoEntidade resultado, final LogExtracaoEntity logExtracao) {
        if (resultado.getErro() != null) {
            resultado.setStatus(StatusValidacao.ERRO);
            return;
        }
        
        // âœ… CORREÃ‡ÃƒO 1: Retornar ERRO se nÃ£o hÃ¡ log
        if (logExtracao == null) {
            logger.error("âŒ Nenhum log de extraÃ§Ã£o encontrado para {}", resultado.getNomeEntidade());
            resultado.setStatus(StatusValidacao.ERRO);
            resultado.setErro("Sem registro de extraÃ§Ã£o. Verifique se o Runner estÃ¡ executando.");
            resultado.adicionarObservacao("Nenhum log de extraÃ§Ã£o encontrado");
            return;
        }
        
        // âœ… CORREÃ‡ÃƒO 2: Retornar ERRO se foi incompleto por erro de API
        if (logExtracao.getStatusFinal() == StatusExtracao.ERRO_API) {
            resultado.setStatus(StatusValidacao.ERRO);
            resultado.setErro("ExtraÃ§Ã£o falhou: " + logExtracao.getMensagem());
            resultado.adicionarObservacao("ExtraÃ§Ã£o falhou: " + logExtracao.getMensagem());
            return;
        }
        
        // âœ… CORREÃ‡ÃƒO 3: Retornar ALERTA se foi incompleto por limite
        if (logExtracao.getStatusFinal() == StatusExtracao.INCOMPLETO_LIMITE) {
            resultado.setStatus(StatusValidacao.ALERTA);
            resultado.adicionarObservacao("ExtraÃ§Ã£o interrompida por limite: " + logExtracao.getMensagem());
            
            // NÃ£o aplicar validaÃ§Ãµes rigorosas se a extraÃ§Ã£o foi interrompida
            logger.info("ðŸ”„ ValidaÃ§Ã£o ajustada para extraÃ§Ã£o interrompida de {}", resultado.getNomeEntidade());
            return;
        }

        if (logExtracao.getStatusFinal() == StatusExtracao.INCOMPLETO_DADOS
            || logExtracao.getStatusFinal() == StatusExtracao.INCOMPLETO_DB
            || logExtracao.getStatusFinal() == StatusExtracao.INCOMPLETO) {
            resultado.setStatus(StatusValidacao.ERRO);
            resultado.setErro("ExtraÃ§Ã£o incompleta por divergÃªncia de qualidade/persistÃªncia: " + logExtracao.getMensagem());
            resultado.adicionarObservacao("ExtraÃ§Ã£o incompleta por dados/persistÃªncia: " + logExtracao.getMensagem());
            return;
        }
        
        logger.info("âœ… ExtraÃ§Ã£o de {} foi completada com sucesso", resultado.getNomeEntidade());
        
        // âœ… CORREÃ‡ÃƒO 4: NÃƒO validar integridade banco vs log quando COMPLETO
        // Motivo: O banco pode ter dados de mÃºltiplas extraÃ§Ãµes (acumulados)
        // O log_extracoes Ã© a fonte confiÃ¡vel para a extraÃ§Ã£o atual
        
        // Apenas verificar se a extraÃ§Ã£o atual trouxe algum dado
        if (logExtracao.getRegistrosExtraidos() == 0) {
            resultado.setStatus(StatusValidacao.ALERTA);
            resultado.adicionarObservacao("Nenhum registro foi extraÃ­do na Ãºltima execuÃ§Ã£o");
            return;
        }
        
        // Verificar se hÃ¡ muitos registros com nulos (baseado no total do log)
        if (resultado.getRegistrosComNulos() > 0) {
            final double percentualNulos = logExtracao.getRegistrosExtraidos() > 0 ? 
                (double) resultado.getRegistrosComNulos() / logExtracao.getRegistrosExtraidos() * 100 : 0;
            
            if (percentualNulos > 10.0) {
                resultado.setStatus(StatusValidacao.ALERTA);
                resultado.adicionarObservacao(String.format("%.1f%% dos registros possuem campos crÃ­ticos nulos", percentualNulos));
                return;
            }
        }
        
        // Verificar se a Ãºltima extraÃ§Ã£o Ã© muito antiga (mais de 25 horas)
        if (resultado.getUltimaExtracao() != null) {
            final long horasDesdeUltimaExtracao = java.time.Duration.between(resultado.getUltimaExtracao(), Instant.now()).toHours();
            if (horasDesdeUltimaExtracao > 25) {
                resultado.setStatus(StatusValidacao.ALERTA);
                resultado.adicionarObservacao(String.format("Ãšltima extraÃ§Ã£o hÃ¡ %d horas", horasDesdeUltimaExtracao));
                return;
            }
        }
        
        // âœ… Se chegou atÃ© aqui, extraÃ§Ã£o foi completa e validaÃ§Ã£o passou
        resultado.setStatus(StatusValidacao.OK);
        resultado.adicionarObservacao(String.format("ExtraÃ§Ã£o completa: %d registros salvos com sucesso", 
            logExtracao.getRegistrosExtraidos()));
    }

    private String mapearNomeTabela(final String nomeEntidade) {
        return switch (nomeEntidade) {
            case "faturas_a_pagar_data_export" -> ConstantesEntidades.CONTAS_A_PAGAR;
            default -> nomeEntidade;
        };
    }
}
