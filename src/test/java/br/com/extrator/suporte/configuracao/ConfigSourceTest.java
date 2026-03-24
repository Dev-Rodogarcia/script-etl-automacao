package br.com.extrator.suporte.configuracao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ConfigSourceTest {

    @Test
    void devePriorizarSystemPropertyDaVariavelAntesDoArquivo() {
        System.setProperty("TESTE_CONFIG_OVERRIDE", "123");
        try {
            assertEquals(
                "123",
                ConfigSource.obterConfiguracao(new String[] {"TESTE_CONFIG_OVERRIDE"}, "chave.ausente")
            );
        } finally {
            System.clearProperty("TESTE_CONFIG_OVERRIDE");
        }
    }

    @Test
    void deveAceitarSystemPropertyPeloNomeDaChaveProperties() {
        System.setProperty("api.graphql.max.paginas", "11");
        try {
            assertEquals("11", ConfigSource.obterConfiguracao(new String[] {"AMBIENTE_INEXISTENTE"}, "api.graphql.max.paginas"));
        } finally {
            System.clearProperty("api.graphql.max.paginas");
        }
    }
}
