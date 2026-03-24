---
context:
  - Documentacao
  - Legado
  - Classificacao
updated_at: 2026-03-18T00:00:32.1501533-03:00
source_of_truth: mixed
classification: atual
related_files:
  - docs/index.md
  - docs/legado/index.md
  - docs/moderno/overview.md
---

# Classificacao do acervo anterior

## Legenda oficial

- `ATUAL`: 100% alinhado ao codigo atual e autorizado como fonte oficial.
- `PARCIAL`: contem fatos uteis, mas exige validacao contra o codigo antes de qualquer uso.
- `LEGADO`: historico preservado sem valor normativo para o runtime moderno.
- `PERIGOSO`: pode induzir implementacao, operacao ou debugging incorretos.

## Resultado da triagem

- Nenhum arquivo historico foi mantido como `ATUAL` sem reescrita.
- A nova fonte oficial esta em `docs/moderno`.
- O codigo atual segue como prioridade absoluta quando houver conflito.

## Matriz de classificacao por caminho

| Caminho | Status | Motivo |
| --- | --- | --- |
| `docs/legado/arquiteturas-antigas/00-documentos-gerais/**` | `LEGADO` | entregas, resumos e snapshots de periodos anteriores |
| `docs/legado/arquiteturas-antigas/01-inicio-rapido/**` | `LEGADO` | onboarding antigo, anterior a arquitetura moderna consolidada |
| `docs/legado/arquiteturas-antigas/04-especificacoes-tecnicas/**` | `PERIGOSO` | mistura conceitos validos com package tree e contratos que nao refletem o codigo atual |
| `docs/legado/arquiteturas-antigas/05-versoes/**` | `LEGADO` | notas e marcos historicos de release |
| `docs/legado/arquiteturas-antigas/dashboards/**` | `LEGADO` | referencias auxiliares fora do runtime principal |
| `docs/legado/arquiteturas-antigas/etl-modernization/**` | `PERIGOSO` | descreve uma arquitetura intermediaria diferente da composicao real atual |
| `docs/legado/arquiteturas-antigas/DER-CLASSES-JAVA-COMPLETO.md` | `PERIGOSO` | enumera estrutura de classes que nao corresponde integralmente ao codigo presente |
| `docs/legado/arquiteturas-antigas/DER-COMPLETO-BANCO-DADOS.md` | `PARCIAL` | o banco continua relevante, mas o documento precisa ser lido junto de `database/` e do codigo |
| `docs/legado/arquiteturas-antigas/FLUXOGRAMA-COMPLETO-SISTEMA.md` | `PERIGOSO` | consolida um fluxo antigo que nao representa o orchestrator atual |
| `docs/legado/arquiteturas-antigas/FLUXOGRAMA-ESTRUTURADO-MIRO.md` | `PERIGOSO` | diagrama historico com narrativa de pipeline desatualizada |
| `docs/legado/arquiteturas-antigas/Resumo-tecnico-codex.md` | `LEGADO` | resumo pontual de auditoria, util apenas como contexto historico |
| `docs/legado/pipelines-antigos/02-apis/rest/**` | `PERIGOSO` | trata REST como trilha principal de extracao, o que nao bate com o runtime moderno |
| `docs/legado/pipelines-antigos/endpoints/README.md` | `PERIGOSO` | reforca descoberta de endpoints como se fosse base operacional atual |
| `docs/legado/pipelines-antigos/02-apis/graphql/**` | `PARCIAL` | varios conceitos de payload ainda ajudam, mas a orquestracao descrita nao e a oficial |
| `docs/legado/pipelines-antigos/02-apis/dataexport/**` | `PARCIAL` | documenta fontes ainda usadas, mas sem refletir a composicao moderna do pipeline |
| `docs/legado/pipelines-antigos/02-apis/analise-critica.md` | `PARCIAL` | analise util para contexto, mas nao normativa |
| `docs/legado/pipelines-antigos/02-apis/requisicoes.md` | `PARCIAL` | exemplos auxiliares, nao contrato oficial |
| `docs/legado/pipelines-antigos/02-apis/teste.md` | `LEGADO` | experimento historico |
| `docs/legado/pipelines-antigos/configuracao/insomnia/**` | `LEGADO` | colecoes de exploracao manual, fora do fluxo automatizado atual |
| `docs/legado/implementacoes-descontinuadas/**` | `LEGADO` | referencias, ideias e artefatos sem suporte operacional |
| `docs/legado/decisoes-antigas/direcionamento/verdades/**` | `PARCIAL` | contem fatos operacionais aproveitados na nova base, mas sem validade isolada |
| `docs/legado/decisoes-antigas/direcionamento/**` | `LEGADO` | planos e TODOs de fases anteriores |
| `docs/legado/decisoes-antigas/relatorios-diarios/**` | `LEGADO` | diario de trabalho e descobertas por data |
| `docs/legado/erros-conhecidos/analises/**` | `PARCIAL` | material util para investigacao de incidentes, mas dependente de contexto temporal |
| `docs/legado/erros-conhecidos/configuracao/troubleshooting/**` | `LEGADO` | troubleshooting de setup antigo, util apenas como referencia residual |
| `docs/legado/erros-conhecidos/SECURITY_ROTATION_REQUIRED.md` | `PARCIAL` | alerta operacional valido, mas nao documentacao arquitetural |
| `docs/legado/erros-conhecidos/relatorio-validacao-etl-2026-03-05.md` | `PARCIAL` | evidencia de auditoria que ajuda no debugging, sem substituir a documentacao moderna |

