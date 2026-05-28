package br.com.extrator.integracao.comum;

import java.time.Duration;
import java.util.Objects;

import br.com.extrator.integracao.ResultadoExtracao;

public final class ChunkedExtractionOutcome<T> {
    private final ResultadoExtracao<T> resultado;
    private final EntityExtractor.SaveMetrics saveMetrics;
    private final Duration duracaoSalvamento;

    public ChunkedExtractionOutcome(final ResultadoExtracao<T> resultado,
                                    final EntityExtractor.SaveMetrics saveMetrics,
                                    final Duration duracaoSalvamento) {
        this.resultado = Objects.requireNonNull(resultado, "resultado");
        this.saveMetrics = Objects.requireNonNull(saveMetrics, "saveMetrics");
        this.duracaoSalvamento = duracaoSalvamento == null ? Duration.ZERO : duracaoSalvamento;
    }

    public ResultadoExtracao<T> getResultado() {
        return resultado;
    }

    public EntityExtractor.SaveMetrics getSaveMetrics() {
        return saveMetrics;
    }

    public Duration getDuracaoSalvamento() {
        return duracaoSalvamento;
    }
}
