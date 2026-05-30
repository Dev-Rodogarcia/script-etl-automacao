/* ==[DOC-FILE]===============================================================
Arquivo : src/main/java/br/com/extrator/aplicacao/extracao/ReconciliacaoUseCase.java
Classe  : ReconciliacaoUseCase (class)
Pacote  : br.com.extrator.aplicacao.extracao
Modulo  : Use Case - Extracao

Papel   : Executa reconciliacao (re-extracao) para um dia especifico com mesmo fluxo de intervalo.

Conecta com:
- ExtracaoPorIntervaloUseCase (delegacao via composicao)

Fluxo geral:
1) executar(data, api, entidade) monta ExtracaoPorIntervaloRequest (data = data, modo reconciliacao explicito).
2) Delega a ExtracaoPorIntervaloUseCase.executar() com intervalo de 1 dia.
3) Retorna CompletableFuture<Void> concluido apenas apos o fluxo delegado encerrar.
4) Reusa pipeline, validacoes e integridade do fluxo de intervalo.

Estrutura interna:
Atributos-chave:
- extracaoPorIntervaloUseCase: delegacao para fluxo de intervalo (composicao).
Metodos principais:
- executar(LocalDate, String, String): ponto de entrada, monta request, delega e expoe conclusao observavel.
[DOC-FILE-END]============================================================== */
package br.com.extrator.aplicacao.extracao;

import java.time.LocalDate;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class ReconciliacaoUseCase {
    private final ExtracaoPorIntervaloUseCase extracaoPorIntervaloUseCase;

    public ReconciliacaoUseCase() {
        this(new ExtracaoPorIntervaloUseCase());
    }

    ReconciliacaoUseCase(final ExtracaoPorIntervaloUseCase extracaoPorIntervaloUseCase) {
        this.extracaoPorIntervaloUseCase = Objects.requireNonNull(
            extracaoPorIntervaloUseCase,
            "extracaoPorIntervaloUseCase nao pode ser null"
        );
    }

    public CompletableFuture<Void> executar(final LocalDate data) {
        return executar(data, null, null);
    }

    public CompletableFuture<Void> executar(final LocalDate data,
                                            final String api,
                                            final String entidade) {
        final ExtracaoPorIntervaloRequest request = new ExtracaoPorIntervaloRequest(
            data,
            data,
            api,
            entidade,
            true,
            false,
            ExtracaoPorIntervaloRequest.ModoExecucao.RECONCILIACAO
        );
        try {
            extracaoPorIntervaloUseCase.executar(request);
            return CompletableFuture.completedFuture(null);
        } catch (final Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }
}
