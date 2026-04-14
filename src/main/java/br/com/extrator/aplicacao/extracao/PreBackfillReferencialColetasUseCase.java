/* ==[DOC-FILE]===============================================================
Arquivo : src/main/java/br/com/extrator/aplicacao/extracao/PreBackfillReferencialColetasUseCase.java
Classe  : PreBackfillReferencialColetasUseCase (class)
Pacote  : br.com.extrator.aplicacao.extracao
Modulo  : Use Case - Extracao

Papel   : Executa hidratacao referencial de coletas para resolver manifestos orfaos
          sem contaminar a janela principal auditada.

Conecta com:
- ManifestoOrfaoQueryPort (consulta orfaos no banco)
- GraphQLExtractionService (executa extracao auxiliar de coletas)
- ConfigEtl (buffer retroativo e lookahead)

Fluxo geral:
1) executar(dataInicio, dataFim) resolve a janela retroativa e executa backfill auxiliar.
2) executarPosExtracao(dataInicio, dataFim) faz duas tentativas cirurgicas:
   - retroativa: apenas antes do dia principal
   - lookahead: apenas depois do dia principal
3) Em ambos os casos, a execucao auxiliar extrai somente coletas.
[DOC-FILE-END]============================================================== */
package br.com.extrator.aplicacao.extracao;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.extrator.aplicacao.contexto.AplicacaoContexto;
import br.com.extrator.aplicacao.portas.ManifestoOrfaoQueryPort;
import br.com.extrator.integracao.graphql.services.GraphQLExtractionService;
import br.com.extrator.suporte.configuracao.ConfigEtl;

public class PreBackfillReferencialColetasUseCase {

    private static final Logger log = LoggerFactory.getLogger(PreBackfillReferencialColetasUseCase.class);
    private static final String MOTIVO_BACKLOG_CLAMP = "clamp_max_expansao_dias";

    @FunctionalInterface
    interface ColetasReferencialExecutor {
        void executar(LocalDate dataInicio, LocalDate dataFim);
    }

    private final Supplier<ManifestoOrfaoQueryPort> manifestoOrfaoQueryPortSupplier;
    private final ReferentialBackfillBacklogService backlogService;
    private final ColetasReferencialExecutor coletasReferencialExecutor;

    public PreBackfillReferencialColetasUseCase() {
        this(
            AplicacaoContexto::manifestoOrfaoQueryPort,
            new ReferentialBackfillBacklogService(),
            (dataInicio, dataFim) -> new GraphQLExtractionService(false).executarSomenteColetasReferencial(dataInicio, dataFim)
        );
    }

    PreBackfillReferencialColetasUseCase(final Supplier<ManifestoOrfaoQueryPort> manifestoOrfaoQueryPortSupplier,
                                         final ReferentialBackfillBacklogService backlogService,
                                         final ColetasReferencialExecutor coletasReferencialExecutor) {
        this.manifestoOrfaoQueryPortSupplier = Objects.requireNonNull(
            manifestoOrfaoQueryPortSupplier,
            "manifestoOrfaoQueryPortSupplier nao pode ser null"
        );
        this.backlogService = Objects.requireNonNull(backlogService, "backlogService nao pode ser null");
        this.coletasReferencialExecutor = Objects.requireNonNull(
            coletasReferencialExecutor,
            "coletasReferencialExecutor nao pode ser null"
        );
    }

    public void executar(final LocalDate dataInicio, final LocalDate dataFim) {
        final Optional<LocalDate> dataOrfao = buscarDataMaisAntigaManifestoOrfao();
        resolverBacklogAntigo(dataInicio, dataOrfao).ifPresent(backlog -> {
            backlogService.mergePending(backlog.inicio(), backlog.fim(), backlog.reason());
            log.warn(
                "PRE-BACKFILL-BACKLOG | backlog_registrado=true | inicio_backlog={} | fim_backlog={} | motivo={}",
                backlog.inicio(),
                backlog.fim(),
                backlog.reason()
            );
        });
        processarBacklogPendente(dataInicio);
        final LocalDate inicioEfetivo = resolverInicioEfetivo(dataInicio, dataOrfao);
        executarColetasReferencial("PRE-BACKFILL", inicioEfetivo, dataFim);
    }

