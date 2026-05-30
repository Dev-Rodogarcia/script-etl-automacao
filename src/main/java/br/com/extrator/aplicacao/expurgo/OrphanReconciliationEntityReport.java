package br.com.extrator.aplicacao.expurgo;

public record OrphanReconciliationEntityReport(
    String entityName,
    int apiKeyCount,
    int dbActiveKeyCount,
    int orphanCount,
    int updatedCount,
    int pagesProcessed,
    int rowsRead,
    int skippedRows,
    boolean dryRun
) {
}
