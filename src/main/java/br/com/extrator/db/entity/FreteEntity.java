package br.com.extrator.db.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Entity (Entidade) que representa uma linha na tabela 'fretes' do banco de dados.
 * Contém os campos-chave "promovidos" para acesso rápido e indexação,
 * e uma coluna 'metadata' para armazenar o JSON bruto completo, garantindo
 * 100% de completude e resiliência a futuras mudanças na API.
 */
public class FreteEntity {

    // --- Coluna de Chave Primária ---
    private Long id;

    // --- Colunas Essenciais para Indexação e Relatórios ---
    private OffsetDateTime servicoEm;
    private OffsetDateTime criadoEm;
    private String status;
    private String modal;
    private String tipoFrete;
    private BigDecimal valorTotal;
    private BigDecimal valorNotas;
    private BigDecimal pesoNotas;
    private Long idCorporacao;
    private Long idCidadeDestino;
    private LocalDate dataPrevisaoEntrega;

    // --- Coluna de Metadados ---
    private String metadata;

    // --- Getters e Setters ---

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public OffsetDateTime getServicoEm() {
        return servicoEm;
    }

    public void setServicoEm(final OffsetDateTime servicoEm) {
        this.servicoEm = servicoEm;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(final OffsetDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public String getModal() {
        return modal;
    }

    public void setModal(final String modal) {
        this.modal = modal;
    }

    public String getTipoFrete() {
        return tipoFrete;
    }

    public void setTipoFrete(final String tipoFrete) {
        this.tipoFrete = tipoFrete;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(final BigDecimal valorTotal) {
        this.valorTotal = valorTotal;
    }

    public BigDecimal getValorNotas() {
        return valorNotas;
    }

    public void setValorNotas(final BigDecimal valorNotas) {
        this.valorNotas = valorNotas;
    }

    public BigDecimal getPesoNotas() {
        return pesoNotas;
    }

    public void setPesoNotas(final BigDecimal pesoNotas) {
        this.pesoNotas = pesoNotas;
    }

    public Long getIdCorporacao() {
        return idCorporacao;
    }

    public void setIdCorporacao(final Long idCorporacao) {
        this.idCorporacao = idCorporacao;
    }

    public Long getIdCidadeDestino() {
        return idCidadeDestino;
    }

    public void setIdCidadeDestino(final Long idCidadeDestino) {
        this.idCidadeDestino = idCidadeDestino;
    }

    public LocalDate getDataPrevisaoEntrega() {
        return dataPrevisaoEntrega;
    }

    public void setDataPrevisaoEntrega(final LocalDate dataPrevisaoEntrega) {
        this.dataPrevisaoEntrega = dataPrevisaoEntrega;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(final String metadata) {
        this.metadata = metadata;
    }
}
