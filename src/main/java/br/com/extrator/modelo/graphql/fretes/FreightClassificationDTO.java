package br.com.extrator.modelo.graphql.fretes;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO aninhado para representar a Classificação (FreightClassification) de um Frete.
 * Conforme documentação em docs/descobertas-endpoints/fretes.md linha 171.
 */
public class FreightClassificationDTO {
    @JsonProperty("name")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

