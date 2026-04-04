package br.com.extrator.persistencia.entidade;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class InventarioEntity {

    private String identificadorUnico;
    private Long sequenceCode;
    private Long numeroMinuta;
    private String pagadorNome;
    private String remetenteNome;
    private String origemCidade;
    private String destinatarioNome;
    private String destinoCidade;
    private String regiaoEntrega;
    private String filialEntregadora;
    private String branchNickname;
    private String type;
    private OffsetDateTime startedAt;
    private OffsetDateTime finishedAt;
    private String status;
    private String conferenteNome;
    private String invoicesMapping;
    private BigDecimal invoicesValue;
    private BigDecimal realWeight;
    private BigDecimal totalCubicVolume;
    private BigDecimal taxedWeight;
    private Integer invoicesVolumes;
    private Integer readVolumes;
    private OffsetDateTime predictedDeliveryAt;
    private OffsetDateTime performanceFinishedAt;
    private OffsetDateTime ultimaOcorrenciaAt;
    private String ultimaOcorrenciaDescricao;
    private String metadata;

    public String getIdentificadorUnico() {
        return identificadorUnico;
    }

    public void setIdentificadorUnico(final String identificadorUnico) {
        this.identificadorUnico = identificadorUnico;
    }

    public Long getSequenceCode() {
        return sequenceCode;
    }

    public void setSequenceCode(final Long sequenceCode) {
        this.sequenceCode = sequenceCode;
    }

    public Long getNumeroMinuta() {
        return numeroMinuta;
    }

    public void setNumeroMinuta(final Long numeroMinuta) {
        this.numeroMinuta = numeroMinuta;
    }

    public String getPagadorNome() {
        return pagadorNome;
    }

    public void setPagadorNome(final String pagadorNome) {
        this.pagadorNome = pagadorNome;
    }

    public String getRemetenteNome() {
        return remetenteNome;
    }

    public void setRemetenteNome(final String remetenteNome) {
        this.remetenteNome = remetenteNome;
    }

    public String getOrigemCidade() {
        return origemCidade;
    }

    public void setOrigemCidade(final String origemCidade) {
        this.origemCidade = origemCidade;
    }

    public String getDestinatarioNome() {
        return destinatarioNome;
    }

    public void setDestinatarioNome(final String destinatarioNome) {
        this.destinatarioNome = destinatarioNome;
    }

    public String getDestinoCidade() {
        return destinoCidade;
    }

    public void setDestinoCidade(final String destinoCidade) {
        this.destinoCidade = destinoCidade;
    }

    public String getRegiaoEntrega() {
        return regiaoEntrega;
    }

    public void setRegiaoEntrega(final String regiaoEntrega) {
        this.regiaoEntrega = regiaoEntrega;
    }

    public String getFilialEntregadora() {
        return filialEntregadora;
    }

    public void setFilialEntregadora(final String filialEntregadora) {
        this.filialEntregadora = filialEntregadora;
    }

    public String getBranchNickname() {
        return branchNickname;
    }

    public void setBranchNickname(final String branchNickname) {
        this.branchNickname = branchNickname;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(final OffsetDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public OffsetDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(final OffsetDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public String getConferenteNome() {
        return conferenteNome;
    }

    public void setConferenteNome(final String conferenteNome) {
        this.conferenteNome = conferenteNome;
    }

    public String getInvoicesMapping() {
        return invoicesMapping;
    }

    public void setInvoicesMapping(final String invoicesMapping) {
        this.invoicesMapping = invoicesMapping;
    }

    public BigDecimal getInvoicesValue() {
        return invoicesValue;
    }

    public void setInvoicesValue(final BigDecimal invoicesValue) {
        this.invoicesValue = invoicesValue;
    }

    public BigDecimal getRealWeight() {
        return realWeight;
    }

    public void setRealWeight(final BigDecimal realWeight) {
        this.realWeight = realWeight;
    }

    public BigDecimal getTotalCubicVolume() {
        return totalCubicVolume;
    }

    public void setTotalCubicVolume(final BigDecimal totalCubicVolume) {
        this.totalCubicVolume = totalCubicVolume;
    }

    public BigDecimal getTaxedWeight() {
        return taxedWeight;
    }

    public void setTaxedWeight(final BigDecimal taxedWeight) {
        this.taxedWeight = taxedWeight;
    }

    public Integer getInvoicesVolumes() {
        return invoicesVolumes;
    }

    public void setInvoicesVolumes(final Integer invoicesVolumes) {
        this.invoicesVolumes = invoicesVolumes;
    }

    public Integer getReadVolumes() {
        return readVolumes;
    }

    public void setReadVolumes(final Integer readVolumes) {
        this.readVolumes = readVolumes;
    }

    public OffsetDateTime getPredictedDeliveryAt() {
        return predictedDeliveryAt;
    }

    public void setPredictedDeliveryAt(final OffsetDateTime predictedDeliveryAt) {
        this.predictedDeliveryAt = predictedDeliveryAt;
    }

    public OffsetDateTime getPerformanceFinishedAt() {
        return performanceFinishedAt;
    }

    public void setPerformanceFinishedAt(final OffsetDateTime performanceFinishedAt) {
        this.performanceFinishedAt = performanceFinishedAt;
    }

    public OffsetDateTime getUltimaOcorrenciaAt() {
        return ultimaOcorrenciaAt;
    }

    public void setUltimaOcorrenciaAt(final OffsetDateTime ultimaOcorrenciaAt) {
        this.ultimaOcorrenciaAt = ultimaOcorrenciaAt;
    }

    public String getUltimaOcorrenciaDescricao() {
        return ultimaOcorrenciaDescricao;
    }

    public void setUltimaOcorrenciaDescricao(final String ultimaOcorrenciaDescricao) {
        this.ultimaOcorrenciaDescricao = ultimaOcorrenciaDescricao;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(final String metadata) {
        this.metadata = metadata;
    }
}
