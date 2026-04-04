package br.com.extrator.aplicacao.politicas;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.http.HttpTimeoutException;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;

import br.com.extrator.suporte.concorrencia.ExecutionTimeoutException;

class ErrorClassifierTest {

    private final ErrorClassifier classifier = new ErrorClassifier();

    @Test
    void deveClassificarExecutionTimeoutExceptionComoTimeout() {
        final ErrorTaxonomy taxonomy = classifier.classificar(new ExecutionTimeoutException("Timeout ao executar graphql:coletas"));

        assertEquals(ErrorTaxonomy.TIMEOUT, taxonomy);
    }

    @Test
    void deveClassificarTimeoutEncadeadoComoTimeout() {
        final RuntimeException error = new RuntimeException(
            "falha ao aguardar step",
            new TimeoutException("timed out waiting for child process")
        );

        final ErrorTaxonomy taxonomy = classifier.classificar(error);

        assertEquals(ErrorTaxonomy.TIMEOUT, taxonomy);
    }

    @Test
    void deveClassificarHttpTimeoutExceptionComoTimeout() {
        final ErrorTaxonomy taxonomy = classifier.classificar(new HttpTimeoutException("request timed out"));

        assertEquals(ErrorTaxonomy.TIMEOUT, taxonomy);
    }

    @Test
    void devePreservarClassificacaoDeErrosNaoRelacionadosATimeout() {
        final ErrorTaxonomy taxonomy = classifier.classificar(new IllegalArgumentException("validation failed"));

        assertEquals(ErrorTaxonomy.PERMANENT_VALIDATION_ERROR, taxonomy);
    }
}
