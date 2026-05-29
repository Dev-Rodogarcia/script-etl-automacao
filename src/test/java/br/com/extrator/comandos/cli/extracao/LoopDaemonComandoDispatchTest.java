package br.com.extrator.comandos.cli.extracao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.EnumMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import br.com.extrator.comandos.cli.extracao.LoopDaemonComando.Modo;
import br.com.extrator.comandos.cli.extracao.daemon.LoopDaemonModeHandler;

class LoopDaemonComandoDispatchTest {

    @Test
    void deveRoteiarModoStart() throws Exception {
        final RegistroHandlers registro = new RegistroHandlers();
        final LoopDaemonComando comando = new LoopDaemonComando(Modo.START, registro.handlers);

        comando.executar(new String[] {"--loop-daemon-start"});

        assertEquals(1, registro.startChamadas);
        assertEquals(0, registro.runChamadas);
    }

    @Test
    void deveRoteiarModoRunComFaturasPorPadrao() throws Exception {
        final RegistroHandlers registro = new RegistroHandlers();
        final LoopDaemonComando comando = new LoopDaemonComando(Modo.RUN, registro.handlers);

        comando.executar(new String[] {"--loop-daemon-run"});

        assertEquals(1, registro.runChamadas);
        assertEquals(0, registro.startChamadas);
    }

    private static final class RegistroHandlers {
        private int startChamadas;
        private int stopChamadas;
        private int statusChamadas;
        private int runChamadas;

        private final Map<Modo, LoopDaemonModeHandler> handlers;

        private RegistroHandlers() {
            final Map<Modo, LoopDaemonModeHandler> mapa = new EnumMap<>(Modo.class);
            mapa.put(Modo.START, () -> {
                startChamadas++;
            });
            mapa.put(Modo.STOP, () -> stopChamadas++);
            mapa.put(Modo.STATUS, () -> statusChamadas++);
            mapa.put(Modo.RUN, () -> {
                runChamadas++;
            });
            this.handlers = mapa;
        }
    }
}
