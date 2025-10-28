package br.com.extrator.modelo.dataexport.manifestos;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO (Data Transfer Object) para representar um registro de Manifesto
 * vindo do template da API Data Export. Mapeia campos essenciais para colunas dedicadas
 * e captura dinamicamente todos os outros campos em um mapa, garantindo
 * resiliência contra futuras alterações no template.
 */
public class ManifestoDTO {

    // --- Campos Essenciais Mapeados Explicitamente ---
    @JsonProperty("sequence_code")
    private Long sequenceCode;

    @JsonProperty("status")
    private String status;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("departured_at")
    private String departuredAt;

    @JsonProperty("finished_at")
    private String finishedAt;

    @JsonProperty("total_cost")
    private String totalCost;

    @JsonProperty("traveled_km")
    private Integer traveledKm;

    @JsonProperty("mft_vie_license_plate")
    private String vehiclePlate;

    @JsonProperty("mft_mdr_iil_name")
    private String driverName;

    @JsonProperty("mft_crn_psn_nickname")
    private String originBranch;

    @JsonProperty("mdfe_status")
    private String mdfeStatus;

    // --- Contêiner Dinâmico ("Resto") ---
    private final Map<String, Object> otherProperties = new HashMap<>();

    @JsonAnySetter
    public void add(final String key, final Object value) {
        this.otherProperties.put(key, value);
    }

    /**
     * Retorna um mapa contendo todas as propriedades do DTO, combinando
     * os campos declarados explicitamente com os capturados dinamicamente.
     * @return Um mapa com todos os dados do manifesto.
     */
    public Map<String, Object> getAllProperties() {
        final Map<String, Object> allProps = new LinkedHashMap<>();
        // Adiciona os campos explícitos ao mapa
        allProps.put("sequence_code", sequenceCode);
        allProps.put("status", status);
        allProps.put("created_at", createdAt);
        allProps.put("departured_at", departuredAt);
        allProps.put("finished_at", finishedAt);
        allProps.put("total_cost", totalCost);
        allProps.put("traveled_km", traveledKm);
        allProps.put("mft_vie_license_plate", vehiclePlate);
        allProps.put("mft_mdr_iil_name", driverName);
        allProps.put("mft_crn_psn_nickname", originBranch);
        allProps.put("mdfe_status", mdfeStatus);
        // Adiciona todos os outros campos capturados
        allProps.putAll(otherProperties);
        return allProps;
    }

    // --- Getters e Setters ---

    public Long getSequenceCode() {
        return sequenceCode;
    }

    public void setSequenceCode(final Long sequenceCode) {
        this.sequenceCode = sequenceCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final String createdAt) {
        this.createdAt = createdAt;
    }

    public String getDeparturedAt() {
        return departuredAt;
    }

    public void setDeparturedAt(final String departuredAt) {
        this.departuredAt = departuredAt;
    }

    public String getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(final String finishedAt) {
        this.finishedAt = finishedAt;
    }

    public String getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(final String totalCost) {
        this.totalCost = totalCost;
    }

    public Integer getTraveledKm() {
        return traveledKm;
    }

    public void setTraveledKm(final Integer traveledKm) {
        this.traveledKm = traveledKm;
    }

    public String getVehiclePlate() {
        return vehiclePlate;
    }

    public void setVehiclePlate(final String vehiclePlate) {
        this.vehiclePlate = vehiclePlate;
    }

    public String getDriverName() {
        return driverName;
    }

    public void setDriverName(final String driverName) {
        this.driverName = driverName;
    }

    public String getOriginBranch() {
        return originBranch;
    }

    public void setOriginBranch(final String originBranch) {
        this.originBranch = originBranch;
    }

    public String getMdfeStatus() {
        return mdfeStatus;
    }

    public void setMdfeStatus(final String mdfeStatus) {
        this.mdfeStatus = mdfeStatus;
    }

    public Map<String, Object> getOtherProperties() {
        return otherProperties;
    }
}
