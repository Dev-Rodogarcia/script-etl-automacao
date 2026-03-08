package br.com.extrator.aplicacao.portas;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Porta para validacao de integridade ETL apos uma execucao de pipeline.
 */
public interface IntegridadeEtlPort {

    ResultadoIntegridade validarExecucao(
        LocalDateTime inicioExecucao,
        LocalDateTime fimExecucao,
        Set<String> entidadesEsperadas,
        boolean modoLoopDaemon
    );

    final class ResultadoIntegridade {
        private final boolean valido;
        private final List<String> falhas;

        public ResultadoIntegridade(final boolean valido, final List<String> falhas) {
            this.valido = valido;
            this.falhas = List.copyOf(falhas);
        }

        public boolean isValido() {
            return valido;
        }

        public List<String> getFalhas() {
            return falhas;
        }
    }
}
