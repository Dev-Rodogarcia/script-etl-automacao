package br.com.extrator.aplicacao.extracao;

import java.time.LocalDate;
import java.util.Objects;

public record ExtracaoPorIntervaloRequest(
    LocalDate dataInicio,
    LocalDate dataFim,
    String apiEspecifica,
    String entidadeEspecifica,
    boolean incluirFaturasGraphQL,
    boolean modoLoopDaemon
) {
    public ExtracaoPorIntervaloRequest {
        Objects.requireNonNull(dataInicio, "dataInicio nao pode ser null");
        Objects.requireNonNull(dataFim, "dataFim nao pode ser null");
        apiEspecifica = normalizar(apiEspecifica);
        entidadeEspecifica = normalizar(entidadeEspecifica);
    }

    private static String normalizar(final String valor) {
        if (valor == null) {
            return null;
        }
        final String limpo = valor.trim();
        return limpo.isEmpty() ? null : limpo;
    }
}
