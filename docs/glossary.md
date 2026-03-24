---
context:
  - ETL
  - Glossario
  - IA
updated_at: 2026-03-18T00:00:32.1501533-03:00
source_of_truth: mixed
classification: atual
related_files:
  - src/main/java/br/com/extrator/suporte/validacao/ConstantesEntidades.java
  - src/main/java/br/com/extrator/aplicacao/pipeline/PipelineOrchestrator.java
  - src/main/java/br/com/extrator/comandos/cli/extracao/daemon/LoopDaemonRunHandler.java
---

# Glossario global

## Termos centrais

**D-1..D**

- Janela operacional diaria usada pelo fluxo completo.
- Nao significa "ultimas 24h corridas".
- Em geral significa `data_fim = hoje` e `data_inicio = hoje - 1 dia`.

**ETL**

- Extract, Transform, Load.
- Neste projeto significa extrair da ESL Cloud, normalizar/mapear e persistir no SQL Server.

**Source of truth**

- Fonte oficial da verdade.
- Neste repositorio, o codigo atual tem prioridade sobre qualquer documento historico.

**Pipeline**

- Sequencia controlada de steps de extracao e validacao.
- No fluxo principal, executa GraphQL e DataExport em paralelo, depois `faturas_graphql`, depois data quality.

**Step**

- Unidade de execucao do pipeline.
- Retorna `StepExecutionResult` com status, duracao, metadata e taxonomia de erro.

**GraphQL**

- API principal para `usuarios_sistema`, `coletas`, `fretes` e `faturas_graphql`.
- Endpoint operacional confirmado: `/graphql`.

**DataExport**

- API baseada em templates/tabular export.
- Fonte de `manifestos`, `cotacoes`, `localizacao_cargas`, `contas_a_pagar` e `faturas_por_cliente`.

**REST**

- Material historico e auxiliar presente no repositorio.
- Nao e o caminho moderno oficial do ETL principal.

**Faturas GraphQL**

- Entidade `faturas_graphql`.
- Executada separadamente porque envolve enriquecimento pesado, backfill por ID e custo operacional maior.

**Pre-backfill referencial de coletas**

- Extracao auxiliar de `coletas` antes do fluxo principal.
- Existe para reduzir `manifestos` orfaos sem contaminar a janela principal auditada.

**Pos-hidratacao referencial**

- Extracao auxiliar adicional de `coletas` depois do fluxo principal.
- Pode executar janela retroativa e lookahead para fechar referencias faltantes.

**Recovery**

- Replay/backfill idempotente sobre um intervalo.
- Reusa o fluxo de extracao por intervalo.

**Reconciliacao**

- Reprocessamento automatico usado pelo loop daemon.
- Agenda `D-1` diariamente e pendencias adicionais quando um ciclo falha.

**Loop daemon**

- Processo em segundo plano que executa ciclos continuos do fluxo completo.
- Mantem estado, historico, logs por ciclo e reconciliacao automatica.

**Watchdog**

- Protecao temporal para impedir travamentos indefinidos.
- No projeto aparece em `OperationTimeoutGuard` e no timeout global do ciclo do daemon.

**Isolamento por processo**

- Modo em que GraphQL e DataExport rodam em processos filhos separados.
- Serve para conter travamentos, futures presos e threads que nao obedecem interrupcao.

**Retry**

- Nova tentativa automatica depois de falha transiente.
- Existe em nivel de API, em nivel de step do pipeline e em cenarios especificos de pagina/timeouts.

**Circuit breaker**

- Mecanismo que interrompe chamadas repetidas a uma origem ou step apos falhas consecutivas.
- No pipeline moderno ele existe em nivel de entidade.

**Failure policy**

- Regra que define como o pipeline reage a falha:
- `ABORT_PIPELINE`
- `CONTINUE_WITH_ALERT`
- `DEGRADE`
- `RETRY`

**Data quality**

- Bateria de checks sobre dados persistidos.
- Hoje cobre unicidade, completude, freshness, integridade referencial e schema.

**Completude**

- Comparacao entre o que a execucao extraiu e o que chegou ao banco.
- Usa logs e contagens da execucao como referencia principal.

**Integridade ETL**

- Validacao mais estrita da carga.
- Verifica schema, logs, contagem, chaves nulas, duplicidade e referencias entre entidades.

**Execution context**

- Contexto propagado via MDC.
- Carrega `execution_id` e comando para logs correlacionados.

**Structured log**

- Log JSON emitido pelo pipeline.
- Util para correlacao por evento, step, entidade e modo de falha.

**Execution history**

- Historico resumido da execucao em `sys_execution_history`.
- Quando falha a persistencia, existe fallback em arquivo NDJSON.

## Entidades modernas

**usuarios_sistema**

- Dimensao auxiliar extraida via GraphQL.
- Necessaria antes de `coletas` em alguns cenarios.

**coletas**

- Entidade GraphQL com papel central na integridade referencial de `manifestos`.

**fretes**

- Entidade GraphQL relacionada a `faturas_graphql` via `accounting_credit_id`.

**manifestos**

- Entidade DataExport relacionada a `coletas` via `pick_sequence_code`.

**cotacoes**

- Entidade DataExport de apoio comercial.

**localizacao_cargas**

- Entidade DataExport ligada a rastreio/localizacao operacional.

**contas_a_pagar**

- Entidade DataExport financeira.

**faturas_por_cliente**

- Entidade DataExport financeira que recebe enriquecimento via ponte com `faturas_graphql`.
