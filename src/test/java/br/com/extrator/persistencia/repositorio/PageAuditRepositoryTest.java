package br.com.extrator.persistencia.repositorio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import br.com.extrator.persistencia.entidade.PageAuditEntity;

class PageAuditRepositoryTest {

    @Test
    void deveAtualizarRegistroExistenteQuandoMesmaPaginaForAuditadaNovamente() throws Exception {
        final String jdbcUrl = "jdbc:sqlite:file:page_audit_upsert?mode=memory&cache=shared";
        try (Connection anchor = DriverManager.getConnection(jdbcUrl)) {
            anchor.createStatement().execute("""
                CREATE TABLE page_audit (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    execution_uuid TEXT NOT NULL,
                    run_uuid TEXT NOT NULL,
                    template_id INTEGER NOT NULL,
                    page INTEGER NOT NULL,
                    per INTEGER NOT NULL,
                    janela_inicio TEXT NULL,
                    janela_fim TEXT NULL,
                    req_hash TEXT NOT NULL,
                    resp_hash TEXT NOT NULL,
                    total_itens INTEGER NOT NULL,
                    id_key TEXT NULL,
                    id_min_num INTEGER NULL,
                    id_max_num INTEGER NULL,
                    id_min_str TEXT NULL,
                    id_max_str TEXT NULL,
                    status_code INTEGER NOT NULL,
                    duracao_ms INTEGER NOT NULL
                )
            """);
            anchor.createStatement().execute("""
                CREATE UNIQUE INDEX ux_page_audit_run_template_page
                    ON page_audit (run_uuid, template_id, page)
            """);

            final PageAuditRepository repository = new TestPageAuditRepository(jdbcUrl);
            repository.inserir(criarAudit("exec-1", "run-1", 6399, 3, 100, "req-a", "resp-a", 100, 200, LocalDate.of(2026, 4, 13)));
            repository.inserir(criarAudit("exec-2", "run-1", 6399, 3, 100, "req-b", "resp-b", 0, 204, LocalDate.of(2026, 4, 14)));

            assertEquals(1, contarRegistros(anchor));

            try (PreparedStatement ps = anchor.prepareStatement("""
                SELECT execution_uuid, req_hash, resp_hash, total_itens, status_code, janela_inicio
                  FROM page_audit
                 WHERE run_uuid = ? AND template_id = ? AND page = ?
            """)) {
                ps.setString(1, "run-1");
                ps.setInt(2, 6399);
                ps.setInt(3, 3);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    assertEquals("exec-2", rs.getString("execution_uuid"));
                    assertEquals("req-b", rs.getString("req_hash"));
                    assertEquals("resp-b", rs.getString("resp_hash"));
                    assertEquals(0, rs.getInt("total_itens"));
                    assertEquals(204, rs.getInt("status_code"));
                    assertEquals("2026-04-14", rs.getString("janela_inicio"));
                }
            }
        }
    }

    @Test
    void deveManterPaginasDistintasComoLinhasSeparadas() throws Exception {
        final String jdbcUrl = "jdbc:sqlite:file:page_audit_multi?mode=memory&cache=shared";
        try (Connection anchor = DriverManager.getConnection(jdbcUrl)) {
            anchor.createStatement().execute("""
                CREATE TABLE page_audit (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    execution_uuid TEXT NOT NULL,
                    run_uuid TEXT NOT NULL,
                    template_id INTEGER NOT NULL,
                    page INTEGER NOT NULL,
                    per INTEGER NOT NULL,
                    janela_inicio TEXT NULL,
                    janela_fim TEXT NULL,
                    req_hash TEXT NOT NULL,
                    resp_hash TEXT NOT NULL,
                    total_itens INTEGER NOT NULL,
                    id_key TEXT NULL,
                    id_min_num INTEGER NULL,
                    id_max_num INTEGER NULL,
                    id_min_str TEXT NULL,
                    id_max_str TEXT NULL,
                    status_code INTEGER NOT NULL,
                    duracao_ms INTEGER NOT NULL
                )
            """);
            anchor.createStatement().execute("""
                CREATE UNIQUE INDEX ux_page_audit_run_template_page
                    ON page_audit (run_uuid, template_id, page)
            """);

            final PageAuditRepository repository = new TestPageAuditRepository(jdbcUrl);
            repository.inserir(criarAudit("exec-1", "run-1", 6399, 1, 100, "req-1", "resp-1", 100, 200, null));
            repository.inserir(criarAudit("exec-1", "run-1", 6399, 2, 100, "req-2", "resp-2", 100, 200, null));

            assertEquals(2, contarRegistros(anchor));
        }
    }

    private int contarRegistros(final Connection connection) throws Exception {
        try (PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM page_audit");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private PageAuditEntity criarAudit(final String executionUuid,
                                       final String runUuid,
                                       final int templateId,
                                       final int page,
                                       final int per,
                                       final String reqHash,
                                       final String respHash,
                                       final int totalItens,
                                       final int statusCode,
                                       final LocalDate janelaInicio) {
        final PageAuditEntity audit = new PageAuditEntity();
        audit.setExecutionUuid(executionUuid);
        audit.setRunUuid(runUuid);
        audit.setTemplateId(templateId);
        audit.setPage(page);
        audit.setPer(per);
        audit.setJanelaInicio(janelaInicio);
        audit.setJanelaFim(janelaInicio);
        audit.setReqHash(reqHash);
        audit.setRespHash(respHash);
        audit.setTotalItens(totalItens);
        audit.setIdKey("id");
        audit.setIdMinNum(1L);
        audit.setIdMaxNum((long) Math.max(1, totalItens));
        audit.setIdMinStr(null);
        audit.setIdMaxStr(null);
        audit.setStatusCode(statusCode);
        audit.setDuracaoMs(123);
        return audit;
    }

    private static final class TestPageAuditRepository extends PageAuditRepository {
        private final String jdbcUrl;

        private TestPageAuditRepository(final String jdbcUrl) {
            this.jdbcUrl = jdbcUrl;
        }

        @Override
        protected Connection obterConexao() throws java.sql.SQLException {
            return DriverManager.getConnection(jdbcUrl);
        }

        @Override
        protected String getNomeTabela() {
            return "page_audit";
        }
    }
}
