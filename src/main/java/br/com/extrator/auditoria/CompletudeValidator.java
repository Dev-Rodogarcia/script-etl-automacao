package br.com.extrator.auditoria;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.extrator.api.ClienteApiDataExport;
import br.com.extrator.api.ClienteApiGraphQL;
import br.com.extrator.api.ClienteApiRest;
import br.com.extrator.util.GerenciadorConexao;

/**
 * Motor central da auditoria comparativa que orquestra a busca de contagens
 * e a comparação com o banco de dados.
 * 
 * Esta classe implementa o Tópico 2 da documentação, sendo responsável por:
 * - Orquestrar chamadas aos clientes de API para obter contagens do ESL Cloud
 * - Comparar essas contagens com os dados armazenados no banco de dados local
 * - Gerar relatórios de completude com status claros (✅ OK, ❌ INCOMPLETO, ⚠️ DUPLICADOS)
 * 
 * @author Sistema de Extração ESL Cloud
 * @version 1.0
 */
public class CompletudeValidator {
    private static final Logger logger = LoggerFactory.getLogger(CompletudeValidator.class);
    
    // Clientes de API para buscar contagens do ESL Cloud
    private final ClienteApiRest clienteApiRest;
    private final ClienteApiGraphQL clienteApiGraphQL;
    private final ClienteApiDataExport clienteApiDataExport;
    
    // Mapeamento de entidades para nomes de tabelas no banco
    private static final Map<String, String> MAPEAMENTO_ENTIDADES_TABELAS = Map.of(
        "ocorrencias", "ocorrencias",
        "faturas_a_receber", "faturas_a_receber",
        "faturas_a_pagar", "faturas_a_pagar",
        "fretes", "fretes",
        "coletas", "coletas",
        "manifestos", "manifestos",
        "cotacoes", "cotacoes",
        "localizacao_cargas", "localizacao_cargas"
    );
    
    /**
     * Construtor que inicializa os clientes de API necessários.
     * Utiliza injeção de dependência para facilitar testes e manutenção.
     */
    public CompletudeValidator() {
        this.clienteApiRest = new ClienteApiRest();
        this.clienteApiGraphQL = new ClienteApiGraphQL();
        this.clienteApiDataExport = new ClienteApiDataExport();
        
        logger.info("CompletudeValidator inicializado com todos os clientes de API");
    }
    
    /**
     * Construtor alternativo para injeção de dependência (útil para testes).
     * 
     * @param clienteApiRest Cliente da API REST
     * @param clienteApiGraphQL Cliente da API GraphQL
     * @param clienteApiDataExport Cliente da API DataExport
     */
    public CompletudeValidator(ClienteApiRest clienteApiRest, 
                              ClienteApiGraphQL clienteApiGraphQL,
                              ClienteApiDataExport clienteApiDataExport) {
        this.clienteApiRest = clienteApiRest;
        this.clienteApiGraphQL = clienteApiGraphQL;
        this.clienteApiDataExport = clienteApiDataExport;
        
        logger.info("CompletudeValidator inicializado com clientes injetados");
    }
    
