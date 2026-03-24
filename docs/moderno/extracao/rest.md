---
context:
  - ETL
  - REST
  - Extracao
updated_at: 2026-03-18T00:00:32.1501533-03:00
source_of_truth: mixed
classification: atual
related_files:
  - src/main/java/br/com/extrator/suporte/configuracao/ConfigApi.java
  - src/main/resources/config.properties
  - config/.env.example
  - docs/legado/pipelines-antigos/02-apis
---

# REST no contexto moderno

## Status oficial

REST nao faz parte do pipeline moderno oficial de extracao.

Essa afirmacao e deliberada e importante porque o repositorio ainda possui:

- token REST no `.env.example`;
- propriedades `api.rest.*`;
- material antigo de descoberta, Insomnia e especificacao;
- referencias residuais em validacoes historicas.

Nada disso muda o fato principal:

- o ETL moderno suportado em producao gira em torno de GraphQL e DataExport.

## Por que este arquivo existe

Sem um documento explicito, a mera presenca de `API_REST_TOKEN` e de dezenas de arquivos antigos induz a leitura errada de que REST ainda e uma trilha oficial de carga.

Este arquivo existe para encerrar essa ambiguidade.

## O que ainda resta de REST no codigo

- configuracao de token e timeout;
- banner e material auxiliar;
- referencias historicas em documentacao e validadores antigos;
- possivel uso manual em investigacoes ou testes fora do pipeline moderno.

## O que nao deve ser assumido

Nao assuma que:

- existe step REST no `PipelineOrchestrator`;
- existe comando moderno de carga REST equivalente a GraphQL/DataExport;
- a documentacao antiga de REST descreve o runtime atual;
- tabelas ou DTOs historicos de REST ainda definem o modelo operacional principal.

## Regra para manutencao futura

Se REST voltar a ser parte do ETL oficial, isso deve acontecer por:

1. implementacao real no codigo atual;
2. integracao explicita no `CommandRegistry`, use cases e pipeline;
3. nova documentacao moderna;
4. promocao controlada do conteudo hoje arquivado.

Enquanto isso nao acontecer, REST deve ser tratado como:

- contexto legado;
- material de descoberta;
- apoio manual;
- nao source of truth da execucao moderna.
