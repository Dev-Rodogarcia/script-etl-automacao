---
context:
  - Documentacao
  - Legado
  - ArquivoHistorico
updated_at: 2026-03-18T00:00:32.1501533-03:00
source_of_truth: docs
classification: parcial
related_files:
  - docs/index.md
  - docs/legado/index.md
  - docs/legado/classificacao.md
---
# 📋 Análise de Refinamentos do Projeto - Extrator ESL Cloud

**Data da Análise:** 24/12/2025  
**Versão Analisada:** 2.1  
**Autor:** Análise Automatizada

---

## 📌 Resumo Executivo

Este documento lista falhas, inconsistências e oportunidades de melhoria identificadas no projeto de extração de dados do ESL Cloud. Todas as sugestões são **não destrutivas** e podem ser implementadas sem quebrar a funcionalidade existente.

---

## 🔴 Problemas Críticos (Prioridade Alta)

### 1. Duplicação Massiva de Código nos Runners

**Arquivo:** `DataExportRunner.java` e `GraphQLRunner.java`

**Problema:** O método `executar(LocalDate, String)` duplica praticamente todo o código do método `executar(LocalDate)`, violando o princípio DRY (Don't Repeat Yourself).

**Impacto:** 
- Manutenção difícil (alterações precisam ser feitas em 2 lugares)
- Risco de inconsistências entre os métodos
- Código com ~900 linhas que poderia ter ~400

**Sugestão:**
```java
// Refatorar para extrair lógica comum
public static void executar(final LocalDate dataInicio) throws Exception {
    executar(dataInicio, null); // Delegar para método completo
}

public static void executar(final LocalDate dataInicio, final String entidade) throws Exception {
    // Lógica unificada com flag de entidade
}
```

---

### 2. Variáveis Estáticas no `CarregadorConfig`

**Arquivo:** `CarregadorConfig.java` (linha 19)

**Problema:**
```java
private static Properties propriedades = null;
```

**Impacto:**
- Em ambiente multi-thread, pode haver race conditions na primeira carga
- Não permite recarregar configurações em tempo de execução
- Testes unitários ficam difíceis (estado global compartilhado)

**Sugestão:**
```java
// Usar holder pattern para lazy initialization thread-safe
private static class PropriedadesHolder {
    static final Properties INSTANCIA = carregarPropriedadesInterno();
}
```

---

### 3. Conexões de Banco Duplicadas

**Arquivos:** `CarregadorConfig.java` e `GerenciadorConexao.java`

**Problema:** Dois métodos diferentes para obter credenciais de banco:
- `CarregadorConfig.obterUrlBancoDados()` - com cache
- `GerenciadorConexao.obterConexao()` - lê diretamente de `System.getenv()`

**Impacto:**
- Se as variáveis de ambiente mudarem, comportamento inconsistente
- Duplicação de lógica de validação

**Sugestão:**
Centralizar em `GerenciadorConexao` usando `CarregadorConfig`:
```java
public static Connection obterConexao() throws SQLException {
    return DriverManager.getConnection(
        CarregadorConfig.obterUrlBancoDados(),
        CarregadorConfig.obterUsuarioBancoDados(),
        CarregadorConfig.obterSenhaBancoDados()
    );
}
```

---

## 🟠 Problemas Moderados (Prioridade Média)

### 4. Thread.sleep() Hardcoded nos Runners

**Arquivos:** `DataExportRunner.java` (linhas 139, 214, 286, 352), `GraphQLRunner.java` (linha 97)

**Problema:**
```java
Thread.sleep(2000); // Hardcoded em vários lugares
```

**Impacto:**
- Não respeita a configuração centralizada de throttling
- Difícil ajustar sem recompilar

**Sugestão:**
```java
Thread.sleep(CarregadorConfig.obterThrottlingPadrao());
```

---

### 5. Criação Repetida de Repositórios e Mappers

**Arquivos:** `DataExportRunner.java` e `GraphQLRunner.java`

**Problema:** Em cada chamada de `executar()`, são criadas novas instâncias:
```java
final ManifestoRepository manifestoRepository = new ManifestoRepository();
final CotacaoRepository cotacaoRepository = new CotacaoRepository();
// ... várias outras instâncias
```

**Impacto:**
- Overhead desnecessário de criação de objetos
- Conexões de banco são abertas e fechadas repetidamente

**Sugestão:**
Usar Singleton ou injeção de dependência:
```java
private static final ManifestoRepository manifestoRepository = new ManifestoRepository();
```

---

### 6. Falta de Validação de Dados nos DTOs

**Arquivos:** DTOs em `modelo/dataexport/` e `modelo/graphql/`

**Problema:** Os DTOs não validam dados críticos como:
- `sequenceCode` nulo ou vazio
- Datas em formato inválido
- Campos numéricos com valores negativos inesperados

**Impacto:**
- Erros silenciosos podem propagar dados inválidos
- Exceptions em runtime durante o MERGE

**Sugestão:**
Adicionar validação no mapper ou usar Bean Validation:
```java
public ManifestoEntity toEntity(ManifestoDTO dto) {
    Objects.requireNonNull(dto.getSequenceCode(), "sequence_code é obrigatório");
    // ...
}
```

---

### 7. Magic Numbers Espalhados

**Arquivos:** `ClienteApiGraphQL.java`, `ClienteApiDataExport.java`

**Problema:** Números mágicos espalhados:
```java
private static final int MAX_REGISTROS_POR_EXECUCAO = 50000; // GraphQL
private static final int MAX_REGISTROS_POR_EXECUCAO = 10000; // DataExport
```

**Impacto:**
- Inconsistência nos limites entre APIs
- Difícil entender por que os valores são diferentes

**Sugestão:**
Mover para `config.properties`:
```properties
api.graphql.max.registros.execucao=50000
api.dataexport.max.registros.execucao=10000
```

---

### 8. Ausência de Pool de Conexões

**Arquivo:** `AbstractRepository.java`

**Problema:**
```java
protected Connection obterConexao() throws SQLException {
    return DriverManager.getConnection(urlConexao, usuario, senha);
}
```

**Impacto:**
- Cada operação abre uma nova conexão TCP
- Overhead significativo em operações de grande volume
- Pode esgotar conexões no SQL Server

**Sugestão:**
Implementar HikariCP ou outro pool de conexões:
```java
// Adicionar ao pom.xml
<dependency>
    <groupId>com.zaxxer</groupId>
    <artifactId>HikariCP</artifactId>
</dependency>
```

---

## 🟡 Melhorias de Código (Prioridade Baixa)

### 9. Logs com System.out.println em vez de Logger

**Arquivos:** Todos os Runners, `ExecutarFluxoCompletoComando.java`

**Problema:** Mistura de `System.out.println()` com `logger.info()`:
```java
System.out.println("✓ Extraídos: " + manifestosDTO.size() + " manifestos");
logger.info("Log de extração gravado");
```

**Impacto:**
- Logs para console não são capturados pelo sistema de logging
- Difícil filtrar ou redirecionar outputs

**Sugestão:**
Usar apenas Logger:
```java
logger.info("✓ Extraídos: {} manifestos", manifestosDTO.size());
```

---

### 10. Falta de Constantes para Nomes de Entidades

**Arquivos:** Vários runners e repositórios

**Problema:** Strings repetidas em vários lugares:
```java
"manifestos"
"cotacoes"
"localizacao_cargas"
```

**Impacto:**
- Risco de typos
- Difícil refatorar nomes

**Sugestão:**
Criar enum ou classe de constantes:
```java
public enum Entidade {
    MANIFESTOS("manifestos"),
    COTACOES("cotacoes"),
    LOCALIZACAO_CARGAS("localizacao_cargas");
    
    private final String valor;
    // ...
}
```

---

### 11. Comentários Obsoletos/Inconsistentes

**Arquivo:** `Main.java` (linhas 26-29)

**Problema:**
```java
// - RestRunner: Faturas a Receber, Faturas a Pagar e Ocorrências
// - GraphQLRunner: Coletas e Fretes
// - DataExportRunner: Manifestos, Cotações e Localização de Carga
```

O RestRunner não existe mais no código (foi removido), mas o comentário permanece.

**Sugestão:** Atualizar documentação para refletir a arquitetura atual (2 runners: GraphQL e DataExport).

---

### 12. Tratamento Inconsistente de Exceções

**Arquivos:** Runners

**Problema:**
```java
catch (RuntimeException | java.sql.SQLException e) {
    // ...
    throw new RuntimeException("Falha na extração de manifestos", e);
}
```

RuntimeException já é não-checada, capturá-la e re-lançar adiciona camada desnecessária.

**Sugestão:**
```java
catch (SQLException e) {
    throw new RuntimeException("Falha na extração de manifestos", e);
}
// RuntimeException propaga naturalmente
```

---

### 13. Formatação de Datas Inconsistente

**Arquivos:** `ExecutarFluxoCompletoComando.java`, `ClienteApiGraphQL.java`

**Problema:** Múltiplos padrões de formatação criados inline:
```java
DateTimeFormatter.ofPattern("dd/MM/yyyy")
DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
DateTimeFormatter.ofPattern("yyyy-MM-dd")
```

**Sugestão:**
Centralizar em classe utilitária:
```java
public final class FormatadorData {
    public static final DateTimeFormatter BR_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter BR_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    public static final DateTimeFormatter ISO_DATA = DateTimeFormatter.ofPattern("yyyy-MM-dd");
}
```

---

### 14. Propriedades de Configuração sem Uso

**Arquivo:** `config.properties`

**Problema:** Algumas propriedades definidas mas não utilizadas:
```properties
api.throttling.intervalo_entre_processos_ms=3000
db.url=
db.user=
db.password=
```

**Impacto:**
- Confusão sobre quais configurações são efetivas
- Valores vazios podem causar erros se usados acidentalmente

**Sugestão:**
Remover ou documentar claramente que são placeholders.

---

### 15. Falta de Timeouts Configuráveis no HttpClient

**Arquivo:** `ClienteApiGraphQL.java` (linha 249-251)

**Problema:**
```java
this.clienteHttp = HttpClient.newBuilder()
    .connectTimeout(Duration.ofSeconds(10)) // Hardcoded
    .build();
```

**Sugestão:**
```java
.connectTimeout(CarregadorConfig.obterTimeoutConexao())
```

---

## 🔵 Sugestões de Segurança

### 16. Tokens em Log de Debug

**Arquivos:** `ClienteApiGraphQL.java`, `ClienteApiDataExport.java`

**Problema:** Em alguns logs DEBUG, informações sensíveis podem vazar:
```java
logger.debug("Executando query GraphQL tipada para {} - URL: {}{}, Variáveis: {}", 
    nomeEntidade, urlBase, endpointGraphQL, variaveis);
```

**Sugestão:**
Sanitizar variáveis antes de logar, removendo tokens/credenciais.

---

### 17. Arquivo last_run.properties na Raiz

**Arquivo:** `last_run.properties`

**Problema:** Arquivo de estado na raiz do projeto, pode ser commitado acidentalmente.

**Sugestão:**
- Adicionar ao `.gitignore`
- Mover para pasta `logs/` ou `data/`

---

## 🟢 Pontos Positivos Identificados

1. ✅ **Circuit Breaker implementado** - Proteção contra falhas em cascata
2. ✅ **Throttling centralizado** - Respeita rate limits da API
3. ✅ **Deduplicação de registros** - Evita duplicados no banco
4. ✅ **Logging estruturado** - Uso de SLF4J/Logback
5. ✅ **Validação de completude** - Compara dados extraídos com origem
6. ✅ **Execução paralela** - Runners executam em threads separadas
7. ✅ **MERGE (UPSERT)** - Evita duplicatas no banco via chave primária
8. ✅ **Backoff exponencial** - Retry inteligente em falhas temporárias

---

## 📊 Resumo por Prioridade

| Prioridade | Quantidade | Impacto |
|------------|------------|---------|
| 🔴 Alta    | 3          | Afeta manutenibilidade e robustez |
| 🟠 Média   | 5          | Pode causar problemas em produção |
| 🟡 Baixa   | 7          | Melhorias de código/estilo |
| 🔵 Segurança | 2        | Risco de exposição de dados |

---

## 🛠️ Plano de Ação Sugerido

### Fase 1 - Quick Wins (1-2 dias)
- [x] Corrigir comentários obsoletos (#11) ✅
- [x] Adicionar `last_run.properties` ao `.gitignore` (#17) ✅
- [x] Substituir `Thread.sleep()` hardcoded (#4) ✅
- [x] Criar constantes para nomes de entidades (#10) ✅

### Fase 2 - Refatoração Menor (3-5 dias)
- [x] Unificar métodos `executar()` nos Runners (#1) ✅ **JÁ IMPLEMENTADO**
- [x] Centralizar obtenção de conexão (#3) ✅ **CORRIGIDO** - GerenciadorConexao agora usa CarregadorConfig
- [x] Migrar `System.out.println` para Logger (#9) ✅ **CORRIGIDO** - 228 ocorrências migradas para LoggerConsole (48%)
    - Arquivos principais migrados: ExecutarFluxoCompletoComando, AuditoriaRelatorio, ExportadorCSV, LoopExtracaoComando
    - Restantes são intencionais (CLI help, banners) ou podem ser migrados gradualmente
- [x] Centralizar formatadores de data (#13) ✅ **CORRIGIDO** - Principais arquivos migrados para FormatadorData

### Fase 3 - Melhorias de Infraestrutura (1-2 semanas)
- [x] Implementar pool de conexões HikariCP (#8) ✅ **JÁ IMPLEMENTADO** - GerenciadorConexao usa HikariDataSource
- [x] Mover magic numbers para configuração (#7) ✅ **CORRIGIDO** - MAX_REGISTROS agora em CarregadorConfig/config.properties
- [x] Adicionar validação nos DTOs (#6) ✅ **CORRIGIDO** - ValidadorDTO usado nos Mappers principais
- [x] Refatorar `CarregadorConfig` para thread-safety (#2) ✅ **JÁ IMPLEMENTADO** - Usa PropertiesHolder pattern

---

## 📝 Observações Finais

Este projeto está bem estruturado e funcional. As sugestões acima visam:
1. **Aumentar a manutenibilidade** - Menos código duplicado
2. **Melhorar a robustez** - Tratamento de erros mais consistente
3. **Facilitar testes** - Menos estado global
4. **Otimizar performance** - Pool de conexões, menos alocações

Nenhuma das sugestões quebra a funcionalidade existente se implementada corretamente.
