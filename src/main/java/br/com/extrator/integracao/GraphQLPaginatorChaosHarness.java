package br.com.extrator.integracao;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.LoggerFactory;

/**
 * Harness enxuto para validar protecoes de paginacao em cenarios de chaos.
 */
public final class GraphQLPaginatorChaosHarness {

    public record ProbeResult(
        boolean completo,
        String motivoInterrupcao,
        int paginasProcessadas,
        int registrosExtraidos,
        long durationMs
    ) {
    }

    private GraphQLPaginatorChaosHarness() {
    }

    public static ProbeResult executarPaginacaoInfinita() {
        final AtomicInteger callCount = new AtomicInteger();
        final GraphQLPaginator paginator = new GraphQLPaginator(
            LoggerFactory.getLogger(GraphQLPaginatorChaosHarness.class),
            1000,
            3,
            Duration.ofMinutes(1),
            new HashMap<>(),
            new HashSet<>(),
            new HashMap<>(),
            null,
            new GraphQLPageFetcher() {
                @Override
                public <T> PaginatedGraphQLResponse<T> fetch(
                    final String query,
                    final String nomeEntidade,
                    final java.util.Map<String, Object> variaveis,
                    final Class<T> tipoClasse
                ) {
                    final int pagina = callCount.incrementAndGet();
                    return cast(List.of(pagina), true, "cursor-" + pagina);
                }
            }
        );

        final long inicio = System.nanoTime();
        final ResultadoExtracao<Integer> resultado = paginator.executarQueryPaginada(
            "chaos-pagination",
            "query",
            "freights",
            java.util.Map.of(),
            Integer.class
        );
        final long duracaoMs = Duration.ofNanos(System.nanoTime() - inicio).toMillis();

        return new ProbeResult(
            resultado.isCompleto(),
            resultado.getMotivoInterrupcao(),
            resultado.getPaginasProcessadas(),
            resultado.getRegistrosExtraidos(),
            duracaoMs
        );
    }

    @SuppressWarnings("unchecked")
    private static <T> PaginatedGraphQLResponse<T> cast(
        final List<?> dados,
        final boolean hasNextPage,
        final String cursor
    ) {
        return new PaginatedGraphQLResponse<>(
            (List<T>) dados,
            hasNextPage,
            cursor,
            200,
            10,
            "req",
            "resp",
            dados.size()
        );
    }
}
