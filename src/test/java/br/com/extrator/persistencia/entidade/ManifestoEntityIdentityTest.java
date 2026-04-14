package br.com.extrator.persistencia.entidade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class ManifestoEntityIdentityTest {

    @Test
    void mesmoJsonComOrdemDiferenteDeveGerarMesmoIdentificador() {
        final ManifestoEntity primeiro = manifestoComMetadata("""
            {"sequence_code":61379,"branch_nickname":"SPO","vehicle_plate":"ABC1D23"}
            """);
        final ManifestoEntity segundo = manifestoComMetadata("""
            {"vehicle_plate":"ABC1D23","sequence_code":61379,"branch_nickname":"SPO"}
            """);

        primeiro.calcularIdentificadorUnico();
        segundo.calcularIdentificadorUnico();

        assertEquals(primeiro.getIdentificadorUnico(), segundo.getIdentificadorUnico());
    }

    @Test
    void jsonProfundamenteAninhadoComOrdemDiferenteDeveGerarMesmoIdentificador() {
        final ManifestoEntity primeiro = manifestoComMetadata("""
            {"sequence_code":1,"vehicle":{"plate":"ABC1D23","owner":{"name":"A","document":"1"}}}
            """);
        final ManifestoEntity segundo = manifestoComMetadata("""
            {"vehicle":{"owner":{"document":"1","name":"A"},"plate":"ABC1D23"},"sequence_code":1}
            """);

        primeiro.calcularIdentificadorUnico();
        segundo.calcularIdentificadorUnico();

        assertEquals(primeiro.getIdentificadorUnico(), segundo.getIdentificadorUnico());
    }

    @Test
    void mudancaEstavelDeNegocioDeveGerarHashDiferente() {
        final ManifestoEntity primeiro = manifestoComMetadata("""
            {"sequence_code":61379,"branch_nickname":"SPO","vehicle_plate":"ABC1D23"}
            """);
        final ManifestoEntity segundo = manifestoComMetadata("""
            {"sequence_code":61379,"branch_nickname":"VCP","vehicle_plate":"ABC1D23"}
            """);

        primeiro.calcularIdentificadorUnico();
        segundo.calcularIdentificadorUnico();

        assertNotEquals(primeiro.getIdentificadorUnico(), segundo.getIdentificadorUnico());
    }

    private ManifestoEntity manifestoComMetadata(final String metadata) {
        final ManifestoEntity entity = new ManifestoEntity();
        entity.setSequenceCode(61379L);
        entity.setMetadata(metadata);
        return entity;
    }
}
