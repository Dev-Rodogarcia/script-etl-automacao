package br.com.extrator.dominio.dataexport.inventario;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

public class InventarioDTO {

    @JsonProperty("cnr_c_s_fit_corporation_sequence_number")
    private Long numeroMinuta;

    @JsonProperty("cnr_c_s_fit_pyr_nickname")
    private String pagadorNome;

    @JsonProperty("cnr_c_s_fit_sdr_nickname")
    private String remetenteNome;

    @JsonProperty("cnr_c_s_fit_sdr_ads_cty_name")
    private String origemCidade;

    @JsonProperty("cnr_c_s_fit_rpt_nickname")
    private String destinatarioNome;

    @JsonProperty("cnr_c_s_fit_rpt_ads_cty_name")
    private String destinoCidade;

    @JsonProperty("cnr_c_s_fit_dyn_name")
    private String regiaoEntrega;

    @JsonProperty("cnr_c_s_fit_dyn_drt_nickname")
    private String filialEntregadora;

    @JsonProperty("sequence_code")
    private Long sequenceCode;

    @JsonProperty("cnr_crn_psn_nickname")
    private String branchNickname;

    @JsonProperty("type")
    private String type;

    @JsonProperty("started_at")
    private String startedAt;

    @JsonProperty("finished_at")
    private String finishedAt;

    @JsonProperty("status")
    private String status;

    @JsonProperty("cnr_cis_eoe_psn_name")
    private String conferenteNome;

    @JsonProperty("cnr_c_s_fit_invoices_mapping")
    private Object invoicesMapping;

    @JsonProperty("cnr_c_s_fit_invoices_value")
    private String invoicesValue;

    @JsonProperty("cnr_c_s_fit_real_weight")
    private String realWeight;

    @JsonProperty("cnr_c_s_fit_total_cubic_volume")
    private String totalCubicVolume;

    @JsonProperty("cnr_c_s_fit_taxed_weight")
    private String taxedWeight;

    @JsonProperty("cnr_c_s_fit_invoices_volumes")
    private Integer invoicesVolumes;

    @JsonProperty("cnr_c_s_read_volumes")
    private Integer readVolumes;

    @JsonProperty("cnr_c_s_fit_dpn_delivery_prediction_at")
    private String predictedDeliveryAt;

    @JsonProperty("cnr_c_s_fit_dpn_performance_finished_at")
    private String performanceFinishedAt;

    @JsonProperty("cnr_c_s_fit_fte_lce_occurrence_at")
    private String ultimaOcorrenciaAt;

    @JsonProperty("cnr_c_s_fit_fte_lce_ore_description")
    private String ultimaOcorrenciaDescricao;

    private final Map<String, Object> otherProperties = new HashMap<>();

    @JsonAnySetter
    public void add(final String key, final Object value) {
        otherProperties.put(key, value);
    }

    @JsonAnyGetter
    public Map<String, Object> getOtherProperties() {
        return otherProperties;
    }

    public Map<String, Object> getAllProperties() {
        final Map<String, Object> all = new LinkedHashMap<>();
        all.put("cnr_c_s_fit_corporation_sequence_number", numeroMinuta);
        all.put("cnr_c_s_fit_pyr_nickname", pagadorNome);
        all.put("cnr_c_s_fit_sdr_nickname", remetenteNome);
        all.put("cnr_c_s_fit_sdr_ads_cty_name", origemCidade);
        all.put("cnr_c_s_fit_rpt_nickname", destinatarioNome);
        all.put("cnr_c_s_fit_rpt_ads_cty_name", destinoCidade);
        all.put("cnr_c_s_fit_dyn_name", regiaoEntrega);
        all.put("cnr_c_s_fit_dyn_drt_nickname", filialEntregadora);
        all.put("sequence_code", sequenceCode);
        all.put("cnr_crn_psn_nickname", branchNickname);
        all.put("type", type);
        all.put("started_at", startedAt);
        all.put("finished_at", finishedAt);
        all.put("status", status);
        all.put("cnr_cis_eoe_psn_name", conferenteNome);
        all.put("cnr_c_s_fit_invoices_mapping", invoicesMapping);
        all.put("cnr_c_s_fit_invoices_value", invoicesValue);
        all.put("cnr_c_s_fit_real_weight", realWeight);
        all.put("cnr_c_s_fit_total_cubic_volume", totalCubicVolume);
        all.put("cnr_c_s_fit_taxed_weight", taxedWeight);
        all.put("cnr_c_s_fit_invoices_volumes", invoicesVolumes);
        all.put("cnr_c_s_read_volumes", readVolumes);
        all.put("cnr_c_s_fit_dpn_delivery_prediction_at", predictedDeliveryAt);
        all.put("cnr_c_s_fit_dpn_performance_finished_at", performanceFinishedAt);
        all.put("cnr_c_s_fit_fte_lce_occurrence_at", ultimaOcorrenciaAt);
        all.put("cnr_c_s_fit_fte_lce_ore_description", ultimaOcorrenciaDescricao);
        all.putAll(otherProperties);
        return all;
    }

