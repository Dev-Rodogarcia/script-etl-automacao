package br.com.extrator.aplicacao.extracao;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface RecoveryReplayGate {
    enum StartResult {
        STARTED,
        ALREADY_RUNNING,
        ALREADY_COMPLETED
    }

    record ReplayAttempt(
        String idempotencyKey,
        String api,
        String entidade,
        LocalDate dataInicio,
        LocalDate dataFim,
        String modo,
        String executionUuid
    ) {
    }

    StartResult tryStart(ReplayAttempt attempt, LocalDateTime now) throws Exception;

    void markCompleted(String idempotencyKey, String executionUuid, LocalDateTime finishedAt) throws Exception;

    void markFailed(String idempotencyKey, String executionUuid, LocalDateTime finishedAt, String errorMessage) throws Exception;
}
