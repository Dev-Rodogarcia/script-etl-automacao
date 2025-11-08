package br.com.extrator.modelo.graphql.fretes;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO aninhado para representar a Tabela de Preço (CustomerPriceTable) de um Frete.
 * Conforme documentação em docs/descobertas-endpoints/fretes.md linha 170.
 */
public class CustomerPriceTableDTO {
    @JsonProperty("name")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

