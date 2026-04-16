package br.com.extrator.suporte.configuracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class ConfigEtlTest {

    @Test
    void deveUsarPadraoMaiorParaTimeoutReferencialDeColetas() {
        final Duration timeoutPrincipal = ConfigEtl.obterTimeoutEntidadeGraphQL("coletas");
        final Duration timeoutReferencial = ConfigEtl.obterTimeoutEntidadeGraphQLColetasReferencial();

        assertEquals(Duration.ofMinutes(30), timeoutReferencial);
        assertTrue(
            timeoutReferencial.compareTo(timeoutPrincipal) >= 0,
            "Timeout referencial nao deve ser menor que o timeout principal de coletas."
        );
    }

    @Test
    void deveUsarTimeoutEspecificoParaFretes() {
        final String propriedadeLegada = "etl.graphql.timeout.entidade.fretes.ms";
        final String propriedadeEnv = "ETL_GRAPHQL_TIMEOUT_ENTIDADE_FRETES_MS";
        final String valorLegadoAnterior = System.getProperty(propriedadeLegada);
        final String valorEnvAnterior = System.getProperty(propriedadeEnv);
        try {
            System.clearProperty(propriedadeLegada);
            System.clearProperty(propriedadeEnv);

            assertEquals(Duration.ofMinutes(15), ConfigEtl.obterTimeoutEntidadeGraphQL("fretes"));
        } finally {
            restaurarPropriedade(propriedadeLegada, valorLegadoAnterior);
            restaurarPropriedade(propriedadeEnv, valorEnvAnterior);
        }
    }

    @Test
    void deveRespeitarOverrideDoTimeoutDeFretes() {
        final String chave = "etl.graphql.timeout.entidade.fretes.ms";
        final String valorAnterior = System.getProperty(chave);
        try {
            System.setProperty(chave, "1020000");

            assertEquals(Duration.ofMinutes(17), ConfigEtl.obterTimeoutEntidadeGraphQL("fretes"));
        } finally {
            restaurarPropriedade(chave, valorAnterior);
        }
    }

    @Test
    void deveRespeitarOverrideDoTimeoutReferencialDeColetas() {
        final String chave = "etl.graphql.timeout.entidade.coletas_referencial.ms";
        final String valorAnterior = System.getProperty(chave);
        try {
            System.setProperty(chave, "2400000");

            assertEquals(Duration.ofMinutes(40), ConfigEtl.obterTimeoutEntidadeGraphQLColetasReferencial());
        } finally {
            restaurarPropriedade(chave, valorAnterior);
        }
    }

    @Test
    void deveUsarDefaultsDoModoIntervaloParaColetas() {
        final String timeoutIntervaloAnterior = System.getProperty("etl.graphql.timeout.entidade.coletas.intervalo.ms");
        final String expansaoAnterior =
            System.getProperty("etl.referencial.coletas.backfill.max_expansao_dias.intervalo");
        final String falhasAnterior = System.getProperty("etl.intervalo.coletas.max_consecutive_failures");
        final String pruneRatioAnterior = System.getProperty("etl.fretes.prune.guardrail.min_ratio");
        final String replayTtlAnterior = System.getProperty("etl.recovery.replay.idempotency.ttl.hours");
        final String daemonAlertsAnterior = System.getProperty("loop.daemon.max_consecutive_alert_cycles");
        try {
            System.clearProperty("etl.graphql.timeout.entidade.coletas.intervalo.ms");
            System.clearProperty("etl.referencial.coletas.backfill.max_expansao_dias.intervalo");
            System.clearProperty("etl.intervalo.coletas.max_consecutive_failures");
            System.clearProperty("etl.fretes.prune.guardrail.min_ratio");
            System.clearProperty("etl.recovery.replay.idempotency.ttl.hours");
            System.clearProperty("loop.daemon.max_consecutive_alert_cycles");

            assertEquals(Duration.ofMinutes(30), ConfigEtl.obterTimeoutEntidadeGraphQLColetasIntervalo());
            assertEquals(400, ConfigEtl.obterEtlReferencialColetasBackfillMaxExpansaoDiasIntervalo());
            assertEquals(2, ConfigEtl.obterEtlIntervaloColetasMaxConsecutiveFailures());
            assertEquals(Duration.ofMinutes(10), ConfigEtl.obterTimeoutEntidadeGraphQL("coletas"));
            assertEquals(30, ConfigEtl.obterEtlReferencialColetasBackfillMaxExpansaoDias());
            assertEquals(0.30d, ConfigEtl.obterFretePruneGuardrailMinRatio());
            assertEquals(Duration.ofHours(12), ConfigEtl.obterRecoveryReplayIdempotencyTtl());
            assertEquals(3, ConfigEtl.obterLoopDaemonMaxConsecutiveAlertCycles());
        } finally {
            restaurarPropriedade("etl.graphql.timeout.entidade.coletas.intervalo.ms", timeoutIntervaloAnterior);
            restaurarPropriedade("etl.referencial.coletas.backfill.max_expansao_dias.intervalo", expansaoAnterior);
            restaurarPropriedade("etl.intervalo.coletas.max_consecutive_failures", falhasAnterior);
            restaurarPropriedade("etl.fretes.prune.guardrail.min_ratio", pruneRatioAnterior);
            restaurarPropriedade("etl.recovery.replay.idempotency.ttl.hours", replayTtlAnterior);
            restaurarPropriedade("loop.daemon.max_consecutive_alert_cycles", daemonAlertsAnterior);
        }
    }

    @Test
    void deveRespeitarOverridesDoModoIntervaloParaColetas() {
        final String timeoutIntervaloAnterior = System.getProperty("etl.graphql.timeout.entidade.coletas.intervalo.ms");
        final String expansaoAnterior =
            System.getProperty("etl.referencial.coletas.backfill.max_expansao_dias.intervalo");
        final String falhasAnterior = System.getProperty("etl.intervalo.coletas.max_consecutive_failures");
        final String pruneRatioAnterior = System.getProperty("etl.fretes.prune.guardrail.min_ratio");
        final String replayTtlAnterior = System.getProperty("etl.recovery.replay.idempotency.ttl.hours");
        final String daemonAlertsAnterior = System.getProperty("loop.daemon.max_consecutive_alert_cycles");
        try {
            System.setProperty("etl.graphql.timeout.entidade.coletas.intervalo.ms", "2400000");
            System.setProperty("etl.referencial.coletas.backfill.max_expansao_dias.intervalo", "730");
            System.setProperty("etl.intervalo.coletas.max_consecutive_failures", "5");
            System.setProperty("etl.fretes.prune.guardrail.min_ratio", "0.55");
            System.setProperty("etl.recovery.replay.idempotency.ttl.hours", "24");
            System.setProperty("loop.daemon.max_consecutive_alert_cycles", "5");

            assertEquals(Duration.ofMinutes(40), ConfigEtl.obterTimeoutEntidadeGraphQLColetasIntervalo());
            assertEquals(730, ConfigEtl.obterEtlReferencialColetasBackfillMaxExpansaoDiasIntervalo());
            assertEquals(5, ConfigEtl.obterEtlIntervaloColetasMaxConsecutiveFailures());
            assertEquals(0.55d, ConfigEtl.obterFretePruneGuardrailMinRatio());
            assertEquals(Duration.ofHours(24), ConfigEtl.obterRecoveryReplayIdempotencyTtl());
            assertEquals(5, ConfigEtl.obterLoopDaemonMaxConsecutiveAlertCycles());
        } finally {
            restaurarPropriedade("etl.graphql.timeout.entidade.coletas.intervalo.ms", timeoutIntervaloAnterior);
            restaurarPropriedade("etl.referencial.coletas.backfill.max_expansao_dias.intervalo", expansaoAnterior);
            restaurarPropriedade("etl.intervalo.coletas.max_consecutive_failures", falhasAnterior);
            restaurarPropriedade("etl.fretes.prune.guardrail.min_ratio", pruneRatioAnterior);
            restaurarPropriedade("etl.recovery.replay.idempotency.ttl.hours", replayTtlAnterior);
            restaurarPropriedade("loop.daemon.max_consecutive_alert_cycles", daemonAlertsAnterior);
        }
    }

    @Test
    void deveUsarTimeoutMaiorParaLockGlobalPorPadrao() {
        final String valorAnterior = System.getProperty("etl.execution.lock.timeout.ms");
        try {
            System.clearProperty("etl.execution.lock.timeout.ms");

            assertEquals(30_000, ConfigEtl.obterTimeoutLockExecucaoMs());
        } finally {
            restaurarPropriedade("etl.execution.lock.timeout.ms", valorAnterior);
        }
    }

    @Test
    void deveUsarTimeoutAgregadoMaisRealistaParaStepDataExport() {
        final String valorAnterior = System.getProperty("etl.pipeline.timeout.step.dataexport.ms");
        try {
            System.clearProperty("etl.pipeline.timeout.step.dataexport.ms");

            assertEquals(Duration.ofHours(1), ConfigEtl.obterTimeoutStepDataExport());
        } finally {
            restaurarPropriedade("etl.pipeline.timeout.step.dataexport.ms", valorAnterior);
        }
    }

    @Test
    void deveRespeitarOverrideDoTimeoutDoStepDataExport() {
        final String chave = "etl.pipeline.timeout.step.dataexport.ms";
        final String valorAnterior = System.getProperty(chave);
        try {
            System.setProperty(chave, "5400000");

            assertEquals(Duration.ofMinutes(90), ConfigEtl.obterTimeoutStepDataExport());
        } finally {
            restaurarPropriedade(chave, valorAnterior);
        }
    }

    @Test
    void execucaoManualDeStepIsoladoDeveVirDesabilitadaPorPadrao() {
        final String valorAnterior = System.getProperty("etl.process.isolated.manual.allow");
        try {
            System.clearProperty("etl.process.isolated.manual.allow");

            assertTrue(!ConfigEtl.isExecucaoManualStepIsoladoPermitida());
        } finally {
            restaurarPropriedade("etl.process.isolated.manual.allow", valorAnterior);
        }
    }

    private void restaurarPropriedade(final String chave, final String valorAnterior) {
        if (valorAnterior == null) {
            System.clearProperty(chave);
            return;
        }
        System.setProperty(chave, valorAnterior);
    }
}
