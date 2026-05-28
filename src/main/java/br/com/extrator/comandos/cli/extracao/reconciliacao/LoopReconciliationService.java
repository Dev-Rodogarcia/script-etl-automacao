/* ==[DOC-FILE]===============================================================
Arquivo : src/main/java/br/com/extrator/comandos/extracao/reconciliacao/LoopReconciliationService.java
Classe  : LoopReconciliationService (class)
Pacote  : br.com.extrator.comandos.cli.extracao.reconciliacao
Modulo  : Comando CLI (extracao)
Papel   : Implementa responsabilidade de loop reconciliation service.

Conecta com:
- ExecutarExtracaoPorIntervaloComando (comandos.extracao)
- CarregadorConfig (util.configuracao)

Fluxo geral:
1) Interpreta parametros e escopo de extracao.
2) Dispara runners/extratores conforme alvo.
3) Consolida status final e tratamento de falhas.

Estrutura interna:
Metodos principais:
- LoopReconciliationService(...6 args): realiza operacao relacionada a "loop reconciliation service".
- criarPadrao(...1 args): instancia ou monta estrutura de dados.
- processarPosCiclo(...5 args): realiza operacao relacionada a "processar pos ciclo".
- agendarPendenciasPorFalha(...3 args): realiza operacao relacionada a "agendar pendencias por falha".
- carregarEstado(): realiza operacao relacionada a "carregar estado".
- salvarEstado(...2 args): persiste dados em armazenamento.
- parseData(...1 args): realiza operacao relacionada a "parse data".
- toStringDate(...1 args): realiza operacao relacionada a "to string date".
- maiorData(...2 args): realiza operacao relacionada a "maior data".
- resumirMensagem(...1 args): realiza operacao relacionada a "resumir mensagem".
Atributos-chave:
- logger: logger da classe para diagnostico.
- KEY_LAST_DAILY_SCHEDULED_DATE: campo de estado para "key last daily scheduled date".
- KEY_LAST_SUCCESSFUL_RECONCILIATION_DATE: campo de estado para "key last successful reconciliation date".
- KEY_PENDING_DATES: campo de estado para "key pending dates".
- KEY_LAST_ERROR: campo de estado para "key last error".
- KEY_UPDATED_AT: campo de estado para "key updated at".
- stateFile: campo de estado para "state file".
- clock: campo de estado para "clock".
- ativo: campo de estado para "ativo".
- maxTentativasPorCiclo: campo de estado para "max tentativas por ciclo".
- diasRetroativosFalha: campo de estado para "dias retroativos falha".
- executor: campo de estado para "executor".
[DOC-FILE-END]============================================================== */

package br.com.extrator.comandos.cli.extracao.reconciliacao;

import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.extrator.aplicacao.extracao.ReconciliacaoUseCase;
import br.com.extrator.comandos.cli.extracao.reconciliacao.LoopReconciliationStateStore.ReconciliationState;
import br.com.extrator.comandos.cli.extracao.reconciliacao.LoopReconciliationStateStore.ReconciliationTarget;
import br.com.extrator.suporte.configuracao.ConfigLoop;
import br.com.extrator.suporte.validacao.ConstantesEntidades;

/**
 * Orquestra a reconciliacao automatica do loop daemon.
 *
 * Regras aplicadas:
 * - Agenda reconciliacao diaria para D-1 (uma vez por dia).
 * - Em falha de ciclo, adiciona janelas retroativas somente para escopos identificados.
 * - Reexecuta pendencias com limite de tentativas por ciclo.
 */
public final class LoopReconciliationService {
    private static final Logger logger = LoggerFactory.getLogger(LoopReconciliationService.class);
    private static final String API_GRAPHQL = "graphql";
    private static final String API_DATAEXPORT = "dataexport";
    private static final Pattern RUNNERS_FALHADOS_PATTERN =
        Pattern.compile("(?i)runners falhados:\\s*([^\\n\\r]+)");
    private static final Comparator<ReconciliationTarget> TARGET_COMPARATOR =
        Comparator.comparing(ReconciliationTarget::data)
            .thenComparing(target -> target.api() == null ? "" : target.api())
            .thenComparing(target -> target.entidade() == null ? "" : target.entidade());

    @FunctionalInterface
    public interface ReconciliationExecutor {
        void execute(LocalDate data, String api, String entidade, boolean incluirFaturasGraphQL) throws Exception;
    }

