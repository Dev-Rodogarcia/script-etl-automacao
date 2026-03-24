---
context:
  - ETL
  - Timeouts
  - Configuracao
updated_at: 2026-03-18T00:00:32.1501533-03:00
source_of_truth: code
classification: atual
related_files:
  - src/main/java/br/com/extrator/suporte/configuracao/ConfigEtl.java
  - src/main/java/br/com/extrator/suporte/configuracao/ConfigApi.java
  - src/main/resources/config.properties
  - src/main/java/br/com/extrator/suporte/concorrencia/OperationTimeoutGuard.java
---

# Timeouts

## Regra geral

Todo timeout importante do runtime moderno deve ser tratado como configuracao operacional, nao como numero magico embutido em script.

## Timeouts de step do pipeline

Defaults atuais:

- GraphQL: `1_200_000 ms` (20 min)
- DataExport: `1_200_000 ms` (20 min)
- Data quality: `300_000 ms` (5 min)
- `faturas_graphql`: `7_200_000 ms` (2 h)

Chaves:

- `etl.pipeline.timeout.step.graphql.ms`
- `etl.pipeline.timeout.step.dataexport.ms`
- `etl.pipeline.timeout.step.quality.ms`
- `etl.pipeline.timeout.step.faturas_graphql.ms`

## Timeouts por entidade

### GraphQL

- `usuarios_sistema`: `1_200_000 ms`
- `faturas_graphql`: `7_200_000 ms`
- demais entidades GraphQL: `600_000 ms`

Chave base:

- `etl.graphql.timeout.entidade.<entidade>.ms`

### DataExport

- default por entidade: `900_000 ms`

Chave base:

- `etl.dataexport.timeout.entidade.<entidade>.ms`

## Timeout de ciclo do daemon

Defaults:

- com `faturas_graphql`: `14_400_000 ms` (4 h)
- sem `faturas_graphql`: `3_600_000 ms` (1 h)

Chaves:

- `etl.daemon.cycle.timeout.com_faturas.ms`
- `etl.daemon.cycle.timeout.ms`

## Timeouts de suporte ao paralelismo

- grace do aguardo paralelo: `5_000 ms`
- shutdown de executor: `250 ms`

Chaves:

- `etl.pipeline.timeout.parallel.grace.ms`
- `etl.pipeline.shutdown.timeout.ms`

## Timeouts de protecao de processo/thread

- destruicao de processo isolado: `1_500 ms`
- grace para thread leak: `500 ms`

Chaves:

- `etl.process.isolation.destroy_timeout.ms`
- `etl.thread.leak.grace.ms`

## Timeout de lock global

- `etl.execution.lock.timeout.ms = 5_000`

Se esse timeout estourar, o fluxo falha ao adquirir exclusividade operacional.

## Timeout HTTP base

`GerenciadorRequisicaoHttp` usa:

- timeout do request explicito, quando existe;
- senao `ConfigApi.obterTimeoutApiRest()`;
- default configurado: `api.rest.timeout.seconds = 120`.

O nome da propriedade e legado, mas hoje ela ainda alimenta o timeout HTTP generico quando o request nao carrega outro valor.

## Timeouts especificos de DataExport 422

Para paginas com `422 timeout`:

- max tentativas por pagina: `8`
- max tentativas pagina 1: `3`
- delay base: `1_500 ms`
- delay maximo: `45_000 ms`
- jitter: `0.35`

## Como raciocinar

Se um step travou, a ordem de analise correta e:

1. timeout do step;
2. timeout da entidade;
3. timeout do ciclo do daemon;
4. tempo de destruicao do processo isolado;
5. existencia de thread leak apos cancelamento.
