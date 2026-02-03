# Requirements Document - Implementação Completa APIs Funcionais

## Introduction

Este documento especifica os requisitos para implementar e validar a extração completa de dados das 5 entidades funcionais do ESL Cloud: Fretes, Coletas (GraphQL), Cotações, Manifestos e Localização de Carga (Data Export). O objetivo é garantir 100% de funcionalidade, cobertura de dados e robustez operacional.

## Glossary

- **Sistema**: Extrator de Dados ESL Cloud (aplicação Java)
- **API GraphQL**: API do ESL Cloud que fornece dados de Fretes e Coletas
- **API Data Export**: API do ESL Cloud que fornece dados de Cotações, Manifestos e Localização de Carga
- **Entity**: Classe Java que representa uma tabela no banco de dados SQL Server
- **DTO**: Data Transfer Object - classe Java que representa dados da API
- **Mapper**: Classe responsável por converter DTO em Entity
- **Repository**: Classe responsável por persistir Entity no banco de dados
- **Runner**: Classe orquestradora que executa o fluxo completo de extração
- **Circuit Breaker**: Mecanismo de proteção que desabilita temporariamente uma entidade após falhas consecutivas
- **Janela de Tempo**: Período de 24 horas usado para filtrar dados (ontem até hoje)
- **Paginação**: Mecanismo de busca incremental de dados em múltiplas requisições
- **Log de Extração**: Registro persistido no banco com métricas de cada execução

## Requirements

### Requirement 1: Extração de Fretes via GraphQL

**User Story:** Como usuário do sistema, quero extrair dados de fretes das últimas 24 horas via API GraphQL, para que eu possa analisar operações de transporte recentes.

#### Acceptance Criteria

1. WHEN o sistema executa a extração de fretes, THE Sistema SHALL usar a query GraphQL `BuscarFretesExpandidaV3` com tipo `FreightBase`, buscar dados das últimas 24 horas usando o campo `serviceAt` com intervalo de datas (formato: `"{{data_inicio}} - {{data_fim}}"`)
2. WHEN o sistema executa paginação de fretes, THE Sistema SHALL usar `first: 100` (registros por página) e cursor `endCursor` com `hasNextPage`
3. WHEN a API retorna dados de fretes, THE Sistema SHALL mapear os 22 campos do CSV do FreteNodeDTO para FreteEntity (187 campos disponíveis na API FreightBase)
4. WHEN a API retorna dados de fretes, THE Sistema SHALL salvar o JSON completo no campo `metadata` para garantir completude
5. WHEN a paginação está ativa, THE Sistema SHALL processar até 500 páginas antes de interromper (proteção contra loop infinito)
6. WHEN a extração é concluída, THE Sistema SHALL registrar no log_extracoes: status (COMPLETO/INCOMPLETO), total de registros, páginas processadas e tempo de execução
7. WHEN ocorrem 5 falhas consecutivas, THE Sistema SHALL ativar o circuit breaker e interromper a extração
8. WHEN a extração é bem-sucedida, THE Sistema SHALL resetar o contador de falhas consecutivas para zero

_Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8_

---

### Requirement 2: Extração de Coletas via GraphQL

**User Story:** Como usuário do sistema, quero extrair dados de coletas das últimas 24 horas via API GraphQL, para que eu possa monitorar solicitações de coleta recentes.

#### Acceptance Criteria