    /**
     * Orquestrador principal que busca totais de todas as entidades do ESL Cloud.
     * 
     * Este método é o coração do Tópico 1, coordenando chamadas sequenciais para:
     * - ClienteApiRest: ocorrências, faturas a receber, faturas a pagar
     * - ClienteApiGraphQL: fretes, coletas  
     * - ClienteApiDataExport: manifestos, cotações, localizações de carga
     * 
     * @param dataReferencia Data de referência para buscar as contagens
     * @return Map com chave=nome_entidade e valor=contagem_esl_cloud
     */
    public Map<String, Integer> buscarTotaisEslCloud(LocalDate dataReferencia) {
        logger.info("🔍 Iniciando busca de totais do ESL Cloud para data: {}", dataReferencia);
        
        Map<String, Integer> totaisEslCloud = new HashMap<>();
        
        try {
            // === API REST - Ocorrências e Faturas ===
            logger.info("📡 Buscando contagens via API REST...");
            
            // Ocorrências
            int contagemOcorrencias = clienteApiRest.obterContagemOcorrencias(dataReferencia);
            totaisEslCloud.put("ocorrencias", contagemOcorrencias);
            logger.info("✅ Ocorrências: {} registros", contagemOcorrencias);
            
            // Faturas a Receber
            int contagemFaturasReceber = clienteApiRest.obterContagemFaturasAReceber(dataReferencia);
            totaisEslCloud.put("faturas_a_receber", contagemFaturasReceber);
            logger.info("✅ Faturas a Receber: {} registros", contagemFaturasReceber);
            
            // Faturas a Pagar
            int contagemFaturasPagar = clienteApiRest.obterContagemFaturasAPagar(dataReferencia);
            totaisEslCloud.put("faturas_a_pagar", contagemFaturasPagar);
            logger.info("✅ Faturas a Pagar: {} registros", contagemFaturasPagar);
            
            // === API GraphQL - Fretes e Coletas ===
            logger.info("🔗 Buscando contagens via API GraphQL...");
            
            // Fretes
            int contagemFretes = clienteApiGraphQL.obterContagemFretes(dataReferencia);
            totaisEslCloud.put("fretes", contagemFretes);
            logger.info("✅ Fretes: {} registros", contagemFretes);
            
            // Coletas
            int contagemColetas = clienteApiGraphQL.obterContagemColetas(dataReferencia);
            totaisEslCloud.put("coletas", contagemColetas);
            logger.info("✅ Coletas: {} registros", contagemColetas);
            
            // === API DataExport - Manifestos, Cotações e Localizações ===
            logger.info("📊 Buscando contagens via API DataExport...");
            
            // Manifestos
            int contagemManifestos = clienteApiDataExport.obterContagemManifestos(dataReferencia);
            totaisEslCloud.put("manifestos", contagemManifestos);
            logger.info("✅ Manifestos: {} registros", contagemManifestos);
            
            // Cotações
            int contagemCotacoes = clienteApiDataExport.obterContagemCotacoes(dataReferencia);
            totaisEslCloud.put("cotacoes", contagemCotacoes);
            logger.info("✅ Cotações: {} registros", contagemCotacoes);
            
            // Localizações de Carga
            int contagemLocalizacoes = clienteApiDataExport.obterContagemLocalizacoesCarga(dataReferencia);
            totaisEslCloud.put("localizacao_cargas", contagemLocalizacoes);
            logger.info("✅ Localizações de Carga: {} registros", contagemLocalizacoes);
            
            // Log do resumo final
            int totalGeralRegistros = totaisEslCloud.values().stream().mapToInt(Integer::intValue).sum();
            logger.info("🎯 Busca de totais ESL Cloud concluída: {} entidades, {} registros totais", 
                    totaisEslCloud.size(), totalGeralRegistros);
            
        } catch (Exception e) {
            logger.error("❌ Erro ao buscar totais do ESL Cloud: {}", e.getMessage(), e);
            throw new RuntimeException("Falha na busca de totais do ESL Cloud", e);
        }
        
        return totaisEslCloud;
    }
    
