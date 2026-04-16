package br.com.extrator.integracao;

/* ==[DOC-FILE]===============================================================
Arquivo : src/main/java/br/com/extrator/integracao/ClienteApiDataExport.java
Classe  : ClienteApiDataExport (class)
Pacote  : br.com.extrator.integracao
Modulo  : Integracao HTTP
Papel   : [DESC PENDENTE]
Conecta com: Sem dependencia interna
Fluxo geral:
1) [PENDENTE]
Estrutura interna:
Metodos: [PENDENTE]
Atributos: [PENDENTE]
[DOC-FILE-END]============================================================== */


import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;

import br.com.extrator.integracao.constantes.ConstantesApiDataExport;
import br.com.extrator.integracao.constantes.ConstantesApiDataExport.ConfiguracaoEntidade;
import br.com.extrator.persistencia.repositorio.PageAuditRepository;
import br.com.extrator.dominio.dataexport.contasapagar.ContasAPagarDTO;
import br.com.extrator.dominio.dataexport.cotacao.CotacaoDTO;
import br.com.extrator.dominio.dataexport.fretes.FreteIndicadorDTO;
import br.com.extrator.dominio.dataexport.inventario.InventarioDTO;
import br.com.extrator.dominio.dataexport.localizacaocarga.LocalizacaoCargaDTO;
import br.com.extrator.dominio.dataexport.manifestos.ManifestoDTO;
import br.com.extrator.dominio.dataexport.sinistros.SinistroDTO;
import br.com.extrator.suporte.configuracao.ConfigApi;
import br.com.extrator.suporte.formatacao.FormatadorData;
import br.com.extrator.suporte.http.GerenciadorRequisicaoHttp;
import br.com.extrator.suporte.tempo.RelogioSistema;
import br.com.extrator.suporte.validacao.ConstantesEntidades;

public class ClienteApiDataExport {
    private static final Logger logger = LoggerFactory.getLogger(ClienteApiDataExport.class);
    private static final int INTERVALO_LOG_PROGRESSO = 10;
    private static final int MAX_FALHAS_CONSECUTIVAS = 5;
    private static final Duration JANELA_REABERTURA_CIRCUITO = Duration.ofMinutes(10);

    private final String urlBase;
    private final Duration timeoutRequisicao;
    private final DataExportRetryConfigFactory retryConfigFactory;
    private final DataExportAdaptiveRetrySupport adaptiveRetrySupport;
    private final DataExportCsvCountSupport csvCountSupport;
    private final DataExportPaginationSupport paginationSupport;
    private final DataExportPaginator paginator;
    private final DataExportTimeWindowSupport timeWindowSupport;
    private String executionUuid;

    protected ClienteApiDataExport(final boolean testMode) {
        this.urlBase = null;
        this.timeoutRequisicao = Duration.ZERO;
        this.retryConfigFactory = null;
        this.adaptiveRetrySupport = null;
        this.csvCountSupport = null;
        this.paginationSupport = null;
        this.paginator = null;
        this.timeWindowSupport = null;
    }

    public ClienteApiDataExport() {
        logger.info("Inicializando cliente da API Data Export");

        final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
        this.urlBase = ConfigApi.obterUrlBaseApi();
        final String token = ConfigApi.obterTokenApiDataExport();
        this.timeoutRequisicao = ConfigApi.obterTimeoutApiRest();
        final String metodoDataExportEfetivo = ConfigApi.obterMetodoHttpDataExportPreferencial();
        final String modoGetDataExportEfetivo = "corpo";
        final int maxTentativasTimeoutPorPagina = ConfigApi.obterMaxTentativasTimeoutApiDataExportPorPagina();
        final int maxTentativasTimeoutPaginaUm = ConfigApi.obterMaxTentativasTimeoutApiDataExportPaginaUm();
        final long delayBaseTimeoutPorPaginaMs = ConfigApi.obterDelayBaseTimeoutApiDataExportPorPaginaMs();
        final long delayMaximoTimeoutPorPaginaMs = ConfigApi.obterDelayMaximoTimeoutApiDataExportPorPaginaMs();
        final double jitterTimeoutPorPagina = ConfigApi.obterJitterTimeoutApiDataExportPorPagina();

        this.retryConfigFactory = new DataExportRetryConfigFactory();
        this.adaptiveRetrySupport = new DataExportAdaptiveRetrySupport(logger);

        if (urlBase == null || urlBase.trim().isEmpty()) {
            throw new IllegalStateException("URL base da API não configurada");
        }
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalStateException("Token da API Data Export não configurado");
        }

