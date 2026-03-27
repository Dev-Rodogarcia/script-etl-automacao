/* ==[DOC-FILE]===============================================================
Arquivo : src/test/java/br/com/extrator/runners/common/ExtractionLoggerTest.java
Classe  : ExtractionLoggerTest (class)
Pacote  : br.com.extrator.integracao.comum
Modulo  : Teste automatizado
Papel   : Valida comportamento da unidade ExtractionLogger.

Conecta com:
- ResultadoExtracao (api)
- ConstantesEntidades (util.validacao)

Fluxo geral:
1) Prepara cenarios e dados de teste.
2) Executa casos para validar comportamento de ExtractionLogger.
3) Assegura regressao controlada nas regras principais.

Estrutura interna:
Metodos principais:
- deveClassificarComoCompletoQuandoSemDivergencias(): verifica comportamento esperado em teste automatizado.
- deveClassificarComoIncompletoDadosQuandoHaInvalidos(): verifica comportamento esperado em teste automatizado.
- deveClassificarComoIncompletoDbQuandoHaDivergenciaPersistencia(): verifica comportamento esperado em teste automatizado.
- deveConsiderarCompletoFaturasGraphqlQuandoBackfillAumentaVolumeSalvo(): verifica comportamento esperado em teste automatizado.
- deveClassificarComoErroApiQuandoMotivoForErroApi(): verifica comportamento esperado em teste automatizado.
- deveClassificarComoIncompletoLimiteQuandoMotivoNaoForErroApi(): verifica comportamento esperado em teste automatizado.
- executar(...2 args): executa o fluxo principal desta responsabilidade.
- executarGraphqlFaturas(...2 args): executa o fluxo principal desta responsabilidade.
Atributos-chave:
- Atributos nao mapeados automaticamente; consulte a implementacao abaixo.
[DOC-FILE-END]============================================================== */

package br.com.extrator.integracao.comum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import br.com.extrator.integracao.ResultadoExtracao;
import br.com.extrator.suporte.validacao.ConstantesEntidades;

class ExtractionLoggerTest {
    private static final String PROP_INTEGRIDADE = "ETL_INTEGRIDADE_MODO";
    private static final String PROP_INVALIDOS_QTD = "ETL_INVALIDOS_QUANTIDADE_MAX";
    private static final String PROP_INVALIDOS_PCT = "ETL_INVALIDOS_PERCENTUAL_MAX";

    @AfterEach
    void limparOverrides() {
        System.clearProperty(PROP_INTEGRIDADE);
        System.clearProperty(PROP_INVALIDOS_QTD);
        System.clearProperty(PROP_INVALIDOS_PCT);
    }

    @Test
    void deveClassificarComoCompletoQuandoSemDivergencias() {
        final ResultadoExtracao<String> resultadoExtracao = ResultadoExtracao.completo(List.of("a", "b"), 1, 2);
        final DataExportEntityExtractor.SaveResult saveResult = new DataExportEntityExtractor.SaveResult(2, 2, 0);
        final ExtractionResult result = executar(resultadoExtracao, saveResult);

        assertEquals(ConstantesEntidades.STATUS_COMPLETO, result.getStatus());
        assertTrue(result.isSucesso());
    }

    @Test
    void deveClassificarComoIncompletoDadosQuandoHaInvalidos() {
        System.setProperty(PROP_INTEGRIDADE, "STRICT_INTEGRITY");
        final ResultadoExtracao<String> resultadoExtracao = ResultadoExtracao.completo(List.of("a", "b"), 1, 2);
        final DataExportEntityExtractor.SaveResult saveResult = new DataExportEntityExtractor.SaveResult(2, 2, 1);
        final ExtractionResult result = executar(resultadoExtracao, saveResult);

        assertEquals(ConstantesEntidades.STATUS_INCOMPLETO_DADOS, result.getStatus());
        assertFalse(result.isSucesso());
    }

