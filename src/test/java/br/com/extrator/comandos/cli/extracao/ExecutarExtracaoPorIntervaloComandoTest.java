package br.com.extrator.comandos.cli.extracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import br.com.extrator.aplicacao.extracao.ExtracaoPorIntervaloRequest;
import br.com.extrator.aplicacao.extracao.ExtracaoPorIntervaloUseCase;

class ExecutarExtracaoPorIntervaloComandoTest {

    @Test
    void deveInferirApiDataExportParaInventarioQuandoApiNaoForInformada() throws Exception {
        final CapturingExtracaoPorIntervaloUseCase useCase = new CapturingExtracaoPorIntervaloUseCase();
        final ExecutarExtracaoPorIntervaloComando comando = new ExecutarExtracaoPorIntervaloComando(useCase);

        comando.executar(new String[] {"--extracao-intervalo", "2026-04-01", "2026-04-02", "inventario"});

        assertNotNull(useCase.requestCapturada);
        assertEquals("dataexport", useCase.requestCapturada.apiEspecifica());
        assertEquals("inventario", useCase.requestCapturada.entidadeEspecifica());
    }

    @Test
    void deveInferirApiDataExportParaSinistrosQuandoApiNaoForInformada() throws Exception {
        final CapturingExtracaoPorIntervaloUseCase useCase = new CapturingExtracaoPorIntervaloUseCase();
        final ExecutarExtracaoPorIntervaloComando comando = new ExecutarExtracaoPorIntervaloComando(useCase);

        comando.executar(new String[] {"--extracao-intervalo", "2026-04-01", "2026-04-02", "sinistros"});

        assertNotNull(useCase.requestCapturada);
        assertEquals("dataexport", useCase.requestCapturada.apiEspecifica());
        assertEquals("sinistros", useCase.requestCapturada.entidadeEspecifica());
    }

    private static final class CapturingExtracaoPorIntervaloUseCase extends ExtracaoPorIntervaloUseCase {
        private ExtracaoPorIntervaloRequest requestCapturada;

        @Override
        public void executar(final ExtracaoPorIntervaloRequest request) {
            this.requestCapturada = request;
        }
    }
}
