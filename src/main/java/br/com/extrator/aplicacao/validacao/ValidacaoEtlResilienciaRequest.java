package br.com.extrator.aplicacao.validacao;

import java.time.Duration;

public record ValidacaoEtlResilienciaRequest(
    boolean autoChaos,
    int maxCycles,
    Duration duracaoMaxima,
    int stressConcorrencia,
    long seed,
    boolean incluirCenariosHttp
) {
    public ValidacaoEtlResilienciaRequest {
        maxCycles = Math.max(1, maxCycles);
        duracaoMaxima = duracaoMaxima == null || duracaoMaxima.isNegative() || duracaoMaxima.isZero()
            ? Duration.ofSeconds(30)
            : duracaoMaxima;
        stressConcorrencia = Math.max(2, stressConcorrencia);
    }
}
