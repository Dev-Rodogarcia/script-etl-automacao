---
context:
  - ETL
  - Arquitetura
  - Decisoes
updated_at: 2026-03-18T00:00:32.1501533-03:00
source_of_truth: mixed
classification: atual
related_files:
  - src/main/java/br/com/extrator/bootstrap/Main.java
  - src/main/java/br/com/extrator/bootstrap/pipeline/PipelineCompositionRoot.java
  - src/main/java/br/com/extrator/aplicacao/extracao/FluxoCompletoUseCase.java
  - src/main/java/br/com/extrator/comandos/cli/extracao/daemon/LoopDaemonRunHandler.java
---

# Decisoes arquiteturais

## D1. Codigo atual vence documentacao antiga

**Problema**

- A base documental acumulou snapshots, rascunhos e relatorios contraditorios.

**Decisao tomada**

- A documentacao moderna passa a ser derivada do codigo e nao o contrario.

**Alternativas consideradas**

- Preservar todos os textos antigos com pequenas correcoes.
- Marcar arquivos antigos como "possivelmente desatualizados" sem reestruturar.

**Impacto**

- Menos ambiguidade.
- Mais confianca para devs e IA.

## D2. O pipeline moderno oficial e GraphQL + DataExport

**Problema**

- A documentacao anterior tratava REST como parte central do ETL.

**Decisao tomada**

- O pipeline moderno oficial cobre GraphQL e DataExport.
- REST fica documentado apenas como contexto historico e operacional auxiliar.

**Alternativas consideradas**

- Manter REST como trilha oficial mesmo sem runtime ativo correspondente.

**Impacto**

- Evita reintroduzir implementacoes e expectativas erradas.

## D3. GraphQL e DataExport rodam em paralelo no core

**Problema**

- A execucao sequencial aumenta tempo total e mascara independencia entre fontes.

**Decisao tomada**

- Quando adjacentes no fluxo principal, os steps `graphql` e `dataexport` rodam em paralelo.

**Alternativas consideradas**

- Sequencial por simplicidade.

**Impacto**

- Melhor throughput.
- Necessidade de observabilidade e timeout robustos.

## D4. `faturas_graphql` roda separadamente e por ultimo

**Problema**

- `faturas_graphql` tem enriquecimento caro e pode alongar muito a execucao.

**Decisao tomada**

- No fluxo completo, ela e executada como step dedicado apos o core.

**Alternativas consideradas**

- Executar junto do step GraphQL principal.

**Impacto**

- Dados principais chegam antes.
- O custo operacional de faturas fica isolado.

## D5. Integridade referencial de manifestos depende de hidratacao de coletas fora da janela principal

**Problema**

- A origem nao oferece filtros perfeitos para fechar manifestos x coletas em uma unica janela simples.

**Decisao tomada**

- Executar pre-backfill e pos-hidratacao de `coletas` fora da janela principal auditada.

**Alternativas consideradas**

- Exigir que a janela principal resolva tudo sozinha.

**Impacto**

- Reduz falsos orfaos.
- Introduz fluxo auxiliar que precisa ser bem explicado.

## D6. Falha de validacao no loop daemon nao derruba automaticamente o processo

**Problema**

- Em modo continuo, algumas divergencias operacionais conhecidas nao devem matar o daemon inteiro.

**Decisao tomada**

- No loop daemon, falha apenas de validacao final vira alerta operacional e nao falha terminal do processo.

**Alternativas consideradas**

- Parar o daemon a cada divergencia de integridade.

**Impacto**

- Maior continuidade operacional.
- Maior necessidade de monitorar alertas.

## D7. Timeout e thread leak sao tratados como riscos de primeira classe

**Problema**

- Chamadas externas e tasks internas podem travar ou ignorar interrupcao.

**Decisao tomada**

- Centralizar watchdog em `OperationTimeoutGuard` e detectar threads residuais.

**Alternativas consideradas**

- Timeout apenas no HTTP client.

**Impacto**

- Contencao melhor de travamentos.
- Maior complexidade de runtime.

## D8. Isolamento por processo fica habilitado por padrao

**Problema**

- Nem todo travamento pode ser resolvido com `interrupt`.

**Decisao tomada**

- GraphQL e DataExport podem rodar em processos filhos por padrao.

**Alternativas consideradas**

- Sempre executar no mesmo processo JVM.

**Impacto**

- Melhor confinamento de falhas.
- Mais artefatos de runtime e custo de spawn.

## D9. Historico de execucao precisa de fallback fora do banco

**Problema**

- Se o banco falhar, o proprio historico de execucao pode se perder.

**Decisao tomada**

- Persistir em `sys_execution_history` e, em falha, escrever fallback NDJSON em arquivo.

**Alternativas consideradas**

- Ignorar historico quando o banco estiver indisponivel.

**Impacto**

- Menor perda de observabilidade em incidentes.

## D10. Moderno e legado ficam separados fisicamente

**Problema**

- Pastas misturadas fazem a IA e devs consumirem material errado.

**Decisao tomada**

- `docs/moderno` vira a base oficial e `docs/legado` vira arquivo historico controlado.

**Alternativas consideradas**

- Apenas colocar avisos dentro dos arquivos antigos.

**Impacto**

- Navegacao muito mais segura para onboarding, IA e debugging.
