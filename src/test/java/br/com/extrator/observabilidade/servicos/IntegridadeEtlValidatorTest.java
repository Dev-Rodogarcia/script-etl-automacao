package br.com.extrator.observabilidade.servicos;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import br.com.extrator.aplicacao.portas.ExecutionAuditPort;
import br.com.extrator.plataforma.auditoria.dominio.ExecutionAuditRecord;
import br.com.extrator.suporte.validacao.ConstantesEntidades;

class IntegridadeEtlValidatorTest {

    @Test
    void deveFalharQuandoManifestoReferenciarColetaInexistente() throws Exception {
        try (Connection conexao = DriverManager.getConnection("jdbc:sqlite:file:int_manifestos?mode=memory&cache=shared")) {
            conexao.createStatement().execute("ATTACH DATABASE ':memory:' AS dbo");
            conexao.createStatement().execute("CREATE TABLE dbo.manifestos (pick_sequence_code INTEGER, data_extracao TIMESTAMP)");
            conexao.createStatement().execute("CREATE TABLE dbo.coletas (sequence_code INTEGER)");
            inserirDataExtracao(conexao, "INSERT INTO dbo.manifestos (pick_sequence_code, data_extracao) VALUES (?, ?)", 999L);

            final IntegridadeEtlValidator validator = new IntegridadeEtlValidator(
                new StubExecutionAuditPort(),
                new SqliteIntegridadeEtlSqlSupport()
            );
            final List<String> falhas = new ArrayList<>();

            final Method method = IntegridadeEtlValidator.class.getDeclaredMethod(
                "validarReferencialManifestos",
                Connection.class,
                LocalDateTime.class,
                LocalDateTime.class,
                String.class,
                Set.class,
                List.class,
                boolean.class
            );
            method.setAccessible(true);
            method.invoke(
                validator,
                conexao,
                LocalDateTime.of(2026, 4, 1, 0, 0),
                LocalDateTime.of(2026, 4, 1, 23, 59),
                "exec-1",
                Set.of(ConstantesEntidades.MANIFESTOS, ConstantesEntidades.COLETAS),
                falhas,
                false
            );

            assertTrue(falhas.stream().anyMatch(falha -> falha.contains("INTEGRIDADE_REFERENCIAL_MANIFESTOS")));
        }
    }

    @Test
    void deveFalharQuandoFreteReferenciarFaturaGraphqlInexistente() throws Exception {
        try (Connection conexao = DriverManager.getConnection("jdbc:sqlite:file:int_fretes?mode=memory&cache=shared")) {
            conexao.createStatement().execute("ATTACH DATABASE ':memory:' AS dbo");
            conexao.createStatement().execute("CREATE TABLE dbo.fretes (accounting_credit_id INTEGER, data_extracao TIMESTAMP)");
            conexao.createStatement().execute("CREATE TABLE dbo.faturas_graphql (id INTEGER)");
            inserirDataExtracao(conexao, "INSERT INTO dbo.fretes (accounting_credit_id, data_extracao) VALUES (?, ?)", 321L);

            final IntegridadeEtlValidator validator = new IntegridadeEtlValidator(
                new StubExecutionAuditPort(),
                new SqliteIntegridadeEtlSqlSupport()
            );
            final List<String> falhas = new ArrayList<>();

            final Method method = IntegridadeEtlValidator.class.getDeclaredMethod(
                "validarReferencialFretes",
                Connection.class,
                LocalDateTime.class,
                LocalDateTime.class,
                Set.class,
                List.class
            );
            method.setAccessible(true);
            method.invoke(
                validator,
                conexao,
                LocalDateTime.of(2026, 4, 1, 0, 0),
                LocalDateTime.of(2026, 4, 1, 23, 59),
                Set.of(ConstantesEntidades.FRETES, ConstantesEntidades.FATURAS_GRAPHQL),
                falhas
            );

            assertTrue(falhas.stream().anyMatch(falha -> falha.contains("INTEGRIDADE_REFERENCIAL_FRETES")));
        }
    }

    private void inserirDataExtracao(final Connection conexao, final String sql, final Long valor) throws Exception {
        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setLong(1, valor);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.of(2026, 4, 1, 10, 0)));
            ps.executeUpdate();
        }
    }

    private static final class StubExecutionAuditPort implements ExecutionAuditPort {
        @Override
        public void registrarResultado(final ExecutionAuditRecord record) {
            // no-op
        }

        @Override
        public Optional<ExecutionAuditRecord> buscarResultado(final String executionUuid, final String entidade) {
            return Optional.of(new ExecutionAuditRecord(
                executionUuid,
                entidade,
                LocalDateTime.of(2026, 4, 1, 0, 0),
                LocalDateTime.of(2026, 4, 1, 23, 59),
                LocalDateTime.of(2026, 4, 1, 0, 0),
                LocalDateTime.of(2026, 4, 1, 23, 59),
                "COMPLETO",
                1,
                1,
                1,
                1,
                true,
                null,
                1,
                0,
                0,
                LocalDateTime.of(2026, 4, 1, 0, 0),
                LocalDateTime.of(2026, 4, 1, 23, 59),
                "--validar",
                "cycle-1",
                null
            ));
        }

        @Override
        public List<ExecutionAuditRecord> listarResultados(final String executionUuid) {
            return List.of();
        }

        @Override
        public Optional<LocalDateTime> buscarWatermarkConfirmado(final String entidade) {
            return Optional.empty();
        }

        @Override
        public void atualizarWatermarkConfirmado(final String entidade, final LocalDateTime watermarkConfirmado) {
            // no-op
        }

        @Override
        public boolean isDisponivel() {
            return true;
        }
    }

    private static final class SqliteIntegridadeEtlSqlSupport extends IntegridadeEtlSqlSupport {
        @Override
        List<Long> executarListaLong(final Connection conexao,
                                     final String sql,
                                     final LocalDateTime inicioExecucao,
                                     final LocalDateTime fimExecucao) throws SQLException {
            final String sqlCompativel = sql.replace("SELECT TOP 10", "SELECT");
            final List<Long> valores = new ArrayList<>();
            try (PreparedStatement stmt = conexao.prepareStatement(sqlCompativel)) {
                stmt.setTimestamp(1, Timestamp.valueOf(inicioExecucao));
                stmt.setTimestamp(2, Timestamp.valueOf(fimExecucao));
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        final long valor = rs.getLong(1);
                        if (!rs.wasNull()) {
                            valores.add(valor);
                        }
                    }
                }
            }
            return valores;
        }
    }
}
