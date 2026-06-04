package br.com.extrator.features.usuarios.aplicacao;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.extrator.aplicacao.contexto.AplicacaoContexto;
import br.com.extrator.aplicacao.extracao.ExtracaoPorIntervaloRequest;
import br.com.extrator.aplicacao.portas.ExecutionAuditPort;
import br.com.extrator.dominio.graphql.usuarios.IndividualNodeDTO;
import br.com.extrator.integracao.ClienteApiGraphQL;
import br.com.extrator.integracao.ResultadoExtracao;
import br.com.extrator.integracao.comum.EntityExtractor;
import br.com.extrator.integracao.mapeamento.graphql.usuarios.UsuarioSistemaMapper;
import br.com.extrator.persistencia.repositorio.UsuarioSistemaRepository;
import br.com.extrator.persistencia.entidade.UsuarioSistemaEntity;
import br.com.extrator.plataforma.auditoria.aplicacao.ExecutionWindowPlanner;
import br.com.extrator.plataforma.auditoria.dominio.ExecutionWindowPlan;
import br.com.extrator.suporte.observabilidade.ExecutionContext;
import br.com.extrator.suporte.tempo.RelogioSistema;
import br.com.extrator.suporte.validacao.ConstantesEntidades;

public final class SincronizacaoUsuariosSistemaService {
    private static final Logger logger = LoggerFactory.getLogger(SincronizacaoUsuariosSistemaService.class);

    private final ClienteApiGraphQL apiClient;
    private final UsuarioSistemaMapper mapper;
    private final UsuarioSistemaRepository repository;
    private final ExecutionAuditPort executionAuditPort;

    public SincronizacaoUsuariosSistemaService() {
        this(
            new ClienteApiGraphQL(),
            new UsuarioSistemaMapper(),
            new UsuarioSistemaRepository(),
            AplicacaoContexto.executionAuditPort()
        );
    }

    SincronizacaoUsuariosSistemaService(final ClienteApiGraphQL apiClient,
                                        final UsuarioSistemaMapper mapper,
                                        final UsuarioSistemaRepository repository,
                                        final ExecutionAuditPort executionAuditPort) {
        this.apiClient = Objects.requireNonNull(apiClient, "apiClient");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
        this.repository = Objects.requireNonNull(repository, "repository");
        this.executionAuditPort = Objects.requireNonNull(executionAuditPort, "executionAuditPort");
    }

    public EntityExtractor.SaveMetrics sincronizar() throws SQLException {
        apiClient.setExecutionUuid(ExecutionContext.currentExecutionId());

        final ExecutionWindowPlan plano = new ExecutionWindowPlanner(executionAuditPort).planejarEntidade(
            ConstantesEntidades.USUARIOS_SISTEMA,
            RelogioSistema.hoje(),
            ExtracaoPorIntervaloRequest.ModoExecucao.MICRO_BATCH
        );
        final ResultadoExtracao<IndividualNodeDTO> resultado =
            apiClient.buscarUsuariosSistema(plano.confirmacaoInicio(), plano.confirmacaoFim());
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
            "sincronizar_usuarios: upsert incremental autorizado | janela={}..{} | api_bruto={} | user_id_unicos={} | paginas_processadas={}",
            plano.confirmacaoInicio(),
            plano.confirmacaoFim(),
            entities.size(),
            unicos.size(),
            resultado.getPaginasProcessadas()
        );
        final int registrosSalvos = repository.salvar(unicos);
        if (executionAuditPort.isDisponivel()) {
            executionAuditPort.atualizarWatermarkConfirmado(
                ConstantesEntidades.USUARIOS_SISTEMA,
                plano.confirmacaoFim()
            );
        }
        return new EntityExtractor.SaveMetrics(
            registrosSalvos,
            unicos.size(),
            0,
            repository.getUltimoResumoSalvamento().getRegistrosPersistidos(),
            repository.getUltimoResumoSalvamento().getRegistrosNoOpIdempotente()
        );
    }

    private void validarResultadoCompleto(final ResultadoExtracao<IndividualNodeDTO> resultado) {
        if (resultado != null && resultado.isCompleto()) {
            return;
        }

        final String motivo = resultado == null ? "RESULTADO_NULO" : String.valueOf(resultado.getMotivoInterrupcao());
        final int paginasProcessadas = resultado == null ? 0 : resultado.getPaginasProcessadas();
        final int registrosExtraidos = resultado == null ? 0 : resultado.getRegistrosExtraidos();

        logger.error(
            "sincronizar_usuarios: upsert incremental cancelado por resultado incompleto | motivo={} | paginas_processadas={} | registros_extraidos={}",
            motivo,
            paginasProcessadas,
            registrosExtraidos
        );
        throw new IllegalStateException(
            "Sincronizacao incremental de usuarios cancelada: resultado incompleto (" + motivo + ")."
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
