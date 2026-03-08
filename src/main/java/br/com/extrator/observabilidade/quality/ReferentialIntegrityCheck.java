package br.com.extrator.observabilidade.quality;

public final class ReferentialIntegrityCheck implements DataQualityCheck {
    @Override
    public String obterNome() {
        return "referential_integrity";
    }

    @Override
    public DataQualityCheckResult executar(final DataQualityContext context) {
        final long breaks = context.getQueryPort().contarQuebrasReferenciais(context.getEntidade());
        final long threshold = context.getMaxReferentialBreaks();
        final boolean passed = breaks <= threshold;
        final String mensagem = passed
            ? (breaks == 0L ? "Integridade referencial ok"
                            : "Quebras referenciais dentro da tolerancia (" + breaks + " <= " + threshold + ")")
            : "Quebras referenciais acima da tolerancia (" + breaks + " > " + threshold + ")";
        return new DataQualityCheckResult(
            context.getEntidade(),
            obterNome(),
            passed,
            breaks,
            threshold,
            mensagem
        );
    }
}


