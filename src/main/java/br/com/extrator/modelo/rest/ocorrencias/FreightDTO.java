package br.com.extrator.modelo.rest.ocorrencias;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO auxiliar para representar o objeto aninhado "freight" (Frete/CT-e)
 * dentro do JSON de uma Ocorrência.
 */
public class FreightDTO {
    @JsonProperty("id")
    private Long id;

    @JsonProperty("cte_key")
    private String cteKey;

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(final Long id) { this.id = id; }
    public String getCteKey() { return cteKey; }
    public void setCteKey(final String cteKey) { this.cteKey = cteKey; }
}
