---
context:
  - ETL
  - GraphQL
  - Extracao
updated_at: 2026-03-18T00:00:32.1501533-03:00
source_of_truth: code
classification: atual
related_files:
  - src/main/java/br/com/extrator/integracao/graphql/services/GraphQLExtractionService.java
  - src/main/java/br/com/extrator/bootstrap/pipeline/GraphQLGatewayAdapter.java
  - src/main/java/br/com/extrator/integracao/GraphQLPaginator.java
  - src/main/java/br/com/extrator/integracao/GraphQLIntervaloHelper.java
  - src/main/java/br/com/extrator/integracao/graphql/extractors/FaturaGraphQLExtractor.java
---

# Extracao GraphQL

## O que cobre

A trilha GraphQL moderna cobre:

- `usuarios_sistema`
- `coletas`
- `fretes`
- `faturas_graphql`

## Por que existe

GraphQL e a fonte principal das entidades operacionais e relacionais do ETL moderno. Ela concentra:

- dimensoes auxiliares como usuarios;
- entidades operacionais criticas como coletas e fretes;
- a trilha financeira enriquecida de `faturas_graphql`.

## Como o step GraphQL funciona

O `GraphQLGatewayAdapter` recebe o periodo e a entidade. Depois:

1. normaliza o filtro;
2. decide se executa no processo atual ou em processo filho;
3. chama `GraphQLExtractionService`;
4. devolve `StepExecutionResult`.

## Ordem interna da execucao GraphQL principal

Quando o filtro e geral (`graphql`), a ordem pratica dentro do service e:

1. `usuarios_sistema`
2. `coletas`
3. `fretes`
4. `faturas_graphql` apenas se o pedido for explicito

Detalhe importante:

- No fluxo completo oficial, `faturas_graphql` nao roda dentro desse bloco geral.
- Ela roda depois, como step dedicado do pipeline.

## Regras especificas por entidade

### `usuarios_sistema`

- Pode rodar antes de `coletas`.
- Serve como apoio para mapear referencias operacionais.

### `coletas`

- Tem papel central para fechar referencias de `manifestos`.
- Participa do pre-backfill e da pos-hidratacao referencial.

### `fretes`

- Carrega `accounting_credit_id`, que depois precisa fechar com `faturas_graphql.id`.

### `faturas_graphql`

- E a entidade GraphQL mais cara do runtime.
- Faz deduplicacao.
- Faz backfill por `accounting_credit_id`.
- Faz enriquecimento concorrente.
- Enriquece `faturas_por_cliente` via tabela ponte.

## Paginacao e protecoes

### `GraphQLPaginator`

- controla limite de paginas;
- controla limite de registros;
- detecta cursor repetido;
- detecta pagina vazia inconsistente;
- marca resultado incompleto quando necessario.

### `GraphQLIntervaloHelper`

- usado quando a origem nao trabalha bem com intervalo amplo;
- quebra a extracao por dia;
- pode repetir dias que falharam com `ERRO_API` ou `CIRCUIT_BREAKER`.

## Credenciais e endpoint

O comportamento operacional confirmado hoje e:

- endpoint GraphQL: `/graphql`
- autenticacao: header `Authorization: Bearer <token>`

Qualquer documento antigo que normalize `/api/graphql` como padrao deve ser tratado como historico incorreto.

## Exemplo real de relacao de negocio

```text
fretes.accounting_credit_id
  -> faturas_graphql.id

manifestos.pick_sequence_code
  -> coletas.sequence_code
```

## Pseudocodigo

```text
executar graphql(dataInicio, dataFim, entidade)
  -> validar banco
  -> limpar avisos de seguranca
  -> se entidade for geral:
       usuarios_sistema
       coletas
       fretes
       informar que faturas rodara depois
     senao se entidade for faturas_graphql:
       executar faturas_graphql
  -> gravar log_extracoes por entidade
  -> se houver falhas relevantes:
       lancar erro para o pipeline
```

## Como evoluiu

Historicamente, a base documental descrevia GraphQL apenas como um conjunto de endpoints. O runtime atual ja trata GraphQL como trilha formal de ETL, com:

- step dedicado;
- timeouts por entidade;
- isolamento por processo;
- backfill referencial;
- enriquecimento pesado para faturas;
- integridade final baseada em logs e banco.
