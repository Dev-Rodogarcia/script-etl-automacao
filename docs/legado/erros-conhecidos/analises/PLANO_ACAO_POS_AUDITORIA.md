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
# 📋 PLANO DE AÇÃO PÓS-AUDITORIA

**Data:** 04/02/2026  
**Versão:** 2.3.3 (pós-correções críticas)  
**Status:** 🟢 Pronto para Execução

---

## 🎯 OBJETIVO

Aplicar todas as correções identificadas na auditoria profunda e elevar o projeto de **7.5/10** para **9.0/10** em qualidade de código e robustez.

---

## ✅ CORREÇÕES JÁ APLICADAS

### CRÍTICAS (100% Concluído)
- ✅ **#1** - Vazamento de conexões corrigido (AbstractRepository usa HikariCP)
- ✅ **#3** - Isolamento de transação e timeout adicionados
- ✅ **#4** - Script SQL para alinhar constraint UNIQUE criado
- ✅ **#7** - Scripts de índices de performance criados
- ✅ **#8** - Circuit Breaker implementado no GerenciadorRequisicaoHttp

### ALTAS (17% Concluído)
- ✅ **#2** - Validação de parâmetros NULL (DataExportRunner, GraphQLRunner)

---

## 📅 CRONOGRAMA DE EXECUÇÃO

### ⚡ IMEDIATO (Hoje - 04/02/2026)

#### 1. Validar Compilação
```bash
mvn clean compile
```
**Responsável:** Dev Lead  
**Tempo Estimado:** 5 minutos  
**Critério de Sucesso:** Build sem erros

#### 2. Executar Scripts SQL
```bash
# Índices de performance
cd database/indices
executar_indices.bat

# Correção de constraint
cd database/migrations
sqlcmd -S localhost -d ESL_Cloud_ETL -i 002_corrigir_constraint_manifestos.sql
```
**Responsável:** DBA / Dev Lead  
**Tempo Estimado:** 15 minutos  
**Critério de Sucesso:** Todos os índices criados, constraint migrada

#### 3. Testes Manuais Críticos
```bash
# Compilar
mvn clean package -DskipTests

# Validar manifestos
java -jar target/extrator.jar --validar-manifestos

# Teste de conexão
java -jar target/extrator.jar --validar
```
**Responsável:** QA / Dev  
**Tempo Estimado:** 20 minutos  
**Critério de Sucesso:** Validações passam sem erros

---

### 🔥 URGENTE (Amanhã - 05/02/2026)

#### 4. Deploy em Ambiente de Testes
```bash
# Backup do JAR atual
copy target\extrator.jar target\extrator_backup_2.3.2.jar

# Deploy da nova versão
copy target\extrator.jar \\servidor-teste\app\
```
**Responsável:** DevOps  
**Tempo Estimado:** 30 minutos  
**Critério de Sucesso:** Sistema rodando em teste

#### 5. Monitoramento de Logs
```bash
# Executar extração completa em teste
01-executar_extracao_completa.bat

# Monitorar logs em tempo real
tail -f logs/extracao_dados_*.log
```
**Responsável:** Dev + QA  
**Tempo Estimado:** 2 horas  
**Critério de Sucesso:** 
- Sem erros de conexão
- Performance melhorada (verificar tempo de execução)
- Circuit breaker funciona corretamente

---

### 📆 CURTO PRAZO (Semana 1 - até 11/02/2026)

#### 6. Implementar Testes Unitários Essenciais

**Prioridade 1 - Mappers (20 testes):**
- `ManifestoMapperTest.java` ✅ (template criado)
- `CotacaoMapperTest.java` ⏳
- `ColetaMapperTest.java` ⏳
- `FreteMapperTest.java` ⏳

**Prioridade 2 - Validators (15 testes):**
- `ValidadorDTOTest.java` ⏳
- `CompletudeValidatorTest.java` ⏳

**Prioridade 3 - Utilities (10 testes):**
- `FormatadorDataTest.java` ⏳
- `MapperUtilTest.java` ⏳

**Meta:** 45 testes, 30% de cobertura

#### 7. Refatorar Métodos Gigantes

**Arquivo:** `ClienteApiDataExport.java`

**Métodos a Refatorar:**
1. `buscarDadosGenericos()` - 280 linhas → dividir em 6 métodos
2. `obterContagemGenericaCsv()` - 100 linhas → dividir em 3 métodos

**Meta:** Nenhum método > 50 linhas

#### 8. Substituir System.out por Logger

**Arquivos Prioritários:**
1. `ValidarManifestosComando.java` - ~100 System.out
2. `LoggingService.java` - uso misto
3. `BannerUtil.java` - System.out para banners

**Estratégia:**
```java
// Usar logger com marker CLI_OUTPUT
private static final Marker CLI = MarkerFactory.getMarker("CLI_OUTPUT");
logger.info(CLI, "Mensagem para CLI");
```

---

### 📆 MÉDIO PRAZO (Semana 2-4 - até 04/03/2026)

#### 9. Testes de Integração com Testcontainers

