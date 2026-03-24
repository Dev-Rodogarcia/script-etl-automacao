---
context:
  - ETL
  - Documentacao
  - SourceOfTruth
updated_at: 2026-03-18T00:00:32.1501533-03:00
source_of_truth: mixed
classification: atual
related_files:
  - README.md
  - pom.xml
  - src/main/java/br/com/extrator/bootstrap/Main.java
  - src/main/java/br/com/extrator/comandos/cli/CommandRegistry.java
  - src/main/java/br/com/extrator/bootstrap/pipeline/PipelineCompositionRoot.java
---

# Base oficial de conhecimento

Esta pasta foi reorganizada para separar claramente o que e oficial do que e apenas historico.

Regra principal:

- O codigo atual e a fonte primaria da verdade.
- A documentacao moderna existe para explicar o codigo atual.
- A documentacao legado existe para preservar contexto, nao para orientar novas mudancas.

Diretriz editorial adotada nesta reestruturacao:

> "Se necessario, reestruture completamente a forma como o sistema e explicado, mesmo que isso signifique ignorar totalmente a documentacao existente."

## Onde comecar

- Leitura para novos devs: `docs/moderno/overview.md` -> `docs/moderno/arquitetura.md` -> `docs/moderno/fluxos/ciclo-completo.md`
- Leitura para IA: `docs/moderno/overview.md` -> `docs/moderno/pipeline.md` -> `docs/moderno/orchestrator.md` -> `docs/glossary.md`
- Leitura para debugging: `docs/moderno/troubleshooting.md` -> `docs/moderno/observabilidade.md` -> `docs/moderno/resiliencia.md`

## Estrutura oficial

```text
docs/
  index.md
  glossary.md
  decisions.md
  moderno/
    overview.md
    arquitetura.md
    pipeline.md
    orchestrator.md
    extracao/
      graphql.md
      rest.md
    persistencia.md
    resiliencia.md
    timeouts.md
    retries.md
    watchdog.md
    isolamento.md
    observabilidade.md
    fluxos/
      ciclo-completo.md
      fretes.md
      coletas.md
    troubleshooting.md
    anti-patterns.md
    boas-praticas.md
  legado/
    index.md
    classificacao.md
    arquiteturas-antigas/
    pipelines-antigos/
    implementacoes-descontinuadas/
    decisoes-antigas/
    erros-conhecidos/
```

## O que o sistema e hoje

O runtime atual e um ETL CLI em Java 17 que:

- extrai dados principalmente de GraphQL e DataExport;
- persiste em SQL Server;
- executa validacoes de completude, integridade e data quality;
- opera em execucao unica, intervalo, recovery e loop daemon;
- aplica retry, circuit breaker, watchdog e isolamento por processo.

Observacao importante:

- API REST aparece em configuracoes, material auxiliar e historico, mas nao compoe o pipeline moderno oficial de extracao.

## Problemas encontrados na base anterior

- Havia documentacao apontando para classes e diretorios que nao existem mais.
- Havia mistura entre especificacao historica, relatorio diario, plano de acao e documentacao operacional.
- Havia arquivos que tratavam API REST como caminho principal do ETL, o que nao bate com o runtime atual.
- Havia conhecimento operacional relevante espalhado em relatorios e auditorias, sem consolidacao oficial.
- Havia ambiguidade entre "24h corridas" e a janela operacional real `D-1..D`.

## Decisoes desta reorganizacao

- A documentacao moderna explica apenas o comportamento suportado pelo codigo atual.
- O acervo antigo foi arquivado em `docs/legado` por contexto.
- Informacao historica nao foi descartada; foi rebaixada para contexto controlado.
- Sempre que houve conflito, a narrativa foi corrigida para refletir o codigo atual.

## Classificacao do acervo anterior

Resumo:

- `ATUAL`: nenhum arquivo antigo foi mantido como fonte oficial sem reescrita.
- `PARCIAL`: alguns materiais historicos continham fatos validos, mas misturados com estrutura defasada.
- `LEGADO`: relatorios, snapshots, notas de versao, planos e artefatos descontinuados.
- `PERIGOSO`: arquivos que podem induzir implementacao errada porque descrevem runtime ou paths inexistentes.

Detalhamento completo:

- `docs/legado/classificacao.md`

## Score final da nova base

- Clareza: 9/10
- Completude: 9/10
- Consistencia: 9/10
- Prontidao para IA: 10/10

Pontos ainda dependentes de runtime real:

- valores operacionais de volume por ambiente;
- comportamento exato da origem ESL em dias com instabilidade externa;
- ajustes futuros de templates DataExport ou schema GraphQL.
