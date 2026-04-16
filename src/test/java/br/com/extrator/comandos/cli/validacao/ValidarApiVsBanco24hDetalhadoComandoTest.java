package br.com.extrator.comandos.cli.validacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import br.com.extrator.aplicacao.validacao.ValidacaoApiBanco24hDetalhadaRequest;
import br.com.extrator.aplicacao.validacao.ValidacaoApiBanco24hDetalhadaUseCase;

class ValidarApiVsBanco24hDetalhadoComandoTest {

    @Test
    void devePropagarExecutionUuidInformadoNaCli() throws Exception {
        final CapturingUseCase useCase = new CapturingUseCase();
        final ValidarApiVsBanco24hDetalhadoComando comando = new ValidarApiVsBanco24hDetalhadoComando(useCase);

        comando.executar(new String[] {
            "--validar-api-banco-24h-detalhado",
            "--periodo-fechado",
            "--sem-faturas-graphql",
            "--execution-uuid",
            "  af699a2e-0177-4c98-9ded-d0166fa4ada1  "
        });

        assertFalse(useCase.request.incluirFaturasGraphQL());
        assertTrue(useCase.request.periodoFechado());
        assertEquals("af699a2e-0177-4c98-9ded-d0166fa4ada1", useCase.request.executionUuidAncora());
        assertEquals("af699a2e-0177-4c98-9ded-d0166fa4ada1", useCase.request.executionUuidAncoraOpt().orElseThrow());
    }

    private static final class CapturingUseCase extends ValidacaoApiBanco24hDetalhadaUseCase {
        private ValidacaoApiBanco24hDetalhadaRequest request;

        @Override
        public void executar(final ValidacaoApiBanco24hDetalhadaRequest request) {
            this.request = request;
        }
    }
}
