# Design Document - Implementação Completa APIs Funcionais

## Overview

Este documento detalha o design técnico para implementar a extração completa de dados das 5 entidades funcionais do ESL Cloud. O sistema já possui a estrutura base implementada (Runners, Clientes API, Entities, Repositories), mas precisa de ajustes e validações para garantir 100% de funcionalidade.

### Status de Implementação

**✅ Já Implementado:**
- Retry com exponential backoff (2s, 4s, 8s) via `GerenciadorRequisicaoHttp`
- Circuit breaker (5 falhas consecutivas)
- Paginação automática (GraphQL com cursor, Data Export com páginas)
- Logging estruturado (INFO, DEBUG, WARN, ERROR)
- Throttling e rate limiting
- Métodos de contagem (`obterContagemFretes`, `obterContagemColetas`, etc.)
- MERGE (UPSERT) nos repositories
- Preservação de dados completos via campo `metadata` (JSON)

**⚠️ Pendente de Implementação:**
- Validação de schema JSON antes de desserializar
- Validação de completude (DTOs processados vs registros salvos)
- Detecção de loop infinito melhorada (mesma página/cursor repetido)
- Padronização de timezone `America/Sao_Paulo` em todos os clientes
- Validação de campos obrigatórios antes de salvar

### Objetivos do Design

1. **Validar e corrigir** a implementação existente dos Runners (GraphQLRunner e DataExportRunner)
2. **Garantir mapeamento completo** de todos os campos DTO → Entity
3. **Implementar validações** de integridade e completude de dados
4. ~~**Adicionar retry e backoff** para maior resiliência~~ ✅ **JÁ IMPLEMENTADO**
5. **Melhorar logging** para facilitar monitoramento e troubleshooting

### Escopo

**Incluído:**
- Fretes e Coletas (API GraphQL)
- Cotações, Manifestos e Localização de Carga (API Data Export)
- Validação de schema e dados
- Circuit breaker e proteções
- Logging estruturado

**Excluído:**
- Faturas a Pagar, Faturas a Receber e Ocorrências (API REST Reports - aguardando solução de autenticação)

## Architecture

### Visão Geral da Arquitetura

```
┌─────────────────────────────────────────────────────────────┐
│                         Main.java                            │
│                  (Orquestrador Principal)                    │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ├──────────────┬──────────────┐
                         │              │              │
                         ▼              ▼              ▼
              ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
              │ GraphQLRunner│  │DataExportRun │  │  RestRunner  │
              │              │  │              │  │  (Desabilitado)│
              └──────┬───────┘  └──────┬───────┘  └──────────────┘
                     │                 │
                     ▼                 ▼
          ┌──────────────────┐  ┌──────────────────┐
          │ClienteApiGraphQL │  │ClienteApiDataExp │
          └──────┬───────────┘  └──────┬───────────┘
                 │                     │
                 ▼                     ▼
          ┌──────────────────────────────────┐
          │   GerenciadorRequisicaoHttp      │
          │  (Throttling + Rate Limiting)    │
          └──────────────────────────────────┘
```

### Fluxo de Dados

```
API → DTO → Mapper → Entity → Repository → SQL Server
                                    ↓
                              LogExtracaoEntity
```

## Components and Interfaces

### 1. GraphQLRunner

**Responsabilidade:** Orquestrar extração de Fretes e Coletas

**Métodos Principais:**

```java
public static void executar(LocalDate dataInicio) throws Exception
```

**Fluxo de Execução:**
1. Validar conexão com banco de dados
2. Inicializar clientes, repositories e mappers
3. Extrair Coletas (2 dias separadamente)
4. Aguardar 2 segundos (throttling)
5. Extrair Fretes (intervalo de 24h)
6. Registrar logs de extração
7. Tratar exceções e registrar erros

**Melhorias Necessárias:**
- ✅ Já implementado: busca de 2 dias para Coletas
- ✅ Já implementado: logging estruturado
- ✅ Já implementado: retry com backoff exponencial (via GerenciadorRequisicaoHttp)
- ⚠️ Adicionar: validação de completude de dados

---

### 2. DataExportRunner

