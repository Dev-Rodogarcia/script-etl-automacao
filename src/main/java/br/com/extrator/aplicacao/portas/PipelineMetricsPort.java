package br.com.extrator.aplicacao.portas;

import java.util.Map;

public interface PipelineMetricsPort {
    void registrarDuracaoEntidade(String entidade, long durationMillis);

    void incrementarSucesso(String entidade);

    void incrementarFalha(String entidade);

    Map<String, Double> obterSnapshot();
}


