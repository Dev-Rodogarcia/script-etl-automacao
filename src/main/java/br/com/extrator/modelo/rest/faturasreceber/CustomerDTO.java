package br.com.extrator.modelo.rest.faturasreceber;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO auxiliar para representar o objeto aninhado "customer" (cliente)
 * dentro do JSON de uma Fatura a Receber.
 */
public class CustomerDTO {

    @JsonProperty("cnpj")
    private String cnpj;

    @JsonProperty("name")
    private String name;

    // --- Getters e Setters ---

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(final String cnpj) {
        this.cnpj = cnpj;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }
}
