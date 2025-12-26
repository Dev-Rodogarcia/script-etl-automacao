package br.com.extrator.runners;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.extrator.api.ClienteApiDataExport;
import br.com.extrator.api.ResultadoExtracao;
import br.com.extrator.db.entity.ContasAPagarDataExportEntity;
import br.com.extrator.db.entity.CotacaoEntity;
import br.com.extrator.db.entity.FaturaPorClienteEntity;
import br.com.extrator.db.entity.LocalizacaoCargaEntity;
import br.com.extrator.db.entity.LogExtracaoEntity;
import br.com.extrator.db.entity.ManifestoEntity;
import br.com.extrator.db.repository.ContasAPagarRepository;
import br.com.extrator.db.repository.CotacaoRepository;
import br.com.extrator.db.repository.FaturaPorClienteRepository;
import br.com.extrator.db.repository.LocalizacaoCargaRepository;
import br.com.extrator.db.repository.LogExtracaoRepository;
import br.com.extrator.db.repository.ManifestoRepository;
import br.com.extrator.modelo.dataexport.contasapagar.ContasAPagarDTO;
import br.com.extrator.modelo.dataexport.contasapagar.ContasAPagarMapper;
import br.com.extrator.modelo.dataexport.cotacao.CotacaoDTO;
import br.com.extrator.modelo.dataexport.cotacao.CotacaoMapper;
import br.com.extrator.modelo.dataexport.faturaporcliente.FaturaPorClienteDTO;
import br.com.extrator.modelo.dataexport.faturaporcliente.FaturaPorClienteMapper;
import br.com.extrator.modelo.dataexport.localizacaocarga.LocalizacaoCargaDTO;
import br.com.extrator.modelo.dataexport.localizacaocarga.LocalizacaoCargaMapper;
import br.com.extrator.modelo.dataexport.manifestos.ManifestoDTO;
import br.com.extrator.modelo.dataexport.manifestos.ManifestoMapper;
import br.com.extrator.util.configuracao.CarregadorConfig;
import br.com.extrator.util.console.LoggerConsole;
import br.com.extrator.util.validacao.ConstantesEntidades;

/**
 * Runner independente para a API Data Export (Manifestos, Cotações e Localização de Carga).
 */
public final class DataExportRunner {
    private static final Logger logger = LoggerFactory.getLogger(DataExportRunner.class);
    private static final LoggerConsole log = LoggerConsole.getLogger(DataExportRunner.class);

    private DataExportRunner() {}

    /**
     * Executa extração de todas as entidades Data Export.
     * 
     * @param dataInicio Data de início para filtro
     * @throws Exception Se houver falha na extração
     */
    public static void executar(final LocalDate dataInicio) throws Exception {
        executar(dataInicio, null);
    }

    /**
     * Executa extração de todas as entidades Data Export para um intervalo de datas.
     * 
     * @param dataInicio Data de início do período
     * @param dataFim Data de fim do período
     * @throws Exception Se houver falha na extração
     */
    public static void executarPorIntervalo(final LocalDate dataInicio, final LocalDate dataFim) throws Exception {
        executarPorIntervalo(dataInicio, dataFim, null);
    }

