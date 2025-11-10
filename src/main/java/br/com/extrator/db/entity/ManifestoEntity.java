package br.com.extrator.db.entity;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;

/**
 * Entity (Entidade) que representa uma linha na tabela 'manifestos' do banco de dados.
 * É o "produto final" da transformação, contendo os dados já estruturados e prontos
 * para serem persistidos. A coluna 'metadata' armazena o JSON completo
 * do objeto original para garantir 100% de completude e resiliência.
 */
public class ManifestoEntity {

    // --- Coluna de Chave Primária ---
    private Long sequenceCode;

    // --- Colunas Essenciais para Indexação e Relatórios ---
    // Campos principais conforme docs/descobertas-endpoints/manifestos.md
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime departuredAt;
    private OffsetDateTime closedAt; // Data Fechamento
    private OffsetDateTime finishedAt;
    private Integer mdfeNumber; // MDF-e
    private String mdfeKey; // Chave MDF-e
    private String mdfeStatus;
    private String distributionPole; // Polo de Distribuição
    private String classification; // Classificação
    private String vehiclePlate;
    private String vehicleType; // Tipo Veículo
    private String vehicleOwner; // Proprietário
    private String driverName;
    private String branchNickname; // Filial (Apelido)
    private Integer vehicleDepartureKm; // Km Saída
    private Integer closingKm; // Km Fechamento
    private Integer traveledKm;
    private Integer invoicesCount; // Total Notas
    private Integer invoicesVolumes; // Total Volumes
    private String invoicesWeight; // Peso Real
    private String totalTaxedWeight; // Peso Taxado
    private String totalCubicVolume; // Cubagem
    private String invoicesValue; // Valor Notas
    private String manifestFreightsTotal; // Valor Fretes
    private Long pickSequenceCode; // Coleta (Item)
    private String contractNumber; // Contrato
    private String dailySubtotal; // Diárias
    private BigDecimal totalCost;
    private String operationalExpensesTotal; // Desp. Operacionais
    private String inssValue; // INSS
    private String sestSenatValue; // SEST/SENAT
    private String irValue; // IR
    private String payingTotal; // Valor a Pagar
    private String creationUserName; // Usuário (Criação)
    private String adjustmentUserName; // Usuário do Acerto

    // --- Coluna de Metadados ---
    private String metadata;

    // --- Coluna de Identificador Único (para chave composta) ---
    private String identificadorUnico;

    // --- Getters e Setters ---

    public Long getSequenceCode() {
        return sequenceCode;
    }

