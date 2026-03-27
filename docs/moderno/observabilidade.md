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
- `sys_execution_audit`
- `sys_execution_watermark`
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
- `etl_cycle_id`

Isso permite correlacionar logs entre:

- comando principal;
- steps paralelos;
- componentes que respeitam MDC.

## Validacao operacional x validacao autorizativa

### Validacao operacional

- atende comandos rapidos e telemetria;
- pode continuar usando sinais auxiliares e leitura de runtime;
- nao autoriza sucesso do ciclo principal.

### Validacao autorizativa

- e a usada no fechamento do `FluxoCompletoUseCase`;
- exige `execution_uuid` ativo;
- consulta `sys_execution_audit`;
- reprova se a API vier parcial ou se a trilha estruturada estiver indisponivel.

## O que olhar primeiro em incidentes

1. status final do comando
2. `sys_execution_history`
3. `sys_execution_audit` do `execution_uuid`
4. `log_extracoes` da janela
5. logs estruturados de step
6. logs do daemon ou de processo isolado

## Sinais fortes

### Saude boa

- `SUCCESS` no historico;
- `COMPLETO` em `sys_execution_audit`;
- `api_completa = true` na trilha estruturada;
- nenhuma falha de integridade;
- nenhum step `FAILED`.

### Saude ruim

- `PARTIAL`, `ERROR` ou `SUCCESS_WITH_ALERT`;
- `INCOMPLETO_*` em logs de entidade;
- ausencia de `execution_uuid` ou de `sys_execution_audit`;
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
- auditoria estruturada por entidade;
- logs por entidade;
- integridade final;
- alertas do daemon.
