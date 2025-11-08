package br.com.extrator.modelo.graphql.fretes;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO aninhado para representar o Endereço Principal (MainAddress) de um Remetente/Destinatário.
 * Conforme documentação em docs/descobertas-endpoints/fretes.md linha 88-97.
 */
public class MainAddressDTO {
    @JsonProperty("city")
    private CityDTO city;

    public CityDTO getCity() {
        return city;
    }

    public void setCity(CityDTO city) {
        this.city = city;
    }
}

