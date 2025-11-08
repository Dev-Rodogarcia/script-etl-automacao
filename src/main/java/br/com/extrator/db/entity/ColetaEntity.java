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

    // --- Campos Expandidos (22 campos do CSV) ---
    private Long clienteId;
    private String clienteNome;
    private String localColeta;
    private String cidadeColeta;
    private String ufColeta;
    private Long usuarioId;
    private String usuarioNome;
    private String requestHour;
    private String serviceStartHour;
    private LocalDate finishDate;
    private String serviceEndHour;
    private String requester;
    private BigDecimal taxedWeight;
    private String comments;
    private Long agentId;
    private Long manifestItemPickId;
    private Long vehicleTypeId;

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

    // --- Getters e Setters para Campos Expandidos ---

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public String getClienteNome() {
        return clienteNome;
    }

    public void setClienteNome(String clienteNome) {
        this.clienteNome = clienteNome;
    }

    public String getLocalColeta() {
        return localColeta;
    }

    public void setLocalColeta(String localColeta) {
        this.localColeta = localColeta;
    }

    public String getCidadeColeta() {
        return cidadeColeta;
    }

    public void setCidadeColeta(String cidadeColeta) {
        this.cidadeColeta = cidadeColeta;
    }

    public String getUfColeta() {
        return ufColeta;
    }

    public void setUfColeta(String ufColeta) {
        this.ufColeta = ufColeta;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getUsuarioNome() {
        return usuarioNome;
    }

    public void setUsuarioNome(String usuarioNome) {
        this.usuarioNome = usuarioNome;
    }

    public String getRequestHour() {
        return requestHour;
    }

    public void setRequestHour(String requestHour) {
        this.requestHour = requestHour;
    }

    public String getServiceStartHour() {
        return serviceStartHour;
    }

    public void setServiceStartHour(String serviceStartHour) {
        this.serviceStartHour = serviceStartHour;
    }

    public LocalDate getFinishDate() {
        return finishDate;
    }

    public void setFinishDate(LocalDate finishDate) {
        this.finishDate = finishDate;
    }

    public String getServiceEndHour() {
        return serviceEndHour;
    }

    public void setServiceEndHour(String serviceEndHour) {
        this.serviceEndHour = serviceEndHour;
    }

    public String getRequester() {
        return requester;
    }

    public void setRequester(String requester) {
        this.requester = requester;
    }

    public BigDecimal getTaxedWeight() {
        return taxedWeight;
    }

    public void setTaxedWeight(BigDecimal taxedWeight) {
        this.taxedWeight = taxedWeight;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public Long getAgentId() {
        return agentId;
    }

    public void setAgentId(Long agentId) {
        this.agentId = agentId;
    }

    public Long getManifestItemPickId() {
        return manifestItemPickId;
    }

    public void setManifestItemPickId(Long manifestItemPickId) {
        this.manifestItemPickId = manifestItemPickId;
    }

    public Long getVehicleTypeId() {
        return vehicleTypeId;
    }

    public void setVehicleTypeId(Long vehicleTypeId) {
        this.vehicleTypeId = vehicleTypeId;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(final String metadata) {
        this.metadata = metadata;
    }
}
