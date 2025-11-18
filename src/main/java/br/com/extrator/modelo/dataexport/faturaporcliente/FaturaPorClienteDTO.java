package br.com.extrator.modelo.dataexport.faturaporcliente;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO para dados de Faturas por Cliente da API Data Export (Template 4924).
 * Representa a resposta JSON da API contendo informações híbridas de CT-e e NFS-e.
 * 
 * @author Sistema de Extração ESL Cloud
 * @version 1.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FaturaPorClienteDTO {

    // Documentos Fiscais
    @JsonProperty("fit_nse_number")
    private Long nfseNumber;

    @JsonProperty("fit_fhe_cte_number")
    private Long cteNumber;

    @JsonProperty("fit_fhe_cte_issued_at")
    private String cteIssuedAt;

    @JsonProperty("fit_fhe_cte_key")
    private String cteKey;

    @JsonProperty("fit_fhe_cte_status_result")
    private String cteStatusResult;

    @JsonProperty("fit_fhe_cte_status")
    private String cteStatus;

    // Dados da Fatura (Cobrança)
    @JsonProperty("fit_ant_document")
    private String faturaDocument;

    @JsonProperty("fit_ant_issue_date")
    private String faturaIssueDate;

    @JsonProperty("fit_ant_value")
    private String faturaValue;

    @JsonProperty("fit_ant_ils_due_date")
    private String faturaDueDate;

    @JsonProperty("fit_ant_ils_atn_transaction_date")
    private String faturaBaixaDate;

    // Valores
    @JsonProperty("total")
    private String valorFrete;

    @JsonProperty("type")
    private String tipoFrete;

    // Classificação Operacional
    @JsonProperty("fit_crn_psn_nickname")
    private String filial;

    @JsonProperty("fit_diy_sae_name")
    private String estado;

    @JsonProperty("fit_fsn_name")
    private String classificacao;

    // Envolvidos
    @JsonProperty("fit_pyr_name")
    private String pagadorNome;

    @JsonProperty("fit_pyr_document")
    private String pagadorDocumento;

    @JsonProperty("fit_rpt_name")
    private String remetenteNome;

    @JsonProperty("fit_sdr_name")
    private String destinatarioNome;

    @JsonProperty("fit_sps_slr_psn_name")
    private String vendedorNome;

    // Listas (Arrays)
    @JsonProperty("invoices_mapping")
    private List<String> notasFiscais;

    @JsonProperty("fit_fte_invoices_order_number")
    private List<String> pedidosCliente;

    // Getters e Setters
    public Long getNfseNumber() {
        return nfseNumber;
    }

    public void setNfseNumber(Long nfseNumber) {
        this.nfseNumber = nfseNumber;
    }

    public Long getCteNumber() {
        return cteNumber;
    }

    public void setCteNumber(Long cteNumber) {
        this.cteNumber = cteNumber;
    }

    public String getCteIssuedAt() {
        return cteIssuedAt;
    }

    public void setCteIssuedAt(String cteIssuedAt) {
        this.cteIssuedAt = cteIssuedAt;
    }

    public String getCteKey() {
        return cteKey;
    }

    public void setCteKey(String cteKey) {
        this.cteKey = cteKey;
    }

    public String getCteStatusResult() {
        return cteStatusResult;
    }

    public void setCteStatusResult(String cteStatusResult) {
        this.cteStatusResult = cteStatusResult;
    }

    public String getCteStatus() {
        return cteStatus;
    }

    public void setCteStatus(String cteStatus) {
        this.cteStatus = cteStatus;
    }

    public String getFaturaDocument() {
        return faturaDocument;
    }

    public void setFaturaDocument(String faturaDocument) {
        this.faturaDocument = faturaDocument;
    }

    public String getFaturaIssueDate() {
        return faturaIssueDate;
    }

    public void setFaturaIssueDate(String faturaIssueDate) {
        this.faturaIssueDate = faturaIssueDate;
    }

    public String getFaturaValue() {
        return faturaValue;
    }

    public void setFaturaValue(String faturaValue) {
        this.faturaValue = faturaValue;
    }

    public String getFaturaDueDate() {
        return faturaDueDate;
    }

    public void setFaturaDueDate(String faturaDueDate) {
        this.faturaDueDate = faturaDueDate;
    }

    public String getFaturaBaixaDate() {
        return faturaBaixaDate;
    }

    public void setFaturaBaixaDate(String faturaBaixaDate) {
        this.faturaBaixaDate = faturaBaixaDate;
    }

    public String getValorFrete() {
        return valorFrete;
    }

    public void setValorFrete(String valorFrete) {
        this.valorFrete = valorFrete;
    }

    public String getTipoFrete() {
        return tipoFrete;
    }

    public void setTipoFrete(String tipoFrete) {
        this.tipoFrete = tipoFrete;
    }

    public String getFilial() {
        return filial;
    }

    public void setFilial(String filial) {
        this.filial = filial;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getClassificacao() {
        return classificacao;
    }

    public void setClassificacao(String classificacao) {
        this.classificacao = classificacao;
    }

    public String getPagadorNome() {
        return pagadorNome;
    }

    public void setPagadorNome(String pagadorNome) {
        this.pagadorNome = pagadorNome;
    }

    public String getPagadorDocumento() {
        return pagadorDocumento;
    }

    public void setPagadorDocumento(String pagadorDocumento) {
        this.pagadorDocumento = pagadorDocumento;
    }

    public String getRemetenteNome() {
        return remetenteNome;
    }

    public void setRemetenteNome(String remetenteNome) {
        this.remetenteNome = remetenteNome;
    }

    public String getDestinatarioNome() {
        return destinatarioNome;
    }

    public void setDestinatarioNome(String destinatarioNome) {
        this.destinatarioNome = destinatarioNome;
    }

    public String getVendedorNome() {
        return vendedorNome;
    }

    public void setVendedorNome(String vendedorNome) {
        this.vendedorNome = vendedorNome;
    }

    public List<String> getNotasFiscais() {
        return notasFiscais;
    }

    public void setNotasFiscais(List<String> notasFiscais) {
        this.notasFiscais = notasFiscais;
    }

    public List<String> getPedidosCliente() {
        return pedidosCliente;
    }

    public void setPedidosCliente(List<String> pedidosCliente) {
        this.pedidosCliente = pedidosCliente;
    }
}
