package br.com.extrator.modelo.rest.faturaspagar;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO auxiliar para representar o objeto aninhado "receiver" (recebedor/fornecedor)
 * dentro do JSON de uma Fatura a Pagar.
 */
public class ReceiverDTO {

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
