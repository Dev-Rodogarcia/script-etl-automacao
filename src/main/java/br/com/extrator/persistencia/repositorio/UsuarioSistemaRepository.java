/* ==[DOC-FILE]===============================================================
Arquivo : src/main/java/br/com/extrator/db/repository/UsuarioSistemaRepository.java
Classe  : UsuarioSistemaRepository (class)
Pacote  : br.com.extrator.persistencia.repositorio
Modulo  : Repositorio de dados
Papel   : Implementa responsabilidade de usuario sistema repository.

Conecta com:
- UsuarioSistemaEntity (db.entity)

Fluxo geral:
1) Monta comandos SQL e parametros.
2) Executa operacoes de persistencia/consulta no banco.
3) Converte resultado para entidades de dominio.

Estrutura interna:
Metodos principais:
- getNomeTabela(): expone valor atual do estado interno.
Atributos-chave:
- logger: logger da classe para diagnostico.
- NOME_TABELA: campo de estado para "nome tabela".
[DOC-FILE-END]============================================================== */

package br.com.extrator.persistencia.repositorio;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.extrator.persistencia.entidade.UsuarioSistemaEntity;

/**
 * Repositório para operações de persistência da entidade UsuarioSistemaEntity.
 * Utiliza operações MERGE (UPSERT) com a chave primária (user_id).
 */
public class UsuarioSistemaRepository extends AbstractRepository<UsuarioSistemaEntity> {
    private static final Logger logger = LoggerFactory.getLogger(UsuarioSistemaRepository.class);
    private static final String NOME_TABELA = "dim_usuarios";
    private static final String HASH_USUARIO_SQL = """
        HASHBYTES('SHA2_256', CONCAT_WS(N'|',
            CONVERT(NVARCHAR(20), V.id),
            COALESCE(V.nome, N'<NULL>'),
            CONVERT(NVARCHAR(1), CONVERT(TINYINT, V.ativo)),
            COALESCE(CONVERT(NVARCHAR(33), V.origem_atualizado_em, 126), N'<NULL>'),
            CONVERT(NVARCHAR(1), CONVERT(TINYINT, V.excluido_na_origem))
        ))
        """;

    @Override
    protected String getNomeTabela() {
        return NOME_TABELA;
    }

    @Override
    protected boolean aceitarMergeSemAlteracoesComoSucesso(final UsuarioSistemaEntity usuario) {
        return true;
    }

    /**
     * Indica se a dimensão de usuários já possui ao menos um registro persistido.
     */
    public boolean temDados() throws SQLException {
        final String sql = String.format("SELECT TOP 1 1 FROM dbo.%s", NOME_TABELA);
        try (Connection conexao = obterConexao();
             PreparedStatement statement = conexao.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next();
        }
    }

    /**
     * Executa a operação MERGE (UPSERT) para inserir ou atualizar um usuário no banco.
     */
    @Override
    protected int executarMerge(final Connection conexao, final UsuarioSistemaEntity usuario) throws SQLException {
        if (usuario.getUserId() == null) {
            throw new SQLException("Não é possível executar o MERGE para Usuário do Sistema sem um 'user_id'.");
        }

        final String freshnessGuard =
            "(COALESCE(T.origem_atualizado_em, T.data_atualizacao) IS NULL "
                + "OR COALESCE(S.origem_atualizado_em, S.data_atualizacao) >= COALESCE(T.origem_atualizado_em, T.data_atualizacao))";
        final String sql = String.format("""
            MERGE dbo.%s WITH (HOLDLOCK) AS T
            USING (
                SELECT
                    V.id,
                    V.nome,
                    V.ativo,
                    V.origem_atualizado_em,
                    V.data_atualizacao,
                    V.ultima_extracao_em,
                    V.excluido_na_origem,
                    %s AS hash_linha
                FROM (VALUES (?, ?, ?, ?, ?, ?, CAST(0 AS bit)))
                    AS V (id, nome, ativo, origem_atualizado_em, data_atualizacao, ultima_extracao_em, excluido_na_origem)
            ) AS S
            ON T.user_id = S.id
            WHEN MATCHED AND (
                (%s OR T.excluido_na_origem = 1)
                AND (
                    T.hash_linha IS NULL
                 OR S.hash_linha IS NULL
                 OR T.hash_linha <> S.hash_linha
                )
            ) THEN
                UPDATE SET
                    T.nome = S.nome,
                    T.ativo = S.ativo,
                    T.origem_atualizado_em = S.origem_atualizado_em,
                    T.data_atualizacao = S.data_atualizacao,
                    T.ultima_extracao_em = S.ultima_extracao_em,
                    T.excluido_na_origem = S.excluido_na_origem,
                    T.hash_linha = S.hash_linha
            WHEN NOT MATCHED THEN
                INSERT (user_id, nome, ativo, origem_atualizado_em, data_atualizacao, ultima_extracao_em, excluido_na_origem, hash_linha)
                VALUES (S.id, S.nome, S.ativo, S.origem_atualizado_em, S.data_atualizacao, S.ultima_extracao_em, S.excluido_na_origem, S.hash_linha);
            """, NOME_TABELA, HASH_USUARIO_SQL, freshnessGuard);

        try (PreparedStatement statement = conexao.prepareStatement(sql)) {
            int paramIndex = 1;
            setLongParameter(statement, paramIndex++, usuario.getUserId());
            setStringParameter(statement, paramIndex++, usuario.getNome());
            statement.setBoolean(paramIndex++, usuario.isAtivo());
            setDateTimeParameter(statement, paramIndex++, usuario.getOrigemAtualizadoEm());
            setDateTimeParameter(statement, paramIndex++, usuario.getDataAtualizacao());
            setDateTimeParameter(statement, paramIndex++, usuario.getUltimaExtracaoEm());
            
            if (paramIndex != 7) {
                throw new SQLException(String.format("Número incorreto de parâmetros: esperado 6, definido %d", paramIndex - 1));
            }

            final int rowsAffected = statement.executeUpdate();
            logger.debug("MERGE executado para Usuário do Sistema user_id {}: {} linha(s) afetada(s)", usuario.getUserId(), rowsAffected);
            return rowsAffected;
        }
    }
}
