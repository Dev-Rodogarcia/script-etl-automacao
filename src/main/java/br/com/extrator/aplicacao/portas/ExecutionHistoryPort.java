package br.com.extrator.aplicacao.portas;

import java.time.LocalDateTime;

@FunctionalInterface
public interface ExecutionHistoryPort {
    int calcularTotalRegistros(LocalDateTime inicio, LocalDateTime fim);
}
