package br.com.extrator.features.usuarios.aplicacao;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.extrator.dominio.graphql.usuarios.IndividualNodeDTO;
import br.com.extrator.features.usuarios.persistencia.sqlserver.SqlServerUsuariosEstadoRepository;
import br.com.extrator.integracao.ClienteApiGraphQL;
import br.com.extrator.integracao.ResultadoExtracao;
import br.com.extrator.integracao.comum.EntityExtractor;
import br.com.extrator.integracao.mapeamento.graphql.usuarios.UsuarioSistemaMapper;
import br.com.extrator.persistencia.entidade.UsuarioSistemaEntity;
import br.com.extrator.suporte.observabilidade.ExecutionContext;

public final class SincronizacaoUsuariosSistemaService {
    private static final Logger logger = LoggerFactory.getLogger(SincronizacaoUsuariosSistemaService.class);

    private final ClienteApiGraphQL apiClient;
    private final UsuarioSistemaMapper mapper;
    private final UsuariosSistemaSnapshotService snapshotService;

    public SincronizacaoUsuariosSistemaService() {
        this(
            new ClienteApiGraphQL(),
            new UsuarioSistemaMapper(),
            new UsuariosSistemaSnapshotService(new SqlServerUsuariosEstadoRepository())
        );
    }

    SincronizacaoUsuariosSistemaService(final ClienteApiGraphQL apiClient,
                                        final UsuarioSistemaMapper mapper,
                                        final UsuariosSistemaSnapshotService snapshotService) {
        this.apiClient = Objects.requireNonNull(apiClient, "apiClient");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
        this.snapshotService = Objects.requireNonNull(snapshotService, "snapshotService");
    }

    public EntityExtractor.SaveMetrics sincronizar() throws SQLException {
        apiClient.setExecutionUuid(ExecutionContext.currentExecutionId());

        final ResultadoExtracao<IndividualNodeDTO> resultado = apiClient.buscarUsuariosSistema();
        validarResultadoCompleto(resultado);

        final List<UsuarioSistemaEntity> entities = resultado.getDados().stream()
            .map(mapper::toEntity)
            .collect(Collectors.toList());
        final List<UsuarioSistemaEntity> unicos = deduplicarPorUserId(entities);

        if (unicos.size() < entities.size()) {
            logger.warn(
                "sincronizar_usuarios: {} nos da API, {} user_id unicos (duplicados removidos).",
                entities.size(),
                unicos.size()
            );
        }

        logger.info(
            "sincronizar_usuarios: snapshot completo autorizado | api_bruto={} | user_id_unicos={} | paginas_processadas={}",
            entities.size(),
            unicos.size(),
            resultado.getPaginasProcessadas()
        );
        return snapshotService.persistirSnapshot(unicos);
    }

    private void validarResultadoCompleto(final ResultadoExtracao<IndividualNodeDTO> resultado) {
        if (resultado != null && resultado.isCompleto()) {
            return;
        }

        final String motivo = resultado == null ? "RESULTADO_NULO" : String.valueOf(resultado.getMotivoInterrupcao());
        final int paginasProcessadas = resultado == null ? 0 : resultado.getPaginasProcessadas();
        final int registrosExtraidos = resultado == null ? 0 : resultado.getRegistrosExtraidos();

        logger.error(
            "sincronizar_usuarios: snapshot cancelado por resultado incompleto | motivo={} | paginas_processadas={} | registros_extraidos={}",
            motivo,
            paginasProcessadas,
            registrosExtraidos
        );
        throw new IllegalStateException(
            "Sincronizacao completa de usuarios cancelada: resultado incompleto (" + motivo + ")."
        );
    }

    private List<UsuarioSistemaEntity> deduplicarPorUserId(final List<UsuarioSistemaEntity> entities) {
        return entities.stream()
            .collect(Collectors.toMap(
                UsuarioSistemaEntity::getUserId,
                entity -> entity,
                (anterior, atual) -> atual,
                LinkedHashMap::new
            ))
            .values()
            .stream()
            .collect(Collectors.toList());
    }
}
