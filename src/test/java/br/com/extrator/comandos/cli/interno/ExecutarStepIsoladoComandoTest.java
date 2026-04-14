package br.com.extrator.comandos.cli.interno;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ExecutarStepIsoladoComandoTest {

    @AfterEach
    void limparOverrides() {
        System.clearProperty("etl.process.isolated.child");
        System.clearProperty("ETL_PROCESS_ISOLATED_CHILD");
        System.clearProperty("etl.process.isolated.manual.allow");
        System.clearProperty("ETL_PROCESS_ISOLATED_MANUAL_ALLOW");
    }

    @Test
    void deveBloquearExecucaoManualDoComandoInternoPorPadrao() {
        final ExecutarStepIsoladoComando comando = new ExecutarStepIsoladoComando();

        assertThrows(
            IllegalStateException.class,
            () -> comando.executar(new String[] {
                "--executar-step-isolado",
                "graphql",
                "2026-04-10",
                "2026-04-10",
                "all"
            })
        );
    }
}
