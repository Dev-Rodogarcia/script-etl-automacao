package br.com.extrator.aplicacao.extracao;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import br.com.extrator.aplicacao.politicas.IdempotencyPolicy;
import br.com.extrator.suporte.console.LoggerConsole;

public final class RecoveryUseCase {
    private static final LoggerConsole log = LoggerConsole.getLogger(RecoveryUseCase.class);

    private final IdempotencyPolicy idempotencyPolicy;
    private final ExtracaoPorIntervaloUseCase extracaoPorIntervaloUseCase;

    public RecoveryUseCase() {
        this(new ExtracaoPorIntervaloUseCase());
    }

    RecoveryUseCase(final ExtracaoPorIntervaloUseCase extracaoPorIntervaloUseCase) {
        this.idempotencyPolicy = new IdempotencyPolicy(
            List.of("data_inicio", "data_fim", "api", "entidade", "modo"),
            Duration.ofDays(365),
            "v2"
        );
        this.extracaoPorIntervaloUseCase = Objects.requireNonNull(
            extracaoPorIntervaloUseCase,
            "extracaoPorIntervaloUseCase nao pode ser null"
        );
    }

    public void executarReplay(
        final LocalDate dataInicio,
        final LocalDate dataFim,
        final String api,
        final String entidade,
        final boolean incluirFaturasGraphQL
    ) throws Exception {
        Objects.requireNonNull(dataInicio, "dataInicio nao pode ser null");
        Objects.requireNonNull(dataFim, "dataFim nao pode ser null");
        if (dataFim.isBefore(dataInicio)) {
            throw new IllegalArgumentException("dataFim nao pode ser anterior a dataInicio");
        }

        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("data_inicio", dataInicio);
        payload.put("data_fim", dataFim);
        payload.put("api", api == null ? "" : api);
        payload.put("entidade", entidade == null ? "" : entidade);
        payload.put("modo", "replay");

        final String idempotencyKey = idempotencyPolicy.gerarChave(payload);
        final boolean dentroJanela = idempotencyPolicy.dentroDaJanela(
            LocalDateTime.of(dataFim, java.time.LocalTime.MAX),
            LocalDateTime.now()
        );

        log.info(
            "Recovery replay iniciado | inicio={} | fim={} | api={} | entidade={} | idempotency_key={} | dentro_janela={}",
            dataInicio,
            dataFim,
            api,
            entidade,
            idempotencyKey,
            dentroJanela
        );

        final ExtracaoPorIntervaloRequest request = new ExtracaoPorIntervaloRequest(
            dataInicio,
            dataFim,
            api,
            entidade,
            incluirFaturasGraphQL,
            false
        );
        extracaoPorIntervaloUseCase.executar(request);
        log.info("Recovery replay concluido com sucesso | idempotency_key={}", idempotencyKey);
    }

    public void executarBackfillHistorico(
        final LocalDate dataInicio,
        final LocalDate dataFim,
        final boolean incluirFaturasGraphQL
    ) throws Exception {
        executarReplay(dataInicio, dataFim, null, null, incluirFaturasGraphQL);
    }
}

