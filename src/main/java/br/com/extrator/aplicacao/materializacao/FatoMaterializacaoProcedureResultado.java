package br.com.extrator.aplicacao.materializacao;

import java.time.Duration;
import java.time.LocalDateTime;

public record FatoMaterializacaoProcedureResultado(
    String procedureName,
    long linhasInseridas,
    long linhasAtualizadas,
    LocalDateTime snapshotEm,
    Duration duracao,
    boolean sucesso,
    String erro
) {
    private static final int ERRO_MAX_LENGTH = 500;

    public FatoMaterializacaoProcedureResultado(
        final String procedureName,
        final long linhasInseridas,
        final long linhasAtualizadas,
        final LocalDateTime snapshotEm,
        final Duration duracao
    ) {
        this(procedureName, linhasInseridas, linhasAtualizadas, snapshotEm, duracao, true, null);
    }

    public static FatoMaterializacaoProcedureResultado falha(final String procedureName,
                                                            final Throwable erro,
                                                            final Duration duracao) {
        return new FatoMaterializacaoProcedureResultado(
            procedureName,
            0L,
            0L,
            null,
            duracao,
            false,
            normalizarErro(erro)
        );
    }

    private static String normalizarErro(final Throwable erro) {
        if (erro == null) {
            return null;
        }
        final String mensagem = erro.getMessage();
        final String normalizada = mensagem == null || mensagem.isBlank()
            ? erro.getClass().getSimpleName()
            : mensagem.replaceAll("\\s+", " ").trim();
        return normalizada.length() <= ERRO_MAX_LENGTH
            ? normalizada
            : normalizada.substring(0, ERRO_MAX_LENGTH) + "...";
    }
}
