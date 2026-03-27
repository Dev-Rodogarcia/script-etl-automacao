package br.com.extrator.plataforma.auditoria.dominio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Janela de consulta e confirmacao da execucao.
 */
public record ExecutionWindowPlan(
    LocalDate consultaDataInicio,
    LocalDate consultaDataFim,
    LocalDateTime confirmacaoInicio,
    LocalDateTime confirmacaoFim
) {
    public LocalDateTime consultaInicioDateTime() {
        return consultaDataInicio.atStartOfDay();
    }

    public LocalDateTime consultaFimDateTime() {
        return consultaDataFim.atTime(LocalTime.MAX);
    }
}
