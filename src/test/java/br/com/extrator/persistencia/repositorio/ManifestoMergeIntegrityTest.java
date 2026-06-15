package br.com.extrator.persistencia.repositorio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import br.com.extrator.persistencia.entidade.ManifestoEntity;
import br.com.extrator.suporte.configuracao.ConfigBanco;

class ManifestoMergeIntegrityTest {

    @Test
    void deveManterManifestoIdempotentePorChaveLogicaEstritaNoMerge() throws SQLException {
        final long seed = Math.abs(System.nanoTime() % 100_000_000L);
        final long sequenceCode = 900_000_000_000L + seed;
        final long pickSequenceCode = 901_000_000_000L + seed;
        final int mdfeNumber = 70_000 + (int) (seed % 10_000);
        final OffsetDateTime baseTime = OffsetDateTime.of(2026, 6, 1, 10, 0, 0, 0, ZoneOffset.UTC);

        try (Connection conexao = abrirConexao()) {
            conexao.setAutoCommit(false);

            try {
                garantirColunasGovernanca(conexao);
                inserirColetaPai(conexao, pickSequenceCode);

                final TestableManifestoRepository repository = new TestableManifestoRepository();
                repository.prepararStaging(conexao);

                final ManifestoEntity primeiro = manifesto(
                    sequenceCode,
                    pickSequenceCode,
                    mdfeNumber,
                    "manifesto-primeiro-" + seed,
                    "em_aberto",
                    "FILIAL-A",
                    new BigDecimal("10.00"),
                    "{\"versao\":1}",
                    baseTime
                );
                final ManifestoEntity segundo = manifesto(
                    sequenceCode,
                    pickSequenceCode,
                    mdfeNumber,
                    "manifesto-segundo-" + seed,
                    "encerrado",
                    "FILIAL-B",
                    new BigDecimal("25.50"),
                    "{\"versao\":2}",
                    baseTime.plusMinutes(5)
                );

                assertEquals(1, repository.mergeNoStaging(conexao, primeiro));
                assertEquals(1, repository.mergeNoStaging(conexao, segundo));
                assertEquals(1, repository.promoverStaging(conexao));

                assertEquals(1L, contarManifestos(conexao, sequenceCode, pickSequenceCode, mdfeNumber));
                assertManifestoAtualizado(conexao, sequenceCode, pickSequenceCode, mdfeNumber, segundo);
            } finally {
                conexao.rollback();
            }
        }
    }

    private static Connection abrirConexao() throws SQLException {
        return DriverManager.getConnection(
            ConfigBanco.obterUrlBancoDados(),
            ConfigBanco.obterUsuarioBancoDados(),
            ConfigBanco.obterSenhaBancoDados()
        );
    }

    private static void inserirColetaPai(final Connection conexao, final long pickSequenceCode) throws SQLException {
        final String sql = """
            INSERT INTO dbo.coletas (
                id,
                sequence_code,
                request_date,
                status,
                metadata,
                data_extracao,
                excluido_na_origem
            )
            VALUES (?, ?, CAST(SYSUTCDATETIME() AS date), N'integration-test', N'{}', SYSUTCDATETIME(), 0)
            """;
        try (PreparedStatement statement = conexao.prepareStatement(sql)) {
            statement.setString(1, "it-mf-" + pickSequenceCode);
            statement.setLong(2, pickSequenceCode);
            statement.executeUpdate();
        }
    }

    private static void garantirColunasGovernanca(final Connection conexao) throws SQLException {
        final String sql = """
            IF COL_LENGTH(N'dbo.manifestos', N'driver_contract_type') IS NULL
                ALTER TABLE dbo.manifestos ADD driver_contract_type NVARCHAR(50) NULL;
            IF COL_LENGTH(N'dbo.manifestos', N'data_exclusao_origem') IS NULL
                ALTER TABLE dbo.manifestos ADD data_exclusao_origem DATETIME2(0) NULL;
            """;
        try (Statement statement = conexao.createStatement()) {
            statement.execute(sql);
        }
    }

    private static ManifestoEntity manifesto(final long sequenceCode,
                                             final long pickSequenceCode,
                                             final int mdfeNumber,
                                             final String identificadorUnico,
                                             final String status,
                                             final String branchNickname,
                                             final BigDecimal totalCost,
                                             final String metadata,
                                             final OffsetDateTime createdAt) {
        final ManifestoEntity manifesto = new ManifestoEntity();
        manifesto.setSequenceCode(sequenceCode);
        manifesto.setPickSequenceCode(pickSequenceCode);
        manifesto.setMdfeNumber(mdfeNumber);
        manifesto.setIdentificadorUnico(identificadorUnico);
        manifesto.setStatus(status);
        manifesto.setBranchNickname(branchNickname);
        manifesto.setTotalCost(totalCost);
        manifesto.setMetadata(metadata);
        manifesto.setCreatedAt(createdAt);
        return manifesto;
    }

    private static long contarManifestos(final Connection conexao,
                                         final long sequenceCode,
                                         final long pickSequenceCode,
                                         final int mdfeNumber) throws SQLException {
        final String sql = """
            SELECT COUNT_BIG(*)
              FROM dbo.manifestos
             WHERE sequence_code = ?
               AND COALESCE(pick_sequence_code, CONVERT(BIGINT, -1)) = ?
               AND COALESCE(mdfe_number, -1) = ?
            """;
        try (PreparedStatement statement = conexao.prepareStatement(sql)) {
            statement.setLong(1, sequenceCode);
            statement.setLong(2, pickSequenceCode);
            statement.setInt(3, mdfeNumber);
            try (ResultSet rs = statement.executeQuery()) {
                assertTrue(rs.next());
                return rs.getLong(1);
            }
        }
    }

    private static void assertManifestoAtualizado(final Connection conexao,
                                                  final long sequenceCode,
                                                  final long pickSequenceCode,
                                                  final int mdfeNumber,
                                                  final ManifestoEntity esperado) throws SQLException {
        final String sql = """
            SELECT status, branch_nickname, total_cost, identificador_unico, metadata
              FROM dbo.manifestos
             WHERE sequence_code = ?
               AND COALESCE(pick_sequence_code, CONVERT(BIGINT, -1)) = ?
               AND COALESCE(mdfe_number, -1) = ?
            """;
        try (PreparedStatement statement = conexao.prepareStatement(sql)) {
            statement.setLong(1, sequenceCode);
            statement.setLong(2, pickSequenceCode);
            statement.setInt(3, mdfeNumber);
            try (ResultSet rs = statement.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(esperado.getStatus(), rs.getString("status"));
                assertEquals(esperado.getBranchNickname(), rs.getString("branch_nickname"));
                assertEquals(0, esperado.getTotalCost().compareTo(rs.getBigDecimal("total_cost")));
                assertEquals(esperado.getIdentificadorUnico(), rs.getString("identificador_unico"));
                assertEquals(esperado.getMetadata(), rs.getString("metadata"));
            }
        }
    }

    private static final class TestableManifestoRepository extends ManifestoRepository {
        private void prepararStaging(final Connection conexao) throws SQLException {
            prepararStagingPorExecucao(conexao);
        }

        private int mergeNoStaging(final Connection conexao, final ManifestoEntity manifesto) throws SQLException {
            return executarMergeNoDestinoDaExecucao(conexao, manifesto);
        }

        private int promoverStaging(final Connection conexao) throws SQLException {
            return promoverStagingPorExecucao(conexao);
        }
    }
}
