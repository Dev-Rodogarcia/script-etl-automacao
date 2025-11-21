package br.com.extrator.db.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Entity que representa a tabela faturas_por_cliente no banco.
 * Armazena dados híbridos de CT-e e NFS-e com informações de faturamento.
 * 
 * @author Sistema de Extração ESL Cloud
 * @version 1.0
 */
public class FaturaPorClienteEntity {

    // Chave Primária
    private String uniqueId;

    // Valores
    private BigDecimal valorFrete;
    private BigDecimal valorFatura;

    // Documentos Fiscais
    private Long numeroCte;
    private String chaveCte;
    private Long numeroNfse;
    private String statusCte;
    private OffsetDateTime dataEmissaoCte;

    // Dados da Fatura (Cobrança)
    private String numeroFatura;
    private LocalDate dataEmissaoFatura;
    private LocalDate dataVencimentoFatura;
    private LocalDate dataBaixaFatura;

    // Classificação Operacional
    private String filial;
    private String tipoFrete;
    private String classificacao;
    private String estado;

    // Envolvidos
    private String pagadorNome;
    private String pagadorDocumento;
    private String remetenteNome;
    private String destinatarioNome;
    private String vendedorNome;

    // Listas (convertidas para texto)
    private String notasFiscais;
    private String pedidosCliente;

    // Sistema
    private String metadata;

    // Getters e Setters
    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(final String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public BigDecimal getValorFrete() {
        return valorFrete;
    }

    public void setValorFrete(final BigDecimal valorFrete) {
        this.valorFrete = valorFrete;
    }

    public BigDecimal getValorFatura() {
        return valorFatura;
    }

    public void setValorFatura(final BigDecimal valorFatura) {
        this.valorFatura = valorFatura;
    }

    public Long getNumeroCte() {
        return numeroCte;
    }

    public void setNumeroCte(final Long numeroCte) {
        this.numeroCte = numeroCte;
    }

    public String getChaveCte() {
        return chaveCte;
    }

    public void setChaveCte(final String chaveCte) {
        this.chaveCte = chaveCte;
    }

    public Long getNumeroNfse() {
        return numeroNfse;
    }

    public void setNumeroNfse(final Long numeroNfse) {
        this.numeroNfse = numeroNfse;
    }

    public String getStatusCte() {
        return statusCte;
    }

    public void setStatusCte(final String statusCte) {
        this.statusCte = statusCte;
    }

    public OffsetDateTime getDataEmissaoCte() {
        return dataEmissaoCte;
    }

    public void setDataEmissaoCte(final OffsetDateTime dataEmissaoCte) {
        this.dataEmissaoCte = dataEmissaoCte;
    }

    public String getNumeroFatura() {
        return numeroFatura;
    }

    public void setNumeroFatura(final String numeroFatura) {
        this.numeroFatura = numeroFatura;
    }

    public LocalDate getDataEmissaoFatura() {
        return dataEmissaoFatura;
    }

    public void setDataEmissaoFatura(final LocalDate dataEmissaoFatura) {
        this.dataEmissaoFatura = dataEmissaoFatura;
    }

    public LocalDate getDataVencimentoFatura() {
        return dataVencimentoFatura;
    }

    public void setDataVencimentoFatura(final LocalDate dataVencimentoFatura) {
        this.dataVencimentoFatura = dataVencimentoFatura;
    }

    public LocalDate getDataBaixaFatura() {
        return dataBaixaFatura;
    }

    public void setDataBaixaFatura(final LocalDate dataBaixaFatura) {
        this.dataBaixaFatura = dataBaixaFatura;
    }

    public String getFilial() {
        return filial;
    }

    public void setFilial(final String filial) {
        this.filial = filial;
    }

    public String getTipoFrete() {
        return tipoFrete;
    }

    public void setTipoFrete(final String tipoFrete) {
        this.tipoFrete = tipoFrete;
    }

    public String getClassificacao() {
        return classificacao;
    }

    public void setClassificacao(final String classificacao) {
        this.classificacao = classificacao;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(final String estado) {
        this.estado = estado;
    }

    public String getPagadorNome() {
        return pagadorNome;
    }

    public void setPagadorNome(final String pagadorNome) {
        this.pagadorNome = pagadorNome;
    }

    public String getPagadorDocumento() {
        return pagadorDocumento;
    }

    public void setPagadorDocumento(final String pagadorDocumento) {
        this.pagadorDocumento = pagadorDocumento;
    }

    public String getRemetenteNome() {
        return remetenteNome;
    }

    public void setRemetenteNome(final String remetenteNome) {
        this.remetenteNome = remetenteNome;
    }

    public String getDestinatarioNome() {
        return destinatarioNome;
    }

    public void setDestinatarioNome(final String destinatarioNome) {
        this.destinatarioNome = destinatarioNome;
    }

    public String getVendedorNome() {
        return vendedorNome;
    }

    public void setVendedorNome(final String vendedorNome) {
        this.vendedorNome = vendedorNome;
    }

    public String getNotasFiscais() {
        return notasFiscais;
    }

    public void setNotasFiscais(final String notasFiscais) {
        this.notasFiscais = notasFiscais;
    }

    public String getPedidosCliente() {
        return pedidosCliente;
    }

    public void setPedidosCliente(final String pedidosCliente) {
        this.pedidosCliente = pedidosCliente;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(final String metadata) {
        this.metadata = metadata;
    }
}