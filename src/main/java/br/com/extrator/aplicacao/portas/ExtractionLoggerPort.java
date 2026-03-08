package br.com.extrator.aplicacao.portas;

import java.util.Map;

public interface ExtractionLoggerPort {
    void logarEstruturado(String eventName, Map<String, Object> fields);
}
