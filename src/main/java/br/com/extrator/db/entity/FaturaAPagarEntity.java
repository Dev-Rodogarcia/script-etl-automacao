package br.com.extrator.db.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entity (Entidade) que representa uma linha na tabela 'faturas_a_pagar' do banco de dados.
 * Esta classe é o "produto final" da nossa linha de montagem, contendo os dados
 * já limpos, convertidos e prontos para serem persistidos pelo Repository.
 */
public class FaturaAPagarEntity {

    // --- Colunas de Chave e Indexação ---
    private Long id; // Chave Primária
    private String documentNumber; // Chave de Negócio

    // --- Colunas de Dados Essenciais ---
    private LocalDate issueDate;
    private LocalDate dueDate;
    private BigDecimal totalValue;
    private String receiverCnpj;
    private String receiverName;
    private String invoiceType;

    // --- Colunas de Metadados (Resiliência e Completude) ---
    private String headerMetadata; // Armazena o JSON completo do cabeçalho
    private String installmentsMetadata; // Armazena o JSON completo dos títulos/parcelas

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

    public String getReceiverCnpj() {
        return receiverCnpj;
    }

    public void setReceiverCnpj(final String receiverCnpj) {
        this.receiverCnpj = receiverCnpj;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(final String receiverName) {
        this.receiverName = receiverName;
    }

    public String getInvoiceType() {
        return invoiceType;
    }

    public void setInvoiceType(final String invoiceType) {
        this.invoiceType = invoiceType;
    }

    public String getHeaderMetadata() {
        return headerMetadata;
    }

    public void setHeaderMetadata(final String headerMetadata) {
        this.headerMetadata = headerMetadata;
    }

    public String getInstallmentsMetadata() {
        return installmentsMetadata;
    }

    public void setInstallmentsMetadata(final String installmentsMetadata) {
        this.installmentsMetadata = installmentsMetadata;
    }
}
