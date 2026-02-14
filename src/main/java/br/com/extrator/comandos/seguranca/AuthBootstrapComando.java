package br.com.extrator.comandos.seguranca;

import java.util.Arrays;

import br.com.extrator.comandos.base.Comando;
import br.com.extrator.seguranca.SegurancaConsolePrompt;
import br.com.extrator.seguranca.SegurancaService;

/**
 * Comando para criar o primeiro usuario administrador.
 */
public class AuthBootstrapComando implements Comando {

    @Override
    public void executar(final String[] args) throws Exception {
        final SegurancaService segurancaService = new SegurancaService();

        final String username = (args.length >= 2)
            ? args[1].trim()
            : SegurancaConsolePrompt.solicitarTextoObrigatorio("Username do ADMIN: ");
        final String displayName = (args.length >= 3)
            ? args[2].trim()
            : SegurancaConsolePrompt.solicitarTextoOpcional("Nome de exibicao (opcional): ");

        final char[] senha = SegurancaConsolePrompt.solicitarSenhaComConfirmacao(
            "Senha do ADMIN: ",
            "Confirmar senha: "
        );
        try {
            segurancaService.bootstrapAdmin(username, displayName, senha);
        } finally {
            Arrays.fill(senha, '\0');
        }

        final SegurancaService.ResumoSeguranca resumo = segurancaService.obterResumo();
        System.out.println("Bootstrap concluido com sucesso.");
        System.out.println("Banco de seguranca: " + resumo.dbPath());
        System.out.println("Usuarios ativos: " + resumo.usuariosAtivos());
    }
}
