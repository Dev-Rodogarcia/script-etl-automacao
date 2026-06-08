package br.com.extrator.aplicacao.materializacao;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FatoMaterializacaoJob {
    private static final Logger logger = LoggerFactory.getLogger(FatoMaterializacaoJob.class);

    private final FatoMaterializacaoRepository repository;

    public FatoMaterializacaoJob() {
        this(new FatoMaterializacaoRepository());
    }

    FatoMaterializacaoJob(final FatoMaterializacaoRepository repository) {
        this.repository = repository;
    }

    public FatoMaterializacaoResumo executar(final List<String> procedures,
                                             final int timeoutSegundos) throws Exception {
        final Instant inicio = Instant.now();
        logger.info("Iniciando materializacao de fatos BI | procedures={}", procedures);
        final List<FatoMaterializacaoProcedureResultado> resultados =
            repository.executarProcedures(procedures, timeoutSegundos);
        final FatoMaterializacaoResumo resumo =
            new FatoMaterializacaoResumo(resultados, Duration.between(inicio, Instant.now()));

        logger.info(
            "Materializacao de fatos BI concluida | procedures={} | falhas={} | inseridas={} | atualizadas={} | duracao_ms={}",
            resultados.size(),
            resumo.totalProceduresFalhas(),
            resumo.totalLinhasInseridas(),
            resumo.totalLinhasAtualizadas(),
            resumo.duracao().toMillis()
        );
        if (resumo.houveFalha()) {
            logger.warn(
                "Materializacao de fatos BI concluiu com falhas parciais | procedures_falhas={}",
                resumo.proceduresComFalha().stream()
                    .map(FatoMaterializacaoProcedureResultado::procedureName)
                    .toList()
            );
        }
        return resumo;
    }
}
