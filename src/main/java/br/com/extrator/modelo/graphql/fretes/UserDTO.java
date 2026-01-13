package br.com.extrator.modelo.graphql.fretes;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO aninhado para representar o Usuário (User) de um Frete.
 * Conforme documentação em docs/descobertas-endpoints/fretes.md linha 173.
 */
public class UserDTO {
    @JsonProperty("name")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

