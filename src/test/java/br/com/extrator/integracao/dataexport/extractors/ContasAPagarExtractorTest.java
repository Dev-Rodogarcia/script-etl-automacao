package br.com.extrator.integracao.dataexport.extractors;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.extrator.dominio.dataexport.contasapagar.ContasAPagarDTO;
import br.com.extrator.integracao.ClienteApiDataExport;
import br.com.extrator.integracao.ResultadoExtracao;
import br.com.extrator.integracao.mapeamento.dataexport.contasapagar.ContasAPagarMapper;
import br.com.extrator.persistencia.entidade.ContasAPagarDataExportEntity;
import br.com.extrator.persistencia.repositorio.ContasAPagarRepository;
import br.com.extrator.suporte.console.LoggerConsole;

class ContasAPagarExtractorTest {

    @Test
    void deveUsarIntervaloInformadoNaExtracao() {
        final FakeClienteApiDataExport apiClient = new FakeClienteApiDataExport();
        final ContasAPagarExtractor extractor = new ContasAPagarExtractor(
            apiClient,
            new FakeContasAPagarRepository(),
            new ContasAPagarMapper(),
            LoggerConsole.getLogger(ContasAPagarExtractorTest.class)
        );

        extractor.extract(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 3));

        assertEquals(LocalDate.of(2026, 4, 1), apiClient.dataInicioCapturada);
        assertEquals(LocalDate.of(2026, 4, 3), apiClient.dataFimCapturada);
    }

    @Test
    void deveDeduplicarContasAPagarAntesDeSalvar() throws SQLException {
        final FakeContasAPagarRepository repository = new FakeContasAPagarRepository();
        final ContasAPagarExtractor extractor = new ContasAPagarExtractor(
            new FakeClienteApiDataExport(),
            repository,
            new ContasAPagarMapper(),
            LoggerConsole.getLogger(ContasAPagarExtractorTest.class)
        );

        final ContasAPagarDTO antiga = conta("100", "2026-04-01T08:00:00-03:00");
        final ContasAPagarDTO recente = conta("100", "2026-04-01T09:00:00-03:00");

        final var resultado = extractor.saveWithDeduplication(List.of(antiga, recente));

        assertEquals(1, repository.salvos.size());
        assertEquals(1, resultado.getTotalUnicos());
        assertEquals(Long.valueOf(100L), repository.salvos.get(0).getSequenceCode());
        assertEquals("2026-04-01T09:00-03:00", repository.salvos.get(0).getDataCriacao().withSecond(0).withNano(0).toString());
    }

    private static ContasAPagarDTO conta(final String sequenceCode, final String createdAt) {
        final ContasAPagarDTO dto = new ContasAPagarDTO();
        dto.setSequenceCode(sequenceCode);
        dto.setIssueDate("2026-04-01");
        dto.setCreatedAt(createdAt);
        return dto;
    }

    private static final class FakeClienteApiDataExport extends ClienteApiDataExport {
        private LocalDate dataInicioCapturada;
        private LocalDate dataFimCapturada;

        private FakeClienteApiDataExport() {
            super(true);
        }

        @Override
        public ResultadoExtracao<ContasAPagarDTO> buscarContasAPagar(final LocalDate dataInicio, final LocalDate dataFim) {
            this.dataInicioCapturada = dataInicio;
            this.dataFimCapturada = dataFim;
            return ResultadoExtracao.completo(List.of(), 0, 0);
        }
    }

    private static final class FakeContasAPagarRepository extends ContasAPagarRepository {
        private List<ContasAPagarDataExportEntity> salvos = List.of();

        @Override
        public int salvar(final List<ContasAPagarDataExportEntity> entidades) {
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
