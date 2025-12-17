package br.com.extrator.modelo.graphql.faturas;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CreditCustomerBillingNodeDTO {
    @JsonProperty("id")
    private Long id;

    @JsonProperty("document")
    private String document;

    @JsonProperty("issueDate")
    private String issueDate;

    @JsonProperty("dueDate")
    private String dueDate;

    @JsonProperty("value")
    private BigDecimal value;

    public static class CustomerDTO {
        @JsonProperty("id")
        private String id;
        @JsonProperty("nickname")
        private String nickname;
        public static class PersonDTO {
            @JsonProperty("name")
            private String name;
            @JsonProperty("nickname")
            private String nickname;
            @JsonProperty("cnpj")
            private String cnpj;
            public String getName() { return name; }
            public void setName(final String name) { this.name = name; }
            public String getNickname() { return nickname; }
            public void setNickname(final String nickname) { this.nickname = nickname; }
            public String getCnpj() { return cnpj; }
            public void setCnpj(final String cnpj) { this.cnpj = cnpj; }
        }
        @JsonProperty("person")
        private PersonDTO person;
        @JsonProperty("name")
        private String name;
        @JsonProperty("cnpj")
        private String cnpj;
        public String getId() { return id; }
        public void setId(final String id) { this.id = id; }
        public String getNickname() { return nickname; }
        public void setNickname(final String nickname) { this.nickname = nickname; }
        public PersonDTO getPerson() { return person; }
        public void setPerson(final PersonDTO person) { this.person = person; }
        public String getName() { return name; }
        public void setName(final String name) { this.name = name; }
        public String getCnpj() { return cnpj; }
        public void setCnpj(final String cnpj) { this.cnpj = cnpj; }
    }

    @JsonProperty("customer")
    private CustomerDTO customer;

    public Long getId() { return id; }
    public void setId(final Long id) { this.id = id; }
    public String getDocument() { return document; }
    public void setDocument(final String document) { this.document = document; }
    public String getIssueDate() { return issueDate; }
    public void setIssueDate(final String issueDate) { this.issueDate = issueDate; }
    public String getDueDate() { return dueDate; }
    public void setDueDate(final String dueDate) { this.dueDate = dueDate; }
    public BigDecimal getValue() { return value; }
    public void setValue(final BigDecimal value) { this.value = value; }
    public CustomerDTO getCustomer() { return customer; }
    public void setCustomer(final CustomerDTO customer) { this.customer = customer; }
}
