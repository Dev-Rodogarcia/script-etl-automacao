package br.com.extrator.modelo.rest.ocorrencias;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO auxiliar para representar o objeto aninhado "occurrence" (Detalhes da Ocorrência)
 * dentro do JSON principal.
 */
public class OccurrenceDetailsDTO {
    @JsonProperty("code")
    private Integer code;

    @JsonProperty("description")
    private String description;

    // Getters e Setters
    public Integer getCode() { return code; }
    public void setCode(final Integer code) { this.code = code; }
    public String getDescription() { return description; }
    public void setDescription(final String description) { this.description = description; }
}
