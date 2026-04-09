package br.com.extrator.observabilidade.quality;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class SqlServerDataQualityQueryAdapterTest {

    @Test
    void deveMapearInventarioESinistrosNaDeteccaoDeSchema() {
        assertEquals("inventario", SqlServerDataQualityQueryAdapter.resolverNomeTabela("inventario"));
        assertEquals("sinistros", SqlServerDataQualityQueryAdapter.resolverNomeTabela("sinistros"));
    }

    @Test
    void deveRetornarNullParaEntidadeSemMapeamentoGenerico() {
        assertNull(SqlServerDataQualityQueryAdapter.resolverNomeTabela("entidade_inexistente"));
    }
}
