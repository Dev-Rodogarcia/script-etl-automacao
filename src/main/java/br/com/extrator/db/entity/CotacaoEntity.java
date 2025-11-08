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
    // Campos principais conforme docs/descobertas-endpoints/cotacoes.md
    private OffsetDateTime requestedAt;
    private String operationType; // Tipo de operação
    private String customerDoc;
    private String customerName; // Cliente Pagador
    private String originCity;
    private String originState;
    private String destinationCity;
    private String destinationState;
    private String priceTable; // Tabela
    private Integer volumes;
    private BigDecimal taxedWeight;
    private BigDecimal invoicesValue;
    private BigDecimal totalValue;
    private String userName; // Usuário
    private String branchNickname; // Filial
    private String companyName; // Empresa
    // Campos adicionais importantes
    private String requesterName; // Solicitante
    private String realWeight; // Peso real
    private String originPostalCode; // CEP Origem
    private String destinationPostalCode; // CEP Destino

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

    // Getters e Setters para campos adicionais
    
    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(final String operationType) {
        this.operationType = operationType;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(final String customerName) {
        this.customerName = customerName;
    }

    public String getPriceTable() {
        return priceTable;
    }

    public void setPriceTable(final String priceTable) {
        this.priceTable = priceTable;
    }

    public Integer getVolumes() {
        return volumes;
    }

    public void setVolumes(final Integer volumes) {
        this.volumes = volumes;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(final String userName) {
        this.userName = userName;
    }

    public String getBranchNickname() {
        return branchNickname;
    }

    public void setBranchNickname(final String branchNickname) {
        this.branchNickname = branchNickname;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(final String companyName) {
        this.companyName = companyName;
    }

    public String getRequesterName() {
        return requesterName;
    }

    public void setRequesterName(final String requesterName) {
        this.requesterName = requesterName;
    }

    public String getRealWeight() {
        return realWeight;
    }

    public void setRealWeight(final String realWeight) {
        this.realWeight = realWeight;
    }

    public String getOriginPostalCode() {
        return originPostalCode;
    }

    public void setOriginPostalCode(final String originPostalCode) {
        this.originPostalCode = originPostalCode;
    }

    public String getDestinationPostalCode() {
        return destinationPostalCode;
    }

    public void setDestinationPostalCode(final String destinationPostalCode) {
        this.destinationPostalCode = destinationPostalCode;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(final String metadata) {
        this.metadata = metadata;
    }
}
