package br.com.extrator.modelo.rest.faturaspagar;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO auxiliar para representar o objeto aninhado "receiver" (recebedor/fornecedor)
 * dentro do JSON de uma Fatura a Pagar.
 * 
 * Fonte: /api/accounting/debit/billings
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReceiverDTO {

    // Fonte: receiver.cnpj (mantido para compatibilidade)
    @JsonProperty("cnpj")
    private String cnpj;

    // Fonte: receiver.cnpjCpf
    @JsonProperty("cnpjCpf")
    private String cnpjCpf;

    // Fonte: receiver.name
    @JsonProperty("name")
    private String name;

    // --- Getters e Setters ---

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(final String cnpj) {
        this.cnpj = cnpj;
    }

    public String getCnpjCpf() {
        return cnpjCpf;
    }

    public void setCnpjCpf(final String cnpjCpf) {
        this.cnpjCpf = cnpjCpf;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }
}
