---
context:
  - ETL
  - Arquitetura
  - Java
updated_at: 2026-03-18T00:00:32.1501533-03:00
source_of_truth: code
classification: atual
related_files:
  - src/main/java/br/com/extrator/bootstrap/Main.java
  - src/main/java/br/com/extrator/bootstrap/pipeline/PipelineCompositionRoot.java
  - src/main/java/br/com/extrator/aplicacao/contexto/AplicacaoContexto.java
  - src/main/java/br/com/extrator/comandos/cli/CommandRegistry.java
---

# Arquitetura real do sistema

## Mapa de camadas

```text
Main
  -> CommandRegistry
  -> Comando CLI
  -> Use case
  -> PipelineCompositionRoot
  -> PipelineOrchestrator
  -> Gateways / Services de integracao
  -> Auditoria estruturada / Validacoes
  -> Repositories / SQL Server
```

## Pacotes relevantes

### `bootstrap`

- Ponto de entrada.
- Wiring de contexto.
- Adapters do pipeline.
- Execucao isolada de steps em processo filho.

### `comandos/cli`

- Interface publica do sistema.
- Despacha fluxo completo, intervalo, recovery, validacoes, seguranca e daemon.

### `aplicacao`

- Use cases.
- Orquestracao de pipeline.
- Politicas de falha, retry e circuit breaker.
- Portas de aplicacao.
- Agora tambem concentra a porta `ExecutionAuditPort`.

### `integracao`

- Clientes HTTP.
- Paginadores.
- Extractors por entidade.
- Mapeamento de payload externo para entidades internas.

### `persistencia`

- Repositories SQL Server.
- Entidades persistidas.
- Queries auxiliares de integridade e auditoria.

### `observabilidade`

- Logging estruturado.
- Data quality.
- Validadores de completude e integridade.
- Historicos e relatorios.

### `features`

- Nova trilha incremental por feature.
- Nesta frente ja abriga:
  - `features/coletas/aplicacao`
  - `features/manifestos/aplicacao`
  - `features/fretes/aplicacao`
  - `features/usuarios/aplicacao`
  - `features/usuarios/persistencia/sqlserver`

### `plataforma`

- Nova trilha incremental para componentes transversais.
- Nesta frente ja abriga:
  - `plataforma/auditoria/aplicacao`
  - `plataforma/auditoria/dominio`
  - `plataforma/auditoria/persistencia/sqlserver`

### `suporte`

- Configuracao.
- Banco.
- concorrencia.
- tempo.
- sanitizacao.
- console.

### `seguranca`

- Banco SQLite separado para autenticacao operacional.
- Usuarios, perfis e politicas de senha.

## Composition root real

`PipelineCompositionRoot` registra no `AplicacaoContexto`:

- factory do `PipelineOrchestrator`
- factory dos steps do fluxo completo
- `GraphQLGateway`
- `DataExportGateway`
- `ExecutionAuditPort`
- adaptadores de completude
- adaptadores de integridade ETL
- query de orfaos de manifestos
- query de logs de extracao

Isso significa que o sistema moderno nao depende de IoC externo. O wiring e feito explicitamente no proprio codigo.

## Runtime view

```text
Main
  -> ExecutionContext.initialize()
  -> CommandRegistry.criarMapaComandos()
  -> PipelineCompositionRoot.inicializarContexto() quando o comando exige
  -> use case principal
  -> ExecutionWindowPlanner / ExecutionPlanContext quando o fluxo exige
  -> LoggingService / ExecutionHistoryRepository
```

## Dependencias estruturais importantes

### `FluxoCompletoUseCase`

- depende de lock global SQL Server;
- planeja janelas por entidade;
- chama pre-backfill de coletas;
- usa `PipelineOrchestrator`;
- roda validacao autorizativa vinculada ao `execution_uuid`;
- atualiza `watermark` confirmado em caso de sucesso pleno;
- grava `runtime/state/last_run.properties` quando tudo fecha como sucesso.

### `ExtracaoPorIntervaloUseCase`

- quebra periodos grandes em blocos;
- executa planner de escopo;
- reusa o mesmo orquestrador;
- valida logs e integridade bloco a bloco.

### `LoopDaemonRunHandler`

- encapsula o ciclo continuo;
- usa watchdog global por ciclo;
- registra historico;
- chama reconciliacao automatica ao final de cada ciclo.

## Organizacao alvo

```text
br.com.extrator/
  features/
    coletas/
    manifestos/
    fretes/
    faturas/
    usuarios/
  plataforma/
    bootstrap/
    pipeline/
    auditoria/
    observabilidade/
    seguranca/
    suporte/
```

Regra de dependencia da trilha nova:

- `integracao` entrega DTOs/resultados;
- `aplicacao` orquestra janela, regra e idempotencia;
- `persistencia` implementa portas;
- `plataforma` oferece infraestrutura transversal.

## Como a arquitetura evoluiu

Arquiteturas antigas descreviam modulos e diretorios que nao existem mais, por exemplo estruturas separadas de `runners/ports/servicos` fora da arvore atual. No codigo moderno, a consolidacao ocorreu em torno de:

- `bootstrap`
- `aplicacao`
- `integracao`
- `persistencia`
- `observabilidade`
- `suporte`

Essa consolidacao continua valida para entendimento do legado.

Para manutencao nova, a direcao oficial e:

- componentes novos em `features/*` e `plataforma/*`;
- drenagem incremental do legado, sem big bang.

## Exemplo de raciocinio correto

Se um dev quer entender "onde uma falha de GraphQL e decidida", o caminho certo e:

1. `GraphQLPipelineStep`
2. `GraphQLGatewayAdapter`
3. `GraphQLExtractionService`
4. `PipelineOrchestrator`
5. `FailurePolicy` e `ErrorClassifier`

Se um documento antigo disser outra ordem, o documento antigo esta errado para fins de manutencao atual.
