package br.com.extrator.aplicacao.contexto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import br.com.extrator.aplicacao.extracao.LogExtracaoInfo;
import br.com.extrator.aplicacao.portas.ExtractionLogQueryPort;

/**
 * Implementacao no-op usada quando o contexto nao foi inicializado.
 * Retorna Optional.empty() para todas as consultas.
 */
final class NoOpExtractionLogQueryPort implements ExtractionLogQueryPort {

    @Override
    public Optional<LogExtracaoInfo> buscarUltimoLogPorEntidadeNoIntervaloExecucao(
        final String entidade,
        final LocalDateTime inicio,
        final LocalDateTime fim
    ) {
        return Optional.empty();
    }

    @Override
    public Optional<LogExtracaoInfo> buscarUltimaExtracaoPorPeriodo(
        final String entidade,
        final LocalDate dataInicio,
        final LocalDate dataFim
    ) {
        return Optional.empty();
    }
}
