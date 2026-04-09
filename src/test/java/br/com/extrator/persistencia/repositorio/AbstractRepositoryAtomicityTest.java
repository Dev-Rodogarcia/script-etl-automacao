package br.com.extrator.persistencia.repositorio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.junit.jupiter.api.Test;

class AbstractRepositoryAtomicityTest {

    @Test
    void deveFazerRollbackIntegralNoModoAtomico() throws Exception {
        final String jdbcUrl = "jdbc:sqlite:file:repo_atomic?mode=memory&cache=shared";
        try (Connection anchor = DriverManager.getConnection(jdbcUrl)) {
            anchor.createStatement().execute("CREATE TABLE TEST_ATOMIC (id TEXT PRIMARY KEY, value TEXT NOT NULL)");
            System.clearProperty("db.atomic.commit");
            System.clearProperty("db.continue.on.error");

            final AtomicTestRepository repository = new AtomicTestRepository(jdbcUrl);
            assertThrows(SQLException.class, () -> repository.salvar(List.of(
                new TestRecord("ok", "A", false, false),
                new TestRecord("fail", "B", true, false)
            )));

            assertEquals(0, contarRegistros(anchor));
        } finally {
            System.clearProperty("db.atomic.commit");
            System.clearProperty("db.continue.on.error");
        }
    }

    @Test
    void devePermitirPersistenciaParcialSomenteQuandoModoAtomicoForDesabilitadoExplicitamente() throws Exception {
        final String jdbcUrl = "jdbc:sqlite:file:repo_best_effort?mode=memory&cache=shared";
        try (Connection anchor = DriverManager.getConnection(jdbcUrl)) {
            anchor.createStatement().execute("CREATE TABLE TEST_ATOMIC (id TEXT PRIMARY KEY, value TEXT NOT NULL)");
            System.setProperty("db.atomic.commit", "false");
            System.setProperty("db.continue.on.error", "true");

            final AtomicTestRepository repository = new AtomicTestRepository(jdbcUrl);
            final int salvos = repository.salvar(List.of(
                new TestRecord("ok", "A", false, false),
                new TestRecord("fail", "B", true, false)
            ));

            assertEquals(1, salvos);
            assertEquals(1, contarRegistros(anchor));
        } finally {
            System.clearProperty("db.atomic.commit");
            System.clearProperty("db.continue.on.error");
        }
    }

    @Test
    void deveContarNoOpIdempotenteComoSucessoQuandoRepositorioPermitir() throws Exception {
        final String jdbcUrl = "jdbc:sqlite:file:repo_idempotent_noop?mode=memory&cache=shared";
        try (Connection anchor = DriverManager.getConnection(jdbcUrl)) {
            anchor.createStatement().execute("CREATE TABLE TEST_ATOMIC (id TEXT PRIMARY KEY, value TEXT NOT NULL)");
            anchor.createStatement().execute("INSERT INTO TEST_ATOMIC (id, value) VALUES ('noop', 'A')");
            System.clearProperty("db.atomic.commit");
            System.clearProperty("db.continue.on.error");

            final AtomicTestRepository repository = new AtomicTestRepository(jdbcUrl, true);
            final int salvos = repository.salvar(List.of(new TestRecord("noop", "A", false, true)));

            assertEquals(1, salvos);
            assertEquals(1, contarRegistros(anchor));
        } finally {
            System.clearProperty("db.atomic.commit");
            System.clearProperty("db.continue.on.error");
        }
    }

    @Test
    void deveExecutarHooksDeStagingQuandoRepositorioSolicitar() throws Exception {
        final String jdbcUrl = "jdbc:sqlite:file:repo_staging_hooks?mode=memory&cache=shared";
        try (Connection anchor = DriverManager.getConnection(jdbcUrl)) {
            anchor.createStatement().execute("CREATE TABLE TEST_ATOMIC (id TEXT PRIMARY KEY, value TEXT NOT NULL)");
            System.clearProperty("db.atomic.commit");
            System.clearProperty("db.continue.on.error");

            final AtomicTestRepository repository = new AtomicTestRepository(jdbcUrl, false, true);
            final int salvos = repository.salvar(List.of(new TestRecord("ok", "A", false, false)));

            assertEquals(1, salvos);
            assertEquals(1, repository.prepararStagingChamadas);
            assertEquals(1, repository.promoverStagingChamadas);
        } finally {
            System.clearProperty("db.atomic.commit");
            System.clearProperty("db.continue.on.error");
        }
    }

    @Test
    void deveFazerRollbackIntegralQuandoPromocaoDeStagingFalhar() throws Exception {
        final String jdbcUrl = "jdbc:sqlite:file:repo_staging_rollback?mode=memory&cache=shared";
        try (Connection anchor = DriverManager.getConnection(jdbcUrl)) {
            anchor.createStatement().execute("CREATE TABLE TEST_ATOMIC (id TEXT PRIMARY KEY, value TEXT NOT NULL)");
            System.clearProperty("db.atomic.commit");
            System.clearProperty("db.continue.on.error");

            final AtomicTestRepository repository = new AtomicTestRepository(
                jdbcUrl,
                false,
                true,
                true,
                true
            );

            assertThrows(SQLException.class, () -> repository.salvar(List.of(new TestRecord("ok", "A", false, false))));
            assertEquals(0, contarRegistros(anchor));
        } finally {
            System.clearProperty("db.atomic.commit");
            System.clearProperty("db.continue.on.error");
        }
    }