    /**
     * Executa extração de entidade(s) Data Export específica(s) para um intervalo de datas.
     * 
     * @param dataInicio Data de início do período
     * @param dataFim Data de fim do período
     * @param entidade Nome da entidade (null = todas)
     * @throws Exception Se houver falha na extração
     */
    public static void executarPorIntervalo(final LocalDate dataInicio, final LocalDate dataFim, final String entidade) throws Exception {
        log.info("🔄 Executando runner DataExport - Período: {} a {}", dataInicio, dataFim);

        CarregadorConfig.validarConexaoBancoDados();

        final ClienteApiDataExport clienteApiDataExport = new ClienteApiDataExport();
        clienteApiDataExport.setExecutionUuid(java.util.UUID.randomUUID().toString());
        final ManifestoRepository manifestoRepository = new ManifestoRepository();
        final CotacaoRepository cotacaoRepository = new CotacaoRepository();
        final LocalizacaoCargaRepository localizacaoRepository = new LocalizacaoCargaRepository();
        final LogExtracaoRepository logExtracaoRepository = new LogExtracaoRepository();
        final ContasAPagarRepository contasAPagarRepository = new ContasAPagarRepository();
        final FaturaPorClienteRepository faturasPorClienteRepository = new FaturaPorClienteRepository();

        final ManifestoMapper manifestoMapper = new ManifestoMapper();
        final CotacaoMapper cotacaoMapper = new CotacaoMapper();
        final LocalizacaoCargaMapper localizacaoMapper = new LocalizacaoCargaMapper();
        final ContasAPagarMapper contasAPagarMapper = new ContasAPagarMapper();
        final FaturaPorClienteMapper faturasPorClienteMapper = new FaturaPorClienteMapper();

        logExtracaoRepository.criarTabelaSeNaoExistir();

        final String ent = entidade == null ? "" : entidade.trim().toLowerCase();
        final boolean executarManifestos = ent.isEmpty() || ConstantesEntidades.MANIFESTOS.equals(ent);
        final boolean executarCotacoes = ent.isEmpty() || ConstantesEntidades.COTACOES.equals(ent) || java.util.Arrays.stream(ConstantesEntidades.ALIASES_COTACOES).anyMatch(alias -> alias.equals(ent));
        final boolean executarLocalizacao = ent.isEmpty() || ConstantesEntidades.LOCALIZACAO_CARGAS.equals(ent) || java.util.Arrays.stream(ConstantesEntidades.ALIASES_LOCALIZACAO).anyMatch(alias -> alias.equals(ent));
        final boolean executarContasAPagar = ent.isEmpty() || ConstantesEntidades.CONTAS_A_PAGAR.equals(ent) || java.util.Arrays.stream(ConstantesEntidades.ALIASES_CONTAS_PAGAR).anyMatch(alias -> alias.equals(ent)) || "constas a pagar".equals(ent) || "constas-a-pagar".equals(ent);
        final boolean executarFaturasPorCliente = ent.isEmpty() || ConstantesEntidades.FATURAS_POR_CLIENTE.equals(ent) || java.util.Arrays.stream(ConstantesEntidades.ALIASES_FATURAS_CLIENTE).anyMatch(alias -> alias.equals(ent));

        if (executarManifestos) {
            log.info("\n🧾 Extraindo Manifestos (período: {} a {})...", dataInicio, dataFim);
            final LocalDateTime inicioManifestos = LocalDateTime.now();
            try {
                final ResultadoExtracao<ManifestoDTO> resultadoManifestos = clienteApiDataExport.buscarManifestos(dataInicio, dataFim);
                final List<ManifestoDTO> manifestosDTO = resultadoManifestos.getDados();
                final String statusManifestos = resultadoManifestos.isCompleto() ? "" : " (INCOMPLETO: " + resultadoManifestos.getMotivoInterrupcao() + ")";
                log.info("✓ Extraídos: {} manifestos{}", manifestosDTO.size(), statusManifestos);
                int registrosSalvos = 0;
                int totalUnicos = 0;
                final int registrosExtraidos = resultadoManifestos.getRegistrosExtraidos();
                if (!manifestosDTO.isEmpty()) {
                    final List<ManifestoEntity> manifestosEntities = manifestosDTO.stream()
                        .map(manifestoMapper::toEntity)
                        .collect(Collectors.toList());
                    final List<ManifestoEntity> manifestosUnicos = deduplicarManifestos(manifestosEntities);
                    totalUnicos = manifestosUnicos.size();
                    if (manifestosEntities.size() != manifestosUnicos.size()) {
                        final int duplicadosRemovidos = manifestosEntities.size() - manifestosUnicos.size();
                        log.warn("⚠️ Removidos {} duplicados da resposta da API antes de salvar", duplicadosRemovidos);
                    }
                    registrosSalvos = manifestoRepository.salvar(manifestosUnicos);
                    log.info("✓ Processados: {}/{} manifestos (INSERTs + UPDATEs)", registrosSalvos, totalUnicos);
                }
                final LogExtracaoEntity.StatusExtracao statusFinal =
                    resultadoManifestos.isCompleto() ? LogExtracaoEntity.StatusExtracao.COMPLETO : LogExtracaoEntity.StatusExtracao.INCOMPLETO_LIMITE;
                final String mensagem = resultadoManifestos.isCompleto() ?
                    ("Extração completa – extraídos " + registrosExtraidos + " (únicos: " + totalUnicos + "), processados " + registrosSalvos + " (INSERTs + UPDATEs) | Período: " + dataInicio + " a " + dataFim) :
                    ("Extração incompleta (" + resultadoManifestos.getMotivoInterrupcao() + ") – extraídos " + registrosExtraidos + " (únicos: " + totalUnicos + "), processados " + registrosSalvos + " (INSERTs + UPDATEs) | Período: " + dataInicio + " a " + dataFim);
                final LogExtracaoEntity logManifestos = new LogExtracaoEntity(
                    ConstantesEntidades.MANIFESTOS,
                    inicioManifestos,
                    LocalDateTime.now(),
                    statusFinal,
                    totalUnicos,
                    resultadoManifestos.getPaginasProcessadas(),
                    mensagem
                );
                logExtracaoRepository.gravarLogExtracao(logManifestos);
                logExtracaoRepository.criarOuAtualizarViewDimVeiculos();
                logExtracaoRepository.criarOuAtualizarViewDimMotoristas();
            } catch (RuntimeException | java.sql.SQLException e) {
                final LogExtracaoEntity logErro = new LogExtracaoEntity(
                    ConstantesEntidades.MANIFESTOS,
                    inicioManifestos,
                    LocalDateTime.now(),
                    ConstantesEntidades.STATUS_ERRO_API,
                    0,
                    0,
                    "Erro: " + e.getMessage()
                );
                logExtracaoRepository.gravarLogExtracao(logErro);
                throw new RuntimeException("Falha na extração de manifestos", e);
            }
            ConstantesEntidades.aplicarDelayEntreExtracoes();
        }

        if (executarCotacoes) {
            log.info("\n💹 Extraindo Cotações (período: {} a {})...", dataInicio, dataFim);
            final LocalDateTime inicioCotacoes = LocalDateTime.now();
            try {
                final ResultadoExtracao<CotacaoDTO> resultadoCotacoes = clienteApiDataExport.buscarCotacoes(dataInicio, dataFim);
                final List<CotacaoDTO> cotacoesDTO = resultadoCotacoes.getDados();
                final String statusCotacoes = resultadoCotacoes.isCompleto() ? "" : " (INCOMPLETO: " + resultadoCotacoes.getMotivoInterrupcao() + ")";
                log.info("✓ Extraídas: {} cotações{}", cotacoesDTO.size(), statusCotacoes);
                int totalUnicos = 0;
                int registrosSalvos = 0;
                final int registrosExtraidos = resultadoCotacoes.getRegistrosExtraidos();
                if (!cotacoesDTO.isEmpty()) {
                    final List<CotacaoEntity> cotacoesEntities = cotacoesDTO.stream()
                        .map(cotacaoMapper::toEntity)
                        .collect(Collectors.toList());
                    final List<CotacaoEntity> cotacoesUnicas = deduplicarCotacoes(cotacoesEntities);
                    totalUnicos = cotacoesUnicas.size();
                    if (cotacoesEntities.size() != cotacoesUnicas.size()) {
                        final int duplicadosRemovidos = cotacoesEntities.size() - cotacoesUnicas.size();
                        log.warn("⚠️ Removidos {} duplicados da resposta da API antes de salvar", duplicadosRemovidos);
                    }
                    registrosSalvos = cotacaoRepository.salvar(cotacoesUnicas);
                    log.info("✓ Processadas: {}/{} cotações (INSERTs + UPDATEs)", registrosSalvos, totalUnicos);
                }
                final LogExtracaoEntity.StatusExtracao statusFinal =
                    resultadoCotacoes.isCompleto() ? LogExtracaoEntity.StatusExtracao.COMPLETO : LogExtracaoEntity.StatusExtracao.INCOMPLETO_LIMITE;
                final String mensagem = resultadoCotacoes.isCompleto() ?
                    ("Extração completa – extraídos " + registrosExtraidos + " (únicos: " + totalUnicos + "), processados " + registrosSalvos + " (INSERTs + UPDATEs) | Período: " + dataInicio + " a " + dataFim) :
                    ("Extração incompleta (" + resultadoCotacoes.getMotivoInterrupcao() + ") – extraídos " + registrosExtraidos + " (únicos: " + totalUnicos + "), processados " + registrosSalvos + " (INSERTs + UPDATEs) | Período: " + dataInicio + " a " + dataFim);
                final LogExtracaoEntity logCotacoes = new LogExtracaoEntity(
                    ConstantesEntidades.COTACOES,
                    inicioCotacoes,
                    LocalDateTime.now(),
                    statusFinal,
                    totalUnicos,
                    resultadoCotacoes.getPaginasProcessadas(),
                    mensagem
                );
                logExtracaoRepository.gravarLogExtracao(logCotacoes);
            } catch (RuntimeException | java.sql.SQLException e) {
                final LogExtracaoEntity logErro = new LogExtracaoEntity(
                    ConstantesEntidades.COTACOES,
                    inicioCotacoes,
                    LocalDateTime.now(),
                    ConstantesEntidades.STATUS_ERRO_API,
                    0,
                    0,
                    "Erro: " + e.getMessage()
                );
                logExtracaoRepository.gravarLogExtracao(logErro);
                throw new RuntimeException("Falha na extração de cotações", e);
            }
            ConstantesEntidades.aplicarDelayEntreExtracoes();
        }

        if (executarLocalizacao) {
            log.info("\n📍 Extraindo Localização de Carga (período: {} a {})...", dataInicio, dataFim);
            final LocalDateTime inicioLocalizacoes = LocalDateTime.now();
            try {
                final ResultadoExtracao<LocalizacaoCargaDTO> resultadoLocalizacoes = clienteApiDataExport.buscarLocalizacaoCarga(dataInicio, dataFim);
                final List<LocalizacaoCargaDTO> localizacoesDTO = resultadoLocalizacoes.getDados();
                final String statusLocalizacoes = resultadoLocalizacoes.isCompleto() ? "" : " (INCOMPLETO: " + resultadoLocalizacoes.getMotivoInterrupcao() + ")";
                log.info("✓ Extraídas: {} localizações{}", localizacoesDTO.size(), statusLocalizacoes);
                final int registrosExtraidos = resultadoLocalizacoes.getRegistrosExtraidos();
                if (!localizacoesDTO.isEmpty()) {
                    final List<LocalizacaoCargaEntity> localizacoesEntities = localizacoesDTO.stream()
                        .map(localizacaoMapper::toEntity)
                        .collect(Collectors.toList());
                    final List<LocalizacaoCargaEntity> localizacoesUnicas = deduplicarLocalizacoes(localizacoesEntities);
                    if (localizacoesEntities.size() != localizacoesUnicas.size()) {
                        final int duplicadosRemovidos = localizacoesEntities.size() - localizacoesUnicas.size();
                        log.warn("⚠️ Removidos {} duplicados da resposta da API antes de salvar", duplicadosRemovidos);
                    }
                    final int registrosSalvos = localizacaoRepository.salvar(localizacoesUnicas);
                    final int totalUnicos = localizacoesUnicas.size();
                    log.info("✓ Processadas: {}/{} localizações (INSERTs + UPDATEs)", registrosSalvos, totalUnicos);
                    final LogExtracaoEntity.StatusExtracao statusFinal =
                        resultadoLocalizacoes.isCompleto() ? LogExtracaoEntity.StatusExtracao.COMPLETO : LogExtracaoEntity.StatusExtracao.INCOMPLETO_LIMITE;
                    final String mensagem = resultadoLocalizacoes.isCompleto() ?
                        ("Extração completa – extraídos " + registrosExtraidos + " (únicos: " + totalUnicos + "), processados " + registrosSalvos + " (INSERTs + UPDATEs) | Período: " + dataInicio + " a " + dataFim) :
                        ("Extração incompleta (" + resultadoLocalizacoes.getMotivoInterrupcao() + ") – extraídos " + registrosExtraidos + " (únicos: " + totalUnicos + "), processados " + registrosSalvos + " (INSERTs + UPDATEs) | Período: " + dataInicio + " a " + dataFim);
                    final LogExtracaoEntity logLocalizacoes = new LogExtracaoEntity(
                        ConstantesEntidades.LOCALIZACAO_CARGAS,
                        inicioLocalizacoes,
                        LocalDateTime.now(),
                        statusFinal,
                        totalUnicos,
                        resultadoLocalizacoes.getPaginasProcessadas(),
                        mensagem
                    );
                    logExtracaoRepository.gravarLogExtracao(logLocalizacoes);
                }
            } catch (RuntimeException | java.sql.SQLException e) {
                final LogExtracaoEntity logErro = new LogExtracaoEntity(
                    ConstantesEntidades.LOCALIZACAO_CARGAS,
                    inicioLocalizacoes,
                    LocalDateTime.now(),
                    ConstantesEntidades.STATUS_ERRO_API,
                    0,
                    0,
                    "Erro: " + e.getMessage()
                );
                logExtracaoRepository.gravarLogExtracao(logErro);
                throw new RuntimeException("Falha na extração de localização de cargas", e);
            }
            ConstantesEntidades.aplicarDelayEntreExtracoes();
        }

        if (executarContasAPagar) {
            log.info("\n💰 Extraindo Faturas a Pagar - Data Export (período: {} a {})...", dataInicio, dataFim);
            final LocalDateTime inicioFaturasAPagar = LocalDateTime.now();
            try {
                final ResultadoExtracao<ContasAPagarDTO> resultadoFaturasAPagar = clienteApiDataExport.buscarContasAPagar(dataInicio, dataFim);
                final List<ContasAPagarDTO> faturasAPagarDTO = resultadoFaturasAPagar.getDados();
                final String statusFaturasAPagar = resultadoFaturasAPagar.isCompleto() ? "" : " (INCOMPLETO: " + resultadoFaturasAPagar.getMotivoInterrupcao() + ")";
                log.info("✓ Extraídas: {} faturas a pagar{}", faturasAPagarDTO.size(), statusFaturasAPagar);
                int totalUnicos = 0;
                int registrosSalvos = 0;
                final int registrosExtraidos = resultadoFaturasAPagar.getRegistrosExtraidos();
                if (!faturasAPagarDTO.isEmpty()) {
                    final List<ContasAPagarDataExportEntity> faturasAPagarEntities = faturasAPagarDTO.stream()
                        .map(contasAPagarMapper::toEntity)
                        .collect(Collectors.toList());
                    final List<ContasAPagarDataExportEntity> faturasAPagarUnicas = deduplicarFaturasAPagar(faturasAPagarEntities);
                    totalUnicos = faturasAPagarUnicas.size();
                    if (faturasAPagarEntities.size() != faturasAPagarUnicas.size()) {
                        final int duplicadosRemovidos = faturasAPagarEntities.size() - faturasAPagarUnicas.size();
                        log.warn("⚠️ Removidos {} duplicados da resposta da API antes de salvar", duplicadosRemovidos);
                    }
                    registrosSalvos = contasAPagarRepository.salvar(faturasAPagarUnicas);
                    log.info("✓ Processadas: {}/{} faturas a pagar (INSERTs + UPDATEs)", registrosSalvos, totalUnicos);
                }
                final LogExtracaoEntity.StatusExtracao statusFinal =
                    resultadoFaturasAPagar.isCompleto() ? LogExtracaoEntity.StatusExtracao.COMPLETO : LogExtracaoEntity.StatusExtracao.INCOMPLETO_LIMITE;
                final String mensagem = resultadoFaturasAPagar.isCompleto() ?
                    ("Extração completa – extraídos " + registrosExtraidos + " (únicos: " + totalUnicos + "), processados " + registrosSalvos + " (INSERTs + UPDATEs) | Período: " + dataInicio + " a " + dataFim) :
                    ("Extração incompleta (" + resultadoFaturasAPagar.getMotivoInterrupcao() + ") – extraídos " + registrosExtraidos + " (únicos: " + totalUnicos + "), processados " + registrosSalvos + " (INSERTs + UPDATEs) | Período: " + dataInicio + " a " + dataFim);
                final LogExtracaoEntity logFaturasAPagar = new LogExtracaoEntity(
                    ConstantesEntidades.CONTAS_A_PAGAR,
                    inicioFaturasAPagar,
                    LocalDateTime.now(),
                    statusFinal,
                    totalUnicos,
                    resultadoFaturasAPagar.getPaginasProcessadas(),
                    mensagem
                );
                logExtracaoRepository.gravarLogExtracao(logFaturasAPagar);
                logExtracaoRepository.criarOuAtualizarViewDimPlanoContas();
            } catch (RuntimeException | java.sql.SQLException e) {
                final LogExtracaoEntity logErro = new LogExtracaoEntity(
                    ConstantesEntidades.CONTAS_A_PAGAR,
                    inicioFaturasAPagar,
                    LocalDateTime.now(),
                    ConstantesEntidades.STATUS_ERRO_API,
                    0,
                    0,
                    "Erro: " + e.getMessage()
                );
                logExtracaoRepository.gravarLogExtracao(logErro);
                throw new RuntimeException("Falha na extração de faturas a pagar", e);
            }
            ConstantesEntidades.aplicarDelayEntreExtracoes();
        }

        if (executarFaturasPorCliente) {
            log.info("\n💰 Extraindo Faturas por Cliente (período: {} a {})...", dataInicio, dataFim);
            final LocalDateTime inicioFaturasPorCliente = LocalDateTime.now();
            try {
                final ResultadoExtracao<FaturaPorClienteDTO> resultadoFaturasPorCliente =
                    clienteApiDataExport.buscarFaturasPorCliente(dataInicio, dataFim);
                final List<FaturaPorClienteDTO> faturasPorClienteDTO = resultadoFaturasPorCliente.getDados();
                final String statusFaturasPorCliente = resultadoFaturasPorCliente.isCompleto() ? "" : " (INCOMPLETO: " + resultadoFaturasPorCliente.getMotivoInterrupcao() + ")";
                log.info("✓ Extraídas: {} faturas por cliente{}", faturasPorClienteDTO.size(), statusFaturasPorCliente);
                int totalUnicos = 0;
                int registrosSalvos = 0;
                final int registrosExtraidos = resultadoFaturasPorCliente.getRegistrosExtraidos();
                if (!faturasPorClienteDTO.isEmpty()) {
                    final List<FaturaPorClienteEntity> faturasPorClienteEntities = faturasPorClienteDTO.stream()
                        .map(faturasPorClienteMapper::toEntity)
                        .collect(Collectors.toList());
                    final List<FaturaPorClienteEntity> faturasPorClienteUnicas =
                        deduplicarFaturasPorCliente(faturasPorClienteEntities);
                    totalUnicos = faturasPorClienteUnicas.size();
                    if (faturasPorClienteEntities.size() != faturasPorClienteUnicas.size()) {
                        final int duplicadosRemovidos = faturasPorClienteEntities.size() - faturasPorClienteUnicas.size();
                        log.warn("⚠️ Removidos {} duplicados da resposta da API antes de salvar", duplicadosRemovidos);
                    }
                    registrosSalvos = faturasPorClienteRepository.salvar(faturasPorClienteUnicas);
                    log.info("✓ Processadas: {}/{} faturas por cliente (INSERTs + UPDATEs)", registrosSalvos, totalUnicos);
                }
                final LogExtracaoEntity.StatusExtracao statusFinal =
                    resultadoFaturasPorCliente.isCompleto() ? LogExtracaoEntity.StatusExtracao.COMPLETO : LogExtracaoEntity.StatusExtracao.INCOMPLETO_LIMITE;
                final String mensagem = resultadoFaturasPorCliente.isCompleto() ?
                    ("Extração completa – extraídos " + registrosExtraidos + 
                     " (únicos: " + totalUnicos + "), processados " + registrosSalvos + " (INSERTs + UPDATEs) | Período: " + dataInicio + " a " + dataFim) :
                    ("Extração incompleta (" + resultadoFaturasPorCliente.getMotivoInterrupcao() + 
                     ") – extraídos " + registrosExtraidos + 
                     " (únicos: " + totalUnicos + "), processados " + registrosSalvos + " (INSERTs + UPDATEs) | Período: " + dataInicio + " a " + dataFim);
                final LogExtracaoEntity logFaturasPorCliente = new LogExtracaoEntity(
                    ConstantesEntidades.FATURAS_POR_CLIENTE,
                    inicioFaturasPorCliente,
                    LocalDateTime.now(),
                    statusFinal,
                    totalUnicos,
                    resultadoFaturasPorCliente.getPaginasProcessadas(),
                    mensagem
                );
                logExtracaoRepository.gravarLogExtracao(logFaturasPorCliente);
            } catch (RuntimeException | java.sql.SQLException e) {
                final LogExtracaoEntity logErro = new LogExtracaoEntity(
                    ConstantesEntidades.FATURAS_POR_CLIENTE,
                    inicioFaturasPorCliente,
                    LocalDateTime.now(),
                    ConstantesEntidades.STATUS_ERRO_API,
                    0,
                    0,
                    "Erro: " + e.getMessage()
                );
                logExtracaoRepository.gravarLogExtracao(logErro);
                throw new RuntimeException("Falha na extração de faturas por cliente", e);
            }
        }
        logExtracaoRepository.criarOuAtualizarViewDimFiliais();
    }

