package br.com.extrator.aplicacao.extracao;

import java.time.LocalDate;
import java.util.Objects;

public class ReconciliacaoUseCase {
    private final ExtracaoPorIntervaloUseCase extracaoPorIntervaloUseCase;

    public ReconciliacaoUseCase() {
        this(new ExtracaoPorIntervaloUseCase());
    }

    ReconciliacaoUseCase(final ExtracaoPorIntervaloUseCase extracaoPorIntervaloUseCase) {
        this.extracaoPorIntervaloUseCase = Objects.requireNonNull(
            extracaoPorIntervaloUseCase,
            "extracaoPorIntervaloUseCase nao pode ser null"
        );
    }

    public void executar(final LocalDate data, final boolean incluirFaturasGraphQL) throws Exception {
        final ExtracaoPorIntervaloRequest request = new ExtracaoPorIntervaloRequest(
            data,
            data,
            null,
            null,
            incluirFaturasGraphQL,
            true
        );
        extracaoPorIntervaloUseCase.executar(request);
    }
}
