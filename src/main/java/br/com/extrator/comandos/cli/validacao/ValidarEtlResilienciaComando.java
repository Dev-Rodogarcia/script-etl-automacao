package br.com.extrator.comandos.cli.validacao;

import java.time.Duration;

import br.com.extrator.aplicacao.validacao.ValidacaoEtlResilienciaRequest;
import br.com.extrator.aplicacao.validacao.ValidacaoEtlResilienciaUseCase;
import br.com.extrator.comandos.cli.base.Comando;

public class ValidarEtlResilienciaComando implements Comando {
    private final ValidacaoEtlResilienciaUseCase useCase;

    public ValidarEtlResilienciaComando() {
        this(new ValidacaoEtlResilienciaUseCase());
    }

    ValidarEtlResilienciaComando(final ValidacaoEtlResilienciaUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    public void executar(final String[] args) throws Exception {
        useCase.executar(new ValidacaoEtlResilienciaRequest(
            possuiFlag(args, "--auto-chaos"),
            resolverInteiro(args, "--ciclos", 8),
            Duration.ofSeconds(resolverInteiro(args, "--duracao-segundos", 30)),
            resolverInteiro(args, "--stress-concorrencia", 6),
            resolverLong(args, "--seed", 20260317L),
            !possuiFlag(args, "--sem-cenarios-http")
        ));
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

    private int resolverInteiro(final String[] args, final String flag, final int defaultValue) {
        if (args == null) {
            return defaultValue;
        }
        for (int i = 0; i < args.length; i++) {
            if (!flag.equalsIgnoreCase(args[i]) || i + 1 >= args.length) {
                continue;
            }
            try {
                return Math.max(1, Integer.parseInt(args[i + 1]));
            } catch (final NumberFormatException e) {
                throw new IllegalArgumentException("Valor invalido para " + flag + ": " + args[i + 1]);
            }
        }
        return defaultValue;
    }

    private long resolverLong(final String[] args, final String flag, final long defaultValue) {
        if (args == null) {
            return defaultValue;
        }
        for (int i = 0; i < args.length; i++) {
            if (!flag.equalsIgnoreCase(args[i]) || i + 1 >= args.length) {
                continue;
            }
            try {
                return Long.parseLong(args[i + 1]);
            } catch (final NumberFormatException e) {
                throw new IllegalArgumentException("Valor invalido para " + flag + ": " + args[i + 1]);
            }
        }
        return defaultValue;
    }
}
