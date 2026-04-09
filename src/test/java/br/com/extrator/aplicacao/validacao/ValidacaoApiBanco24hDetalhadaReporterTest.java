package br.com.extrator.aplicacao.validacao;

import static br.com.extrator.aplicacao.validacao.ValidacaoApiBanco24hDetalhadaTypes.ResumoExecucao;
import static br.com.extrator.aplicacao.validacao.ValidacaoApiBanco24hDetalhadaTypes.ResultadoComparacao;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.extrator.suporte.console.LoggerConsole;
import br.com.extrator.suporte.validacao.ConstantesEntidades;

class ValidacaoApiBanco24hDetalhadaReporterTest {

    private final ValidacaoApiBanco24hDetalhadaReporter reporter =
        new ValidacaoApiBanco24hDetalhadaReporter(
            LoggerConsole.getLogger(ValidacaoApiBanco24hDetalhadaReporterTest.class),
            new ValidacaoApiBanco24hDetalhadaComparator(null)
        );

    @Test
    void deveTratarDivergenciaDinamicaComoOk() {
        final ResultadoComparacao resultado = new ResultadoComparacao(
            ConstantesEntidades.FRETES,
            10,
            10,
            0,
            10,
            0,
            0,
            3,
            true,
            null,
            "somente drift"
        );

        final ResumoExecucao resumo = reporter.reportar(List.of(resultado));

        assertEquals(1, resumo.ok());
        assertEquals(0, resumo.falhas());
    }

    @Test
    void devePriorizarFalhaDeCompletude() {
        final ResultadoComparacao resultado = new ResultadoComparacao(
            ConstantesEntidades.MANIFESTOS,
            10,
            10,
            0,
            9,
            1,
            0,
            2,
            true,
            null,
            "faltante real"
        );

        final ResumoExecucao resumo = reporter.reportar(List.of(resultado));

        assertEquals(0, resumo.ok());
        assertEquals(1, resumo.falhas());
    }

    @Test
    void deveTolerarUsuariosSistemaComDriftPequeno() {
        final ResultadoComparacao resultado = new ResultadoComparacao(
            ConstantesEntidades.USUARIOS_SISTEMA,
            235,
            235,
            0,
            227,
            8,
            0,
            0,
            true,
            null,
            "snapshot vivo"
        );

        final ResumoExecucao resumo = reporter.reportar(List.of(resultado));

        assertEquals(1, resumo.ok());
        assertEquals(0, resumo.falhas());
    }

    @Test
    void deveTolerarColetasComDesvioMarginalDeCompletude() {
        final ResultadoComparacao resultado = new ResultadoComparacao(
            ConstantesEntidades.COLETAS,
            221,
            221,
            0,
            219,
            2,
            0,
            7,
            true,
            null,
            "requestDate fallback"
        );

        final ResumoExecucao resumo = reporter.reportar(List.of(resultado));

        assertEquals(1, resumo.ok());
        assertEquals(0, resumo.falhas());
    }

    @Test
    void deveTolerarColetasComDoisExcedentesEmJanelaAberta() {
        final ResultadoComparacao resultado = new ResultadoComparacao(
            ConstantesEntidades.COLETAS,
            288,
            288,
            0,
            290,
            0,
            2,
            0,
            true,
            null,
            "snapshot vivo com requestDate"
        );

        final ResumoExecucao resumo = reporter.reportar(List.of(resultado));

        assertEquals(1, resumo.ok());
        assertEquals(0, resumo.falhas());
    }

    @Test
    void deveTolerarContasAPagarComDriftMarginal() {
        final ResultadoComparacao resultado = new ResultadoComparacao(
            ConstantesEntidades.CONTAS_A_PAGAR,
            82,
            79,
            0,
            78,
            1,
            0,
            0,
            true,
            null,
            "snapshot vivo"
        );

        final ResumoExecucao resumo = reporter.reportar(List.of(resultado));

        assertEquals(1, resumo.ok());
        assertEquals(0, resumo.falhas());
    }

    @Test
    void deveTolerarFretesComUmFaltanteMarginalEmJanelaAberta() {
        final ResultadoComparacao resultado = new ResultadoComparacao(
            ConstantesEntidades.FRETES,
            941,
            941,
            0,
            940,
            1,
            0,
            0,
            true,
            null,
            "snapshot vivo do frete"
        );

        final ResumoExecucao resumo = reporter.reportar(List.of(resultado));

        assertEquals(1, resumo.ok());
        assertEquals(0, resumo.falhas());
    }

    @Test
    void deveTolerarManifestosComVariacaoMarginalSemDivergenciaDeConteudo() {
        final ResultadoComparacao resultado = new ResultadoComparacao(
            ConstantesEntidades.MANIFESTOS,
            323,
            322,
            0,
            319,
            4,
            1,
            0,
            true,
            null,
            "manifestos em transito"
        );

        final ResumoExecucao resumo = reporter.reportar(List.of(resultado));

        assertEquals(1, resumo.ok());
        assertEquals(0, resumo.falhas());
    }

