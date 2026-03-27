package br.com.extrator.features.usuarios.aplicacao;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import br.com.extrator.integracao.comum.EntityExtractor;
import br.com.extrator.persistencia.entidade.UsuarioSistemaEntity;
import br.com.extrator.suporte.observabilidade.ExecutionContext;

public final class UsuariosSistemaSnapshotService {
    private final UsuariosEstadoPort usuariosEstadoPort;

    public UsuariosSistemaSnapshotService(final UsuariosEstadoPort usuariosEstadoPort) {
        this.usuariosEstadoPort = usuariosEstadoPort;
    }

    public EntityExtractor.SaveMetrics persistirSnapshot(final List<UsuarioSistemaEntity> usuariosAtivos) throws SQLException {
        final LocalDateTime observadoEm = LocalDateTime.now();
        final UsuariosEstadoPort.SnapshotMetrics snapshotMetrics = usuariosEstadoPort.aplicarSnapshot(
            usuariosAtivos,
            ExecutionContext.currentExecutionId(),
            observadoEm
        );
        return new EntityExtractor.SaveMetrics(
            snapshotMetrics.totalOperacoes(),
            usuariosAtivos != null ? usuariosAtivos.size() : 0,
            0,
            snapshotMetrics.registrosPersistidos(),
            snapshotMetrics.noOpIdempotente()
        );
    }
}
