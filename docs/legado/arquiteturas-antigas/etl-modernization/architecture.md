---
context:
  - Documentacao
  - Legado
  - ArquivoHistorico
updated_at: 2026-03-18T00:00:32.1501533-03:00
source_of_truth: docs
classification: perigoso
related_files:
  - docs/index.md
  - docs/legado/index.md
  - docs/legado/classificacao.md
---
# Arquitetura ETL Refatorada

## Diagrama
```text
Main/CommandRegistry
        |
        v
ExecutarFluxoCompletoComando
        |
        v
PipelineCompositionRoot
        |
        +--> Ports
        |    - DataExportGateway
        |    - GraphQLGateway
        |    - ExtractionLoggerPort
        |    - ClockPort / ConfigPort
        |
        +--> Policies
        |    - RetryPolicy (exponential backoff + jitter)
        |    - FailurePolicy
        |    - CircuitBreaker
        |    - ErrorClassifier / ErrorTaxonomy
        |    - IdempotencyPolicy
        |
        +--> PipelineOrchestrator
               |
               +--> Step: GraphQL
               +--> Step: DataExport
               +--> Step: Faturas GraphQL (opcional)
               +--> Step: DataQuality
```

## Estrutura de diretórios (núcleo novo)
```text
src/main/java/br/com/extrator/
  runners/
    ports/
    pipeline/
  servicos/
    policies/
    observability/
  auditoria/
    quality/
  comandos/extracao/recovery/
```

## Decisões
- Refatoração foi aplicada no projeto existente (`src/main/java`) para manter compatibilidade.
- `ExecutarFluxoCompletoComando` agora usa `PipelineOrchestrator`.
- `RecoveryUseCase` reutiliza `--extracao-intervalo` para replay/backfill idempotente.