**Responsabilidade:** Orquestrar extração de Cotações, Manifestos e Localização de Carga

**Métodos Principais:**

```java
public static void executar(LocalDate dataInicio) throws Exception
```

**Fluxo de Execução:**
1. Validar conexão com banco de dados
2. Inicializar clientes, repositories e mappers
3. Extrair Manifestos (últimas 24h)
4. Aguardar 2 segundos (throttling)
5. Extrair Cotações (últimas 24h)
6. Aguardar 2 segundos (throttling)
7. Extrair Localização de Carga (últimas 24h)
8. Registrar logs de extração
9. Tratar exceções e registrar erros

**Melhorias Necessárias:**
- ✅ Já implementado: logging estruturado
- ✅ Já implementado: circuit breaker
- ✅ Já implementado: retry com backoff exponencial (via GerenciadorRequisicaoHttp)
- ⚠️ Adicionar: validação de completude de dados

---

### 3. ClienteApiGraphQL

**Responsabilidade:** Comunicação com API GraphQL (Fretes e Coletas)

**Endpoint:** `POST {{base_url}}/graphql`

**Autenticação:** `Bearer {{token_graphql}}`

**Métodos Principais:**

```java
public ResultadoExtracao<FreteNodeDTO> buscarFretes(LocalDate dataReferencia)
public ResultadoExtracao<ColetaNodeDTO> buscarColetas(LocalDate dataReferencia)
public int obterContagemFretes(LocalDate dataReferencia)
public int obterContagemColetas(LocalDate dataReferencia)
private <T> ResultadoExtracao<T> executarQueryPaginada(...)
```

**Configuração de Fretes:**
- **Tipo GraphQL:** `FreightBase`
- **Query:** `BuscarFretesExpandidaV3`
- **Campo de Filtro de Data:** `serviceAt` (formato: `"{{data_inicio}} - {{data_fim}}"`)
- **Paginação:** `first: 100` (registros por página)
- **Cursor:** `endCursor` e `hasNextPage` (paginação automática)
- **Cobertura:** 22 campos do CSV mapeados (187 campos disponíveis na API)

**Configuração de Coletas:**
- **Tipo GraphQL:** `Pick`
- **Query:** `BuscarColetasExpandidaV2`
- **Campo de Filtro de Data:** `requestDate` (formato: `"{{data_inicio}}"`)
- **Paginação:** `first: 100` (registros por página)
- **Cursor:** `endCursor` e `hasNextPage` (paginação automática)
- **Cobertura:** 22 campos do CSV mapeados (44 campos disponíveis na API)

**Características Implementadas:**
- ✅ Paginação automática com cursor
- ✅ Circuit breaker (5 falhas consecutivas)
- ✅ Proteção contra loop infinito (500 páginas, 50k registros)
- ✅ Logging estruturado (INFO, DEBUG, WARN, ERROR)
- ✅ Throttling via GerenciadorRequisicaoHttp
- ✅ Retry com backoff exponencial (via GerenciadorRequisicaoHttp: 2s, 4s, 8s para 503, timeout, IOException)
- ✅ Métodos de contagem: `obterContagemFretes()`, `obterContagemColetas()`

**Melhorias Necessárias:**
- ⚠️ Adicionar: validação de schema JSON antes de desserializar
- ⚠️ Adicionar: detecção de loop infinito melhorada (mesma página/cursor repetido após 3 iterações)
- ⚠️ Adicionar: padronizar timezone `America/Sao_Paulo` (atualmente só ClienteApiRest usa)

---

### 4. ClienteApiDataExport

**Responsabilidade:** Comunicação com API Data Export (Cotações, Manifestos, Localização)

**Endpoint:** `GET {{base_url}}/api/analytics/reports/{template_id}/data`

**Autenticação:** `Bearer {{token_dataexport}}`

**Métodos Principais:**

```java
public ResultadoExtracao<ManifestoDTO> buscarManifestos()
public ResultadoExtracao<CotacaoDTO> buscarCotacoes()
public ResultadoExtracao<LocalizacaoCargaDTO> buscarLocalizacaoCarga()
public int obterContagemManifestos(LocalDate dataReferencia)
public int obterContagemCotacoes(LocalDate dataReferencia)
public int obterContagemLocalizacoesCarga(LocalDate dataReferencia)
private <T> ResultadoExtracao<T> buscarDadosGenericos(...)
```

