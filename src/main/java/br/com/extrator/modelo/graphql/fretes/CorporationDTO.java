package br.com.extrator.modelo.graphql.fretes;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO aninhado para representar a Filial (Corporation) de um Frete.
 * Conforme documentação em docs/descobertas-endpoints/fretes.md linha 152.
 */
public class CorporationDTO {
    @JsonProperty("name")
    private String name;

    @JsonProperty("nickname")
    private String nickname;

    @JsonProperty("cnpj")
    private String cnpj;

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(final String nickname) {
        this.nickname = nickname;
    }

    public String getCnpj() { return cnpj; }
    public void setCnpj(final String cnpj) { this.cnpj = cnpj; }
}
