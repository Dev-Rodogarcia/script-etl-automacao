package br.com.extrator.plataforma.extracao.persistencia.sqlserver;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;

import org.junit.jupiter.api.Test;

class SqlServerRecoveryReplayGateTest {

    @Test
    void deveAceitarExatamenteUmaLinhaAfetadaNoStatusFinal() {
        assertDoesNotThrow(() ->
            SqlServerRecoveryReplayGate.validarAtualizacaoStatusFinal(1, "idem-1", "exec-1", "COMPLETED")
        );
    }

    @Test
    void deveFalharQuandoUpdateFinalStatusNaoAfetarNenhumaLinha() {
        final SQLException erro = assertThrows(
            SQLException.class,
            () -> SqlServerRecoveryReplayGate.validarAtualizacaoStatusFinal(0, "idem-1", "exec-1", "FAILED")
        );

        assertTrue(erro.getMessage().contains("rows_affected=0"));
        assertTrue(erro.getMessage().contains("idempotency_key=idem-1"));
        assertTrue(erro.getMessage().contains("status_pretendido=FAILED"));
    }
}
