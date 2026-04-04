package br.com.extrator.integracao.mapeamento.dataexport.sinistros;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import br.com.extrator.dominio.dataexport.sinistros.SinistroDTO;
import br.com.extrator.persistencia.entidade.SinistroEntity;

class SinistroMapperTest {

    private final SinistroMapper mapper = new SinistroMapper();

    @Test
    void deveNormalizarCamposDeHoraQuandoApiRetornaTimestampCompleto() {
        final SinistroDTO dto = new SinistroDTO();
        dto.setSequenceCode(883L);
        dto.setOpeningAtDate("04/02/2026");
        dto.setOccurrenceAtDate("04/02/2026");
        dto.setOccurrenceAtTime("2000-01-01T19:00:00.000-03:00");
        dto.setFinishedAtDate("04/02/2026");
        dto.setFinishedAtTime("2000-01-01T21:15:30.000-03:00");

        final SinistroEntity entity = mapper.toEntity(dto);

        assertEquals("19:00:00", entity.getOccurrenceAtTime());
        assertEquals("21:15:30", entity.getFinishedAtTime());
    }
}
