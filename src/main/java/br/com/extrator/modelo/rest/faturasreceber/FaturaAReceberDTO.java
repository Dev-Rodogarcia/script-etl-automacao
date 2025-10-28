package br.com.extrator.modelo.rest.faturasreceber;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.Map;

/**
 * DTO (Data Transfer Object) para representar os dados de uma Fatura a Receber,
 * exatamente como recebido do endpoint da API.
 * Esta classe é projetada para ser resiliente: ela mapeia campos essenciais
 * e captura dinamicamente todas as outras propriedades, incluindo a lista
 * de parcelas ('installments'), em um contêiner genérico.
 */
public class FaturaAReceberDTO {

    // --- Campos Essenciais Mapeados ---
    @JsonProperty("id")
    private Long id;

    @JsonProperty("document")
    private String document;

    @JsonProperty("issue_date")
    private String issueDate; // Recebido como String para flexibilidade no parsing

    @JsonProperty("due_date")
    private String dueDate; // Recebido como String para flexibilidade no parsing

    @JsonProperty("value")
    private String value; // Recebido como String, será convertido para BigDecimal no Mapper

    @JsonProperty("type")
    private String type;

    @JsonProperty("customer")
    private CustomerDTO customer;

    // --- Contêiner Dinâmico ("Resto") ---
    // Este mapa capturará todos os campos não declarados explicitamente,
    // como 'installments', 'comments', etc.
    private Map<String, Object> otherProperties = new HashMap<>();

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

    public String getDocument() {
        return document;
    }

    public void setDocument(String document) {
        this.document = document;
    }

    public String getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(String issueDate) {
        this.issueDate = issueDate;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public CustomerDTO getCustomer() {
        return customer;
    }

    public void setCustomer(CustomerDTO customer) {
        this.customer = customer;
    }

    public Map<String, Object> getOtherProperties() {
        return otherProperties;
    }
}
