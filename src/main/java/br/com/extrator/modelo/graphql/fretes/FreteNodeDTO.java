package br.com.extrator.modelo.graphql.fretes;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO (Data Transfer Object) para representar um "node" de Frete,
 * conforme retornado pela API GraphQL. Mapeia os campos essenciais
 * e inclui um contêiner dinâmico para capturar todas as outras
 * propriedades, garantindo resiliência e completude.
 */
public class FreteNodeDTO {

    // --- Campos Essenciais Mapeados ---
    @JsonProperty("id")
    private Long id;

    @JsonProperty("serviceAt")
    private String serviceAt; // Recebe como String para ser convertido para OffsetDateTime

    @JsonProperty("createdAt")
    private String createdAt; // Recebe como String para ser convertido para OffsetDateTime

    @JsonProperty("status")
    private String status;

    @JsonProperty("modal")
    private String modal;

    @JsonProperty("type")
    private String type;

    @JsonProperty("total")
    private BigDecimal totalValue;

    @JsonProperty("invoicesValue")
    private BigDecimal invoicesValue;

    @JsonProperty("invoicesWeight")
    private BigDecimal invoicesWeight;

    @JsonProperty("corporationId")
    private Long corporationId;

    @JsonProperty("destinationCityId")
    private Long destinationCityId;

    @JsonProperty("deliveryPredictionDate")
    private String deliveryPredictionDate; // Recebe como String para ser convertido para LocalDate

    // --- Contêiner Dinâmico ("Resto") ---
    private final Map<String, Object> otherProperties = new HashMap<>();

    @JsonAnySetter
    public void add(final String key, final Object value) {
        this.otherProperties.put(key, value);
    }

    // --- Getters e Setters ---

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getServiceAt() {
        return serviceAt;
    }

    public void setServiceAt(final String serviceAt) {
        this.serviceAt = serviceAt;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final String createdAt) {
        this.createdAt = createdAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public String getModal() {
        return modal;
    }

    public void setModal(final String modal) {
        this.modal = modal;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public BigDecimal getTotalValue() {
        return totalValue;
    }

    public void setTotalValue(final BigDecimal totalValue) {
        this.totalValue = totalValue;
    }

    public BigDecimal getInvoicesValue() {
        return invoicesValue;
    }

    public void setInvoicesValue(final BigDecimal invoicesValue) {
        this.invoicesValue = invoicesValue;
    }

    public BigDecimal getInvoicesWeight() {
        return invoicesWeight;
    }

    public void setInvoicesWeight(final BigDecimal invoicesWeight) {
        this.invoicesWeight = invoicesWeight;
    }

    public Long getCorporationId() {
        return corporationId;
    }

    public void setCorporationId(final Long corporationId) {
        this.corporationId = corporationId;
    }

    public Long getDestinationCityId() {
        return destinationCityId;
    }

    public void setDestinationCityId(final Long destinationCityId) {
        this.destinationCityId = destinationCityId;
    }

    public String getDeliveryPredictionDate() {
        return deliveryPredictionDate;
    }

    public void setDeliveryPredictionDate(final String deliveryPredictionDate) {
        this.deliveryPredictionDate = deliveryPredictionDate;
    }

    @JsonAnyGetter
    public Map<String, Object> getOtherProperties() {
        return otherProperties;
    }
}
