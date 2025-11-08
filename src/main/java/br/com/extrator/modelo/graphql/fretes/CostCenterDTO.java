package br.com.extrator.modelo.graphql.fretes;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO aninhado para representar o Centro de Custo (CostCenter) de um Frete.
 * Conforme documentação em docs/descobertas-endpoints/fretes.md linha 172.
 */
public class CostCenterDTO {
    @JsonProperty("name")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

