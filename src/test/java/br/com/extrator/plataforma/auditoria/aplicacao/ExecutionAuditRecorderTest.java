package br.com.extrator.plataforma.auditoria.aplicacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import br.com.extrator.aplicacao.portas.ExecutionAuditPort;
import br.com.extrator.integracao.comum.ExtractionResult;
import br.com.extrator.plataforma.auditoria.dominio.ExecutionAuditRecord;
import br.com.extrator.suporte.observabilidade.ExecutionContext;

class ExecutionAuditRecorderTest {

    @AfterEach
    void limparContexto() {
        ExecutionContext.clear();
    }

    @Test
    void deveGravarTotaisEstruturadosSemPerderDadoDeduplicado() {
        final RecordingExecutionAuditPort port = new RecordingExecutionAuditPort();
        final String executionId = ExecutionContext.initialize("--fluxo-completo");

        final ExtractionResult result = new ExtractionResult.Builder("manifestos", LocalDateTime.of(2026, 3, 25, 8, 0))
            .fim(LocalDateTime.of(2026, 3, 25, 8, 5))
            .status("COMPLETO")
            .registrosExtraidos(10)
            .totalUnicos(8)
            .registrosSalvos(8)
            .registrosPersistidos(8)
            .registrosNoOpIdempotente(2)
            .registrosInvalidos(1)
            .paginasProcessadas(3)
            .apiCompleta(true)
            .janelaConsultaInicio(LocalDateTime.of(2026, 3, 24, 0, 0))
            .janelaConsultaFim(LocalDateTime.of(2026, 3, 25, 23, 59))
            .janelaConfirmacaoInicio(LocalDateTime.of(2026, 3, 24, 0, 0))
            .janelaConfirmacaoFim(LocalDateTime.of(2026, 3, 25, 23, 59))
            .mensagem("ok")
            .sucesso(true)
            .build();

        ExecutionAuditRecorder.registrar(port, result);

        assertNotNull(port.record);
        assertEquals(executionId, port.record.executionUuid());
        assertEquals(10, port.record.apiTotalBruto());
        assertEquals(8, port.record.apiTotalUnico());
        assertEquals(8, port.record.dbPersistidos());
        assertEquals(2, port.record.noopCount());
    }

    private static final class RecordingExecutionAuditPort implements ExecutionAuditPort {
        private ExecutionAuditRecord record;

        @Override
        public void registrarResultado(final ExecutionAuditRecord record) {
            this.record = record;
        }

        @Override
        public Optional<ExecutionAuditRecord> buscarResultado(final String executionUuid, final String entidade) {
            return Optional.empty();
        }

        @Override
        public List<ExecutionAuditRecord> listarResultados(final String executionUuid) {
            return List.of();
        }

        @Override
        public Optional<LocalDateTime> buscarWatermarkConfirmado(final String entidade) {
            return Optional.empty();
        }

        @Override
        public void atualizarWatermarkConfirmado(final String entidade, final LocalDateTime watermarkConfirmado) {
            // no-op
        }
    }
}
