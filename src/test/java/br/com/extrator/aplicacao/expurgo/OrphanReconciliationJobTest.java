package br.com.extrator.aplicacao.expurgo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import br.com.extrator.aplicacao.extracao.ExecutionLockManager;
import br.com.extrator.integracao.constantes.ConstantesApiDataExport.ConfiguracaoEntidade;

class OrphanReconciliationJobTest {

    @Test
    void falhaNoSnapshotAbortaAntesDeQualquerUpdate() {
        final EntityReconciliationSpec primeira = spec("primeira");
        final EntityReconciliationSpec segunda = spec("segunda");
        final FakeSnapshotPort snapshots = new FakeSnapshotPort();
        snapshots.snapshots.put(primeira.entityName(), new SourceKeySnapshot(Set.of("A"), true, 1, 1, 0));
        snapshots.falharEm.add(segunda.entityName());
        final FakeStore store = new FakeStore();
        store.activeKeys.put(primeira.entityName(), set("A", "B"));

        final OrphanReconciliationJob job = new OrphanReconciliationJob(snapshots, store, lockManager());
        final OrphanReconciliationRequest request = new OrphanReconciliationRequest(
            LocalDate.of(2026, 5, 29),
            LocalDate.of(2026, 5, 30),
            List.of(primeira, segunda),
            false,
            500
        );

        final IllegalStateException erro = assertThrows(IllegalStateException.class, () -> job.executar(request));

        assertTrue(erro.getMessage().contains("Expurgo abortado antes de qualquer UPDATE"));
        assertEquals(0, store.updateCalls);
        assertEquals(0, store.buscarCalls);
    }

    @Test
    void marcaSomenteChavesAtivasAusentesNaApi() throws Exception {
        final EntityReconciliationSpec spec = spec("faturas_por_cliente");
        final FakeSnapshotPort snapshots = new FakeSnapshotPort();
        snapshots.snapshots.put(spec.entityName(), new SourceKeySnapshot(set("A", "C"), true, 2, 2, 0));
        final FakeStore store = new FakeStore();
        store.activeKeys.put(spec.entityName(), set("A", "B", "C", "D"));

        final OrphanReconciliationJob job = new OrphanReconciliationJob(snapshots, store, lockManager());
        final OrphanReconciliationReport report = job.executar(new OrphanReconciliationRequest(
            LocalDate.of(2026, 5, 29),
            LocalDate.of(2026, 5, 30),
            List.of(spec),
            false,
            2
        ));

        assertEquals(List.of("B", "D"), store.updatedKeys);
        assertEquals(2, store.lastBatchSize);
        assertEquals(LocalDate.of(2026, 5, 29), store.lastDataInicio);
        assertEquals(LocalDate.of(2026, 5, 30), store.lastDataFim);
        assertEquals(2, report.totalOrphans());
        assertEquals(2, report.totalUpdated());
    }

    @Test
    void dryRunNaoExecutaUpdate() throws Exception {
        final EntityReconciliationSpec spec = spec("manifestos");
        final FakeSnapshotPort snapshots = new FakeSnapshotPort();
        snapshots.snapshots.put(spec.entityName(), new SourceKeySnapshot(set("1"), true, 1, 1, 0));
        final FakeStore store = new FakeStore();
        store.activeKeys.put(spec.entityName(), set("1", "2"));

        final OrphanReconciliationJob job = new OrphanReconciliationJob(snapshots, store, lockManager());
        final OrphanReconciliationReport report = job.executar(new OrphanReconciliationRequest(
            LocalDate.of(2026, 5, 29),
            LocalDate.of(2026, 5, 30),
            List.of(spec),
            true,
            500
        ));

        assertEquals(0, store.updateCalls);
        assertEquals(1, report.totalOrphans());
        assertEquals(0, report.totalUpdated());
    }

    private static EntityReconciliationSpec spec(final String entityName) {
        return new EntityReconciliationSpec(
            entityName,
            "dbo." + entityName,
            "id",
            "CAST(base.data_extracao AS date)",
            new ConfiguracaoEntidade(1, "service_at", "freights", "100", Duration.ofSeconds(1), "id asc", false),
            ignored -> "id"
        );
    }

    private static ExecutionLockManager lockManager() {
        return ignored -> () -> {
        };
    }

    private static LinkedHashSet<String> set(final String... values) {
        final LinkedHashSet<String> set = new LinkedHashSet<>();
        for (final String value : values) {
            set.add(value);
        }
        return set;
    }

    private static final class FakeSnapshotPort implements SourceKeySnapshotPort {
        private final Map<String, SourceKeySnapshot> snapshots = new LinkedHashMap<>();
        private final Set<String> falharEm = new LinkedHashSet<>();

        @Override
        public SourceKeySnapshot carregarChaves(final EntityReconciliationSpec spec,
                                                final LocalDate dataInicio,
                                                final LocalDate dataFim) {
            if (falharEm.contains(spec.entityName())) {
                throw new IllegalStateException("API indisponivel");
            }
            return snapshots.get(spec.entityName());
        }
    }

    private static final class FakeStore implements OrphanReconciliationStore {
        private final Map<String, Set<String>> activeKeys = new LinkedHashMap<>();
        private final List<String> updatedKeys = new ArrayList<>();
        private int updateCalls;
        private int buscarCalls;
        private int lastBatchSize;
        private LocalDate lastDataInicio;
        private LocalDate lastDataFim;

        @Override
        public Set<String> buscarChavesAtivas(final EntityReconciliationSpec spec,
                                              final LocalDate dataInicio,
                                              final LocalDate dataFim) throws SQLException {
            buscarCalls++;
            lastDataInicio = dataInicio;
            lastDataFim = dataFim;
            return activeKeys.getOrDefault(spec.entityName(), Set.of());
        }

        @Override
        public int marcarOrfaos(final EntityReconciliationSpec spec,
                                final List<String> orphanKeys,
                                final int batchSize) throws SQLException {
            updateCalls++;
            lastBatchSize = batchSize;
            updatedKeys.addAll(orphanKeys);
            return orphanKeys.size();
        }
    }
}
