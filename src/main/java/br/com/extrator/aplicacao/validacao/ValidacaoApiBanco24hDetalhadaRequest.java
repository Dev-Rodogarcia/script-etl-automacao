package br.com.extrator.aplicacao.validacao;

import java.time.LocalDate;
import java.util.Objects;

public record ValidacaoApiBanco24hDetalhadaRequest(
    boolean incluirFaturasGraphQL,
    boolean periodoFechado,
    boolean permitirFallbackJanela,
    LocalDate dataReferenciaSistema
) {
    public ValidacaoApiBanco24hDetalhadaRequest {
        Objects.requireNonNull(dataReferenciaSistema, "dataReferenciaSistema");
    }
}
