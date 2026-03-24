---
context:
  - ETL
  - Resiliencia
  - Operacao
updated_at: 2026-03-18T00:00:32.1501533-03:00
source_of_truth: code
classification: atual
related_files:
  - src/main/java/br/com/extrator/aplicacao/pipeline/PipelineOrchestrator.java
  - src/main/java/br/com/extrator/suporte/http/GerenciadorRequisicaoHttp.java
  - src/main/java/br/com/extrator/comandos/cli/extracao/daemon/LoopDaemonRunHandler.java
  - src/main/java/br/com/extrator/aplicacao/validacao/ValidacaoEtlResilienciaUseCase.java
---

# Resiliencia

## Objetivo

O sistema foi desenhado para continuar util mesmo quando a origem, a rede ou uma etapa interna degradam. Resiliencia aqui nao significa "ignorar erro"; significa:

- detectar rapido;
- conter o dano;
- continuar quando isso for seguro;
- abortar quando continuar seria enganoso.

## Mecanismos principais

### Retry HTTP

- Aplicado pelo `GerenciadorRequisicaoHttp`.
- Trata timeout, `429`, `5xx` e IO transiente.

### Retry de pipeline

- Aplicado pelo `PipelineOrchestrator`.
- Usa `ExponentialBackoffRetryPolicy`.

### Circuit breaker

- Existe no gerenciador HTTP e no orquestrador do pipeline.
- Evita insistencia cega em origem ou step que esta falhando em cascata.

### Watchdog

- `OperationTimeoutGuard` mata operacoes que passam do tempo.
- O daemon tambem tem timeout global por ciclo.

### Isolamento por processo

- GraphQL e DataExport podem rodar em processo filho.
- Serve para conter travas que nao obedecem interrupcao.

### Reconciliacao automatica

- O loop daemon agenda reprocessamento de dias pendentes.
- Ajuda a recuperar lacunas sem intervencao manual imediata.

### Validacao de resiliencia

- `--validar-etl-resiliencia` executa cenarios de chaos e stress.

## Filosofia de falha

O sistema diferencia falhas em duas categorias praticas:

### Falhas que exigem parar

- step com `ABORT_PIPELINE`;
- erro que impediria confiar no resultado;
- excecoes irrecuperaveis;
- timeout global critico do ciclo.

### Falhas que permitem seguir com alerta

- step com `CONTINUE_WITH_ALERT`;
- degradacao explicitamente tolerada;
- divergencia de validacao final em modo loop daemon.

## Exemplo real

Se `graphql` falha, o comportamento nao e automaticamente "morrer". O orquestrador consulta:

1. taxonomia do erro;
2. failure policy da entidade;
3. estado do circuit breaker.

So depois disso decide se:

- continua;
- degrada;
- repete;
- aborta.

## Evolucao

Os documentos antigos falavam de falha muito mais como um problema de endpoint ou script. O runtime moderno trata falha como uma dimensao arquitetural transversal.

Isso aparece em:

- configuracao;
- pipeline;
- daemon;
- validacoes de chaos;
- protecoes de pagina e de volume;
- fallback de historico.
