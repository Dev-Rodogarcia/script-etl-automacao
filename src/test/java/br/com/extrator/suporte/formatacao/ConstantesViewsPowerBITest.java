package br.com.extrator.suporte.formatacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ConstantesViewsPowerBITest {

    @Test
    void deveExporViewPowerBiParaInventarioESinistros() {
        assertTrue(ConstantesViewsPowerBI.possuiView("inventario"));
        assertTrue(ConstantesViewsPowerBI.possuiView("sinistros"));
        assertEquals("vw_inventario_powerbi", ConstantesViewsPowerBI.obterNomeView("inventario"));
        assertEquals("vw_sinistros_powerbi", ConstantesViewsPowerBI.obterNomeView("sinistros"));
    }
}
