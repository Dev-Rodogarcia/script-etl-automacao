package br.com.extrator.aplicacao.expurgo;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public record OrphanReconciliationRequest(
    LocalDate dataInicio,
    LocalDate dataFim,
    List<EntityReconciliationSpec> specs,
    boolean dryRun,
    int batchSize
) {
    public OrphanReconciliationRequest {
        dataInicio = Objects.requireNonNull(dataInicio, "dataInicio");
        dataFim = Objects.requireNonNull(dataFim, "dataFim");
        if (dataFim.isBefore(dataInicio)) {
            throw new IllegalArgumentException("dataFim nao pode ser anterior a dataInicio");
        }
        specs = specs == null ? List.of() : List.copyOf(specs);
        if (specs.isEmpty()) {
            throw new IllegalArgumentException("Ao menos uma entidade deve ser informada para reconciliacao");
        }
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize deve ser maior que zero");
        }
    }
}
