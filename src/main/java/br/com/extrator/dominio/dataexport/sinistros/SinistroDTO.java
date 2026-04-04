package br.com.extrator.dominio.dataexport.sinistros;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SinistroDTO {

    @JsonProperty("sequence_code")
    private Long sequenceCode;

    @JsonProperty("opening_at_date")
    private String openingAtDate;

    @JsonProperty("occurrence_at_date")
    private String occurrenceAtDate;

    @JsonProperty("occurrence_at_time")
    private String occurrenceAtTime;

    @JsonProperty("expected_solution_date")
    private String expectedSolutionDate;

    @JsonProperty("insurance_claim_location")
    private String insuranceClaimLocation;

    @JsonProperty("informed_by")
    private String informedBy;

    @JsonProperty("finished_at_date")
    private String finishedAtDate;

    @JsonProperty("finished_at_time")
    private String finishedAtTime;

    @JsonProperty("invoices_count")
    private Integer invoicesCount;

    @JsonProperty("icm_fis_fit_corporation_sequence_number")
    private Long corporationSequenceNumber;

    @JsonProperty("icm_fis_ioe_number")
    private Long insuranceOccurrenceNumber;

    @JsonProperty("invoices_volumes")
    private Integer invoicesVolumes;

    @JsonProperty("invoices_weight")
    private String invoicesWeight;

    @JsonProperty("invoices_value")
    private String invoicesValue;

    @JsonProperty("icm_fis_fit_pyr_nickname")
    private String payerNickname;

    @JsonProperty("customer_debits_subtotal")
    private String customerDebitsSubtotal;

    @JsonProperty("customer_credit_entries_subtotal")
    private String customerCreditEntriesSubtotal;

    @JsonProperty("responsible_credits_subtotal")
    private String responsibleCreditsSubtotal;

    @JsonProperty("responsible_debit_entries_subtotal")
    private String responsibleDebitEntriesSubtotal;

    @JsonProperty("insurer_credits_subtotal")
    private String insurerCreditsSubtotal;

    @JsonProperty("insurance_claim_total")
    private String insuranceClaimTotal;

    @JsonProperty("icm_crn_psn_nickname")
    private String branchNickname;

    @JsonProperty("icm_dvr_iil_name")
    private String eventName;

    @JsonProperty("icm_fer_name")
    private String userName;

    @JsonProperty("icm_vie_license_plate")
    private String vehiclePlate;

    @JsonProperty("icm_ttt_ore_description")
    private String occurrenceDescription;

    @JsonProperty("icm_ttt_ore_code")
    private String occurrenceCode;

    @JsonProperty("icm_ttt_treatment_at")
    private String treatmentAt;

    @JsonProperty("icm_ttt_dealing_type")
    private String dealingType;

    @JsonProperty("icm_ttt_solution_type")
    private String solutionType;

    private final Map<String, Object> otherProperties = new HashMap<>();

    @JsonAnySetter
    public void add(final String key, final Object value) {
        otherProperties.put(key, value);
    }

    @JsonAnyGetter
    public Map<String, Object> getOtherProperties() {
        return otherProperties;
    }

    public Map<String, Object> getAllProperties() {
        final Map<String, Object> all = new LinkedHashMap<>();
        all.put("sequence_code", sequenceCode);
        all.put("opening_at_date", openingAtDate);
        all.put("occurrence_at_date", occurrenceAtDate);
        all.put("occurrence_at_time", occurrenceAtTime);
        all.put("expected_solution_date", expectedSolutionDate);
        all.put("insurance_claim_location", insuranceClaimLocation);
        all.put("informed_by", informedBy);
        all.put("finished_at_date", finishedAtDate);
        all.put("finished_at_time", finishedAtTime);
        all.put("invoices_count", invoicesCount);
        all.put("icm_fis_fit_corporation_sequence_number", corporationSequenceNumber);
        all.put("icm_fis_ioe_number", insuranceOccurrenceNumber);
        all.put("invoices_volumes", invoicesVolumes);
        all.put("invoices_weight", invoicesWeight);
        all.put("invoices_value", invoicesValue);
        all.put("icm_fis_fit_pyr_nickname", payerNickname);
        all.put("customer_debits_subtotal", customerDebitsSubtotal);
        all.put("customer_credit_entries_subtotal", customerCreditEntriesSubtotal);
        all.put("responsible_credits_subtotal", responsibleCreditsSubtotal);
        all.put("responsible_debit_entries_subtotal", responsibleDebitEntriesSubtotal);
        all.put("insurer_credits_subtotal", insurerCreditsSubtotal);
        all.put("insurance_claim_total", insuranceClaimTotal);
        all.put("icm_crn_psn_nickname", branchNickname);
        all.put("icm_dvr_iil_name", eventName);
        all.put("icm_fer_name", userName);
        all.put("icm_vie_license_plate", vehiclePlate);
        all.put("icm_ttt_ore_description", occurrenceDescription);
        all.put("icm_ttt_ore_code", occurrenceCode);
        all.put("icm_ttt_treatment_at", treatmentAt);
        all.put("icm_ttt_dealing_type", dealingType);
        all.put("icm_ttt_solution_type", solutionType);
        all.putAll(otherProperties);
        return all;
    }

    public Long getSequenceCode() {
        return sequenceCode;
    }

    public void setSequenceCode(final Long sequenceCode) {
        this.sequenceCode = sequenceCode;
    }

    public String getOpeningAtDate() {
        return openingAtDate;
    }

    public void setOpeningAtDate(final String openingAtDate) {
        this.openingAtDate = openingAtDate;
    }

    public String getOccurrenceAtDate() {
        return occurrenceAtDate;
    }

    public void setOccurrenceAtDate(final String occurrenceAtDate) {
        this.occurrenceAtDate = occurrenceAtDate;
    }

    public String getOccurrenceAtTime() {
        return occurrenceAtTime;
    }

    public void setOccurrenceAtTime(final String occurrenceAtTime) {
        this.occurrenceAtTime = occurrenceAtTime;
    }

    public String getExpectedSolutionDate() {
        return expectedSolutionDate;
    }

    public void setExpectedSolutionDate(final String expectedSolutionDate) {
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

    public String getFinishedAtDate() {
        return finishedAtDate;
    }

    public void setFinishedAtDate(final String finishedAtDate) {
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

    public String getInvoicesWeight() {
        return invoicesWeight;
    }

    public void setInvoicesWeight(final String invoicesWeight) {
        this.invoicesWeight = invoicesWeight;
    }

    public String getInvoicesValue() {
        return invoicesValue;
    }

    public void setInvoicesValue(final String invoicesValue) {
        this.invoicesValue = invoicesValue;
    }

    public String getPayerNickname() {
        return payerNickname;
    }

    public void setPayerNickname(final String payerNickname) {
        this.payerNickname = payerNickname;
    }

    public String getCustomerDebitsSubtotal() {
        return customerDebitsSubtotal;
    }

    public void setCustomerDebitsSubtotal(final String customerDebitsSubtotal) {
        this.customerDebitsSubtotal = customerDebitsSubtotal;
    }

    public String getCustomerCreditEntriesSubtotal() {
        return customerCreditEntriesSubtotal;
    }

    public void setCustomerCreditEntriesSubtotal(final String customerCreditEntriesSubtotal) {
        this.customerCreditEntriesSubtotal = customerCreditEntriesSubtotal;
    }

    public String getResponsibleCreditsSubtotal() {
        return responsibleCreditsSubtotal;
    }

    public void setResponsibleCreditsSubtotal(final String responsibleCreditsSubtotal) {
        this.responsibleCreditsSubtotal = responsibleCreditsSubtotal;
    }

    public String getResponsibleDebitEntriesSubtotal() {
        return responsibleDebitEntriesSubtotal;
    }

    public void setResponsibleDebitEntriesSubtotal(final String responsibleDebitEntriesSubtotal) {
        this.responsibleDebitEntriesSubtotal = responsibleDebitEntriesSubtotal;
    }

    public String getInsurerCreditsSubtotal() {
        return insurerCreditsSubtotal;
    }

    public void setInsurerCreditsSubtotal(final String insurerCreditsSubtotal) {
        this.insurerCreditsSubtotal = insurerCreditsSubtotal;
    }

    public String getInsuranceClaimTotal() {
        return insuranceClaimTotal;
    }

    public void setInsuranceClaimTotal(final String insuranceClaimTotal) {
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

    public String getTreatmentAt() {
        return treatmentAt;
    }

    public void setTreatmentAt(final String treatmentAt) {
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
}
