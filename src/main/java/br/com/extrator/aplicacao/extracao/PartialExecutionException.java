package br.com.extrator.aplicacao.extracao;

/**
 * Excecao para sinalizar execucao parcial: parte do fluxo concluiu,
 * mas houve falhas que impedem classificar como sucesso total.
 */
public class PartialExecutionException extends RuntimeException {

    public PartialExecutionException(final String message) {
        super(message);
    }

    public PartialExecutionException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
