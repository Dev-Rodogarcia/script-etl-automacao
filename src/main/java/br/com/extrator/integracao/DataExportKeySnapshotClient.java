package br.com.extrator.integracao;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import br.com.extrator.aplicacao.expurgo.EntityReconciliationSpec;
import br.com.extrator.aplicacao.expurgo.SourceKeySnapshot;
import br.com.extrator.aplicacao.expurgo.SourceKeySnapshotPort;
import br.com.extrator.integracao.constantes.ConstantesApiDataExport;
import br.com.extrator.integracao.constantes.ConstantesApiDataExport.ConfiguracaoEntidade;
import br.com.extrator.suporte.ThreadUtil;
import br.com.extrator.suporte.configuracao.ConfigApi;
import br.com.extrator.suporte.http.GerenciadorRequisicaoHttp;
import br.com.extrator.suporte.mapeamento.MapperUtil;

public class DataExportKeySnapshotClient implements SourceKeySnapshotPort {
    private static final Logger logger = LoggerFactory.getLogger(DataExportKeySnapshotClient.class);

    private final String urlBase;
    private final DataExportRequestBodyFactory requestBodyFactory;
    private final DataExportHttpExecutor httpExecutor;
    private final DataExportCsvCountSupport csvCountSupport;
    private final DataExportTimeWindowSupport timeWindowSupport;
    private final int maxTentativasTimeoutPorPagina;
    private final int maxTentativasTimeoutPaginaUm;

