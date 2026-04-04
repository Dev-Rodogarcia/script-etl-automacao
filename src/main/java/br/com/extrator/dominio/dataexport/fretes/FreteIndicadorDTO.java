package br.com.extrator.dominio.dataexport.fretes;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FreteIndicadorDTO {

    @JsonProperty("corporation_sequence_number")
    private Long corporationSequenceNumber;

    @JsonProperty("finished_at")
    private String finishedAt;

    @JsonProperty("fit_dpn_performance_finished_at")
    private String performanceFinishedAt;

    public Long getCorporationSequenceNumber() {
        return corporationSequenceNumber;
    }

    public void setCorporationSequenceNumber(final Long corporationSequenceNumber) {
        this.corporationSequenceNumber = corporationSequenceNumber;
    }

    public String getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(final String finishedAt) {
        this.finishedAt = finishedAt;
    }

    public String getPerformanceFinishedAt() {
        return performanceFinishedAt;
    }

    public void setPerformanceFinishedAt(final String performanceFinishedAt) {
        this.performanceFinishedAt = performanceFinishedAt;
    }
}
