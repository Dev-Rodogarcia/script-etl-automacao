package br.com.extrator.comandos.cli.utilitarios;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.extrator.comandos.cli.base.Comando;
import br.com.extrator.features.usuarios.aplicacao.SincronizacaoUsuariosSistemaService;
import br.com.extrator.integracao.comum.EntityExtractor;

public class SincronizarUsuariosComando implements Comando {
    private static final Logger logger = LoggerFactory.getLogger(SincronizarUsuariosComando.class);

    private final SincronizacaoUsuariosSistemaService service;

    public SincronizarUsuariosComando() {
        this(new SincronizacaoUsuariosSistemaService());
    }

    SincronizarUsuariosComando(final SincronizacaoUsuariosSistemaService service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    @Override
    public void executar(final String[] args) throws Exception {
        logger.info("Iniciando sincronizacao completa explicita de dim_usuarios...");
        final EntityExtractor.SaveMetrics metrics = service.sincronizar();
        logger.info(
            "Sincronizacao completa de dim_usuarios concluida | user_id_unicos={} | operacoes={} | persistidos={} | noop={}",
            metrics.getTotalUnicos(),
            metrics.getRegistrosSalvos(),
            metrics.getRegistrosPersistidos(),
            metrics.getRegistrosNoOpIdempotente()
        );
    }
}
