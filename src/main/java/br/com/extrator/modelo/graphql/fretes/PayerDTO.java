package br.com.extrator.modelo.graphql.fretes;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO aninhado para representar o Pagador (Payer) de um Frete.
 * Conforme documentação em docs/descobertas-endpoints/fretes.md linha 81-84.
 */
public class PayerDTO {
    @JsonProperty("id")
    private Long id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("cnpj")
    private String cnpj;

    @JsonProperty("cpf")
    private String cpf;

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

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }
}