## Documentos explicitamente perigosos

Os arquivos abaixo merecem atencao especial porque sao os que mais facilmente induzem erro em novos devs ou agentes:

- `docs/legado/arquiteturas-antigas/etl-modernization/architecture.md`
- `docs/legado/arquiteturas-antigas/etl-modernization/README.md`
- `docs/legado/arquiteturas-antigas/etl-modernization/pipeline-lifecycle.md`
- `docs/legado/arquiteturas-antigas/04-especificacoes-tecnicas/implementacao-apis/technical-specification.md`
- `docs/legado/arquiteturas-antigas/04-especificacoes-tecnicas/implementacao-apis/design.md`
- `docs/legado/pipelines-antigos/02-apis/rest/faturas-a-pagar.md`
- `docs/legado/pipelines-antigos/02-apis/rest/faturas-a-receber.md`
- `docs/legado/pipelines-antigos/02-apis/rest/ocorrencias.md`
- `docs/legado/pipelines-antigos/endpoints/README.md`

Motivos recorrentes:

- assumem REST como parte central do ETL atual;
- descrevem packages, classes ou diretorios que nao existem mais;
- tratam diagramas intermediarios como se fossem estado final;
- escondem mudancas posteriores de resiliencia, isolamento e reconciliacao do daemon.

## Documentos historicos que ainda ajudam

Os materiais abaixo nao sao oficiais, mas ainda ajudam em investigacoes:

- `docs/legado/pipelines-antigos/02-apis/graphql/**`
- `docs/legado/pipelines-antigos/02-apis/dataexport/**`
- `docs/legado/decisoes-antigas/direcionamento/verdades/VERDADES-OPERACIONAIS.md`
- `docs/legado/erros-conhecidos/analises/**`
- `docs/legado/erros-conhecidos/relatorio-validacao-etl-2026-03-05.md`

Use esses arquivos apenas para responder perguntas como:

- como o time chegou na arquitetura atual;
- quais bugs e travamentos motivaram retries, timeout guard, watchdog e isolamento;
- quais diferencas historicas existiram entre janelas operacionais e comportamento da origem.

## Regra final

Se houver qualquer duvida entre um arquivo legado e o sistema em execucao:

1. confie no codigo atual;
2. confirme em `docs/moderno`;
3. trate o legado apenas como contexto historico.
