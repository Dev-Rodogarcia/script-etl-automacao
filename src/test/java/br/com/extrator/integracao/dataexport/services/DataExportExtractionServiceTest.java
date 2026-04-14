package br.com.extrator.integracao.dataexport.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import br.com.extrator.integracao.comum.ExtractionResult;
import br.com.extrator.suporte.validacao.ConstantesEntidades;

class DataExportExtractionServiceTest {

    @Test
    void modoOperacionalAindaDeveFalharParaEntidadeCoreIncompleta() throws Exception {
        final String valorAnterior = System.getProperty("etl.integridade.modo");
        System.setProperty("etl.integridade.modo", "OPERACIONAL");
        try {
            final DataExportExtractionService service = new DataExportExtractionService();
            final Method method = DataExportExtractionService.class.getDeclaredMethod(
                "deveFalharExecucaoFinal",
                ExtractionResult.class,
                boolean.class
            );
            method.setAccessible(true);

            final boolean resultado = (boolean) method.invoke(
                service,
                resultado(ConstantesEntidades.MANIFESTOS, ConstantesEntidades.STATUS_INCOMPLETO_DADOS, false),
                false
            );

            assertTrue(resultado);
        } finally {
            restaurarPropriedade("etl.integridade.modo", valorAnterior);
        }
    }

    @Test
    void modoOperacionalNaoDeveFalharQuandoResultadoEstaCompleto() throws Exception {
        final DataExportExtractionService service = new DataExportExtractionService();
        final Method method = DataExportExtractionService.class.getDeclaredMethod(
            "deveFalharExecucaoFinal",
            ExtractionResult.class,
            boolean.class
        );
        method.setAccessible(true);

        final boolean resultado = (boolean) method.invoke(
            service,
            resultado(ConstantesEntidades.MANIFESTOS, ConstantesEntidades.STATUS_COMPLETO, true),
            false
        );

        assertFalse(resultado);
    }

    @Test
    void reflexaoDevePropagarErroQuandoMetodoPrivadoNaoExistir() {
        assertThrows(RuntimeException.class, () -> {
            try {
                DataExportExtractionService.class.getDeclaredMethod("metodo_inexistente");
            } catch (final NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private ExtractionResult resultado(final String entidade, final String status, final boolean sucesso) {
        return new ExtractionResult.Builder(entidade, LocalDateTime.of(2026, 4, 13, 10, 0))
            .fim(LocalDateTime.of(2026, 4, 13, 10, 1))
            .status(status)
            .registrosSalvos(sucesso ? 10 : 0)
            .registrosExtraidos(sucesso ? 10 : 0)
            .totalUnicos(sucesso ? 10 : 0)
            .paginasProcessadas(sucesso ? 1 : 0)
            .sucesso(sucesso)
            .build();
    }

    private void restaurarPropriedade(final String chave, final String valorAnterior) {
        if (valorAnterior == null) {
            System.clearProperty(chave);
            return;
        }
        System.setProperty(chave, valorAnterior);
    }
}
