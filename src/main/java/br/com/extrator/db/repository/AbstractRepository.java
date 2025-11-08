package br.com.extrator.db.repository;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.extrator.util.CarregadorConfig;

/**
 * Classe base abstrata para repositórios com operações comuns de banco de dados.
 * Fornece métodos de conexão, MERGE (UPSERT) e utilitários para conversão de tipos.
 *
 * VERSÃO CORRIGIDA: Implementa tratamento robusto de erros individuais,
 * commit em batches e logging detalhado para identificar registros problemáticos.
 *
 * @param <T> Tipo da entidade gerenciada pelo repositório
 */
public abstract class AbstractRepository<T> {
    private static final Logger logger = LoggerFactory.getLogger(AbstractRepository.class);
    
    // Configuração de batch - commit a cada N registros
    private static final int BATCH_SIZE = 100;
    
    // Flag para controlar se deve parar na primeira falha ou continuar
    private static final boolean CONTINUAR_APOS_ERRO = true;

    private final String urlConexao;
    private final String usuario;
    private final String senha;

    /**
     * Construtor que inicializa as configurações de conexão com o banco de dados
     */
    protected AbstractRepository() {
        this.urlConexao = CarregadorConfig.obterUrlBancoDados();
        this.usuario = CarregadorConfig.obterUsuarioBancoDados();
        this.senha = CarregadorConfig.obterSenhaBancoDados();
    }

    /**
     * Obtém uma conexão com o banco de dados
     * @return Conexão com o banco de dados
     * @throws SQLException Se ocorrer um erro ao conectar
     */
    protected Connection obterConexao() throws SQLException {
        logger.debug("Conectando ao banco de dados: {}", urlConexao);
        return DriverManager.getConnection(urlConexao, usuario, senha);
    }

    /**
     * Salva uma lista de entidades no banco de dados usando operação MERGE (UPSERT)
     * 
     * VERSÃO CORRIGIDA:
     * - Trata cada registro individualmente (não perde todos por causa de 1 erro)
     * - Commit em batches para evitar transações gigantes
     * - Log detalhado de erros com identificação do registro problemático
     * - Retorna quantidade de registros salvos com sucesso
     * 
     * @param entidades Lista de entidades a serem salvas
     * @return Número de registros SALVOS COM SUCESSO (não total tentado)
     * @throws SQLException Se ocorrer um erro crítico na conexão
     */
    public int salvar(final List<T> entidades) throws SQLException {
        if (entidades == null || entidades.isEmpty()) {
            logger.warn("Lista de entidades vazia para {}", getClass().getSimpleName());
            return 0;
        }

        int totalSucesso = 0;
        int totalFalhas = 0;
        int registroAtual = 0;
        final int totalRegistros = entidades.size();

        logger.info("🔄 Iniciando salvamento de {} registros de {} (batch size: {})", 
            totalRegistros, getClass().getSimpleName(), BATCH_SIZE);

        try (Connection conexao = obterConexao()) {
            conexao.setAutoCommit(false);

            try {
                // Garantir que a tabela existe
                criarTabelaSeNaoExistir(conexao);

                // Processar cada entidade individualmente
                for (final T entidade : entidades) {
                    registroAtual++;
                    
                    try {
                        // Tenta executar o MERGE para este registro
                        final int rowsAffected = executarMerge(conexao, entidade);
                        totalSucesso += rowsAffected;
                        
                        // Commit em batches para evitar transações muito grandes
                        if (registroAtual % BATCH_SIZE == 0) {
                            conexao.commit();
                            logger.debug("✅ Batch commit: {}/{} registros salvos", registroAtual, totalRegistros);
                        }
                        
                    } catch (final SQLException e) {
                        totalFalhas++;
                        
                        // Log detalhado do erro COM o registro que falhou
                        logger.error("❌ Erro ao salvar registro {}/{} de {}: {} | Detalhes: {}", 
                            registroAtual, 
                            totalRegistros,
                            getClass().getSimpleName(),
                            e.getMessage(),
                            obterIdentificadorEntidade(entidade)); // ← Novo método auxiliar
                        
                        // Log da stack trace completa em nível DEBUG
                        logger.debug("Stack trace completo do erro:", e);
                        
                        if (!CONTINUAR_APOS_ERRO) {
                            // Se configurado para parar na primeira falha
                            logger.error("🚨 Abortando salvamento devido a erro crítico");
                            conexao.rollback();
                            throw e;
                        }
                        
                        // Caso contrário, continua processando os próximos registros
                        // Não faz rollback - mantém os registros salvos com sucesso
                    }
                }

                // Commit final dos registros restantes
                conexao.commit();
                
                // Log final com estatísticas
                if (totalFalhas > 0) {
                    logger.warn("⚠️ Salvamento concluído com {} sucessos e {} falhas de {} total ({}%)", 
                        totalSucesso, 
                        totalFalhas, 
                        totalRegistros,
                        String.format("%.1f", (totalSucesso * 100.0 / totalRegistros)));
                } else {
                    logger.info("✅ Salvamento 100% concluído: {} registros de {} no banco", 
                        totalSucesso, getClass().getSimpleName());
                }

            } catch (final SQLException e) {
                // Erro crítico na conexão/transação
                conexao.rollback();
                logger.error("🚨 Erro crítico ao salvar entidades de {}: {}", 
                    getClass().getSimpleName(), e.getMessage());
                throw e;
            }
        }

        return totalSucesso;
    }

