# Documentação de Pacotes

Este documento consolida a documentação dos principais pacotes do código.

## br.com.extrator.api
- Clientes de API para ESL Cloud:
  - `ClienteApiGraphQL.java`: busca coletas e fretes via GraphQL com paginação e pausa entre requisições.
  - `ClienteApiRest.java`: busca faturas e ocorrências via endpoints REST com paginação.
  - `ClienteApiDataExport.java`: exporta e processa Excel/CSV, converte para entidades dinâmicas.
  - `PaginatedGraphQLResponse.java`: resposta paginada para queries GraphQL.
- Padrões: retry/throttling via utilitário, validação de acesso, logging estruturado.

## br.com.extrator.modelo
- Modelos de dados:
  - `EntidadeDinamica.java`: entidade genérica com mapeamento dinâmico de campos e conversões tipadas (String, Integer, Double, Boolean).
- Integração: `@JsonAnyGetter/@JsonAnySetter` para serialização/deserialização dinâmica.

## br.com.extrator.util
- Utilitários:
  - `CarregadorConfig.java`: centraliza carregamento de `config.properties` e variáveis de ambiente; valida conexão com DB.
  - `GerenciadorRequisicaoHttp.java`: gerenciamento centralizado de requisições HTTP com throttling, retry com backoff exponencial + jitter para HTTP 429; suporte a respostas texto e binárias.

Para detalhes de arquitetura e guias, consulte:
- `../arquitetura/README.md`
- `../guias/README.md`