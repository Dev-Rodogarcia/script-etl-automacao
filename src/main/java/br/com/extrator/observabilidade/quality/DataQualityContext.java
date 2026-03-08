package br.com.extrator.observabilidade.quality;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class DataQualityContext {
    private final String entidade;
    private final LocalDate dataInicio;
    private final LocalDate dataFim;
    private final LocalDateTime now;
    private final DataQualityQueryPort queryPort;
    private final int maxLagMinutes;
    private final String expectedSchemaVersion;
    private final int maxReferentialBreaks;

    public DataQualityContext(
        final String entidade,
        final LocalDate dataInicio,
        final LocalDate dataFim,
        final LocalDateTime now,
        final DataQualityQueryPort queryPort,
        final int maxLagMinutes,
        final String expectedSchemaVersion,
        final int maxReferentialBreaks
    ) {
        this.entidade = entidade;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
        this.now = now;
        this.queryPort = queryPort;
        this.maxLagMinutes = maxLagMinutes;
        this.expectedSchemaVersion = expectedSchemaVersion;
        this.maxReferentialBreaks = maxReferentialBreaks;
    }

    public String getEntidade() {
        return entidade;
    }

    public LocalDate getDataInicio() {
        return dataInicio;
    }

    public LocalDate getDataFim() {
        return dataFim;
    }

    public LocalDateTime getNow() {
        return now;
    }

    public DataQualityQueryPort getQueryPort() {
        return queryPort;
    }

    public int getMaxLagMinutes() {
        return maxLagMinutes;
    }

    public String getExpectedSchemaVersion() {
        return expectedSchemaVersion;
    }

    public int getMaxReferentialBreaks() {
        return maxReferentialBreaks;
    }
}