**Configuração de Cotações:**
- **Template ID:** `6906`
- **Campo de Filtro de Data:** `search.quotes.requested_at` (formato: `"{{data_inicio}} - {{data_fim}}"`)
- **Paginação:** `per: "1000"` (registros por página)
- **Cobertura:** 36 campos do CSV mapeados (37 chaves na API)

**Configuração de Manifestos:**
- **Template ID:** `6399`
- **Campo de Filtro de Data:** `search.manifests.service_date` (formato: `"{{data_inicio}} - {{data_fim}}"`)
- **Paginação:** `per: "10000"` (registros por página)
- **Cobertura:** 80 campos do CSV mapeados (92 chaves na API)

**Configuração de Localização de Carga:**
- **Template ID:** `8656`
- **Campo de Filtro de Data:** `search.freights.service_at` (formato: `"{{data_inicio}} - {{data_fim}}"`)
- **Paginação:** `per: "10000"` (registros por página)
- **Cobertura:** 17 campos do CSV mapeados (17 chaves na API)

**Características Implementadas:**
- ✅ Paginação automática
- ✅ Circuit breaker (5 falhas consecutivas)
- ✅ Proteção contra loop infinito (200 páginas, 10k registros)
- ✅ Logging estruturado
- ✅ Throttling via GerenciadorRequisicaoHttp
- ✅ Retry com backoff exponencial (via GerenciadorRequisicaoHttp: 2s, 4s, 8s para 503, timeout, IOException)
- ✅ Métodos de contagem: `obterContagemManifestos()`, `obterContagemCotacoes()`, `obterContagemLocalizacoesCarga()`

**Melhorias Necessárias:**
- ⚠️ Adicionar: validação de schema JSON antes de desserializar
- ⚠️ Adicionar: detecção de loop infinito melhorada (mesma página repetida após 3 iterações)
- ⚠️ Adicionar: padronizar timezone `America/Sao_Paulo` (atualmente só ClienteApiRest usa)

---

### 5. Mappers (DTO → Entity)

**Responsabilidade:** Converter DTOs da API em Entities do banco

**Classes:**
- `FreteMapper`: FreteNodeDTO → FreteEntity
- `ColetaMapper`: ColetaNodeDTO → ColetaEntity
- `CotacaoMapper`: CotacaoDTO → CotacaoEntity
- `ManifestoMapper`: ManifestoDTO → ManifestoEntity
- `LocalizacaoCargaMapper`: LocalizacaoCargaDTO → LocalizacaoCargaEntity

**Padrão de Implementação:**

```java
public class FreteMapper {
    public FreteEntity toEntity(FreteNodeDTO dto) {
        FreteEntity entity = new FreteEntity();
        
        // Mapear campos individuais
        entity.setId(dto.getId());
        entity.setReferenceNumber(dto.getReferenceNumber());
        // ... (22 campos do CSV mapeados, 187 campos disponíveis na API FreightBase)
        
        // Salvar JSON completo para garantir completude
        entity.setMetadata(serializarParaJson(dto));
        
        // Timestamp de extração
        entity.setDataExtracao(LocalDateTime.now());
        
        return entity;
    }
}
```

**Características Implementadas:**
- ✅ Mapeamento de campos essenciais (id, status, datas, valores principais)
- ✅ Preservação de 100% dos dados originais via campo `metadata` (JSON completo)
- ✅ Conversão segura de tipos (String → OffsetDateTime, String → LocalDate)
- ✅ Tratamento de erros de parsing com logging detalhado

**Melhorias Necessárias:**
- ⚠️ Validar: todos os campos estão mapeados explicitamente? (atualmente usa campos essenciais + metadata)
- ⚠️ Adicionar: validação de campos obrigatórios antes de salvar
- ⚠️ Adicionar: validação de schema JSON antes de desserializar

---

### 6. Repositories (Entity → SQL Server)

**Responsabilidade:** Persistir Entities no banco de dados

