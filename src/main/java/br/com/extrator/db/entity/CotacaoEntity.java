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

    private String customerNickname;
    private String disapproveComments;
    private String freightComments;
    private BigDecimal discountSubtotal;
    private BigDecimal itrSubtotal;
    private BigDecimal tdeSubtotal;
    private BigDecimal collectSubtotal;
    private BigDecimal deliverySubtotal;
    private BigDecimal otherFees;
    private OffsetDateTime cteIssuedAt;
    private OffsetDateTime nfseIssuedAt;
    private String senderDocument;
    private String senderNickname;
    private String receiverDocument;
    private String receiverNickname;

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

    public String getCustomerNickname() {
        return customerNickname;
    }

    public void setCustomerNickname(final String customerNickname) {
        this.customerNickname = customerNickname;
    }

    public String getDisapproveComments() {
        return disapproveComments;
    }

    public void setDisapproveComments(final String disapproveComments) {
        this.disapproveComments = disapproveComments;
    }

    public String getFreightComments() {
        return freightComments;
    }

    public void setFreightComments(final String freightComments) {
        this.freightComments = freightComments;
    }

    public BigDecimal getDiscountSubtotal() {
        return discountSubtotal;
    }

    public void setDiscountSubtotal(final BigDecimal discountSubtotal) {
        this.discountSubtotal = discountSubtotal;
    }

    public BigDecimal getItrSubtotal() {
        return itrSubtotal;
    }

    public void setItrSubtotal(final BigDecimal itrSubtotal) {
        this.itrSubtotal = itrSubtotal;
    }

    public BigDecimal getTdeSubtotal() {
        return tdeSubtotal;
    }

    public void setTdeSubtotal(final BigDecimal tdeSubtotal) {
        this.tdeSubtotal = tdeSubtotal;
    }

    public BigDecimal getCollectSubtotal() {
        return collectSubtotal;
    }

    public void setCollectSubtotal(final BigDecimal collectSubtotal) {
        this.collectSubtotal = collectSubtotal;
    }

    public BigDecimal getDeliverySubtotal() {
        return deliverySubtotal;
    }

    public void setDeliverySubtotal(final BigDecimal deliverySubtotal) {
        this.deliverySubtotal = deliverySubtotal;
    }

    public BigDecimal getOtherFees() {
        return otherFees;
    }

    public void setOtherFees(final BigDecimal otherFees) {
        this.otherFees = otherFees;
    }

    public OffsetDateTime getCteIssuedAt() {
        return cteIssuedAt;
    }

    public void setCteIssuedAt(final OffsetDateTime cteIssuedAt) {
        this.cteIssuedAt = cteIssuedAt;
    }

    public OffsetDateTime getNfseIssuedAt() {
        return nfseIssuedAt;
    }

    public void setNfseIssuedAt(final OffsetDateTime nfseIssuedAt) {
        this.nfseIssuedAt = nfseIssuedAt;
    }

    public String getSenderDocument() {
        return senderDocument;
    }

    public void setSenderDocument(final String senderDocument) {
        this.senderDocument = senderDocument;
    }

    public String getSenderNickname() {
        return senderNickname;
    }

    public void setSenderNickname(final String senderNickname) {
        this.senderNickname = senderNickname;
    }

    public String getReceiverDocument() {
        return receiverDocument;
    }

    public void setReceiverDocument(final String receiverDocument) {
        this.receiverDocument = receiverDocument;
    }

    public String getReceiverNickname() {
        return receiverNickname;
    }

    public void setReceiverNickname(final String receiverNickname) {
        this.receiverNickname = receiverNickname;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(final String metadata) {
        this.metadata = metadata;
    }
}
