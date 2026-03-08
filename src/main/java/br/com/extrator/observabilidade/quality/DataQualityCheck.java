package br.com.extrator.observabilidade.quality;

public interface DataQualityCheck {
    String obterNome();

    DataQualityCheckResult executar(DataQualityContext context);
}


