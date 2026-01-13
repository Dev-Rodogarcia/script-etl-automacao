package br.com.extrator.modelo.graphql.coletas;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO aninhado para representar o Usuário/Motorista (User) de uma Coleta.
 * Conforme documentação em docs/descobertas-endpoints/coletas.md linha 92-95.
 */
public class UserDTO {
    @JsonProperty("id")
    private Long id;

    @JsonProperty("name")
    private String name;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

