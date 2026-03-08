package br.com.extrator.observabilidade.quality;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DataQualityReport {
    private final List<DataQualityCheckResult> results;

    public DataQualityReport(final List<DataQualityCheckResult> results) {
        this.results = Collections.unmodifiableList(new ArrayList<>(results));
    }

    public List<DataQualityCheckResult> obterResultados() {
        return results;
    }

    public boolean isAprovado() {
        return results.stream().allMatch(DataQualityCheckResult::isAprovado);
    }

    public long totalFalhas() {
        return results.stream().filter(r -> !r.isAprovado()).count();
    }
}


