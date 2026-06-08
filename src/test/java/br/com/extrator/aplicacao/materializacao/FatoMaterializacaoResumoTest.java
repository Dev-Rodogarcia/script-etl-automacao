package br.com.extrator.aplicacao.materializacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

class FatoMaterializacaoResumoTest {

    @Test
    void deveContabilizarFalhasParciaisSemMisturarTotaisDeLinhas() {
        final FatoMaterializacaoProcedureResultado sucesso = new FatoMaterializacaoProcedureResultado(
            "dbo.sp_ok",
            10,
            20,
            LocalDateTime.of(2026, 6, 8, 21, 0),
            Duration.ofSeconds(1)
        );
        final FatoMaterializacaoProcedureResultado falha = FatoMaterializacaoProcedureResultado.falha(
            "dbo.sp_falha",
            new IllegalStateException("lock ocupado"),
            Duration.ofMillis(500)
        );

        final FatoMaterializacaoResumo resumo = new FatoMaterializacaoResumo(
            List.of(sucesso, falha),
            Duration.ofSeconds(2)
        );

        assertEquals(10, resumo.totalLinhasInseridas());
        assertEquals(20, resumo.totalLinhasAtualizadas());
        assertEquals(1, resumo.totalProceduresFalhas());
        assertTrue(resumo.houveFalha());
        assertEquals(List.of(falha), resumo.proceduresComFalha());
        assertTrue(sucesso.sucesso());
        assertFalse(falha.sucesso());
    }
}
