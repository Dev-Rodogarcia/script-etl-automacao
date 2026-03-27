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
                int registrosHistorico = 0;

                for (final UsuarioSistemaEntity usuario : snapshot) {
                    if (usuario.getUserId() == null) {
                        continue;
                    }
                    idsSnapshot.add(usuario.getUserId());
                    final EstadoAtualUsuario anterior = existentes.get(usuario.getUserId());
                    upsertUsuarioAtivo(conexao, usuario, observadoEm);
                    totalOperacoes++;
                    if (houveMudancaSemantica(anterior, usuario, true)) {
                        inserirHistorico(conexao, executionUuid, usuario, true, observadoEm, tipoMudanca(anterior, true));
                        registrosHistorico++;
                    }
                }

                final List<EstadoAtualUsuario> desativados = existentes.values().stream()
                    .filter(EstadoAtualUsuario::ativo)
                    .filter(usuario -> !idsSnapshot.contains(usuario.userId()))
                    .toList();
                for (final EstadoAtualUsuario usuarioDesativado : desativados) {
                    desativarUsuario(conexao, usuarioDesativado.userId(), observadoEm);
                    totalOperacoes++;
                    inserirHistorico(
                        conexao,
                        executionUuid,
                        usuarioDesativado.toEntity(observadoEm),
                        false,
                        observadoEm,
                        "DEACTIVATED"
                    );
                    registrosHistorico++;
                }

                conexao.commit();
                conexao.setAutoCommit(autoCommitOriginal);
                return new SnapshotMetrics(totalOperacoes, totalOperacoes, 0, registrosHistorico);
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

    private void upsertUsuarioAtivo(final Connection conexao,
                                    final UsuarioSistemaEntity usuario,
                                    final LocalDateTime observadoEm) throws SQLException {
        final String sql = """
            MERGE dbo.dim_usuarios AS T
            USING (VALUES (?, ?, ?, ?, ?, ?))
                AS S (user_id, nome, ativo, origem_atualizado_em, data_atualizacao, ultima_extracao_em)
            ON T.user_id = S.user_id
            WHEN MATCHED THEN
                UPDATE SET
                    T.nome = S.nome,
                    T.ativo = S.ativo,
                    T.origem_atualizado_em = S.origem_atualizado_em,
                    T.data_atualizacao = S.data_atualizacao,
                    T.ultima_extracao_em = S.ultima_extracao_em
            WHEN NOT MATCHED THEN
                INSERT (user_id, nome, ativo, origem_atualizado_em, data_atualizacao, ultima_extracao_em)
                VALUES (S.user_id, S.nome, S.ativo, S.origem_atualizado_em, S.data_atualizacao, S.ultima_extracao_em);
            """;
        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setLong(1, usuario.getUserId());
            stmt.setString(2, usuario.getNome());
            stmt.setBoolean(3, true);
            stmt.setTimestamp(4, timestamp(usuario.getOrigemAtualizadoEm()));
            stmt.setTimestamp(5, timestamp(observadoEm));
            stmt.setTimestamp(6, timestamp(observadoEm));
            stmt.executeUpdate();
        }
    }

    private void desativarUsuario(final Connection conexao,
                                  final Long userId,
                                  final LocalDateTime observadoEm) throws SQLException {
        final String sql = """
            UPDATE dbo.dim_usuarios
            SET ativo = 0,
                data_atualizacao = ?,
                ultima_extracao_em = ?
            WHERE user_id = ?
              AND ativo = 1
            """;
        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setTimestamp(1, timestamp(observadoEm));
            stmt.setTimestamp(2, timestamp(observadoEm));
            stmt.setLong(3, userId);
            stmt.executeUpdate();
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
                observado_em,
                tipo_alteracao
            )
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
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

    private boolean houveMudancaSemantica(final EstadoAtualUsuario anterior,
                                          final UsuarioSistemaEntity atual,
                                          final boolean ativoAtual) {
        if (anterior == null) {
            return true;
        }
        if (anterior.ativo() != ativoAtual) {
            return true;
        }
        if (!java.util.Objects.equals(anterior.nome(), atual.getNome())) {
            return true;
        }
        return !java.util.Objects.equals(anterior.origemAtualizadoEm(), atual.getOrigemAtualizadoEm());
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