1. WHEN o sistema executa a extração de coletas, THE Sistema SHALL usar a query GraphQL `BuscarColetasExpandidaV2` com tipo `Pick`, buscar dados de 2 dias separadamente (ontem e hoje) usando o campo `requestDate` (formato: `"{{data_inicio}}"`)
2. WHEN o sistema executa paginação de coletas, THE Sistema SHALL usar `first: 100` (registros por página) e cursor `endCursor` com `hasNextPage`
3. WHEN a API retorna dados de coletas, THE Sistema SHALL mapear os 22 campos do CSV do ColetaNodeDTO para ColetaEntity (44 campos disponíveis na API Pick)
4. WHEN a API retorna dados de coletas, THE Sistema SHALL salvar o JSON completo no campo `metadata` para garantir completude
5. WHEN ambas as buscas (ontem + hoje) são concluídas com sucesso, THE Sistema SHALL consolidar os resultados em uma única lista
6. WHEN qualquer uma das buscas falha ou é interrompida, THE Sistema SHALL marcar o resultado consolidado como INCOMPLETO
7. WHEN a paginação está ativa, THE Sistema SHALL processar até 500 páginas por dia antes de interromper
8. WHEN a extração é concluída, THE Sistema SHALL registrar no log_extracoes: status, total de registros consolidados, páginas processadas e tempo de execução

_Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8_

---

### Requirement 3: Extração de Cotações via Data Export

**User Story:** Como usuário do sistema, quero extrair dados de cotações das últimas 24 horas via API Data Export, para que eu possa analisar propostas de frete recentes.

#### Acceptance Criteria

1. WHEN o sistema executa a extração de cotações, THE Sistema SHALL usar o Template ID 6906 e o campo de data `search.quotes.requested_at` (formato: `"{{data_inicio}} - {{data_fim}}"`)
2. WHEN o sistema executa paginação de cotações, THE Sistema SHALL usar `per: "1000"` (registros por página)
3. WHEN a API retorna dados de cotações, THE Sistema SHALL mapear os 36 campos do CSV do CotacaoDTO para CotacaoEntity (37 chaves na API)
4. WHEN a API retorna dados de cotações, THE Sistema SHALL salvar o JSON completo no campo `metadata` para garantir completude
5. WHEN a paginação está ativa, THE Sistema SHALL processar até 200 páginas antes de interromper (proteção contra loop infinito)
6. WHEN a extração é concluída, THE Sistema SHALL registrar no log_extracoes: status, total de registros, páginas processadas e tempo de execução
7. WHEN ocorrem 5 falhas consecutivas, THE Sistema SHALL ativar o circuit breaker e interromper a extração
8. WHEN a extração é bem-sucedida, THE Sistema SHALL resetar o contador de falhas consecutivas para zero

_Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8_

---

### Requirement 4: Extração de Manifestos via Data Export

**User Story:** Como usuário do sistema, quero extrair dados de manifestos das últimas 24 horas via API Data Export, para que eu possa acompanhar documentos de transporte recentes.

#### Acceptance Criteria

1. WHEN o sistema executa a extração de manifestos, THE Sistema SHALL usar o Template ID 6399 e o campo de data `search.manifests.service_date` (formato: `"{{data_inicio}} - {{data_fim}}"`)
2. WHEN o sistema executa paginação de manifestos, THE Sistema SHALL usar `per: "10000"` (registros por página)
3. WHEN a API retorna dados de manifestos, THE Sistema SHALL mapear os 80 campos do CSV do ManifestoDTO para ManifestoEntity (92 chaves na API)
4. WHEN a API retorna dados de manifestos, THE Sistema SHALL salvar o JSON completo no campo `metadata` para garantir completude
5. WHEN a paginação está ativa, THE Sistema SHALL processar até 200 páginas antes de interromper (proteção contra loop infinito)
6. WHEN a extração é concluída, THE Sistema SHALL registrar no log_extracoes: status, total de registros, páginas processadas e tempo de execução
7. WHEN ocorrem 5 falhas consecutivas, THE Sistema SHALL ativar o circuit breaker e interromper a extração
8. WHEN a extração é bem-sucedida, THE Sistema SHALL resetar o contador de falhas consecutivas para zero

_Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8_

---

### Requirement 5: Extração de Localização de Carga via Data Export

**User Story:** Como usuário do sistema, quero extrair dados de localização de carga das últimas 24 horas via API Data Export, para que eu possa rastrear o status de entregas recentes.

#### Acceptance Criteria

