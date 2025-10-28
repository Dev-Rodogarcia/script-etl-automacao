package br.com.extrator.db.repository;

import br.com.extrator.util.CarregadorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe base abstrata para repositórios com operações comuns de banco de dados.
 * Fornece métodos de conexão, MERGE (UPSERT) e utilitários para conversão de tipos.
 *
 * @param <T> Tipo da entidade gerenciada pelo repositório
 */
public abstract class AbstractRepository<T> {
    private static final Logger logger = LoggerFactory.getLogger(AbstractRepository.class);

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
     * @param entidades Lista de entidades a serem salvas
     * @return Número de registros afetados
     * @throws SQLException Se ocorrer um erro durante a operação
     */
    public int salvar(final List<T> entidades) throws SQLException {
        if (entidades == null || entidades.isEmpty()) {
            logger.warn("Lista de entidades vazia para {}", getClass().getSimpleName());
            return 0;
        }

        int totalAfetados = 0;

        try (Connection conexao = obterConexao()) {
            conexao.setAutoCommit(false);

            try {
                // Garantir que a tabela existe
                criarTabelaSeNaoExistir(conexao);

                // Executar MERGE para cada entidade
                for (final T entidade : entidades) {
                    totalAfetados += executarMerge(conexao, entidade);
                }

                conexao.commit();
                logger.info("Salvos {} registros de {} no banco de dados", totalAfetados, getClass().getSimpleName());

            } catch (final SQLException e) {
                conexao.rollback();
                logger.error("Erro ao salvar entidades de {}: {}", getClass().getSimpleName(), e.getMessage());
                throw e;
            }
        }

        return totalAfetados;
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
     * Método abstrato que deve ser implementado por cada repositório específico
     * para criar a tabela correspondente se ela não existir
     * @param conexao Conexão com o banco de dados
     * @throws SQLException Se ocorrer um erro durante a criação da tabela
     */
    protected abstract void criarTabelaSeNaoExistir(Connection conexao) throws SQLException;

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
