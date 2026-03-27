package br.com.extrator.plataforma.auditoria.dominio;

import java.util.List;

/**
 * Resultado autorizativo de validacao da execucao corrente.
 */
public record ExecutionValidationResult(
    boolean valido,
    int totalEntidades,
    int entidadesNaoOk,
    List<String> falhas
) {
    public ExecutionValidationResult {
        falhas = List.copyOf(falhas);
    }
}
