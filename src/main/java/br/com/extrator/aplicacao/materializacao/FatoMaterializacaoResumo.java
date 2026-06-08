package br.com.extrator.aplicacao.materializacao;

import java.time.Duration;
import java.util.List;

public record FatoMaterializacaoResumo(
    List<FatoMaterializacaoProcedureResultado> procedures,
    Duration duracao
) {
    public long totalLinhasInseridas() {
        return procedures.stream()
            .mapToLong(FatoMaterializacaoProcedureResultado::linhasInseridas)
            .sum();
    }

    public long totalLinhasAtualizadas() {
        return procedures.stream()
            .mapToLong(FatoMaterializacaoProcedureResultado::linhasAtualizadas)
            .sum();
    }
}
