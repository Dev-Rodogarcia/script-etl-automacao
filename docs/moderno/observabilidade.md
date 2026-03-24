---
context:
  - ETL
  - Observabilidade
  - Logging
updated_at: 2026-03-18T00:00:32.1501533-03:00
source_of_truth: code
classification: atual
related_files:
  - src/main/java/br/com/extrator/observabilidade/LoggingService.java
  - src/main/java/br/com/extrator/observabilidade/pipeline/JsonStructuredExtractionLogger.java
  - src/main/java/br/com/extrator/observabilidade/pipeline/InMemoryPipelineMetrics.java
  - src/main/java/br/com/extrator/persistencia/repositorio/LogExtracaoRepository.java
  - src/main/java/br/com/extrator/comandos/cli/extracao/daemon/DaemonPaths.java
---

# Observabilidade

## Fontes de observabilidade

### Logs de console e arquivo

- `LoggingService` pode espelhar `stdout` e `stderr` para arquivos em `logs/`.

### Logs estruturados do pipeline

- `JsonStructuredExtractionLogger` escreve eventos JSON sanitizados.

### Metricas em memoria

- `InMemoryPipelineMetrics` registra sucesso, falha e duracao por entidade.

### Banco

- `log_extracoes`
- `page_audit`
- `sys_execution_history`

### Runtime do daemon

- `logs/daemon/loop_daemon.state`
- `logs/daemon/loop_daemon.pid`
- `logs/daemon/ciclos/*`
- `logs/daemon/history/*`
- `logs/daemon/reconciliacao/*`

## Correlacao de execucao

`ExecutionContext` propaga:

- `etl_execution_id`
- `etl_command`

Isso permite correlacionar logs entre:

- comando principal;
- steps paralelos;
- componentes que respeitam MDC.

## O que olhar primeiro em incidentes

1. status final do comando
2. `sys_execution_history`
3. `log_extracoes` da janela
4. logs estruturados de step
5. logs do daemon ou de processo isolado
6. relatorios de validacao extrema/resiliencia quando existirem

## Sinais fortes

### Saude boa

- `SUCCESS` no historico;
- `COMPLETO` em `log_extracoes`;
- nenhuma falha de integridade;
- nenhum step `FAILED`.

### Saude ruim

- `PARTIAL`, `ERROR` ou `SUCCESS_WITH_ALERT`;
- `INCOMPLETO_*` em logs de entidade;
- divergencia em `INTEGRIDADE_ETL`;
- timeout ou thread leak;
- reconciliacao acumulando pendencias.

## Relatorios especializados

### Resiliencia

- gera JSON e Markdown em `logs/etl_resilience_report_*`

### Bateria extrema

- gera evidencias de API x banco x log x paginacao

### Daemon

- cada ciclo possui log proprio;
- o estado corrente fica em properties legivel.

## Regra pratica

Nunca conclua "o ETL funcionou" apenas porque o processo terminou com exit code `0`. O veredito operacional depende de:

- historico;
- logs por entidade;
- integridade final;
- alertas do daemon.
