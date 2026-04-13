package br.com.extrator.persistencia.repositorio;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Avalia se uma reconciliacao destrutiva de fretes pode prosseguir com base no historico recente.
 */
public final class FretePruneGuardrailEvaluator {
    public enum BlockReason {
        ZERO_WITH_BASELINE,
        SHARP_DROP
    }

    public record Decision(
        boolean allowDeletion,
        BlockReason blockReason,
        int baselineMedian,
        double currentToBaselineRatio
    ) {
    }

    private final double minimumRatio;
    private final int minimumBaselineRecords;

    public FretePruneGuardrailEvaluator(final double minimumRatio, final int minimumBaselineRecords) {
        this.minimumRatio = minimumRatio;
        this.minimumBaselineRecords = minimumBaselineRecords;
    }

    public Decision evaluate(final List<Integer> historicalVolumes, final int currentVolume) {
        final int baselineMedian = median(historicalVolumes);
        if (baselineMedian < minimumBaselineRecords) {
            return new Decision(true, null, baselineMedian, baselineMedian <= 0 ? 1.0d : currentVolume / (double) baselineMedian);
        }

        if (currentVolume == 0) {
            return new Decision(false, BlockReason.ZERO_WITH_BASELINE, baselineMedian, 0.0d);
        }

        final double ratio = currentVolume / (double) baselineMedian;
        if (ratio < minimumRatio) {
            return new Decision(false, BlockReason.SHARP_DROP, baselineMedian, ratio);
        }

        return new Decision(true, null, baselineMedian, ratio);
    }

    static int median(final List<Integer> values) {
        if (values == null || values.isEmpty()) {
            return 0;
        }
        final List<Integer> filtered = new ArrayList<>();
        for (final Integer value : values) {
            if (value != null && value >= 0) {
                filtered.add(value);
            }
        }
        if (filtered.isEmpty()) {
            return 0;
        }
        filtered.sort(Comparator.naturalOrder());
        return filtered.get(filtered.size() / 2);
    }
}
