package br.com.extrator.features.usuarios.aplicacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import br.com.extrator.integracao.comum.EntityExtractor;
import br.com.extrator.persistencia.entidade.UsuarioSistemaEntity;
import br.com.extrator.suporte.observabilidade.ExecutionContext;

class UsuariosSistemaSnapshotServiceTest {

    @AfterEach
    void limparContexto() {
        ExecutionContext.clear();
    }

    @Test
    void devePropagarExecutionIdEMapearMetricasDoSnapshot() throws SQLException {
        final StubUsuariosEstadoPort port = new StubUsuariosEstadoPort();
        final UsuariosSistemaSnapshotService service = new UsuariosSistemaSnapshotService(port);
        final UsuarioSistemaEntity usuario = new UsuarioSistemaEntity();
        usuario.setUserId(10L);
        usuario.setNome("Maria");

        final String executionId = ExecutionContext.initialize("--teste-snapshot");
        final EntityExtractor.SaveMetrics metrics = service.persistirSnapshot(List.of(usuario));

        assertEquals(executionId, port.executionUuidRecebido);
        assertEquals(1, port.usuariosRecebidos.size());
        assertNotNull(port.observadoEmRecebido);
        assertEquals(3, metrics.getRegistrosSalvos());
        assertEquals(1, metrics.getTotalUnicos());
        assertEquals(2, metrics.getRegistrosPersistidos());
        assertEquals(1, metrics.getRegistrosNoOpIdempotente());
    }

    private static final class StubUsuariosEstadoPort implements UsuariosEstadoPort {
        private List<UsuarioSistemaEntity> usuariosRecebidos = List.of();
        private String executionUuidRecebido;
        private LocalDateTime observadoEmRecebido;

        @Override
        public SnapshotMetrics aplicarSnapshot(final List<UsuarioSistemaEntity> usuariosAtivos,
                                               final String executionUuid,
                                               final LocalDateTime observadoEm) {
            this.usuariosRecebidos = usuariosAtivos;
            this.executionUuidRecebido = executionUuid;
            this.observadoEmRecebido = observadoEm;
            return new SnapshotMetrics(3, 2, 1, 1);
        }
    }
}