    /**
     * Executa extração de entidade(s) Data Export específica(s).
     * 
     * @param dataInicio Data de início para filtro
     * @param entidade Nome da entidade (null = todas)
     * @throws Exception Se houver falha na extração
     */
    public static void executar(final LocalDate dataInicio, final String entidade) throws Exception {
        log.info("🔄 Executando runner DataExport...");

        CarregadorConfig.validarConexaoBancoDados();

        final ClienteApiDataExport clienteApiDataExport = new ClienteApiDataExport();
        clienteApiDataExport.setExecutionUuid(java.util.UUID.randomUUID().toString());
        final ManifestoRepository manifestoRepository = new ManifestoRepository();
        final CotacaoRepository cotacaoRepository = new CotacaoRepository();
        final LocalizacaoCargaRepository localizacaoRepository = new LocalizacaoCargaRepository();
        final LogExtracaoRepository logExtracaoRepository = new LogExtracaoRepository();
        final ContasAPagarRepository contasAPagarRepository = new ContasAPagarRepository();
        final FaturaPorClienteRepository faturasPorClienteRepository = new FaturaPorClienteRepository();

        final ManifestoMapper manifestoMapper = new ManifestoMapper();
        final CotacaoMapper cotacaoMapper = new CotacaoMapper();
        final LocalizacaoCargaMapper localizacaoMapper = new LocalizacaoCargaMapper();
        final ContasAPagarMapper contasAPagarMapper = new ContasAPagarMapper();
        final FaturaPorClienteMapper faturasPorClienteMapper = new FaturaPorClienteMapper();

        logExtracaoRepository.criarTabelaSeNaoExistir();

        final String ent = entidade == null ? "" : entidade.trim().toLowerCase();
        final boolean executarManifestos = ent.isEmpty() || ConstantesEntidades.MANIFESTOS.equals(ent);
        final boolean executarCotacoes = ent.isEmpty() || ConstantesEntidades.COTACOES.equals(ent) || java.util.Arrays.stream(ConstantesEntidades.ALIASES_COTACOES).anyMatch(alias -> alias.equals(ent));
        final boolean executarLocalizacao = ent.isEmpty() || ConstantesEntidades.LOCALIZACAO_CARGAS.equals(ent) || java.util.Arrays.stream(ConstantesEntidades.ALIASES_LOCALIZACAO).anyMatch(alias -> alias.equals(ent));
        final boolean executarContasAPagar = ent.isEmpty() || ConstantesEntidades.CONTAS_A_PAGAR.equals(ent) || java.util.Arrays.stream(ConstantesEntidades.ALIASES_CONTAS_PAGAR).anyMatch(alias -> alias.equals(ent)) || "constas a pagar".equals(ent) || "constas-a-pagar".equals(ent);
        final boolean executarFaturasPorCliente = ent.isEmpty() || ConstantesEntidades.FATURAS_POR_CLIENTE.equals(ent) || java.util.Arrays.stream(ConstantesEntidades.ALIASES_FATURAS_CLIENTE).anyMatch(alias -> alias.equals(ent));

        if (executarManifestos) {
            log.info("\n🧾 Extraindo Manifestos (últimas 24h)...");
            final LocalDateTime inicioManifestos = LocalDateTime.now();
            try {
                final ResultadoExtracao<ManifestoDTO> resultadoManifestos = clienteApiDataExport.buscarManifestos();
                final List<ManifestoDTO> manifestosDTO = resultadoManifestos.getDados();
                final String statusManifestos = resultadoManifestos.isCompleto() ? "" : " (INCOMPLETO: " + resultadoManifestos.getMotivoInterrupcao() + ")";
                log.info("✓ Extraídos: {} manifestos{}", manifestosDTO.size(), statusManifestos);
                int registrosSalvos = 0;
                int totalUnicos = 0;
                final int registrosExtraidos = resultadoManifestos.getRegistrosExtraidos();
                if (!manifestosDTO.isEmpty()) {
                    final List<ManifestoEntity> manifestosEntities = manifestosDTO.stream()
                        .map(manifestoMapper::toEntity)
                        .collect(Collectors.toList());
                    final List<ManifestoEntity> manifestosUnicos = deduplicarManifestos(manifestosEntities);
                    totalUnicos = manifestosUnicos.size();
                    if (manifestosEntities.size() != manifestosUnicos.size()) {
                        final int duplicadosRemovidos = manifestosEntities.size() - manifestosUnicos.size();
                        log.warn("⚠️ Removidos {} duplicados da resposta da API antes de salvar", duplicadosRemovidos);
                    }
                    registrosSalvos = manifestoRepository.salvar(manifestosUnicos);
                    log.info("✓ Processados: {}/{} manifestos (INSERTs + UPDATEs)", registrosSalvos, totalUnicos);
                }
                final LogExtracaoEntity.StatusExtracao statusFinal =
                    resultadoManifestos.isCompleto() ? LogExtracaoEntity.StatusExtracao.COMPLETO : LogExtracaoEntity.StatusExtracao.INCOMPLETO_LIMITE;
                final String mensagem = resultadoManifestos.isCompleto() ?
                    ("Extração completa – extraídos " + registrosExtraidos + " (únicos: " + totalUnicos + "), processados " + registrosSalvos + " (INSERTs + UPDATEs)") :
                    ("Extração incompleta (" + resultadoManifestos.getMotivoInterrupcao() + ") – extraídos " + registrosExtraidos + " (únicos: " + totalUnicos + "), processados " + registrosSalvos + " (INSERTs + UPDATEs)");
                final LogExtracaoEntity logManifestos = new LogExtracaoEntity(
                    ConstantesEntidades.MANIFESTOS,
                    inicioManifestos,
                    LocalDateTime.now(),
                    statusFinal,
                    totalUnicos,
                    resultadoManifestos.getPaginasProcessadas(),
                    mensagem
                );
                logExtracaoRepository.gravarLogExtracao(logManifestos);
                logExtracaoRepository.criarOuAtualizarViewDimVeiculos();
                logExtracaoRepository.criarOuAtualizarViewDimMotoristas();
            } catch (RuntimeException | java.sql.SQLException e) {
                final LogExtracaoEntity logErro = new LogExtracaoEntity(
                    ConstantesEntidades.MANIFESTOS,
                    inicioManifestos,
                    LocalDateTime.now(),
                    ConstantesEntidades.STATUS_ERRO_API,
                    0,
                    0,
                    "Erro: " + e.getMessage()
                );
                logExtracaoRepository.gravarLogExtracao(logErro);
                throw new RuntimeException("Falha na extração de manifestos", e);
            }
            ConstantesEntidades.aplicarDelayEntreExtracoes();
        }

        if (executarCotacoes) {
            log.info("\n💹 Extraindo Cotações (últimas 24h)...");
            final LocalDateTime inicioCotacoes = LocalDateTime.now();
            try {
                final ResultadoExtracao<CotacaoDTO> resultadoCotacoes = clienteApiDataExport.buscarCotacoes();
                final List<CotacaoDTO> cotacoesDTO = resultadoCotacoes.getDados();
                final String statusCotacoes = resultadoCotacoes.isCompleto() ? "" : " (INCOMPLETO: " + resultadoCotacoes.getMotivoInterrupcao() + ")";
                log.info("✓ Extraídas: {} cotações{}", cotacoesDTO.size(), statusCotacoes);
                int totalUnicos = 0;
                int registrosSalvos = 0;
                final int registrosExtraidos = resultadoCotacoes.getRegistrosExtraidos();
                if (!cotacoesDTO.isEmpty()) {
                    final List<CotacaoEntity> cotacoesEntities = cotacoesDTO.stream()
                        .map(cotacaoMapper::toEntity)
                        .collect(Collectors.toList());
                    final List<CotacaoEntity> cotacoesUnicas = deduplicarCotacoes(cotacoesEntities);
                    totalUnicos = cotacoesUnicas.size();
                    if (cotacoesEntities.size() != cotacoesUnicas.size()) {
                        final int duplicadosRemovidos = cotacoesEntities.size() - cotacoesUnicas.size();
                        log.warn("⚠️ Removidos {} duplicados da resposta da API antes de salvar", duplicadosRemovidos);
                    }
                    registrosSalvos = cotacaoRepository.salvar(cotacoesUnicas);
                    log.info("✓ Processadas: {}/{} cotações (INSERTs + UPDATEs)", registrosSalvos, totalUnicos);
                }
                final LogExtracaoEntity.StatusExtracao statusFinal =
                    resultadoCotacoes.isCompleto() ? LogExtracaoEntity.StatusExtracao.COMPLETO : LogExtracaoEntity.StatusExtracao.INCOMPLETO_LIMITE;
                final String mensagem = resultadoCotacoes.isCompleto() ?
                    ("Extração completa – extraídos " + registrosExtraidos + " (únicos: " + totalUnicos + "), processados " + registrosSalvos + " (INSERTs + UPDATEs)") :
                    ("Extração incompleta (" + resultadoCotacoes.getMotivoInterrupcao() + ") – extraídos " + registrosExtraidos + " (únicos: " + totalUnicos + "), processados " + registrosSalvos + " (INSERTs + UPDATEs)");
                final LogExtracaoEntity logCotacoes = new LogExtracaoEntity(
                    ConstantesEntidades.COTACOES,
                    inicioCotacoes,
                    LocalDateTime.now(),
                    statusFinal,
                    totalUnicos,
                    resultadoCotacoes.getPaginasProcessadas(),
                    mensagem
                );
                logExtracaoRepository.gravarLogExtracao(logCotacoes);
            } catch (RuntimeException | java.sql.SQLException e) {
                final LogExtracaoEntity logErro = new LogExtracaoEntity(
                    ConstantesEntidades.COTACOES,
                    inicioCotacoes,
                    LocalDateTime.now(),
                    ConstantesEntidades.STATUS_ERRO_API,
                    0,
                    0,
                    "Erro: " + e.getMessage()
                );
                logExtracaoRepository.gravarLogExtracao(logErro);
                throw new RuntimeException("Falha na extração de cotações", e);
            }
            ConstantesEntidades.aplicarDelayEntreExtracoes();
        }

        if (executarLocalizacao) {
            log.info("\n📍 Extraindo Localização de Carga (últimas 24h)...");
            final LocalDateTime inicioLocalizacoes = LocalDateTime.now();
            try {
                final ResultadoExtracao<LocalizacaoCargaDTO> resultadoLocalizacoes = clienteApiDataExport.buscarLocalizacaoCarga();
                final List<LocalizacaoCargaDTO> localizacoesDTO = resultadoLocalizacoes.getDados();
                final String statusLocalizacoes = resultadoLocalizacoes.isCompleto() ? "" : " (INCOMPLETO: " + resultadoLocalizacoes.getMotivoInterrupcao() + ")";
                log.info("✓ Extraídas: {} localizações{}", localizacoesDTO.size(), statusLocalizacoes);
                final int registrosExtraidos = resultadoLocalizacoes.getRegistrosExtraidos();
                if (!localizacoesDTO.isEmpty()) {
                    final List<LocalizacaoCargaEntity> localizacoesEntities = localizacoesDTO.stream()
                        .map(localizacaoMapper::toEntity)
                        .collect(Collectors.toList());
                    final List<LocalizacaoCargaEntity> localizacoesUnicas = deduplicarLocalizacoes(localizacoesEntities);
                    if (localizacoesEntities.size() != localizacoesUnicas.size()) {
                        final int duplicadosRemovidos = localizacoesEntities.size() - localizacoesUnicas.size();
                        log.warn("⚠️ Removidos {} duplicados da resposta da API antes de salvar", duplicadosRemovidos);
                    }
                    final int registrosSalvos = localizacaoRepository.salvar(localizacoesUnicas);
                    final int totalUnicos = localizacoesUnicas.size();
                    log.info("✓ Processadas: {}/{} localizações (INSERTs + UPDATEs)", registrosSalvos, totalUnicos);
                    final LogExtracaoEntity.StatusExtracao statusFinal =
                        resultadoLocalizacoes.isCompleto() ? LogExtracaoEntity.StatusExtracao.COMPLETO : LogExtracaoEntity.StatusExtracao.INCOMPLETO_LIMITE;
                    final String mensagem = resultadoLocalizacoes.isCompleto() ?
                        ("Extração completa – extraídos " + registrosExtraidos + " (únicos: " + totalUnicos + "), processados " + registrosSalvos + " (INSERTs + UPDATEs)") :
                        ("Extração incompleta (" + resultadoLocalizacoes.getMotivoInterrupcao() + ") – extraídos " + registrosExtraidos + " (únicos: " + totalUnicos + "), processados " + registrosSalvos + " (INSERTs + UPDATEs)");
                    final LogExtracaoEntity logLocalizacoes = new LogExtracaoEntity(
                        ConstantesEntidades.LOCALIZACAO_CARGAS,
                        inicioLocalizacoes,
                        LocalDateTime.now(),
                        statusFinal,
                        totalUnicos,
                        resultadoLocalizacoes.getPaginasProcessadas(),
                        mensagem
                    );
                    logExtracaoRepository.gravarLogExtracao(logLocalizacoes);
                }
            } catch (RuntimeException | java.sql.SQLException e) {
                final LogExtracaoEntity logErro = new LogExtracaoEntity(
                    ConstantesEntidades.LOCALIZACAO_CARGAS,
                    inicioLocalizacoes,
                    LocalDateTime.now(),
                    ConstantesEntidades.STATUS_ERRO_API,
                    0,
                    0,
                    "Erro: " + e.getMessage()
                );
                logExtracaoRepository.gravarLogExtracao(logErro);
                throw new RuntimeException("Falha na extração de localização de cargas", e);
            }
            ConstantesEntidades.aplicarDelayEntreExtracoes();
        }

        if (executarContasAPagar) {
            log.info("\n💰 Extraindo Faturas a Pagar - Data Export (últimas 24h)...");
            final LocalDateTime inicioFaturasAPagar = LocalDateTime.now();
            try {
                final ResultadoExtracao<ContasAPagarDTO> resultadoFaturasAPagar = clienteApiDataExport.buscarContasAPagar();
                final List<ContasAPagarDTO> faturasAPagarDTO = resultadoFaturasAPagar.getDados();
                final String statusFaturasAPagar = resultadoFaturasAPagar.isCompleto() ? "" : " (INCOMPLETO: " + resultadoFaturasAPagar.getMotivoInterrupcao() + ")";
                log.info("✓ Extraídas: {} faturas a pagar{}", faturasAPagarDTO.size(), statusFaturasAPagar);
                int totalUnicos = 0;
                int registrosSalvos = 0;
                final int registrosExtraidos = resultadoFaturasAPagar.getRegistrosExtraidos();
                if (!faturasAPagarDTO.isEmpty()) {
                    final List<ContasAPagarDataExportEntity> faturasAPagarEntities = faturasAPagarDTO.stream()
                        .map(contasAPagarMapper::toEntity)
                        .collect(Collectors.toList());
                    final List<ContasAPagarDataExportEntity> faturasAPagarUnicas = deduplicarFaturasAPagar(faturasAPagarEntities);
                    totalUnicos = faturasAPagarUnicas.size();
                    if (faturasAPagarEntities.size() != faturasAPagarUnicas.size()) {
                        final int duplicadosRemovidos = faturasAPagarEntities.size() - faturasAPagarUnicas.size();
                        log.warn("⚠️ Removidos {} duplicados da resposta da API antes de salvar", duplicadosRemovidos);
                    }
                    registrosSalvos = contasAPagarRepository.salvar(faturasAPagarUnicas);
                    log.info("✓ Processadas: {}/{} faturas a pagar (INSERTs + UPDATEs)", registrosSalvos, totalUnicos);
                }
                final LogExtracaoEntity.StatusExtracao statusFinal =
                    resultadoFaturasAPagar.isCompleto() ? LogExtracaoEntity.StatusExtracao.COMPLETO : LogExtracaoEntity.StatusExtracao.INCOMPLETO_LIMITE;
                final String mensagem = resultadoFaturasAPagar.isCompleto() ?
                    ("Extração completa – extraídos " + registrosExtraidos + " (únicos: " + totalUnicos + "), processados " + registrosSalvos + " (INSERTs + UPDATEs)") :
                    ("Extração incompleta (" + resultadoFaturasAPagar.getMotivoInterrupcao() + ") – extraídos " + registrosExtraidos + " (únicos: " + totalUnicos + "), processados " + registrosSalvos + " (INSERTs + UPDATEs)");
                final LogExtracaoEntity logFaturasAPagar = new LogExtracaoEntity(
                    ConstantesEntidades.CONTAS_A_PAGAR,
                    inicioFaturasAPagar,
                    LocalDateTime.now(),
                    statusFinal,
                    totalUnicos,
                    resultadoFaturasAPagar.getPaginasProcessadas(),
                    mensagem
                );
                logExtracaoRepository.gravarLogExtracao(logFaturasAPagar);
                logExtracaoRepository.criarOuAtualizarViewDimPlanoContas();
            } catch (RuntimeException | java.sql.SQLException e) {
                final LogExtracaoEntity logErro = new LogExtracaoEntity(
                    ConstantesEntidades.CONTAS_A_PAGAR,
                    inicioFaturasAPagar,
                    LocalDateTime.now(),
                    ConstantesEntidades.STATUS_ERRO_API,
                    0,
                    0,
                    "Erro: " + e.getMessage()
                );
                logExtracaoRepository.gravarLogExtracao(logErro);
                throw new RuntimeException("Falha na extração de faturas a pagar", e);
            }
            ConstantesEntidades.aplicarDelayEntreExtracoes();
        }

        if (executarFaturasPorCliente) {
            log.info("\n💰 Extraindo Faturas por Cliente (últimas 24h)...");
            final LocalDateTime inicioFaturasPorCliente = LocalDateTime.now();
            try {
                final ResultadoExtracao<FaturaPorClienteDTO> resultadoFaturasPorCliente =
                    clienteApiDataExport.buscarFaturasPorCliente();
                final List<FaturaPorClienteDTO> faturasPorClienteDTO = resultadoFaturasPorCliente.getDados();
                final String statusFaturasPorCliente = resultadoFaturasPorCliente.isCompleto() ? "" : " (INCOMPLETO: " + resultadoFaturasPorCliente.getMotivoInterrupcao() + ")";
                log.info("✓ Extraídas: {} faturas por cliente{}", faturasPorClienteDTO.size(), statusFaturasPorCliente);
                int totalUnicos = 0;
                int registrosSalvos = 0;
                final int registrosExtraidos = resultadoFaturasPorCliente.getRegistrosExtraidos();
                if (!faturasPorClienteDTO.isEmpty()) {
                    final List<FaturaPorClienteEntity> faturasPorClienteEntities = faturasPorClienteDTO.stream()
                        .map(faturasPorClienteMapper::toEntity)
                        .collect(Collectors.toList());
                    final List<FaturaPorClienteEntity> faturasPorClienteUnicas =
                        deduplicarFaturasPorCliente(faturasPorClienteEntities);
                    totalUnicos = faturasPorClienteUnicas.size();
                    if (faturasPorClienteEntities.size() != faturasPorClienteUnicas.size()) {
                        final int duplicadosRemovidos = faturasPorClienteEntities.size() - faturasPorClienteUnicas.size();
                        log.warn("⚠️ Removidos {} duplicados da resposta da API antes de salvar", duplicadosRemovidos);
                    }
                    registrosSalvos = faturasPorClienteRepository.salvar(faturasPorClienteUnicas);
                    log.info("✓ Processadas: {}/{} faturas por cliente (INSERTs + UPDATEs)", registrosSalvos, totalUnicos);
                }
                final LogExtracaoEntity.StatusExtracao statusFinal =
                    resultadoFaturasPorCliente.isCompleto() ? LogExtracaoEntity.StatusExtracao.COMPLETO : LogExtracaoEntity.StatusExtracao.INCOMPLETO_LIMITE;
                final String mensagem = resultadoFaturasPorCliente.isCompleto() ?
                    ("Extração completa – extraídos " + registrosExtraidos + 
                     " (únicos: " + totalUnicos + "), processados " + registrosSalvos + " (INSERTs + UPDATEs)") :
                    ("Extração incompleta (" + resultadoFaturasPorCliente.getMotivoInterrupcao() + 
                     ") – extraídos " + registrosExtraidos + 
                     " (únicos: " + totalUnicos + "), processados " + registrosSalvos + " (INSERTs + UPDATEs)");
                final LogExtracaoEntity logFaturasPorCliente = new LogExtracaoEntity(
                    ConstantesEntidades.FATURAS_POR_CLIENTE,
                    inicioFaturasPorCliente,
                    LocalDateTime.now(),
                    statusFinal,
                    totalUnicos,
                    resultadoFaturasPorCliente.getPaginasProcessadas(),
                    mensagem
                );
                logExtracaoRepository.gravarLogExtracao(logFaturasPorCliente);
            } catch (RuntimeException | java.sql.SQLException e) {
                final LogExtracaoEntity logErro = new LogExtracaoEntity(
                    ConstantesEntidades.FATURAS_POR_CLIENTE,
                    inicioFaturasPorCliente,
                    LocalDateTime.now(),
                    ConstantesEntidades.STATUS_ERRO_API,
                    0,
                    0,
                    "Erro: " + e.getMessage()
                );
                logExtracaoRepository.gravarLogExtracao(logErro);
                throw new RuntimeException("Falha na extração de faturas por cliente", e);
            }
        }
        logExtracaoRepository.criarOuAtualizarViewDimFiliais();
    }
    
