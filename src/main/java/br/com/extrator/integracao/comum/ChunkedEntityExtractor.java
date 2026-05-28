package br.com.extrator.integracao.comum;

import java.time.LocalDate;

import br.com.extrator.integracao.PageChunkConsumer;
import br.com.extrator.integracao.ResultadoExtracao;

public interface ChunkedEntityExtractor<T> extends EntityExtractor<T> {
    ResultadoExtracao<T> extractInChunks(
        LocalDate dataInicio,
        LocalDate dataFim,
        PageChunkConsumer<T> chunkConsumer
    );

    default ChunkedExtractionOutcome<T> extractAndSaveWithMetrics(
        final LocalDate dataInicio,
        final LocalDate dataFim
    ) throws Exception {
        final ChunkedSaveMetricsAccumulator<T> accumulator = new ChunkedSaveMetricsAccumulator<>(this);
        final ResultadoExtracao<T> resultado = extractInChunks(dataInicio, dataFim, accumulator::processar);
        return new ChunkedExtractionOutcome<>(
            resultado,
            accumulator.toSaveMetrics(),
            accumulator.getDuracaoSalvamento()
        );
    }
}
