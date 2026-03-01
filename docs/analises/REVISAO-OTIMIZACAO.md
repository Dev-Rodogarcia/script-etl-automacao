# Auditoria de Código e Arquitetura — ESL Cloud Extrator v2.1

## Objetivo
- Identificar vícios, repetições e oportunidades de otimização sem alterar comportamentos nem remover código crítico.
- Propor melhorias incrementais e seguras, focadas em reutilização e padronização.

## Principais Achados
- Mappers repetem criação de `ObjectMapper` e serialização de metadados.
  - `src/main/java/br/com/extrator/modelo/graphql/coletas/ColetaMapper.java:26-29`
  - `src/main/java/br/com/extrator/modelo/graphql/fretes/FreteMapper.java:28-31`
  - `src/main/java/br/com/extrator/modelo/dataexport/manifestos/ManifestoMapper.java:25-28`
  - `src/main/java/br/com/extrator/modelo/dataexport/cotacao/CotacaoMapper.java:27-30`
  - `src/main/java/br/com/extrator/modelo/dataexport/faturaporcliente/FaturaPorClienteMapper.java:31-35`
- Conversão de data/hora e números está duplicada e com abordagens diferentes entre mappers.
  - Hora ISO/HH:mm: `src/main/java/br/com/extrator/modelo/graphql/coletas/ColetaMapper.java:146-204`
  - Data/hora GraphQL: `src/main/java/br/com/extrator/modelo/graphql/fretes/FreteMapper.java:196-214`
  - Data/hora Manifestos: `src/main/java/br/com/extrator/modelo/dataexport/manifestos/ManifestoMapper.java:120-147`
  - BigDecimal (Locale US): `src/main/java/br/com/extrator/modelo/dataexport/contasapagar/ContasAPagarMapper.java:136-162`, `src/main/java/br/com/extrator/modelo/dataexport/faturaporcliente/FaturaPorClienteMapper.java:151-163`
- Repositórios duplicam utilitários de schema.
  - Adição de coluna se não existir:
    - `src/main/java/br/com/extrator/db/repository/ManifestoRepository.java:261-268`
    - `src/main/java/br/com/extrator/db/repository/ColetaRepository.java:229-238`
  - Truncamento de strings (apenas em Manifestos): `src/main/java/br/com/extrator/db/repository/ManifestoRepository.java:906-914`
- Pontos menores:
  - Query GraphQL com campo repetido: `src/main/java/br/com/extrator/api/ClienteApiGraphQL.java:354-355` (freightWeightSubtotal)
  - Mapper placeholder vazio: `src/main/java/br/com/extrator/modelo/dataexport/ocorrencias/OcorrenciaMapper.java:1-4`
  - Inconsistência de registro do `JavaTimeModule` em `FaturaPorClienteMapper`: `src/main/java/br/com/extrator/modelo/dataexport/faturaporcliente/FaturaPorClienteMapper.java:31-35`

## Recomendações (não quebrantes)
- Centralizar JSON e metadados.
  - Criar `br.com.extrator.util.MapperUtil` com:
    - `sharedJson()` retornando `ObjectMapper` com `JavaTimeModule`.
    - `toJson(Object)` com fallback para erro controlado.
- Unificar parsing de datas e números.
  - `br.com.extrator.util.DataUtil`: `parseLocalDate(String)`, `parseOffsetDateTime(String)` com null-safety e logs.
  - `br.com.extrator.util.NumeroUtil`: `parseBigDecimalUS(String)` via `DecimalFormat` e `Locale.US`.
  - `br.com.extrator.util.HorarioUtil`: `normalizarHora(String)` reutilizando a regra de `ColetaMapper`.
- Mover utilitários comuns para o repositório base.
  - Adicionar em `AbstractRepository`: `adicionarColunaSeNaoExistir(Connection, tabela, coluna, definicao)` e `truncate(String, maxLen)`.
- Padronizar logs de mappers.
  - Mesma mensagem/nível para conversões e serialização de metadados em todos os mappers.
- Corrigir pequeno detalhe na query GraphQL.
  - Remover a duplicidade de `freightWeightSubtotal` para clareza (`ClienteApiGraphQL.buscarFretes`).

## Benefícios Esperados
- Redução de código duplicado (20–30%) e menor risco de inconsistência.
- Facilita manutenção e onboarding; pontos de conversão e serialização ficam em um só lugar.
- Repositórios mais enxutos e com utilidades compartilhadas.

## Plano de Implementação Sugerido
- Fase 1: Introduzir `MapperUtil`, `DataUtil`, `NumeroUtil`, `HorarioUtil` sem alterar mappers.
- Fase 2: Migrar mappers para usar utilidades (uma classe por vez, com build/teste entre passos).
- Fase 3: Extrair utilitários para `AbstractRepository` e usar em `ColetaRepository` e `ManifestoRepository`.
- Fase 4: Correção cosmética na query GraphQL e padronização de logs.

## Compatibilidade e Segurança
- Manter assinaturas públicas de `toEntity(...)` inalteradas.
- Preservar o comportamento de MERGE e chaves; não tocar em DDL crítico.
- Não remover classes; apenas reutilizar utilidades.

## Checklist de Ação
- [ ] Criar `MapperUtil` e adotar nos mappers.
- [ ] Criar `DataUtil`/`NumeroUtil`/`HorarioUtil` e adotar.
- [ ] Extrair `adicionarColunaSeNaoExistir`/`truncate` para `AbstractRepository`.
- [ ] Corrigir duplicidade em `ClienteApiGraphQL.buscarFretes`.
- [ ] Alinhar logs de conversão/metadata em todos os mappers.

## Observações Finais
- O projeto está bem estruturado (orquestrador + runners + mappers + repositories) e com proteções robustas de API (throttling, retry, circuit breaker).
- As melhorias propostas são incrementais e visam reduzir manutenção sem alterar a lógica de negócio já validada.

