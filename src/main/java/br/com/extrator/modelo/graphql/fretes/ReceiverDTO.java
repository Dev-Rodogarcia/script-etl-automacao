package br.com.extrator.modelo.graphql.fretes;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO aninhado para representar o Destinatário (Receiver) de um Frete.
 * Conforme documentação em docs/descobertas-endpoints/fretes.md linha 101-112.
 */
public class ReceiverDTO {
    @JsonProperty("id")
    private Long id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("cnpj")
    private String cnpj;

    @JsonProperty("cpf")
    private String cpf;

    @JsonProperty("inscricaoEstadual")
    private String inscricaoEstadual;

    @JsonProperty("mainAddress")
    private MainAddressDTO mainAddress;

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

    public String getInscricaoEstadual() {
        return inscricaoEstadual;
    }

    public void setInscricaoEstadual(String inscricaoEstadual) {
        this.inscricaoEstadual = inscricaoEstadual;
    }

    public MainAddressDTO getMainAddress() {
        return mainAddress;
    }

    public void setMainAddress(MainAddressDTO mainAddress) {
        this.mainAddress = mainAddress;
    }
}
