/* ==[DOC-FILE]===============================================================
Arquivo : src/main/java/br/com/extrator/db/repository/PageAuditRepository.java
Classe  : PageAuditRepository (class)
Pacote  : br.com.extrator.persistencia.repositorio
Modulo  : Repositorio de dados
Papel   : Implementa responsabilidade de page audit repository.

Conecta com:
- PageAuditEntity (db.entity)
- GerenciadorConexao (util.banco)

Fluxo geral:
1) Monta comandos SQL e parametros.
2) Executa operacoes de persistencia/consulta no banco.
3) Converte resultado para entidades de dominio.

Estrutura interna:
Metodos principais:
- inserir(...1 args): inclui registros no destino configurado.
Atributos-chave:
- logger: logger da classe para diagnostico.
[DOC-FILE-END]============================================================== */

package br.com.extrator.persistencia.repositorio;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.extrator.persistencia.entidade.PageAuditEntity;
import br.com.extrator.suporte.banco.GerenciadorConexao;

/**
 * Repository para persistência de auditoria de páginas (PageAudit) no banco de dados.
 * 
 * ⚠️ NOTA: Este repositório não estende AbstractRepository porque PageAudit
 * tem uma operação específica de UPSERT idempotente por (run_uuid, template_id, page)
 * e não segue o padrão de outras entidades. Mantém uso direto de GerenciadorConexao
 * para consistência com LogExtracaoRepository.
 */
public class PageAuditRepository {
    private static final Logger logger = LoggerFactory.getLogger(PageAuditRepository.class);
    private static final String SQL_INSERT =
        """
        INSERT INTO %s(\
        execution_uuid, run_uuid, template_id, page, per, janela_inicio, janela_fim, \
        req_hash, resp_hash, total_itens, id_key, id_min_num, id_max_num, \
        id_min_str, id_max_str, status_code, duracao_ms) \
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""";
    private static final String SQL_UPDATE =
        """
        UPDATE %s
           SET execution_uuid = ?,
               per = ?,
               janela_inicio = ?,
               janela_fim = ?,
               req_hash = ?,
               resp_hash = ?,
               total_itens = ?,
               id_key = ?,
               id_min_num = ?,
               id_max_num = ?,
               id_min_str = ?,
               id_max_str = ?,
               status_code = ?,
               duracao_ms = ?
         WHERE run_uuid = ?
           AND template_id = ?
           AND page = ?""";

    /**
     * Insere um novo registro de auditoria de página.
     * 
     * @param a Entidade PageAuditEntity a ser inserida
     * @throws RuntimeException Se ocorrer erro ao inserir
     */
    public void inserir(final PageAuditEntity a) {
        if (a == null) {
            logger.warn("⚠️ Tentativa de inserir PageAuditEntity NULL");
            throw new IllegalArgumentException("PageAuditEntity não pode ser null");
        }
        
        try (Connection conn = obterConexao()) {
            final int rowsUpdated = atualizarExistente(conn, a);
            if (rowsUpdated > 0) {
                logger.debug(
                    "♻️ PageAudit atualizado idempotentemente: run_uuid={}, template_id={}, page={}",
                    a.getRunUuid(),
                    a.getTemplateId(),
                    a.getPage()
                );
                return;
            }

            final int rowsInserted = inserirNovo(conn, a);
            if (rowsInserted > 0) {
                logger.debug(
                    "✅ PageAudit inserido: execution_uuid={}, template_id={}, page={}",
                    a.getExecutionUuid(),
                    a.getTemplateId(),
                    a.getPage()
                );
                return;
            }

            logger.warn(
                "⚠️ UPSERT não afetou nenhuma linha para PageAudit: run_uuid={}, template_id={}, page={}",
                a.getRunUuid(),
                a.getTemplateId(),
                a.getPage()
            );
        } catch (final SQLException e) {
            logger.error(
                "❌ Erro ao gravar PageAudit: run_uuid={}, template_id={}, page={} - {}",
                a.getRunUuid(),
                a.getTemplateId(),
                a.getPage(),
                e.getMessage(),
                e
            );
            throw new RuntimeException("Falha ao gravar page_audit", e);
        }
    }

    protected Connection obterConexao() throws SQLException {
        return GerenciadorConexao.obterConexao();
    }

    protected String getNomeTabela() {
        return "dbo.page_audit";
    }

    private int atualizarExistente(final Connection conn, final PageAuditEntity a) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE.formatted(getNomeTabela()))) {
            stmt.setString(1, a.getExecutionUuid());
            stmt.setInt(2, a.getPer());
            stmt.setObject(3, a.getJanelaInicio());
            stmt.setObject(4, a.getJanelaFim());
            stmt.setString(5, a.getReqHash());
            stmt.setString(6, a.getRespHash());
            stmt.setInt(7, a.getTotalItens());
            stmt.setString(8, a.getIdKey());
            stmt.setObject(9, a.getIdMinNum());
            stmt.setObject(10, a.getIdMaxNum());
            stmt.setString(11, a.getIdMinStr());
            stmt.setString(12, a.getIdMaxStr());
            stmt.setInt(13, a.getStatusCode());
            stmt.setInt(14, a.getDuracaoMs());
            stmt.setString(15, a.getRunUuid());
            stmt.setInt(16, a.getTemplateId());
            stmt.setInt(17, a.getPage());
            return stmt.executeUpdate();
        }
    }

    private int inserirNovo(final Connection conn, final PageAuditEntity a) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERT.formatted(getNomeTabela()))) {
            stmt.setString(1, a.getExecutionUuid());
            stmt.setString(2, a.getRunUuid());
            stmt.setInt(3, a.getTemplateId());
            stmt.setInt(4, a.getPage());
            stmt.setInt(5, a.getPer());
            stmt.setObject(6, a.getJanelaInicio());
            stmt.setObject(7, a.getJanelaFim());
            stmt.setString(8, a.getReqHash());
            stmt.setString(9, a.getRespHash());
            stmt.setInt(10, a.getTotalItens());
            stmt.setString(11, a.getIdKey());
            stmt.setObject(12, a.getIdMinNum());
            stmt.setObject(13, a.getIdMaxNum());
            stmt.setString(14, a.getIdMinStr());
            stmt.setString(15, a.getIdMaxStr());
            stmt.setInt(16, a.getStatusCode());
            stmt.setInt(17, a.getDuracaoMs());
            return stmt.executeUpdate();
        } catch (final SQLException e) {
            if (isViolacaoDeChaveDuplicada(e)) {
                logger.warn(
                    "⚠️ Colisão de page_audit detectada, aplicando atualização idempotente: run_uuid={}, template_id={}, page={}",
                    a.getRunUuid(),
                    a.getTemplateId(),
                    a.getPage()
                );
                return atualizarExistente(conn, a);
            }
            throw e;
        }
    }

    private boolean isViolacaoDeChaveDuplicada(final SQLException e) {
        final String sqlState = e.getSQLState();
        if (sqlState != null && sqlState.startsWith("23")) {
            return true;
        }

        final int errorCode = e.getErrorCode();
        if (errorCode == 2601 || errorCode == 2627) {
            return true;
        }

        final String message = e.getMessage();
        if (message == null) {
            return false;
        }

        final String normalized = message.toLowerCase();
        return normalized.contains("duplicate")
            || normalized.contains("duplicada")
            || normalized.contains("unique")
            || normalized.contains("constraint");
    }
}
