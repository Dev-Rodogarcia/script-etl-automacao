package br.com.extrator.integracao;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import br.com.extrator.suporte.formatacao.FormatadorData;

final class DataExportTimeWindowSupport {
    private final ZoneId zoneId;

    DataExportTimeWindowSupport(final ZoneId zoneId) {
        this.zoneId = zoneId;
    }

    static DataExportTimeWindowSupport createConfigured() {
        return new DataExportTimeWindowSupport(br.com.extrator.suporte.configuracao.ConfigApi.obterZoneIdDataExport());
    }

    Instant inicioDoDia(final LocalDate data) {
        return data.atStartOfDay(zoneId).toInstant();
    }

    Instant fimDoDia(final LocalDate data) {
        return data.plusDays(1).atStartOfDay(zoneId).toInstant();
    }

    LocalDate toLocalDate(final Instant instant) {
        return instant.atZone(zoneId).toLocalDate();
    }

    LocalDate toInclusiveEndDate(final Instant instant) {
        final ZonedDateTime zonedDateTime = instant.atZone(zoneId);
        if (LocalTime.MIDNIGHT.equals(zonedDateTime.toLocalTime())) {
            return zonedDateTime.toLocalDate().minusDays(1);
        }
        return zonedDateTime.toLocalDate();
    }

    String formatarRange(final Instant dataInicio, final Instant dataFim) {
        final String dataInicioStr = toLocalDate(dataInicio).format(FormatadorData.ISO_DATE);
        final String dataFimStr = toInclusiveEndDate(dataFim).format(FormatadorData.ISO_DATE);
        return dataInicioStr + " - " + dataFimStr;
    }

    ZoneId getZoneId() {
        return zoneId;
    }
}
