package br.com.extrator.features.usuarios.aplicacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import br.com.extrator.aplicacao.portas.ExecutionAuditPort;
import br.com.extrator.dominio.graphql.usuarios.IndividualNodeDTO;
import br.com.extrator.integracao.ClienteApiGraphQL;
import br.com.extrator.integracao.ResultadoExtracao;
import br.com.extrator.integracao.comum.EntityExtractor;
import br.com.extrator.integracao.mapeamento.graphql.usuarios.UsuarioSistemaMapper;
import br.com.extrator.persistencia.entidade.UsuarioSistemaEntity;
import br.com.extrator.persistencia.repositorio.AbstractRepository;
import br.com.extrator.persistencia.repositorio.UsuarioSistemaRepository;
import br.com.extrator.plataforma.auditoria.dominio.ExecutionAuditRecord;
import br.com.extrator.suporte.observabilidade.ExecutionContext;
import br.com.extrator.suporte.validacao.ConstantesEntidades;

class SincronizacaoUsuariosSistemaServiceTest {

    @AfterEach
    void limparContexto() {
        ExecutionContext.clear();
    }

    @Test
    void deveAplicarUpsertIncrementalSomenteQuandoResultadoCompleto() throws SQLException {
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
        final RecordingUsuarioSistemaRepository repository = new RecordingUsuarioSistemaRepository();
        final LocalDateTime watermark = LocalDateTime.of(2026, 6, 3, 10, 45);
        final RecordingExecutionAuditPort auditPort = new RecordingExecutionAuditPort(watermark);
        final SincronizacaoUsuariosSistemaService service = new SincronizacaoUsuariosSistemaService(
            apiClient,
            new UsuarioSistemaMapper(),
            repository,
            auditPort
        );

        final String executionId = ExecutionContext.initialize("--sincronizar-usuarios");
        final EntityExtractor.SaveMetrics metrics = service.sincronizar();

        assertFalse(apiClient.fullLoadChamado);
        assertTrue(apiClient.incrementalChamado);
        assertEquals(executionId, apiClient.executionUuidRecebido);
        assertEquals(watermark, apiClient.inicioRecebido);
        assertEquals(1, repository.salvarChamadas);
        assertEquals(List.of(10L, 11L), repository.userIdsRecebidos);
        assertEquals(List.of("Ana Silva", "Bruno"), repository.nomesRecebidos);
        assertEquals(ConstantesEntidades.USUARIOS_SISTEMA, auditPort.entidadeAtualizada);
        assertTrue(auditPort.watermarkAtualizado.isAfter(watermark));
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
        final RecordingUsuarioSistemaRepository repository = new RecordingUsuarioSistemaRepository();
        final RecordingExecutionAuditPort auditPort = new RecordingExecutionAuditPort();
        final SincronizacaoUsuariosSistemaService service = new SincronizacaoUsuariosSistemaService(
            apiClient,
            new UsuarioSistemaMapper(),
            repository,
            auditPort
        );

        final IllegalStateException error = assertThrows(IllegalStateException.class, service::sincronizar);

        assertTrue(apiClient.incrementalChamado);
        assertFalse(apiClient.fullLoadChamado);
        assertEquals(0, repository.salvarChamadas);
        assertEquals(null, auditPort.watermarkAtualizado);
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
        private LocalDateTime inicioRecebido;

        private RecordingClienteApiGraphQL(final ResultadoExtracao<IndividualNodeDTO> resultado) {
            this.resultado = resultado;
        }

        @Override
        public void setExecutionUuid(final String uuid) {
            this.executionUuidRecebido = uuid;
        }

        @Override
        public ResultadoExtracao<IndividualNodeDTO> buscarUsuariosSistema(final LocalDateTime atualizadoApos,
                                                                          final LocalDateTime atualizadoAte) {
            incrementalChamado = true;
            inicioRecebido = atualizadoApos;
            return resultado;
        }

        @Override
        public ResultadoExtracao<IndividualNodeDTO> buscarUsuariosSistema() {
            fullLoadChamado = true;
            return resultado;
        }
    }

    private static final class RecordingUsuarioSistemaRepository extends UsuarioSistemaRepository {
        private int salvarChamadas;
        private List<Long> userIdsRecebidos = List.of();
        private List<String> nomesRecebidos = List.of();

        @Override
        public int salvar(final List<UsuarioSistemaEntity> entidades) {
            salvarChamadas++;
            this.userIdsRecebidos = entidades.stream()
                .map(UsuarioSistemaEntity::getUserId)
                .toList();
            this.nomesRecebidos = entidades.stream()
                .map(UsuarioSistemaEntity::getNome)
                .toList();
            return entidades.size();
        }

        @Override
        public AbstractRepository.SaveSummary getUltimoResumoSalvamento() {
            return new AbstractRepository.SaveSummary(
                userIdsRecebidos.size(),
                userIdsRecebidos.size(),
                0,
                0,
                0
            );
        }
    }

    private static final class RecordingExecutionAuditPort implements ExecutionAuditPort {
        private final LocalDateTime watermarkInicial;
        private String entidadeAtualizada;
        private LocalDateTime watermarkAtualizado;

        private RecordingExecutionAuditPort() {
            this(null);
        }

        private RecordingExecutionAuditPort(final LocalDateTime watermarkInicial) {
            this.watermarkInicial = watermarkInicial;
        }

        @Override
        public void registrarResultado(final ExecutionAuditRecord record) {
            // no-op
        }

        @Override
        public Optional<ExecutionAuditRecord> buscarResultado(final String executionUuid, final String entidade) {
            return Optional.empty();
        }

        @Override
        public List<ExecutionAuditRecord> listarResultados(final String executionUuid) {
            return List.of();
        }

        @Override
        public Optional<LocalDateTime> buscarWatermarkConfirmado(final String entidade) {
            return Optional.ofNullable(watermarkInicial);
        }

        @Override
        public void atualizarWatermarkConfirmado(final String entidade, final LocalDateTime watermarkConfirmado) {
            this.entidadeAtualizada = entidade;
            this.watermarkAtualizado = watermarkConfirmado;
        }
    }
}
