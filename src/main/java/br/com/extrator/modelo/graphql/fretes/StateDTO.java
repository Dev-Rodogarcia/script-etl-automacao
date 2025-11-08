package br.com.extrator.modelo.graphql.fretes;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO aninhado para representar o Estado (State) de uma Cidade.
 * Conforme documentação em docs/descobertas-endpoints/fretes.md linha 93-95.
 */
public class StateDTO {
    @JsonProperty("code")
    private String code;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}

