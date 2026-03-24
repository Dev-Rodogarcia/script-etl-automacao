---
context:
  - ETL
  - Fluxo
  - Fretes
updated_at: 2026-03-18T00:00:32.1501533-03:00
source_of_truth: code
classification: atual
related_files:
  - src/main/java/br/com/extrator/integracao/graphql/services/GraphQLExtractionService.java
  - src/main/java/br/com/extrator/integracao/graphql/extractors/FreteExtractor.java
  - src/main/java/br/com/extrator/observabilidade/servicos/IntegridadeEtlValidator.java
  - database/tabelas/002_criar_tabela_fretes.sql
---

# Fluxo de fretes

## O que entra

`fretes` entra pela trilha GraphQL.

## Passo a passo

1. `GraphQLExtractionService` decide executar `fretes`
2. `FreteExtractor` chama a API GraphQL
3. `FreteMapper` transforma os nodes em entidade persistivel
4. `FreteRepository` executa persistencia
5. `LogExtracaoRepository` grava o resumo da entidade

## Campos de negocio que importam

- `id`
- `service_date`
- `accounting_credit_id`
- dados de origem/destino
- dados fiscais
- dados de NFSe

## Por que `fretes` e critico

`fretes` faz a ponte operacional entre a execucao logistica e a conciliacao financeira. O campo que mais importa para integridade cruzada e:

- `accounting_credit_id`

Ele deve fechar com:

- `faturas_graphql.id`

## Validacao posterior

`IntegridadeEtlValidator` verifica se existem fretes orfaos em relacao a `faturas_graphql`.

Se houver muitos orfaos, o problema pode estar em:

- falha real na extracao de `faturas_graphql`;
- criterio temporal inadequado;
- backfill ainda nao realizado;
- instabilidade de origem.

## Exemplo mental

```text
frete extraido
  -> salva em dbo.fretes
  -> expõe accounting_credit_id
  -> validacao final confere se esse id existe em dbo.faturas_graphql
```

## Como evoluiu

Na base antiga, fretes aparecia mais como entidade de payload. No runtime atual, fretes e uma entidade de integridade cruzada.