    public DataExportKeySnapshotClient() {
        final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(30))
            .build();
        this.urlBase = validarTexto(ConfigApi.obterUrlBaseApi(), "URL base da API nao configurada");
        final String token = validarTexto(ConfigApi.obterTokenApiDataExport(), "Token da API DataExport nao configurado");
        this.timeWindowSupport = DataExportTimeWindowSupport.createConfigured();
        this.requestBodyFactory = new DataExportRequestBodyFactory(logger, timeWindowSupport);
        final DataExportRequestFactory requestFactory = new DataExportRequestFactory(token);
        this.httpExecutor = new DataExportHttpExecutor(
            logger,
            httpClient,
            GerenciadorRequisicaoHttp.getInstance(),
            requestFactory,
            ConfigApi.obterMetodoHttpDataExportPreferencial(),
            "corpo",
            ConfigApi.obterDelayBaseTimeoutApiDataExportPorPaginaMs(),
            ConfigApi.obterDelayMaximoTimeoutApiDataExportPorPaginaMs(),
            ConfigApi.obterJitterTimeoutApiDataExportPorPagina()
        );
        this.csvCountSupport = new DataExportCsvCountSupport(
            logger,
            this.urlBase,
            ConfigApi.obterTimeoutApiRest(),
            requestBodyFactory,
            timeWindowSupport,
            httpExecutor::executarRequisicaoDataExportCsv,
            ignored -> false,
            ignored -> {
            },
            (ignored, tipoAmigavel) -> {
            }
        );
        this.maxTentativasTimeoutPorPagina = ConfigApi.obterMaxTentativasTimeoutApiDataExportPorPagina();
        this.maxTentativasTimeoutPaginaUm = ConfigApi.obterMaxTentativasTimeoutApiDataExportPaginaUm();
    }

    @Override
    public SourceKeySnapshot carregarChaves(final EntityReconciliationSpec spec,
                                            final LocalDate dataInicio,
                                            final LocalDate dataFim) {
        if (ConfigApi.isParticionamentoJanelaDataExportAtivo() && dataInicio.isBefore(dataFim)) {
            return carregarChavesParticionadas(spec, dataInicio, dataFim);
        }
        return carregarChavesJanelaUnica(spec, dataInicio, dataFim);
    }

    private SourceKeySnapshot carregarChavesParticionadas(final EntityReconciliationSpec spec,
                                                          final LocalDate dataInicio,
                                                          final LocalDate dataFim) {
        final Set<String> keys = new HashSet<>();
        int pages = 0;
        int rows = 0;
        int skipped = 0;

        LocalDate dia = dataInicio;
        while (!dia.isAfter(dataFim)) {
            final SourceKeySnapshot snapshotDia = carregarChavesJanelaUnica(spec, dia, dia);
            keys.addAll(snapshotDia.keys());
            pages += snapshotDia.pagesProcessed();
            rows += snapshotDia.rowsRead();
            skipped += snapshotDia.skippedRows();
            dia = dia.plusDays(1);
        }

        return new SourceKeySnapshot(keys, true, pages, rows, skipped);
    }

    private SourceKeySnapshot carregarChavesJanelaUnica(final EntityReconciliationSpec spec,
                                                        final LocalDate dataInicio,
                                                        final LocalDate dataFim) {
        final ConfiguracaoEntidade config = spec.dataExportConfig();
        final int per = parsePer(config);
        final int limitePaginas = ConfigApi.obterLimitePaginasApiDataExportPorTemplate(config.templateId());
        final int maxRegistros = ConfigApi.obterMaxRegistrosDataExportPorTemplate(config.templateId());
        final Instant inicio = timeWindowSupport.inicioDoDia(dataInicio);
        final Instant fim = timeWindowSupport.fimDoDia(dataFim);
        final Set<String> keys = new HashSet<>();

        int paginaAtual = 1;
        int pages = 0;
        int rows = 0;
        int skipped = 0;
        Integer tamanhoPaginaAnterior = null;

        while (true) {
            verificarInterrupcao(spec.entityName(), paginaAtual);
            if (paginaAtual > limitePaginas) {
                throw new DataExportSnapshotException("Limite de paginas atingido no snapshot DataExport de "
                    + spec.entityName() + ": " + limitePaginas);
            }
            if (rows >= maxRegistros) {
                throw new DataExportSnapshotException("Limite de registros atingido no snapshot DataExport de "
                    + spec.entityName() + ": " + maxRegistros);
            }

            final JsonNode dataNode = executarPaginaJson(spec, inicio, fim, paginaAtual);
            if (!dataNode.isArray()) {
                throw new DataExportSnapshotException(
                    "Payload DataExport invalido para " + spec.entityName() + " pagina " + paginaAtual
                        + ": esperado array em data"
                );
            }

            if (dataNode.isEmpty()) {
                if (paginaVaziaComprovaFim(spec, dataInicio, dataFim, paginaAtual, tamanhoPaginaAnterior, per, rows)) {
                    break;
                }
                throw new DataExportSnapshotException(
                    "Pagina vazia inesperada no snapshot DataExport de "
                        + spec.entityName()
                        + " pagina "
                        + paginaAtual
                        + ". Expurgo abortado por falta de prova de completude."
                );
            }

            for (final JsonNode row : dataNode) {
                final String key = spec.sourceKeyExtractor().apply(row);
                if (key == null || key.isBlank()) {
                    skipped++;
                    continue;
                }
                keys.add(key.trim());
            }

            rows += dataNode.size();
            pages++;
            tamanhoPaginaAnterior = dataNode.size();
            paginaAtual++;
        }

        logger.info(
            "Snapshot DataExport concluido | entidade={} | periodo={}..{} | chaves={} | linhas={} | paginas={} | ignoradas={}",
            spec.entityName(),
            dataInicio,
            dataFim,
            keys.size(),
            rows,
            pages,
            skipped
        );
        return new SourceKeySnapshot(keys, true, pages, rows, skipped);
    }

    private JsonNode executarPaginaJson(final EntityReconciliationSpec spec,
                                        final Instant inicio,
                                        final Instant fim,
                                        final int paginaAtual) {
        final ConfiguracaoEntidade config = spec.dataExportConfig();
        final String url = urlBase + ConstantesApiDataExport.formatarEndpoint(config.templateId());
        final String corpoJson = requestBodyFactory.construirCorpoRequisicao(
            config.tabelaApi(),
            config.campoData(),
            inicio,
            fim,
            paginaAtual,
            config,
            Map.of()
        );

        HttpResponse<String> resposta = null;
        int tentativa = 1;
        final int maxTentativas = paginaAtual == 1
            ? Math.min(maxTentativasTimeoutPorPagina, maxTentativasTimeoutPaginaUm)
            : maxTentativasTimeoutPorPagina;

        while (tentativa <= maxTentativas) {
            try {
                resposta = httpExecutor.executarRequisicaoDataExportJson(
                    url,
                    corpoJson,
                    config.timeout(),
                    "orphan-reconciliation-" + spec.entityName() + "-page-" + paginaAtual
                );
            } catch (final RuntimeException e) {
                throw new DataExportSnapshotException(
                    "Falha HTTP no snapshot DataExport de "
                        + spec.entityName()
                        + " pagina "
                        + paginaAtual,
                    e
                );
            }
            if (!httpExecutor.ehRespostaTimeout422(resposta) || tentativa == maxTentativas) {
                break;
            }
            aguardarRetryTimeout(tentativa, spec.entityName(), paginaAtual);
            tentativa++;
        }

        if (resposta == null) {
            throw new DataExportSnapshotException("Resposta nula no snapshot DataExport de "
                + spec.entityName() + " pagina " + paginaAtual);
        }
        if (resposta.statusCode() != 200) {
            final String tipoErro = resposta.statusCode() == 429
                ? "Rate limit HTTP 429"
                : "Erro HTTP " + resposta.statusCode();
            throw new DataExportSnapshotException(
                tipoErro
                    + " no snapshot DataExport de "
                    + spec.entityName()
                    + " pagina "
                    + paginaAtual
            );
        }

        try {
            final JsonNode raizJson = MapperUtil.sharedJson().readTree(resposta.body());
            return raizJson.has("data") ? raizJson.get("data") : raizJson;
        } catch (final Exception e) {
            throw new DataExportSnapshotException(
                "Erro ao parsear pagina " + paginaAtual + " do snapshot DataExport de " + spec.entityName(),
                e
            );
        }
    }

    private boolean paginaVaziaComprovaFim(final EntityReconciliationSpec spec,
                                           final LocalDate dataInicio,
                                           final LocalDate dataFim,
                                           final int paginaAtual,
                                           final Integer tamanhoPaginaAnterior,
                                           final int per,
                                           final int rowsRead) {
        if (paginaAtual <= 1) {
            return contagemCsvConfirmaFim(spec, dataInicio, dataFim, rowsRead);
        }
        return ehFimNatural(tamanhoPaginaAnterior, per)
            || contagemCsvConfirmaFim(spec, dataInicio, dataFim, rowsRead);
    }

    private boolean ehFimNatural(final Integer tamanhoPaginaAnterior,
                                 final int per) {
        return tamanhoPaginaAnterior != null && tamanhoPaginaAnterior < per;
    }

    private boolean contagemCsvConfirmaFim(final EntityReconciliationSpec spec,
                                           final LocalDate dataInicio,
                                           final LocalDate dataFim,
                                           final int rowsRead) {
        final ConfiguracaoEntidade config = spec.dataExportConfig();
        final int contagemEsperada = csvCountSupport.obterContagemGenericaCsv(
            config.templateId(),
            config.tabelaApi(),
            config.campoData(),
            dataInicio,
            dataFim,
            spec.entityName()
        );
        return contagemEsperada <= rowsRead;
    }

    private void aguardarRetryTimeout(final int tentativa,
                                      final String entityName,
                                      final int paginaAtual) {
        final long delayMs = httpExecutor.calcularAtrasoRetryTimeoutPagina(tentativa);
        logger.warn(
            "Timeout 422 no snapshot DataExport de {} pagina {}. Retentativa {} em {}ms.",
            entityName,
            paginaAtual,
            tentativa + 1,
            delayMs
        );
        try {
            ThreadUtil.aguardar(delayMs);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DataExportSnapshotException(
                "Thread interrompida durante retry do snapshot DataExport de "
                    + entityName
                    + " pagina "
                    + paginaAtual,
                e
            );
        }
    }

    private int parsePer(final ConfiguracaoEntidade config) {
        try {
            return Integer.parseInt(config.valorPer());
        } catch (final NumberFormatException e) {
            throw new IllegalStateException("Valor per invalido para template " + config.templateId(), e);
        }
    }

    private void verificarInterrupcao(final String entityName, final int paginaAtual) {
        if (!Thread.currentThread().isInterrupted()) {
            return;
        }
        throw new DataExportSnapshotException(
            "Thread interrompida durante snapshot DataExport de " + entityName + " pagina " + paginaAtual
        );
    }

    private static String validarTexto(final String valor, final String mensagem) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException(mensagem);
        }
        return valor.trim();
    }
}
