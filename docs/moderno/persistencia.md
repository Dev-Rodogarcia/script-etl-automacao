---
context:
  - ETL
  - Persistencia
  - SQLServer
updated_at: 2026-03-18T00:00:32.1501533-03:00
source_of_truth: code
classification: atual
related_files:
  - database/README.md
  - database/tabelas/001_criar_tabela_coletas.sql
  - database/tabelas/002_criar_tabela_fretes.sql
  - database/tabelas/003_criar_tabela_manifestos.sql
  - database/tabelas/008_criar_tabela_faturas_graphql.sql
  - src/main/java/br/com/extrator/persistencia/repositorio/ExecutionHistoryRepository.java
---

# Persistencia

## Visao geral

O ETL moderno persiste principalmente em SQL Server. O banco contem:

- tabelas fact e operacionais por entidade;
- tabelas de auditoria e historico;
- views para consumo analitico;
- views dimensionais;
- scripts de validacao.

## Tabelas centrais do ETL

### GraphQL

- `coletas`
- `fretes`
- `faturas_graphql`
- `dim_usuarios`

### DataExport

- `manifestos`
- `cotacoes`
- `localizacao_cargas`
- `contas_a_pagar`
- `faturas_por_cliente`

### Observabilidade

- `log_extracoes`
- `page_audit`
- `sys_execution_history`
- `sys_auditoria_temp`

## Chaves e relacoes importantes

### `coletas`

- PK: `id`
- chave de negocio relevante: `sequence_code`

### `fretes`

- PK: `id`
- relacao financeira importante: `accounting_credit_id`

### `manifestos`

- PK tecnica: `id` identity
- chave de merge: hash persistido com `sequence_code`, `pick_sequence_code`, `mdfe_number`

### `faturas_graphql`

- PK: `id`

## Relacoes operacionais criticas

```text
manifestos.pick_sequence_code -> coletas.sequence_code
fretes.accounting_credit_id   -> faturas_graphql.id
faturas_por_cliente.fit_ant_document -> faturas_graphql.document
```

## Como a gravacao e feita

O padrao predominante e:

1. extractor recebe DTOs;
2. mapper converte DTO para entidade persistivel;
3. repository executa `MERGE` ou `INSERT`;
4. `log_extracoes` recebe o resumo da entidade.

Isso significa que o ETL moderno usa persistencia orientada a repositories, nao SQL solto dentro dos comandos.

## Auditoria de execucao

### `log_extracoes`

Registra por entidade:

- inicio;
- fim;
- status final;
- registros extraidos;
- paginas processadas;
- mensagem.

Esse log e usado por:

- validacao de completude;
- integridade ETL;
- testes de API;
- correlacao de janela executada.

### `sys_execution_history`

Registra por comando:

- inicio;
- fim;
- duracao;
- status;
- total de registros;
- categoria e mensagem de erro.

Se a persistencia falhar, o runtime ainda grava fallback em `logs/execution_history_fallback.ndjson`.

### `page_audit`

Usado para auditoria de paginas em paginadores, com hashes de request/resposta e metricas por pagina.

## Lock global de execucao

O ETL usa `sp_getapplock` no SQL Server para impedir duas execucoes concorrentes do mesmo fluxo operacional.

Recurso usado:

- `etl-global-execution`

Esse lock protege:

- fluxo completo;
- extracao por intervalo;
- teste de API.

## Estado em arquivo

Nem todo estado fica no banco.

Arquivos relevantes:

- `runtime/state/last_run.properties`
- `logs/daemon/loop_daemon.state`
- `logs/daemon/loop_daemon.pid`
- `logs/daemon/loop_reconciliation.state`

## Seguranca fora do SQL Server

Autenticacao operacional usa um SQLite separado para usuarios e perfis. Esse banco nao e a persistencia do ETL em si; ele e a persistencia de seguranca do CLI.

## Exemplo de leitura correta

Se a pergunta for "onde descubro se uma entidade executou e quantos registros gravou?", a resposta correta comeca em:

- `log_extracoes`

Se a pergunta for "onde descubro o status do comando inteiro?", a resposta correta comeca em:

- `sys_execution_history`
