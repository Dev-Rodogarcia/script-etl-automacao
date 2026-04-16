package br.com.extrator.aplicacao.validacao;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import br.com.extrator.aplicacao.validacao.ValidacaoApiBanco24hDetalhadaTypes.ResultadoComparacao;
import br.com.extrator.suporte.validacao.ConstantesEntidades;

class ValidacaoApiBanco24hDetalhadaUseCaseTest {

    private final ValidacaoApiBanco24hDetalhadaUseCase useCase = new ValidacaoApiBanco24hDetalhadaUseCase();

    @AfterEach
    void limparConfig() {
        System.clearProperty("ETL_LATE_DATA_AUTO_REPLAY_ENABLED");
        System.clearProperty("ETL_LATE_DATA_AUTO_REPLAY_MAX_ATTEMPTS");
    }

    @Test
    void devePermitirReplayAutomaticoSomenteUmaVezParaFaltantesDeDadoTardio() {
        System.setProperty("ETL_LATE_DATA_AUTO_REPLAY_ENABLED", "true");
        System.setProperty("ETL_LATE_DATA_AUTO_REPLAY_MAX_ATTEMPTS", "1");
        final ValidacaoApiBanco24hDetalhadaRequest request = new ValidacaoApiBanco24hDetalhadaRequest(
            true,
            true,
            false,
            LocalDate.of(2026, 4, 15)
        );

        final ResultadoComparacao primeiraValidacaoComFaltante = new ResultadoComparacao(
            ConstantesEntidades.FRETES,
            756,
            756,
            0,
            755,
            1,
            0,
            0,
            true,
            null,
            "janela=[2026-04-15T13:18:12 .. 2026-04-15T13:20:50]"
        );
        final ResultadoComparacao segundaValidacaoVerde = new ResultadoComparacao(
            ConstantesEntidades.FRETES,
            756,
            756,
            0,
            756,
            0,
            0,
            0,
            true,
            null,
            "janela=[2026-04-15T13:18:12 .. 2026-04-15T13:20:50]"
        );

        assertTrue(useCase.somenteFaltantesPorDadoTardio(List.of(primeiraValidacaoComFaltante)));
        assertTrue(useCase.deveExecutarReplayDadoTardio(request, List.of(primeiraValidacaoComFaltante), 0));
        assertFalse(useCase.deveExecutarReplayDadoTardio(request, List.of(primeiraValidacaoComFaltante), 1));
        assertFalse(useCase.deveExecutarReplayDadoTardio(request, List.of(segundaValidacaoVerde), 0));
    }

    @Test
    void naoDeveExecutarReplayAutomaticoParaDivergenciaDeConteudo() {
        System.setProperty("ETL_LATE_DATA_AUTO_REPLAY_ENABLED", "true");
        final ValidacaoApiBanco24hDetalhadaRequest request = new ValidacaoApiBanco24hDetalhadaRequest(
            true,
            true,
            false,
            LocalDate.of(2026, 4, 15)
        );
        final ResultadoComparacao divergenciaConteudo = new ResultadoComparacao(
            ConstantesEntidades.MANIFESTOS,
            296,
            296,
            0,
            296,
            0,
            0,
            1,
            true,
            null,
            "amostra_divergencias_dados=[61379]"
        );

        assertFalse(useCase.somenteFaltantesPorDadoTardio(List.of(divergenciaConteudo)));
        assertFalse(useCase.deveExecutarReplayDadoTardio(request, List.of(divergenciaConteudo), 0));
    }
}
