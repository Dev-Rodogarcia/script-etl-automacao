/* ==[DOC-FILE]===============================================================
Arquivo : src/main/java/br/com/extrator/aplicacao/extracao/ExtracaoPorIntervaloRequest.java
Classe  : ExtracaoPorIntervaloRequest (record)
Pacote  : br.com.extrator.aplicacao.extracao
Modulo  : Use Case - Extracao

Papel   : Requisicao para execucao de extracao em um intervalo de datas especifico.

Conecta com:
- ExtracaoPorIntervaloUseCase (consume)

Fluxo geral:
1) Cliente monta request com datas inicio/fim e filtros opcionais (API, entidade).
2) Compact constructor valida e normaliza campos (null-check, trim).
3) Use case recebe request e a decompoe em blocos de extracao.

Estrutura interna:
Campos:
- dataInicio, dataFim: LocalDate (obrigatorios, validados).
- apiEspecifica: String (opcional, normalizado para null se blank).
- entidadeEspecifica: String (opcional, normalizado para null se blank).
- incluirFaturasGraphQL, modoLoopDaemon: boolean (flags de comportamento).
- modoExecucao: classifica a carga para evitar acoplamento entre daemon, micro-batch e reconciliacao.
[DOC-FILE-END]============================================================== */
package br.com.extrator.aplicacao.extracao;

import java.time.LocalDate;
import java.util.Objects;

public record ExtracaoPorIntervaloRequest(
    LocalDate dataInicio,
    LocalDate dataFim,
    String apiEspecifica,
    String entidadeEspecifica,
    boolean incluirFaturasGraphQL,
    boolean modoLoopDaemon,
    boolean modoRapido24h,
    ModoExecucao modoExecucao
) {
    public ExtracaoPorIntervaloRequest(
        final LocalDate dataInicio,
        final LocalDate dataFim,
        final String apiEspecifica,
        final String entidadeEspecifica,
        final boolean incluirFaturasGraphQL,
        final boolean modoLoopDaemon
    ) {
        this(dataInicio, dataFim, apiEspecifica, entidadeEspecifica, incluirFaturasGraphQL, modoLoopDaemon, false);
    }

    public ExtracaoPorIntervaloRequest(
        final LocalDate dataInicio,
        final LocalDate dataFim,
        final String apiEspecifica,
        final String entidadeEspecifica,
        final boolean incluirFaturasGraphQL,
        final boolean modoLoopDaemon,
        final boolean modoRapido24h
    ) {
        this(
            dataInicio,
            dataFim,
            apiEspecifica,
            entidadeEspecifica,
            incluirFaturasGraphQL,
            modoLoopDaemon,
            modoRapido24h,
            ModoExecucao.padrao(modoLoopDaemon)
        );
    }

    public ExtracaoPorIntervaloRequest {
        Objects.requireNonNull(dataInicio, "dataInicio nao pode ser null");
        Objects.requireNonNull(dataFim, "dataFim nao pode ser null");
        apiEspecifica = normalizar(apiEspecifica);
        entidadeEspecifica = normalizar(entidadeEspecifica);
        modoExecucao = modoExecucao == null ? ModoExecucao.padrao(modoLoopDaemon) : modoExecucao;
    }

    private static String normalizar(final String valor) {
        if (valor == null) {
            return null;
        }
        final String limpo = valor.trim();
        return limpo.isEmpty() ? null : limpo;
    }

    public enum ModoExecucao {
        INTERVALO("intervalo"),
        MICRO_BATCH("micro_batch"),
        RECONCILIACAO("reconciliacao"),
        BACKFILL("backfill");

        private final String modoLookbackFretes;

        ModoExecucao(final String modoLookbackFretes) {
            this.modoLookbackFretes = modoLookbackFretes;
        }

        static ModoExecucao padrao(final boolean modoLoopDaemon) {
            return modoLoopDaemon ? MICRO_BATCH : INTERVALO;
        }

        public String modoLookbackFretes() {
            return modoLookbackFretes;
        }
    }
}
