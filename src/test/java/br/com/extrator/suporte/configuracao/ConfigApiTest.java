package br.com.extrator.suporte.configuracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.ZoneId;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ConfigApiTest {

    @AfterEach
    void limparOverrides() {
        System.clearProperty("api.dataexport.timezone");
        System.clearProperty("etl.integridade.modo");
    }

    @Test
    void deveFalharSemTimezoneEmModoEstrito() {
        final String valorAnterior = System.getProperty("api.dataexport.timezone");
        final String valorArquivo = ConfigSource.carregarPropriedades().getProperty("api.dataexport.timezone");
        System.setProperty("etl.integridade.modo", "STRICT_INTEGRITY");
        System.clearProperty("api.dataexport.timezone");
        ConfigSource.carregarPropriedades().remove("api.dataexport.timezone");

        try {
            assertThrows(IllegalStateException.class, ConfigApi::obterZoneIdDataExport);
        } finally {
            if (valorArquivo != null) {
                ConfigSource.carregarPropriedades().setProperty("api.dataexport.timezone", valorArquivo);
            }
            if (valorAnterior != null) {
                System.setProperty("api.dataexport.timezone", valorAnterior);
            }
        }
    }

    @Test
    void deveFalharComTimezoneInvalidaEmModoEstrito() {
        System.setProperty("etl.integridade.modo", "STRICT_INTEGRITY");
        System.setProperty("api.dataexport.timezone", "timezone-invalida");

        assertThrows(IllegalStateException.class, ConfigApi::obterZoneIdDataExport);
    }

    @Test
    void deveUsarFallbackDoSistemaForaDoModoEstrito() {
        System.setProperty("etl.integridade.modo", "OPERACIONAL");
        System.setProperty("api.dataexport.timezone", "timezone-invalida");

        assertEquals(ZoneId.systemDefault(), ConfigApi.obterZoneIdDataExport());
    }

    @Test
    void deveHonrarTimezoneConfiguradaQuandoValida() {
        System.setProperty("etl.integridade.modo", "STRICT_INTEGRITY");
        System.setProperty("api.dataexport.timezone", "America/Sao_Paulo");

        assertEquals(ZoneId.of("America/Sao_Paulo"), ConfigApi.obterZoneIdDataExport());
    }
}
