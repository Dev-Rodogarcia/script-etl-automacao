package br.com.extrator.runners;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import br.com.extrator.api.ClienteApiDataExport;
import br.com.extrator.db.entity.CotacaoEntity;
import br.com.extrator.db.entity.LocalizacaoCargaEntity;
import br.com.extrator.db.entity.ManifestoEntity;
import br.com.extrator.db.repository.CotacaoRepository;
import br.com.extrator.db.repository.LocalizacaoCargaRepository;
import br.com.extrator.db.repository.ManifestoRepository;
import br.com.extrator.modelo.dataexport.cotacao.CotacaoDTO;
import br.com.extrator.modelo.dataexport.cotacao.CotacaoMapper;
import br.com.extrator.modelo.dataexport.localizacaocarga.LocalizacaoCargaDTO;
import br.com.extrator.modelo.dataexport.localizacaocarga.LocalizacaoCargaMapper;
import br.com.extrator.modelo.dataexport.manifestos.ManifestoDTO;
import br.com.extrator.modelo.dataexport.manifestos.ManifestoMapper;

/**
 * Runner independente para a API Data Export (Manifestos, Cotações e Localização de Carga).
 */
public final class DataExportRunner {

    private DataExportRunner() {}

    public static void executar(final LocalDate dataInicio) throws Exception {
        System.out.println("🔄 Executando runner DataExport...");

        br.com.extrator.util.CarregadorConfig.validarConexaoBancoDados();

        final ClienteApiDataExport clienteApiDataExport = new ClienteApiDataExport();
        final ManifestoRepository manifestoRepository = new ManifestoRepository();
        final CotacaoRepository cotacaoRepository = new CotacaoRepository();
        final LocalizacaoCargaRepository localizacaoRepository = new LocalizacaoCargaRepository();

        final ManifestoMapper manifestoMapper = new ManifestoMapper();
        final CotacaoMapper cotacaoMapper = new CotacaoMapper();
        final LocalizacaoCargaMapper localizacaoMapper = new LocalizacaoCargaMapper();

        // Manifestos
        System.out.println("\n🧾 Extraindo Manifestos...");
        final List<ManifestoDTO> manifestosDTO = clienteApiDataExport.buscarManifestos(dataInicio);
        System.out.println("✓ Extraídos: " + manifestosDTO.size() + " manifestos");
        if (!manifestosDTO.isEmpty()) {
            final List<ManifestoEntity> manifestosEntities = manifestosDTO.stream()
                .map(manifestoMapper::toEntity)
                .collect(Collectors.toList());
            final int processados = manifestoRepository.salvar(manifestosEntities);
            System.out.println("✓ Salvos: " + processados + "/" + manifestosDTO.size() + " manifestos");
        }

        Thread.sleep(2000);

        // Cotações
        System.out.println("\n💹 Extraindo Cotações...");
        final List<CotacaoDTO> cotacoesDTO = clienteApiDataExport.buscarCotacoes(dataInicio);
        System.out.println("✓ Extraídas: " + cotacoesDTO.size() + " cotações");
        if (!cotacoesDTO.isEmpty()) {
            final List<CotacaoEntity> cotacoesEntities = cotacoesDTO.stream()
                .map(cotacaoMapper::toEntity)
                .collect(Collectors.toList());
            final int processados = cotacaoRepository.salvar(cotacoesEntities);
            System.out.println("✓ Salvas: " + processados + "/" + cotacoesDTO.size() + " cotações");
        }

        Thread.sleep(2000);

        // Localização de Carga
        System.out.println("\n📍 Extraindo Localização de Carga...");
        final List<LocalizacaoCargaDTO> localizacoesDTO = clienteApiDataExport.buscarLocalizacaoCarga(dataInicio);
        System.out.println("✓ Extraídas: " + localizacoesDTO.size() + " localizações");
        if (!localizacoesDTO.isEmpty()) {
            final List<LocalizacaoCargaEntity> localizacoesEntities = localizacoesDTO.stream()
                .map(localizacaoMapper::toEntity)
                .collect(Collectors.toList());
            final int processados = localizacaoRepository.salvar(localizacoesEntities);
            System.out.println("✓ Salvas: " + processados + "/" + localizacoesDTO.size() + " localizações");
        }
    }
}