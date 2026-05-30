package br.com.extrator.aplicacao.expurgo;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.extrator.aplicacao.extracao.ExecutionLockManager;
import br.com.extrator.integracao.DataExportKeySnapshotClient;
import br.com.extrator.persistencia.repositorio.OrphanReconciliationRepository;
import br.com.extrator.suporte.banco.SqlServerExecutionLockManager;

public class OrphanReconciliationJob {
    private static final Logger logger = LoggerFactory.getLogger(OrphanReconciliationJob.class);
    static final String LOCK_RESOURCE = "etl-orphan-reconciliation";

    private final SourceKeySnapshotPort sourceKeySnapshotPort;
    private final OrphanReconciliationStore store;
    private final ExecutionLockManager lockManager;

    public OrphanReconciliationJob() {
        this(
            new DataExportKeySnapshotClient(),
            new OrphanReconciliationRepository(),
            new SqlServerExecutionLockManager()
        );
    }

    OrphanReconciliationJob(final SourceKeySnapshotPort sourceKeySnapshotPort,
                            final OrphanReconciliationStore store,
                            final ExecutionLockManager lockManager) {
        this.sourceKeySnapshotPort = sourceKeySnapshotPort;
        this.store = store;
        this.lockManager = lockManager;
    }

    public OrphanReconciliationReport executar(final OrphanReconciliationRequest request) throws Exception {
        final String runId = UUID.randomUUID().toString();
        final Instant inicio = Instant.now();
        logger.info(
            "Iniciando expurgo logico de orfaos | run_id={} | periodo={}..{} | dry_run={} | entidades={}",
            runId,
            request.dataInicio(),
            request.dataFim(),
            request.dryRun(),
            request.specs().stream().map(EntityReconciliationSpec::entityName).toList()
        );

        try (AutoCloseable ignored = lockManager.acquire(LOCK_RESOURCE)) {
            final Map<EntityReconciliationSpec, SourceKeySnapshot> snapshots = carregarSnapshotsAntesDeExpurgar(request);
            final List<OrphanReconciliationEntityReport> reports = compararEAtualizar(request, snapshots);
            final Duration duration = Duration.between(inicio, Instant.now());
            logger.info(
                "Expurgo logico de orfaos concluido | run_id={} | orfaos={} | atualizados={} | duracao_ms={}",
                runId,
                reports.stream().mapToInt(OrphanReconciliationEntityReport::orphanCount).sum(),
                reports.stream().mapToInt(OrphanReconciliationEntityReport::updatedCount).sum(),
                duration.toMillis()
            );
            return new OrphanReconciliationReport(
                runId,
                request.dataInicio(),
                request.dataFim(),
                request.dryRun(),
                reports,
                duration
            );
        }
    }

    private Map<EntityReconciliationSpec, SourceKeySnapshot> carregarSnapshotsAntesDeExpurgar(
        final OrphanReconciliationRequest request
    ) {
        final Map<EntityReconciliationSpec, SourceKeySnapshot> snapshots = new LinkedHashMap<>();
        for (final EntityReconciliationSpec spec : request.specs()) {
            try {
                final SourceKeySnapshot snapshot = sourceKeySnapshotPort.carregarChaves(
                    spec,
                    request.dataInicio(),
                    request.dataFim()
                );
                if (!snapshot.complete()) {
                    throw new IllegalStateException("Snapshot incompleto para " + spec.entityName());
                }
                snapshots.put(spec, snapshot);
                logger.info(
                    "Snapshot de chaves carregado | entidade={} | chaves={} | linhas={} | paginas={} | ignoradas={}",
                    spec.entityName(),
                    snapshot.keys().size(),
                    snapshot.rowsRead(),
                    snapshot.pagesProcessed(),
                    snapshot.skippedRows()
                );
            } catch (final RuntimeException e) {
                throw new IllegalStateException(
                    "Falha ao carregar snapshot de chaves da origem para "
                        + spec.entityName()
                        + ". Expurgo abortado antes de qualquer UPDATE.",
                    e
                );
            }
        }
        return snapshots;
    }

    private List<OrphanReconciliationEntityReport> compararEAtualizar(
        final OrphanReconciliationRequest request,
        final Map<EntityReconciliationSpec, SourceKeySnapshot> snapshots
    ) throws Exception {
        final List<OrphanReconciliationEntityReport> reports = new ArrayList<>();
        for (final Map.Entry<EntityReconciliationSpec, SourceKeySnapshot> entry : snapshots.entrySet()) {
            final EntityReconciliationSpec spec = entry.getKey();
            final SourceKeySnapshot snapshot = entry.getValue();
            final Set<String> dbActiveKeys = store.buscarChavesAtivas(spec);
            if (snapshot.keys().isEmpty() && !dbActiveKeys.isEmpty()) {
                throw new IllegalStateException(
                    "Snapshot da API retornou zero chaves para "
                        + spec.entityName()
                        + " com "
                        + dbActiveKeys.size()
                        + " chaves ativas no banco. Expurgo abortado para evitar falso positivo."
                );
            }

            final List<String> orphans = new ArrayList<>();
            for (final String dbKey : dbActiveKeys) {
                if (!snapshot.keys().contains(dbKey)) {
                    orphans.add(dbKey);
                }
            }

            final int updated = request.dryRun()
                ? 0
                : store.marcarOrfaos(spec, orphans, request.batchSize());

            logger.info(
                "Reconciliacao de entidade concluida | entidade={} | api_keys={} | db_ativas={} | orfaos={} | atualizados={} | dry_run={}",
                spec.entityName(),
                snapshot.keys().size(),
                dbActiveKeys.size(),
                orphans.size(),
                updated,
                request.dryRun()
            );
            reports.add(new OrphanReconciliationEntityReport(
                spec.entityName(),
                snapshot.keys().size(),
                dbActiveKeys.size(),
                orphans.size(),
                updated,
                snapshot.pagesProcessed(),
                snapshot.rowsRead(),
                snapshot.skippedRows(),
                request.dryRun()
            ));
        }
        return List.copyOf(reports);
    }
}
