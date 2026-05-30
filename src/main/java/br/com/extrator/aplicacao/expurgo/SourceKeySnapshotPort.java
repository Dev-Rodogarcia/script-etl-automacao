package br.com.extrator.aplicacao.expurgo;

import java.time.LocalDate;

public interface SourceKeySnapshotPort {
    SourceKeySnapshot carregarChaves(EntityReconciliationSpec spec, LocalDate dataInicio, LocalDate dataFim);
}
