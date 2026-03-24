---
context:
  - ETL
  - Retries
  - Resiliencia
updated_at: 2026-03-18T00:00:32.1501533-03:00
source_of_truth: code
classification: atual
related_files:
  - src/main/java/br/com/extrator/suporte/http/GerenciadorRequisicaoHttp.java
  - src/main/java/br/com/extrator/aplicacao/politicas/ExponentialBackoffRetryPolicy.java
  - src/main/java/br/com/extrator/integracao/GraphQLIntervaloHelper.java
  - src/main/java/br/com/extrator/integracao/DataExportAdaptiveRetrySupport.java
---

# Retries

## Niveis de retry no sistema

Existem retries em mais de um nivel. Isso e intencional.

### Nivel HTTP geral

Aplicado por `GerenciadorRequisicaoHttp`.

Defaults:

- max tentativas: `5`
- delay base: `2000 ms`
- multiplicador: `2.0`

Retenta principalmente:

- `429`
- `5xx`
- timeout
- `IOException`

## Retry por dia em GraphQL

`GraphQLIntervaloHelper` pode repetir um dia quando o resultado volta incompleto por:

- `ERRO_API`
- `CIRCUIT_BREAKER`

Default:

- `api.graphql.retry.max_tentativas_dia = 5`

## Retry do pipeline

O `PipelineOrchestrator` usa `ExponentialBackoffRetryPolicy` com defaults:

- `etl.retry.max_tentativas = 3`
- `etl.retry.delay_base_ms = 1000`
- `etl.retry.multiplicador = 2.0`
- `etl.retry.jitter = 0.2`

Esse retry acontece em volta do `step.executar()`.

## Retry orientado a payload para DataExport

`DataExportAdaptiveRetrySupport` nao apenas repete; ele pode tentar uma configuracao mais leve de request quando o resultado parcial ou o erro sugere isso.

Em outras palavras:

- retry em DataExport pode mudar o payload;
- nao e so repetir o mesmo request.

## Retry especifico para timeout 422 em pagina DataExport

O paginador DataExport trata `422 timeout` pagina a pagina com:

- backoff exponencial;
- jitter;
- sonda opcional com `per` menor;
- retorno parcial explicito quando nao e seguro assumir continuidade.

## Retry da failure policy

Se `FailurePolicy` resolver `RETRY`, o orquestrador pode fazer uma nova execucao imediata do step, alem do retry que ja tenha ocorrido no nivel inferior.

## Anti-ambiguidade

Quando um log fala em retry, sempre pergunte:

1. retry HTTP?
2. retry por dia de GraphQL?
3. retry do pipeline?
4. retry adaptativo de DataExport?
5. retry decidido pela failure policy?

Sem esse contexto, a palavra "retry" fica ambigua e inutil para debugging.
