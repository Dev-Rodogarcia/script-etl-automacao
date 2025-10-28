package br.com.extrator.modelo.rest.ocorrencias;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO auxiliar para representar o objeto aninhado "invoice" (Nota Fiscal)
 * dentro do JSON de uma Ocorrência.
 */
public class InvoiceDTO {
    @JsonProperty("id")
    private Long id;

    @JsonProperty("key")
    private String key;

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(final Long id) { this.id = id; }
    public String getKey() { return key; }
    public void setKey(final String key) { this.key = key; }
}
