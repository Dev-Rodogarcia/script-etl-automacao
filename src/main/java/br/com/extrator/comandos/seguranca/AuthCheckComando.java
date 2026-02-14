package br.com.extrator.comandos.seguranca;

import java.util.Arrays;

import br.com.extrator.comandos.base.Comando;
import br.com.extrator.seguranca.AcaoSeguranca;
import br.com.extrator.seguranca.SegurancaConsolePrompt;
import br.com.extrator.seguranca.SegurancaService;

/**
 * Comando para autenticar usuario e autorizar acao sensivel.
 */
public class AuthCheckComando implements Comando {

    @Override
    public void executar(final String[] args) throws Exception {
        if (args.length < 2) {
            throw new IllegalArgumentException("Uso: --auth-check <ACAO_SEGURANCA> [detalhe]");
        }

        final AcaoSeguranca acao = AcaoSeguranca.fromToken(args[1]);
        final String detalhe = construirDetalhe(args);
        final SegurancaConsolePrompt.Credenciais credenciais = SegurancaConsolePrompt.solicitarCredenciais(
            "Autenticacao obrigatoria: " + acao.getDescricao()
        );
        final SegurancaService segurancaService = new SegurancaService();
        try {
            segurancaService.autenticarEAutorizar(credenciais.usuario(), credenciais.senha(), acao, detalhe);
        } finally {
            Arrays.fill(credenciais.senha(), '\0');
        }
        System.out.println("Acesso autorizado para " + acao.name() + ".");
    }

    private String construirDetalhe(final String[] args) {
        if (args.length < 3) {
            return null;
        }
        final StringBuilder sb = new StringBuilder();
        for (int i = 2; i < args.length; i++) {
            if (i > 2) {
                sb.append(' ');
            }
            sb.append(args[i]);
        }
        return sb.toString();
    }
}
