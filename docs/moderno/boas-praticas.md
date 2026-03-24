---
context:
  - ETL
  - BoasPraticas
  - Onboarding
updated_at: 2026-03-18T00:00:32.1501533-03:00
source_of_truth: mixed
classification: atual
related_files:
  - docs/moderno/overview.md
  - docs/moderno/troubleshooting.md
  - docs/glossary.md
  - src/main/java/br/com/extrator/comandos/cli/console/ExibirAjudaComando.java
---

# Boas praticas

## Para devs

- Comece sempre pela documentacao moderna.
- Ao mudar comportamento do runtime, atualize a documentacao moderna na mesma entrega.
- Use os nomes oficiais das entidades (`coletas`, `fretes`, `faturas_graphql`, etc.).
- Diferencie claramente janela operacional de janela de auditoria.
- Antes de concluir que o ETL falhou, verifique logs, historico e integridade.

## Para IA e agentes

- Trate `docs/moderno` como contexto primario.
- Use `docs/glossary.md` para normalizar termos.
- Se encontrar conflito entre moderno e legado, escolha o moderno.
- Se houver conflito entre moderno e codigo, escolha o codigo e trate a documentacao como defeito a corrigir.

## Para debugging

- Correlacione `execution_id`, comando, `log_extracoes` e `sys_execution_history`.
- Olhe primeiro para a entidade falhada e suas referencias.
- Em casos de travamento, cheque timeouts, watchdog e processo isolado.

## Para manutencao documental

- Nao promova documento para moderno sem validar no codigo.
- Nao use relatorio diario como manual oficial.
- Nao apague contexto historico; arquive em `docs/legado`.

## Para onboarding

Ordem recomendada para primeiros dias:

1. `overview`
2. `arquitetura`
3. `pipeline`
4. `fluxos/ciclo-completo`
5. `persistencia`
6. `troubleshooting`

## Regra de ouro

Documentacao boa neste projeto e a que reduz ambiguidade para tres perfis ao mesmo tempo:

- dev que vai manter codigo;
- IA que vai consumir contexto;
- operador que vai depurar incidente.
