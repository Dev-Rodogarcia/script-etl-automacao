package br.com.extrator.integracao.graphql.extractors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import br.com.extrator.dominio.graphql.fretes.FreteNodeDTO;
import br.com.extrator.integracao.comum.EntityExtractor;
import br.com.extrator.integracao.mapeamento.graphql.fretes.FreteMapper;
import br.com.extrator.persistencia.entidade.FreteEntity;
import br.com.extrator.persistencia.repositorio.FreteRepository;

class FreteExtractorTest {

    @AfterEach
    void limparFlagDePrune() {
        System.clearProperty("ETL_FRETES_PRUNE_AUSENTES");
    }

    @Test
    void deveRemoverAusentesQuandoPruneEstaAtivoEExtracaoFoiCompleta() throws SQLException {
        System.setProperty("ETL_FRETES_PRUNE_AUSENTES", "true");
        final FakeFreteRepository repository = new FakeFreteRepository();
        final FreteExtractor extractor = new FreteExtractor(null, repository, new FreteMapper());
        extractor.registrarUltimaExtracaoParaTeste(LocalDate.of(2026, 3, 25), LocalDate.of(2026, 3, 25), true);

        final EntityExtractor.SaveMetrics metrics = extractor.saveWithMetrics(List.of(criarFrete(47882015L)));

        assertEquals(1, metrics.getRegistrosSalvos());
        assertTrue(repository.remocaoExecutada);
        assertEquals(LocalDate.of(2026, 3, 25), repository.dataInicioRemocao);
        assertEquals(LocalDate.of(2026, 3, 25), repository.dataFimRemocao);
        assertEquals(List.of(47882015L), repository.idsPresentesRemocao);
    }

    @Test
    void naoDeveRemoverAusentesQuandoExtracaoNaoFoiCompleta() throws SQLException {
        System.setProperty("ETL_FRETES_PRUNE_AUSENTES", "true");
        final FakeFreteRepository repository = new FakeFreteRepository();
        final FreteExtractor extractor = new FreteExtractor(null, repository, new FreteMapper());
        extractor.registrarUltimaExtracaoParaTeste(LocalDate.of(2026, 3, 25), LocalDate.of(2026, 3, 25), false);

        extractor.saveWithMetrics(List.of(criarFrete(47882015L)));

        assertFalse(repository.remocaoExecutada);
    }

    private static FreteNodeDTO criarFrete(final long id) {
        final FreteNodeDTO dto = new FreteNodeDTO();
        dto.setId(id);
        dto.setServiceAt("2026-03-25T20:11:00-03:00");
        dto.setCreatedAt("2026-03-25T20:24:02-03:00");
        dto.setStatus("pending");
        return dto;
    }

    private static final class FakeFreteRepository extends FreteRepository {
        private boolean remocaoExecutada;
        private LocalDate dataInicioRemocao;
        private LocalDate dataFimRemocao;
        private List<Long> idsPresentesRemocao = List.of();

        @Override
        public int salvar(final List<FreteEntity> entidades) {
            return entidades == null ? 0 : entidades.size();
        }

        @Override
        public SaveSummary getUltimoResumoSalvamento() {
            return new SaveSummary(1, 1, 0, 1, 0);
        }

        @Override
        public int removerAusentesNoPeriodo(final LocalDate dataInicio,
                                            final LocalDate dataFim,
                                            final Collection<Long> idsPresentes) {
            this.remocaoExecutada = true;
            this.dataInicioRemocao = dataInicio;
            this.dataFimRemocao = dataFim;
            this.idsPresentesRemocao = new ArrayList<>(idsPresentes);
            return 1;
        }
    }
}
