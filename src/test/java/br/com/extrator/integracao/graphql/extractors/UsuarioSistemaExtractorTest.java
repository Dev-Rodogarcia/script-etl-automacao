package br.com.extrator.integracao.graphql.extractors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.extrator.dominio.graphql.usuarios.IndividualNodeDTO;
import br.com.extrator.integracao.ClienteApiGraphQL;
import br.com.extrator.integracao.ResultadoExtracao;
import br.com.extrator.integracao.comum.EntityExtractor;
import br.com.extrator.integracao.mapeamento.graphql.usuarios.UsuarioSistemaMapper;
import br.com.extrator.persistencia.entidade.UsuarioSistemaEntity;
import br.com.extrator.persistencia.repositorio.AbstractRepository;
import br.com.extrator.persistencia.repositorio.UsuarioSistemaRepository;

class UsuarioSistemaExtractorTest {

    @Test
    void deveUsarCargaIncrementalQuandoDimUsuariosJaPossuiDados() {
        final RecordingClienteApiGraphQL apiClient = new RecordingClienteApiGraphQL();
        final FakeUsuarioSistemaRepository repository = new FakeUsuarioSistemaRepository(true);
        final UsuarioSistemaExtractor extractor = new UsuarioSistemaExtractor(
            apiClient,
            repository,
            new UsuarioSistemaMapper()
        );

        extractor.extract(LocalDate.of(2026, 3, 24), LocalDate.of(2026, 3, 25));

        assertTrue(apiClient.incrementalChamado);
        assertFalse(apiClient.fullLoadChamado);
    }

    @Test
    void deveManterExtracaoIncrementalMesmoQuandoDimUsuariosEstiverVazia() {
        final RecordingClienteApiGraphQL apiClient = new RecordingClienteApiGraphQL();
        final FakeUsuarioSistemaRepository repository = new FakeUsuarioSistemaRepository(false);
        final UsuarioSistemaExtractor extractor = new UsuarioSistemaExtractor(
            apiClient,
            repository,
            new UsuarioSistemaMapper()
        );

        extractor.extract(LocalDate.of(2026, 3, 24), LocalDate.of(2026, 3, 25));

        assertTrue(apiClient.incrementalChamado);
        assertFalse(apiClient.fullLoadChamado);
    }

    @Test
    void devePersistirIncrementalSemAplicarSnapshotQuandoDimUsuariosJaExiste() throws SQLException {
        final FakeUsuarioSistemaRepository repository = new FakeUsuarioSistemaRepository(true);
        final UsuarioSistemaExtractor extractor = new UsuarioSistemaExtractor(
            new RecordingClienteApiGraphQL(),
            repository,
            new UsuarioSistemaMapper()
        );

        final EntityExtractor.SaveMetrics metrics = extractor.saveWithMetrics(List.of(usuarioDto(10L, "Ana")));

        assertEquals(1, repository.salvarChamadas);
        assertEquals(1, metrics.getRegistrosSalvos());
        assertEquals(1, metrics.getRegistrosPersistidos());
    }

    @Test
    void devePersistirIncrementalMesmoQuandoDimUsuariosEstiverVazia() throws SQLException {
        final FakeUsuarioSistemaRepository repository = new FakeUsuarioSistemaRepository(false);
        final UsuarioSistemaExtractor extractor = new UsuarioSistemaExtractor(
            new RecordingClienteApiGraphQL(),
            repository,
            new UsuarioSistemaMapper()
        );

        final EntityExtractor.SaveMetrics metrics = extractor.saveWithMetrics(List.of(usuarioDto(11L, "Bruno")));

        assertEquals(1, repository.salvarChamadas);
        assertEquals(1, metrics.getRegistrosSalvos());
        assertEquals(1, metrics.getRegistrosPersistidos());
    }

    private static IndividualNodeDTO usuarioDto(final Long id, final String nome) {
        final IndividualNodeDTO dto = new IndividualNodeDTO();
        dto.setId(id);
        dto.setName(nome);
        return dto;
    }

    private static final class RecordingClienteApiGraphQL extends ClienteApiGraphQL {
        private boolean incrementalChamado;
        private boolean fullLoadChamado;

        @Override
        public ResultadoExtracao<IndividualNodeDTO> buscarUsuariosSistema(final LocalDate dataInicio, final LocalDate dataFim) {
            incrementalChamado = true;
            return ResultadoExtracao.completo(List.of(), 0, 0);
        }

        @Override
        public ResultadoExtracao<IndividualNodeDTO> buscarUsuariosSistema() {
            fullLoadChamado = true;
            return ResultadoExtracao.completo(List.of(), 0, 0);
        }
    }

    private static final class FakeUsuarioSistemaRepository extends UsuarioSistemaRepository {
        private final boolean temDados;
        private int salvarChamadas;

        private FakeUsuarioSistemaRepository(final boolean temDados) {
            this.temDados = temDados;
        }

        @Override
        public boolean temDados() {
            return temDados;
        }

        @Override
        public int salvar(final List<UsuarioSistemaEntity> entidades) {
            salvarChamadas++;
            return entidades == null ? 0 : entidades.size();
        }

        @Override
        public AbstractRepository.SaveSummary getUltimoResumoSalvamento() {
            return new AbstractRepository.SaveSummary(1, 1, 0, 0, 0);
        }
    }
}
