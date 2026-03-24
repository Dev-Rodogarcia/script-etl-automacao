---
context:
  - ETL
  - AntiPatterns
  - Operacao
updated_at: 2026-03-18T00:00:32.1501533-03:00
source_of_truth: mixed
classification: atual
related_files:
  - src/main/java/br/com/extrator/aplicacao/extracao/FluxoCompletoUseCase.java
  - src/main/java/br/com/extrator/comandos/cli/extracao/daemon/LoopDaemonRunHandler.java
  - docs/legado/classificacao.md
---

# Anti-patterns

## O que nao fazer

### Usar documentacao legado como especificacao de implementacao nova

Isso reintroduz conceitos, paths e APIs que o runtime moderno ja abandonou.

### Tratar `D-1..D` como "24h corridas"

Isso distorce validacao, comparacao de volume e leitura de incidentes.

### Misturar `faturas_graphql` no mesmo raciocinio de custo das outras entidades GraphQL

`faturas_graphql` tem comportamento operacional proprio e bem mais pesado.

### Desabilitar isolamento por processo sem motivo claro

Isso remove uma das principais protecoes contra travas nao cooperativas.

### Ignorar `INCOMPLETO_*` como se fosse sucesso

Esse foi um erro historico que gerou falso positivo operacional.

### Considerar o daemon saudavel apenas porque ele nao morreu

O daemon pode continuar vivo e, ainda assim, estar acumulando alertas, pendencias e falhas de reconciliacao.

### Tentar "consertar" manifesto orfao alterando a documentacao em vez de validar `coletas`

O problema e de referencia operacional, nao de narrativa.

### Projetar nova manutencao assumindo REST como trilha principal

Hoje isso esta incorreto para o ETL moderno.

### Escrever documentacao moderna sem apontar para comportamento do codigo

Documentacao sem ancoragem em runtime vira opiniao, nao source of truth.

## Erros historicos que esta reorganizacao quer evitar

- falso sucesso com GraphQL falhando e `0` registros;
- confusao entre janela principal e janela de auditoria;
- conclusao prematura de que orfaos eram sempre erro terminal do daemon;
- consumo de material antigo como se fosse manual operacional atual.
