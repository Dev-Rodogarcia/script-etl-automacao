package br.com.extrator.features.usuarios.aplicacao;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import br.com.extrator.persistencia.entidade.UsuarioSistemaEntity;

public interface UsuariosEstadoPort {

    SnapshotMetrics aplicarSnapshot(List<UsuarioSistemaEntity> usuariosAtivos,
                                    String executionUuid,
                                    LocalDateTime observadoEm) throws SQLException;

    record SnapshotMetrics(
        int totalOperacoes,
        int registrosPersistidos,
        int noOpIdempotente,
        int registrosHistorico
    ) {
    }
}
