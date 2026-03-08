package br.com.extrator.aplicacao.extracao;

import java.time.LocalDateTime;

/**
 * Representacao de dominio do log de extracao, sem acoplamento com persistencia.
 */
public final class LogExtracaoInfo {

    public enum StatusExtracao {
        COMPLETO,
        INCOMPLETO,
        INCOMPLETO_LIMITE,
        INCOMPLETO_DADOS,
        INCOMPLETO_DB,
        ERRO_API
    }

    private final StatusExtracao statusFinal;
    private final LocalDateTime timestampFim;
    private final Integer registrosExtraidos;

    public LogExtracaoInfo(
        final StatusExtracao statusFinal,
        final LocalDateTime timestampFim,
        final Integer registrosExtraidos
    ) {
        this.statusFinal = statusFinal;
        this.timestampFim = timestampFim;
        this.registrosExtraidos = registrosExtraidos;
    }

    public StatusExtracao getStatusFinal() {
        return statusFinal;
    }

    public LocalDateTime getTimestampFim() {
        return timestampFim;
    }

    public Integer getRegistrosExtraidos() {
        return registrosExtraidos;
    }
}
