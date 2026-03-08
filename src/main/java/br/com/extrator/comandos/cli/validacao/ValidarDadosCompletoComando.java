package br.com.extrator.comandos.cli.validacao;

import br.com.extrator.aplicacao.validacao.ValidacaoDadosCompletoUseCase;
import br.com.extrator.comandos.cli.base.Comando;

public class ValidarDadosCompletoComando implements Comando {
    private final ValidacaoDadosCompletoUseCase useCase;

    public ValidarDadosCompletoComando() {
        this(new ValidacaoDadosCompletoUseCase());
    }

    ValidarDadosCompletoComando(final ValidacaoDadosCompletoUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    public void executar(final String[] args) throws Exception {
        useCase.executar();
    }
}
