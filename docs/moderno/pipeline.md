---
context:
  - ETL
  - Pipeline
  - Runtime
updated_at: 2026-03-18T00:00:32.1501533-03:00
source_of_truth: code
classification: atual
related_files:
  - src/main/java/br/com/extrator/bootstrap/pipeline/PipelineCompositionRoot.java
  - src/main/java/br/com/extrator/aplicacao/pipeline/PipelineOrchestrator.java
  - src/main/java/br/com/extrator/aplicacao/extracao/FluxoCompletoUseCase.java
  - src/main/java/br/com/extrator/aplicacao/extracao/ExtracaoPorIntervaloUseCase.java
---

# Pipeline moderno

## Ordem oficial do fluxo completo

No fluxo completo, os steps sao montados assim:

1. `graphql`
2. `dataexport`
3. `faturas_graphql` quando habilitado
4. `quality`

Detalhe critico:

- `graphql` e `dataexport` sao colocados lado a lado propositalmente para rodar em paralelo.
- `faturas_graphql` fica separado porque e caro e depende de regras especificas.
- `quality` roda por ultimo porque depende de dados ja persistidos.

## Composicao dos steps

### Step `graphql`

- Quando recebe entidade `graphql`, executa o bloco GraphQL principal.
- Na pratica, esse bloco cobre `usuarios_sistema`, `coletas` e `fretes`.
- `faturas_graphql` so entra dentro do mesmo service quando foi pedida explicitamente como entidade unica.

### Step `dataexport`

- Quando recebe entidade `dataexport`, executa o bloco DataExport principal.
- Cobre `manifestos`, `cotacoes`, `localizacao_cargas`, `contas_a_pagar` e `faturas_por_cliente`.

### Step `faturas_graphql`

- GraphQL dedicado para a entidade `faturas_graphql`.
- Tem timeout e comportamento operacional proprios.

### Step `quality`

- Executa 5 checks:
- unicidade
- completude
- freshness
- integridade referencial
- schema

## Paralelismo do core

`PipelineOrchestrator` detecta quando dois steps adjacentes sao `graphql` e `dataexport`. Nessa situacao:

- cria um executor fixo com 2 threads daemon;
- envia os dois steps em paralelo;
- aguarda ambos com timeout controlado;
- preserva o restante do pipeline mesmo se um deles falhar, conforme `failure policy`.

## Resultado de um step

Cada step produz um `StepExecutionResult` com:

- nome da etapa;
- entidade;
- status;
- inicio e fim;
- tentativa;
- mensagem;
- taxonomia de erro;
- metadata.

Status usados no pipeline:

- `SUCCESS`
- `FAILED`
- `DEGRADED`
- `SKIPPED`

## Como o pipeline e usado em cada comando

### Fluxo completo

- Usa steps do fluxo oficial acima.

### Extracao por intervalo

- Usa `PlanejadorEscopoExtracaoIntervalo`.
- Pode montar steps reduzidos por API ou entidade.
- Pode quebrar o periodo em blocos de ate 30 dias.

### Teste de API

- Monta apenas os steps relevantes para a API escolhida.

## Janela operacional

No fluxo completo:

- `dataInicio = hoje - 1 dia`
- `dataFim = hoje`

No intervalo:

- a janela vem do comando;
- se o periodo for longo, vira varios blocos.

## Pseudocodigo do pipeline

```text
steps = [graphql, dataexport, faturas_graphql?, quality?]

for step in steps:
  if step atual e proximo sao graphql + dataexport:
    executar em paralelo
  else:
    executar com politicas

  se failure policy pedir abort:
    parar pipeline
```

## Divergencias corrigidas em relacao ao acervo antigo

- A documentacao antiga misturava "pipeline" com ordem interna dos extractors e com scripts bat.
- A fonte oficial agora e a lista de `PipelineStep` criada por `PipelineCompositionRoot`.
