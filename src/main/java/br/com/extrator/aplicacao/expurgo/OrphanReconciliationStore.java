package br.com.extrator.aplicacao.expurgo;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public interface OrphanReconciliationStore {
    Set<String> buscarChavesAtivas(EntityReconciliationSpec spec, LocalDate dataInicio, LocalDate dataFim)
        throws SQLException;

    int marcarOrfaos(EntityReconciliationSpec spec, List<String> orphanKeys, int batchSize) throws SQLException;
}
