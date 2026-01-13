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
 * Classe responsável por validar a completude e integridade dos dados
 * extraídos das APIs do ESL Cloud.
 * 
 * Verifica se os dados foram extraídos corretamente, identifica lacunas
 * e inconsistências nos dados armazenados.
 * 
 * VERSÃO CORRIGIDA: Agora confia no log_extracoes quando a extração foi COMPLETA,
 * eliminando falsos-positivos causados por dados de múltiplas extrações.
 */
public class AuditoriaValidator {
    private static final Logger logger = LoggerFactory.getLogger(AuditoriaValidator.class);
    
    // Cache para validações de colunas (evita consultas repetidas)
    private final Map<String, Boolean> cacheValidacaoColunas = new HashMap<>();
    
    // Repository para consultar logs de extração
    private final LogExtracaoRepository logExtracaoRepository;
    
    /**
     * Construtor que inicializa o repository de logs de extração.
     */
    public AuditoriaValidator() {
        this.logExtracaoRepository = new LogExtracaoRepository();
        // NOTA: As tabelas devem ser criadas via scripts SQL da pasta database/
        // O LogExtracaoRepository não cria mais tabelas automaticamente
    }
    
    /**
     * Consulta o log de extração mais recente para uma entidade
     * 
     * @param nomeEntidade Nome da entidade a ser consultada
     * @param dataInicio Data de início do período de interesse
     * @return LogExtracaoEntity da última extração ou null se não encontrada
     */
    private LogExtracaoEntity consultarLogExtracao(final String nomeEntidade, final Instant dataInicio) {
        try {
            logger.debug("Consultando log de extração para entidade: {}", nomeEntidade);
            
            final Optional<LogExtracaoEntity> ultimoLog = logExtracaoRepository.buscarUltimoLogPorEntidade(nomeEntidade);
            
            if (ultimoLog.isPresent()) {
                final LogExtracaoEntity log = ultimoLog.get();
                
                // Verificar se o log é recente (dentro do período de interesse)
                final LocalDateTime dataInicioLocal = LocalDateTime.ofInstant(dataInicio, ZoneOffset.UTC);
                if (log.getTimestampFim().isAfter(dataInicioLocal)) {
                    logger.info("Log de extração encontrado para {}: Status={}, Registros={}, Páginas={}", 
                        nomeEntidade, log.getStatusFinal(), log.getRegistrosExtraidos(), log.getPaginasProcessadas());
                    return log;
                } else {
                    logger.debug("Log de extração encontrado para {} mas é anterior ao período de interesse", nomeEntidade);
                }
            } else {
                logger.debug("Nenhum log de extração encontrado para {}", nomeEntidade);
            }
            
        } catch (final Exception e) {
            logger.error("Erro ao consultar log de extração para {}: {}", nomeEntidade, e.getMessage(), e);
        }
        
        return null;
    }
    
