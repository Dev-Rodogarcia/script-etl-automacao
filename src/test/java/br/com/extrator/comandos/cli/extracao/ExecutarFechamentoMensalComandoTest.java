package br.com.extrator.comandos.cli.extracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import br.com.extrator.aplicacao.extracao.ExtracaoPorIntervaloRequest;
import br.com.extrator.aplicacao.extracao.ExtracaoPorIntervaloUseCase;

class ExecutarFechamentoMensalComandoTest {

    @Test
    void deveExecutarMesAnteriorFechadoEmModoIntervalo() throws Exception {
        final CapturingExtracaoPorIntervaloUseCase useCase = new CapturingExtracaoPorIntervaloUseCase();
        final ExecutarFechamentoMensalComando comando = new ExecutarFechamentoMensalComando(
            useCase,
            () -> LocalDate.of(2026, 7, 2)
        );

        comando.executar(new String[] {"--fechamento-mensal"});

        assertNotNull(useCase.requestCapturada);
        assertEquals(LocalDate.of(2026, 6, 1), useCase.requestCapturada.dataInicio());
        assertEquals(LocalDate.of(2026, 6, 30), useCase.requestCapturada.dataFim());
        assertNull(useCase.requestCapturada.apiEspecifica());
        assertNull(useCase.requestCapturada.entidadeEspecifica());
        assertFalse(useCase.requestCapturada.modoLoopDaemon());
        assertFalse(useCase.requestCapturada.modoRapido24h());
        assertEquals(ExtracaoPorIntervaloRequest.ModoExecucao.INTERVALO, useCase.requestCapturada.modoExecucao());
    }

    @Test
    void deveResolverMesAnteriorMesmoNaViradaDoAno() throws Exception {
        final CapturingExtracaoPorIntervaloUseCase useCase = new CapturingExtracaoPorIntervaloUseCase();
        final ExecutarFechamentoMensalComando comando = new ExecutarFechamentoMensalComando(
            useCase,
            () -> LocalDate.of(2026, 1, 1)
        );

        comando.executar(new String[] {"--fechamento-mensal"});

        assertNotNull(useCase.requestCapturada);
        assertEquals(LocalDate.of(2025, 12, 1), useCase.requestCapturada.dataInicio());
        assertEquals(LocalDate.of(2025, 12, 31), useCase.requestCapturada.dataFim());
    }

    private static final class CapturingExtracaoPorIntervaloUseCase extends ExtracaoPorIntervaloUseCase {
        private ExtracaoPorIntervaloRequest requestCapturada;

        @Override
        public void executar(final ExtracaoPorIntervaloRequest request) {
            this.requestCapturada = request;
        }
    }
}
