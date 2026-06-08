package br.com.extrator.aplicacao.materializacao;

import java.util.List;

import br.com.extrator.suporte.configuracao.ConfigEtl;

public class FatoMaterializacaoService {
    private final FatoMaterializacaoJob job;

    public FatoMaterializacaoService() {
        this(new FatoMaterializacaoJob());
    }

    FatoMaterializacaoService(final FatoMaterializacaoJob job) {
        this.job = job;
    }

    public FatoMaterializacaoResumo processarTodasFatos() throws Exception {
        final List<String> procedures = ConfigEtl.obterMaterializacaoFatosBiProcedures();
        final int timeoutSegundos = ConfigEtl.obterMaterializacaoFatosBiTimeoutSegundos();
        return job.executar(procedures, timeoutSegundos);
    }
}
