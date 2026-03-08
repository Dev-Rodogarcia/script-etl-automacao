package br.com.extrator.aplicacao.extracao;

public record TesteApiRequest(
    String tipoApi,
    String entidade,
    boolean incluirFaturasGraphQL
) {
}
