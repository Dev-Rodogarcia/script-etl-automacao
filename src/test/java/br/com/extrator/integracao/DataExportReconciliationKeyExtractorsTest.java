package br.com.extrator.integracao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import br.com.extrator.dominio.dataexport.faturaporcliente.FaturaPorClienteDTO;
import br.com.extrator.integracao.mapeamento.dataexport.faturaporcliente.FaturaPorClienteMapper;
import br.com.extrator.suporte.mapeamento.MapperUtil;

class DataExportReconciliationKeyExtractorsTest {

    @Test
    void manifestoDeveUsarChaveCompostaNormalizada() throws Exception {
        final JsonNode row = MapperUtil.sharedJson().readTree("""
            {
              "sequence_code": 70035,
              "mft_pfs_pck_sequence_code": "100895.0",
              "mft_mfs_number": null
            }
            """);

        assertEquals("70035|100895|-1", DataExportReconciliationKeyExtractors.manifesto(row));
    }

    @Test
    void faturaPorClienteDeveGerarMesmoUniqueIdDoMapperCanonico() throws Exception {
        final JsonNode row = MapperUtil.sharedJson().readTree("""
            {
              "fit_fhe_cte_number": 123456,
              "fit_fhe_cte_key": "35260412345678901234570010001234561001234567",
              "fit_pyr_document": "12.345.678/0001-90",
              "fit_rpt_document": "98.765.432/0001-10",
              "fit_sdr_document": "11.222.333/0001-44",
              "fit_ant_document": "FAT-999",
              "fit_ant_issue_date": "2026-05-29",
              "total": "150.00",
              "fit_ant_value": "150.00"
            }
            """);

        final FaturaPorClienteDTO dto = MapperUtil.sharedJson().convertValue(row, FaturaPorClienteDTO.class);
        final String expected = new FaturaPorClienteMapper().calcularIdentificadorUnico(dto);

        assertEquals(expected, DataExportReconciliationKeyExtractors.faturaPorCliente(row));
    }
}