    /**
     * Salva uma única entidade no banco de dados
     * @param entidade Entidade a ser salva
     * @return Número de registros afetados (0 ou 1)
     * @throws SQLException Se ocorrer um erro durante a operação
     */
    public int salvar(final T entidade) throws SQLException {
        if (entidade == null) {
            logger.warn("Entidade nula para {}", getClass().getSimpleName());
            return 0;
        }

        final List<T> entidades = new ArrayList<>();
        entidades.add(entidade);
        return salvar(entidades);
    }

    /**
     * Método auxiliar para obter um identificador legível da entidade para logs.
     * Tenta extrair informações básicas da entidade usando reflexão.
     * 
     * @param entidade Entidade a ser identificada
     * @return String com identificação básica da entidade
     */
    private String obterIdentificadorEntidade(final T entidade) {
        if (entidade == null) {
            return "null";
        }
        
        try {
            // Tenta obter campos comuns via reflexão
            final StringBuilder info = new StringBuilder();
            info.append(entidade.getClass().getSimpleName()).append("{");
            
            // Tenta pegar alguns campos comuns
            final String[] camposPossiveis = {"id", "sequenceCode", "sequence_code", "documentNumber"};
            boolean encontrouCampo = false;
            
            for (final String campo : camposPossiveis) {
                try {
                    final java.lang.reflect.Method getter = encontrarGetter(entidade.getClass(), campo);
                    if (getter != null) {
                        final Object valor = getter.invoke(entidade);
                        if (valor != null) {
                            if (encontrouCampo) info.append(", ");
                            info.append(campo).append("=").append(valor);
                            encontrouCampo = true;
                        }
                    }
                } catch (final java.lang.reflect.InvocationTargetException | 
                        java.lang.IllegalAccessException | 
                        java.lang.IllegalArgumentException ignored) {
                    // Ignora se não conseguir acessar o campo via reflexão
                }
            }
            
            if (!encontrouCampo) {
                info.append("toString=").append(entidade.toString());
            }
            
            info.append("}");
            return info.toString();
            
        } catch (final Exception e) {
            // Fallback geral: retorna identificador simples se qualquer operação falhar
            return entidade.getClass().getSimpleName() + "@" + entidade.hashCode();
        }
    }
    
    /**
     * Encontra o método getter para um campo usando convenções Java
     */
    private java.lang.reflect.Method encontrarGetter(final Class<?> clazz, final String nomeCampo) {
        try {
            // Tenta get + CamelCase
            final String getterName = "get" + nomeCampo.substring(0, 1).toUpperCase() + nomeCampo.substring(1);
            return clazz.getMethod(getterName);
        } catch (final NoSuchMethodException e1) {
            try {
                // Tenta snake_case convertido para camelCase
                final String camelCase = converterSnakeToCamel(nomeCampo);
                final String getterName = "get" + camelCase.substring(0, 1).toUpperCase() + camelCase.substring(1);
                return clazz.getMethod(getterName);
            } catch (final NoSuchMethodException e2) {
                return null;
            }
        }
    }
    
