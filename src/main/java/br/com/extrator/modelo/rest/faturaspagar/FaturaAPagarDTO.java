package br.com.extrator.modelo.rest.faturaspagar;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.Map;

/**
 * DTO (Data Transfer Object) para representar os dados do cabeçalho de uma Fatura a Pagar,
 * exatamente como recebido do endpoint principal da API (ex: .../billings).
 * Contém campos essenciais mapeados e um contêiner dinâmico para capturar
 * todas as outras propriedades, garantindo resiliência a mudanças na API.
 */
public class FaturaAPagarDTO {

    // --- Campos Essenciais Mapeados ---
    @JsonProperty("id")
    private Long id;

    @JsonProperty("document")
    private String document;

    @JsonProperty("issue_date")
    private String issueDate; // Recebido como String para flexibilidade

    @JsonProperty("due_date")
    private String dueDate; // Recebido como String para flexibilidade

    @JsonProperty("value")
    private String value; // Recebido como String, será convertido depois

    @JsonProperty("type")
    private String type;

    @JsonProperty("receiver")
    private ReceiverDTO receiver;

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

    public String getDocument() {
        return document;
    }

    public void setDocument(final String document) {
        this.document = document;
    }

    public String getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(final String issueDate) {
        this.issueDate = issueDate;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(final String dueDate) {
        this.dueDate = dueDate;
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public ReceiverDTO getReceiver() {
        return receiver;
    }

    public void setReceiver(final ReceiverDTO receiver) {
        this.receiver = receiver;
    }

    public Map<String, Object> getOtherProperties() {
        return otherProperties;
    }
}
