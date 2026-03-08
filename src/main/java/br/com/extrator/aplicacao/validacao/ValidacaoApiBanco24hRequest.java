package br.com.extrator.aplicacao.validacao;

import java.time.LocalDate;
import java.util.Objects;

public record ValidacaoApiBanco24hRequest(
    boolean incluirFaturasGraphQL,
    boolean permitirFallbackJanela,
    LocalDate dataReferenciaSistema
) {
    public ValidacaoApiBanco24hRequest {
        Objects.requireNonNull(dataReferenciaSistema, "dataReferenciaSistema");
    }
}