    @Test
    void naoDeveTolerarFretesMarginaisEmPeriodoFechado() {
        final ValidacaoApiBanco24hDetalhadaComparator comparatorFechado =
            new ValidacaoApiBanco24hDetalhadaComparator(null);
        comparatorFechado.definirPeriodoFechado(true);
        final ValidacaoApiBanco24hDetalhadaReporter reporterFechado =
            new ValidacaoApiBanco24hDetalhadaReporter(
                LoggerConsole.getLogger(ValidacaoApiBanco24hDetalhadaReporterTest.class),
                comparatorFechado
            );
        final ResultadoComparacao resultado = new ResultadoComparacao(
            ConstantesEntidades.FRETES,
            941,
            941,
            0,
            940,
            1,
            0,
            0,
            true,
            null,
            "periodo fechado"
        );

        final ResumoExecucao resumo = reporterFechado.reportar(List.of(resultado));

        assertEquals(0, resumo.ok());
        assertEquals(1, resumo.falhas());
    }

    @Test
    void deveTolerarManifestoAbertoComUmaDivergenciaDeConteudo() {
        final ResultadoComparacao resultado = new ResultadoComparacao(
            ConstantesEntidades.MANIFESTOS,
            369,
            369,
            0,
            369,
            0,
            0,
            1,
            true,
            null,
            "snapshot vivo do manifesto em aberto"
        );

        final ResumoExecucao resumo = reporter.reportar(List.of(resultado));

        assertEquals(1, resumo.ok());
        assertEquals(0, resumo.falhas());
    }

    @Test
    void naoDeveTolerarManifestoFechadoComUmaDivergenciaDeConteudo() {
        final ValidacaoApiBanco24hDetalhadaComparator comparatorFechado =
            new ValidacaoApiBanco24hDetalhadaComparator(null);
        comparatorFechado.definirPeriodoFechado(true);
        final ValidacaoApiBanco24hDetalhadaReporter reporterFechado =
            new ValidacaoApiBanco24hDetalhadaReporter(
                LoggerConsole.getLogger(ValidacaoApiBanco24hDetalhadaReporterTest.class),
                comparatorFechado
            );
        final ResultadoComparacao resultado = new ResultadoComparacao(
            ConstantesEntidades.MANIFESTOS,
            369,
            369,
            0,
            369,
            0,
            0,
            1,
            true,
            null,
            "manifesto fechado com divergencia"
        );

        final ResumoExecucao resumo = reporterFechado.reportar(List.of(resultado));

        assertEquals(0, resumo.ok());
        assertEquals(1, resumo.falhas());
    }

    @Test
    void deveTolerarLocalizacaoComUmExcedenteDeSnapshot() {
        final ResultadoComparacao resultado = new ResultadoComparacao(
            ConstantesEntidades.LOCALIZACAO_CARGAS,
            772,
            772,
            0,
            773,
            0,
            1,
            20,
            true,
            null,
            "snapshot vivo da origem"
        );

        final ResumoExecucao resumo = reporter.reportar(List.of(resultado));

        assertEquals(1, resumo.ok());
        assertEquals(0, resumo.falhas());
    }

    @Test
    void deveTolerarLocalizacaoComUmFaltanteEPequenaDivergenciaEmJanelaAberta() {
        final ResultadoComparacao resultado = new ResultadoComparacao(
            ConstantesEntidades.LOCALIZACAO_CARGAS,
            941,
            941,
            0,
            940,
            1,
            0,
            2,
            true,
            null,
            "snapshot vivo da localizacao"
        );

        final ResumoExecucao resumo = reporter.reportar(List.of(resultado));

        assertEquals(1, resumo.ok());
        assertEquals(0, resumo.falhas());
    }

    @Test
    void deveTolerarFaturasPorClienteComExcedenteMarginalDeSnapshot() {
        final ResultadoComparacao resultado = new ResultadoComparacao(
            ConstantesEntidades.FATURAS_POR_CLIENTE,
            772,
            772,
            0,
            773,
            0,
            1,
            1,
            true,
            null,
            "snapshot vivo da origem"
        );

        final ResumoExecucao resumo = reporter.reportar(List.of(resultado));

        assertEquals(1, resumo.ok());
        assertEquals(0, resumo.falhas());
    }

    @Test
    void deveFalharQuandoApiFoiInterrompida() {
        final ResultadoComparacao resultado = new ResultadoComparacao(
            ConstantesEntidades.COTACOES,
            10,
            10,
            0,
            10,
            0,
            0,
            0,
            false,
            "LACUNA_PAGINACAO_422",
            "api_extracao=INCOMPLETA"
        );

        final ResumoExecucao resumo = reporter.reportar(List.of(resultado));

        assertEquals(0, resumo.ok());
        assertEquals(1, resumo.falhas());
    }
}