**Classes:**
- `FreteRepository`
- `ColetaRepository`
- `CotacaoRepository`
- `ManifestoRepository`
- `LocalizacaoCargaRepository`

**Padrão de Implementação:**

```java
public class FreteRepository extends AbstractRepository<FreteEntity> {
    
    public int salvar(List<FreteEntity> entities) {
        // Usar MERGE para evitar duplicatas
        String sql = """
            MERGE INTO fretes AS target
            USING (VALUES (?, ?, ...)) AS source (id, reference_number, ...)
            ON target.id = source.id
            WHEN MATCHED THEN UPDATE SET ...
            WHEN NOT MATCHED THEN INSERT ...
        """;
        
        // Executar em batch para performance
        return executarBatch(sql, entities);
    }
}
```

**Características Implementadas:**
- ✅ MERGE para evitar duplicatas
- ✅ Batch insert para performance
- ✅ Tratamento de exceções SQL

**Melhorias Necessárias:**
- ⚠️ Adicionar: validação de registros salvos vs processados
- ⚠️ Adicionar: logging de discrepâncias

---

### 7. GerenciadorRequisicaoHttp

**Responsabilidade:** Gerenciar requisições HTTP com throttling e rate limiting

**Características Implementadas:**
- ✅ Throttling: 1 requisição por segundo (configurável via `CarregadorConfig`)
- ✅ Rate limiting: máximo de requisições por minuto
- ✅ Logging de requisições (INFO, DEBUG, WARN, ERROR)
- ✅ Tratamento de timeouts com retry
- ✅ Retry com backoff exponencial: 2s, 4s, 8s (configurável via `CarregadorConfig`)
- ✅ Detecção e retry automático para status 503 (Service Unavailable)
- ✅ Detecção e retry automático para status 5xx (erros de servidor)
- ✅ Detecção e retry automático para IOException e HttpTimeoutException
- ✅ Retry seletivo: não retenta para 404, 401, 403 (erros definitivos)

**Melhorias Necessárias:**
- Nenhuma - componente completo e funcional

---

### 8. LogExtracaoEntity e LogExtracaoRepository

**Responsabilidade:** Registrar métricas de cada extração

**Campos:**
- `entidade`: Nome da entidade (fretes, coletas, etc.)
- `dataHoraInicio`: Timestamp de início
- `dataHoraFim`: Timestamp de fim
- `status`: COMPLETO, INCOMPLETO, ERRO_API
- `registrosProcessados`: Total de registros extraídos
- `paginasProcessadas`: Total de páginas processadas
- `mensagem`: Detalhes adicionais

**Características Implementadas:**
- ✅ Persistência automática após cada extração
- ✅ Registro de erros e exceções
- ✅ Campos: `entidade`, `dataHoraInicio`, `dataHoraFim`, `status`, `registrosProcessados`, `paginasProcessadas`, `mensagem`
- ✅ Status: COMPLETO, INCOMPLETO, ERRO_API
- ✅ `ResultadoExtracao` já inclui `motivoInterrupcao` (LIMITE_PAGINAS, CIRCUIT_BREAKER, etc.)

**Melhorias Necessárias:**
- ⚠️ Adicionar: campo `tempoExecucaoMs` na tabela `log_extracoes` (atualmente calculado via `dataHoraFim - dataHoraInicio`)

## Data Models

### Entities (Banco de Dados)

#### FreteEntity
```sql
CREATE TABLE fretes (
    id BIGINT PRIMARY KEY,
    reference_number VARCHAR(50),
    service_at DATETIME,
    total DECIMAL(18,2),
    -- ... (22 campos do CSV mapeados, 187 campos disponíveis na API FreightBase)
    metadata NVARCHAR(MAX), -- JSON completo
    data_extracao DATETIME DEFAULT GETDATE()
);
```

#### ColetaEntity
```sql
CREATE TABLE coletas (
    id BIGINT PRIMARY KEY,
    sequence_code INT,
    request_date DATE,
    status VARCHAR(50),
    -- ... (22 campos do CSV mapeados, 44 campos disponíveis na API Pick)
    metadata NVARCHAR(MAX), -- JSON completo
    data_extracao DATETIME DEFAULT GETDATE()
);
```

