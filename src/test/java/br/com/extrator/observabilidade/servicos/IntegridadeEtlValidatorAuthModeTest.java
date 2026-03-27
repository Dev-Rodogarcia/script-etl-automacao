package br.com.extrator.observabilidade.servicos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import br.com.extrator.aplicacao.portas.ExecutionAuditPort;
import br.com.extrator.plataforma.auditoria.dominio.ExecutionAuditRecord;
import br.com.extrator.suporte.observabilidade.ExecutionContext;
import br.com.extrator.suporte.validacao.ConstantesEntidades;

class IntegridadeEtlValidatorAuthModeTest {

    @AfterEach
    void limparContexto() {
        ExecutionContext.clear();
    }

    @Test
    void deveReprovarQuandoExecutionIdNaoEstiverInicializado() {
        final IntegridadeEtlValidator validator = new IntegridadeEtlValidator(new UnavailableExecutionAuditPort());

        final IntegridadeEtlValidator.ResultadoValidacao resultado = validator.validarExecucao(
            LocalDateTime.of(2026, 3, 25, 10, 0),
            LocalDateTime.of(2026, 3, 25, 10, 5),
            Set.of(ConstantesEntidades.COLETAS),
            false
        );

        assertFalse(resultado.isValido());
        assertEquals(1, resultado.getTotalEntidades());
        assertEquals(1, resultado.getEntidadesNaoOk());
        assertTrue(resultado.getFalhas().stream().anyMatch(falha -> falha.contains("EXECUTION_ID_AUSENTE")));
    }

    @Test
    void deveReprovarQuandoAuditoriaEstruturadaNaoEstiverDisponivel() {
        ExecutionContext.initialize("--fluxo-completo");
        final IntegridadeEtlValidator validator = new IntegridadeEtlValidator(new UnavailableExecutionAuditPort());

        final IntegridadeEtlValidator.ResultadoValidacao resultado = validator.validarExecucao(
            LocalDateTime.of(2026, 3, 25, 10, 0),
            LocalDateTime.of(2026, 3, 25, 10, 5),
            Set.of(ConstantesEntidades.COLETAS),
            false
        );

        assertFalse(resultado.isValido());
        assertEquals(1, resultado.getTotalEntidades());
        assertEquals(1, resultado.getEntidadesNaoOk());
        assertTrue(resultado.getFalhas().stream().anyMatch(falha -> falha.contains("AUDITORIA_ESTRUTURADA_INDISPONIVEL")));
    }

    private static final class UnavailableExecutionAuditPort implements ExecutionAuditPort {
        @Override
        public void registrarResultado(final ExecutionAuditRecord record) {
            // no-op
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

        @Override
        public boolean isDisponivel() {
            return false;
        }
    }
}
