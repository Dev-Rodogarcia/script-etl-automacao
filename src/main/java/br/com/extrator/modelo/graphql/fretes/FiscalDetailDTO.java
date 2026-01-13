package br.com.extrator.modelo.graphql.fretes;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FiscalDetailDTO {
    @JsonProperty("cstType")
    private String cstType;

    @JsonProperty("taxValue")
    private Double taxValue;

    @JsonProperty("pisValue")
    private Double pisValue;

    @JsonProperty("cofinsValue")
    private Double cofinsValue;

    @JsonProperty("cfopCode")
    private String cfopCode;

    @JsonProperty("calculationBasis")
    private Double calculationBasis;

    @JsonProperty("taxRate")
    private Double taxRate;

    @JsonProperty("pisRate")
    private Double pisRate;

    @JsonProperty("cofinsRate")
    private Double cofinsRate;

    @JsonProperty("hasDifal")
    private Boolean hasDifal;

    @JsonProperty("difalTaxValueOrigin")
    private Double difalTaxValueOrigin;

    @JsonProperty("difalTaxValueDestination")
    private Double difalTaxValueDestination;

    public String getCstType() { return cstType; }
    public void setCstType(final String cstType) { this.cstType = cstType; }
    public Double getTaxValue() { return taxValue; }
    public void setTaxValue(final Double taxValue) { this.taxValue = taxValue; }
    public Double getPisValue() { return pisValue; }
    public void setPisValue(final Double pisValue) { this.pisValue = pisValue; }
    public Double getCofinsValue() { return cofinsValue; }
    public void setCofinsValue(final Double cofinsValue) { this.cofinsValue = cofinsValue; }
    public String getCfopCode() { return cfopCode; }
    public void setCfopCode(final String cfopCode) { this.cfopCode = cfopCode; }
    public Double getCalculationBasis() { return calculationBasis; }
    public void setCalculationBasis(final Double calculationBasis) { this.calculationBasis = calculationBasis; }
    public Double getTaxRate() { return taxRate; }
    public void setTaxRate(final Double taxRate) { this.taxRate = taxRate; }
    public Double getPisRate() { return pisRate; }
    public void setPisRate(final Double pisRate) { this.pisRate = pisRate; }
    public Double getCofinsRate() { return cofinsRate; }
    public void setCofinsRate(final Double cofinsRate) { this.cofinsRate = cofinsRate; }
    public Boolean getHasDifal() { return hasDifal; }
    public void setHasDifal(final Boolean hasDifal) { this.hasDifal = hasDifal; }
    public Double getDifalTaxValueOrigin() { return difalTaxValueOrigin; }
    public void setDifalTaxValueOrigin(final Double difalTaxValueOrigin) { this.difalTaxValueOrigin = difalTaxValueOrigin; }
    public Double getDifalTaxValueDestination() { return difalTaxValueDestination; }
    public void setDifalTaxValueDestination(final Double difalTaxValueDestination) { this.difalTaxValueDestination = difalTaxValueDestination; }
}
