package br.com.extrator.modelo.graphql.coletas;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO (Data Transfer Object) para representar um "node" de Coleta,
 * conforme retornado pela API GraphQL.
 * Mapeia os campos essenciais solicitados na query e inclui um contêiner
 * dinâmico para garantir a captura de quaisquer outros campos que a query
 * possa vir a retornar no futuro.
 */
public class ColetaNodeDTO {

    // --- Campos Essenciais Mapeados ---
    @JsonProperty("id")
    private String id; // O ID é uma String, não um número.

    @JsonProperty("sequenceCode")
    private Long sequenceCode;

    @JsonProperty("requestDate")
    private String requestDate; // Recebido como String YYYY-MM-DD

    @JsonProperty("serviceDate")
    private String serviceDate; // Recebido como String YYYY-MM-DD

    @JsonProperty("status")
    private String status;

    @JsonProperty("invoicesValue")
    private BigDecimal invoicesValue;

    @JsonProperty("invoicesWeight")
    private BigDecimal invoicesWeight;

    @JsonProperty("invoicesVolumes")
    private Integer invoicesVolumes;

    // --- Contêiner Dinâmico ("Resto") ---
    private final Map<String, Object> otherProperties = new HashMap<>();

    @JsonAnySetter
    public void add(final String key, final Object value) {
        this.otherProperties.put(key, value);
    }

    // --- Getters e Setters ---

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public Long getSequenceCode() {
        return sequenceCode;
    }

    public void setSequenceCode(final Long sequenceCode) {
        this.sequenceCode = sequenceCode;
    }

    public String getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(final String requestDate) {
        this.requestDate = requestDate;
    }

    public String getServiceDate() {
        return serviceDate;
    }

    public void setServiceDate(final String serviceDate) {
        this.serviceDate = serviceDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
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

    public Integer getInvoicesVolumes() {
        return invoicesVolumes;
    }

    public void setInvoicesVolumes(final Integer invoicesVolumes) {
        this.invoicesVolumes = invoicesVolumes;
    }

    public Map<String, Object> getOtherProperties() {
        return otherProperties;
    }
}
