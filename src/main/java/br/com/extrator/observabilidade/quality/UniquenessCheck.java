package br.com.extrator.observabilidade.quality;

public final class UniquenessCheck implements DataQualityCheck {
    @Override
    public String obterNome() {
        return "uniqueness";
    }

    @Override
    public DataQualityCheckResult executar(final DataQualityContext context) {
        final long duplicados = context.getQueryPort().contarDuplicidadesChaveNatural(
            context.getEntidade(),
            context.getDataInicio(),
            context.getDataFim()
        );
        final boolean passed = duplicados == 0L;
        return new DataQualityCheckResult(
            context.getEntidade(),
            obterNome(),
            passed,
            duplicados,
            0.0d,
            passed ? "Nenhuma duplicidade detectada" : "Duplicidades detectadas"
        );
    }
}


