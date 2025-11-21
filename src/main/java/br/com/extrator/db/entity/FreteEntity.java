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

    // --- Campos Expandidos (22 campos do CSV) ---
    private Long pagadorId;
    private String pagadorNome;
    private Long remetenteId;
    private String remetenteNome;
    private String origemCidade;
    private String origemUf;
    private Long destinatarioId;
    private String destinatarioNome;
    private String destinoCidade;
    private String destinoUf;
    private String filialNome;
    private String numeroNotaFiscal;
    private String tabelaPrecoNome;
    private String classificacaoNome;
    private String centroCustoNome;
    private String usuarioNome;
    private String referenceNumber;
    private Integer invoicesTotalVolumes;
    private BigDecimal taxedWeight;
    private BigDecimal realWeight;
    private BigDecimal totalCubicVolume;
    private BigDecimal subtotal;

    private String chaveCte;
    private Integer numeroCte;
    private Integer serieCte;

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

    // --- Getters e Setters para Campos Expandidos ---

    public Long getPagadorId() {
        return pagadorId;
    }

    public void setPagadorId(Long pagadorId) {
        this.pagadorId = pagadorId;
    }

    public String getPagadorNome() {
        return pagadorNome;
    }

    public void setPagadorNome(String pagadorNome) {
        this.pagadorNome = pagadorNome;
    }

    public Long getRemetenteId() {
        return remetenteId;
    }

    public void setRemetenteId(Long remetenteId) {
        this.remetenteId = remetenteId;
    }

    public String getRemetenteNome() {
        return remetenteNome;
    }

    public void setRemetenteNome(String remetenteNome) {
        this.remetenteNome = remetenteNome;
    }

    public String getOrigemCidade() {
        return origemCidade;
    }

    public void setOrigemCidade(String origemCidade) {
        this.origemCidade = origemCidade;
    }

    public String getOrigemUf() {
        return origemUf;
    }

    public void setOrigemUf(String origemUf) {
        this.origemUf = origemUf;
    }

    public Long getDestinatarioId() {
        return destinatarioId;
    }

    public void setDestinatarioId(Long destinatarioId) {
        this.destinatarioId = destinatarioId;
    }

    public String getDestinatarioNome() {
        return destinatarioNome;
    }

    public void setDestinatarioNome(String destinatarioNome) {
        this.destinatarioNome = destinatarioNome;
    }

    public String getDestinoCidade() {
        return destinoCidade;
    }

    public void setDestinoCidade(String destinoCidade) {
        this.destinoCidade = destinoCidade;
    }

    public String getDestinoUf() {
        return destinoUf;
    }

    public void setDestinoUf(String destinoUf) {
        this.destinoUf = destinoUf;
    }

    public String getFilialNome() {
        return filialNome;
    }

    public void setFilialNome(String filialNome) {
        this.filialNome = filialNome;
    }

    public String getNumeroNotaFiscal() {
        return numeroNotaFiscal;
    }

    public void setNumeroNotaFiscal(String numeroNotaFiscal) {
        this.numeroNotaFiscal = numeroNotaFiscal;
    }

    public String getTabelaPrecoNome() {
        return tabelaPrecoNome;
    }

    public void setTabelaPrecoNome(String tabelaPrecoNome) {
        this.tabelaPrecoNome = tabelaPrecoNome;
    }

    public String getClassificacaoNome() {
        return classificacaoNome;
    }

    public void setClassificacaoNome(String classificacaoNome) {
        this.classificacaoNome = classificacaoNome;
    }

    public String getCentroCustoNome() {
        return centroCustoNome;
    }

    public void setCentroCustoNome(String centroCustoNome) {
        this.centroCustoNome = centroCustoNome;
    }

    public String getUsuarioNome() {
        return usuarioNome;
    }

    public void setUsuarioNome(String usuarioNome) {
        this.usuarioNome = usuarioNome;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public Integer getInvoicesTotalVolumes() {
        return invoicesTotalVolumes;
    }

    public void setInvoicesTotalVolumes(Integer invoicesTotalVolumes) {
        this.invoicesTotalVolumes = invoicesTotalVolumes;
    }

    public BigDecimal getTaxedWeight() {
        return taxedWeight;
    }

    public void setTaxedWeight(BigDecimal taxedWeight) {
        this.taxedWeight = taxedWeight;
    }

    public BigDecimal getRealWeight() {
        return realWeight;
    }

    public void setRealWeight(BigDecimal realWeight) {
        this.realWeight = realWeight;
    }

    public BigDecimal getTotalCubicVolume() {
        return totalCubicVolume;
    }

    public void setTotalCubicVolume(BigDecimal totalCubicVolume) {
        this.totalCubicVolume = totalCubicVolume;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public String getChaveCte() {
        return chaveCte;
    }

    public void setChaveCte(final String chaveCte) {
        this.chaveCte = chaveCte;
    }

    public Integer getNumeroCte() {
        return numeroCte;
    }

    public void setNumeroCte(final Integer numeroCte) {
        this.numeroCte = numeroCte;
    }

    public Integer getSerieCte() {
        return serieCte;
    }

    public void setSerieCte(final Integer serieCte) {
        this.serieCte = serieCte;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(final String metadata) {
        this.metadata = metadata;
    }
}