    public void executarPosExtracao(final LocalDate dataInicio, final LocalDate dataFim) {
        final Optional<LocalDate> dataOrfao = buscarDataMaisAntigaManifestoOrfao();
        if (dataOrfao.isEmpty()) {
            log.info(
                "POS-HIDRATACAO | janela_dinamica=false | inicio_estatico={} | fim_estatico={} | motivo=sem_orfaos_no_banco",
                dataInicio,
                dataFim
            );
            return;
        }

        final LocalDate inicioRetroativo = resolverInicioEfetivo(dataInicio, dataOrfao);
        final LocalDate fimRetroativo = resolverFimRetroativoPosExtracao(dataInicio);
        if (!inicioRetroativo.isAfter(fimRetroativo)) {
            executarColetasReferencial("POS-HIDRATACAO-RETROATIVA", inicioRetroativo, fimRetroativo);
        } else {
            log.info(
                "POS-HIDRATACAO-RETROATIVA | inicio_principal={} | orfao_mais_antigo={} | motivo=sem_janela_fora_do_periodo_principal",
                dataInicio,
                dataOrfao.get()
            );
        }

        final Optional<LocalDate> orfaoRemanescente = buscarDataMaisAntigaManifestoOrfao();
        if (orfaoRemanescente.isEmpty()) {
            log.info(
                "POS-HIDRATACAO | inicio_principal={} | fim_principal={} | motivo=orfaos_resolvidos_sem_lookahead",
                dataInicio,
                dataFim
            );
            return;
        }

        final Optional<LocalDate> fimLookahead = resolverFimLookaheadPosExtracao(dataFim);
        if (fimLookahead.isEmpty()) {
            log.info(
                "POS-HIDRATACAO-LOOKAHEAD | fim_principal={} | motivo=lookahead_desabilitado",
                dataFim
            );
            return;
        }

        final LocalDate inicioLookahead = resolverInicioLookaheadPosExtracao(dataFim);
        executarColetasReferencial("POS-HIDRATACAO-LOOKAHEAD", inicioLookahead, fimLookahead.get());
    }

    private Optional<LocalDate> buscarDataMaisAntigaManifestoOrfao() {
        return manifestoOrfaoQueryPortSupplier.get().buscarDataMaisAntigaManifestoOrfao();
    }

    private LocalDate resolverInicioEfetivo(
        final LocalDate inicioEstatico,
        final Optional<LocalDate> dataOrfao
    ) {
        final int bufferDias = ConfigEtl.obterEtlReferencialColetasBackfillBufferDias();
        final int maxExpansaoDias = ConfigEtl.obterEtlReferencialColetasBackfillMaxExpansaoDias();
        if (dataOrfao.isPresent()) {
            final LocalDate inicioDinamico = dataOrfao.get().minusDays(bufferDias);
            if (inicioDinamico.isBefore(inicioEstatico)) {
                final LocalDate inicioMinimoPermitido = inicioEstatico.minusDays(Math.max(0, maxExpansaoDias));
                if (inicioDinamico.isBefore(inicioMinimoPermitido)) {
                    log.info(
                        "PRE-BACKFILL | janela_dinamica=limitada | orfao_mais_antigo={} | buffer_dias={} | inicio_estatico={} | max_expansao_dias={} | usando inicio_limitado={}",
                        dataOrfao.get(),
                        bufferDias,
                        inicioEstatico,
                        maxExpansaoDias,
                        inicioMinimoPermitido
                    );
                    return inicioMinimoPermitido;
                }
                log.info(
                    "PRE-BACKFILL | janela_dinamica=true | orfao_mais_antigo={} | buffer_dias={} | inicio_estatico={} | usando inicio_dinamico={}",
                    dataOrfao.get(), bufferDias, inicioEstatico, inicioDinamico
                );
                return inicioDinamico;
            }
            log.info(
                "PRE-BACKFILL | janela_dinamica=false | inicio_estatico={} | orfao_mais_antigo={} | buffer_dias={} | motivo=janela_estatica_ja_cobre_buffer",
                inicioEstatico, dataOrfao.get(), bufferDias
            );
            return inicioEstatico;
        }
        log.info(
            "PRE-BACKFILL | janela_dinamica=false | inicio_estatico={} | motivo=sem_orfaos_no_banco",
            inicioEstatico
        );
        return inicioEstatico;
    }