    public void setSequenceCode(Long sequenceCode) {
        this.sequenceCode = sequenceCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getDeparturedAt() {
        return departuredAt;
    }

    public void setDeparturedAt(OffsetDateTime departuredAt) {
        this.departuredAt = departuredAt;
    }

    public OffsetDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(OffsetDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }

    public Integer getTraveledKm() {
        return traveledKm;
    }

    public void setTraveledKm(Integer traveledKm) {
        this.traveledKm = traveledKm;
    }

    public String getVehiclePlate() {
        return vehiclePlate;
    }

    public void setVehiclePlate(String vehiclePlate) {
        this.vehiclePlate = vehiclePlate;
    }

    public String getDriverName() {
        return driverName;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }

    // Getters e Setters para campos adicionais
    
    public OffsetDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(final OffsetDateTime closedAt) {
        this.closedAt = closedAt;
    }

    public Integer getMdfeNumber() {
        return mdfeNumber;
    }

    public void setMdfeNumber(final Integer mdfeNumber) {
        this.mdfeNumber = mdfeNumber;
    }

    public String getMdfeKey() {
        return mdfeKey;
    }

    public void setMdfeKey(final String mdfeKey) {
        this.mdfeKey = mdfeKey;
    }

    public String getMdfeStatus() {
        return mdfeStatus;
    }

    public void setMdfeStatus(final String mdfeStatus) {
        this.mdfeStatus = mdfeStatus;
    }

    public String getDistributionPole() {
        return distributionPole;
    }

    public void setDistributionPole(final String distributionPole) {
        this.distributionPole = distributionPole;
    }

    public String getClassification() {
        return classification;
    }

    public void setClassification(final String classification) {
        this.classification = classification;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(final String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public String getVehicleOwner() {
        return vehicleOwner;
    }

    public void setVehicleOwner(final String vehicleOwner) {
        this.vehicleOwner = vehicleOwner;
    }

    public String getBranchNickname() {
        return branchNickname;
    }

    public void setBranchNickname(final String branchNickname) {
        this.branchNickname = branchNickname;
    }

    public Integer getVehicleDepartureKm() {
        return vehicleDepartureKm;
    }

    public void setVehicleDepartureKm(final Integer vehicleDepartureKm) {
        this.vehicleDepartureKm = vehicleDepartureKm;
    }

    public Integer getClosingKm() {
        return closingKm;
    }

    public void setClosingKm(final Integer closingKm) {
        this.closingKm = closingKm;
    }

    public Integer getInvoicesCount() {
        return invoicesCount;
    }

    public void setInvoicesCount(final Integer invoicesCount) {
        this.invoicesCount = invoicesCount;
    }

    public Integer getInvoicesVolumes() {
        return invoicesVolumes;
    }

    public void setInvoicesVolumes(final Integer invoicesVolumes) {
        this.invoicesVolumes = invoicesVolumes;
    }

    public String getInvoicesWeight() {
        return invoicesWeight;
    }

    public void setInvoicesWeight(final String invoicesWeight) {
        this.invoicesWeight = invoicesWeight;
    }

    public String getTotalTaxedWeight() {
        return totalTaxedWeight;
    }

    public void setTotalTaxedWeight(final String totalTaxedWeight) {
        this.totalTaxedWeight = totalTaxedWeight;
    }

    public String getTotalCubicVolume() {
        return totalCubicVolume;
    }

    public void setTotalCubicVolume(final String totalCubicVolume) {
        this.totalCubicVolume = totalCubicVolume;
    }

    public String getInvoicesValue() {
        return invoicesValue;
    }

    public void setInvoicesValue(final String invoicesValue) {
        this.invoicesValue = invoicesValue;
    }

    public String getManifestFreightsTotal() {
        return manifestFreightsTotal;
    }

    public void setManifestFreightsTotal(final String manifestFreightsTotal) {
        this.manifestFreightsTotal = manifestFreightsTotal;
    }

    public Long getPickSequenceCode() {
        return pickSequenceCode;
    }

    public void setPickSequenceCode(final Long pickSequenceCode) {
        this.pickSequenceCode = pickSequenceCode;
    }

    public String getContractNumber() {
        return contractNumber;
    }

    public void setContractNumber(final String contractNumber) {
        this.contractNumber = contractNumber;
    }

    public String getDailySubtotal() {
        return dailySubtotal;
    }

    public void setDailySubtotal(final String dailySubtotal) {
        this.dailySubtotal = dailySubtotal;
    }

    public String getOperationalExpensesTotal() {
        return operationalExpensesTotal;
    }

    public void setOperationalExpensesTotal(final String operationalExpensesTotal) {
        this.operationalExpensesTotal = operationalExpensesTotal;
    }

    public String getInssValue() {
        return inssValue;
    }

    public void setInssValue(final String inssValue) {
        this.inssValue = inssValue;
    }

    public String getSestSenatValue() {
        return sestSenatValue;
    }

    public void setSestSenatValue(final String sestSenatValue) {
        this.sestSenatValue = sestSenatValue;
    }

    public String getIrValue() {
        return irValue;
    }

    public void setIrValue(final String irValue) {
        this.irValue = irValue;
    }

    public String getPayingTotal() {
        return payingTotal;
    }

    public void setPayingTotal(final String payingTotal) {
        this.payingTotal = payingTotal;
    }

    public String getCreationUserName() {
        return creationUserName;
    }

    public void setCreationUserName(final String creationUserName) {
        this.creationUserName = creationUserName;
    }

    public String getAdjustmentUserName() {
        return adjustmentUserName;
    }

    public void setAdjustmentUserName(final String adjustmentUserName) {
        this.adjustmentUserName = adjustmentUserName;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(final String metadata) {
        this.metadata = metadata;
    }

    public String getIdentificadorUnico() {
        return identificadorUnico;
    }

    public void setIdentificadorUnico(final String identificadorUnico) {
        this.identificadorUnico = identificadorUnico;
    }

    /**
     * Calcula o identificador único para este manifesto.
     * Este método deve ser chamado DEPOIS que o metadata for definido.
     * 
     * Prioridade 1: pick_sequence_code (quando disponível e não NULL)
     * Prioridade 2: hash SHA-256 do metadata completo (quando pick_sequence_code é NULL)
     * 
     * O hash do metadata garante que manifestos com mesmo sequence_code mas
     * metadata diferentes (duplicados naturais) sejam tratados como registros distintos.
     */
    public void calcularIdentificadorUnico() {
        if (this.pickSequenceCode != null) {
            // Prioridade 1: Usar pick_sequence_code quando disponível
            this.identificadorUnico = String.valueOf(this.pickSequenceCode);
        } else {
            // Prioridade 2: Calcular hash do metadata quando pick_sequence_code é NULL
            this.identificadorUnico = calcularHashMetadata(this.metadata);
        }
    }

    /**
     * Calcula hash SHA-256 do metadata JSON.
     * Usado quando pick_sequence_code não está disponível para diferenciar
     * duplicados naturais que têm mesmo sequence_code mas metadata diferentes.
     * 
     * @param metadata String JSON do metadata
     * @return String hexadecimal do hash SHA-256 (64 caracteres) ou fallback se metadata estiver vazio
     */
    private String calcularHashMetadata(final String metadata) {
        if (metadata == null || metadata.trim().isEmpty()) {
            // Fallback: usar hash do sequence_code se metadata estiver vazio
            return "NULL_METADATA_" + (this.sequenceCode != null ? this.sequenceCode : "UNKNOWN");
        }
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            final byte[] hash = digest.digest(metadata.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (final NoSuchAlgorithmException e) {
            // Fallback: usar hash simples se SHA-256 não disponível (não deve acontecer)
            throw new RuntimeException("Erro ao calcular hash SHA-256 do metadata", e);
        }
    }

    /**
     * Converte array de bytes para string hexadecimal.
     * 
     * @param bytes Array de bytes a ser convertido
     * @return String hexadecimal (2 caracteres por byte)
     */
    private String bytesToHex(final byte[] bytes) {
        final StringBuilder result = new StringBuilder();
        for (final byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
