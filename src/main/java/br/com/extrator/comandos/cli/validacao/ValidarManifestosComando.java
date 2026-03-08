package br.com.extrator.comandos.cli.validacao;

import br.com.extrator.aplicacao.validacao.ValidacaoManifestosUseCase;
import br.com.extrator.comandos.cli.base.Comando;

public class ValidarManifestosComando implements Comando {
    private final ValidacaoManifestosUseCase useCase;

    public ValidarManifestosComando() {
        this(new ValidacaoManifestosUseCase());
    }

    ValidarManifestosComando(final ValidacaoManifestosUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    public void executar(final String[] args) throws Exception {
        useCase.executar();
    }
}
