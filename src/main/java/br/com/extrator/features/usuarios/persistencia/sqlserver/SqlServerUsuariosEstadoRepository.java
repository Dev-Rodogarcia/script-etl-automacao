package br.com.extrator.features.usuarios.persistencia.sqlserver;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import br.com.extrator.features.usuarios.aplicacao.UsuariosEstadoPort;
import br.com.extrator.persistencia.entidade.UsuarioSistemaEntity;
import br.com.extrator.suporte.banco.GerenciadorConexao;

public final class SqlServerUsuariosEstadoRepository implements UsuariosEstadoPort {
    private static final String HASH_USUARIO_ATUAL_SQL = """
        HASHBYTES('SHA2_256', CONCAT_WS(N'|',
            CONVERT(NVARCHAR(20), V.user_id),
            COALESCE(V.nome, N'<NULL>'),
            CONVERT(NVARCHAR(1), CONVERT(TINYINT, V.ativo)),
            COALESCE(CONVERT(NVARCHAR(33), V.origem_atualizado_em, 126), N'<NULL>'),
            CONVERT(NVARCHAR(1), CONVERT(TINYINT, V.excluido_na_origem))
        ))
        """;

    private static final String HASH_USUARIO_HISTORICO_SQL = """
        HASHBYTES('SHA2_256', CONCAT_WS(N'|',
            CONVERT(NVARCHAR(20), V.user_id),
            COALESCE(V.nome, N'<NULL>'),
            CONVERT(NVARCHAR(1), CONVERT(TINYINT, V.ativo)),
            COALESCE(CONVERT(NVARCHAR(33), V.origem_atualizado_em, 126), N'<NULL>')
        ))
        """;

    @Override
    public SnapshotMetrics aplicarSnapshot(final List<UsuarioSistemaEntity> usuariosAtivos,
                                           final String executionUuid,
                                           final LocalDateTime observadoEm) throws SQLException {
        final List<UsuarioSistemaEntity> snapshot = usuariosAtivos == null ? List.of() : List.copyOf(usuariosAtivos);
        try (Connection conexao = GerenciadorConexao.obterConexao()) {
            final boolean autoCommitOriginal = conexao.getAutoCommit();
            conexao.setAutoCommit(false);
            try {
                final Map<Long, EstadoAtualUsuario> existentes = carregarUsuarios(conexao);
                final Set<Long> idsSnapshot = new LinkedHashSet<>();
                int totalOperacoes = 0;
                int registrosPersistidos = 0;
                int noOpIdempotente = 0;
                int registrosHistorico = 0;

                for (final UsuarioSistemaEntity usuario : snapshot) {
                    if (usuario.getUserId() == null) {
                        continue;
                    }
                    idsSnapshot.add(usuario.getUserId());
                    final EstadoAtualUsuario anterior = existentes.get(usuario.getUserId());
                    totalOperacoes++;
                    final boolean alterado = upsertUsuarioAtivo(conexao, usuario, observadoEm);
                    if (alterado) {
                        registrosPersistidos++;
                        inserirHistorico(conexao, executionUuid, usuario, true, observadoEm, tipoMudanca(anterior, true));
                        registrosHistorico++;
                    } else {
                        noOpIdempotente++;
                    }
                }

                final List<EstadoAtualUsuario> desativados = existentes.values().stream()
                    .filter(EstadoAtualUsuario::ativo)
                    .filter(usuario -> !idsSnapshot.contains(usuario.userId()))
                    .toList();
                for (final EstadoAtualUsuario usuarioDesativado : desativados) {
                    totalOperacoes++;
                    final boolean alterado = desativarUsuario(conexao, usuarioDesativado.userId(), observadoEm);
                    if (alterado) {
                        registrosPersistidos++;
                        inserirHistorico(
                            conexao,
                            executionUuid,
                            usuarioDesativado.toEntity(observadoEm),
                            false,
                            observadoEm,
                            "DEACTIVATED"
                        );
                        registrosHistorico++;
                    } else {
                        noOpIdempotente++;
                    }
                }

                conexao.commit();
                conexao.setAutoCommit(autoCommitOriginal);
                return new SnapshotMetrics(totalOperacoes, registrosPersistidos, noOpIdempotente, registrosHistorico);
            } catch (final SQLException e) {
                conexao.rollback();
                throw e;
            }
        }
    }