    /**
     * Converte snake_case para camelCase
     */
    private String converterSnakeToCamel(final String snake) {
        final StringBuilder camel = new StringBuilder();
        boolean proximaMaiuscula = false;
        for (final char c : snake.toCharArray()) {
            if (c == '_') {
                proximaMaiuscula = true;
            } else {
                camel.append(proximaMaiuscula ? Character.toUpperCase(c) : c);
                proximaMaiuscula = false;
            }
        }
        return camel.toString();
    }

    /**
     * Método abstrato que deve ser implementado por cada repositório específico
     * para criar a tabela correspondente se ela não existir
     * @param conexao Conexão com o banco de dados
     * @throws SQLException Se ocorrer um erro durante a criação da tabela
     */
    protected abstract void criarTabelaSeNaoExistir(Connection conexao) throws SQLException;
    
    /**
     * Método público para criar a tabela se não existir.
     * Permite que classes externas (como AuditoriaValidator) criem tabelas sem precisar salvar dados.
     * 
     * @param conexao Conexão com o banco de dados
     * @throws SQLException Se ocorrer um erro durante a criação da tabela
     */
    public void criarTabelaSeNaoExistirPublico(Connection conexao) throws SQLException {
        criarTabelaSeNaoExistir(conexao);
    }

    /**
     * Método abstrato que deve ser implementado por cada repositório específico
     * para executar a operação MERGE (UPSERT) da entidade
     * @param conexao Conexão com o banco de dados
     * @param entidade Entidade a ser inserida/atualizada
     * @return Número de registros afetados (0 ou 1)
     * @throws SQLException Se ocorrer um erro durante a operação
     */
    protected abstract int executarMerge(Connection conexao, T entidade) throws SQLException;

    /**
     * Método abstrato que retorna o nome da tabela para este repositório
     * @return Nome da tabela
     */
    protected abstract String getNomeTabela();

    /**
     * Verifica se uma tabela existe no banco de dados
     * @param conexao Conexão com o banco de dados
     * @param nomeTabela Nome da tabela a verificar
     * @return true se a tabela existe, false caso contrário
     * @throws SQLException Se ocorrer um erro durante a verificação
     */
    protected boolean verificarTabelaExiste(final Connection conexao, final String nomeTabela) throws SQLException {
        final DatabaseMetaData metaData = conexao.getMetaData();
        try (ResultSet resultSet = metaData.getTables(null, null, nomeTabela.toUpperCase(), new String[]{"TABLE"})) {
            return resultSet.next();
        }
    }

    /**
     * Executa um comando DDL (CREATE TABLE, ALTER TABLE, etc.)
     * @param conexao Conexão com o banco de dados
     * @param sql Comando SQL a ser executado
     * @throws SQLException Se ocorrer um erro durante a execução
     */
    protected void executarDDL(final Connection conexao, final String sql) throws SQLException {
        logger.debug("Executando DDL: {}", sql);
        try (Statement statement = conexao.createStatement()) {
            statement.execute(sql);
        }
    }

    /**
     * Define um parâmetro no PreparedStatement, tratando valores nulos adequadamente
     * @param statement PreparedStatement
     * @param index Índice do parâmetro (1-based)
     * @param value Valor a ser definido
     * @param sqlType Tipo SQL do parâmetro
     * @throws SQLException Se ocorrer um erro ao definir o parâmetro
     */
    protected void setParameter(final PreparedStatement statement, final int index, final Object value, final int sqlType) throws SQLException {
        if (value == null) {
            statement.setNull(index, sqlType);
        } else {
            statement.setObject(index, value, sqlType);
        }
    }

