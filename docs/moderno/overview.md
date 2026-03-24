---
context:
  - ETL
  - Overview
  - Onboarding
updated_at: 2026-03-18T00:00:32.1501533-03:00
source_of_truth: code
classification: atual
related_files:
  - src/main/java/br/com/extrator/bootstrap/Main.java
  - src/main/java/br/com/extrator/comandos/cli/CommandRegistry.java
  - src/main/java/br/com/extrator/aplicacao/extracao/FluxoCompletoUseCase.java
  - src/main/resources/config.properties
  - config/.env.example
---

# Visao geral

## O que este sistema e

Este repositorio contem um ETL CLI em Java 17 que integra dados da ESL Cloud com um banco SQL Server. O sistema extrai dados de duas trilhas principais:

- GraphQL
- DataExport

Depois da extracao, o sistema:

- persiste os dados em tabelas operacionais;
- registra logs de extracao e historico de execucao;
- valida completude;
- valida integridade ETL;
- executa checks de data quality;
- pode operar em modo unico, intervalo, recovery e loop daemon.

## O que este sistema nao e

- Nao e um servico web.
- Nao e um ETL orientado a eventos.
- Nao e um conjunto de scripts soltos sem orquestracao.
- Nao usa a documentacao historica como autoridade primaria.

## Fonte oficial da verdade

Para entendimento do sistema, a ordem de confianca e:

1. Codigo atual em `src/main/java`
2. Scripts SQL em `database`
3. Configuracao ativa em `config/.env.example`, `.env` local e `src/main/resources/config.properties`
4. Documentacao moderna em `docs/moderno`
5. Documentacao legado apenas como contexto historico

## Modos principais de execucao

### Fluxo completo

- Ponto de entrada padrao quando a aplicacao roda sem argumentos ou com `--fluxo-completo`.
- Janela principal diaria `D-1..D`.
- Executa pre-backfill de coletas quando aplicavel.
- Roda pipeline principal.
- Executa validacoes finais.

### Extracao por intervalo

- Comando `--extracao-intervalo`.
- Divide intervalos longos em blocos de ate 30 dias.
- Permite limitar por API ou entidade.

### Recovery

- Comando `--recovery`.
- Reusa extracao por intervalo para replay/backfill idempotente.

### Teste de API

- Comando `--testar-api`.
- Usa pipeline reduzido para validar GraphQL ou DataExport.

### Loop daemon

- Comandos `--loop-daemon-start`, `--loop-daemon-stop`, `--loop-daemon-status`, `--loop-daemon-run`.
- Mantem ciclos continuos com reconciliacao automatica e historico proprio.

### Validacoes dedicadas

- `--validar-api-banco-24h`
- `--validar-api-banco-24h-detalhado`
- `--validar-etl-extremo`
- `--validar-etl-resiliencia`

Esses comandos nao sao apenas "relatorios"; eles fazem parte do modelo operacional do sistema.

## Entidades modernas

### GraphQL

- `usuarios_sistema`
- `coletas`
- `fretes`
- `faturas_graphql`

### DataExport

- `manifestos`
- `cotacoes`
- `localizacao_cargas`
- `contas_a_pagar`
- `faturas_por_cliente`

## Evolucao resumida

O repositorio possui marcas claras de varias fases:

- fase antiga centrada em descobertas e experimentos documentais;
- fase intermediaria com forte volume de especificacoes e auditorias;
- fase atual com composition root, pipeline orquestrado, politicas de resiliencia, isolamento por processo e loop daemon com reconciliacao.

Na pratica, a fase atual reescreveu a explicacao do sistema a partir do runtime moderno.

## O que uma IA precisa assumir

- O ETL moderno oficial nao deve ser inferido a partir de `docs/02-apis/rest`.
- `D-1..D` e a janela operacional padrao.
- `faturas_graphql` e um step separado e mais custoso.
- `coletas` tem papel referencial central para `manifestos`.
- `fretes` depende de `faturas_graphql` para fechar referencias financeiras.
- o daemon pode continuar vivo mesmo quando a validacao final gera alerta operacional.

## O que um dev novo deve ler primeiro

1. `docs/moderno/arquitetura.md`
2. `docs/moderno/pipeline.md`
3. `docs/moderno/fluxos/ciclo-completo.md`
4. `docs/moderno/persistencia.md`
5. `docs/moderno/troubleshooting.md`

## Pseudocodigo mental do sistema

```text
main(args)
  -> resolve comando
  -> inicializa contexto do pipeline quando necessario
  -> executa use case
  -> grava historico da execucao

fluxo completo
  -> define janela D-1..D
  -> pre-backfill de coletas
  -> pipeline core: graphql || dataexport
  -> step opcional: faturas_graphql
  -> step: data quality
  -> pos-hidratacao de coletas
  -> validacao de completude
  -> validacao de integridade ETL
  -> resumo executivo
```
