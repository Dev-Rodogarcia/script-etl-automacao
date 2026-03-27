package br.com.extrator.plataforma.auditoria.dominio;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Contexto thread-local com as janelas planejadas por entidade.
 */
public final class ExecutionPlanContext {
    private static final ThreadLocal<Map<String, ExecutionWindowPlan>> PLANOS =
        ThreadLocal.withInitial(Map::of);

    private ExecutionPlanContext() {
    }

    public static void setPlanos(final Map<String, ExecutionWindowPlan> planos) {
        if (planos == null || planos.isEmpty()) {
            PLANOS.set(Map.of());
            return;
        }
        PLANOS.set(new LinkedHashMap<>(planos));
    }

    public static Optional<ExecutionWindowPlan> getPlano(final String entidade) {
        if (entidade == null || entidade.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(PLANOS.get().get(entidade));
    }

    public static void clear() {
        PLANOS.remove();
    }
}
