---
context:
  - Documentacao
  - Legado
  - ArquivoHistorico
updated_at: 2026-03-18T00:00:32.1501533-03:00
source_of_truth: docs
classification: atual
related_files:
  - docs/index.md
  - docs/legado/classificacao.md
---

# Acervo legado controlado

Esta area preserva historico, contexto de migracao, auditorias, planos antigos e artefatos que ajudaram a evoluir o ETL.

Ela existe para consulta retroativa, nao para orientar manutencao do runtime atual.

## Regra de uso

- Se um arquivo daqui conflitar com `docs/moderno` ou com o codigo, o arquivo legado deve ser considerado desatualizado.
- Novas mudancas arquiteturais, operacionais ou de pipeline nao devem ser documentadas aqui.
- Este acervo so deve ser consultado em tres cenarios: investigacao historica, comparacao de comportamento antigo ou recuperacao de contexto de decisao.

## Estrutura

- `arquiteturas-antigas/`: visoes anteriores da arquitetura, especificacoes e diagramas historicos.
- `pipelines-antigos/`: materiais de APIs, endpoints, colecoes de teste e pipelines que nao representam o runtime oficial atual.
- `implementacoes-descontinuadas/`: referencias, ideias abandonadas, descobertas e artefatos auxiliares sem suporte operacional atual.
- `decisoes-antigas/`: relatorios diarios, checklists e planos de acao que explicam a evolucao do sistema.
- `erros-conhecidos/`: auditorias, incidentes, validacoes passadas e troubleshooting historico.

## Como navegar sem se perder

1. Comece por `docs/legado/classificacao.md`.
2. Identifique se o arquivo esta marcado como `PARCIAL`, `LEGADO` ou `PERIGOSO`.
3. Use o material apenas como contexto de apoio.
4. Volte para `docs/moderno` antes de implementar qualquer mudanca.

## Regra editorial aplicada nesta reorganizacao

> "Se necessario, reestruture completamente a forma como o sistema e explicado, mesmo que isso signifique ignorar totalmente a documentacao existente."

Essa regra foi aplicada de forma intencional: o acervo foi preservado, mas perdeu status de documentacao oficial.
