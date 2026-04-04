package br.com.extrator.analises.indicadoresgestao;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import br.com.extrator.dominio.dataexport.inventario.InventarioDTO;
import br.com.extrator.dominio.dataexport.localizacaocarga.LocalizacaoCargaDTO;
import br.com.extrator.dominio.dataexport.manifestos.ManifestoDTO;
import br.com.extrator.dominio.dataexport.sinistros.SinistroDTO;
import br.com.extrator.integracao.constantes.ConstantesApiDataExport;
import br.com.extrator.integracao.mapeamento.dataexport.inventario.InventarioMapper;
import br.com.extrator.integracao.mapeamento.dataexport.localizacaocarga.LocalizacaoCargaMapper;
import br.com.extrator.integracao.mapeamento.dataexport.manifestos.ManifestoMapper;
import br.com.extrator.integracao.mapeamento.dataexport.sinistros.SinistroMapper;
import br.com.extrator.persistencia.entidade.InventarioEntity;
import br.com.extrator.persistencia.entidade.LocalizacaoCargaEntity;
import br.com.extrator.persistencia.entidade.ManifestoEntity;
import br.com.extrator.persistencia.entidade.SinistroEntity;
import br.com.extrator.suporte.formatacao.FormatadorData;
import br.com.extrator.suporte.mapeamento.MapperUtil;

class IndicadoresGestaoCurrentGapsTest {

    @Test
    void formatadorDataDeveParsearFormatoMmDdComHoraDoDataExport() {
        assertNotNull(FormatadorData.parseOffsetDateTime("03/05/2026 23:59:00"));
    }

    @Test
    void localizacaoCargaMapperDevePreservarPrevisaoQuandoPayloadVemNoFormatoRealDoDataExport() throws Exception {
        final String json = """
            {
              "corporation_sequence_number": 332352,
              "service_at": "03/01/2026 08:00:00",
              "fit_dpn_delivery_prediction_at": "03/05/2026 23:59:00",
              "fit_dyn_drt_nickname": "CPQ - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA",
              "fit_crn_psn_nickname": "SPO - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA",
              "fit_dyn_name": "CAMPINAS - POLO",
              "fit_fln_status": "in_transit",
              "total": "1500.25"
            }
            """;

        final LocalizacaoCargaDTO dto = MapperUtil.sharedJson().readValue(json, LocalizacaoCargaDTO.class);
        final LocalizacaoCargaEntity entity = new LocalizacaoCargaMapper().toEntity(dto);

        assertNotNull(entity);
        assertNotNull(entity.getPredictedDeliveryAt());
        assertEqualsText(
            "CPQ - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA",
            entity.getDestinationBranchNickname()
        );
    }

    @Test
    void manifestoMapperDevePreservarArrayAninhadoDeDescarregamentoEFinishedAtNoFormatoMmDd() throws Exception {
        final String json = """
            {
              "sequence_code": 60509,
              "status": "finished",
              "mft_crn_psn_nickname": "CAS - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA",
              "finished_at": "03/02/2026 16:10:35",
              "mft_mte_unloading_recipient_names": [["CWB - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA"]],
              "mft_mte_delivery_region_names": [["SUL"]]
            }
            """;

        final ManifestoDTO dto = MapperUtil.sharedJson().readValue(json, ManifestoDTO.class);
        final ManifestoEntity entity = new ManifestoMapper().toEntity(dto);

        assertNotNull(entity);
        assertNotNull(entity.getFinishedAt());
        assertNotNull(entity.getUnloadingRecipientNames());
        assertTrue(entity.getUnloadingRecipientNames().contains("CWB - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA"));
        assertTrue(entity.getDeliveryRegionNames().contains("SUL"));
    }

    @Test
    void onboardingDeInventarioESinistrosDeveEstarRegistradoNasConstantesDoDataExport() {
        assertTrue(ConstantesApiDataExport.possuiConfiguracao("inventario"));
        assertTrue(ConstantesApiDataExport.possuiConfiguracao("sinistros"));
    }

    @Test
    void inventarioMapperDeveGerarIdentificadorEPreservarCamposCriticos() throws Exception {
        final String json = """
            {
              "cnr_c_s_fit_corporation_sequence_number": 346841,
              "sequence_code": 99123,
              "started_at": "03/02/2026 08:10:00",
              "finished_at": "03/02/2026 10:35:00",
              "cnr_c_s_fit_invoices_mapping": ["12345"],
              "cnr_c_s_fit_invoices_value": "1500.25",
              "cnr_c_s_fit_real_weight": "12.3",
              "cnr_c_s_fit_total_cubic_volume": "0.8",
              "cnr_c_s_fit_taxed_weight": "14.1",
              "cnr_c_s_fit_dpn_performance_finished_at": "03/03/2026 23:59:00"
            }
            """;

        final InventarioDTO dto = MapperUtil.sharedJson().readValue(json, InventarioDTO.class);
        final InventarioEntity entity = new InventarioMapper().toEntity(dto);

        assertNotNull(entity);
        assertNotNull(entity.getIdentificadorUnico());
        assertEqualsText("346841", String.valueOf(entity.getNumeroMinuta()));
        assertNotNull(entity.getStartedAt());
        assertNotNull(entity.getPerformanceFinishedAt());
    }

    @Test
    void sinistroMapperDeveGerarIdentificadorEPreservarCamposCriticos() throws Exception {
        final String json = """
            {
              "sequence_code": 501,
              "opening_at_date": "03/02/2026",
              "occurrence_at_date": "03/01/2026",
              "icm_fis_fit_corporation_sequence_number": 346841,
              "icm_fis_ioe_number": 778899,
              "insurance_claim_total": "-195.03",
              "icm_ttt_treatment_at": "03/03/2026 12:20:00"
            }
            """;

        final SinistroDTO dto = MapperUtil.sharedJson().readValue(json, SinistroDTO.class);
        final SinistroEntity entity = new SinistroMapper().toEntity(dto);

        assertNotNull(entity);
        assertNotNull(entity.getIdentificadorUnico());
        assertNotNull(entity.getOpeningAtDate());
        assertNotNull(entity.getTreatmentAt());
        assertEqualsText("-195.03", entity.getInsuranceClaimTotal().toPlainString());
    }

    private static void assertEqualsText(final String esperado, final String atual) {
        assertTrue(esperado.equals(atual), "Esperado '" + esperado + "', atual '" + atual + "'");
    }
}
