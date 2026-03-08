package br.com.extrator.observabilidade.quality;

public final class CompletenessCheck implements DataQualityCheck {
    @Override
    public String obterNome() {
        return "completeness";
    }

    @Override
    public DataQualityCheckResult executar(final DataQualityContext context) {
        final long incompletos = context.getQueryPort().contarLinhasIncompletas(
            context.getEntidade(),
            context.getDataInicio(),
            context.getDataFim()
        );
        final boolean passed = incompletos == 0L;
        return new DataQualityCheckResult(
            context.getEntidade(),
            obterNome(),
            passed,
            incompletos,
            0.0d,
            passed ? "Completude ok" : "Registros incompletos detectados"
        );
    }
}


