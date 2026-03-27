package br.com.extrator.features.usuarios.aplicacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import br.com.extrator.dominio.graphql.usuarios.IndividualNodeDTO;
import br.com.extrator.integracao.ClienteApiGraphQL;
import br.com.extrator.integracao.ResultadoExtracao;
import br.com.extrator.integracao.comum.EntityExtractor;
import br.com.extrator.integracao.mapeamento.graphql.usuarios.UsuarioSistemaMapper;
import br.com.extrator.persistencia.entidade.UsuarioSistemaEntity;
import br.com.extrator.suporte.observabilidade.ExecutionContext;

class SincronizacaoUsuariosSistemaServiceTest {

    @AfterEach
    void limparContexto() {
        ExecutionContext.clear();
    }

    @Test
    void deveAplicarSnapshotSomenteQuandoResultadoCompleto() throws SQLException {
        final RecordingClienteApiGraphQL apiClient = new RecordingClienteApiGraphQL(
            ResultadoExtracao.completo(
                List.of(
                    usuarioDto(10L, "Ana"),
                    usuarioDto(10L, "Ana Silva"),
                    usuarioDto(11L, "Bruno")
                ),
                2,
                3
            )
        );
        final RecordingUsuariosEstadoPort snapshotPort = new RecordingUsuariosEstadoPort();
        final SincronizacaoUsuariosSistemaService service = new SincronizacaoUsuariosSistemaService(
            apiClient,
            new UsuarioSistemaMapper(),
            new UsuariosSistemaSnapshotService(snapshotPort)
        );

        final String executionId = ExecutionContext.initialize("--sincronizar-usuarios");
        final EntityExtractor.SaveMetrics metrics = service.sincronizar();

        assertTrue(apiClient.fullLoadChamado);
        assertFalse(apiClient.incrementalChamado);
        assertEquals(executionId, apiClient.executionUuidRecebido);
        assertEquals(1, snapshotPort.snapshotChamadas);
        assertEquals(List.of(10L, 11L), snapshotPort.userIdsRecebidos);
        assertEquals(List.of("Ana Silva", "Bruno"), snapshotPort.nomesRecebidos);
        assertEquals(executionId, snapshotPort.executionUuidRecebido);
        assertEquals(2, metrics.getTotalUnicos());
        assertEquals(2, metrics.getRegistrosSalvos());
        assertEquals(2, metrics.getRegistrosPersistidos());
    }

    @Test
    void deveFalharSemAplicarSnapshotQuandoResultadoVierIncompleto() {
        final RecordingClienteApiGraphQL apiClient = new RecordingClienteApiGraphQL(
            ResultadoExtracao.incompleto(
                List.of(usuarioDto(10L, "Ana")),
                ResultadoExtracao.MotivoInterrupcao.LIMITE_PAGINAS,
                5000,
                1
            )
        );
        final RecordingUsuariosEstadoPort snapshotPort = new RecordingUsuariosEstadoPort();
        final SincronizacaoUsuariosSistemaService service = new SincronizacaoUsuariosSistemaService(
            apiClient,
            new UsuarioSistemaMapper(),
            new UsuariosSistemaSnapshotService(snapshotPort)
        );

        final IllegalStateException error = assertThrows(IllegalStateException.class, service::sincronizar);

        assertTrue(apiClient.fullLoadChamado);
        assertEquals(0, snapshotPort.snapshotChamadas);
        assertTrue(error.getMessage().contains("resultado incompleto"));
        assertTrue(error.getMessage().contains("LIMITE_PAGINAS"));
    }

    private static IndividualNodeDTO usuarioDto(final Long id, final String nome) {
        final IndividualNodeDTO dto = new IndividualNodeDTO();
        dto.setId(id);
        dto.setName(nome);
        return dto;
    }

    private static final class RecordingClienteApiGraphQL extends ClienteApiGraphQL {
        private final ResultadoExtracao<IndividualNodeDTO> resultado;
        private boolean incrementalChamado;
        private boolean fullLoadChamado;
        private String executionUuidRecebido;

        private RecordingClienteApiGraphQL(final ResultadoExtracao<IndividualNodeDTO> resultado) {
            this.resultado = resultado;
        }

        @Override
        public void setExecutionUuid(final String uuid) {
            this.executionUuidRecebido = uuid;
        }

        @Override
        public ResultadoExtracao<IndividualNodeDTO> buscarUsuariosSistema(final LocalDate dataInicio, final LocalDate dataFim) {
            incrementalChamado = true;
            return resultado;
        }

        @Override
        public ResultadoExtracao<IndividualNodeDTO> buscarUsuariosSistema() {
            fullLoadChamado = true;
            return resultado;
        }
    }

    private static final class RecordingUsuariosEstadoPort implements UsuariosEstadoPort {
        private int snapshotChamadas;
        private List<Long> userIdsRecebidos = List.of();
        private List<String> nomesRecebidos = List.of();
        private String executionUuidRecebido;

        @Override
        public SnapshotMetrics aplicarSnapshot(final List<UsuarioSistemaEntity> usuariosAtivos,
                                               final String executionUuid,
                                               final LocalDateTime observadoEm) {
            snapshotChamadas++;
            this.executionUuidRecebido = executionUuid;
            this.userIdsRecebidos = usuariosAtivos.stream()
                .map(UsuarioSistemaEntity::getUserId)
                .toList();
            this.nomesRecebidos = usuariosAtivos.stream()
                .map(UsuarioSistemaEntity::getNome)
                .toList();
            final int total = usuariosAtivos.size();
            return new SnapshotMetrics(total, total, 0, total);
        }
    }
}
