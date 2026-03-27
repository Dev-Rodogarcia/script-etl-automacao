---
context:
  - ETL
  - Testes
  - Migracao
updated_at: 2026-03-25T00:00:00-03:00
source_of_truth: code
classification: atual
related_files:
  - src/test/java/br/com/extrator/plataforma/auditoria/aplicacao/ExecutionWindowPlannerTest.java
  - src/test/java/br/com/extrator/plataforma/auditoria/aplicacao/ExecutionAuditRecorderTest.java
  - src/test/java/br/com/extrator/features/usuarios/aplicacao/UsuariosSistemaSnapshotServiceTest.java
  - src/test/java/br/com/extrator/observabilidade/servicos/IntegridadeEtlValidatorAuthModeTest.java
---

# Testes da migracao de integridade

## Regra de escopo

- nao incluir `faturas_graphql` nesta frente;
- homologacao e aceite focam em `coletas`, `manifestos`, `fretes` e `usuarios_sistema`.

## Suite baseline obrigatoria

```bat
mvn --% -q -Dtest="GraphQLPaginatorTest,DataExportTimeWindowSupportTest,GraphQLColetaSupportTest,AbstractRepositoryAtomicityTest,DataQualityServiceTest,DataQualityPipelineStepTest,PipelineOrchestratorTest,ValidacaoApiBanco24hDetalhadaRepositoryTest,ValidacaoApiBanco24hDetalhadaReporterTest,GraphQLExtractionServiceTest,PipelineE2ETest" test
```

## Testes novos desta migracao

```bat
mvn --% -q -Dtest="ExecutionWindowPlannerTest,ExecutionAuditRecorderTest,UsuariosSistemaSnapshotServiceTest,IntegridadeEtlValidatorAuthModeTest" test
```

## Matriz por fase

### Fase 0. Baseline

- objetivo: provar que a refatoracao parte de uma base verde;
- esperado: 100% verde na suite baseline.

### Fase 1. Auditoria estruturada

- foco: `ExecutionAuditRecorderTest`;
- esperado: `api_total_unico`, `db_persistidos` e `noop_count` corretos por entidade.

### Fase 2. Validacao autorizativa

- foco: `IntegridadeEtlValidatorAuthModeTest`;
- esperado: run reprova sem `execution_uuid` ou sem `sys_execution_audit`.

### Fase 3. Janela e watermark

- foco: `ExecutionWindowPlannerTest`;
- esperado:
  - consulta de `D-1..D` para o ciclo corrente;
  - `confirmacaoInicio` vindo do `watermark` quando existir;
  - nenhuma inclusao de `faturas_graphql` quando a flag estiver desligada.

### Fase 4. Usuarios com estado

- foco: `UsuariosSistemaSnapshotServiceTest`;
- esperado:
  - `execution_uuid` propagado;
  - metricas de snapshot preservadas;
  - persistencia preparada para historico simples.

## Homologacao final

1. parar o daemon conforme `runbook-daemon.md`;
2. aplicar scripts SQL `014` a `017`;
3. rodar `mvn -q -DskipTests compile`;
4. rodar suite baseline;
5. rodar testes novos;
6. executar extracao controlada sem `faturas_graphql`;
7. validar `sys_execution_audit`, `sys_execution_watermark` e `dim_usuarios_historico`;
8. religar o daemon.

## Evidencias esperadas

- registros em `sys_execution_audit` por entidade do run;
- `watermark_confirmado` atualizado para `coletas`, `manifestos`, `fretes` e `usuarios_sistema`;
- `dim_usuarios` com `ativo`, `origem_atualizado_em` e `ultima_extracao_em`;
- `dim_usuarios_historico` com eventos `INSERTED`, `UPDATED`, `DEACTIVATED` ou `REACTIVATED`.
