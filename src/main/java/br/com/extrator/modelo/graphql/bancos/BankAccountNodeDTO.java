package br.com.extrator.modelo.graphql.bancos;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO para representar dados de conta bancária via GraphQL.
 * Usado para resolver detalhes do banco via ticketAccountId.
 */
public class BankAccountNodeDTO {
    
    @JsonProperty("id")
    private Integer id;
    
    @JsonProperty("bankName")
    private String bankName;
    
    @JsonProperty("portfolioVariation")
    private String portfolioVariation;
    
    @JsonProperty("customInstruction")
    private String customInstruction;
    
    // Getters e Setters
    public Integer getId() { return id; }
    public void setId(final Integer id) { this.id = id; }
    
    public String getBankName() { return bankName; }
    public void setBankName(final String bankName) { this.bankName = bankName; }
    
    public String getPortfolioVariation() { return portfolioVariation; }
    public void setPortfolioVariation(final String portfolioVariation) { this.portfolioVariation = portfolioVariation; }
    
    public String getCustomInstruction() { return customInstruction; }
    public void setCustomInstruction(final String customInstruction) { this.customInstruction = customInstruction; }
}
