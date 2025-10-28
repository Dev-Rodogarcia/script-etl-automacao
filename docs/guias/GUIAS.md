# Guias e Instruções

Este documento consolida instruções de uso e operação.

## Execução
- `01_extrair_dados_24h.bat`: extrai dados do período das últimas 24h.
- `02_extrair_dados_por_data.bat`: extrai dados para intervalo informado.
- `03_testar_api_24h.bat`: testa conectividade e respostas da API no período 24h.
- `04_testar_api_por_data.bat`: testa conectividade em intervalo informado.
- `05_auditar_dados_24h.bat`: executa auditorias sobre dados das últimas 24h.

## Configuração
- Variáveis de ambiente obrigatórias: `API_REST_TOKEN`, `API_GRAPHQL_TOKEN`, `API_DATAEXPORT_TOKEN`, `DB_URL`, `DB_USER`, `DB_PASSWORD`.
- `config.properties` (opcional): `api.baseurl`, `api.graphql.endpoint`, `api.retry.*`, `api.corporation.id`.

## Logs
- `src/main/resources/logback.xml` configura níveis e appenders.

## Troubleshooting
- Tokens ausentes: configure env; falha fail-fast.
- Erros 429: utilitário de retry aplica backoff com jitter.
- Conexão DB: valide com `CarregadorConfig.validarConexaoBancoDados()`.

Para detalhes técnicos, veja `docs/arquitetura/README.md`.