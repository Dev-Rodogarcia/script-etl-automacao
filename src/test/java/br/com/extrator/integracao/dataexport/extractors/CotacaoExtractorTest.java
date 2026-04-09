package br.com.extrator.integracao.dataexport.extractors;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.extrator.dominio.dataexport.cotacao.CotacaoDTO;
import br.com.extrator.integracao.ClienteApiDataExport;
import br.com.extrator.integracao.ResultadoExtracao;
import br.com.extrator.integracao.mapeamento.dataexport.cotacao.CotacaoMapper;
import br.com.extrator.persistencia.entidade.CotacaoEntity;
import br.com.extrator.persistencia.repositorio.CotacaoRepository;
import br.com.extrator.suporte.console.LoggerConsole;

class CotacaoExtractorTest {

    @Test
    void deveUsarIntervaloInformadoNaExtracao() {
        final FakeClienteApiDataExport apiClient = new FakeClienteApiDataExport();
        final CotacaoExtractor extractor = new CotacaoExtractor(
            apiClient,
            new FakeCotacaoRepository(),
            new CotacaoMapper(),
            LoggerConsole.getLogger(CotacaoExtractorTest.class)
        );

        extractor.extract(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 3));

        assertEquals(LocalDate.of(2026, 4, 1), apiClient.dataInicioCapturada);
        assertEquals(LocalDate.of(2026, 4, 3), apiClient.dataFimCapturada);
    }

    @Test
    void deveDeduplicarCotacoesMantendoRequestedAtMaisRecente() throws SQLException {
        final FakeCotacaoRepository repository = new FakeCotacaoRepository();
        final CotacaoExtractor extractor = new CotacaoExtractor(
            new FakeClienteApiDataExport(),
            repository,
            new CotacaoMapper(),
            LoggerConsole.getLogger(CotacaoExtractorTest.class)
        );

        final CotacaoDTO antiga = cotacao(100L, "2026-04-01T08:00:00-03:00");
        final CotacaoDTO recente = cotacao(100L, "2026-04-01T09:00:00-03:00");

        final var resultado = extractor.saveWithDeduplication(List.of(antiga, recente));

        assertEquals(1, repository.salvos.size());
        assertEquals(1, resultado.getTotalUnicos());
        assertEquals(OffsetDateTime.parse("2026-04-01T09:00:00-03:00"), repository.salvos.get(0).getRequestedAt());
    }

    private static CotacaoDTO cotacao(final Long sequenceCode, final String requestedAt) {
        final CotacaoDTO dto = new CotacaoDTO();
        dto.setSequenceCode(sequenceCode);
        dto.setRequestedAt(requestedAt);
        return dto;
    }

    private static final class FakeClienteApiDataExport extends ClienteApiDataExport {
        private LocalDate dataInicioCapturada;
        private LocalDate dataFimCapturada;

        private FakeClienteApiDataExport() {
            super(true);
        }

        @Override
        public ResultadoExtracao<CotacaoDTO> buscarCotacoes(final LocalDate dataInicio, final LocalDate dataFim) {
            this.dataInicioCapturada = dataInicio;
            this.dataFimCapturada = dataFim;
            return ResultadoExtracao.completo(List.of(), 0, 0);
        }
    }

    private static final class FakeCotacaoRepository extends CotacaoRepository {
        private List<CotacaoEntity> salvos = List.of();

        @Override
        public int salvar(final List<CotacaoEntity> entidades) {
            this.salvos = List.copyOf(entidades);
            return entidades.size();
        }

        @Override
        public br.com.extrator.persistencia.repositorio.AbstractRepository.SaveSummary getUltimoResumoSalvamento() {
            return new br.com.extrator.persistencia.repositorio.AbstractRepository.SaveSummary(
                salvos.size(),
                salvos.size(),
                0,
                0,
                0
            );
        }
    }
}
