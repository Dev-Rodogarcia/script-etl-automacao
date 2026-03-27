package br.com.extrator.aplicacao.contexto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import br.com.extrator.aplicacao.portas.ExecutionAuditPort;
import br.com.extrator.plataforma.auditoria.dominio.ExecutionAuditRecord;

final class NoOpExecutionAuditPort implements ExecutionAuditPort {

    @Override
    public void registrarResultado(final ExecutionAuditRecord record) {
        // no-op
    }

    @Override
    public Optional<ExecutionAuditRecord> buscarResultado(final String executionUuid, final String entidade) {
        return Optional.empty();
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
        return false;
    }
}
