package br.com.extrator.persistencia.repositorio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.Test;

class AbstractRepositoryNoOpRefreshTest {

    @Test
    void deveContabilizarRefreshDeDataExtracaoQuandoNoOpIdempotenteForAceito() throws Exception {
        final String jdbcUrl = "jdbc:sqlite:file:repo_noop_refresh?mode=memory&cache=shared";
        try (Connection anchor = DriverManager.getConnection(jdbcUrl)) {
            anchor.createStatement().execute("""
                CREATE TABLE inventario_noop_refresh (
                    id TEXT PRIMARY KEY,
                    data_extracao TEXT
                )
                """);
            anchor.createStatement().execute("""
                INSERT INTO inventario_noop_refresh (id, data_extracao)
                VALUES ('inv-1', '2026-04-14T18:03:35')
                """);

            final NoOpRefreshRepository repository = new NoOpRefreshRepository(jdbcUrl);
            final int sucesso = repository.salvar(List.of("inv-1"));

            assertEquals(1, sucesso);
            assertEquals(1, repository.getUltimoResumoSalvamento().getRegistrosPersistidos());
            assertEquals(1, repository.getUltimoResumoSalvamento().getRegistrosNoOpIdempotente());

            try (PreparedStatement stmt = anchor.prepareStatement(
                "SELECT data_extracao FROM inventario_noop_refresh WHERE id = ?"
            )) {
                stmt.setString(1, "inv-1");
                try (ResultSet rs = stmt.executeQuery()) {
                    assertTrue(rs.next());
                    final Instant atualizado = Instant.parse(rs.getString("data_extracao"));
                    assertTrue(atualizado.isAfter(Instant.parse("2026-04-14T18:03:35Z")));
                }
            }
        }
    }

    private static final class NoOpRefreshRepository extends AbstractRepository<String> {
        private final String jdbcUrl;

        private NoOpRefreshRepository(final String jdbcUrl) {
            this.jdbcUrl = jdbcUrl;
        }

        @Override
        protected String getNomeTabela() {
            return "inventario_noop_refresh";
        }

        @Override
        protected Connection obterConexao() throws SQLException {
            return DriverManager.getConnection(jdbcUrl);
        }

        @Override
        protected void verificarTabelaExisteOuLancarErro(final Connection conexao) {
            // Tabela de teste preparada no fixture.
        }

        @Override
        protected boolean aceitarMergeSemAlteracoesComoSucesso(final String entidade) {
            return true;
        }

        @Override
        protected int refrescarDataExtracaoQuandoNoOp(final Connection conexao, final String entidade)
            throws SQLException {
            final Instant agora = Instant.now().truncatedTo(ChronoUnit.SECONDS);
            try (PreparedStatement stmt = conexao.prepareStatement(
                "UPDATE inventario_noop_refresh SET data_extracao = ? WHERE id = ?"
            )) {
                stmt.setString(1, Timestamp.from(agora).toInstant().toString());
                stmt.setString(2, entidade);
                return stmt.executeUpdate();
            }
        }

        @Override
        protected int executarMerge(final Connection conexao, final String entidade) {
            return 0;
        }
    }
}
