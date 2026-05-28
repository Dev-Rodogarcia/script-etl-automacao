package br.com.extrator.integracao.comum;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

final class ChunkedSaveMetricsAccumulator<T> {
    private final EntityExtractor<T> extractor;
    private int registrosSalvos;
    private int totalUnicos;
    private int registrosInvalidos;
    private int registrosPersistidos;
    private int registrosNoOpIdempotente;
    private long nanosSalvamento;

    ChunkedSaveMetricsAccumulator(final EntityExtractor<T> extractor) {
        this.extractor = Objects.requireNonNull(extractor, "extractor");
    }

    void processar(final List<T> registros) throws java.sql.SQLException {
        if (registros == null || registros.isEmpty()) {
            return;
        }
        final long inicio = System.nanoTime();
        final EntityExtractor.SaveMetrics metrics = extractor.saveWithMetrics(registros);
        nanosSalvamento += System.nanoTime() - inicio;
        registrosSalvos += metrics.getRegistrosSalvos();
        totalUnicos += metrics.getTotalUnicos();
        registrosInvalidos += metrics.getRegistrosInvalidos();
        registrosPersistidos += metrics.getRegistrosPersistidos();
        registrosNoOpIdempotente += metrics.getRegistrosNoOpIdempotente();
    }

    EntityExtractor.SaveMetrics toSaveMetrics() {
        return new EntityExtractor.SaveMetrics(
            registrosSalvos,
            totalUnicos,
            registrosInvalidos,
            registrosPersistidos,
            registrosNoOpIdempotente
        );
    }

    Duration getDuracaoSalvamento() {
        return Duration.ofNanos(nanosSalvamento);
    }
}