    private int contarRegistros(final Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM TEST_ATOMIC");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private record TestRecord(String id, String value, boolean fail, boolean noOp) {}

    private static final class AtomicTestRepository extends AbstractRepository<TestRecord> {
        private final String jdbcUrl;
        private final boolean aceitarNoOpComoSucesso;
        private final boolean usarStaging;
        private final boolean stagingReal;
        private final boolean falharNaPromocao;
        private int prepararStagingChamadas;
        private int promoverStagingChamadas;

        private AtomicTestRepository(final String jdbcUrl) {
            this(jdbcUrl, false, false, false, false);
        }

        private AtomicTestRepository(final String jdbcUrl, final boolean aceitarNoOpComoSucesso) {
            this(jdbcUrl, aceitarNoOpComoSucesso, false, false, false);
        }

        private AtomicTestRepository(final String jdbcUrl,
                                     final boolean aceitarNoOpComoSucesso,
                                     final boolean usarStaging) {
            this(jdbcUrl, aceitarNoOpComoSucesso, usarStaging, false, false);
        }

        private AtomicTestRepository(final String jdbcUrl,
                                     final boolean aceitarNoOpComoSucesso,
                                     final boolean usarStaging,
                                     final boolean stagingReal,
                                     final boolean falharNaPromocao) {
            this.jdbcUrl = jdbcUrl;
            this.aceitarNoOpComoSucesso = aceitarNoOpComoSucesso;
            this.usarStaging = usarStaging;
            this.stagingReal = stagingReal;
            this.falharNaPromocao = falharNaPromocao;
        }

        @Override
        protected Connection obterConexao() throws SQLException {
            return DriverManager.getConnection(jdbcUrl);
        }

        @Override
        protected int executarMerge(final Connection conexao, final TestRecord entidade) throws SQLException {
            if (entidade.noOp()) {
                return 0;
            }
            if (entidade.fail()) {
                throw new SQLException("falha forçada");
            }
            try (PreparedStatement ps = conexao.prepareStatement(
                "INSERT INTO TEST_ATOMIC (id, value) VALUES (?, ?) ON CONFLICT(id) DO UPDATE SET value = excluded.value"
            )) {
                ps.setString(1, entidade.id());
                ps.setString(2, entidade.value());
                return ps.executeUpdate();
            }
        }

        @Override
        protected String getNomeTabela() {
            return "TEST_ATOMIC";
        }

        @Override
        protected boolean aceitarMergeSemAlteracoesComoSucesso(final TestRecord entidade) {
            return aceitarNoOpComoSucesso;
        }

        @Override
        protected boolean usarStagingPorExecucao() {
            return usarStaging;
        }

        @Override
        protected void prepararStagingPorExecucao(final Connection conexao) throws SQLException {
            prepararStagingChamadas++;
            if (!stagingReal) {
                return;
            }
            try (PreparedStatement create = conexao.prepareStatement(
                "CREATE TABLE IF NOT EXISTS TEST_ATOMIC_STAGING (id TEXT PRIMARY KEY, value TEXT NOT NULL)"
            )) {
                create.executeUpdate();
            }
            try (PreparedStatement clear = conexao.prepareStatement("DELETE FROM TEST_ATOMIC_STAGING")) {
                clear.executeUpdate();
            }
        }

        @Override
        protected int executarMergeNoDestinoDaExecucao(final Connection conexao, final TestRecord entidade) throws SQLException {
            if (!stagingReal) {
                return super.executarMergeNoDestinoDaExecucao(conexao, entidade);
            }
            if (entidade.noOp()) {
                return 0;
            }
            if (entidade.fail()) {
                throw new SQLException("falha forçada");
            }
            try (PreparedStatement ps = conexao.prepareStatement(
                "INSERT INTO TEST_ATOMIC_STAGING (id, value) VALUES (?, ?) " +
                    "ON CONFLICT(id) DO UPDATE SET value = excluded.value"
            )) {
                ps.setString(1, entidade.id());
                ps.setString(2, entidade.value());
                return ps.executeUpdate();
            }
        }

        @Override
        protected int promoverStagingPorExecucao(final Connection conexao) throws SQLException {
            promoverStagingChamadas++;
            if (!stagingReal) {
                return 0;
            }
            try (PreparedStatement promote = conexao.prepareStatement(
                "INSERT INTO TEST_ATOMIC (id, value) SELECT id, value FROM TEST_ATOMIC_STAGING " +
                    "ON CONFLICT(id) DO UPDATE SET value = excluded.value"
            )) {
                final int rows = promote.executeUpdate();
                if (falharNaPromocao) {
                    throw new SQLException("falha na promocao");
                }
                return rows;
            }
        }

        @Override
        protected void verificarTabelaExisteOuLancarErro(final Connection conexao) {
            // SQLite de teste usa schema efemero controlado pelo teste.
        }
    }
}