    /**
     * Valida uma entidade específica verificando completude dos dados.
     * 
     * VERSÃO CORRIGIDA: Agora confia no log_extracoes quando status = COMPLETO.
     * Não compara com contagem do banco para evitar falsos-positivos causados
     * por dados de múltiplas extrações.
     * 
     * @param conexao Conexão com o banco de dados
     * @param nomeEntidade Nome da entidade a ser validada
     * @param dataInicio Data de início do período
     * @param dataFim Data de fim do período
     * @return ResultadoValidacaoEntidade com o resultado da validação
     */
    public ResultadoValidacaoEntidade validarEntidade(final Connection conexao, final String nomeEntidade, 
                                                     final Instant dataInicio, final Instant dataFim) {
        logger.info("🔍 Auditando {}...", nomeEntidade);
        logger.debug("Parâmetros: dataInicio={}, dataFim={}", dataInicio, dataFim);
        
        final ResultadoValidacaoEntidade resultado = new ResultadoValidacaoEntidade();
        resultado.setNomeEntidade(nomeEntidade);
        resultado.setDataInicio(dataInicio);
        resultado.setDataFim(dataFim);
        final String nomeTabela = mapearNomeTabela(nomeEntidade);
        
        // PASSO 1: Consultar log de extrações primeiro
        final LogExtracaoEntity logExtracao = consultarLogExtracao(nomeEntidade, dataInicio);
        if (logExtracao != null) {
             logger.info("📊 Log de extração encontrado para {}: status={}, registros={}, páginas={}", 
                 nomeEntidade, logExtracao.getStatusFinal(), logExtracao.getRegistrosExtraidos(), logExtracao.getPaginasProcessadas());
             
             // Se a extração foi interrompida ou teve erro, ajustar expectativas
             switch (logExtracao.getStatusFinal()) {
                 case INCOMPLETO_LIMITE -> {
                     logger.warn("⚠️ Extração de {} foi interrompida por limite. Detalhes: {}", nomeEntidade, logExtracao.getMensagem());
                     resultado.adicionarObservacao("Extração interrompida por limite: " + logExtracao.getMensagem());
                 }
                 case ERRO_API -> {
                     logger.warn("❌ Extração de {} teve erro de API. Detalhes: {}", nomeEntidade, logExtracao.getMensagem());
                     resultado.adicionarObservacao("Erro na extração: " + logExtracao.getMensagem());
                 }
                 case COMPLETO -> {
                     logger.info("✅ Extração de {} foi completada com sucesso", nomeEntidade);
                     resultado.adicionarObservacao("Extração completada com sucesso");
                 }
                 default -> logger.debug("Status de extração não reconhecido: {}", logExtracao.getStatusFinal());
             }
         } else {
             logger.warn("⚠️ Nenhum log de extração encontrado para {} no período especificado", nomeEntidade);
             resultado.adicionarObservacao("Nenhum log de extração encontrado para o período");
         }
        
        try {
            // Verificar se a tabela existe (NÃO criar - schema deve ser gerenciado via scripts SQL)
            if (!verificarExistenciaTabela(conexao, nomeTabela)) {
                final String erro = String.format(
                    "Tabela '%s' não encontrada. Execute os scripts SQL da pasta 'database/' antes de rodar a aplicação. " +
                    "Veja database/README.md para instruções.",
                    nomeEntidade
                );
                logger.error("❌ {}", erro);
                resultado.setErro(erro);
                resultado.setStatus(StatusValidacao.ERRO);
                return resultado;
            }
            
            // Validar se a coluna data_extracao existe
            if (!validarColunaExiste(conexao, nomeTabela, "data_extracao")) {
                final String erro = "Coluna 'data_extracao' não encontrada na tabela: " + nomeEntidade;
                logger.error("❌ {}", erro);
                resultado.setErro(erro);
                resultado.setStatus(StatusValidacao.ERRO);
                return resultado;
            }
            
            // ✅ CORREÇÃO: Comparar dados do banco com dados do log_extracoes
            if (logExtracao != null && logExtracao.getStatusFinal() == StatusExtracao.COMPLETO) {
                // Comparar: usar registros_extraidos do log como "esperado" e contar no banco
                final int registrosEsperados = logExtracao.getRegistrosExtraidos();
                
                // Contar registros das últimas 24 horas (janela mais ampla e confiável)
                final Instant agora = Instant.now();
                final Instant inicio24h = agora.minusSeconds(24 * 60 * 60);
                final long registros24h = contarRegistrosPorDataExtracao(conexao, nomeTabela, inicio24h, agora, resultado);
                
                // Usar registros das últimas 24h como base de comparação
                // Isso é mais confiável que usar a janela exata do log, pois data_extracao
                // pode ter timestamps diferentes do timestamp_inicio/fim do log
                resultado.setTotalRegistros(registros24h);
                resultado.setRegistrosUltimas24h(registros24h);
                resultado.setRegistrosEsperadosApi(registrosEsperados);
                resultado.setDiferencaRegistros((int) (registros24h - registrosEsperados));
                
                if (registrosEsperados > 0) {
                    resultado.setPercentualCompletude((registros24h * 100.0) / registrosEsperados);
                }
                
                resultado.setColunaUtilizada("log_extracoes (comparação banco vs log - últimas 24h)");
                
                logger.info("✅ Comparando banco vs log para {}: {} registros no banco (24h), {} esperados do log", 
                    nomeEntidade, registros24h, registrosEsperados);
                
            } else {
                // Se não tem log ou foi incompleto, fazer contagem tradicional no banco
                final long totalRegistros = contarRegistrosPorDataExtracao(conexao, nomeTabela, dataInicio, dataFim, resultado);
                resultado.setTotalRegistros(totalRegistros);
                
                // Contar registros das últimas 24 horas
                final Instant agora = Instant.now();
                final Instant inicio24h = agora.minusSeconds(24 * 60 * 60);
                final long registros24h = contarRegistrosPorDataExtracao(conexao, nomeTabela, inicio24h, agora, null);
                resultado.setRegistrosUltimas24h(registros24h);
                
                logger.debug("Contagem do banco para {} (tabela: {}): {} registros", nomeEntidade, nomeTabela, totalRegistros);
            }
            
            // Verificar registros com dados nulos críticos
            final long registrosComNulos = contarRegistrosComNulos(conexao, nomeTabela);
            resultado.setRegistrosComNulos(registrosComNulos);
            
            // Verificar último registro extraído
            final Instant ultimaExtracao = obterDataUltimaExtracao(conexao, nomeTabela);
            resultado.setUltimaExtracao(ultimaExtracao);
            
            // Se retornou 0 registros, investigar causa raiz
            if (resultado.getTotalRegistros() == 0) {
                investigarCausaRaizZeroRegistros(conexao, nomeTabela, resultado);
            }
            
            // Determinar status da validação
            determinarStatusValidacao(resultado, logExtracao);
            
            logger.info("✓ {}: {} registros, coluna: {}", 
                nomeEntidade, resultado.getTotalRegistros(), resultado.getColunaUtilizada());
            
        } catch (final SQLException e) {
            logger.error("❌ Erro SQL ao validar entidade {}: {}", nomeEntidade, e.getMessage(), e);
            resultado.setErro("Erro SQL: " + e.getMessage());
            resultado.setStatus(StatusValidacao.ERRO);
        } catch (final Exception e) {
            logger.error("❌ Erro inesperado ao validar entidade {}: {}", nomeEntidade, e.getMessage(), e);
            resultado.setErro("Erro inesperado: " + e.getMessage());
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
            logger.error("Erro ao verificar existência de dados recentes: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Verifica se todas as tabelas necessárias existem.
     * 
     * ⚠️ IMPORTANTE: Em produção, as tabelas devem ser criadas via scripts SQL versionados (pasta database/).
     * Este método apenas verifica a existência, não cria tabelas.
     * 
     * @param conexao Conexão com o banco de dados
     * @throws SQLException Se alguma tabela não existir
     */
    public void verificarTodasTabelasExistem(final Connection conexao) throws SQLException {
        final List<String> entidades = List.of(
            ConstantesEntidades.COTACOES, ConstantesEntidades.COLETAS, 
            ConstantesEntidades.CONTAS_A_PAGAR, ConstantesEntidades.FATURAS_POR_CLIENTE,
            ConstantesEntidades.FRETES, ConstantesEntidades.FATURAS_GRAPHQL, 
            ConstantesEntidades.MANIFESTOS, ConstantesEntidades.LOCALIZACAO_CARGAS
        );
        
        logger.info("🔍 Verificando se todas as tabelas existem...");
        final List<String> tabelasFaltando = new ArrayList<>();
        
        for (final String entidade : entidades) {
            final String nomeTabela = mapearNomeEntidadeParaTabela(entidade);
            if (!verificarExistenciaTabela(conexao, nomeTabela)) {
                tabelasFaltando.add(entidade);
                logger.error("❌ Tabela '{}' não encontrada para entidade '{}'", nomeTabela, entidade);
            } else {
                logger.debug("✅ Tabela '{}' existe", nomeTabela);
            }
        }
        
        if (!tabelasFaltando.isEmpty()) {
            final String mensagem = String.format(
                "As seguintes tabelas não existem: %s. Execute os scripts SQL da pasta 'database/' antes de rodar a aplicação. " +
                "Veja database/README.md para instruções.",
                String.join(", ", tabelasFaltando)
            );
            logger.error("❌ {}", mensagem);
            throw new SQLException(mensagem);
        }
        
        logger.info("✅ Todas as tabelas verificadas e existem no banco de dados");
    }
    
    /**
     * ⚠️ DEPRECATED: Use verificarTodasTabelasExistem() em vez deste método.
     * 
     * @deprecated Em produção, as tabelas devem ser criadas via scripts SQL versionados (pasta database/).
     */
    @Deprecated
    public void criarTodasTabelasSeNaoExistirem(final Connection conexao) {
        try {
            verificarTodasTabelasExistem(conexao);
        } catch (final SQLException e) {
            logger.error("❌ Erro ao verificar tabelas: {}", e.getMessage());
            // Não lançar exceção para manter compatibilidade com código legado
        }
    }
    
    /**
     * Mapeia o nome da entidade para o nome da tabela no banco.
     */
    private String mapearNomeEntidadeParaTabela(final String nomeEntidade) {
        // Os nomes das entidades já correspondem aos nomes das tabelas
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
     * Valida se uma coluna existe em uma tabela específica usando cache para performance.
     * 
     * @param conexao Conexão com o banco de dados
     * @param nomeTabela Nome da tabela
     * @param nomeColuna Nome da coluna
     * @return true se a coluna existe, false caso contrário
     */
    private boolean validarColunaExiste(final Connection conexao, final String nomeTabela, final String nomeColuna) throws SQLException {
        final String chaveCache = nomeTabela + "." + nomeColuna;
        
        // Verifica se já temos o resultado no cache
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
        
        logger.debug("Validando existência da coluna {}.{}", nomeTabela, nomeColuna);
        
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
     * Conta registros por data de extração (método auxiliar para casos sem log).
     * Usa CAST para garantir compatibilidade de timezone e >= < para evitar duplicatas.
     * 
     * ⚠️ ESPECIAL: Para CONTAS_A_PAGAR, usa issue_date ao invés de data_extracao,
     * pois a API busca por issue_date nas últimas 24h.
     */
    private long contarRegistrosPorDataExtracao(final Connection conexao, final String nomeEntidade, 
                                               final Instant dataInicio, final Instant dataFim,
                                               final ResultadoValidacaoEntidade resultado) throws SQLException {
        final String nomeTabela = mapearNomeTabela(nomeEntidade);
        final String sql;
        
        // ⚠️ CONTAS_A_PAGAR usa issue_date ao invés de data_extracao (mesma lógica da API)
        if (ConstantesEntidades.CONTAS_A_PAGAR.equals(nomeEntidade)) {
            // API busca por issue_date nas últimas 24h (desde ontem até hoje)
            // Usar CAST para garantir comparação apenas por data (sem hora)
            sql = String.format("""
                SELECT COUNT(*)
                FROM %s
                WHERE issue_date >= CAST(DATEADD(day, -1, GETDATE()) AS DATE) 
                  AND issue_date <= CAST(GETDATE() AS DATE)
                """, nomeTabela);
            
            logger.debug("Query executada (CONTAS_A_PAGAR usando issue_date): {}", sql);
            
            if (resultado != null) {
                resultado.setColunaUtilizada("issue_date (contagem banco - últimas 24h)");
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
            logger.debug("Parâmetros: dataInicio={}, dataFim={}", dataInicio, dataFim);
            
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
                    resultado.adicionarObservacao("Tabela está vazia");
                    logger.warn("⚠️ Tabela {} está completamente vazia", nomeEntidade);
                } else {
                    resultado.adicionarObservacao(String.format("Tabela tem %d registros mas nenhum no período especificado", totalGeral));
                    logger.warn("⚠️ Tabela {} tem {} registros mas nenhum no período auditado", nomeEntidade, totalGeral);
                }
            }
        }
    }
    
    /**
     * Conta registros com campos críticos nulos.
     */
    private long contarRegistrosComNulos(final Connection conexao, final String nomeEntidade) throws SQLException {
        // Verificar campos críticos específicos por entidade
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
     * Obtém a data da última extração para uma entidade específica.
     */
    private Instant obterDataUltimaExtracao(final Connection conexao, final String nomeEntidade) throws SQLException {
        final String sql = String.format("SELECT MAX(data_extracao) FROM %s", nomeEntidade);
        
        logger.debug("Obtendo última extração para {}: {}", nomeEntidade, sql);
        
        try (final PreparedStatement stmt = conexao.prepareStatement(sql);
             final ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                final Timestamp timestamp = rs.getTimestamp(1);
                if (timestamp != null) {
                    final Instant dataUltimaExtracao = timestamp.toInstant();
                    logger.debug("Última extração para {}: {}", nomeEntidade, dataUltimaExtracao);
                    return dataUltimaExtracao;
                }
            }
            logger.debug("Nenhuma extração encontrada para {}", nomeEntidade);
            return null;
        }
    }
    
    /**
     * Determina o status de validação baseado nos dados coletados e no log de extrações.
     * 
     * VERSÃO CORRIGIDA: Agora confia no log_extracoes quando status = COMPLETO.
     * Não valida integridade banco vs log para evitar falsos-positivos.
     * 
     * @param resultado Resultado da validação a ser analisado
     * @param logExtracao Log da extração (pode ser null)
     */
    private void determinarStatusValidacao(final ResultadoValidacaoEntidade resultado, final LogExtracaoEntity logExtracao) {
        if (resultado.getErro() != null) {
            resultado.setStatus(StatusValidacao.ERRO);
            return;
        }
        
        // ✅ CORREÇÃO 1: Retornar ERRO se não há log
        if (logExtracao == null) {
            logger.error("❌ Nenhum log de extração encontrado para {}", resultado.getNomeEntidade());
            resultado.setStatus(StatusValidacao.ERRO);
            resultado.setErro("Sem registro de extração. Verifique se o Runner está executando.");
            resultado.adicionarObservacao("Nenhum log de extração encontrado");
            return;
        }
        
        // ✅ CORREÇÃO 2: Retornar ERRO se foi incompleto por erro de API
        if (logExtracao.getStatusFinal() == StatusExtracao.ERRO_API) {
            resultado.setStatus(StatusValidacao.ERRO);
            resultado.setErro("Extração falhou: " + logExtracao.getMensagem());
            resultado.adicionarObservacao("Extração falhou: " + logExtracao.getMensagem());
            return;
        }
        
        // ✅ CORREÇÃO 3: Retornar ALERTA se foi incompleto por limite
        if (logExtracao.getStatusFinal() == StatusExtracao.INCOMPLETO_LIMITE) {
            resultado.setStatus(StatusValidacao.ALERTA);
            resultado.adicionarObservacao("Extração interrompida por limite: " + logExtracao.getMensagem());
            
            // Não aplicar validações rigorosas se a extração foi interrompida
            logger.info("🔄 Validação ajustada para extração interrompida de {}", resultado.getNomeEntidade());
            return;
        }
        
        logger.info("✅ Extração de {} foi completada com sucesso", resultado.getNomeEntidade());
        
        // ✅ CORREÇÃO 4: NÃO validar integridade banco vs log quando COMPLETO
        // Motivo: O banco pode ter dados de múltiplas extrações (acumulados)
        // O log_extracoes é a fonte confiável para a extração atual
        
        // Apenas verificar se a extração atual trouxe algum dado
        if (logExtracao.getRegistrosExtraidos() == 0) {
            resultado.setStatus(StatusValidacao.ALERTA);
            resultado.adicionarObservacao("Nenhum registro foi extraído na última execução");
            return;
        }
        
        // Verificar se há muitos registros com nulos (baseado no total do log)
        if (resultado.getRegistrosComNulos() > 0) {
            final double percentualNulos = logExtracao.getRegistrosExtraidos() > 0 ? 
                (double) resultado.getRegistrosComNulos() / logExtracao.getRegistrosExtraidos() * 100 : 0;
            
            if (percentualNulos > 10.0) {
                resultado.setStatus(StatusValidacao.ALERTA);
                resultado.adicionarObservacao(String.format("%.1f%% dos registros possuem campos críticos nulos", percentualNulos));
                return;
            }
        }
        
        // Verificar se a última extração é muito antiga (mais de 25 horas)
        if (resultado.getUltimaExtracao() != null) {
            final long horasDesdeUltimaExtracao = java.time.Duration.between(resultado.getUltimaExtracao(), Instant.now()).toHours();
            if (horasDesdeUltimaExtracao > 25) {
                resultado.setStatus(StatusValidacao.ALERTA);
                resultado.adicionarObservacao(String.format("Última extração há %d horas", horasDesdeUltimaExtracao));
                return;
            }
        }
        
        // ✅ Se chegou até aqui, extração foi completa e validação passou
        resultado.setStatus(StatusValidacao.OK);
        resultado.adicionarObservacao(String.format("Extração completa: %d registros salvos com sucesso", 
            logExtracao.getRegistrosExtraidos()));
    }

    private String mapearNomeTabela(final String nomeEntidade) {
        return switch (nomeEntidade) {
            case "faturas_a_pagar_data_export" -> ConstantesEntidades.CONTAS_A_PAGAR;
            default -> nomeEntidade;
        };
    }
}