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
5. define janela `D-1..D`
6. executa pre-backfill de coletas quando aplicavel
7. executa pipeline principal
8. executa pos-hidratacao de coletas quando aplicavel
9. valida completude
10. valida integridade ETL
11. considera data quality
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

- essa e a janela principal diaria;
- ela nao substitui janelas auxiliares de hidratacao referencial.

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

- completude por logs fecha;
- integridade ETL fecha;
- data quality aprova.

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
completude = validar_completude()
integridade = validar_integridade()
quality = validar_data_quality()
status = consolidar(report, completude, integridade, quality)
persistir_historico(status)
release lock
```

## Onde depurar cada fase

- entrada e historico: `Main`
- janela e regras executivas: `FluxoCompletoUseCase`
- wiring: `PipelineCompositionRoot`
- step runtime: `PipelineOrchestrator`
- coletas auxiliares: `PreBackfillReferencialColetasUseCase`
- validacao: `CompletudeValidator` e `IntegridadeEtlValidator`
