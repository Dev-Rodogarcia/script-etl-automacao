package br.com.extrator.modelo.graphql.fretes;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO aninhado para representar a Filial (Corporation) de um Frete.
 * Conforme documentação em docs/descobertas-endpoints/fretes.md linha 152.
 */
public class CorporationDTO {
    @JsonProperty("name")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

