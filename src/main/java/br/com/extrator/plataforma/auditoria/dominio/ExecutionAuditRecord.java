package br.com.extrator.plataforma.auditoria.dominio;

import java.time.LocalDateTime;

/**
 * Registro estruturado de auditoria por entidade e execucao.
 */
public record ExecutionAuditRecord(
    String executionUuid,
    String entidade,
    LocalDateTime janelaConsultaInicio,
    LocalDateTime janelaConsultaFim,
    LocalDateTime janelaConfirmacaoInicio,
    LocalDateTime janelaConfirmacaoFim,
    String statusExecucao,
    int apiTotalBruto,
    int apiTotalUnico,
    int dbUpserts,
    int dbPersistidos,
    boolean apiCompleta,
    String motivoIncompletude,
    int paginasProcessadas,
    int noopCount,
    int invalidCount,
    LocalDateTime startedAt,
    LocalDateTime finishedAt,
    String commandName,
    String cycleId,
    String detalhe
) {
    public boolean isStatusCompleto() {
        return "COMPLETO".equalsIgnoreCase(statusExecucao);
    }
}
