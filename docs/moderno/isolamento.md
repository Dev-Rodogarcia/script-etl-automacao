---
context:
  - ETL
  - Isolamento
  - Processos
updated_at: 2026-03-18T00:00:32.1501533-03:00
source_of_truth: code
classification: atual
related_files:
  - src/main/java/br/com/extrator/bootstrap/pipeline/IsolatedStepProcessExecutor.java
  - src/main/java/br/com/extrator/bootstrap/pipeline/GraphQLGatewayAdapter.java
  - src/main/java/br/com/extrator/bootstrap/pipeline/DataExportGatewayAdapter.java
  - src/main/java/br/com/extrator/comandos/cli/interno/ExecutarStepIsoladoComando.java
---

# Isolamento por processo

## O que e

GraphQL e DataExport podem rodar em processo filho em vez de compartilhar a mesma JVM do comando principal.

## Por que existe

Esse mecanismo foi adicionado para conter falhas que o timeout por thread nao resolve completamente, por exemplo:

- task travada que ignora interrupcao;
- future que nao devolve;
- vazamento de thread;
- dependencia externa que nao libera recursos.

## Como funciona

`GraphQLGatewayAdapter` e `DataExportGatewayAdapter` verificam:

- `etl.process.isolation.enabled`
- `etl.process.isolated.child`

Se o isolamento estiver ativo e o processo atual nao for filho:

1. o adapter chama `IsolatedStepProcessExecutor`;
2. o executor monta uma nova linha de comando Java;
3. o processo filho roda `--executar-step-isolado`;
4. a saida vai para `logs/isolated_steps`;
5. o processo pai acompanha e mata o filho se necessario.

## O que o filho executa

`ExecutarStepIsoladoComando` aceita:

- `graphql`
- `dataexport`

e executa apenas o service daquela API para a entidade/pedaco solicitado.

## Resultado observado pelo pai

O `StepExecutionResult` recebe metadata como:

- `execution_mode = isolated_process`
- `child_pid = <pid>`

## Quando desabilitar

So faz sentido desabilitar isolamento quando:

- existe motivo diagnostico claro;
- o ambiente e controlado;
- o risco de travamento esta aceito conscientemente.

No fluxo moderno, o padrao e manter ligado.
