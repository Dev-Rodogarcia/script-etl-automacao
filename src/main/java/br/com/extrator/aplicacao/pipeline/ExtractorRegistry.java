package br.com.extrator.aplicacao.pipeline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public final class ExtractorRegistry {
    private final Map<String, Supplier<PipelineStep>> stepsPorEntidade = new LinkedHashMap<>();

    public void registrar(final String entidade, final Supplier<PipelineStep> stepSupplier) {
        final String chave = normalize(entidade);
        if (chave.isBlank()) {
            throw new IllegalArgumentException("entidade nao pode ser vazia");
        }
        stepsPorEntidade.put(chave, stepSupplier);
    }

    public Optional<PipelineStep> get(final String entidade) {
        final Supplier<PipelineStep> supplier = stepsPorEntidade.get(normalize(entidade));
        if (supplier == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(supplier.get());
    }

    public List<PipelineStep> listarTodos() {
        final List<PipelineStep> steps = new ArrayList<>();
        for (Supplier<PipelineStep> supplier : stepsPorEntidade.values()) {
            steps.add(supplier.get());
        }
        return Collections.unmodifiableList(steps);
    }

    public List<PipelineStep> listarPorEntidades(final List<String> entidades) {
        final List<PipelineStep> steps = new ArrayList<>();
        for (String entidade : entidades) {
            get(entidade).ifPresent(steps::add);
        }
        return Collections.unmodifiableList(steps);
    }

    private String normalize(final String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}


