package br.com.extrator.aplicacao.expurgo;

import java.util.Set;

public record SourceKeySnapshot(
    Set<String> keys,
    boolean complete,
    int pagesProcessed,
    int rowsRead,
    int skippedRows
) {
    public SourceKeySnapshot {
        keys = keys == null ? Set.of() : Set.copyOf(keys);
    }
}