#### CotacaoEntity
```sql
CREATE TABLE cotacoes (
    sequence_code INT PRIMARY KEY,
    requested_at DATETIME,
    total DECIMAL(18,2),
    -- ... (36 campos)
    metadata NVARCHAR(MAX), -- JSON completo
    data_extracao DATETIME DEFAULT GETDATE()
);
```

#### ManifestoEntity
```sql
CREATE TABLE manifestos (
    sequence_code INT PRIMARY KEY,
    created_at DATETIME,
    status VARCHAR(50),
    -- ... (80 campos)
    metadata NVARCHAR(MAX), -- JSON completo
    data_extracao DATETIME DEFAULT GETDATE()
);
```

#### LocalizacaoCargaEntity
```sql
CREATE TABLE localizacao_cargas (
    corporation_sequence_number INT PRIMARY KEY,
    service_at DATETIME,
    status VARCHAR(50),
    -- ... (17 campos)
    metadata NVARCHAR(MAX), -- JSON completo
    data_extracao DATETIME DEFAULT GETDATE()
);
```

## Error Handling

### Estratégia de Tratamento de Erros

#### 1. Erros de Rede (IOException, TimeoutException)
```java
try {
    HttpResponse<String> resposta = httpClient.send(requisicao);
} catch (IOException | InterruptedException e) {
    logger.error("Erro de rede: {}", e.getMessage());
    // Retry com backoff exponencial (3 tentativas)
    // Se todas falharem, incrementar contador de falhas
}
```

#### 2. Erros de API (Status 4xx, 5xx)
```java
if (resposta.statusCode() == 503) {
    // Service Unavailable - retry com backoff
} else if (resposta.statusCode() >= 400) {
    // Erro permanente - incrementar contador de falhas
    logger.error("Erro da API: status {}", resposta.statusCode());
}
```

#### 3. Erros de Parsing (JsonProcessingException)
```java
try {
    T dto = objectMapper.readValue(json, tipoClasse);
} catch (JsonProcessingException e) {
    logger.warn("Erro ao parsear JSON: {}", e.getMessage());
    // Registrar warning mas continuar processamento
}
```

#### 4. Erros de Banco de Dados (SQLException)
```java
try {
    int salvos = repository.salvar(entities);
} catch (SQLException e) {
    logger.error("Erro ao salvar no banco: {}", e.getMessage());
    // Registrar no log_extracoes com status ERRO_API
    throw new RuntimeException("Falha na persistência", e);
}
```

### Circuit Breaker

**Lógica:**
1. Contador de falhas consecutivas por entidade
2. Após 5 falhas, ativar circuit breaker
3. Quando ativo, retornar resultado vazio sem tentar requisição
4. Resetar contador após sucesso

**Implementação:**
```java
private final Map<String, Integer> contadorFalhas = new HashMap<>();
private final Set<String> entidadesComCircuitAberto = new HashSet<>();

private void incrementarContadorFalhas(String chave, String nome) {
    int falhas = contadorFalhas.getOrDefault(chave, 0) + 1;
    contadorFalhas.put(chave, falhas);
    
    if (falhas >= 5) {
        entidadesComCircuitAberto.add(chave);
        logger.error("🚨 CIRCUIT BREAKER ATIVADO - {}", nome);
    }
}
```

## Testing Strategy

### 1. Testes de Integração

**Objetivo:** Validar comunicação com APIs reais

```java
@Test
public void testBuscarFretesIntegracao() {
    ClienteApiGraphQL cliente = new ClienteApiGraphQL();
    LocalDate hoje = LocalDate.now();
    
    ResultadoExtracao<FreteNodeDTO> resultado = cliente.buscarFretes(hoje);
    
    assertNotNull(resultado);
    assertTrue(resultado.isCompleto() || resultado.getMotivoInterrupcao() != null);
    assertFalse(resultado.getDados().isEmpty());
}
```

### 2. Testes de Mapeamento

**Objetivo:** Validar conversão DTO → Entity