1. WHEN o sistema executa a extração de localização de carga, THE Sistema SHALL usar o Template ID 8656 e o campo de data `search.freights.service_at` (formato: `"{{data_inicio}} - {{data_fim}}"`)
2. WHEN o sistema executa paginação de localização de carga, THE Sistema SHALL usar `per: "10000"` (registros por página)
3. WHEN a API retorna dados de localização, THE Sistema SHALL mapear os 17 campos do CSV do LocalizacaoCargaDTO para LocalizacaoCargaEntity (17 chaves na API)
4. WHEN a API retorna dados de localização, THE Sistema SHALL salvar o JSON completo no campo `metadata` para garantir completude
5. WHEN a paginação está ativa, THE Sistema SHALL processar até 200 páginas antes de interromper (proteção contra loop infinito)
6. WHEN a extração é concluída, THE Sistema SHALL registrar no log_extracoes: status, total de registros, páginas processadas e tempo de execução
7. WHEN ocorrem 5 falhas consecutivas, THE Sistema SHALL ativar o circuit breaker e interromper a extração
8. WHEN a extração é bem-sucedida, THE Sistema SHALL resetar o contador de falhas consecutivas para zero

_Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 5.8_

---

### Requirement 6: Validação e Auditoria de Dados

**User Story:** Como usuário do sistema, quero que os dados extraídos sejam validados automaticamente, para que eu possa confiar na integridade das informações.

#### Acceptance Criteria

1. WHEN o sistema salva dados no banco, THE Sistema SHALL usar operação MERGE para evitar duplicatas baseado em chaves únicas
2. WHEN o sistema detecta campos obrigatórios ausentes, THE Sistema SHALL registrar warning no log mas continuar o processamento
3. WHEN o sistema completa uma extração, THE Sistema SHALL validar que o número de registros salvos corresponde ao número de DTOs processados
4. WHEN há discrepância entre DTOs processados e registros salvos, THE Sistema SHALL registrar warning com detalhes da diferença
5. WHEN o sistema executa auditoria, THE Sistema SHALL verificar completude de dados comparando com contagens da API
6. WHEN a auditoria detecta incompletude, THE Sistema SHALL gerar relatório detalhado com entidades afetadas e percentual de cobertura

_Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6_

---

### Requirement 7: Circuit Breaker e Proteções

**User Story:** Como administrador do sistema, quero que o sistema tenha proteções contra falhas e loops infinitos, para que a aplicação seja robusta e confiável.

#### Acceptance Criteria

1. WHEN uma entidade falha 5 vezes consecutivamente, THE Sistema SHALL ativar o circuit breaker e desabilitar temporariamente a extração dessa entidade
2. WHEN o circuit breaker está ativo, THE Sistema SHALL registrar warning no log e retornar resultado vazio sem tentar nova requisição
3. WHEN uma extração atinge o limite de 500 páginas (GraphQL) ou 200 páginas (Data Export), THE Sistema SHALL interromper e marcar como INCOMPLETO
4. WHEN uma extração atinge o limite de 50.000 registros (GraphQL) ou 10.000 registros (Data Export), THE Sistema SHALL interromper e marcar como INCOMPLETO
5. WHEN uma extração é bem-sucedida após falhas anteriores, THE Sistema SHALL resetar o circuit breaker e permitir novas tentativas
6. WHEN o sistema detecta loop infinito (mesma página retornando dados repetidamente), THE Sistema SHALL interromper após 3 iterações idênticas

_Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6_

---

### Requirement 8: Logging e Monitoramento

**User Story:** Como administrador do sistema, quero logs detalhados de todas as operações, para que eu possa monitorar e diagnosticar problemas facilmente.

#### Acceptance Criteria

