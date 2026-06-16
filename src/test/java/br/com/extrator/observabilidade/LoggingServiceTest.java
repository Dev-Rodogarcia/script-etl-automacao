package br.com.extrator.observabilidade;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LoggingServiceTest {

    @Test
    void devePreservarDocumentosVersionadosDaRaizAoOrganizarArtefatos() {
        assertTrue(LoggingService.ehDocumentoVersionadoDaRaiz("readme.md"));
        assertTrue(LoggingService.ehDocumentoVersionadoDaRaiz("agents.md"));
        assertTrue(LoggingService.ehDocumentoVersionadoDaRaiz("readme.txt"));
        assertFalse(LoggingService.ehDocumentoVersionadoDaRaiz("relatorio-operacional.md"));
    }
}
