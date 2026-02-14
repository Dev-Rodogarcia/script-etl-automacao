package br.com.extrator.comandos.auditoria;

import br.com.extrator.auditoria.validacao.AuditorEstruturaApi;
import br.com.extrator.comandos.base.Comando;

/**
 * Comando para auditar a estrutura das APIs e gerar relatorio CSV.
 */
public class AuditarEstruturaApiComando implements Comando {

    @Override
    public void executar(final String[] args) throws Exception {
        final int exitCode = AuditorEstruturaApi.executar();
        if (exitCode != 0) {
            throw new IllegalStateException("Auditoria de estrutura finalizada com erro (codigo " + exitCode + ").");
        }
    }
}