    @Test
    void deveClassificarComoCompletoQuandoExtractorPermiteInvalidosAuditadosNoModoEstrito() {
        System.setProperty(PROP_INTEGRIDADE, "STRICT_INTEGRITY");
        System.setProperty(PROP_INVALIDOS_QTD, "10");
        System.setProperty(PROP_INVALIDOS_PCT, "10.0");
        final ResultadoExtracao<String> resultadoExtracao =
            ResultadoExtracao.completo(List.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j"), 1, 10);
        final DataExportEntityExtractor.SaveResult saveResult = new DataExportEntityExtractor.SaveResult(9, 9, 1);
        final ExtractionResult result = executar(resultadoExtracao, saveResult, true);

        assertEquals(ConstantesEntidades.STATUS_COMPLETO, result.getStatus());
        assertEquals(1, result.getRegistrosInvalidos());
        assertTrue(result.isSucesso());
    }

    @Test
    void deveClassificarComoIncompletoDbQuandoHaDivergenciaPersistencia() {
        final ResultadoExtracao<String> resultadoExtracao = ResultadoExtracao.completo(List.of("a", "b"), 1, 2);
        final DataExportEntityExtractor.SaveResult saveResult = new DataExportEntityExtractor.SaveResult(1, 2, 0);
        final ExtractionResult result = executar(resultadoExtracao, saveResult);

        assertEquals(ConstantesEntidades.STATUS_INCOMPLETO_DB, result.getStatus());
        assertFalse(result.isSucesso());
    }

    @Test
    void deveConsiderarCompletoFaturasGraphqlQuandoBackfillAumentaVolumeSalvo() {
        final ResultadoExtracao<String> resultadoExtracao = ResultadoExtracao.completo(List.of("a", "b"), 1, 2);
        final ExtractionResult result = executarGraphqlFaturas(resultadoExtracao, 5);

        assertEquals(ConstantesEntidades.STATUS_COMPLETO, result.getStatus());
        assertEquals(5, result.getTotalUnicos());
        assertTrue(result.isSucesso());
    }

    @Test
    void deveClassificarComoErroApiQuandoMotivoForErroApi() {
        final ResultadoExtracao<String> resultadoExtracao = ResultadoExtracao.incompleto(
            List.of("a"),
            ResultadoExtracao.MotivoInterrupcao.ERRO_API,
            1,
            1
        );
        final DataExportEntityExtractor.SaveResult saveResult = new DataExportEntityExtractor.SaveResult(1, 1, 0);
        final ExtractionResult result = executar(resultadoExtracao, saveResult);

        assertEquals(ConstantesEntidades.STATUS_ERRO_API, result.getStatus());
        assertFalse(result.isSucesso());
    }

    @Test
    void deveClassificarComoIncompletoLimiteQuandoMotivoNaoForErroApi() {
        final ResultadoExtracao<String> resultadoExtracao = ResultadoExtracao.incompleto(
            List.of("a"),
            ResultadoExtracao.MotivoInterrupcao.LIMITE_PAGINAS,
            1,
            1
        );
        final DataExportEntityExtractor.SaveResult saveResult = new DataExportEntityExtractor.SaveResult(1, 1, 0);
        final ExtractionResult result = executar(resultadoExtracao, saveResult);

        assertEquals(ConstantesEntidades.STATUS_INCOMPLETO_LIMITE, result.getStatus());
        assertFalse(result.isSucesso());
    }

    private ExtractionResult executar(final ResultadoExtracao<String> resultadoExtracao,
                                      final DataExportEntityExtractor.SaveResult saveResult) {
        return executar(resultadoExtracao, saveResult, false);
    }

    private ExtractionResult executar(final ResultadoExtracao<String> resultadoExtracao,
                                      final DataExportEntityExtractor.SaveResult saveResult,
                                      final boolean permiteInvalidosAuditados) {
        final ExtractionLogger logger = new ExtractionLogger(ExtractionLoggerTest.class);
        final DummyDataExportExtractor extractor =
            new DummyDataExportExtractor(resultadoExtracao, saveResult, permiteInvalidosAuditados);
        return logger.executeWithLogging(extractor, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 1), "");
    }

    private ExtractionResult executarGraphqlFaturas(final ResultadoExtracao<String> resultadoExtracao,
                                                    final int salvos) {
        final ExtractionLogger logger = new ExtractionLogger(ExtractionLoggerTest.class);
        final DummyGraphqlFaturasExtractor extractor = new DummyGraphqlFaturasExtractor(resultadoExtracao, salvos);
        return logger.executeWithLogging(extractor, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 1), "");
    }

    private static final class DummyDataExportExtractor implements DataExportEntityExtractor<String> {
        private final ResultadoExtracao<String> resultadoExtracao;
        private final SaveResult saveResult;
        private final boolean permiteInvalidosAuditados;

        private DummyDataExportExtractor(final ResultadoExtracao<String> resultadoExtracao,
                                         final SaveResult saveResult,
                                         final boolean permiteInvalidosAuditados) {
            this.resultadoExtracao = resultadoExtracao;
            this.saveResult = saveResult;
            this.permiteInvalidosAuditados = permiteInvalidosAuditados;
        }

        @Override
        public ResultadoExtracao<String> extract(final LocalDate dataInicio, final LocalDate dataFim) {
            return resultadoExtracao;
        }

        @Override
        public SaveResult saveWithDeduplication(final List<String> dtos) {
            return saveResult;
        }

        @Override
        public String getEntityName() {
            return "entidade_teste";
        }

        @Override
        public String getEmoji() {
            return "";
        }

        @Override
        public boolean permiteConcluirComInvalidosAuditados() {
            return permiteInvalidosAuditados;
        }
    }

    private static final class DummyGraphqlFaturasExtractor implements EntityExtractor<String> {
        private final ResultadoExtracao<String> resultadoExtracao;
        private final int salvos;

        private DummyGraphqlFaturasExtractor(final ResultadoExtracao<String> resultadoExtracao,
                                             final int salvos) {
            this.resultadoExtracao = resultadoExtracao;
            this.salvos = salvos;
        }

        @Override
        public ResultadoExtracao<String> extract(final LocalDate dataInicio, final LocalDate dataFim) {
            return resultadoExtracao;
        }

        @Override
        public int save(final List<String> dtos) {
            return salvos;
        }

        @Override
        public String getEntityName() {
            return ConstantesEntidades.FATURAS_GRAPHQL;
        }

        @Override
        public String getEmoji() {
            return "";
        }
    }
}
