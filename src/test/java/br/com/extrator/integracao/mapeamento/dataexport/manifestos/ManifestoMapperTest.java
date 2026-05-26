package br.com.extrator.integracao.mapeamento.dataexport.manifestos;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import br.com.extrator.dominio.dataexport.manifestos.ManifestoDTO;
import br.com.extrator.persistencia.entidade.ManifestoEntity;

class ManifestoMapperTest {

    private final ManifestoMapper mapper = new ManifestoMapper();

    @Test
    void deveMapearMetricasFinanceirasOperacionaisDoPayloadEsl() {
        final ManifestoDTO dto = new ManifestoDTO();
        dto.setSequenceCode(69533L);
        dto.setKm("460.00");
        dto.setTotalCost("1234.56");
        dto.setManifestFreightsTotal("3456.78");
        dto.setTotalTaxedWeight("987.654");
        dto.setVehicleWeightCapacity("12000.00");
        dto.setManifestItemsCount(12);
        dto.setFinalizedManifestItemsCount(11);

        final ManifestoEntity entity = mapper.toEntity(dto);

        assertEquals(new BigDecimal("460.00"), entity.getKm());
        assertEquals(new BigDecimal("1234.56"), entity.getTotalCost());
        assertEquals(new BigDecimal("3456.78"), entity.getManifestFreightsTotal());
        assertEquals(new BigDecimal("987.654"), entity.getTotalTaxedWeight());
        assertEquals(new BigDecimal("12000.00"), entity.getCapacidadeKg());
        assertEquals(12, entity.getManifestItemsCount());
        assertEquals(11, entity.getFinalizedManifestItemsCount());
    }
}