    private Map<Long, EstadoAtualUsuario> carregarUsuarios(final Connection conexao) throws SQLException {
        final String sql = """
            SELECT user_id, nome, ativo, origem_atualizado_em
            FROM dbo.dim_usuarios
            """;
        final Map<Long, EstadoAtualUsuario> usuarios = new LinkedHashMap<>();
        try (PreparedStatement stmt = conexao.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                usuarios.put(
                    rs.getLong("user_id"),
                    new EstadoAtualUsuario(
                        rs.getLong("user_id"),
                        rs.getString("nome"),
                        rs.getBoolean("ativo"),
                        toLocalDateTime(rs.getTimestamp("origem_atualizado_em"))
                    )
                );
            }
        }
        return usuarios;
    }

    private boolean upsertUsuarioAtivo(final Connection conexao,
                                       final UsuarioSistemaEntity usuario,
                                       final LocalDateTime observadoEm) throws SQLException {
        final String freshnessGuard =
            "(COALESCE(T.origem_atualizado_em, T.data_atualizacao) IS NULL "
                + "OR COALESCE(S.origem_atualizado_em, S.data_atualizacao) >= COALESCE(T.origem_atualizado_em, T.data_atualizacao))";
        final String sql = """
            MERGE dbo.dim_usuarios WITH (HOLDLOCK) AS T
            USING (
                SELECT
                    V.user_id,
                    V.nome,
                    V.ativo,
                    V.origem_atualizado_em,
                    V.data_atualizacao,
                    V.ultima_extracao_em,
                    V.excluido_na_origem,
                    %s AS hash_linha
                FROM (VALUES (?, ?, ?, ?, ?, ?, CAST(0 AS bit)))
                    AS V (user_id, nome, ativo, origem_atualizado_em, data_atualizacao, ultima_extracao_em, excluido_na_origem)
            ) AS S
            ON T.user_id = S.user_id
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
                VALUES (S.user_id, S.nome, S.ativo, S.origem_atualizado_em, S.data_atualizacao, S.ultima_extracao_em, S.excluido_na_origem, S.hash_linha);
            """.formatted(HASH_USUARIO_ATUAL_SQL, freshnessGuard);
        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setLong(1, usuario.getUserId());
            stmt.setString(2, usuario.getNome());
            stmt.setBoolean(3, true);
            stmt.setTimestamp(4, timestamp(usuario.getOrigemAtualizadoEm()));
            stmt.setTimestamp(5, timestamp(observadoEm));
            stmt.setTimestamp(6, timestamp(observadoEm));
            return stmt.executeUpdate() > 0;
        }
    }

    private boolean desativarUsuario(final Connection conexao,
                                     final Long userId,
                                     final LocalDateTime observadoEm) throws SQLException {
        final String sql = """
            UPDATE T
            SET ativo = 0,
                data_atualizacao = ?,
                ultima_extracao_em = ?,
                hash_linha = HASHBYTES('SHA2_256', CONCAT_WS(N'|',
                    CONVERT(NVARCHAR(20), T.user_id),
                    COALESCE(T.nome, N'<NULL>'),
                    CONVERT(NVARCHAR(1), CONVERT(TINYINT, CAST(0 AS bit))),
                    COALESCE(CONVERT(NVARCHAR(33), T.origem_atualizado_em, 126), N'<NULL>'),
                    CONVERT(NVARCHAR(1), CONVERT(TINYINT, T.excluido_na_origem))
                ))
            FROM dbo.dim_usuarios AS T
            WHERE T.user_id = ?
              AND T.ativo = 1
            """;
        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setTimestamp(1, timestamp(observadoEm));
            stmt.setTimestamp(2, timestamp(observadoEm));
            stmt.setLong(3, userId);
            return stmt.executeUpdate() > 0;
        }
    }

    private void inserirHistorico(final Connection conexao,
                                  final String executionUuid,
                                  final UsuarioSistemaEntity usuario,
                                  final boolean ativo,
                                  final LocalDateTime observadoEm,
                                  final String tipoAlteracao) throws SQLException {
        final String sql = """
            INSERT INTO dbo.dim_usuarios_historico (
                execution_uuid,
                user_id,
                nome,
                ativo,
                origem_atualizado_em,
                hash_linha,
                observado_em,
                tipo_alteracao
            )
            SELECT
                V.execution_uuid,
                V.user_id,
                V.nome,
                V.ativo,
                V.origem_atualizado_em,
                %s AS hash_linha,
                V.observado_em,
                V.tipo_alteracao
            FROM (VALUES (?, ?, ?, ?, ?, ?, ?))
                AS V (execution_uuid, user_id, nome, ativo, origem_atualizado_em, observado_em, tipo_alteracao)
            """.formatted(HASH_USUARIO_HISTORICO_SQL);
        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setString(1, executionUuid);
            stmt.setLong(2, usuario.getUserId());
            stmt.setString(3, usuario.getNome());
            stmt.setBoolean(4, ativo);
            stmt.setTimestamp(5, timestamp(usuario.getOrigemAtualizadoEm()));
            stmt.setTimestamp(6, timestamp(observadoEm));
            stmt.setString(7, tipoAlteracao);
            stmt.executeUpdate();
        }
    }

    private String tipoMudanca(final EstadoAtualUsuario anterior, final boolean ativoAtual) {
        if (anterior == null) {
            return "INSERTED";
        }
        if (anterior.ativo() != ativoAtual) {
            return ativoAtual ? "REACTIVATED" : "DEACTIVATED";
        }
        return "UPDATED";
    }

    private Timestamp timestamp(final LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    private LocalDateTime toLocalDateTime(final Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private record EstadoAtualUsuario(
        Long userId,
        String nome,
        boolean ativo,
        LocalDateTime origemAtualizadoEm
    ) {
        private UsuarioSistemaEntity toEntity(final LocalDateTime observadoEm) {
            final UsuarioSistemaEntity entity = new UsuarioSistemaEntity();
            entity.setUserId(userId);
            entity.setNome(nome);
            entity.setAtivo(false);
            entity.setOrigemAtualizadoEm(origemAtualizadoEm);
            entity.setDataAtualizacao(observadoEm);
            entity.setUltimaExtracaoEm(observadoEm);
            return entity;
        }
    }
}
