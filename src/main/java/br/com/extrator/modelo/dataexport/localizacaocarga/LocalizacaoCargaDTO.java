package br.com.extrator.modelo.dataexport.localizacaocarga;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO (Data Transfer Object) para representar um registro de Localização de Carga
 * vindo do template da API Data Export. Mapeia campos essenciais para colunas dedicadas
 * e captura dinamicamente todos os outros campos em um mapa, garantindo
 * resiliência contra futuras alterações no template.
 */
public class LocalizacaoCargaDTO {

    // --- Campos Essenciais Mapeados Explicitamente ---
    @JsonProperty("corporation_sequence_number")
    private Long sequenceNumber;

    @JsonProperty("service_at")
    private String serviceAt;

    @JsonProperty("fit_fln_status")
    private String status;

    @JsonProperty("total")
    private String totalValue;

    @JsonProperty("fit_dpn_delivery_prediction_at")
    private String predictedDeliveryAt;

    @JsonProperty("fit_o_n_name")
    private String originLocationName;

    @JsonProperty("fit_dyn_name")
    private String destinationLocationName;

    // --- Contêiner Dinâmico ("Resto") ---
    private final Map<String, Object> otherProperties = new HashMap<>();

    @JsonAnySetter
    public void add(final String key, final Object value) {
        this.otherProperties.put(key, value);
    }

    /**
     * Retorna um mapa contendo todas as propriedades do DTO, combinando
     * os campos declarados explicitamente com os capturados dinamicamente.
     * @return Um mapa com todos os dados do registro.
     */
    public Map<String, Object> getAllProperties() {
        // Usar LinkedHashMap para manter a ordem original dos campos, se desejado
        final Map<String, Object> allProps = new LinkedHashMap<>();
        allProps.put("corporation_sequence_number", sequenceNumber);
        allProps.put("service_at", serviceAt);
        allProps.put("fit_fln_status", status);
        allProps.put("total", totalValue);
        allProps.put("fit_dpn_delivery_prediction_at", predictedDeliveryAt);
        allProps.put("fit_o_n_name", originLocationName);
        allProps.put("fit_dyn_name", destinationLocationName);
        allProps.putAll(otherProperties);
        return allProps;
    }

    // --- Getters e Setters ---

    public Long getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(final Long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public String getServiceAt() {
        return serviceAt;
    }

    public void setServiceAt(final String serviceAt) {
        this.serviceAt = serviceAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public String getTotalValue() {
        return totalValue;
    }

    public void setTotalValue(final String totalValue) {
        this.totalValue = totalValue;
    }

    public String getPredictedDeliveryAt() {
        return predictedDeliveryAt;
    }

    public void setPredictedDeliveryAt(final String predictedDeliveryAt) {
        this.predictedDeliveryAt = predictedDeliveryAt;
    }

    public String getOriginLocationName() {
        return originLocationName;
    }

    public void setOriginLocationName(final String originLocationName) {
        this.originLocationName = originLocationName;
    }

    public String getDestinationLocationName() {
        return destinationLocationName;
    }

    public void setDestinationLocationName(final String destinationLocationName) {
        this.destinationLocationName = destinationLocationName;
    }

    public Map<String, Object> getOtherProperties() {
        return otherProperties;
    }
}
