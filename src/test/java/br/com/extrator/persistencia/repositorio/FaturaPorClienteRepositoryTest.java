package br.com.extrator.persistencia.repositorio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.extrator.persistencia.entidade.FaturaPorClienteEntity;

class FaturaPorClienteRepositoryTest {

    @Test
    void deveReconciliarAliasLegadoAntesDoMergeFinal() throws Exception {
        try (Connection conexao = DriverManager.getConnection("jdbc:sqlite:file:fpc_alias?mode=memory&cache=shared")) {
            conexao.createStatement().execute("ATTACH DATABASE ':memory:' AS dbo");
            conexao.createStatement().execute("""
                CREATE TABLE dbo.faturas_por_cliente (
                    unique_id TEXT PRIMARY KEY,
                    metadata TEXT
                )
            """);
            conexao.createStatement().execute("INSERT INTO dbo.faturas_por_cliente (unique_id, metadata) VALUES ('NFSE-123456', '{}')");

            final FaturaPorClienteEntity entity = new FaturaPorClienteEntity();
            entity.setUniqueId("FPC-HASH-ABC");
            entity.setLegacyUniqueIds(List.of("NFSE-123456"));

            final FaturaPorClienteRepository repository = new FaturaPorClienteRepository();
            final Method method = FaturaPorClienteRepository.class.getDeclaredMethod(
                "reconciliarAliasLegado",
                Connection.class,
                FaturaPorClienteEntity.class
            );
            method.setAccessible(true);
            method.invoke(repository, conexao, entity);

            assertEquals(List.of("FPC-HASH-ABC"), listarUniqueIds(conexao));
        }
    }

    @Test
    void deveRemoverAliasesDuplicadosAposPromoverUniqueIdCanonico() throws Exception {
        try (Connection conexao = DriverManager.getConnection("jdbc:sqlite:file:fpc_alias_dup?mode=memory&cache=shared")) {
            conexao.createStatement().execute("ATTACH DATABASE ':memory:' AS dbo");
            conexao.createStatement().execute("""
                CREATE TABLE dbo.faturas_por_cliente (
                    unique_id TEXT PRIMARY KEY,
                    metadata TEXT
                )
            """);
            conexao.createStatement().execute("INSERT INTO dbo.faturas_por_cliente (unique_id, metadata) VALUES ('FPC-HASH-ABC', '{}')");
            conexao.createStatement().execute("INSERT INTO dbo.faturas_por_cliente (unique_id, metadata) VALUES ('BILLING-9988', '{}')");

            final FaturaPorClienteEntity entity = new FaturaPorClienteEntity();
            entity.setUniqueId("FPC-HASH-ABC");
            entity.setLegacyUniqueIds(List.of("BILLING-9988"));

            final FaturaPorClienteRepository repository = new FaturaPorClienteRepository();
            final Method method = FaturaPorClienteRepository.class.getDeclaredMethod(
                "reconciliarAliasLegado",
                Connection.class,
                FaturaPorClienteEntity.class
            );
            method.setAccessible(true);
            method.invoke(repository, conexao, entity);

            assertEquals(List.of("FPC-HASH-ABC"), listarUniqueIds(conexao));
        }
    }

    private List<String> listarUniqueIds(final Connection conexao) throws Exception {
        try (PreparedStatement ps = conexao.prepareStatement(
            "SELECT unique_id FROM dbo.faturas_por_cliente ORDER BY unique_id"
        ); ResultSet rs = ps.executeQuery()) {
            final java.util.ArrayList<String> ids = new java.util.ArrayList<>();
            while (rs.next()) {
                ids.add(rs.getString(1));
            }
            return List.copyOf(ids);
        }
    }
}
