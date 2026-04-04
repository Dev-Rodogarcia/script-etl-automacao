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
        try {
            System.clearProperty("etl.graphql.timeout.entidade.coletas.intervalo.ms");
            System.clearProperty("etl.referencial.coletas.backfill.max_expansao_dias.intervalo");
            System.clearProperty("etl.intervalo.coletas.max_consecutive_failures");

            assertEquals(Duration.ofMinutes(30), ConfigEtl.obterTimeoutEntidadeGraphQLColetasIntervalo());
            assertEquals(400, ConfigEtl.obterEtlReferencialColetasBackfillMaxExpansaoDiasIntervalo());
            assertEquals(2, ConfigEtl.obterEtlIntervaloColetasMaxConsecutiveFailures());
            assertEquals(Duration.ofMinutes(10), ConfigEtl.obterTimeoutEntidadeGraphQL("coletas"));
            assertEquals(30, ConfigEtl.obterEtlReferencialColetasBackfillMaxExpansaoDias());
        } finally {
            restaurarPropriedade("etl.graphql.timeout.entidade.coletas.intervalo.ms", timeoutIntervaloAnterior);
            restaurarPropriedade("etl.referencial.coletas.backfill.max_expansao_dias.intervalo", expansaoAnterior);
            restaurarPropriedade("etl.intervalo.coletas.max_consecutive_failures", falhasAnterior);
        }
    }

    @Test
    void deveRespeitarOverridesDoModoIntervaloParaColetas() {
        final String timeoutIntervaloAnterior = System.getProperty("etl.graphql.timeout.entidade.coletas.intervalo.ms");
        final String expansaoAnterior =
            System.getProperty("etl.referencial.coletas.backfill.max_expansao_dias.intervalo");
        final String falhasAnterior = System.getProperty("etl.intervalo.coletas.max_consecutive_failures");
        try {
            System.setProperty("etl.graphql.timeout.entidade.coletas.intervalo.ms", "2400000");
            System.setProperty("etl.referencial.coletas.backfill.max_expansao_dias.intervalo", "730");
            System.setProperty("etl.intervalo.coletas.max_consecutive_failures", "5");

            assertEquals(Duration.ofMinutes(40), ConfigEtl.obterTimeoutEntidadeGraphQLColetasIntervalo());
            assertEquals(730, ConfigEtl.obterEtlReferencialColetasBackfillMaxExpansaoDiasIntervalo());
            assertEquals(5, ConfigEtl.obterEtlIntervaloColetasMaxConsecutiveFailures());
        } finally {
            restaurarPropriedade("etl.graphql.timeout.entidade.coletas.intervalo.ms", timeoutIntervaloAnterior);
            restaurarPropriedade("etl.referencial.coletas.backfill.max_expansao_dias.intervalo", expansaoAnterior);
            restaurarPropriedade("etl.intervalo.coletas.max_consecutive_failures", falhasAnterior);
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
