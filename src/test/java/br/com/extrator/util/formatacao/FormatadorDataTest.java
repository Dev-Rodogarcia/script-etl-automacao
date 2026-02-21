package br.com.extrator.util.formatacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

class FormatadorDataTest {

    @Test
    void deveParsearOffsetDateTimeIsoComTimezone() {
        final OffsetDateTime resultado = FormatadorData.parseOffsetDateTime("2025-10-02T00:00:00.000-03:00");

        assertNotNull(resultado);
        assertEquals(2025, resultado.getYear());
        assertEquals(10, resultado.getMonthValue());
        assertEquals(2, resultado.getDayOfMonth());
        assertEquals(-3, resultado.getOffset().getTotalSeconds() / 3600);
    }

    @Test
    void deveParsearDataSemHoraComoInicioDoDiaNoOffsetPadrao() {
        final OffsetDateTime resultado = FormatadorData.parseOffsetDateTime("2025-10-07");

        assertNotNull(resultado);
        assertEquals(2025, resultado.getYear());
        assertEquals(10, resultado.getMonthValue());
        assertEquals(7, resultado.getDayOfMonth());
        assertEquals(0, resultado.getHour());
        assertEquals(-3, resultado.getOffset().getTotalSeconds() / 3600);
    }

    @Test
    void deveRetornarNullParaValorInvalido() {
        final OffsetDateTime resultado = FormatadorData.parseOffsetDateTime("data-invalida");

        assertNull(resultado);
    }
}
