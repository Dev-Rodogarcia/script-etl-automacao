package br.com.extrator.observabilidade.quality;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class DataQualityService {
    private final DataQualityQueryPort queryPort;
    private final List<DataQualityCheck> checks;
    private final int maxLagMinutes;
    private final String schemaVersion;
    private final int maxReferentialBreaks;

    public DataQualityService(
        final DataQualityQueryPort queryPort,
        final List<DataQualityCheck> checks,
        final int maxLagMinutes,
        final String schemaVersion,
        final int maxReferentialBreaks
    ) {
        this.queryPort = queryPort;
        this.checks = checks;
        this.maxLagMinutes = maxLagMinutes;
        this.schemaVersion = schemaVersion;
        this.maxReferentialBreaks = maxReferentialBreaks;
    }

    public DataQualityReport avaliar(final LocalDate dataInicio, final LocalDate dataFim, final List<String> entidades) {
        final List<DataQualityCheckResult> resultados = new ArrayList<>();
        final LocalDateTime now = LocalDateTime.now();

        for (String entidade : entidades) {
            final DataQualityContext context = new DataQualityContext(
                entidade,
                dataInicio,
                dataFim,
                now,
                queryPort,
                maxLagMinutes,
                schemaVersion,
                maxReferentialBreaks
            );
            for (DataQualityCheck check : checks) {
                resultados.add(check.executar(context));
            }
        }
        return new DataQualityReport(resultados);
    }
}


