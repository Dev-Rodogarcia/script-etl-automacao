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
# Pipeline Lifecycle

1. CLI recebe comando (`--fluxo-completo` ou `--recovery`).
2. Composition root monta policies, observabilidade e steps.
3. Orquestrador executa steps em sequência:
   - GraphQL
   - DataExport
   - Faturas GraphQL (quando habilitado)
   - Data Quality
4. A cada step:
   - aplica retry policy
   - classifica erro
   - aplica failure policy
   - atualiza circuit breaker
   - publica métricas e log estruturado
5. Comando consolida:
   - resumo executivo
   - validação de completude/integridade existente
   - status final (success/partial/error)