    /**
     * Deduplica lista de manifestos removendo registros duplicados da API.
     * Usa chave composta (sequence_code + identificador_unico) para identificar duplicados.
     * Mantém o primeiro registro encontrado e descarta duplicados subsequentes.
     * 
     * @param manifestos Lista de manifestos a deduplicar
     * @return Lista deduplicada de manifestos
     */
    private static List<ManifestoEntity> deduplicarManifestos(final List<ManifestoEntity> manifestos) {
        if (manifestos == null || manifestos.isEmpty()) {
            return manifestos;
        }
        
        return manifestos.stream()
            .collect(Collectors.toMap(
                m -> {
                    // Chave única: sequence_code + "_" + identificador_unico
                    if (m.getSequenceCode() == null) {
                        throw new IllegalStateException("Manifesto com sequence_code NULL não pode ser deduplicado");
                    }
                    if (m.getIdentificadorUnico() == null || m.getIdentificadorUnico().trim().isEmpty()) {
                        throw new IllegalStateException("Manifesto com identificador_unico NULL/vazio não pode ser deduplicado");
                    }
                    return m.getSequenceCode() + "_" + m.getIdentificadorUnico();
                },
                m -> m,
                (primeiro, segundo) -> {
                    // Se houver duplicado, manter o primeiro e logar o segundo
                    logger.warn("⚠️ Duplicado detectado na resposta da API: sequence_code={}, identificador_unico={}", 
                        segundo.getSequenceCode(), segundo.getIdentificadorUnico());
                    return primeiro; // Mantém o primeiro
                }
            ))
            .values()
            .stream()
            .collect(Collectors.toList());
    }
    
