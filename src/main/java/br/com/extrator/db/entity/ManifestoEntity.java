package br.com.extrator.db.entity;

import java.math.BigDecimal;
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
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime departuredAt;
    private OffsetDateTime finishedAt;
    private BigDecimal totalCost;
    private Integer traveledKm;
    private String vehiclePlate;
    private String driverName;
    private String originBranch;
    private String mdfeStatus;

    // --- Coluna de Metadados ---
    private String metadata;

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

    public String getOriginBranch() {
        return originBranch;
    }

    public void setOriginBranch(String originBranch) {
        this.originBranch = originBranch;
    }

    public String getMdfeStatus() {
        return mdfeStatus;
    }

    public void setMdfeStatus(String mdfeStatus) {
        this.mdfeStatus = mdfeStatus;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
}
