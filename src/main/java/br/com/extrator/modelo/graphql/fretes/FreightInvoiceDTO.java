package br.com.extrator.modelo.graphql.fretes;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO aninhado para representar uma Nota Fiscal (FreightInvoice) de um Frete.
 * Conforme documentação em docs/descobertas-endpoints/fretes.md linha 162.
 * Nota: A API retorna um array de FreightInvoice, mas para mapeamento do CSV
 * precisamos apenas do campo 'number'.
 */
public class FreightInvoiceDTO {
    @JsonProperty("number")
    private String number;

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }
}

