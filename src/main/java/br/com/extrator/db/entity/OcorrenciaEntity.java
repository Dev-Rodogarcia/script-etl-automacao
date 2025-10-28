package br.com.extrator.db.entity;

import java.time.OffsetDateTime;

/**
 * Entity (Entidade) que representa uma linha na tabela 'ocorrencias' do banco de dados.
 * Contém os campos-chave "promovidos" para acesso rápido e indexação,
 * e uma coluna 'metadata' para armazenar o JSON bruto completo, garantindo
 * 100% de completude e resiliência a futuras mudanças na API.
 */
public class OcorrenciaEntity {

    // --- Coluna de Chave Primária ---
    private Long id;

    // --- Colunas de Dados Essenciais e Indexáveis ---
    private OffsetDateTime occurrenceAt;
    private Integer occurrenceCode;
    private String occurrenceDescription;
    private Long freightId;
    private String cteKey;
    private Long invoiceId;
    private String invoiceKey;

    // --- Coluna de Metadados ---
    private String metadata;

    // --- Getters e Setters ---

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public OffsetDateTime getOccurrenceAt() {
        return occurrenceAt;
    }

    public void setOccurrenceAt(final OffsetDateTime occurrenceAt) {
        this.occurrenceAt = occurrenceAt;
    }

    public Integer getOccurrenceCode() {
        return occurrenceCode;
    }

    public void setOccurrenceCode(final Integer occurrenceCode) {
        this.occurrenceCode = occurrenceCode;
    }

    public String getOccurrenceDescription() {
        return occurrenceDescription;
    }

    public void setOccurrenceDescription(final String occurrenceDescription) {
        this.occurrenceDescription = occurrenceDescription;
    }

    public Long getFreightId() {
        return freightId;
    }

    public void setFreightId(final Long freightId) {
        this.freightId = freightId;
    }

    public String getCteKey() {
        return cteKey;
    }

    public void setCteKey(final String cteKey) {
        this.cteKey = cteKey;
    }

    public Long getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(final Long invoiceId) {
        this.invoiceId = invoiceId;
    }

    public String getInvoiceKey() {
        return invoiceKey;
    }

    public void setInvoiceKey(final String invoiceKey) {
        this.invoiceKey = invoiceKey;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(final String metadata) {
        this.metadata = metadata;
    }
}