        final GerenciadorRequisicaoHttp gerenciadorRequisicao = GerenciadorRequisicaoHttp.getInstance();
        final DataExportRequestFactory requestFactory = new DataExportRequestFactory(token);
        this.timeWindowSupport = DataExportTimeWindowSupport.createConfigured();
        final DataExportRequestBodyFactory requestBodyFactory = new DataExportRequestBodyFactory(logger, timeWindowSupport);
        final DataExportPageAuditLogger pageAuditLogger = new DataExportPageAuditLogger(new PageAuditRepository());
        final DataExportHttpExecutor httpExecutor = new DataExportHttpExecutor(
            logger,
            httpClient,
            gerenciadorRequisicao,
            requestFactory,
            metodoDataExportEfetivo,
            modoGetDataExportEfetivo,
            delayBaseTimeoutPorPaginaMs,
            delayMaximoTimeoutPorPaginaMs,
            jitterTimeoutPorPagina
        );
        this.paginationSupport = new DataExportPaginationSupport(
            logger,
            MAX_FALHAS_CONSECUTIVAS,
            JANELA_REABERTURA_CIRCUITO,
            new java.util.HashMap<>(),
            new java.util.HashSet<>(),
            new java.util.HashMap<>()
        );
        this.csvCountSupport = new DataExportCsvCountSupport(
            logger,
            this.urlBase,
            this.timeoutRequisicao,
            requestBodyFactory,
            this.timeWindowSupport,
            httpExecutor::executarRequisicaoDataExportCsv,
            paginationSupport::isCircuitBreakerAtivo,
            paginationSupport::resetarEstadoFalhasTemplate,
            paginationSupport::incrementarContadorFalhas
        );
        this.paginator = new DataExportPaginator(
            logger,
            this.urlBase,
            requestBodyFactory,
            pageAuditLogger,
            httpExecutor,
            maxTentativasTimeoutPorPagina,
            maxTentativasTimeoutPaginaUm,
            INTERVALO_LOG_PROGRESSO,
            this.paginationSupport,
            this.timeWindowSupport,
            (templateId, nomeTabela, campoData, dataInicio, dataFim, tipoAmigavel) ->
                this.csvCountSupport.obterContagemGenericaCsv(templateId, nomeTabela, campoData, dataInicio, dataFim, tipoAmigavel)
        );

