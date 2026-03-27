package br.com.extrator.aplicacao.validacao;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import br.com.extrator.suporte.console.LoggerConsole;
import br.com.extrator.suporte.validacao.ConstantesEntidades;

class ValidacaoApiBanco24hDetalhadaComparatorTest {

    private final ValidacaoApiBanco24hDetalhadaComparator comparator =
        new ValidacaoApiBanco24hDetalhadaComparator(
            new ValidacaoApiBanco24hDetalhadaRepository(
                LoggerConsole.getLogger(ValidacaoApiBanco24hDetalhadaComparatorTest.class),
                new ValidacaoApiBanco24hDetalhadaMetadataHasher()
            )
        );

    @Test
    void deveTolerarManifestosMarginaisNaJanelaAberta() {
        comparator.definirPeriodoFechado(false);

        assertTrue(comparator.completudeDinamicaTolerada(resultado(
            ConstantesEntidades.MANIFESTOS,
            477,
            464,
            16,
            3,
            0
        )));
    }

    @Test
    void naoDeveTolerarManifestosMarginaisNoPeriodoFechado() {
        comparator.definirPeriodoFechado(true);

        assertFalse(comparator.completudeDinamicaTolerada(resultado(
            ConstantesEntidades.MANIFESTOS,
            477,
            464,
            16,
            3,
            0
        )));
    }

    @Test
    void deveTolerarCotacoesMarginaisNaJanelaAberta() {
        comparator.definirPeriodoFechado(false);

        assertTrue(comparator.completudeDinamicaTolerada(resultado(
            ConstantesEntidades.COTACOES,
            301,
            299,
            2,
            0,
            2
        )));
    }

    @Test
    void deveTolerarLocalizacaoContasEFaturasComDriftCurto() {
        comparator.definirPeriodoFechado(false);

        assertTrue(comparator.completudeDinamicaTolerada(resultado(
            ConstantesEntidades.LOCALIZACAO_CARGAS,
            953,
            951,
            2,
            0,
            4
        )));
        assertTrue(comparator.completudeDinamicaTolerada(resultado(
            ConstantesEntidades.CONTAS_A_PAGAR,
            218,
            216,
            2,
            0,
            0
        )));
        assertTrue(comparator.completudeDinamicaTolerada(resultado(
            ConstantesEntidades.FATURAS_POR_CLIENTE,
            953,
            951,
            2,
            0,
            0
        )));
    }

    private ValidacaoApiBanco24hDetalhadaTypes.ResultadoComparacao resultado(
        final String entidade,
        final int apiUnico,
        final int banco,
        final int faltantes,
        final int excedentes,
        final int divergenciasDados
    ) {
        return new ValidacaoApiBanco24hDetalhadaTypes.ResultadoComparacao(
            entidade,
            apiUnico,
            apiUnico,
            0,
            banco,
            faltantes,
            excedentes,
            divergenciasDados,
            true,
            null,
            "teste"
        );
    }
}