**Setup:**
```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>mssqlserver</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>
```

**Testes:**
- Repository integration tests (CRUD completo)
- Fluxo completo de ETL
- Validação de constraints

**Meta:** 60% de cobertura total

#### 10. Configurar JaCoCo e CI/CD

**JaCoCo:**
```bash
mvn test jacoco:report
# Verificar: target/site/jacoco/index.html
```

**CI/CD Pipeline (GitHub Actions / Jenkins):**
```yaml
# .github/workflows/build.yml
name: Build e Testes
on: [push, pull_request]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
      - run: mvn clean verify
      - run: mvn jacoco:report
```

#### 11. Implementar Métricas (Micrometer)

**Setup:**
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-core</artifactId>
    <version>1.12.0</version>
</dependency>
```

**Métricas a Coletar:**
- Duração de extrações por entidade
- Taxa de sucesso/falha
- Throughput (registros/segundo)
- Latência P50, P95, P99

---

### 📆 LONGO PRAZO (1-3 meses)

#### 12. SonarQube
- Configurar servidor SonarQube
- Integrar com pipeline CI/CD
- Corrigir code smells identificados

#### 13. Documentação JavaDoc
- Adicionar JavaDoc em todos os métodos públicos
- Gerar site de documentação: `mvn javadoc:javadoc`

#### 14. Health Checks
```java
@RestController
@RequestMapping("/health")
public class HealthController {
    @GetMapping
    public HealthStatus check() {
        return new HealthStatus(
            GerenciadorConexao.isPoolSaudavel(),
            circuitBreaker.isOpen(),
            lastExtractionTimestamp
        );
    }
}
```

---

## 🎯 CRITÉRIOS DE SUCESSO

### Curto Prazo (1 semana)
- ✅ Cobertura de testes: 30%
- ✅ Nenhum método > 50 linhas
- ✅ Sem System.out em código de produção
- ✅ Build automático configurado

### Médio Prazo (1 mês)
- ✅ Cobertura de testes: 60%
- ✅ Testes de integração funcionando
- ✅ Métricas de performance coletadas
- ✅ CI/CD pipeline operacional

### Longo Prazo (3 meses)
- ✅ Cobertura de testes: 80%+
- ✅ SonarQube configurado (0 bugs críticos)
- ✅ Documentação JavaDoc completa
- ✅ Health checks implementados

---

## 📊 MÉTRICAS DE ACOMPANHAMENTO

### Dashboard Semanal
```
┌─────────────────────────────────────────┐
│ MÉTRICAS DE QUALIDADE - Semana X        │
├─────────────────────────────────────────┤
│ Cobertura de Testes:      XX%           │
│ Bugs Críticos:            X              │
│ Code Smells:              XX             │
│ Duplicação de Código:     X%             │
│ Complexity > 15:          X métodos      │
│ Métodos > 50 linhas:      X métodos      │
└─────────────────────────────────────────┘
```

---

## ⚠️ RISCOS E MITIGAÇÕES

### Risco 1: Regressão em Produção
**Probabilidade:** Baixa  
**Impacto:** Alto  
**Mitigação:** 
- Testes extensivos em ambiente de staging
- Deploy gradual (canary deployment)
- Rollback preparado

### Risco 2: Performance Degradada
**Probabilidade:** Muito Baixa  
**Impacto:** Médio  
**Mitigação:**
- Correções melhoram performance (não degradam)
- Monitorar métricas pós-deploy
- Índices criados com ONLINE = ON (SQL Server Enterprise)

### Risco 3: Incompatibilidade de Constraint
**Probabilidade:** Baixa  
**Impacto:** Médio  
**Mitigação:**
- Script SQL valida duplicados antes de migrar
- Backup de tabela antes da alteração
- Procedimento de rollback documentado

---

## 📞 CONTATOS E RESPONSÁVEIS

### Dev Lead
- **Responsável:** [Nome do Tech Lead]
- **Atribuições:** Revisão de código, aprovação de PRs

### DBA
- **Responsável:** [Nome do DBA]
- **Atribuições:** Execução de scripts SQL, monitoramento de performance

### QA
- **Responsável:** [Nome do QA]
- **Atribuições:** Testes manuais, validação de comportamento

### DevOps
- **Responsável:** [Nome do DevOps]
- **Atribuições:** Deploy, monitoramento, CI/CD

---

## 📝 CHECKLIST PRÉ-DEPLOY

```
PRÉ-DEPLOY CHECKLIST:
□ Código compilado sem erros
□ Testes unitários passando (mínimo 30%)
□ Scripts SQL executados em staging
□ Validação manual de manifestos OK
□ Logs revisados (sem erros críticos)
□ Backup do banco de dados feito
□ Backup do JAR anterior feito
□ Plano de rollback preparado
□ Equipe de suporte notificada
□ Monitoramento configurado

DEPLOY AUTORIZADO POR:
Nome: ___________________
Data: ___/___/______
Assinatura: _____________
```

---

**FIM DO PLANO DE AÇÃO**

*Este plano de ação é um guia vivo e deve ser atualizado conforme progresso.*
