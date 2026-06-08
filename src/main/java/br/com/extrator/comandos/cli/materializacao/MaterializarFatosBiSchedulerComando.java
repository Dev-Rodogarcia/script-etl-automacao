package br.com.extrator.comandos.cli.materializacao;

import java.time.Duration;
import java.util.List;

import br.com.extrator.aplicacao.materializacao.FatoMaterializacaoJob;
import br.com.extrator.aplicacao.materializacao.FatoMaterializacaoScheduler;
import br.com.extrator.comandos.cli.base.Comando;
import br.com.extrator.suporte.configuracao.ConfigEtl;

public class MaterializarFatosBiSchedulerComando implements Comando {
    @Override
    public void executar(final String[] args) throws Exception {
        final List<String> procedures = ConfigEtl.obterMaterializacaoFatosBiProcedures();
        final Duration intervalo = ConfigEtl.obterMaterializacaoFatosBiIntervalo();
        final Duration atrasoInicial = ConfigEtl.obterMaterializacaoFatosBiAtrasoInicial();
        final int timeoutSegundos = ConfigEtl.obterMaterializacaoFatosBiTimeoutSegundos();

        final FatoMaterializacaoScheduler scheduler = new FatoMaterializacaoScheduler(
            new FatoMaterializacaoJob(),
            procedures,
            intervalo,
            atrasoInicial,
            timeoutSegundos
        );
        scheduler.iniciarEBloquear();
    }
}
