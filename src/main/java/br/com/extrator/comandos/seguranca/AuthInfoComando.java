package br.com.extrator.comandos.seguranca;

import br.com.extrator.comandos.base.Comando;
import br.com.extrator.seguranca.SegurancaService;

/**
 * Comando para exibir informacoes do modulo de seguranca.
 */
public class AuthInfoComando implements Comando {

    @Override
    public void executar(final String[] args) throws Exception {
        final SegurancaService.ResumoSeguranca resumo = new SegurancaService().obterResumo();
        System.out.println("Banco de seguranca: " + resumo.dbPath());
        System.out.println("Usuarios ativos: " + resumo.usuariosAtivos());
        System.out.println("Eventos de auditoria: " + resumo.eventosAuditoria());
    }
}