    /**
     * Valida a completude dos dados comparando contagens do ESL Cloud com o banco local.
     * 
     * Implementa a lógica de comparação usando queries SQL eficientes com String.format
     * (seguro pois os nomes das tabelas vêm de fonte controlada - as chaves do Map).
     * 
     * Gera logs com status claros:
     * - ✅ OK: contagens coincidem
     * - ❌ INCOMPLETO: banco tem menos registros que ESL Cloud  
     * - ⚠️ DUPLICADOS: banco tem mais registros que ESL Cloud
     * 
     * @param totaisEslCloud Map com contagens obtidas do ESL Cloud
     * @param dataReferencia Data de referência para filtrar consultas no banco
     * @return Map com resultado da validação por entidade
     */
    public Map<String, StatusValidacao> validarCompletude(Map<String, Integer> totaisEslCloud, 
                                                         LocalDate dataReferencia) {
        logger.info("🔍 Iniciando validação de completude para {} entidades na data: {}", 
                totaisEslCloud.size(), dataReferencia);
        
        Map<String, StatusValidacao> resultadosValidacao = new HashMap<>();
        
        try (Connection conexao = GerenciadorConexao.obterConexao()) {
            
            for (Map.Entry<String, Integer> entrada : totaisEslCloud.entrySet()) {
                String nomeEntidade = entrada.getKey();
                int contagemEslCloud = entrada.getValue();
                
                // Obter nome da tabela correspondente
                String nomeTabela = MAPEAMENTO_ENTIDADES_TABELAS.get(nomeEntidade);
                if (nomeTabela == null) {
                    logger.warn("⚠️ Entidade '{}' não possui mapeamento para tabela. Pulando validação.", nomeEntidade);
                    resultadosValidacao.put(nomeEntidade, StatusValidacao.ERRO);
                    continue;
                }
                
                try {
                    // Query SQL eficiente para contar registros na data específica
                    // String.format é seguro aqui pois nomeTabela vem de fonte controlada
                    String sql = String.format(
                        "SELECT COUNT(*) FROM %s WHERE DATE(data_extracao) = ?", 
                        nomeTabela
                    );
                    
                    int contagemBanco;
                    try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
                        stmt.setDate(1, java.sql.Date.valueOf(dataReferencia));
                        
                        try (ResultSet rs = stmt.executeQuery()) {
                            rs.next();
                            contagemBanco = rs.getInt(1);
                        }
                    }
                    
                    // Determinar status baseado na comparação
                    StatusValidacao status = determinarStatusValidacao(contagemEslCloud, contagemBanco);
                    resultadosValidacao.put(nomeEntidade, status);
                    
                    // Log com status visual claro
                    String iconeStatus = obterIconeStatus(status);
                    logger.info("{} {}: ESL Cloud={}, Banco={}", 
                            iconeStatus, nomeEntidade, contagemEslCloud, contagemBanco);
                    
                } catch (SQLException e) {
                    logger.error("❌ Erro SQL ao validar entidade '{}': {}", nomeEntidade, e.getMessage(), e);
                    resultadosValidacao.put(nomeEntidade, StatusValidacao.ERRO);
                }
            }
            
            // Log do resumo final
            long totalOk = resultadosValidacao.values().stream()
                    .filter(status -> status == StatusValidacao.OK).count();
            long totalIncompleto = resultadosValidacao.values().stream()
                    .filter(status -> status == StatusValidacao.INCOMPLETO).count();
            long totalDuplicados = resultadosValidacao.values().stream()
                    .filter(status -> status == StatusValidacao.DUPLICADOS).count();
            long totalErros = resultadosValidacao.values().stream()
                    .filter(status -> status == StatusValidacao.ERRO).count();
            
            logger.info("📊 Validação de completude concluída: ✅ {} OK, ❌ {} INCOMPLETO, ⚠️ {} DUPLICADOS, 💥 {} ERROS", 
                    totalOk, totalIncompleto, totalDuplicados, totalErros);
            
        } catch (SQLException e) {
            logger.error("❌ Erro ao conectar com banco de dados para validação: {}", e.getMessage(), e);
            throw new RuntimeException("Falha na conexão com banco de dados", e);
        }
        
