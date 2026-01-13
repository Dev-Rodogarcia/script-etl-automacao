package br.com.extrator.modelo.graphql.coletas;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO aninhado para representar o Cliente (Customer) de uma Coleta.
 * Conforme documentação em docs/descobertas-endpoints/coletas.md linha 77-80.
 */
public class CustomerDTO {
    @JsonProperty("id")
    private Long id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("cnpj")
    private String cnpj;

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

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }
}
