package br.com.extrator.modelo.dataexport.cotacao;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO (Data Transfer Object) para representar um registro de Cotação
 * vindo da API Data Export. Mapeia campos essenciais para colunas dedicadas
 * e captura dinamicamente todos os outros campos do template em um mapa,
 * garantindo resiliência contra futuras alterações no template.
 */
public class CotacaoDTO {

    // --- Campos Essenciais Mapeados Explicitamente ---
    @JsonProperty("sequence_code")
    private Long sequenceCode;

    @JsonProperty("requested_at")
    private String requestedAt;

    @JsonProperty("qoe_qes_total")
    private String totalValue;

    @JsonProperty("qoe_qes_taxed_weight")
    private String taxedWeight;

    @JsonProperty("qoe_qes_invoices_value")
    private String invoicesValue;

    @JsonProperty("qoe_qes_ony_name")
    private String originCity;

    @JsonProperty("qoe_qes_ony_sae_code")
    private String originState;

    @JsonProperty("qoe_qes_diy_name")
    private String destinationCity;

    @JsonProperty("qoe_qes_diy_sae_code")
    private String destinationState;

    @JsonProperty("qoe_cor_document")
    private String customerDocument;

    // --- Contêiner Dinâmico ("Resto") ---
    private final Map<String, Object> otherProperties = new HashMap<>();

    @JsonAnySetter
    public void add(final String key, final Object value) {
        this.otherProperties.put(key, value);
    }

    /**
     * Retorna um mapa contendo todas as propriedades do DTO, combinando
     * os campos declarados explicitamente com os capturados dinamicamente.
     * Essencial para a serialização completa no campo de metadados.
     * @return Um mapa com todos os dados da cotação.
     */
    public Map<String, Object> getAllProperties() {
        final Map<String, Object> allProps = new HashMap<>();
        // Adiciona os campos explícitos ao mapa
        allProps.put("sequence_code", sequenceCode);
        allProps.put("requested_at", requestedAt);
        allProps.put("qoe_qes_total", totalValue);
        allProps.put("qoe_qes_taxed_weight", taxedWeight);
        allProps.put("qoe_qes_invoices_value", invoicesValue);
        allProps.put("qoe_qes_ony_name", originCity);
        allProps.put("qoe_qes_ony_sae_code", originState);
        allProps.put("qoe_qes_diy_name", destinationCity);
        allProps.put("qoe_qes_diy_sae_code", destinationState);
        allProps.put("qoe_cor_document", customerDocument);
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

    public String getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(final String requestedAt) {
        this.requestedAt = requestedAt;
    }

    public String getTotalValue() {
        return totalValue;
    }

    public void setTotalValue(final String totalValue) {
        this.totalValue = totalValue;
    }

    public String getTaxedWeight() {
        return taxedWeight;
    }

    public void setTaxedWeight(final String taxedWeight) {
        this.taxedWeight = taxedWeight;
    }

    public String getInvoicesValue() {
        return invoicesValue;
    }

    public void setInvoicesValue(final String invoicesValue) {
        this.invoicesValue = invoicesValue;
    }

    public String getOriginCity() {
        return originCity;
    }

    public void setOriginCity(final String originCity) {
        this.originCity = originCity;
    }

    public String getOriginState() {
        return originState;
    }

    public void setOriginState(final String originState) {
        this.originState = originState;
    }

    public String getDestinationCity() {
        return destinationCity;
    }

    public void setDestinationCity(final String destinationCity) {
        this.destinationCity = destinationCity;
    }

    public String getDestinationState() {
        return destinationState;
    }

    public void setDestinationState(final String destinationState) {
        this.destinationState = destinationState;
    }

    public String getCustomerDocument() {
        return customerDocument;
    }

    public void setCustomerDocument(final String customerDocument) {
        this.customerDocument = customerDocument;
    }

    @JsonAnyGetter
    public Map<String, Object> getOtherProperties() {
        return otherProperties;
    }
}
