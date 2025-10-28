package br.com.extrator.db.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Entity (Entidade) que representa uma linha na tabela 'cotacoes' do banco de dados.
 * É o "produto final" da transformação, contendo os dados já estruturados
 * e prontos para serem persistidos. A coluna 'metadata' armazena o JSON
 * completo do objeto original para garantir 100% de completude e resiliência.
 */
public class CotacaoEntity {

    // --- Coluna de Chave Primária ---
    private Long sequenceCode;

    // --- Colunas Essenciais para Indexação e Relatórios ---
    private OffsetDateTime requestedAt;
    private BigDecimal totalValue;
    private BigDecimal taxedWeight;
    private BigDecimal invoicesValue;
    private String originCity;
    private String originState;
    private String destinationCity;
    private String destinationState;
    private String customerDoc;

    // --- Coluna de Metadados ---
    private String metadata;

    // --- Getters e Setters ---

    public Long getSequenceCode() {
        return sequenceCode;
    }

    public void setSequenceCode(final Long sequenceCode) {
        this.sequenceCode = sequenceCode;
    }

    public OffsetDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(final OffsetDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public BigDecimal getTotalValue() {
        return totalValue;
    }

    public void setTotalValue(final BigDecimal totalValue) {
        this.totalValue = totalValue;
    }

    public BigDecimal getTaxedWeight() {
        return taxedWeight;
    }

    public void setTaxedWeight(final BigDecimal taxedWeight) {
        this.taxedWeight = taxedWeight;
    }

    public BigDecimal getInvoicesValue() {
        return invoicesValue;
    }

    public void setInvoicesValue(final BigDecimal invoicesValue) {
        this.invoicesValue = invoicesValue;
    }

    public String getOriginCity() {
        return originCity;
    }

    public void setOriginCity(final String originCity) {
        this.originCity = originCity;
    }

    public String getOriginState() {
        return originState;
    }

    public void setOriginState(final String originState) {
        this.originState = originState;
    }

    public String getDestinationCity() {
        return destinationCity;
    }

    public void setDestinationCity(final String destinationCity) {
        this.destinationCity = destinationCity;
    }

    public String getDestinationState() {
        return destinationState;
    }

    public void setDestinationState(final String destinationState) {
        this.destinationState = destinationState;
    }

    public String getCustomerDoc() {
        return customerDoc;
    }

    public void setCustomerDoc(final String customerDoc) {
        this.customerDoc = customerDoc;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(final String metadata) {
        this.metadata = metadata;
    }
}
