package br.com.extrator.modelo.graphql.fretes;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO aninhado para representar o Remetente (Sender) de um Frete.
 * Conforme documentação em docs/descobertas-endpoints/fretes.md linha 87-98.
 */
public class SenderDTO {
    @JsonProperty("id")
    private Long id;

    @JsonProperty("name")
    private String name;

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

    public MainAddressDTO getMainAddress() {
        return mainAddress;
    }

    public void setMainAddress(MainAddressDTO mainAddress) {
        this.mainAddress = mainAddress;
    }
}