```java
@Test
public void testFreteMapper() {
    FreteNodeDTO dto = criarDtoMock();
    FreteMapper mapper = new FreteMapper();
    
    FreteEntity entity = mapper.toEntity(dto);
    
    assertEquals(dto.getId(), entity.getId());
    assertEquals(dto.getReferenceNumber(), entity.getReferenceNumber());
    assertNotNull(entity.getMetadata());
}
```

### 3. Testes de Persistência

**Objetivo:** Validar salvamento no banco

```java
@Test
public void testFreteRepositorySalvar() {
    FreteRepository repo = new FreteRepository();
    List<FreteEntity> entities = criarEntitiesMock(10);
    
    int salvos = repo.salvar(entities);
    
    assertEquals(10, salvos);
}
```

### 4. Testes de Circuit Breaker

**Objetivo:** Validar proteções contra falhas

```java
@Test
public void testCircuitBreakerAtivacao() {
    ClienteApiGraphQL cliente = new ClienteApiGraphQL();
    
    // Simular 5 falhas consecutivas
    for (int i = 0; i < 5; i++) {
        simularFalha();
    }
    
    // Próxima chamada deve retornar vazio sem tentar requisição
    ResultadoExtracao<FreteNodeDTO> resultado = cliente.buscarFretes(LocalDate.now());
    
    assertTrue(resultado.getDados().isEmpty());
}
```

### 5. Testes de Validação

**Objetivo:** Validar completude de dados

```java
@Test
public void testValidacaoCompletude() {
    AuditoriaService auditoria = new AuditoriaService();
    
    ResultadoAuditoria resultado = auditoria.validarCompletude("fretes");
    
    assertEquals(StatusAuditoria.COMPLETO, resultado.getStatus());
    assertTrue(resultado.getPercentualCobertura() >= 95.0);
}
```

## Deployment Considerations

### Configuração de Ambiente

**Variáveis de Ambiente Obrigatórias:**
```bash
API_BASEURL=https://empresa.eslcloud.com.br
API_GRAPHQL_TOKEN=token_graphql
API_DATAEXPORT_TOKEN=token_dataexport
DB_URL=jdbc:sqlserver://localhost:1433;databaseName=esl_cloud
DB_USER=sa
DB_PASSWORD=***SENHA_CENSURADA_POR_SEGURANCA***
```

**Configurações Opcionais:**
```bash
API_GRAPHQL_LIMITE_PAGINAS=500
API_DATAEXPORT_LIMITE_PAGINAS=200
API_TIMEOUT_SEGUNDOS=30
```

### Execução

**Extração Completa:**
```bash
java -jar extrator.jar --fluxo-completo
```

**Extração de API Específica:**
```bash
java -jar extrator.jar --testar-api graphql
java -jar extrator.jar --testar-api dataexport
```

**Auditoria:**
```bash
java -jar extrator.jar --auditoria
```

### Monitoramento

**Logs:**
- `logs/extracao_dados_YYYY-MM-DD.log` - Log principal
- `logs/sql_YYYY-MM-DD.log` - Queries SQL executadas

**Métricas:**
- `metricas/metricas-YYYY-MM-DD.json` - Métricas de execução

**Banco de Dados:**
- Tabela `log_extracoes` - Histórico de extrações

### Performance

**Tempos Esperados (24h de dados):**
- Fretes: 2-5 minutos (100-500 registros)
- Coletas: 1-3 minutos (50-200 registros)
- Cotações: 1-2 minutos (30-100 registros)
- Manifestos: 2-4 minutos (50-150 registros)
- Localização: 1-2 minutos (100-300 registros)

**Total:** 7-16 minutos para extração completa

### Troubleshooting

**Problema:** Circuit breaker ativado
**Solução:** Verificar logs para identificar causa das falhas, corrigir e reiniciar extração

**Problema:** Extração incompleta (limite de páginas)
**Solução:** Aumentar `API_GRAPHQL_LIMITE_PAGINAS` ou `API_DATAEXPORT_LIMITE_PAGINAS`

**Problema:** Timeout em requisições
**Solução:** Aumentar `API_TIMEOUT_SEGUNDOS` ou verificar conectividade de rede

**Problema:** Discrepância entre DTOs e registros salvos
**Solução:** Verificar logs SQL para identificar erros de constraint ou duplicatas
