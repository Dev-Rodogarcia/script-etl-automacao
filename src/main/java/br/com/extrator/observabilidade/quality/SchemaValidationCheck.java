package br.com.extrator.observabilidade.quality;

public final class SchemaValidationCheck implements DataQualityCheck {
    @Override
    public String obterNome() {
        return "schema_validation";
    }

    @Override
    public DataQualityCheckResult executar(final DataQualityContext context) {
        final String actual = context.getQueryPort().detectarVersaoSchema(context.getEntidade());
        final String expected = context.getExpectedSchemaVersion();
        final boolean passed = expected != null && expected.equalsIgnoreCase(actual);
        return new DataQualityCheckResult(
            context.getEntidade(),
            obterNome(),
            passed,
            passed ? 1.0d : 0.0d,
            1.0d,
            "expected=" + expected + ", actual=" + actual
        );
    }
}


