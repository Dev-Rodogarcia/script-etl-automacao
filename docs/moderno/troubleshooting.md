---
context:
  - ETL
  - Troubleshooting
  - Debugging
updated_at: 2026-03-18T00:00:32.1501533-03:00
source_of_truth: mixed
classification: atual
related_files:
  - src/main/java/br/com/extrator/bootstrap/Main.java
  - src/main/java/br/com/extrator/comandos/cli/extracao/daemon/DaemonPaths.java
  - src/main/java/br/com/extrator/observabilidade/servicos/IntegridadeEtlValidator.java
  - src/main/resources/config.properties
  - config/.env.example
---

# Troubleshooting

## 1. GraphQL falha logo no inicio

Verifique primeiro:

- endpoint configurado e `/graphql`
- header usa `Bearer`
- `API_GRAPHQL_TOKEN` esta presente

Sintoma tipico:

- `ERRO_API`
- `0` registros
- falso entendimento de que "nao havia dados"

## 2. Fluxo trava ou demora muito

Verifique:

- logs de `OperationTimeoutGuard`
- timeout configurado para o step
- logs em `logs/isolated_steps`
- se `faturas_graphql` estava habilitado

## 3. Daemon parece parado ou preso

Arquivos-chave:

- `logs/daemon/loop_daemon.state`
- `logs/daemon/loop_daemon.pid`
- `logs/daemon/ciclos/*`

Perguntas certas:

- o processo ainda esta vivo?
- o estado ficou em `RUNNING` sem novo ciclo?
- houve timeout global do ciclo?

## 4. Manifestos orfaos em relacao a coletas

Nao conclua imediatamente que a extracao de coletas falhou.

Verifique:

- se houve pre-backfill;
- se houve pos-hidratacao;
- percentual de orfaos;
- modo loop daemon ou modo normal;
- janela real usada.

## 5. Fretes orfaos em relacao a faturas

Olhe:

- `accounting_credit_id` em `fretes`
- existencia de `faturas_graphql`
- backfill de faturas por ID

## 6. Validacao fala em "24h" e os numeros parecem estranhos

Lembrete:

- o sistema operacionalmente usa a janela `D-1..D`;
- isso nao e sinônimo de ultimas 24h corridas.

## 7. Nao consigo rodar dois fluxos ao mesmo tempo

Isso geralmente e esperado.

Verifique:

- lock global `etl-global-execution`
- timeout do lock

## 8. Documentacao antiga contradiz o comportamento observado

Interpretacao correta:

- o documento antigo esta arquivado por motivo.
- confirme primeiro no codigo atual e na documentacao moderna.

## 9. Tentativa de usar REST para reproduzir o ETL principal

Diagnostico:

- voce esta seguindo o acervo legado, nao a arquitetura moderna.

Correcao:

- volte para `docs/moderno/extracao/graphql.md` e `docs/moderno/extracao/rest.md`.

## Sequencia de debug recomendada

1. confirmar comando executado
2. confirmar janela
3. confirmar source API envolvida
4. ler `sys_execution_history`
5. ler `log_extracoes`
6. ler logs estruturados e logs do daemon/processo isolado
7. validar relacao entre entidades afetadas