    private Optional<ReferentialBackfillBacklogService.BacklogWindow> resolverBacklogAntigo(
        final LocalDate inicioEstatico,
        final Optional<LocalDate> dataOrfao
    ) {
        if (dataOrfao.isEmpty()) {
            return Optional.empty();
        }

        final int bufferDias = ConfigEtl.obterEtlReferencialColetasBackfillBufferDias();
        final int maxExpansaoDias = ConfigEtl.obterEtlReferencialColetasBackfillMaxExpansaoDias();
        final LocalDate inicioDinamico = dataOrfao.get().minusDays(bufferDias);
        final LocalDate inicioMinimoPermitido = inicioEstatico.minusDays(Math.max(0, maxExpansaoDias));
        if (!inicioDinamico.isBefore(inicioMinimoPermitido)) {
            return Optional.empty();
        }

        final LocalDate fimBacklog = inicioMinimoPermitido.minusDays(1);
        if (fimBacklog.isBefore(inicioDinamico)) {
            return Optional.empty();
        }
        return Optional.of(new ReferentialBackfillBacklogService.BacklogWindow(inicioDinamico, fimBacklog, MOTIVO_BACKLOG_CLAMP));
    }

    private void processarBacklogPendente(final LocalDate inicioEstatico) {
        final Optional<ReferentialBackfillBacklogService.BacklogWindow> backlogPendente = backlogService.loadPending();
        if (backlogPendente.isEmpty()) {
            return;
        }

        final ReferentialBackfillBacklogService.BacklogWindow backlog = backlogPendente.get();
        final LocalDate limiteSuperior = inicioEstatico.minusDays(1);
        if (backlog.inicio().isAfter(limiteSuperior)) {
            log.info(
                "PRE-BACKFILL-BACKLOG | backlog_preservado=true | inicio_backlog={} | fim_backlog={} | motivo=aguardando_janela_segura",
                backlog.inicio(),
                backlog.fim()
            );
            return;
        }

        final int chunkDias = Math.max(1, ConfigEtl.obterEtlReferencialColetasBackfillMaxExpansaoDiasIntervalo());
        LocalDate fimChunk = backlog.inicio().plusDays(chunkDias - 1L);
        if (fimChunk.isAfter(backlog.fim())) {
            fimChunk = backlog.fim();
        }
        if (fimChunk.isAfter(limiteSuperior)) {
            fimChunk = limiteSuperior;
        }

        log.warn(
            "PRE-BACKFILL-BACKLOG | processando_chunk=true | inicio_backlog={} | fim_backlog={} | chunk_inicio={} | chunk_fim={} | chunk_dias={}",
            backlog.inicio(),
            backlog.fim(),
            backlog.inicio(),
            fimChunk,
            chunkDias
        );
        executarColetasReferencial("PRE-BACKFILL-BACKLOG", backlog.inicio(), fimChunk);
        backlogService.markProcessedUntil(fimChunk);
    }

    private LocalDate resolverFimRetroativoPosExtracao(final LocalDate inicioPrincipal) {
        return inicioPrincipal.minusDays(1);
    }

    private LocalDate resolverInicioLookaheadPosExtracao(final LocalDate fimPrincipal) {
        return fimPrincipal.plusDays(1);
    }

    private Optional<LocalDate> resolverFimLookaheadPosExtracao(final LocalDate fimPrincipal) {
        final int lookaheadDias = ConfigEtl.obterEtlReferencialColetasLookaheadDias();
        if (lookaheadDias <= 0) {
            return Optional.empty();
        }

        final LocalDate fimDinamico = fimPrincipal.plusDays(lookaheadDias);
        log.info(
            "POS-HIDRATACAO-LOOKAHEAD | fim_estatico={} | lookahead_dias={} | usando fim_dinamico={}",
            fimPrincipal,
            lookaheadDias,
            fimDinamico
        );
        return Optional.of(fimDinamico);
    }

    private void executarColetasReferencial(
        final String contexto,
        final LocalDate dataInicio,
        final LocalDate dataFim
    ) {
        log.info(
            "{} | periodo={} a {} | estrategia=somente_coletas",
            contexto,
            dataInicio,
            dataFim
        );
        coletasReferencialExecutor.executar(dataInicio, dataFim);
    }
}