    /**
     * Deduplica lista de cotações removendo registros duplicados da API.
     * Usa sequence_code como chave única (PRIMARY KEY da tabela).
     * Mantém o primeiro registro encontrado e descarta duplicados subsequentes.
     * 
     * @param cotacoes Lista de cotações a deduplicar
     * @return Lista deduplicada de cotações
     */
    private static List<CotacaoEntity> deduplicarCotacoes(final List<CotacaoEntity> cotacoes) {
        if (cotacoes == null || cotacoes.isEmpty()) {
            return cotacoes;
        }
        
        return cotacoes.stream()
            .collect(Collectors.toMap(
                c -> {
                    // Chave única: sequence_code
                    if (c.getSequenceCode() == null) {
                        throw new IllegalStateException("Cotação com sequence_code NULL não pode ser deduplicada");
                    }
                    return c.getSequenceCode();
                },
                c -> c,
                (primeiro, segundo) -> {
                    // Se houver duplicado, manter o primeiro e logar o segundo
                    logger.warn("⚠️ Duplicado detectado na resposta da API: sequence_code={}", 
                        segundo.getSequenceCode());
                    return primeiro; // Mantém o primeiro
                }
            ))
            .values()
            .stream()
            .collect(Collectors.toList());
    }
    
    /**
     * Deduplica lista de localizações removendo registros duplicados da API.
     * Usa sequence_number como chave única (PRIMARY KEY da tabela).
     * Mantém o primeiro registro encontrado e descarta duplicados subsequentes.
     * 
     * @param localizacoes Lista de localizações a deduplicar
     * @return Lista deduplicada de localizações
     */
    private static List<LocalizacaoCargaEntity> deduplicarLocalizacoes(final List<LocalizacaoCargaEntity> localizacoes) {
        if (localizacoes == null || localizacoes.isEmpty()) {
            return localizacoes;
        }
        
        return localizacoes.stream()
            .collect(Collectors.toMap(
                l -> {
                    // Chave única: sequence_number
                    if (l.getSequenceNumber() == null) {
                        throw new IllegalStateException("Localização com sequence_number NULL não pode ser deduplicada");
                    }
                    return l.getSequenceNumber();
                },
                l -> l,
                (primeiro, segundo) -> {
                    // Se houver duplicado, manter o primeiro e logar o segundo
                    logger.warn("⚠️ Duplicado detectado na resposta da API: sequence_number={}", 
                        segundo.getSequenceNumber());
                    return primeiro; // Mantém o primeiro
                }
            ))
            .values()
            .stream()
            .collect(Collectors.toList());
    }