    private final LoopReconciliationStateStore stateStore;
    private final Clock clock;
    private final boolean ativo;
    private final int maxTentativasPorCiclo;
    private final int diasRetroativosFalha;
    private final ReconciliationExecutor executor;

    public LoopReconciliationService(final Path stateFile,
                                     final Clock clock,
                                     final boolean ativo,
                                     final int maxTentativasPorCiclo,
                                     final int diasRetroativosFalha,
                                     final ReconciliationExecutor executor) {
        this.clock = Objects.requireNonNull(clock, "clock nao pode ser null");
        this.stateStore = new LoopReconciliationStateStore(
            Objects.requireNonNull(stateFile, "stateFile nao pode ser null"),
            clock,
            logger
        );
        this.ativo = ativo;
        this.maxTentativasPorCiclo = Math.max(1, maxTentativasPorCiclo);
        this.diasRetroativosFalha = Math.max(0, diasRetroativosFalha);
        this.executor = Objects.requireNonNull(executor, "executor nao pode ser null");
    }

    public static LoopReconciliationService criarPadrao(final Path stateFile) {
        final ReconciliacaoUseCase reconciliacaoUseCase = new ReconciliacaoUseCase();
        return new LoopReconciliationService(
            stateFile,
            Clock.systemDefaultZone(),
            ConfigLoop.isReconciliacaoAtiva(),
            ConfigLoop.obterReconciliacaoMaxPorCiclo(),
            ConfigLoop.obterReconciliacaoDiasRetroativosFalha(),
            (data, api, entidade, incluirFaturasGraphQL) ->
                reconciliacaoUseCase.executar(data, api, entidade, incluirFaturasGraphQL)
        );
    }

    public ReconciliationSummary processarPosCiclo(final LocalDateTime inicioCiclo,
                                                   final LocalDateTime fimCiclo,
                                                   final boolean cicloSucesso,
                                                   final boolean incluirFaturasGraphQL,
                                                   final String detalheFalhaCiclo) {
        if (!ativo) {
            return ReconciliationSummary.inativo();
        }

        final ReconciliationState estado = stateStore.load();
        removerPendenciasNaoEspecificas(estado);
        final LocalDate hoje = LocalDate.now(clock);
        final LocalDate ontem = hoje.minusDays(1);

        boolean agendouDiaria = false;
        boolean adicionouPorFalha = false;
        int tentativas = 0;
        int executadas = 0;
        int falhas = 0;
        final List<String> detalhesFalha = new ArrayList<>();

        if (estado.lastDailyScheduledDate == null || estado.lastDailyScheduledDate.isBefore(ontem)) {
            agendarEscoposParaData(estado, ontem, resolverEscoposDiarios(incluirFaturasGraphQL));
            estado.lastDailyScheduledDate = ontem;
            agendouDiaria = true;
            logger.info("Reconciliacao diaria agendada para {} com escopos explicitos", ontem);
        }

        if (!cicloSucesso) {
            adicionouPorFalha = agendarPendenciasPorFalha(estado, inicioCiclo, fimCiclo, detalheFalhaCiclo);
        }

        final List<ReconciliationTarget> pendenciasOrdenadas = estado.pendingTargets.stream()
            .filter(target -> !target.data().isAfter(hoje))
            .sorted(TARGET_COMPARATOR)
            .toList();

        for (final ReconciliationTarget target : pendenciasOrdenadas) {
            if (tentativas >= maxTentativasPorCiclo) {
                break;
            }
            tentativas++;
            try {
                logger.info(
                    "Iniciando reconciliacao automatica para {} | api={} | entidade={}",
                    target.data(),
                    target.api() == null ? "all" : target.api(),
                    target.entidade() == null ? "all" : target.entidade()
                );
                executor.execute(target.data(), target.api(), target.entidade(), incluirFaturasGraphQL);
                estado.pendingTargets.remove(target);
                estado.lastSuccessfulReconciliationDate = maiorData(estado.lastSuccessfulReconciliationDate, target.data());
                executadas++;
                logger.info(
                    "Reconciliacao concluida para {} | api={} | entidade={}",
                    target.data(),
                    target.api() == null ? "all" : target.api(),
                    target.entidade() == null ? "all" : target.entidade()
                );
            } catch (final Exception e) {
                falhas++;
                final String detalhe = target.token() + ": " + resumirMensagem(e.getMessage());
                detalhesFalha.add(detalhe);
                logger.error("Falha na reconciliacao automatica para {}: {}", target.token(), e.getMessage(), e);
            }
        }

        stateStore.save(estado, detalhesFalha);

        final List<LocalDate> pendenciasRestantes = estado.pendingTargets.stream()
            .map(ReconciliationTarget::data)
            .distinct()
            .sorted()
            .toList();

        return new ReconciliationSummary(
            true,
            executadas,
            falhas,
            pendenciasRestantes,
            detalhesFalha,
            agendouDiaria,
            adicionouPorFalha
        );
    }

