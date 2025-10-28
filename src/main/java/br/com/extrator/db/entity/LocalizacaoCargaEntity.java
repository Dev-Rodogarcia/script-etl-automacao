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
    private OffsetDateTime serviceAt;
    private String status;
    private BigDecimal totalValue;
    private OffsetDateTime predictedDeliveryAt;
    private String originLocationName;
    private String destinationLocationName;

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

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(final String metadata) {
        this.metadata = metadata;
    }
}