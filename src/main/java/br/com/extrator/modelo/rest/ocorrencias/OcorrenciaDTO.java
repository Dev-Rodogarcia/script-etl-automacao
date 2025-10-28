package br.com.extrator.modelo.rest.ocorrencias;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.Map;

/**
 * DTO (Data Transfer Object) principal para representar os dados de uma Ocorrência,
 * conforme recebido do endpoint da API. A estrutura espelha o JSON aninhado,
 * contendo DTOs para os objetos 'invoice', 'freight' e 'occurrence'.
 * Inclui um contêiner dinâmico para capturar quaisquer novos campos futuros.
 */
public class OcorrenciaDTO {

    // --- Campos Essenciais Mapeados ---
    @JsonProperty("id")
    private Long id;

    @JsonProperty("occurrence_at")
    private String occurrenceAt; // Recebido como String para ser convertido para OffsetDateTime

    @JsonProperty("comments")
    private String comments;

    // --- Objetos Aninhados ---
    @JsonProperty("invoice")
    private InvoiceDTO invoice;

    @JsonProperty("freight")
    private FreightDTO freight;

    @JsonProperty("occurrence")
    private OccurrenceDetailsDTO occurrenceDetails;

    // --- Contêiner Dinâmico ("Resto") ---
    private final Map<String, Object> otherProperties = new HashMap<>();

    @JsonAnySetter
    public void add(String key, Object value) {
        this.otherProperties.put(key, value);
    }

    // --- Getters e Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOccurrenceAt() {
        return occurrenceAt;
    }

    public void setOccurrenceAt(String occurrenceAt) {
        this.occurrenceAt = occurrenceAt;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public InvoiceDTO getInvoice() {
        return invoice;
    }

    public void setInvoice(InvoiceDTO invoice) {
        this.invoice = invoice;
    }

    public FreightDTO getFreight() {
        return freight;
    }

    public void setFreight(FreightDTO freight) {
        this.freight = freight;
    }

    public OccurrenceDetailsDTO getOccurrenceDetails() {
        return occurrenceDetails;
    }

    public void setOccurrenceDetails(OccurrenceDetailsDTO occurrenceDetails) {
        this.occurrenceDetails = occurrenceDetails;
    }

    public Map<String, Object> getOtherProperties() {
        return otherProperties;
    }
}
