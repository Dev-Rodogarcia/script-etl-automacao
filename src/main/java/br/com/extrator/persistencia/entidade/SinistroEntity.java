package br.com.extrator.persistencia.entidade;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public class SinistroEntity {

    private String identificadorUnico;
    private Long sequenceCode;
    private LocalDate openingAtDate;
    private LocalDate occurrenceAtDate;
    private String occurrenceAtTime;
    private LocalDate expectedSolutionDate;
    private String insuranceClaimLocation;
    private String informedBy;
    private LocalDate finishedAtDate;
    private String finishedAtTime;
    private Integer invoicesCount;
    private Long corporationSequenceNumber;
    private Long insuranceOccurrenceNumber;
    private Integer invoicesVolumes;
    private BigDecimal invoicesWeight;
    private BigDecimal invoicesValue;
    private String payerNickname;
    private BigDecimal customerDebitsSubtotal;
    private BigDecimal customerCreditEntriesSubtotal;
    private BigDecimal responsibleCreditsSubtotal;
    private BigDecimal responsibleDebitEntriesSubtotal;
    private BigDecimal insurerCreditsSubtotal;
    private BigDecimal insuranceClaimTotal;
    private String branchNickname;
    private String eventName;
    private String userName;
    private String vehiclePlate;
    private String occurrenceDescription;
    private String occurrenceCode;
    private OffsetDateTime treatmentAt;
    private String dealingType;
    private String solutionType;
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

    public LocalDate getOpeningAtDate() {
        return openingAtDate;
    }

    public void setOpeningAtDate(final LocalDate openingAtDate) {
        this.openingAtDate = openingAtDate;
    }

    public LocalDate getOccurrenceAtDate() {
        return occurrenceAtDate;
    }

    public void setOccurrenceAtDate(final LocalDate occurrenceAtDate) {
        this.occurrenceAtDate = occurrenceAtDate;
    }

    public String getOccurrenceAtTime() {
        return occurrenceAtTime;
    }

    public void setOccurrenceAtTime(final String occurrenceAtTime) {
        this.occurrenceAtTime = occurrenceAtTime;
    }

    public LocalDate getExpectedSolutionDate() {
        return expectedSolutionDate;
    }

    public void setExpectedSolutionDate(final LocalDate expectedSolutionDate) {
        this.expectedSolutionDate = expectedSolutionDate;
    }

    public String getInsuranceClaimLocation() {
        return insuranceClaimLocation;
    }

    public void setInsuranceClaimLocation(final String insuranceClaimLocation) {
        this.insuranceClaimLocation = insuranceClaimLocation;
    }

    public String getInformedBy() {
        return informedBy;
    }

    public void setInformedBy(final String informedBy) {
        this.informedBy = informedBy;
    }

    public LocalDate getFinishedAtDate() {
        return finishedAtDate;
    }

    public void setFinishedAtDate(final LocalDate finishedAtDate) {
        this.finishedAtDate = finishedAtDate;
    }

    public String getFinishedAtTime() {
        return finishedAtTime;
    }

    public void setFinishedAtTime(final String finishedAtTime) {
        this.finishedAtTime = finishedAtTime;
    }

    public Integer getInvoicesCount() {
        return invoicesCount;
    }

    public void setInvoicesCount(final Integer invoicesCount) {
        this.invoicesCount = invoicesCount;
    }

    public Long getCorporationSequenceNumber() {
        return corporationSequenceNumber;
    }

    public void setCorporationSequenceNumber(final Long corporationSequenceNumber) {
        this.corporationSequenceNumber = corporationSequenceNumber;
    }

    public Long getInsuranceOccurrenceNumber() {
        return insuranceOccurrenceNumber;
    }

    public void setInsuranceOccurrenceNumber(final Long insuranceOccurrenceNumber) {
        this.insuranceOccurrenceNumber = insuranceOccurrenceNumber;
    }

    public Integer getInvoicesVolumes() {
        return invoicesVolumes;
    }

    public void setInvoicesVolumes(final Integer invoicesVolumes) {
        this.invoicesVolumes = invoicesVolumes;
    }

    public BigDecimal getInvoicesWeight() {
        return invoicesWeight;
    }

    public void setInvoicesWeight(final BigDecimal invoicesWeight) {
        this.invoicesWeight = invoicesWeight;
    }

    public BigDecimal getInvoicesValue() {
        return invoicesValue;
    }

    public void setInvoicesValue(final BigDecimal invoicesValue) {
        this.invoicesValue = invoicesValue;
    }

    public String getPayerNickname() {
        return payerNickname;
    }

    public void setPayerNickname(final String payerNickname) {
        this.payerNickname = payerNickname;
    }

    public BigDecimal getCustomerDebitsSubtotal() {
        return customerDebitsSubtotal;
    }

    public void setCustomerDebitsSubtotal(final BigDecimal customerDebitsSubtotal) {
        this.customerDebitsSubtotal = customerDebitsSubtotal;
    }

    public BigDecimal getCustomerCreditEntriesSubtotal() {
        return customerCreditEntriesSubtotal;
    }

    public void setCustomerCreditEntriesSubtotal(final BigDecimal customerCreditEntriesSubtotal) {
        this.customerCreditEntriesSubtotal = customerCreditEntriesSubtotal;
    }

    public BigDecimal getResponsibleCreditsSubtotal() {
        return responsibleCreditsSubtotal;
    }

    public void setResponsibleCreditsSubtotal(final BigDecimal responsibleCreditsSubtotal) {
        this.responsibleCreditsSubtotal = responsibleCreditsSubtotal;
    }

    public BigDecimal getResponsibleDebitEntriesSubtotal() {
        return responsibleDebitEntriesSubtotal;
    }

    public void setResponsibleDebitEntriesSubtotal(final BigDecimal responsibleDebitEntriesSubtotal) {
        this.responsibleDebitEntriesSubtotal = responsibleDebitEntriesSubtotal;
    }

    public BigDecimal getInsurerCreditsSubtotal() {
        return insurerCreditsSubtotal;
    }

    public void setInsurerCreditsSubtotal(final BigDecimal insurerCreditsSubtotal) {
        this.insurerCreditsSubtotal = insurerCreditsSubtotal;
    }

    public BigDecimal getInsuranceClaimTotal() {
        return insuranceClaimTotal;
    }

    public void setInsuranceClaimTotal(final BigDecimal insuranceClaimTotal) {
        this.insuranceClaimTotal = insuranceClaimTotal;
    }

    public String getBranchNickname() {
        return branchNickname;
    }

    public void setBranchNickname(final String branchNickname) {
        this.branchNickname = branchNickname;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(final String eventName) {
        this.eventName = eventName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(final String userName) {
        this.userName = userName;
    }

    public String getVehiclePlate() {
        return vehiclePlate;
    }

    public void setVehiclePlate(final String vehiclePlate) {
        this.vehiclePlate = vehiclePlate;
    }

    public String getOccurrenceDescription() {
        return occurrenceDescription;
    }

    public void setOccurrenceDescription(final String occurrenceDescription) {
        this.occurrenceDescription = occurrenceDescription;
    }

    public String getOccurrenceCode() {
        return occurrenceCode;
    }

    public void setOccurrenceCode(final String occurrenceCode) {
        this.occurrenceCode = occurrenceCode;
    }

    public OffsetDateTime getTreatmentAt() {
        return treatmentAt;
    }

    public void setTreatmentAt(final OffsetDateTime treatmentAt) {
        this.treatmentAt = treatmentAt;
    }

    public String getDealingType() {
        return dealingType;
    }

    public void setDealingType(final String dealingType) {
        this.dealingType = dealingType;
    }

    public String getSolutionType() {
        return solutionType;
    }

    public void setSolutionType(final String solutionType) {
        this.solutionType = solutionType;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(final String metadata) {
        this.metadata = metadata;
    }
}