    public Long getNumeroMinuta() {
        return numeroMinuta;
    }

    public void setNumeroMinuta(final Long numeroMinuta) {
        this.numeroMinuta = numeroMinuta;
    }

    public String getPagadorNome() {
        return pagadorNome;
    }

    public void setPagadorNome(final String pagadorNome) {
        this.pagadorNome = pagadorNome;
    }

    public String getRemetenteNome() {
        return remetenteNome;
    }

    public void setRemetenteNome(final String remetenteNome) {
        this.remetenteNome = remetenteNome;
    }

    public String getOrigemCidade() {
        return origemCidade;
    }

    public void setOrigemCidade(final String origemCidade) {
        this.origemCidade = origemCidade;
    }

    public String getDestinatarioNome() {
        return destinatarioNome;
    }

    public void setDestinatarioNome(final String destinatarioNome) {
        this.destinatarioNome = destinatarioNome;
    }

    public String getDestinoCidade() {
        return destinoCidade;
    }

    public void setDestinoCidade(final String destinoCidade) {
        this.destinoCidade = destinoCidade;
    }

    public String getRegiaoEntrega() {
        return regiaoEntrega;
    }

    public void setRegiaoEntrega(final String regiaoEntrega) {
        this.regiaoEntrega = regiaoEntrega;
    }

    public String getFilialEntregadora() {
        return filialEntregadora;
    }

    public void setFilialEntregadora(final String filialEntregadora) {
        this.filialEntregadora = filialEntregadora;
    }

    public Long getSequenceCode() {
        return sequenceCode;
    }

    public void setSequenceCode(final Long sequenceCode) {
        this.sequenceCode = sequenceCode;
    }

    public String getBranchNickname() {
        return branchNickname;
    }

    public void setBranchNickname(final String branchNickname) {
        this.branchNickname = branchNickname;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public String getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(final String startedAt) {
        this.startedAt = startedAt;
    }

    public String getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(final String finishedAt) {
        this.finishedAt = finishedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public String getConferenteNome() {
        return conferenteNome;
    }

    public void setConferenteNome(final String conferenteNome) {
        this.conferenteNome = conferenteNome;
    }

    public Object getInvoicesMapping() {
        return invoicesMapping;
    }

    public void setInvoicesMapping(final Object invoicesMapping) {
        this.invoicesMapping = invoicesMapping;
    }

    public String getInvoicesValue() {
        return invoicesValue;
    }

    public void setInvoicesValue(final String invoicesValue) {
        this.invoicesValue = invoicesValue;
    }

    public String getRealWeight() {
        return realWeight;
    }

    public void setRealWeight(final String realWeight) {
        this.realWeight = realWeight;
    }

    public String getTotalCubicVolume() {
        return totalCubicVolume;
    }

    public void setTotalCubicVolume(final String totalCubicVolume) {
        this.totalCubicVolume = totalCubicVolume;
    }

    public String getTaxedWeight() {
        return taxedWeight;
    }

    public void setTaxedWeight(final String taxedWeight) {
        this.taxedWeight = taxedWeight;
    }

    public Integer getInvoicesVolumes() {
        return invoicesVolumes;
    }

    public void setInvoicesVolumes(final Integer invoicesVolumes) {
        this.invoicesVolumes = invoicesVolumes;
    }

    public Integer getReadVolumes() {
        return readVolumes;
    }

    public void setReadVolumes(final Integer readVolumes) {
        this.readVolumes = readVolumes;
    }

    public String getPredictedDeliveryAt() {
        return predictedDeliveryAt;
    }

    public void setPredictedDeliveryAt(final String predictedDeliveryAt) {
        this.predictedDeliveryAt = predictedDeliveryAt;
    }

    public String getPerformanceFinishedAt() {
        return performanceFinishedAt;
    }

    public void setPerformanceFinishedAt(final String performanceFinishedAt) {
        this.performanceFinishedAt = performanceFinishedAt;
    }

    public String getUltimaOcorrenciaAt() {
        return ultimaOcorrenciaAt;
    }

    public void setUltimaOcorrenciaAt(final String ultimaOcorrenciaAt) {
        this.ultimaOcorrenciaAt = ultimaOcorrenciaAt;
    }

    public String getUltimaOcorrenciaDescricao() {
        return ultimaOcorrenciaDescricao;
    }

    public void setUltimaOcorrenciaDescricao(final String ultimaOcorrenciaDescricao) {
        this.ultimaOcorrenciaDescricao = ultimaOcorrenciaDescricao;
    }
}