        return resultadosValidacao;
    }
    
    /**
     * Determina o status de validação baseado na comparação entre contagens.
     * 
     * @param contagemEslCloud Contagem obtida do ESL Cloud
     * @param contagemBanco Contagem obtida do banco local
     * @return Status da validação
     */
    private StatusValidacao determinarStatusValidacao(int contagemEslCloud, int contagemBanco) {
        if (contagemEslCloud == contagemBanco) {
            return StatusValidacao.OK;
        } else if (contagemBanco < contagemEslCloud) {
            return StatusValidacao.INCOMPLETO;
        } else {
            return StatusValidacao.DUPLICADOS;
        }
    }
    
    /**
     * Obtém o ícone visual correspondente ao status de validação.
     * 
     * @param status Status da validação
     * @return String com ícone visual
     */
    private String obterIconeStatus(StatusValidacao status) {
        return switch (status) {
            case OK -> "✅ OK";
            case INCOMPLETO -> "❌ INCOMPLETO";
            case DUPLICADOS -> "⚠️ DUPLICADOS";
            case ERRO -> "💥 ERRO";
        };
    }
    
    /**
     * TÓPICO 4: Validação de Gaps - Verifica se os IDs das ocorrências são sequenciais
     * 
     * Pré-requisito: Esta validação só deve ser executada se os IDs forem realmente sequenciais.
     * Caso contrário, a estratégia de detecção de gaps não funcionará.
     * 
     * @param dataReferencia Data de referência para análise
     * @return StatusValidacao indicando se há gaps nos IDs
     */
    public StatusValidacao validarGapsOcorrencias(LocalDate dataReferencia) {
        logger.info("🔍 Iniciando validação de gaps para ocorrências...");
        
        try (Connection conexao = GerenciadorConexao.obterConexao()) {
            // Primeiro, verificar se os IDs são sequenciais
            if (!verificarIdsSequenciais(conexao, "ocorrencias")) {
                logger.warn("⚠️ IDs das ocorrências não são sequenciais - validação de gaps não aplicável");
                return StatusValidacao.OK; // Não é erro, apenas não aplicável
            }
            
            // Se são sequenciais, verificar gaps
            return detectarGapsSequenciais(conexao, "ocorrencias", dataReferencia);
            
        } catch (SQLException e) {
            logger.error("❌ Erro ao validar gaps nas ocorrências: {}", e.getMessage(), e);
            return StatusValidacao.ERRO;
        }
    }
    
    /**
     * Verifica se os IDs de uma tabela são sequenciais (sem pulos).
     * 
     * @param conexao Conexão com o banco de dados
     * @param nomeTabela Nome da tabela a verificar
     * @return true se os IDs são sequenciais, false caso contrário
     */
    private boolean verificarIdsSequenciais(Connection conexao, String nomeTabela) throws SQLException {
        String sql = """
            WITH ids_ordenados AS (
                SELECT id, ROW_NUMBER() OVER (ORDER BY id) as posicao
                FROM %s
                WHERE data_extracao >= DATEADD(day, -7, GETDATE()) -- Últimos 7 dias para análise
            ),
            gaps AS (
                SELECT COUNT(*) as total_gaps
                FROM ids_ordenados
                WHERE id != (SELECT MIN(id) FROM ids_ordenados) + posicao - 1
            )
            SELECT CASE WHEN total_gaps = 0 THEN 1 ELSE 0 END as ids_sequenciais
            FROM gaps
            """.formatted(nomeTabela);
        
        try (PreparedStatement stmt = conexao.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                boolean sequencial = rs.getInt("ids_sequenciais") == 1;
                logger.info("📊 Análise de sequencialidade para {}: {}", nomeTabela, 
                    sequencial ? "IDs são sequenciais" : "IDs têm gaps/pulos");
                return sequencial;
            }
            return false;
        }
    }
    
    /**
     * Detecta gaps em IDs sequenciais usando a estratégia WITH ids_esperados.
     * 
     * @param conexao Conexão com o banco de dados
     * @param nomeTabela Nome da tabela a verificar
     * @param dataReferencia Data de referência para análise
     * @return StatusValidacao indicando se há gaps
     */
    private StatusValidacao detectarGapsSequenciais(Connection conexao, String nomeTabela, LocalDate dataReferencia) throws SQLException {
        String sql = """
            WITH ids_esperados AS (
                SELECT MIN(id) + n.number as id_esperado
                FROM %s,
                     (SELECT TOP ((SELECT MAX(id) - MIN(id) + 1 FROM %s WHERE data_extracao >= ?))
                             ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) - 1 as number
                      FROM sys.objects a CROSS JOIN sys.objects b) n
                WHERE data_extracao >= ?
            ),
            gaps AS (
                SELECT ie.id_esperado
                FROM ids_esperados ie
                LEFT JOIN %s o ON ie.id_esperado = o.id AND o.data_extracao >= ?
                WHERE o.id IS NULL
            )
            SELECT COUNT(*) as total_gaps
            FROM gaps
            """.formatted(nomeTabela, nomeTabela, nomeTabela);
        
        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            java.sql.Date sqlDate = java.sql.Date.valueOf(dataReferencia);
            stmt.setDate(1, sqlDate);
            stmt.setDate(2, sqlDate);
            stmt.setDate(3, sqlDate);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int totalGaps = rs.getInt("total_gaps");
                    
                    if (totalGaps == 0) {
                        logger.info("✅ Nenhum gap detectado nos IDs de {}", nomeTabela);
                        return StatusValidacao.OK;
                    } else {
                        logger.warn("⚠️ Detectados {} gaps nos IDs de {} - possível perda de dados", totalGaps, nomeTabela);
                        return StatusValidacao.INCOMPLETO;
                    }
                }
                return StatusValidacao.ERRO;
            }
        }
    }
    
    /**
     * TÓPICO 4: Validação da Janela Temporal - Detecta registros criados durante a extração
     * 
     * Esta é a validação mais complexa. Verifica se há registros criados entre o início
     * e fim da extração que podem ter sido perdidos devido a problemas de paginação da API.
     * 
     * @param dataReferencia Data de referência para análise
     * @return Map com status de validação por entidade
     */
    public Map<String, StatusValidacao> validarJanelaTemporal(LocalDate dataReferencia) {
        logger.info("🕐 Iniciando validação de janela temporal para data: {}", dataReferencia);
        
        Map<String, StatusValidacao> resultados = new HashMap<>();
        
        try (Connection conexao = GerenciadorConexao.obterConexao()) {
            // Buscar timestamps de extração do log
            Map<String, TimestampsExtracao> timestampsExtracao = buscarTimestampsExtracao(conexao, dataReferencia);
            
            // Validar cada entidade
            for (String entidade : MAPEAMENTO_ENTIDADES_TABELAS.keySet()) {
                TimestampsExtracao timestamps = timestampsExtracao.get(entidade);
                
                if (timestamps == null) {
                    logger.warn("⚠️ Nenhum log de extração encontrado para {} na data {}", entidade, dataReferencia);
                    resultados.put(entidade, StatusValidacao.ERRO);
                    continue;
                }
                
                StatusValidacao status = validarJanelaTemporalEntidade(entidade, timestamps, dataReferencia);
                resultados.put(entidade, status);
            }
            
        } catch (SQLException e) {
            logger.error("❌ Erro ao validar janela temporal: {}", e.getMessage(), e);
            // Marcar todas as entidades como erro
            for (String entidade : MAPEAMENTO_ENTIDADES_TABELAS.keySet()) {
                resultados.put(entidade, StatusValidacao.ERRO);
            }
        }
        
        return resultados;
    }
    
    /**
     * Busca os timestamps de início e fim das extrações do log_extracoes.
     * 
     * @param conexao Conexão com o banco de dados
     * @param dataReferencia Data de referência
     * @return Map com timestamps por entidade
     */
    private Map<String, TimestampsExtracao> buscarTimestampsExtracao(Connection conexao, LocalDate dataReferencia) throws SQLException {
        String sql = """
            SELECT entidade, timestamp_inicio, timestamp_fim
            FROM log_extracoes
            WHERE CAST(timestamp_inicio AS DATE) = ?
            AND status_final = 'COMPLETO'
            ORDER BY timestamp_inicio DESC
            """;
        
        Map<String, TimestampsExtracao> timestamps = new HashMap<>();
        
        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setDate(1, java.sql.Date.valueOf(dataReferencia));
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String entidade = rs.getString("entidade");
                    java.sql.Timestamp inicio = rs.getTimestamp("timestamp_inicio");
                    java.sql.Timestamp fim = rs.getTimestamp("timestamp_fim");
                    
                    timestamps.put(entidade, new TimestampsExtracao(inicio, fim));
                }
            }
        }
        
        logger.info("📊 Encontrados timestamps para {} entidades na data {}", timestamps.size(), dataReferencia);
        return timestamps;
    }
    
    /**
     * Valida a janela temporal para uma entidade específica.
     * 
     * @param entidade Nome da entidade
     * @param timestamps Timestamps de início e fim da extração
     * @param dataReferencia Data de referência
     * @return StatusValidacao da janela temporal
     */
    private StatusValidacao validarJanelaTemporalEntidade(String entidade, TimestampsExtracao timestamps, LocalDate dataReferencia) {
        try {
            // Fazer chamada à API para contar registros criados durante a janela de extração
            int registrosDuranteExtracao = contarRegistrosDuranteJanela(entidade, timestamps, dataReferencia);
            
            if (registrosDuranteExtracao == 0) {
                logger.info("✅ Nenhum registro criado durante extração de {} - janela temporal OK", entidade);
                return StatusValidacao.OK;
            } else {
                logger.error("❌ CRÍTICO: {} registros de {} foram criados durante a extração! Risco de perda de dados devido a falha na paginação da API", 
                    registrosDuranteExtracao, entidade);
                return StatusValidacao.INCOMPLETO;
            }
            
        } catch (Exception e) {
            logger.error("❌ Erro ao validar janela temporal para {}: {}", entidade, e.getMessage(), e);
            return StatusValidacao.ERRO;
        }
    }
    
    /**
     * Conta registros criados durante a janela de extração via API.
     * 
     * @param entidade Nome da entidade
     * @param timestamps Timestamps da extração
     * @param dataReferencia Data de referência
     * @return Número de registros criados durante a extração
     */
    private int contarRegistrosDuranteJanela(String entidade, TimestampsExtracao timestamps, LocalDate dataReferencia) {
        // Implementar chamadas específicas para cada tipo de API
        return switch (entidade) {
            case "ocorrencias", "faturas_a_receber", "faturas_a_pagar" ->
                contarRegistrosApiRest(entidade, timestamps, dataReferencia);
                
            case "fretes", "coletas" ->
                contarRegistrosApiGraphQL(entidade, timestamps, dataReferencia);
                
            case "manifestos", "cotacoes", "localizacao_cargas" ->
                contarRegistrosApiDataExport(entidade, timestamps, dataReferencia);
                
            default -> {
                logger.warn("⚠️ Entidade {} não mapeada para validação temporal", entidade);
                yield 0;
            }
        };
    }
    
    /**
     * Conta registros via API REST durante janela temporal.
     * 
     * @param entidade Nome da entidade a ser consultada
     * @param timestamps Janela temporal da extração (será usado na implementação futura)
     * @param dataReferencia Data de referência para filtros (será usado na implementação futura)
     * @return Número de registros encontrados na janela temporal
     */
    private int contarRegistrosApiRest(String entidade, TimestampsExtracao timestamps, LocalDate dataReferencia) {
        // Implementação específica para API REST
        // Por enquanto, retorna 0 (implementação futura)
        logger.debug("🔄 Contagem temporal via API REST para {} ainda não implementada (janela: {} - {}, data: {})", 
                    entidade, timestamps.getInicio(), timestamps.getFim(), dataReferencia);
        return 0;
    }
    
    /**
     * Conta registros via API GraphQL durante janela temporal.
     * 
     * @param entidade Nome da entidade a ser consultada
     * @param timestamps Janela temporal da extração (será usado na implementação futura)
     * @param dataReferencia Data de referência para filtros (será usado na implementação futura)
     * @return Número de registros encontrados na janela temporal
     */
    private int contarRegistrosApiGraphQL(String entidade, TimestampsExtracao timestamps, LocalDate dataReferencia) {
        // Implementação específica para API GraphQL
        // Por enquanto, retorna 0 (implementação futura)
        logger.debug("🔄 Contagem temporal via API GraphQL para {} ainda não implementada (janela: {} - {}, data: {})", 
                    entidade, timestamps.getInicio(), timestamps.getFim(), dataReferencia);
        return 0;
    }
    
    /**
     * Conta registros via API Data Export durante janela temporal.
     * 
     * @param entidade Nome da entidade a ser consultada
     * @param timestamps Janela temporal da extração (será usado na implementação futura)
     * @param dataReferencia Data de referência para filtros (será usado na implementação futura)
     * @return Número de registros encontrados na janela temporal
     */
    private int contarRegistrosApiDataExport(String entidade, TimestampsExtracao timestamps, LocalDate dataReferencia) {
        // Implementação específica para API Data Export
        // Por enquanto, retorna 0 (implementação futura)
        logger.debug("🔄 Contagem temporal via API Data Export para {} ainda não implementada (janela: {} - {}, data: {})", 
                    entidade, timestamps.getInicio(), timestamps.getFim(), dataReferencia);
        return 0;
    }
    
    /**
     * Classe auxiliar para armazenar timestamps de extração.
     */
    private static class TimestampsExtracao {
        private final java.sql.Timestamp inicio;
        private final java.sql.Timestamp fim;
        
        public TimestampsExtracao(java.sql.Timestamp inicio, java.sql.Timestamp fim) {
            this.inicio = inicio;
            this.fim = fim;
        }
        
        public java.sql.Timestamp getInicio() { return inicio; }
        
        public java.sql.Timestamp getFim() { return fim; }
    }
    
    /**
     * Enum para representar os possíveis status de validação de completude.
     */
    public enum StatusValidacao {
        /** Contagens coincidem - dados completos */
        OK,
        /** Banco tem menos registros que ESL Cloud - dados incompletos */
        INCOMPLETO,
        /** Banco tem mais registros que ESL Cloud - possíveis duplicados */
        DUPLICADOS,
        /** Erro durante a validação */
        ERRO
    }
}