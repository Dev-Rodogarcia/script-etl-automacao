package br.com.extrator.comandos.cli.validacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import br.com.extrator.aplicacao.validacao.ValidacaoEtlResilienciaRequest;
import br.com.extrator.aplicacao.validacao.ValidacaoEtlResilienciaUseCase;

class ValidarEtlResilienciaComandoTest {

    @Test
    void deveMapearFlagsParaRequestCorretamente() throws Exception {
        final AtomicReference<ValidacaoEtlResilienciaRequest> capturado = new AtomicReference<>();
        final ValidarEtlResilienciaComando comando = new ValidarEtlResilienciaComando(new ValidacaoEtlResilienciaUseCase() {
            @Override
            public ReportFiles executar(final ValidacaoEtlResilienciaRequest request) {
                capturado.set(request);
                return new ReportFiles(null, null);
            }
        });

        comando.executar(new String[] {
            "--validar-etl-resiliencia",
            "--auto-chaos",
            "--ciclos", "12",
            "--duracao-segundos", "90",
            "--stress-concorrencia", "8",
            "--seed", "42",
            "--sem-cenarios-http"
        });

        assertTrue(capturado.get().autoChaos());
        assertEquals(12, capturado.get().maxCycles());
        assertEquals(Duration.ofSeconds(90), capturado.get().duracaoMaxima());
        assertEquals(8, capturado.get().stressConcorrencia());
        assertEquals(42L, capturado.get().seed());
        assertFalse(capturado.get().incluirCenariosHttp());
    }
}
