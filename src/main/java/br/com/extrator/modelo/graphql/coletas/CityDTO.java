package br.com.extrator.modelo.graphql.coletas;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO aninhado para representar a Cidade (City) de um Endereço.
 * Conforme documentação em docs/descobertas-endpoints/coletas.md linha 84-90.
 */
public class CityDTO {
    @JsonProperty("name")
    private String name;

    @JsonProperty("state")
    private StateDTO state;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public StateDTO getState() {
        return state;
    }

    public void setState(StateDTO state) {
        this.state = state;
    }
}

