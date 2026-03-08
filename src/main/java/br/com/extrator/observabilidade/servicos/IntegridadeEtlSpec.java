package br.com.extrator.observabilidade.servicos;

import java.util.List;

record IntegridadeEtlSpec(
    String entidade,
    String tabela,
    String colunaTimestamp,
    List<String> chavesUnicas,
    List<String> colunasObrigatorias
) {
    IntegridadeEtlSpec {
        chavesUnicas = List.copyOf(chavesUnicas);
        colunasObrigatorias = List.copyOf(colunasObrigatorias);
    }
}
