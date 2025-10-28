# Arquitetura

Este documento consolida a visão técnica e a arquitetura de arquivos.

## Visão Técnica
- Stack: Java 17, Maven, SLF4J/Logback, Jackson, Apache POI, JDBC SQL Server.
- Módulos principais: APIs (REST, GraphQL, DataExport), Serviços de extração, Auditoria, Persistência (DB), Utilitários, Modelos dinâmicos.
- Configuração: Variáveis de ambiente para dados sensíveis; `config.properties` para opcionais.

## Estrutura de Arquivos
- `src/main/java/br/com/extrator/`: código fonte principal, organizado por pacotes.
- `src/main/resources/logback.xml`: configuração de logging.
- `docs/`: documentação consolidada e relatórios.
- `.bat`: automações e execução de rotinas.

## Fluxos
- Extração: serviços acionam clientes de API com utilitários de retry/throttling.
- Persistência: dados validados/auditados e gravados via utilitários de DB.
- Auditoria: relatórios e validações sobre inconsistências.

## Padrões
- Retentativas: backoff exponencial com jitter para HTTP 429.
- Serialização dinâmica: `EntidadeDinamica` com `@JsonAnyGetter/@JsonAnySetter`.
- Logging estruturado com SLF4J.

## Configurações
- Ambiente: `API_*`, `DB_*` obrigatoriamente via env.
- `config.properties`: endpoints e ajustes de retry/throttling com defaults.

Para detalhes adicionais, veja `docs/guias/README.md` e `docs/pacotes/README.md`.