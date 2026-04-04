---
context:
  - ETL
  - Fluxo
  - Fretes
updated_at: 2026-04-03T17:05:00-03:00
source_of_truth: code
classification: atual
related_files:
  - src/main/java/br/com/extrator/integracao/graphql/services/GraphQLExtractionService.java
  - src/main/java/br/com/extrator/integracao/graphql/extractors/FreteExtractor.java
  - src/main/java/br/com/extrator/integracao/ClienteApiDataExport.java
  - src/main/java/br/com/extrator/observabilidade/servicos/IntegridadeEtlValidator.java
  - database/tabelas/002_criar_tabela_fretes.sql
  - database/views/012_criar_view_fretes_powerbi.sql
---

# Fluxo de fretes

## O que entra

`fretes` entra pela trilha GraphQL.

No runtime atual, essa trilha pode ser enriquecida com Data Export `6389`
para completar `fit_dpn_performance_finished_at` e reforcar `finished_at`
quando a origem oficial de performance estiver disponivel.

## Passo a passo

1. `GraphQLExtractionService` decide executar `fretes`
2. `FreteExtractor` chama a API GraphQL
3. `FreteMapper` transforma os nodes em entidade persistivel
4. `FreteExtractor` tenta enriquecer por minuta com Data Export `6389`
5. `FreteRepository` executa persistencia
6. `LogExtracaoRepository` grava o resumo da entidade

## Campos de negocio que importam

- `id`
- `corporation_sequence_number`
- `service_date`
- `accounting_credit_id`
- `data_previsao_entrega`
- `finished_at`
- `fit_dpn_performance_finished_at`
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
  -> pode enriquecer performance oficial via 6389
  -> expõe accounting_credit_id
  -> validacao final confere se esse id existe em dbo.faturas_graphql
```

## Reflexo analitico

`vw_fretes_powerbi` publica os campos usados pelos indicadores de gestao,
incluindo:

- `Responsável pela Região de Destino`
- `Data de Finalização`
- `Finalização da Performance`
- `Performance Diferença de Dias`
- `Performance Status`
- `Performance Status Dif de Dias`
- `Performance Status Dif de Dias Oficial`

## Como evoluiu

Na base antiga, fretes aparecia mais como entidade de payload. No runtime atual, fretes e uma entidade de integridade cruzada.
