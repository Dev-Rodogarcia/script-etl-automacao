package br.com.extrator.aplicacao.portas;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import br.com.extrator.plataforma.auditoria.dominio.ExecutionAuditRecord;

/**
 * Porta para auditoria estruturada da execucao corrente.
 */
public interface ExecutionAuditPort {

    void registrarResultado(ExecutionAuditRecord record);

    Optional<ExecutionAuditRecord> buscarResultado(String executionUuid, String entidade);

    List<ExecutionAuditRecord> listarResultados(String executionUuid);

    Optional<LocalDateTime> buscarWatermarkConfirmado(String entidade);

    void atualizarWatermarkConfirmado(String entidade, LocalDateTime watermarkConfirmado);

    default boolean isDisponivel() {
        return true;
    }
}
