---
context:
  - ETL
  - Fluxo
  - CicloCompleto
updated_at: 2026-03-18T00:00:32.1501533-03:00
source_of_truth: code
classification: atual
related_files:
  - src/main/java/br/com/extrator/bootstrap/Main.java
  - src/main/java/br/com/extrator/aplicacao/extracao/FluxoCompletoUseCase.java
  - src/main/java/br/com/extrator/bootstrap/pipeline/PipelineCompositionRoot.java
  - src/main/java/br/com/extrator/aplicacao/pipeline/PipelineOrchestrator.java
---

# Fluxo completo

## Visao ponta a ponta

O ciclo completo moderno segue esta sequencia:

1. `Main` resolve comando e inicializa `ExecutionContext`
2. `CommandRegistry` entrega `ExecutarFluxoCompletoComando`
3. `PipelineCompositionRoot` registra o contexto do pipeline
4. `FluxoCompletoUseCase` adquire lock global
5. planeja janelas por entidade com `ExecutionWindowPlanner`
6. executa pre-backfill de coletas quando aplicavel
7. executa pipeline principal
8. executa pos-hidratacao de coletas quando aplicavel
9. executa validacao autorizativa do run
10. considera data quality
11. atualiza `watermark` confirmado no sucesso pleno
12. grava historico e, se for sucesso pleno, grava `last_run.properties`

## Fluxo detalhado

### Passo 1. Entrada

Sem argumentos, o comando padrao e `--fluxo-completo`.

### Passo 2. Contexto

`Main` inicializa:

- `execution_id`
- tipo de comando
- captura opcional de log
- persistencia de historico de execucao

### Passo 3. Exclusao mutua

`SqlServerExecutionLockManager` tenta adquirir `etl-global-execution`.

Sem esse lock, a execucao falha antes de mover dados.

### Passo 4. Janela

`dataFim = hoje`

`dataInicio = hoje - 1 dia`

Observacao:

- essa e a base do ciclo;
- entidades criticas passam por planejamento de `consulta` e `confirmacao`;
- o fluxo deixa explicita a separacao entre overlap de consulta e watermark confirmado.

### Passo 5. Pre-backfill

Antes do pipeline oficial, o sistema pode buscar coletas retroativas para reduzir manifestos orfaos.

### Passo 6. Pipeline principal

```text
graphql || dataexport
-> faturas_graphql (opcional)
-> quality
```

### Passo 7. Pos-hidratacao

Se nao houve falha nos runners e o modo nao e loop daemon:

- tenta janela retroativa adicional;
- tenta lookahead de coletas.

### Passo 8. Validacao final

O fluxo so e considerado plenamente valido quando:

- a validacao autorizativa do run fecha;
- existe trilha estruturada por `execution_uuid`;
- data quality aprova.

Observacao:

- a validacao rapida de 24h continua existindo como telemetria;
- ela nao aprova a execucao principal.

### Passo 9. Fechamento

Possibilidades:

- `SUCCESS`
- `PARTIAL`
- `ERROR`
- `SUCCESS_WITH_ALERT` em situacoes especificas do loop daemon

## Pseudocodigo

```text
acquire lock
window = D-1..D
pre_backfill_coletas()
report = orchestrator.executar(steps)
pos_hidratacao_coletas()
integridade = validar_integridade_autorizativa_por_execution_uuid()
quality = validar_data_quality()
status = consolidar(report, integridade, quality)
if success: atualizar_watermarks()
persistir_historico(status)
release lock
```

## Onde depurar cada fase

- entrada e historico: `Main`
- janela e regras executivas: `FluxoCompletoUseCase`
- plano de janela e watermark: `ExecutionWindowPlanner`
- wiring: `PipelineCompositionRoot`
- step runtime: `PipelineOrchestrator`
- coletas auxiliares: `PreBackfillReferencialColetasUseCase`
- validacao autorizativa: `IntegridadeEtlValidator`
