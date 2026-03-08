package br.com.extrator.aplicacao.portas;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import br.com.extrator.aplicacao.extracao.LogExtracaoInfo;

public interface ExtractionLogQueryPort {
    Optional<LogExtracaoInfo> buscarUltimoLogPorEntidadeNoIntervaloExecucao(
        String entidade,
        LocalDateTime inicio,
        LocalDateTime fim
    );

    Optional<LogExtracaoInfo> buscarUltimaExtracaoPorPeriodo(
        String entidade,
        LocalDate dataInicio,
        LocalDate dataFim
    );
}
