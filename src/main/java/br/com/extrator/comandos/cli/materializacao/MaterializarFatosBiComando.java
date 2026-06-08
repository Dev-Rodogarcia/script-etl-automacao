package br.com.extrator.comandos.cli.materializacao;

import java.util.List;

import br.com.extrator.aplicacao.materializacao.FatoMaterializacaoJob;
import br.com.extrator.aplicacao.materializacao.FatoMaterializacaoProcedureResultado;
import br.com.extrator.aplicacao.materializacao.FatoMaterializacaoResumo;
import br.com.extrator.comandos.cli.base.Comando;
import br.com.extrator.suporte.configuracao.ConfigEtl;
import br.com.extrator.suporte.console.LoggerConsole;

public class MaterializarFatosBiComando implements Comando {
    private static final LoggerConsole log = LoggerConsole.getLogger(MaterializarFatosBiComando.class);

    private final FatoMaterializacaoJob job;

    public MaterializarFatosBiComando() {
        this(new FatoMaterializacaoJob());
    }

    MaterializarFatosBiComando(final FatoMaterializacaoJob job) {
        this.job = job;
    }

    @Override
    public void executar(final String[] args) throws Exception {
        final List<String> procedures = ConfigEtl.obterMaterializacaoFatosBiProcedures();
        final int timeoutSegundos = ConfigEtl.obterMaterializacaoFatosBiTimeoutSegundos();
        final FatoMaterializacaoResumo resumo = job.executar(procedures, timeoutSegundos);
        imprimirResumo(resumo);
    }

    private void imprimirResumo(final FatoMaterializacaoResumo resumo) {
        log.console("Materializacao de fatos BI concluida");
        for (final FatoMaterializacaoProcedureResultado resultado : resumo.procedures()) {
            if (resultado.sucesso()) {
                log.console(
                    "  {} | status=OK | inseridas={} | atualizadas={} | snapshot={} | duracao_ms={}",
                    resultado.procedureName(),
                    resultado.linhasInseridas(),
                    resultado.linhasAtualizadas(),
                    resultado.snapshotEm(),
                    resultado.duracao().toMillis()
                );
            } else {
                log.console(
                    "  {} | status=FALHA | erro={} | duracao_ms={}",
                    resultado.procedureName(),
                    resultado.erro(),
                    resultado.duracao().toMillis()
                );
            }
        }
        log.console(
            "Totais | falhas={} | inseridas={} | atualizadas={} | duracao_ms={}",
            resumo.totalProceduresFalhas(),
            resumo.totalLinhasInseridas(),
            resumo.totalLinhasAtualizadas(),
            resumo.duracao().toMillis()
        );
    }
}
