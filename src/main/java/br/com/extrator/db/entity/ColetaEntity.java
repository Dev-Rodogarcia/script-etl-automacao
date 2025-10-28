package br.com.extrator.db.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entity (Entidade) que representa uma linha na tabela 'coletas' do banco de dados.
 * É o produto final da transformação, contendo os dados já estruturados e prontos
 * para serem persistidos. A coluna 'metadata' armazena o JSON completo
 * do objeto original para garantir 100% de completude e resiliência.
 */
public class ColetaEntity {

    // --- Colunas de Chave ---
    private String id; // Chave Primária (VARCHAR)

    // --- Colunas Essenciais para Indexação e Relatórios ---
    private Long sequenceCode;
    private LocalDate requestDate;
    private LocalDate serviceDate;
    private String status;
    private BigDecimal totalValue;
    private BigDecimal totalWeight;
    private Integer totalVolumes;

    // --- Coluna de Metadados ---
    private String metadata;

    // --- Getters e Setters ---

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }



    public Long getSequenceCode() {
        return sequenceCode;
    }

    public void setSequenceCode(final Long sequenceCode) {
        this.sequenceCode = sequenceCode;
    }

    public LocalDate getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(final LocalDate requestDate) {
        this.requestDate = requestDate;
    }

    public LocalDate getServiceDate() {
        return serviceDate;
    }

    public void setServiceDate(final LocalDate serviceDate) {
        this.serviceDate = serviceDate;
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

    public BigDecimal getTotalWeight() {
        return totalWeight;
    }

    public void setTotalWeight(final BigDecimal totalWeight) {
        this.totalWeight = totalWeight;
    }

    public Integer getTotalVolumes() {
        return totalVolumes;
    }

    public void setTotalVolumes(final Integer totalVolumes) {
        this.totalVolumes = totalVolumes;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(final String metadata) {
        this.metadata = metadata;
    }
}
