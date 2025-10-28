package br.com.extrator.db.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entity (Entidade) que representa uma linha na tabela 'faturas_a_receber' do banco de dados.
 * É o "produto final" da transformação, contendo os dados já estruturados
 * e prontos para serem persistidos. A coluna 'metadata' armazena o JSON
 * completo do objeto original para garantir 100% de completude e resiliência.
 */
public class FaturaAReceberEntity {

    // --- Colunas de Chave e Indexação ---
    private Long id; // Chave Primária
    private String documentNumber; // Chave de Negócio

    // --- Colunas de Dados Essenciais ---
    private LocalDate issueDate;
    private LocalDate dueDate;
    private BigDecimal totalValue;
    private String customerCnpj;
    private String customerName;
    private String invoiceType;

    // --- Coluna de Metadados (Resiliência e Completude) ---
    private String metadata; // Armazena o JSON completo da fatura e seus itens

    // --- Getters e Setters ---

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(final String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(final LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(final LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public BigDecimal getTotalValue() {
        return totalValue;
    }

    public void setTotalValue(final BigDecimal totalValue) {
        this.totalValue = totalValue;
    }

    public String getCustomerCnpj() {
        return customerCnpj;
    }

    public void setCustomerCnpj(final String customerCnpj) {
        this.customerCnpj = customerCnpj;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(final String customerName) {
        this.customerName = customerName;
    }

    public String getInvoiceType() {
        return invoiceType;
    }

    public void setInvoiceType(final String invoiceType) {
        this.invoiceType = invoiceType;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(final String metadata) {
        this.metadata = metadata;
    }
}
