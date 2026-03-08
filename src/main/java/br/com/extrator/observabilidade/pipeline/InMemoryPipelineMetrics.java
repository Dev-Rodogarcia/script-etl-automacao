package br.com.extrator.observabilidade.pipeline;

import br.com.extrator.aplicacao.portas.PipelineMetricsPort;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.DoubleAdder;

public final class InMemoryPipelineMetrics implements PipelineMetricsPort {
    private final Map<String, DoubleAdder> values = new ConcurrentHashMap<>();

    @Override
    public void registrarDuracaoEntidade(final String entidade, final long durationMillis) {
        add("etl_entity_duration_ms_sum{entidade=\"" + normalize(entidade) + "\"}", durationMillis);
        add("etl_entity_duration_ms_count{entidade=\"" + normalize(entidade) + "\"}", 1.0d);
    }

    @Override
    public void incrementarSucesso(final String entidade) {
        add("etl_entity_success_total{entidade=\"" + normalize(entidade) + "\"}", 1.0d);
    }

    @Override
    public void incrementarFalha(final String entidade) {
        add("etl_entity_failure_total{entidade=\"" + normalize(entidade) + "\"}", 1.0d);
    }

    @Override
    public Map<String, Double> obterSnapshot() {
        final Map<String, Double> obterSnapshot = new LinkedHashMap<>();
        for (Map.Entry<String, DoubleAdder> entry : values.entrySet()) {
            obterSnapshot.put(entry.getKey(), entry.getValue().sum());
        }
        return Collections.unmodifiableMap(obterSnapshot);
    }

    private void add(final String metric, final double value) {
        values.computeIfAbsent(metric, ignored -> new DoubleAdder()).add(value);
    }

    private String normalize(final String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.trim().toLowerCase();
    }
}


