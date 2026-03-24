---
context:
  - ETL
  - Orchestrator
  - Resiliencia
updated_at: 2026-03-18T00:00:32.1501533-03:00
source_of_truth: code
classification: atual
related_files:
  - src/main/java/br/com/extrator/aplicacao/pipeline/PipelineOrchestrator.java
  - src/main/java/br/com/extrator/aplicacao/politicas/ExponentialBackoffRetryPolicy.java
  - src/main/java/br/com/extrator/aplicacao/politicas/MapFailurePolicy.java
  - src/main/java/br/com/extrator/aplicacao/politicas/CircuitBreaker.java
  - src/main/java/br/com/extrator/aplicacao/politicas/ErrorClassifier.java
---

# Orquestrador do pipeline

## Responsabilidade

`PipelineOrchestrator` e o componente que decide como executar cada step com seguranca operacional. Ele nao conhece detalhes de negocio de cada entidade; ele conhece:

- timeout;
- retry;
- circuit breaker;
- failure policy;
- logs estruturados;
- metricas;
- abortar ou continuar.

## Politicas aplicadas

### Retry policy

- Executa novamente uma operacao quando a politica considera valido.
- No wiring padrao usa backoff exponencial com jitter.

### Failure policy

- Decide o que fazer quando um step falha.
- Pode continuar, degradar, repetir ou abortar.

### Circuit breaker

- Bloqueia temporariamente steps de uma entidade apos falhas consecutivas.

### Error classifier

- Traduz excecoes para taxonomias padronizadas, por exemplo `TIMEOUT` ou `DATA_QUALITY_BREACH`.

## Fluxo interno

```text
executar(step):
  se circuit breaker aberto:
    return SKIPPED

  try:
    return OperationTimeoutGuard(
      RetryPolicy(
        step.executar()
      )
    )
  catch erro:
    taxonomy = ErrorClassifier.classificar(erro)
    modo = FailurePolicy.resolver(entidade, taxonomy)
    aplicar modo
```

## Como cada modo altera o resultado

### `ABORT_PIPELINE`

- O step falha.
- O pipeline para.
- `PipelineReport` marca `aborted=true`.

### `CONTINUE_WITH_ALERT`

- O step falha.
- O pipeline continua.
- O erro entra no report.

### `DEGRADE`

- O step retorna `DEGRADED`.
- O pipeline continua.

### `RETRY`

- O orquestrador tenta mais uma execucao imediata do step.
- Se ainda falhar, registra falha final.

## Paralelismo

No par `graphql` + `dataexport`, o orquestrador:

- cria futures separados;
- aplica timeout de espera derivado do timeout do step;
- cancela o future em timeout;
- converte o problema em falha de step sem congelar o pipeline inteiro.

Isso e importante porque evita que um future preso trave todo o fluxo.

## Logs e metricas

Para cada step, o orquestrador publica eventos como:

- `pipeline.step.success`
- `pipeline.step.failure`
- `pipeline.step.skipped`
- `pipeline.parallel.start`
- `pipeline.parallel.end`

Tambem incrementa metricas em memoria por entidade:

- sucesso;
- falha;
- soma de duracao;
- contagem de duracao.

## Resultado final

`PipelineReport` consolida:

- lista de `StepExecutionResult`;
- flag de abort;
- step que abortou;
- metricas do snapshot;
- periodo da execucao.

Esse report nao substitui as validacoes finais do fluxo completo. Ele e uma camada de execucao; a validacao executiva final acontece depois.
