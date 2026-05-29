package br.com.extrator.comandos.cli.extracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void deveInferirApiRasterParaRasterViagensQuandoApiNaoForInformada() throws Exception {
        final CapturingExtracaoPorIntervaloUseCase useCase = new CapturingExtracaoPorIntervaloUseCase();
        final ExecutarExtracaoPorIntervaloComando comando = new ExecutarExtracaoPorIntervaloComando(useCase);

        comando.executar(new String[] {"--extracao-intervalo", "2026-04-01", "2026-04-02", "raster_viagens"});

        assertNotNull(useCase.requestCapturada);
        assertEquals("raster", useCase.requestCapturada.apiEspecifica());
        assertEquals("raster_viagens", useCase.requestCapturada.entidadeEspecifica());
    }

    @Test
    void deveAceitarApiRasterExplicitamente() throws Exception {
        final CapturingExtracaoPorIntervaloUseCase useCase = new CapturingExtracaoPorIntervaloUseCase();
        final ExecutarExtracaoPorIntervaloComando comando = new ExecutarExtracaoPorIntervaloComando(useCase);

        comando.executar(new String[] {"--extracao-intervalo", "2026-04-01", "2026-04-02", "raster"});

        assertNotNull(useCase.requestCapturada);
        assertEquals("raster", useCase.requestCapturada.apiEspecifica());
    }

    @Test
    void deveInferirApiRasterParaTabelaDerivadaDeParadas() throws Exception {
        final CapturingExtracaoPorIntervaloUseCase useCase = new CapturingExtracaoPorIntervaloUseCase();
        final ExecutarExtracaoPorIntervaloComando comando = new ExecutarExtracaoPorIntervaloComando(useCase);

        comando.executar(new String[] {"--extracao-intervalo", "2026-04-01", "2026-04-02", "raster_viagem_paradas"});

        assertNotNull(useCase.requestCapturada);
        assertEquals("raster", useCase.requestCapturada.apiEspecifica());
        assertEquals("raster_viagem_paradas", useCase.requestCapturada.entidadeEspecifica());
    }

    @Test
    void deveAtivarModoRapido24h() throws Exception {
        final CapturingExtracaoPorIntervaloUseCase useCase = new CapturingExtracaoPorIntervaloUseCase();
        final ExecutarExtracaoPorIntervaloComando comando = new ExecutarExtracaoPorIntervaloComando(useCase);

        comando.executar(new String[] {
            "--extracao-intervalo",
            "2026-04-27",
            "2026-04-28",
            "--modo-rapido-24h"
        });

        assertNotNull(useCase.requestCapturada);
        assertTrue(useCase.requestCapturada.modoRapido24h());
    }

    @Test
    void modoLoopDaemonDeveGerarRequestMicroBatch() throws Exception {
        final CapturingExtracaoPorIntervaloUseCase useCase = new CapturingExtracaoPorIntervaloUseCase();
        final ExecutarExtracaoPorIntervaloComando comando = new ExecutarExtracaoPorIntervaloComando(useCase);

        comando.executar(new String[] {
            "--extracao-intervalo",
            "2026-04-27",
            "2026-04-28",
            "--modo-loop-daemon"
        });

        assertNotNull(useCase.requestCapturada);
        assertTrue(useCase.requestCapturada.modoLoopDaemon());
        assertEquals(ExtracaoPorIntervaloRequest.ModoExecucao.MICRO_BATCH, useCase.requestCapturada.modoExecucao());
    }

    private static final class CapturingExtracaoPorIntervaloUseCase extends ExtracaoPorIntervaloUseCase {
        private ExtracaoPorIntervaloRequest requestCapturada;

        @Override
        public void executar(final ExtracaoPorIntervaloRequest request) {
            this.requestCapturada = request;
        }
    }
}