    private boolean agendarPendenciasPorFalha(final ReconciliationState estado,
                                              final LocalDateTime inicioCiclo,
                                              final LocalDateTime fimCiclo,
                                              final String detalheFalhaCiclo) {
        LocalDate inicioPendencia = inicioCiclo.toLocalDate().minusDays(diasRetroativosFalha);
        final LocalDate fimPendencia = fimCiclo.toLocalDate();
        if (inicioPendencia.isAfter(fimPendencia)) {
            inicioPendencia = fimPendencia;
        }

        final List<ReconciliationTarget> escopos = resolverEscoposFalha(detalheFalhaCiclo);
        if (escopos.isEmpty()) {
            logger.warn(
                "Falha de ciclo sem entidade identificada. Nenhuma pendencia generica de reconciliacao sera agendada: {}",
                resumirMensagem(detalheFalhaCiclo)
            );
            return false;
        }

        boolean adicionou = false;
        LocalDate atual = inicioPendencia;
        while (!atual.isAfter(fimPendencia)) {
            for (final ReconciliationTarget escopo : escopos) {
                if (adicionarPendencia(estado, new ReconciliationTarget(atual, escopo.api(), escopo.entidade()))) {
                    adicionou = true;
                }
            }
            atual = atual.plusDays(1);
        }

        if (adicionou) {
            logger.warn(
                "Pendencias de reconciliacao adicionadas por falha de ciclo: {} ate {} | escopos={}",
                inicioPendencia,
                fimPendencia,
                escopos.stream().map(escopo -> escopo.api() + "/" + escopo.entidade()).toList()
            );
        }
        return adicionou;
    }

    private List<ReconciliationTarget> resolverEscoposFalha(final String detalheFalhaCiclo) {
        if (detalheFalhaCiclo == null || detalheFalhaCiclo.isBlank()) {
            return List.of();
        }
        final Matcher matcher = RUNNERS_FALHADOS_PATTERN.matcher(detalheFalhaCiclo);
        final String candidatos = matcher.find() ? matcher.group(1) : detalheFalhaCiclo;
        final Set<ReconciliationTarget> escopos = new LinkedHashSet<>();
        for (final String token : candidatos.split(",")) {
            final ReconciliationTarget target = normalizarRunnerFalho(token);
            if (isAlvoEspecifico(target)) {
                escopos.add(target);
            }
        }
        return List.copyOf(escopos);
    }

    private ReconciliationTarget normalizarRunnerFalho(final String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String normalizado = token.trim().toLowerCase(Locale.ROOT);
        if (normalizado.startsWith("aborted:")) {
            normalizado = normalizado.substring("aborted:".length()).trim();
        }

        final int idxSeparador = normalizado.indexOf('/');
        final String runner = idxSeparador >= 0 ? normalizado.substring(0, idxSeparador).trim() : normalizado;
        final String entidadeBruta = limparEntidadeFalha(idxSeparador >= 0 ? normalizado.substring(idxSeparador + 1) : runner);
        final String apiInformada = switch (runner) {
            case API_GRAPHQL -> API_GRAPHQL;
            case API_DATAEXPORT -> API_DATAEXPORT;
            case "faturasgraphql" -> API_GRAPHQL;
            case ConstantesEntidades.RASTER -> ConstantesEntidades.RASTER;
            default -> null;
        };
        final String entidade = normalizarEntidade(entidadeBruta);
        if (entidade == null) {
            return null;
        }
        final String apiInferida = resolverApiEntidade(entidade);
        final String api = apiInformada == null ? apiInferida : apiInformada;
        if (api == null || apiInferida == null || !api.equalsIgnoreCase(apiInferida)) {
            logger.warn(
                "Escopo de falha ignorado por incompatibilidade api/entidade: token={} api={} entidade={}",
                token,
                apiInformada == null ? "n/a" : apiInformada,
                entidade
            );
            return null;
        }
        return new ReconciliationTarget(LocalDate.MIN, api, entidade);
    }

