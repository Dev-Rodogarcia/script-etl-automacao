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

    private void restaurarPropriedade(final String chave, final String valorAnterior) {
        if (valorAnterior == null) {
            System.clearProperty(chave);
            return;
        }
        System.setProperty(chave, valorAnterior);
    }
}
