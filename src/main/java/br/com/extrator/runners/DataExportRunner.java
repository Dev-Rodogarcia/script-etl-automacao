package br.com.extrator.runners;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import br.com.extrator.api.ClienteApiDataExport;
import br.com.extrator.api.ResultadoExtracao;
import br.com.extrator.db.entity.CotacaoEntity;
import br.com.extrator.db.entity.LocalizacaoCargaEntity;
import br.com.extrator.db.entity.ManifestoEntity;
import br.com.extrator.db.entity.LogExtracaoEntity;
import br.com.extrator.db.repository.CotacaoRepository;
import br.com.extrator.db.repository.LocalizacaoCargaRepository;
import br.com.extrator.db.repository.ManifestoRepository;
import br.com.extrator.db.repository.LogExtracaoRepository;
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
        final LogExtracaoRepository logExtracaoRepository = new LogExtracaoRepository();

        final ManifestoMapper manifestoMapper = new ManifestoMapper();
        final CotacaoMapper cotacaoMapper = new CotacaoMapper();
        final LocalizacaoCargaMapper localizacaoMapper = new LocalizacaoCargaMapper();

        // Garante que a tabela log_extracoes existe
        logExtracaoRepository.criarTabelaSeNaoExistir();

        // Manifestos
        System.out.println("\n🧾 Extraindo Manifestos (últimas 24h)...");
        LocalDateTime inicioManifestos = LocalDateTime.now();
        try {
            final ResultadoExtracao<ManifestoDTO> resultadoManifestos = clienteApiDataExport.buscarManifestos();
            final List<ManifestoDTO> manifestosDTO = resultadoManifestos.getDados();
            System.out.println("✓ Extraídos: " + manifestosDTO.size() + " manifestos" + 
                              (resultadoManifestos.isCompleto() ? "" : " (INCOMPLETO: " + resultadoManifestos.getMotivoInterrupcao() + ")"));
            
            int registrosSalvos = 0;
            final int registrosExtraidos = resultadoManifestos.getRegistrosExtraidos();
            if (!manifestosDTO.isEmpty()) {
                final List<ManifestoEntity> manifestosEntities = manifestosDTO.stream()
                    .map(manifestoMapper::toEntity)
                    .collect(Collectors.toList());
                registrosSalvos = manifestoRepository.salvar(manifestosEntities);
                System.out.println("✓ Salvos: " + registrosSalvos + "/" + manifestosDTO.size() + " manifestos");
            }

            // Registrar no log (status enum + quantidade extraída)
            final LogExtracaoEntity.StatusExtracao statusFinal = 
                resultadoManifestos.isCompleto() ? LogExtracaoEntity.StatusExtracao.COMPLETO : LogExtracaoEntity.StatusExtracao.INCOMPLETO_LIMITE;

            final String mensagem = resultadoManifestos.isCompleto() ?
                ("Extração completa – extraídos " + registrosExtraidos + ", salvos " + registrosSalvos) :
                ("Extração incompleta (" + resultadoManifestos.getMotivoInterrupcao() + ") – extraídos " + registrosExtraidos + ", salvos " + registrosSalvos);

            LogExtracaoEntity logManifestos = new LogExtracaoEntity(
                "manifestos",
                inicioManifestos,
                LocalDateTime.now(),
                statusFinal,
                registrosExtraidos,
                resultadoManifestos.getPaginasProcessadas(),
                mensagem
            );
            logExtracaoRepository.gravarLogExtracao(logManifestos);

        } catch (RuntimeException | java.sql.SQLException e) {
            // Registrar erro no log
            LogExtracaoEntity logErro = new LogExtracaoEntity(
                "manifestos",
                inicioManifestos,
                LocalDateTime.now(),
                "ERRO_API",
                0,
                0,
                "Erro: " + e.getMessage()
            );
            logExtracaoRepository.gravarLogExtracao(logErro);
            throw new RuntimeException("Falha na extração de manifestos", e);
        }

        Thread.sleep(2000);

        // Cotações
        System.out.println("\n💹 Extraindo Cotações (últimas 24h)...");
        LocalDateTime inicioCotacoes = LocalDateTime.now();
        try {
            final ResultadoExtracao<CotacaoDTO> resultadoCotacoes = clienteApiDataExport.buscarCotacoes();
            final List<CotacaoDTO> cotacoesDTO = resultadoCotacoes.getDados();
            System.out.println("✓ Extraídas: " + cotacoesDTO.size() + " cotações" + 
                              (resultadoCotacoes.isCompleto() ? "" : " (INCOMPLETO: " + resultadoCotacoes.getMotivoInterrupcao() + ")"));
            
            int registrosSalvos = 0;
            final int registrosExtraidos = resultadoCotacoes.getRegistrosExtraidos();
            if (!cotacoesDTO.isEmpty()) {
                final List<CotacaoEntity> cotacoesEntities = cotacoesDTO.stream()
                    .map(cotacaoMapper::toEntity)
                    .collect(Collectors.toList());
                registrosSalvos = cotacaoRepository.salvar(cotacoesEntities);
                System.out.println("✓ Salvas: " + registrosSalvos + "/" + cotacoesDTO.size() + " cotações");
            }

            // Registrar no log (status enum + quantidade extraída)
            final LogExtracaoEntity.StatusExtracao statusFinal = 
                resultadoCotacoes.isCompleto() ? LogExtracaoEntity.StatusExtracao.COMPLETO : LogExtracaoEntity.StatusExtracao.INCOMPLETO_LIMITE;

            final String mensagem = resultadoCotacoes.isCompleto() ?
                ("Extração completa – extraídos " + registrosExtraidos + ", salvos " + registrosSalvos) :
                ("Extração incompleta (" + resultadoCotacoes.getMotivoInterrupcao() + ") – extraídos " + registrosExtraidos + ", salvos " + registrosSalvos);

            LogExtracaoEntity logCotacoes = new LogExtracaoEntity(
                "cotacoes",
                inicioCotacoes,
                LocalDateTime.now(),
                statusFinal,
                registrosExtraidos,
                resultadoCotacoes.getPaginasProcessadas(),
                mensagem
            );
            logExtracaoRepository.gravarLogExtracao(logCotacoes);

        } catch (RuntimeException | java.sql.SQLException e) {
            // Registrar erro no log
            LogExtracaoEntity logErro = new LogExtracaoEntity(
                "cotacoes",
                inicioCotacoes,
                LocalDateTime.now(),
                "ERRO_API",
                0,
                0,
                "Erro: " + e.getMessage()
            );
            logExtracaoRepository.gravarLogExtracao(logErro);
            throw new RuntimeException("Falha na extração de cotações", e);
        }

        Thread.sleep(2000);

        // Localização de Carga
        System.out.println("\n📍 Extraindo Localização de Carga (últimas 24h)...");
        LocalDateTime inicioLocalizacoes = LocalDateTime.now();
        try {
            final ResultadoExtracao<LocalizacaoCargaDTO> resultadoLocalizacoes = clienteApiDataExport.buscarLocalizacaoCarga();
            final List<LocalizacaoCargaDTO> localizacoesDTO = resultadoLocalizacoes.getDados();
            System.out.println("✓ Extraídas: " + localizacoesDTO.size() + " localizações" + 
                              (resultadoLocalizacoes.isCompleto() ? "" : " (INCOMPLETO: " + resultadoLocalizacoes.getMotivoInterrupcao() + ")"));
            
            int registrosSalvos = 0;
            final int registrosExtraidos = resultadoLocalizacoes.getRegistrosExtraidos();
            if (!localizacoesDTO.isEmpty()) {
                final List<LocalizacaoCargaEntity> localizacoesEntities = localizacoesDTO.stream()
                    .map(localizacaoMapper::toEntity)
                    .collect(Collectors.toList());
                registrosSalvos = localizacaoRepository.salvar(localizacoesEntities);
                System.out.println("✓ Salvas: " + registrosSalvos + "/" + localizacoesDTO.size() + " localizações");
            }

            // Registrar no log (status enum + quantidade extraída)
            final LogExtracaoEntity.StatusExtracao statusFinal = 
                resultadoLocalizacoes.isCompleto() ? LogExtracaoEntity.StatusExtracao.COMPLETO : LogExtracaoEntity.StatusExtracao.INCOMPLETO_LIMITE;

            final String mensagem = resultadoLocalizacoes.isCompleto() ?
                ("Extração completa – extraídos " + registrosExtraidos + ", salvos " + registrosSalvos) :
                ("Extração incompleta (" + resultadoLocalizacoes.getMotivoInterrupcao() + ") – extraídos " + registrosExtraidos + ", salvos " + registrosSalvos);

            LogExtracaoEntity logLocalizacoes = new LogExtracaoEntity(
                "localizacao_cargas",
                inicioLocalizacoes,
                LocalDateTime.now(),
                statusFinal,
                registrosExtraidos,
                resultadoLocalizacoes.getPaginasProcessadas(),
                mensagem
            );
            logExtracaoRepository.gravarLogExtracao(logLocalizacoes);

        } catch (RuntimeException | java.sql.SQLException e) {
            // Registrar erro no log
            LogExtracaoEntity logErro = new LogExtracaoEntity(
                "localizacao_cargas",
                inicioLocalizacoes,
                LocalDateTime.now(),
                "ERRO_API",
                0,
                0,
                "Erro: " + e.getMessage()
            );
            logExtracaoRepository.gravarLogExtracao(logErro);
            throw new RuntimeException("Falha na extração de localização de cargas", e);
        }
    }
}