    private List<ReconciliationTarget> resolverEscoposDiarios(final boolean incluirFaturasGraphQL) {
        final List<ReconciliationTarget> escopos = new ArrayList<>();
        escopos.add(alvoDiario(API_GRAPHQL, ConstantesEntidades.USUARIOS_SISTEMA));
        escopos.add(alvoDiario(API_GRAPHQL, ConstantesEntidades.COLETAS));
        escopos.add(alvoDiario(API_GRAPHQL, ConstantesEntidades.FRETES));
        if (incluirFaturasGraphQL) {
            escopos.add(alvoDiario(API_GRAPHQL, ConstantesEntidades.FATURAS_GRAPHQL));
        }
        escopos.add(alvoDiario(API_DATAEXPORT, ConstantesEntidades.MANIFESTOS));
        escopos.add(alvoDiario(API_DATAEXPORT, ConstantesEntidades.COTACOES));
        escopos.add(alvoDiario(API_DATAEXPORT, ConstantesEntidades.LOCALIZACAO_CARGAS));
        escopos.add(alvoDiario(API_DATAEXPORT, ConstantesEntidades.CONTAS_A_PAGAR));
        escopos.add(alvoDiario(API_DATAEXPORT, ConstantesEntidades.FATURAS_POR_CLIENTE));
        escopos.add(alvoDiario(API_DATAEXPORT, ConstantesEntidades.INVENTARIO));
        escopos.add(alvoDiario(API_DATAEXPORT, ConstantesEntidades.SINISTROS));
        return List.copyOf(escopos);
    }

    private ReconciliationTarget alvoDiario(final String api, final String entidade) {
        return new ReconciliationTarget(LocalDate.MIN, api, entidade);
    }

    private boolean agendarEscoposParaData(final ReconciliationState estado,
                                           final LocalDate data,
                                           final List<ReconciliationTarget> escopos) {
        boolean adicionou = false;
        for (final ReconciliationTarget escopo : escopos) {
            if (adicionarPendencia(estado, new ReconciliationTarget(data, escopo.api(), escopo.entidade()))) {
                adicionou = true;
            }
        }
        return adicionou;
    }

    private boolean adicionarPendencia(final ReconciliationState estado, final ReconciliationTarget target) {
        if (!isAlvoEspecifico(target)) {
            logger.warn(
                "Pendencia de reconciliacao ignorada por escopo generico: {}",
                target == null ? "null" : target.token()
            );
            return false;
        }
        return estado.pendingTargets.add(target);
    }

    private void removerPendenciasNaoEspecificas(final ReconciliationState estado) {
        final int totalAntes = estado.pendingTargets.size();
        estado.pendingTargets.removeIf(target -> !isAlvoEspecifico(target));
        final int removidas = totalAntes - estado.pendingTargets.size();
        if (removidas > 0) {
            logger.warn(
                "{} pendencia(s) generica(s) de reconciliacao foram descartadas antes da execucao",
                removidas
            );
        }
    }

    private boolean isAlvoEspecifico(final ReconciliationTarget target) {
        if (target == null || target.data() == null || isEscopoGenerico(target.api()) || isEscopoGenerico(target.entidade())) {
            return false;
        }
        final String apiInferida = resolverApiEntidade(target.entidade());
        return apiInferida != null && apiInferida.equalsIgnoreCase(target.api());
    }

    private boolean isEscopoGenerico(final String valor) {
        if (valor == null || valor.isBlank()) {
            return true;
        }
        final String normalizado = valor.trim().toLowerCase(Locale.ROOT);
        return "*".equals(normalizado) || "all".equals(normalizado) || "todas".equals(normalizado);
    }

    private String resolverApiEntidade(final String entidade) {
        if (entidade == null || entidade.isBlank()) {
            return null;
        }
        return switch (entidade) {
            case ConstantesEntidades.USUARIOS_SISTEMA,
                 ConstantesEntidades.COLETAS,
                 ConstantesEntidades.FRETES,
                 ConstantesEntidades.FATURAS_GRAPHQL -> API_GRAPHQL;
            case ConstantesEntidades.MANIFESTOS,
                 ConstantesEntidades.COTACOES,
                 ConstantesEntidades.LOCALIZACAO_CARGAS,
                 ConstantesEntidades.CONTAS_A_PAGAR,
                 ConstantesEntidades.FATURAS_POR_CLIENTE,
                 ConstantesEntidades.INVENTARIO,
                 ConstantesEntidades.SINISTROS -> API_DATAEXPORT;
            case ConstantesEntidades.RASTER_VIAGENS,
                 ConstantesEntidades.RASTER_VIAGEM_PARADAS -> ConstantesEntidades.RASTER;
            default -> null;
        };
    }