        logger.info("Cliente da API Data Export inicializado com sucesso");
        logger.debug("URL base configurada: {}", urlBase);
    }

    public void setExecutionUuid(final String uuid) {
        this.executionUuid = uuid;
    }

    public ResultadoExtracao<ManifestoDTO> buscarManifestos() {
        final LocalDate hoje = RelogioSistema.hoje();
        return buscarManifestos(hoje.minusDays(1), hoje);
    }

    public ResultadoExtracao<CotacaoDTO> buscarCotacoes() {
        final LocalDate hoje = RelogioSistema.hoje();
        return buscarCotacoes(hoje.minusDays(1), hoje);
    }

    public ResultadoExtracao<LocalizacaoCargaDTO> buscarLocalizacaoCarga() {
        final LocalDate hoje = RelogioSistema.hoje();
        return buscarLocalizacaoCarga(hoje.minusDays(1), hoje);
    }

    public ResultadoExtracao<ContasAPagarDTO> buscarContasAPagar() {
        final LocalDate hoje = RelogioSistema.hoje();
        return buscarContasAPagar(hoje.minusDays(1), hoje);
    }

    public ResultadoExtracao<br.com.extrator.dominio.dataexport.faturaporcliente.FaturaPorClienteDTO> buscarFaturasPorCliente() {
        final LocalDate hoje = RelogioSistema.hoje();
        return buscarFaturasPorCliente(hoje.minusDays(1), hoje);
    }

    public ResultadoExtracao<InventarioDTO> buscarInventario() {
        final LocalDate hoje = RelogioSistema.hoje();
        return buscarInventario(hoje.minusDays(1), hoje);
    }

    public ResultadoExtracao<SinistroDTO> buscarSinistros() {
        final LocalDate hoje = RelogioSistema.hoje();
        return buscarSinistros(hoje.minusDays(1), hoje);
    }

    public ResultadoExtracao<ManifestoDTO> buscarManifestos(final LocalDate dataInicio, final LocalDate dataFim) {
        logger.info("Buscando manifestos da API DataExport - Período: {} a {}", dataInicio, dataFim);
        final ConfiguracaoEntidade config = ConstantesApiDataExport.obterConfiguracao(ConstantesEntidades.MANIFESTOS);
        final String chaveTemplate = "Template-" + config.templateId();
        paginationSupport.resetarEstadoFalhasTemplate(chaveTemplate);
        final Instant inicio = timeWindowSupport.inicioDoDia(dataInicio);
        final Instant fim = timeWindowSupport.fimDoDia(dataFim);
        final List<ConfiguracaoEntidade> tentativas = retryConfigFactory.criarTentativasManifestos(config);

        return adaptiveRetrySupport.executar(
            "Manifestos",
            chaveTemplate,
            tentativas,
            configTentativa -> paginator.buscarDadosGenericos(
                this.executionUuid,
                configTentativa.templateId(),
                configTentativa.tabelaApi(),
                configTentativa.campoData(),
                new TypeReference<List<ManifestoDTO>>() {},
                inicio,
                fim,
                configTentativa
            ),
            paginationSupport::deveRetentarResultadoIncompleto,
            paginationSupport::selecionarMelhorResultadoParcial,
            paginationSupport::ehErroTimeoutOu422,
            paginationSupport::resetarEstadoFalhasTemplate,
            paginationSupport::resetarEstadoFalhasTemplate,
            paginationSupport::resetarEstadoFalhasTemplate
        );
    }

    public ResultadoExtracao<CotacaoDTO> buscarCotacoes(final LocalDate dataInicio, final LocalDate dataFim) {
        logger.info("Buscando cotações da API DataExport - Período: {} a {}", dataInicio, dataFim);
        final ConfiguracaoEntidade config = ConstantesApiDataExport.obterConfiguracao(ConstantesEntidades.COTACOES);
        return buscarDadosDiretos(
            dataInicio,
            dataFim,
            config,
            new TypeReference<List<CotacaoDTO>>() {}
        );
    }

    public ResultadoExtracao<LocalizacaoCargaDTO> buscarLocalizacaoCarga(final LocalDate dataInicio, final LocalDate dataFim) {
        logger.info("Buscando localização de carga da API DataExport - Período: {} a {}", dataInicio, dataFim);
        final ConfiguracaoEntidade config = ConstantesApiDataExport.obterConfiguracao(ConstantesEntidades.LOCALIZACAO_CARGAS);
        return buscarDadosDiretos(
            dataInicio,
            dataFim,
            config,
            new TypeReference<List<LocalizacaoCargaDTO>>() {}
        );
    }

    public ResultadoExtracao<ContasAPagarDTO> buscarContasAPagar(final LocalDate dataInicio, final LocalDate dataFim) {
        logger.info("Buscando Faturas a Pagar da API DataExport - Período: {} a {}", dataInicio, dataFim);
        final ConfiguracaoEntidade config = ConstantesApiDataExport.obterConfiguracao(ConstantesEntidades.CONTAS_A_PAGAR);
        final List<ConfiguracaoEntidade> tentativas = retryConfigFactory.criarTentativasContasAPagar(config);
        final Instant inicioBusiness = timeWindowSupport.inicioDoDia(dataInicio);
        final Instant fimBusiness = timeWindowSupport.fimDoDia(dataFim);
        final List<ResultadoExtracao<ContasAPagarDTO>> resultadosSegmentados = new ArrayList<>();

        for (final LocalDate diaSegmento : listarSegmentosDiarios(dataInicio, dataFim)) {
            final Instant inicioSegmento = timeWindowSupport.inicioDoDia(diaSegmento);
            final Instant fimSegmento = timeWindowSupport.fimDoDia(diaSegmento);
            final Map<String, String> filtrosExtras = Map.of(
                "created_at",
                timeWindowSupport.formatarRange(inicioSegmento, fimSegmento)
            );

            final ResultadoExtracao<ContasAPagarDTO> resultadoSegmento = adaptiveRetrySupport.executar(
                "Contas a Pagar",
                "Template-" + config.templateId() + "-created-at-" + diaSegmento,
                tentativas,
                configTentativa -> paginator.buscarDadosGenericos(
                    this.executionUuid,
                    configTentativa.templateId(),
                    configTentativa.tabelaApi(),
                    configTentativa.campoData(),
                    new TypeReference<List<ContasAPagarDTO>>() {},
                    inicioBusiness,
                    fimBusiness,
                    configTentativa,
                    false,
                    filtrosExtras
                ),
                paginationSupport::deveRetentarResultadoIncompleto,
                paginationSupport::selecionarMelhorResultadoParcial,
                paginationSupport::ehErroTimeoutOu422,
                null,
                null,
                null
            );
            resultadosSegmentados.add(resultadoSegmento);
        }

        return consolidarResultadosContasAPagar(resultadosSegmentados);
    }

    public ResultadoExtracao<br.com.extrator.dominio.dataexport.faturaporcliente.FaturaPorClienteDTO> buscarFaturasPorCliente(final LocalDate dataInicio, final LocalDate dataFim) {
        logger.info("Buscando Faturas por Cliente da API DataExport - Período: {} a {}", dataInicio, dataFim);
        final ConfiguracaoEntidade config = ConstantesApiDataExport.obterConfiguracao(ConstantesEntidades.FATURAS_POR_CLIENTE);
        return buscarDadosDiretos(
            dataInicio,
            dataFim,
            config,
            new TypeReference<List<br.com.extrator.dominio.dataexport.faturaporcliente.FaturaPorClienteDTO>>() {}
        );
    }

    public ResultadoExtracao<InventarioDTO> buscarInventario(final LocalDate dataInicio, final LocalDate dataFim) {
        logger.info("Buscando inventario da API DataExport - Período: {} a {}", dataInicio, dataFim);
        final ConfiguracaoEntidade config = ConstantesApiDataExport.obterConfiguracao(ConstantesEntidades.INVENTARIO);
        return buscarDadosDiretos(
            dataInicio,
            dataFim,
            config,
            new TypeReference<List<InventarioDTO>>() {}
        );
    }

    public ResultadoExtracao<SinistroDTO> buscarSinistros(final LocalDate dataInicio, final LocalDate dataFim) {
        logger.info("Buscando sinistros da API DataExport - Período: {} a {}", dataInicio, dataFim);
        final ConfiguracaoEntidade config = ConstantesApiDataExport.obterConfiguracao(ConstantesEntidades.SINISTROS);
        return buscarDadosDiretos(
            dataInicio,
            dataFim,
            config,
            new TypeReference<List<SinistroDTO>>() {}
        );
    }

    public ResultadoExtracao<FreteIndicadorDTO> buscarFretesIndicadores(final LocalDate dataInicio,
                                                                         final LocalDate dataFim) {
        logger.info("Buscando fretes indicadores da API DataExport - Período: {} a {}", dataInicio, dataFim);
        final ConfiguracaoEntidade config = new ConfiguracaoEntidade(
            6389,
            "service_at",
            "freights",
            "1000",
            Duration.ofSeconds(120),
            "corporation_sequence_number asc",
            false
        );
        return buscarDadosDiretos(
            dataInicio,
            dataFim,
            config,
            new TypeReference<List<FreteIndicadorDTO>>() {}
        );
    }

    public int obterContagemManifestos(final LocalDate dataReferencia) {
        final ConfiguracaoEntidade config = ConstantesApiDataExport.obterConfiguracao(ConstantesEntidades.MANIFESTOS);
        try {
            return obterContagemGenericaCsv(config.templateId(), config.tabelaApi(), config.campoData(), dataReferencia, "manifestos");
        } catch (final RuntimeException e) {
            if (!paginationSupport.ehErroTimeoutOu422(e)) {
                throw e;
            }
            logger.warn("Contagem CSV de manifestos falhou por timeout/422. Aplicando fallback para extracao paginada. erro={}", e.getMessage());
            final ResultadoExtracao<ManifestoDTO> resultado = buscarManifestos(dataReferencia, dataReferencia);
            final int total = resultado.getDados() == null ? 0 : resultado.getDados().size();
            logger.info("Contagem de manifestos via fallback paginado: {} registros (completo={}, motivo={})", total, resultado.isCompleto(), resultado.getMotivoInterrupcao());
            return total;
        }
    }

    public int obterContagemCotacoes(final LocalDate dataReferencia) {
        final ConfiguracaoEntidade config = ConstantesApiDataExport.obterConfiguracao(ConstantesEntidades.COTACOES);
        return obterContagemGenericaCsv(config.templateId(), config.tabelaApi(), config.campoData(), dataReferencia, "cotações");
    }

    public int obterContagemLocalizacoesCarga(final LocalDate dataReferencia) {
        final ConfiguracaoEntidade config = ConstantesApiDataExport.obterConfiguracao(ConstantesEntidades.LOCALIZACAO_CARGAS);
        return obterContagemGenericaCsv(config.templateId(), config.tabelaApi(), config.campoData(), dataReferencia, "localizações de carga");
    }

    public int obterContagemContasAPagar(final LocalDate dataReferencia) {
        final ConfiguracaoEntidade config = ConstantesApiDataExport.obterConfiguracao(ConstantesEntidades.CONTAS_A_PAGAR);
        return obterContagemGenericaCsv(config.templateId(), config.tabelaApi(), config.campoData(), dataReferencia, "faturas a pagar");
    }

    public int obterContagemFaturasPorCliente(final LocalDate dataReferencia) {
        final ConfiguracaoEntidade config = ConstantesApiDataExport.obterConfiguracao(ConstantesEntidades.FATURAS_POR_CLIENTE);
        return obterContagemGenericaCsv(config.templateId(), config.tabelaApi(), config.campoData(), dataReferencia, "faturas por cliente");
    }

    public int obterContagemInventario(final LocalDate dataReferencia) {
        final ConfiguracaoEntidade config = ConstantesApiDataExport.obterConfiguracao(ConstantesEntidades.INVENTARIO);
        return obterContagemGenericaCsv(config.templateId(), config.tabelaApi(), config.campoData(), dataReferencia, "inventario");
    }

    public int obterContagemSinistros(final LocalDate dataReferencia) {
        final ConfiguracaoEntidade config = ConstantesApiDataExport.obterConfiguracao(ConstantesEntidades.SINISTROS);
        return obterContagemGenericaCsv(config.templateId(), config.tabelaApi(), config.campoData(), dataReferencia, "sinistros");
    }

    static ResultadoExtracao<ContasAPagarDTO> consolidarResultadosContasAPagar(
        final List<ResultadoExtracao<ContasAPagarDTO>> resultadosSegmentados
    ) {
        if (resultadosSegmentados == null || resultadosSegmentados.isEmpty()) {
            return ResultadoExtracao.completo(List.of(), 0, 0);
        }

        final List<ContasAPagarDTO> bruto = new ArrayList<>();
        int paginasProcessadas = 0;
        int registrosExtraidos = 0;
        boolean completo = true;
        String motivoInterrupcao = null;

        for (final ResultadoExtracao<ContasAPagarDTO> resultado : resultadosSegmentados) {
            if (resultado == null) {
                continue;
            }
            bruto.addAll(resultado.getDados());
            paginasProcessadas += resultado.getPaginasProcessadas();
            registrosExtraidos += resultado.getRegistrosExtraidos();
            if (!resultado.isCompleto()) {
                completo = false;
                motivoInterrupcao = selecionarMotivoInterrupcao(motivoInterrupcao, resultado.getMotivoInterrupcao());
            }
        }

        final List<ContasAPagarDTO> deduplicado = consolidarContasAPagarPorSequenceCode(bruto);
        return completo
            ? ResultadoExtracao.completo(deduplicado, paginasProcessadas, registrosExtraidos)
            : ResultadoExtracao.incompleto(
                deduplicado,
                motivoInterrupcao != null ? motivoInterrupcao : ResultadoExtracao.MotivoInterrupcao.LIMITE_PAGINAS.getCodigo(),
                paginasProcessadas,
                registrosExtraidos
            );
    }

    static List<ContasAPagarDTO> consolidarContasAPagarPorSequenceCode(final List<ContasAPagarDTO> registros) {
        if (registros == null || registros.isEmpty()) {
            return List.of();
        }

        final Map<String, ContasAPagarDTO> unicos = new LinkedHashMap<>();
        int sequencialSemChave = 0;
        for (final ContasAPagarDTO registro : registros) {
            if (registro == null) {
                continue;
            }
            final String sequenceCode = registro.getSequenceCode();
            if (sequenceCode == null || sequenceCode.isBlank()) {
                unicos.put("__SEM_SEQUENCE_CODE__:" + sequencialSemChave++, registro);
                continue;
            }
            unicos.merge(sequenceCode.trim(), registro, ClienteApiDataExport::preferirContaMaisFresca);
        }
        return List.copyOf(unicos.values());
    }

    static ContasAPagarDTO preferirContaMaisFresca(final ContasAPagarDTO atual,
                                                   final ContasAPagarDTO candidata) {
        final int comparacao = compararFrescorConta(candidata, atual);
        if (comparacao > 0) {
            return candidata;
        }
        if (comparacao < 0) {
            return atual;
        }
        return fingerprintConta(atual).compareTo(fingerprintConta(candidata)) >= 0 ? atual : candidata;
    }

    static int compararFrescorConta(final ContasAPagarDTO esquerda, final ContasAPagarDTO direita) {
        final int createdAt = compararDataHora(parseDataHora(esquerda == null ? null : esquerda.getCreatedAt()),
            parseDataHora(direita == null ? null : direita.getCreatedAt()));
        if (createdAt != 0) {
            return createdAt;
        }
        final int transactionDate = compararDataHora(parseData(esquerda == null ? null : esquerda.getTransactionDate()),
            parseData(direita == null ? null : direita.getTransactionDate()));
        if (transactionDate != 0) {
            return transactionDate;
        }
        final int liquidationDate = compararDataHora(parseData(esquerda == null ? null : esquerda.getLiquidationDate()),
            parseData(direita == null ? null : direita.getLiquidationDate()));
        if (liquidationDate != 0) {
            return liquidationDate;
        }
        return compararDataHora(parseData(esquerda == null ? null : esquerda.getIssueDate()),
            parseData(direita == null ? null : direita.getIssueDate()));
    }

    static List<LocalDate> listarSegmentosDiarios(final LocalDate dataInicio, final LocalDate dataFim) {
        if (dataInicio == null || dataFim == null || dataFim.isBefore(dataInicio)) {
            return List.of();
        }
        final List<LocalDate> segmentos = new ArrayList<>();
        LocalDate cursor = dataInicio;
        while (!cursor.isAfter(dataFim)) {
            segmentos.add(cursor);
            cursor = cursor.plusDays(1);
        }
        return List.copyOf(segmentos);
    }

    private <T> ResultadoExtracao<T> buscarDadosDiretos(final LocalDate dataInicio,
                                                        final LocalDate dataFim,
                                                        final ConfiguracaoEntidade config,
                                                        final TypeReference<List<T>> typeReference) {
        final Instant inicio = timeWindowSupport.inicioDoDia(dataInicio);
        final Instant fim = timeWindowSupport.fimDoDia(dataFim);
        return paginator.buscarDadosGenericos(
            this.executionUuid,
            config.templateId(),
            config.tabelaApi(),
            config.campoData(),
            typeReference,
            inicio,
            fim,
            config
        );
    }

    private int obterContagemGenericaCsv(final int templateId,
                                         final String nomeTabela,
                                         final String campoData,
                                         final LocalDate dataReferencia,
                                         final String tipoAmigavel) {
        return csvCountSupport.obterContagemGenericaCsv(templateId, nomeTabela, campoData, dataReferencia, tipoAmigavel);
    }

    private static OffsetDateTime parseDataHora(final String valor) {
        return FormatadorData.parseOffsetDateTime(valor);
    }

    private static OffsetDateTime parseData(final String valor) {
        return FormatadorData.parseOffsetDateTime(valor);
    }

    private static int compararDataHora(final OffsetDateTime esquerda, final OffsetDateTime direita) {
        if (esquerda == null && direita == null) {
            return 0;
        }
        if (esquerda == null) {
            return -1;
        }
        if (direita == null) {
            return 1;
        }
        return esquerda.compareTo(direita);
    }

    private static String selecionarMotivoInterrupcao(final String atual, final String candidato) {
        if (candidato == null || candidato.isBlank()) {
            return atual;
        }
        if (ResultadoExtracao.MotivoInterrupcao.ERRO_API.getCodigo().equals(candidato)
            || ResultadoExtracao.MotivoInterrupcao.CIRCUIT_BREAKER.getCodigo().equals(candidato)
            || ResultadoExtracao.MotivoInterrupcao.LACUNA_PAGINACAO_422.getCodigo().equals(candidato)
            || ResultadoExtracao.MotivoInterrupcao.PAGINA_VAZIA_INESPERADA.getCodigo().equals(candidato)) {
            return candidato;
        }
        return (atual == null || atual.isBlank()) ? candidato : atual;
    }

    private static String fingerprintConta(final ContasAPagarDTO registro) {
        if (registro == null) {
            return "";
        }
        final Map<String, String> ordenado = new TreeMap<>();
        registro.getAllProperties().forEach((chave, valor) -> ordenado.put(chave, Objects.toString(valor, "")));
        return ordenado.toString();
    }
}
