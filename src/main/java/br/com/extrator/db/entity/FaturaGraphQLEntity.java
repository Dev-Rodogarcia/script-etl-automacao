package br.com.extrator.db.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

public class FaturaGraphQLEntity {
    private Long id;
    private String document;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private BigDecimal value;
    private Long customerId;
    private String customerName;
    private String customerCnpj;
    private String metadata;

    public Long getId() { return id; }
    public void setId(final Long id) { this.id = id; }
    public String getDocument() { return document; }
    public void setDocument(final String document) { this.document = document; }
    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(final LocalDate issueDate) { this.issueDate = issueDate; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(final LocalDate dueDate) { this.dueDate = dueDate; }
    public BigDecimal getValue() { return value; }
    public void setValue(final BigDecimal value) { this.value = value; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(final Long customerId) { this.customerId = customerId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(final String customerName) { this.customerName = customerName; }
    public String getCustomerCnpj() { return customerCnpj; }
    public void setCustomerCnpj(final String customerCnpj) { this.customerCnpj = customerCnpj; }
    public String getMetadata() { return metadata; }
    public void setMetadata(final String metadata) { this.metadata = metadata; }
}