1. WHEN o sistema inicia uma extração, THE Sistema SHALL registrar log INFO com: entidade, janela de tempo e configurações
2. WHEN o sistema processa uma página, THE Sistema SHALL registrar log DEBUG com: número da página, registros encontrados e cursor de paginação
3. WHEN o sistema completa uma extração, THE Sistema SHALL registrar log INFO com: status final, total de registros, páginas processadas e tempo de execução
4. WHEN o sistema detecta erro ou warning, THE Sistema SHALL registrar log WARN ou ERROR com: mensagem detalhada, stack trace e contexto da operação
5. WHEN o sistema ativa proteções (circuit breaker, limite de páginas), THE Sistema SHALL registrar log WARN com: motivo da ativação e ação tomada
6. WHEN o sistema salva dados no banco, THE Sistema SHALL registrar log INFO com: entidade, registros salvos e tempo de persistência
7. WHEN o sistema completa uma extração, THE Sistema SHALL persistir registro em log_extracoes com: entidade, timestamps, status, métricas e mensagem

_Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 8.6, 8.7_

---

### Requirement 9: Configuração de Janela de Tempo

**User Story:** Como usuário do sistema, quero configurar a janela de tempo de extração, para que eu possa controlar o período de dados a ser extraído.

#### Acceptance Criteria

1. WHEN o sistema calcula a janela de extração, THE Sistema SHALL usar timezone America/Sao_Paulo por padrão
2. WHEN o sistema calcula a janela de extração, THE Sistema SHALL buscar dados desde 24 horas atrás até o momento atual
3. WHEN o usuário fornece data de referência via parâmetro, THE Sistema SHALL usar essa data como fim do intervalo (últimas 24h a partir dela)
4. WHEN o sistema formata datas para GraphQL, THE Sistema SHALL usar formato yyyy-MM-dd
5. WHEN o sistema formata datas para Data Export, THE Sistema SHALL usar formato yyyy-MM-dd - yyyy-MM-dd (intervalo)
6. WHEN o sistema calcula intervalo para Coletas, THE Sistema SHALL buscar 2 dias separadamente (ontem e hoje) devido a limitação da API

_Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6_

---

### Requirement 10: Retry com Exponential Backoff

**User Story:** Como usuário do sistema, quero que o sistema tente novamente em caso de falhas temporárias, para que eu tenha maior taxa de sucesso nas extrações.

#### Acceptance Criteria

1. WHEN uma requisição falha com status 503 (Service Unavailable), THE Sistema SHALL tentar novamente até 3 vezes
2. WHEN uma requisição falha com timeout, THE Sistema SHALL tentar novamente até 3 vezes
3. WHEN o sistema executa retry, THE Sistema SHALL usar backoff exponencial: 2s, 4s, 8s entre tentativas
4. WHEN o sistema executa retry, THE Sistema SHALL registrar log DEBUG com: número da tentativa, motivo e tempo de espera
5. WHEN todas as tentativas falham, THE Sistema SHALL registrar log ERROR e incrementar contador de falhas consecutivas
6. WHEN uma tentativa de retry é bem-sucedida, THE Sistema SHALL registrar log INFO e resetar contador de falhas

_Requirements: 10.1, 10.2, 10.3, 10.4, 10.5, 10.6_

---

### Requirement 11: Validação de Schema

**User Story:** Como desenvolvedor do sistema, quero que o sistema valide o schema dos dados retornados, para que eu possa detectar mudanças na API rapidamente.

#### Acceptance Criteria

1. WHEN a API retorna dados, THE Sistema SHALL validar que o JSON corresponde ao DTO esperado
2. WHEN campos esperados estão ausentes no JSON, THE Sistema SHALL registrar warning com: entidade, campos faltantes e ID do registro
3. WHEN campos esperados estão ausentes, THE Sistema SHALL continuar processamento usando campos disponíveis
4. WHEN campos extras estão presentes no JSON, THE Sistema SHALL ignorá-los silenciosamente (compatibilidade futura)
5. WHEN o tipo de dado de um campo não corresponde ao esperado, THE Sistema SHALL registrar warning e usar valor padrão (null, 0, "")
6. WHEN mais de 10% dos registros têm campos faltantes, THE Sistema SHALL registrar ERROR e marcar extração como INCOMPLETO

_Requirements: 11.1, 11.2, 11.3, 11.4, 11.5, 11.6_
