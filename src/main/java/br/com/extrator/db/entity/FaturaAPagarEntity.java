package br.com.extrator.db.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entity (Entidade) que representa uma linha na tabela 'faturas_a_pagar' do banco de dados.
 * Esta classe é o "produto final" da nossa linha de montagem, contendo os dados
 * já limpos, convertidos e prontos para serem persistidos pelo Repository.
 * 
 * ATUALIZAÇÃO: Expandido para 14 campos disponíveis + 10 campos futuros (placeholders)
 */
public class FaturaAPagarEntity {

    // --- Colunas de Chave e Indexação ---
    private Long id; // Chave Primária
    private String documentNumber; // Chave de Negócio (Fonte: document)

    // --- Colunas de Dados Essenciais ---
    private LocalDate issueDate; // Fonte: issue_date
    private LocalDate dueDate; // Fonte: due_date
    private BigDecimal totalValue; // Fonte: value
    private String receiverCnpj; // Fonte: receiver.cnpjCpf ou receiver.cnpj
    private String receiverName; // Fonte: receiver.name
    private String invoiceType; // Fonte: type

    // --- NOVOS CAMPOS DISPONÍVEIS (14/24) ---
    private String cnpjFilial; // Fonte: corporation.cnpj
    private String filial; // Fonte: corporation.nickname
    private String observacoes; // Fonte: comments ou installments[].comments
    private String contaContabil; // Fonte: accounting_planning_management.name
    private String centroCusto; // Fonte: cost_centers[].name (concatenado)
    private String status; // Calculado localmente: Pendente/Vencido
    private String formaPagamento; // Fonte: installments[].payment_method (traduzido)

    // --- CAMPOS FUTUROS (10/24) - Preparados para integração futura ---
    // Estes campos ainda não são retornados pela API REST
    private String sequencia; // FUTURO: Sequência do lançamento
    private String cheque; // FUTURO: Número do cheque
    private LocalDate vencimentoOriginal; // FUTURO: Vencimento original
    private String competencia; // FUTURO: Competência contábil (MM/YYYY)
    private LocalDate dataBaixa; // FUTURO: Data de baixa
    private LocalDate dataLiquidacao; // FUTURO: Data de liquidação
    private String bancoPagamento; // FUTURO: Banco de pagamento
    private String contaPagamento; // FUTURO: Conta bancária de pagamento
    private String descricaoDespesa; // FUTURO: Descrição detalhada

    // --- Colunas de Metadados (Resiliência e Completude) ---
    private String headerMetadata; // Armazena o JSON completo do cabeçalho
    private String installmentsMetadata; // Armazena o JSON completo dos títulos/parcelas

    // --- Getters e Setters ---

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(final String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(final LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(final LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public BigDecimal getTotalValue() {
        return totalValue;
    }

    public void setTotalValue(final BigDecimal totalValue) {
        this.totalValue = totalValue;
    }

    public String getReceiverCnpj() {
        return receiverCnpj;
    }

    public void setReceiverCnpj(final String receiverCnpj) {
        this.receiverCnpj = receiverCnpj;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(final String receiverName) {
        this.receiverName = receiverName;
    }

    public String getInvoiceType() {
        return invoiceType;
    }

    public void setInvoiceType(final String invoiceType) {
        this.invoiceType = invoiceType;
    }

    public String getHeaderMetadata() {
        return headerMetadata;
    }

    public void setHeaderMetadata(final String headerMetadata) {
        this.headerMetadata = headerMetadata;
    }

    public String getInstallmentsMetadata() {
        return installmentsMetadata;
    }

    public void setInstallmentsMetadata(final String installmentsMetadata) {
        this.installmentsMetadata = installmentsMetadata;
    }

    // --- Getters e Setters dos Novos Campos ---

    public String getCnpjFilial() {
        return cnpjFilial;
    }

    public void setCnpjFilial(final String cnpjFilial) {
        this.cnpjFilial = cnpjFilial;
    }

    public String getFilial() {
        return filial;
    }

    public void setFilial(final String filial) {
        this.filial = filial;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(final String observacoes) {
        this.observacoes = observacoes;
    }

    public String getContaContabil() {
        return contaContabil;
    }

    public void setContaContabil(final String contaContabil) {
        this.contaContabil = contaContabil;
    }

    public String getCentroCusto() {
        return centroCusto;
    }

    public void setCentroCusto(final String centroCusto) {
        this.centroCusto = centroCusto;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public String getFormaPagamento() {
        return formaPagamento;
    }

    public void setFormaPagamento(final String formaPagamento) {
        this.formaPagamento = formaPagamento;
    }

    // --- Getters e Setters dos Campos Futuros ---

    public String getSequencia() {
        return sequencia;
    }

    public void setSequencia(final String sequencia) {
        this.sequencia = sequencia;
    }

    public String getCheque() {
        return cheque;
    }

    public void setCheque(final String cheque) {
        this.cheque = cheque;
    }

    public LocalDate getVencimentoOriginal() {
        return vencimentoOriginal;
    }

    public void setVencimentoOriginal(final LocalDate vencimentoOriginal) {
        this.vencimentoOriginal = vencimentoOriginal;
    }

    public String getCompetencia() {
        return competencia;
    }

    public void setCompetencia(final String competencia) {
        this.competencia = competencia;
    }

    public LocalDate getDataBaixa() {
        return dataBaixa;
    }

    public void setDataBaixa(final LocalDate dataBaixa) {
        this.dataBaixa = dataBaixa;
    }

    public LocalDate getDataLiquidacao() {
        return dataLiquidacao;
    }

    public void setDataLiquidacao(final LocalDate dataLiquidacao) {
        this.dataLiquidacao = dataLiquidacao;
    }

    public String getBancoPagamento() {
        return bancoPagamento;
    }

    public void setBancoPagamento(final String bancoPagamento) {
        this.bancoPagamento = bancoPagamento;
    }

    public String getContaPagamento() {
        return contaPagamento;
    }

    public void setContaPagamento(final String contaPagamento) {
        this.contaPagamento = contaPagamento;
    }

    public String getDescricaoDespesa() {
        return descricaoDespesa;
    }

    public void setDescricaoDespesa(final String descricaoDespesa) {
        this.descricaoDespesa = descricaoDespesa;
    }
}
