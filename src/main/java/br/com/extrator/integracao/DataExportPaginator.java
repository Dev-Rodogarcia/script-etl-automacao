package br.com.extrator.integracao;

/* ==[DOC-FILE]===============================================================
Arquivo : src/main/java/br/com/extrator/integracao/DataExportPaginator.java
Classe  :  (class)
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


import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

import br.com.extrator.integracao.constantes.ConstantesApiDataExport;
import br.com.extrator.integracao.constantes.ConstantesApiDataExport.ConfiguracaoEntidade;
import br.com.extrator.suporte.ThreadUtil;
import br.com.extrator.suporte.configuracao.ConfigApi;
import br.com.extrator.suporte.mapeamento.MapperUtil;

final class DataExportPaginator {
    private static final int MAX_PAGINAS_TIMEOUT_422_PULADAS = 25;

    private final Logger logger;
    private final String urlBase;
    private final DataExportRequestBodyFactory requestBodyFactory;
    private final DataExportPageAuditLogger pageAuditLogger;
    private final DataExportHttpExecutor httpExecutor;
    private final int maxTentativasTimeoutPorPagina;
    private final int maxTentativasTimeoutPaginaUm;
    private final int intervaloLogProgresso;
    private final DataExportPaginationSupport paginationSupport;
    private final DataExportTimeout422Probe timeout422Probe;

    DataExportPaginator(final Logger logger,
                        final String urlBase,
                        final DataExportRequestBodyFactory requestBodyFactory,
                        final DataExportPageAuditLogger pageAuditLogger,
                        final DataExportHttpExecutor httpExecutor,
                        final int maxTentativasTimeoutPorPagina,
                        final int maxTentativasTimeoutPaginaUm,
                        final int intervaloLogProgresso,
                        final DataExportPaginationSupport paginationSupport) {
        this.logger = logger;
        this.urlBase = urlBase;
        this.requestBodyFactory = requestBodyFactory;
        this.pageAuditLogger = pageAuditLogger;
        this.httpExecutor = httpExecutor;
        this.maxTentativasTimeoutPorPagina = maxTentativasTimeoutPorPagina;
        this.maxTentativasTimeoutPaginaUm = maxTentativasTimeoutPaginaUm;
        this.intervaloLogProgresso = intervaloLogProgresso;
        this.paginationSupport = paginationSupport;
        this.timeout422Probe = new DataExportTimeout422Probe(logger, requestBodyFactory, httpExecutor);
    }

    <T> ResultadoExtracao<T> buscarDadosGenericos(final String executionUuid,
                                                  final int templateId,
                                                  final String nomeTabela,
                                                  final String campoData,
                                                  final TypeReference<List<T>> typeReference,
                                                  final Instant dataInicio,
                                                  final Instant dataFim,
                                                  final ConfiguracaoEntidade config) {
        return buscarDadosGenericos(
            executionUuid,
            templateId,
            nomeTabela,
            campoData,
            typeReference,
            dataInicio,
            dataFim,
            config,
            true
        );
    }

    <T> ResultadoExtracao<T> buscarDadosGenericos(final String executionUuid,
                                                  final int templateId,
                                                  final String nomeTabela,
                                                  final String campoData,
                                                  final TypeReference<List<T>> typeReference,
                                                  final Instant dataInicio,
                                                  final Instant dataFim,
                                                  final ConfiguracaoEntidade config,
                                                  final boolean permitirParticionamento) {
        final String tipoAmigavel = obterNomeAmigavelTipo(nomeTabela);
        final String chaveTemplate = "Template-" + templateId;
        final String executionId = (executionUuid == null || executionUuid.isBlank())
            ? java.util.UUID.randomUUID().toString()
            : executionUuid;
        final String runUuid = java.util.UUID.randomUUID().toString();

        if (paginationSupport.isCircuitBreakerAtivo(chaveTemplate)) {
            logger.warn(
                "âš ï¸ CIRCUIT BREAKER ATIVO - Template {} ({}) temporariamente desabilitado devido a falhas consecutivas",
                templateId,
                tipoAmigavel
            );
            return ResultadoExtracao.incompleto(
                new ArrayList<>(),
                ResultadoExtracao.MotivoInterrupcao.CIRCUIT_BREAKER,
                0,
                0
            );
        }

        final String valorPer = config.valorPer();
        final Duration timeout = config.timeout();
        final int perInt;
        try {
            perInt = Integer.parseInt(valorPer);
        } catch (final NumberFormatException e) {
            return ResultadoExtracao.incompleto(
                new ArrayList<>(),
                ResultadoExtracao.MotivoInterrupcao.ERRO_API,
                0,
                0
            );
        }

        final LocalDate janelaInicio = dataInicio.atZone(java.time.ZoneOffset.UTC).toLocalDate();
        final LocalDate janelaFim = dataFim.atZone(java.time.ZoneOffset.UTC).toLocalDate();

        if (permitirParticionamento && ConfigApi.isParticionamentoJanelaDataExportAtivo() && janelaInicio.isBefore(janelaFim)) {
            logger.info(
                "Particionamento automatico DataExport ativo para template {} ({}): {} ate {} (sub-janelas diarias)",
                templateId,
                tipoAmigavel,
                janelaInicio,
                janelaFim
            );

            final List<T> consolidados = new ArrayList<>();
            int paginasConsolidadas = 0;
            String motivoInterrupcaoConsolidado = null;
            boolean completo = true;

            LocalDate dia = janelaInicio;
            while (!dia.isAfter(janelaFim)) {
                final Instant inicioDia = dia.atStartOfDay().atZone(java.time.ZoneOffset.UTC).toInstant();
                final Instant fimDia = dia.atTime(23, 59, 59).atZone(java.time.ZoneOffset.UTC).toInstant();

                final ResultadoExtracao<T> resultadoDia = buscarDadosGenericos(
                    executionId,
                    templateId,
                    nomeTabela,
                    campoData,
                    typeReference,
                    inicioDia,
                    fimDia,
                    config,
                    false
                );
                consolidados.addAll(resultadoDia.getDados());
                paginasConsolidadas += resultadoDia.getPaginasProcessadas();

                if (!resultadoDia.isCompleto()) {
                    completo = false;
                    motivoInterrupcaoConsolidado = selecionarMotivoInterrupcao(
                        motivoInterrupcaoConsolidado,
                        resultadoDia.getMotivoInterrupcao()
                    );
                }
                dia = dia.plusDays(1);
            }

            return completo
                ? ResultadoExtracao.completo(consolidados, paginasConsolidadas, consolidados.size())
                : ResultadoExtracao.incompleto(
                    consolidados,
                    motivoInterrupcaoConsolidado != null
                        ? motivoInterrupcaoConsolidado
                        : ResultadoExtracao.MotivoInterrupcao.LIMITE_PAGINAS.getCodigo(),
                    paginasConsolidadas,
                    consolidados.size()
                );
        }

        logger.info("INICIANDO EXTRACAO: Template {} - {}", templateId, tipoAmigavel);
        logger.info("PerÃ­odo: {} atÃ© {}", janelaInicio, janelaFim);
        logger.info("Valor 'per': {}", valorPer);
        logger.info("Timeout: {} segundos", timeout.getSeconds());

        final List<T> resultadosFinais = new ArrayList<>();
        int paginaAtual = 1;
        int totalPaginas = 0;
        int totalRegistrosProcessados = 0;
        int paginasTimeout422Puladas = 0;
        boolean interrompido = false;
        ResultadoExtracao.MotivoInterrupcao motivoInterrupcao = null;

        final int limitePaginas = ConfigApi.obterLimitePaginasApiDataExportPorTemplate(templateId);
        final int maxRegistros = ConfigApi.obterMaxRegistrosDataExportPorTemplate(templateId);

        try {
            while (true) {
                if (paginaAtual > limitePaginas) {
                    logger.warn(
                        "ðŸš¨ PROTECAO ATIVADA - Template {} ({}): Limite de {} pÃ¡ginas atingido. Interrompendo busca para evitar loop infinito.",
                        templateId,
                        tipoAmigavel,
                        limitePaginas
                    );
                    interrompido = true;
                    motivoInterrupcao = ResultadoExtracao.MotivoInterrupcao.LIMITE_PAGINAS;
                    break;
                }
                if (totalRegistrosProcessados >= maxRegistros) {
                    logger.warn(
                        "ðŸš¨ PROTECAO ATIVADA - Template {} ({}): Limite de {} registros atingido. Interrompendo busca para evitar sobrecarga.",
                        templateId,
                        tipoAmigavel,
                        maxRegistros
                    );
                    interrompido = true;
                    motivoInterrupcao = ResultadoExtracao.MotivoInterrupcao.LIMITE_REGISTROS;
                    break;
                }

                logger.info("â†’ Requisitando pÃ¡gina {}...", paginaAtual);
                final String url = urlBase + ConstantesApiDataExport.formatarEndpoint(templateId);
                final String corpoJson = requestBodyFactory.construirCorpoRequisicao(
                    nomeTabela,
                    campoData,
                    dataInicio,
                    dataFim,
                    paginaAtual,
                    config
                );
                final String reqHash = PayloadHashUtil.sha256Hex(corpoJson);

                HttpResponse<String> resposta = null;
                long duracaoMs = 0L;
                int tentativaTimeoutPagina = 1;
                final int maxTentativasTimeoutPaginaAtual = paginaAtual == 1
                    ? Math.min(maxTentativasTimeoutPorPagina, maxTentativasTimeoutPaginaUm)
                    : maxTentativasTimeoutPorPagina;

                while (tentativaTimeoutPagina <= maxTentativasTimeoutPaginaAtual) {
                    final long tempoInicio = System.currentTimeMillis();
                    resposta = httpExecutor.executarRequisicaoDataExportJson(
                        url,
                        corpoJson,
                        timeout,
                        "DataExport-Template-" + templateId + "-Page-" + paginaAtual
                    );
                    duracaoMs = System.currentTimeMillis() - tempoInicio;

                    if (!httpExecutor.ehRespostaTimeout422(resposta)
                        || tentativaTimeoutPagina == maxTentativasTimeoutPaginaAtual) {
                        break;
                    }

                    final long atrasoRetryTimeoutMs = httpExecutor.calcularAtrasoRetryTimeoutPagina(tentativaTimeoutPagina);
                    logger.warn(
                        "Timeout 422 em {} pÃ¡gina {}. Retentativa {}/{} em {}ms (backoff exponencial+jitter).",
                        tipoAmigavel,
                        paginaAtual,
                        tentativaTimeoutPagina + 1,
                        maxTentativasTimeoutPaginaAtual,
                        atrasoRetryTimeoutMs
                    );
                    try {
                        ThreadUtil.aguardar(atrasoRetryTimeoutMs);
                    } catch (final InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(
                            "Thread interrompida durante retentativa de timeout da pÃ¡gina " + paginaAtual,
                            ex
                        );
                    }
                    tentativaTimeoutPagina++;
                }

                if (resposta == null) {
                    throw new RuntimeException("Resposta nula na paginaÃ§Ã£o - pÃ¡gina " + paginaAtual);
                }

                logger.info("â† Resposta recebida: Status {}, Tempo: {}ms", resposta.statusCode(), duracaoMs);
                final String respHash = PayloadHashUtil.sha256Hex(resposta.body());

                if (resposta.statusCode() != 200) {
                    if (httpExecutor.ehRespostaTimeout422(resposta) && paginaAtual > 1) {
                        final DataExportTimeout422Probe.ResultadoSondaTimeout422 resultadoSonda = timeout422Probe.sondarPaginaTimeout422(
                            url,
                            nomeTabela,
                            campoData,
                            dataInicio,
                            dataFim,
                            paginaAtual,
                            config,
                            timeout,
                            templateId
                        );

                        if (resultadoSonda == DataExportTimeout422Probe.ResultadoSondaTimeout422.PAGINA_VAZIA) {
                            logger.warn(
                                "Timeout 422 na pagina {} de {}. Sonda com per alternativo retornou pagina vazia; assumindo fim da paginacao.",
                                paginaAtual,
                                tipoAmigavel
                            );
                            totalPaginas = paginaAtual - 1;
                            break;
                        }

                        if (paginasTimeout422Puladas < MAX_PAGINAS_TIMEOUT_422_PULADAS) {
                            paginasTimeout422Puladas++;
                            logger.warn(
                                "Timeout 422 na pagina {} de {}. Pulando pagina e continuando ({} de {} pulos permitidos). resultado_sonda={}",
                                paginaAtual,
                                tipoAmigavel,
                                paginasTimeout422Puladas,
                                MAX_PAGINAS_TIMEOUT_422_PULADAS,
                                resultadoSonda
                            );
                            paginaAtual++;
                            continue;
                        }

                        logger.warn(
                            "Interrompendo paginacao de {} no timeout 422 da pagina {} apos atingir limite de pulos. Retornando resultado parcial com {} registros.",
                            tipoAmigavel,
                            paginaAtual,
                            totalRegistrosProcessados
                        );
                        interrompido = true;
                        motivoInterrupcao = ResultadoExtracao.MotivoInterrupcao.ERRO_API;
                        totalPaginas = paginaAtual - 1;
                        break;
                    }
                    throw new RuntimeException("Erro HTTP " + resposta.statusCode() + " na pÃ¡gina " + paginaAtual);
                }

                final List<T> registrosPagina;
                try {
                    final JsonNode raizJson = MapperUtil.sharedJson().readTree(resposta.body());
                    final JsonNode dadosNode = raizJson.has("data") ? raizJson.get("data") : raizJson;
                    final String idKey = ConstantesApiDataExport.obterCampoIdPrimario(config);

                    if (dadosNode != null && dadosNode.isArray()) {
                        if (dadosNode.size() == 0) {
                            pageAuditLogger.registrarPaginaVazia(
                                executionId,
                                runUuid,
                                templateId,
                                paginaAtual,
                                perInt,
                                janelaInicio,
                                janelaFim,
                                reqHash,
                                respHash,
                                idKey,
                                resposta.statusCode(),
                                (int) duracaoMs
                            );
                            logger.info("â–  Fim da paginaÃ§Ã£o (pÃ¡gina vazia)");
                            totalPaginas = paginaAtual - 1;
                            break;
                        }

                        pageAuditLogger.registrarPaginaComDados(
                            executionId,
                            runUuid,
                            templateId,
                            paginaAtual,
                            perInt,
                            janelaInicio,
                            janelaFim,
                            reqHash,
                            respHash,
                            idKey,
                            resposta.statusCode(),
                            (int) duracaoMs,
                            dadosNode
                        );
                        registrosPagina = MapperUtil.sharedJson().convertValue(dadosNode, typeReference);
                    } else {
                        pageAuditLogger.registrarPayloadInvalido(
                            executionId,
                            runUuid,
                            templateId,
                            paginaAtual,
                            perInt,
                            janelaInicio,
                            janelaFim,
                            reqHash,
                            respHash,
                            resposta.statusCode(),
                            (int) duracaoMs
                        );
                        final String tipoPayload = dadosNode == null ? "null" : dadosNode.getNodeType().name();
                        final String amostraPayload = httpExecutor.extrairAmostraPayload(resposta.body(), 400);
                        throw new IllegalStateException(
                            "Payload invÃ¡lido na pagina " + paginaAtual
                                + ": esperado array, recebido " + tipoPayload
                                + " | resp_hash=" + respHash
                                + " | amostra=" + amostraPayload
                        );
                    }
                } catch (final Exception e) {
                    if (e instanceof final IllegalStateException illegalStateException) {
                        throw illegalStateException;
                    }
                    throw new RuntimeException("Erro ao parsear pÃ¡gina " + paginaAtual, e);
                }

                logger.info("âœ“ PÃ¡gina {}: {} registros parseados", paginaAtual, registrosPagina.size());
                resultadosFinais.addAll(registrosPagina);
                totalRegistrosProcessados += registrosPagina.size();
                paginationSupport.resetarEstadoFalhasTemplate(chaveTemplate);

                logger.info("â†‘ Total acumulado: {} registros", totalRegistrosProcessados);
                if (paginaAtual % intervaloLogProgresso == 0) {
                    logger.info("â³ Progresso: {} pÃ¡ginas processadas, {} registros", paginaAtual, totalRegistrosProcessados);
                }
                paginaAtual++;
            }

            paginationSupport.resetarEstadoFalhasTemplate(chaveTemplate);
            if (paginasTimeout422Puladas > 0) {
                logger.warn(
                    "Extracao de {} concluiu com {} pagina(s) timeout 422 pulada(s).",
                    tipoAmigavel,
                    paginasTimeout422Puladas
                );
            }

            return interrompido
                ? ResultadoExtracao.incompleto(
                    resultadosFinais,
                    motivoInterrupcao != null ? motivoInterrupcao : ResultadoExtracao.MotivoInterrupcao.LIMITE_PAGINAS,
                    totalPaginas > 0 ? totalPaginas : (paginaAtual - 1),
                    totalRegistrosProcessados
                )
                : ResultadoExtracao.completo(
                    resultadosFinais,
                    totalPaginas > 0 ? totalPaginas : (paginaAtual - 1),
                    totalRegistrosProcessados
                );
        } catch (final RuntimeException e) {
            logger.error("âŒ ERRO CRÃTICO na extraÃ§Ã£o de {}: {}", tipoAmigavel, e.getMessage(), e);
            paginationSupport.incrementarContadorFalhas(chaveTemplate, tipoAmigavel);
            throw new RuntimeException("Falha na extraÃ§Ã£o de " + tipoAmigavel, e);
        }
    }

    private String obterNomeAmigavelTipo(final String nomeTabela) {
        return switch (nomeTabela) {
            case "manifests" -> "Manifestos";
            case "quotes" -> "CotaÃ§Ãµes";
            case "freights" -> "LocalizaÃ§Ãµes de Carga / Fretes";
            case "accounting_debits" -> "Contas a Pagar";
            default -> "Dados";
        };
    }

    private String selecionarMotivoInterrupcao(final String atual, final String candidato) {
        if (candidato == null || candidato.isBlank()) {
            return atual;
        }
        if (ResultadoExtracao.MotivoInterrupcao.ERRO_API.getCodigo().equals(candidato)
            || ResultadoExtracao.MotivoInterrupcao.CIRCUIT_BREAKER.getCodigo().equals(candidato)) {
            return candidato;
        }
        return (atual == null || atual.isBlank()) ? candidato : atual;
    }

}
