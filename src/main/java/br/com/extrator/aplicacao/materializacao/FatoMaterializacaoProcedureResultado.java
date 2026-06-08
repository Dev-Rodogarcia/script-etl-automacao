package br.com.extrator.aplicacao.materializacao;

import java.time.Duration;
import java.time.LocalDateTime;

public record FatoMaterializacaoProcedureResultado(
    String procedureName,
    long linhasInseridas,
    long linhasAtualizadas,
    LocalDateTime snapshotEm,
    Duration duracao
) {
}
