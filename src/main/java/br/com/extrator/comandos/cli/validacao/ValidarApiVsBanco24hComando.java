package br.com.extrator.comandos.cli.validacao;

import br.com.extrator.aplicacao.validacao.ValidacaoApiBanco24hRequest;
import br.com.extrator.aplicacao.validacao.ValidacaoApiBanco24hUseCase;
import br.com.extrator.comandos.cli.base.Comando;
import br.com.extrator.suporte.tempo.RelogioSistema;

public class ValidarApiVsBanco24hComando implements Comando {
    private final ValidacaoApiBanco24hUseCase useCase;

    public ValidarApiVsBanco24hComando() {
        this(new ValidacaoApiBanco24hUseCase());
    }

    ValidarApiVsBanco24hComando(final ValidacaoApiBanco24hUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    public void executar(final String[] args) throws Exception {
        useCase.executar(
            new ValidacaoApiBanco24hRequest(
                !possuiFlag(args, "--sem-faturas-graphql"),
                possuiFlag(args, "--permitir-fallback-janela"),
                RelogioSistema.hoje()
            )
        );
    }

    private boolean possuiFlag(final String[] args, final String flag) {
        if (args == null || flag == null) {
            return false;
        }
        for (final String arg : args) {
            if (flag.equalsIgnoreCase(arg)) {
                return true;
            }
        }
        return false;
    }
}
