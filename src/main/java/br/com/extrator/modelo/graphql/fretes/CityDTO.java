package br.com.extrator.modelo.graphql.fretes;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO aninhado para representar a Cidade (City) de um Endereço.
 * Conforme documentação em docs/descobertas-endpoints/fretes.md linha 90-96.
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

