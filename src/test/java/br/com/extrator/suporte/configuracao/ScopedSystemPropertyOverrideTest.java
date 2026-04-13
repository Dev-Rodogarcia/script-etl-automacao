package br.com.extrator.suporte.configuracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;

import org.junit.jupiter.api.Test;

class ScopedSystemPropertyOverrideTest {

    @Test
    void deveRestaurarValoresAnterioresAoEncerrarEscopo() {
        final String timeoutAnterior = System.getProperty("etl.teste.timeout");
        final String pruneAnterior = System.getProperty("etl.teste.prune");
        try {
            System.setProperty("etl.teste.timeout", "120000");
            System.clearProperty("etl.teste.prune");

            try (ScopedSystemPropertyOverride ignored = ScopedSystemPropertyOverride.apply(Map.of(
                "etl.teste.timeout", "1800000",
                "etl.teste.prune", "true"
            ))) {
                assertEquals("1800000", System.getProperty("etl.teste.timeout"));
                assertEquals("true", System.getProperty("etl.teste.prune"));
            }

            assertEquals("120000", System.getProperty("etl.teste.timeout"));
            assertNull(System.getProperty("etl.teste.prune"));
        } finally {
            restaurar("etl.teste.timeout", timeoutAnterior);
            restaurar("etl.teste.prune", pruneAnterior);
        }
    }

    private void restaurar(final String chave, final String valorAnterior) {
        if (valorAnterior == null) {
            System.clearProperty(chave);
        } else {
            System.setProperty(chave, valorAnterior);
        }
    }
}