    /**
     * Define um parâmetro String no PreparedStatement
     * @param statement PreparedStatement
     * @param index Índice do parâmetro (1-based)
     * @param value Valor String a ser definido
     * @throws SQLException Se ocorrer um erro ao definir o parâmetro
     */
    protected void setStringParameter(final PreparedStatement statement, final int index, final String value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            statement.setString(index, value);
        }
    }

    /**
     * Define um parâmetro Integer no PreparedStatement
     * @param statement PreparedStatement
     * @param index Índice do parâmetro (1-based)
     * @param value Valor Integer a ser definido
     * @throws SQLException Se ocorrer um erro ao definir o parâmetro
     */
    protected void setIntegerParameter(final PreparedStatement statement, final int index, final Integer value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.INTEGER);
        } else {
            statement.setInt(index, value);
        }
    }

    /**
     * Define um parâmetro Double no PreparedStatement
     * @param statement PreparedStatement
     * @param index Índice do parâmetro (1-based)
     * @param value Valor Double a ser definido
     * @throws SQLException Se ocorrer um erro ao definir o parâmetro
     */
    protected void setDoubleParameter(final PreparedStatement statement, final int index, final Double value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.DOUBLE);
        } else {
            statement.setDouble(index, value);
        }
    }

    /**
     * Define um parâmetro Boolean no PreparedStatement
     * @param statement PreparedStatement
     * @param index Índice do parâmetro (1-based)
     * @param value Valor Boolean a ser definido
     * @throws SQLException Se ocorrer um erro ao definir o parâmetro
     */
    protected void setBooleanParameter(final PreparedStatement statement, final int index, final Boolean value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.BOOLEAN);
        } else {
            statement.setBoolean(index, value);
        }
    }

    /**
     * Define um parâmetro BigDecimal no PreparedStatement
     * @param statement PreparedStatement
     * @param index Índice do parâmetro (1-based)
     * @param value Valor BigDecimal a ser definido
     * @throws SQLException Se ocorrer um erro ao definir o parâmetro
     */
    protected void setBigDecimalParameter(final PreparedStatement statement, final int index, final java.math.BigDecimal value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.DECIMAL);
        } else {
            statement.setBigDecimal(index, value);
        }
    }

    /**
     * Define um parâmetro LocalDateTime no PreparedStatement
     * @param statement PreparedStatement
     * @param index Índice do parâmetro (1-based)
     * @param value Valor LocalDateTime a ser definido
     * @throws SQLException Se ocorrer um erro ao definir o parâmetro
     */
    protected void setDateTimeParameter(final PreparedStatement statement, final int index, final LocalDateTime value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.TIMESTAMP);
        } else {
            statement.setTimestamp(index, Timestamp.valueOf(value));
        }
    }

    /**
     * Define um parâmetro Instant no PreparedStatement
     * @param statement PreparedStatement
     * @param index Índice do parâmetro (1-based)
     * @param value Valor Instant a ser definido
     * @throws SQLException Se ocorrer um erro ao definir o parâmetro
     */
    protected void setInstantParameter(final PreparedStatement statement, final int index, final Instant value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.TIMESTAMP);
        } else {
            statement.setTimestamp(index, Timestamp.from(value));
        }
    }

    /**
     * Define um parâmetro Long no PreparedStatement
     * @param statement PreparedStatement
     * @param index Índice do parâmetro (1-based)
     * @param value Valor Long a ser definido
     * @throws SQLException Se ocorrer um erro ao definir o parâmetro
     */
    protected void setLongParameter(final PreparedStatement statement, final int index, final Long value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.BIGINT);
        } else {
            statement.setLong(index, value);
        }
    }

    /**
     * Define um parâmetro LocalDate no PreparedStatement
     * @param statement PreparedStatement
     * @param index Índice do parâmetro (1-based)
     * @param value Valor LocalDate a ser definido
     * @throws SQLException Se ocorrer um erro ao definir o parâmetro
     */
    protected void setDateParameter(final PreparedStatement statement, final int index, final LocalDate value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.DATE);
        } else {
            statement.setDate(index, Date.valueOf(value));
        }
    }

    /**
     * Normaliza o nome de um campo para uso em SQL (remove caracteres especiais, etc.)
     * @param nomeCampo Nome do campo original
     * @return Nome do campo normalizado
     */
    protected String normalizarNomeCampo(final String nomeCampo) {
        if (nomeCampo == null) {
            return null;
        }

        return nomeCampo
                .replaceAll("[^a-zA-Z0-9_]", "_")
                .replaceAll("_{2,}", "_")
                .replaceAll("^_|_$", "")
                .toLowerCase();
    }
}