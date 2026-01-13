package br.com.extrator.db.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Entity (Entidade) que representa uma linha na tabela 'localizacao_cargas' do banco de dados.
 * É o "produto final" da transformação, contendo os dados já estruturados e prontos
 * para serem persistidos. A coluna 'metadata' armazena o JSON completo
 * do objeto original para garantir 100% de completude e resiliência.
 */
public class LocalizacaoCargaEntity {

    // --- Coluna de Chave Primária ---
    private Long sequenceNumber;

    // --- Colunas Essenciais para Indexação e Relatórios ---
    // Campos principais conforme docs/descobertas-endpoints/localizacaocarga.md
    private String type; // Tipo
    private OffsetDateTime serviceAt;
    private Integer invoicesVolumes; // Volumes
    private String taxedWeight; // Peso Taxado
    private String invoicesValue; // Valor NF
    private BigDecimal totalValue;
    private String serviceType; // Serviço
    private String branchNickname; // Filial
    private OffsetDateTime predictedDeliveryAt;
    private String destinationLocationName; // Polo de Destino
    private String destinationBranchNickname; // Filial de Destino
    private String classification; // Classificação
    private String status;
    private String statusBranchNickname; // Filial do Status
    private String originLocationName; // Polo de Origem
    private String originBranchNickname; // Filial de Origem

    // --- Coluna de Metadados ---
    private String metadata;

    // --- Getters e Setters ---

    public Long getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(final Long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public OffsetDateTime getServiceAt() {
        return serviceAt;
    }

    public void setServiceAt(final OffsetDateTime serviceAt) {
        this.serviceAt = serviceAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public BigDecimal getTotalValue() {
        return totalValue;
    }

    public void setTotalValue(final BigDecimal totalValue) {
        this.totalValue = totalValue;
    }

    public OffsetDateTime getPredictedDeliveryAt() {
        return predictedDeliveryAt;
    }

    public void setPredictedDeliveryAt(final OffsetDateTime predictedDeliveryAt) {
        this.predictedDeliveryAt = predictedDeliveryAt;
    }

    public String getOriginLocationName() {
        return originLocationName;
    }

    public void setOriginLocationName(final String originLocationName) {
        this.originLocationName = originLocationName;
    }

    public String getDestinationLocationName() {
        return destinationLocationName;
    }

    public void setDestinationLocationName(final String destinationLocationName) {
        this.destinationLocationName = destinationLocationName;
    }

    // Getters e Setters para campos adicionais
    
    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public Integer getInvoicesVolumes() {
        return invoicesVolumes;
    }

    public void setInvoicesVolumes(final Integer invoicesVolumes) {
        this.invoicesVolumes = invoicesVolumes;
    }

    public String getTaxedWeight() {
        return taxedWeight;
    }

    public void setTaxedWeight(final String taxedWeight) {
        this.taxedWeight = taxedWeight;
    }

    public String getInvoicesValue() {
        return invoicesValue;
    }

    public void setInvoicesValue(final String invoicesValue) {
        this.invoicesValue = invoicesValue;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(final String serviceType) {
        this.serviceType = serviceType;
    }

    public String getBranchNickname() {
        return branchNickname;
    }

    public void setBranchNickname(final String branchNickname) {
        this.branchNickname = branchNickname;
    }

    public String getDestinationBranchNickname() {
        return destinationBranchNickname;
    }

    public void setDestinationBranchNickname(final String destinationBranchNickname) {
        this.destinationBranchNickname = destinationBranchNickname;
    }

    public String getClassification() {
        return classification;
    }

    public void setClassification(final String classification) {
        this.classification = classification;
    }

    public String getStatusBranchNickname() {
        return statusBranchNickname;
    }

    public void setStatusBranchNickname(final String statusBranchNickname) {
        this.statusBranchNickname = statusBranchNickname;
    }

    public String getOriginBranchNickname() {
        return originBranchNickname;
    }

    public void setOriginBranchNickname(final String originBranchNickname) {
        this.originBranchNickname = originBranchNickname;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(final String metadata) {
        this.metadata = metadata;
    }
}