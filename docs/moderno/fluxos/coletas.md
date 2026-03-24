---
context:
  - ETL
  - Fluxo
  - Coletas
updated_at: 2026-03-18T00:00:32.1501533-03:00
source_of_truth: code
classification: atual
related_files:
  - src/main/java/br/com/extrator/integracao/graphql/services/GraphQLExtractionService.java
  - src/main/java/br/com/extrator/aplicacao/extracao/PreBackfillReferencialColetasUseCase.java
  - src/main/java/br/com/extrator/observabilidade/servicos/IntegridadeEtlValidator.java
  - database/tabelas/001_criar_tabela_coletas.sql
---

# Fluxo de coletas

## Papel da entidade

`coletas` nao e apenas uma entidade GraphQL a mais. Ela e a ancora referencial de `manifestos`.

Relacao critica:

- `manifestos.pick_sequence_code -> coletas.sequence_code`

## Fluxo moderno

### Antes do pipeline principal

Pode existir pre-backfill referencial:

1. busca a data mais antiga de manifesto orfao;
2. amplia a janela com buffer configurado;
3. extrai somente `coletas`.

### Durante o pipeline principal

`coletas` roda no bloco GraphQL principal.

Se `usuarios_sistema` for necessario, ele roda antes.

### Depois do pipeline principal

Pode existir pos-hidratacao:

- retroativa, antes do inicio principal;
- lookahead, depois do fim principal.

## Por que esse desenho existe

A origem real nao garante que uma unica janela simples resolva toda a referencia entre manifestos e coletas. O sistema moderno assume isso explicitamente e trata `coletas` como entidade com fluxo auxiliar.

## Validacao final

`IntegridadeEtlValidator` mede:

- quantos `manifestos` ficaram sem `coletas`;
- percentual de orfaos;
- amostra de `pick_sequence_code`;
- tolerancia configurada;
- diferenca entre modo normal e modo loop daemon.

## Pseudocodigo

```text
if nao for loop daemon:
  pre_backfill_coletas()

pipeline graphql principal
  -> usuarios_sistema quando necessario
  -> coletas
  -> fretes

if nao for loop daemon e nao houver falha nos runners:
  pos_hidratacao_coletas()

validar manifestos x coletas
```

## Erro comum de interpretacao

Erro:

- assumir que a janela principal sozinha define toda a integridade de `coletas`.

Interpretacao correta:

- a janela principal mede o ciclo oficial;
- as janelas auxiliares existem para fechar referencia sem adulterar essa medicao.
