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

    public long totalProceduresFalhas() {
        return procedures.stream()
            .filter(procedure -> !procedure.sucesso())
            .count();
    }

    public boolean houveFalha() {
        return totalProceduresFalhas() > 0;
    }

    public List<FatoMaterializacaoProcedureResultado> proceduresComFalha() {
        return procedures.stream()
            .filter(procedure -> !procedure.sucesso())
            .toList();
    }
}
