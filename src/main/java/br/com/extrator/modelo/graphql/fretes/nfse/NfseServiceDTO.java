package br.com.extrator.modelo.graphql.fretes.nfse;

import com.fasterxml.jackson.annotation.JsonProperty;

public class NfseServiceDTO {
    @JsonProperty("id")
    private Long id;

    @JsonProperty("description")
    private String description;

    public Long getId() { return id; }
    public void setId(final Long id) { this.id = id; }

    public String getDescription() { return description; }
    public void setDescription(final String description) { this.description = description; }
}
