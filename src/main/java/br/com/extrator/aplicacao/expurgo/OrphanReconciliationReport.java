package br.com.extrator.aplicacao.expurgo;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

public record OrphanReconciliationReport(
    String runId,
    LocalDate dataInicio,
    LocalDate dataFim,
    boolean dryRun,
    List<OrphanReconciliationEntityReport> entities,
    Duration duration
) {
    public OrphanReconciliationReport {
        entities = entities == null ? List.of() : List.copyOf(entities);
    }

    public int totalOrphans() {
        return entities.stream().mapToInt(OrphanReconciliationEntityReport::orphanCount).sum();
    }

    public int totalUpdated() {
        return entities.stream().mapToInt(OrphanReconciliationEntityReport::updatedCount).sum();
    }
}