    /**
     * Deduplica Faturas a Pagar por sequence_code (chave primária).
     */
    private static List<ContasAPagarDataExportEntity> deduplicarFaturasAPagar(final List<ContasAPagarDataExportEntity> lista) {
        if (lista == null || lista.isEmpty()) {
            return lista;
        }

        return lista.stream()
            .collect(Collectors.toMap(
                e -> {
                    if (e.getSequenceCode() == null) {
                        throw new IllegalStateException("Fatura a pagar com sequence_code NULL não pode ser deduplicada");
                    }
                    return e.getSequenceCode();
                },
                e -> e,
                (primeiro, segundo) -> {
                    logger.warn("⚠️ Duplicado detectado: sequence_code={}", segundo.getSequenceCode());
                    return primeiro;
                }
            ))
            .values()
            .stream()
            .collect(Collectors.toList());
    }

    private static List<FaturaPorClienteEntity> deduplicarFaturasPorCliente(final List<FaturaPorClienteEntity> lista) {
        if (lista == null || lista.isEmpty()) {
            return lista;
        }

        return lista.stream()
            .collect(Collectors.toMap(
                e -> {
                    if (e.getUniqueId() == null || e.getUniqueId().trim().isEmpty()) {
                        throw new IllegalStateException(
                            "Fatura por cliente com unique_id NULL não pode ser deduplicada");
                    }
                    return e.getUniqueId();
                },
                e -> e,
                (primeiro, segundo) -> {
                    logger.warn("⚠️ Duplicado detectado: unique_id={}", segundo.getUniqueId());
                    return primeiro;
                }
            ))
            .values()
            .stream()
            .collect(Collectors.toList());
    }
}
