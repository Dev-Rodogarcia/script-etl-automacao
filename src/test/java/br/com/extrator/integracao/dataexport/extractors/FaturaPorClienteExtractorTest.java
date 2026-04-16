package br.com.extrator.integracao.dataexport.extractors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.extrator.dominio.dataexport.faturaporcliente.FaturaPorClienteDTO;
import br.com.extrator.integracao.ClienteApiDataExport;
import br.com.extrator.integracao.ResultadoExtracao;
import br.com.extrator.integracao.mapeamento.dataexport.faturaporcliente.FaturaPorClienteMapper;
import br.com.extrator.persistencia.entidade.FaturaPorClienteEntity;
import br.com.extrator.persistencia.repositorio.FaturaPorClienteRepository;
import br.com.extrator.suporte.console.LoggerConsole;

class FaturaPorClienteExtractorTest {

    @Test
    void deveUsarIntervaloInformadoNaExtracao() {
        final FakeClienteApiDataExport apiClient = new FakeClienteApiDataExport();
        final FaturaPorClienteExtractor extractor = new FaturaPorClienteExtractor(
            apiClient,
            new FakeFaturaPorClienteRepository(),
            new FaturaPorClienteMapper(),
            LoggerConsole.getLogger(FaturaPorClienteExtractorTest.class)
        );

        extractor.extract(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 3));

        assertEquals(LocalDate.of(2026, 4, 1), apiClient.dataInicioCapturada);
        assertEquals(LocalDate.of(2026, 4, 3), apiClient.dataFimCapturada);
    }

    @Test
    void deveManterUltimoDtoQuandoUniqueIdCanonicoSeRepete() throws SQLException {
        final FakeFaturaPorClienteRepository repository = new FakeFaturaPorClienteRepository();
        final FaturaPorClienteExtractor extractor = new FaturaPorClienteExtractor(
            new FakeClienteApiDataExport(),
            repository,
            new FaturaPorClienteMapper(),
            LoggerConsole.getLogger(FaturaPorClienteExtractorTest.class)
        );

        final FaturaPorClienteDTO inicial = faturaBase();
        final FaturaPorClienteDTO enriquecida = faturaBase();
        enriquecida.setFaturaDocument("164781");
        enriquecida.setFaturaIssueDate("2026-04-14");
        enriquecida.setFaturaOriginalDueDate("2026-04-14");
        enriquecida.setFaturaDueDate("2026-04-14");
        enriquecida.setFaturaValue("143.08");

        final var resultado = extractor.saveWithDeduplication(List.of(inicial, enriquecida));

        assertEquals(1, repository.salvos.size());
        assertEquals(1, resultado.getTotalUnicos());
        assertNotNull(repository.salvos.get(0).getNumeroFatura());
        assertEquals("164781", repository.salvos.get(0).getNumeroFatura());
        assertEquals(LocalDate.of(2026, 4, 14), repository.salvos.get(0).getDataEmissaoFatura());
        assertEquals(LocalDate.of(2026, 4, 14), repository.salvos.get(0).getDataVencimentoFatura());
    }

    private static FaturaPorClienteDTO faturaBase() {
        final FaturaPorClienteDTO dto = new FaturaPorClienteDTO();
        dto.setCteNumber(55139L);
        dto.setCteKey("35123456789012345678901234567890123456789012");
        dto.setCteIssuedAt("2026-04-13T19:29:17-03:00");
        dto.setValorFrete("143.08");
        dto.setPagadorDocumento("12345678000199");
        dto.setRemetenteDocumento("98765432000188");
        dto.setDestinatarioDocumento("11223344000155");
        return dto;
    }

    private static final class FakeClienteApiDataExport extends ClienteApiDataExport {
        private LocalDate dataInicioCapturada;
        private LocalDate dataFimCapturada;

        private FakeClienteApiDataExport() {
            super(true);
        }

        @Override
        public ResultadoExtracao<FaturaPorClienteDTO> buscarFaturasPorCliente(final LocalDate dataInicio,
                                                                              final LocalDate dataFim) {
            this.dataInicioCapturada = dataInicio;
            this.dataFimCapturada = dataFim;
            return ResultadoExtracao.completo(List.of(), 0, 0);
        }
    }

    private static final class FakeFaturaPorClienteRepository extends FaturaPorClienteRepository {
        private List<FaturaPorClienteEntity> salvos = List.of();

        @Override
        public int salvar(final List<FaturaPorClienteEntity> entidades) {
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
