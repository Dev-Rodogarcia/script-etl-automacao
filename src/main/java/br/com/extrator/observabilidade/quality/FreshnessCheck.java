package br.com.extrator.observabilidade.quality;

import java.time.Duration;
import java.time.LocalDateTime;

public final class FreshnessCheck implements DataQualityCheck {
    @Override
    public String obterNome() {
        return "freshness";
    }

    @Override
    public DataQualityCheckResult executar(final DataQualityContext context) {
        final LocalDateTime latest = context.getQueryPort().buscarTimestampMaisRecente(context.getEntidade());
        if (latest == null) {
            return new DataQualityCheckResult(
                context.getEntidade(),
                obterNome(),
                false,
                Double.MAX_VALUE,
                context.getMaxLagMinutes(),
                "Sem timestamp de referencia"
            );
        }
        final long lagMin = Math.max(0L, Duration.between(latest, context.getNow()).toMinutes());
        final boolean passed = lagMin <= context.getMaxLagMinutes();
        return new DataQualityCheckResult(
            context.getEntidade(),
            obterNome(),
            passed,
            lagMin,
            context.getMaxLagMinutes(),
            passed ? "Freshness dentro do limite" : "Freshness acima do limite"
        );
    }
}


