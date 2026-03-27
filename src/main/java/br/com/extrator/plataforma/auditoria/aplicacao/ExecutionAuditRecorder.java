package br.com.extrator.plataforma.auditoria.aplicacao;

import br.com.extrator.aplicacao.portas.ExecutionAuditPort;
import br.com.extrator.integracao.comum.ExtractionResult;
import br.com.extrator.plataforma.auditoria.dominio.ExecutionAuditRecord;
import br.com.extrator.suporte.observabilidade.ExecutionContext;

public final class ExecutionAuditRecorder {
    private ExecutionAuditRecorder() {
    }

    public static void registrar(final ExecutionAuditPort port, final ExtractionResult result) {
        if (port == null || result == null) {
            return;
        }

        final String executionUuid = ExecutionContext.currentExecutionId();
        if ("n/a".equalsIgnoreCase(executionUuid)) {
            return;
        }

        port.registrarResultado(new ExecutionAuditRecord(
            executionUuid,
            result.getEntityName(),
            result.getJanelaConsultaInicio(),
            result.getJanelaConsultaFim(),
            result.getJanelaConfirmacaoInicio(),
            result.getJanelaConfirmacaoFim(),
            result.getStatus(),
            result.getRegistrosExtraidos(),
            result.getTotalUnicos() > 0 ? result.getTotalUnicos() : result.getRegistrosExtraidos(),
            result.getRegistrosSalvos(),
            result.getRegistrosPersistidos(),
            result.isApiCompleta(),
            result.getMotivoIncompletude(),
            result.getPaginasProcessadas(),
            result.getRegistrosNoOpIdempotente(),
            result.getRegistrosInvalidos(),
            result.getInicio(),
            result.getFim(),
            ExecutionContext.currentCommand(),
            ExecutionContext.currentCycleId(),
            result.isSucesso() ? result.toLogEntity().getMensagem() : result.getMotivoIncompletude()
        ));
    }
}