    private String limparEntidadeFalha(final String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        final int idxStatus = valor.indexOf('(');
        return (idxStatus >= 0 ? valor.substring(0, idxStatus) : valor).trim();
    }

    private String normalizarEntidade(final String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return switch (valor.trim().toLowerCase(Locale.ROOT)) {
            case "usuarios_sistema", "usuarios", "usuariossistema" -> ConstantesEntidades.USUARIOS_SISTEMA;
            case "coletas", "coleta" -> ConstantesEntidades.COLETAS;
            case "fretes", "frete" -> ConstantesEntidades.FRETES;
            case "manifestos", "manifesto" -> ConstantesEntidades.MANIFESTOS;
            case "cotacoes", "cotacao" -> ConstantesEntidades.COTACOES;
            case "localizacao_cargas", "localizacao_carga", "localizacao de carga", "localizacao-carga" ->
                ConstantesEntidades.LOCALIZACAO_CARGAS;
            case "contas_a_pagar", "contasapagar", "contas a pagar", "contas-a-pagar" -> ConstantesEntidades.CONTAS_A_PAGAR;
            case "faturas_por_cliente", "faturasporcliente", "faturas por cliente", "faturas-por-cliente" ->
                ConstantesEntidades.FATURAS_POR_CLIENTE;
            case "inventario", "inventário" -> ConstantesEntidades.INVENTARIO;
            case "sinistros", "sinistro" -> ConstantesEntidades.SINISTROS;
            case "faturas_graphql", "faturasgraphql", "faturas" -> ConstantesEntidades.FATURAS_GRAPHQL;
            case "raster", "raster_viagens", "viagens_raster", "raster_viagem_paradas", "paradas_raster" ->
                ConstantesEntidades.RASTER_VIAGENS;
            default -> null;
        };
    }

    private LocalDate maiorData(final LocalDate atual, final LocalDate candidato) {
        if (atual == null) {
            return candidato;
        }
        return candidato.isAfter(atual) ? candidato : atual;
    }

    private String resumirMensagem(final String mensagem) {
        if (mensagem == null || mensagem.isBlank()) {
            return "sem detalhes";
        }
        final String limpa = mensagem.replace('\n', ' ').replace('\r', ' ').trim();
        return limpa.length() > 180 ? limpa.substring(0, 180) + "..." : limpa;
    }

    public static final class ReconciliationSummary {
        private final boolean ativo;
        private final int reconciliacoesExecutadas;
        private final int falhas;
        private final List<LocalDate> pendenciasRestantes;
        private final List<String> detalhesFalha;
        private final boolean agendouReconciliacaoDiaria;
        private final boolean pendenciaPorFalha;

        private ReconciliationSummary(final boolean ativo,
                                      final int reconciliacoesExecutadas,
                                      final int falhas,
                                      final List<LocalDate> pendenciasRestantes,
                                      final List<String> detalhesFalha,
                                      final boolean agendouReconciliacaoDiaria,
                                      final boolean pendenciaPorFalha) {
            this.ativo = ativo;
            this.reconciliacoesExecutadas = reconciliacoesExecutadas;
            this.falhas = falhas;
            this.pendenciasRestantes = List.copyOf(pendenciasRestantes);
            this.detalhesFalha = List.copyOf(detalhesFalha);
            this.agendouReconciliacaoDiaria = agendouReconciliacaoDiaria;
            this.pendenciaPorFalha = pendenciaPorFalha;
        }

        private static ReconciliationSummary inativo() {
            return new ReconciliationSummary(false, 0, 0, List.of(), List.of(), false, false);
        }

        public static ReconciliationSummary semAcao() {
            return new ReconciliationSummary(true, 0, 0, List.of(), List.of(), false, false);
        }

        public boolean isAtivo() {
            return ativo;
        }

        public int getReconciliacoesExecutadas() {
            return reconciliacoesExecutadas;
        }

        public int getFalhas() {
            return falhas;
        }

        public List<LocalDate> getPendenciasRestantes() {
            return pendenciasRestantes;
        }

        public List<String> getDetalhesFalha() {
            return detalhesFalha;
        }

        public boolean isAgendouReconciliacaoDiaria() {
            return agendouReconciliacaoDiaria;
        }

        public boolean isPendenciaPorFalha() {
            return pendenciaPorFalha;
        }
    }
}
