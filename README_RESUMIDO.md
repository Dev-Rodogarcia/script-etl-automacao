# 📦 Extrator de Dados ESL Cloud - Versão Resumida

**Sistema de Automação ETL (Extract, Transform, Load)** desenvolvido em Java para extrair dados das APIs GraphQL e Data Export do ESL Cloud e carregá-los em SQL Server.

**Versão:** 2.3.4 | **Última Atualização:** 21/02/2026 | **Status:** ✅ Estável e em Produção

---

## 🎯 O Que Faz?

1. **Extrai dados** de 2 APIs do ESL Cloud (GraphQL, Data Export) em **execução paralela**
2. **Transforma** dados JSON em entidades estruturadas
3. **Carrega** dados no SQL Server usando MERGE (UPSERT)
4. **Valida completude** comparando contagens entre API e banco
5. **Exporta dados** para CSV

---

## 🚀 Início Rápido

### Pré-requisitos
- Java 17+
- Maven
- SQL Server com banco configurado
- Variáveis de ambiente configuradas (`DB_URL`, `DB_USER`, `DB_PASSWORD`, `API_GRAPHQL_TOKEN`, `API_DATAEXPORT_TOKEN`)

### Executar Extração Completa

```bash
01-executar_extracao_completa.bat
```

### Configuração do Banco

**⚠️ IMPORTANTE**: As tabelas devem ser criadas via scripts SQL antes de executar:

```bash
cd database
# Execute os scripts SQL na ordem:
# 1. Tabelas (tabelas/*.sql)
# 2. Views (views/*.sql)
# 3. Views Dimensão (views-dimensao/*.sql)
```

Ver documentação completa em `database/README.md`.

---

## 📊 Entidades Extraídas

- **GraphQL**: Usuários do Sistema (dim_usuarios), Coletas, Fretes, Faturas GraphQL
- **Data Export**: Manifestos, Cotações, Localização de Carga, Contas a Pagar, Faturas por Cliente

**Total**: 9 entidades

---

## 🏗️ Arquitetura Simplificada

```
Main.java (CLI)
    ├── ExecutarFluxoCompletoComando
    │   ├── GraphQLRunner (Thread 1)
    │   │   ├── Usuários do Sistema (dim_usuarios)
    │   │   ├── Coletas
    │   │   ├── Fretes
    │   │   └── Faturas GraphQL (Fase 3)
    │   └── DataExportRunner (Thread 2)
    │       ├── Manifestos
    │       ├── Cotações
    │       ├── Localização de Carga
    │       ├── Contas a Pagar
    │       └── Faturas por Cliente
    └── Validações (Completude, Gaps, Temporal)
```

---

## 📁 Estrutura Principal

```
script-automacao/
├── src/main/java/          # Código fonte Java
│   ├── br/com/extrator/
│   │   ├── Main.java                  # Ponto de entrada
│   │   ├── api/                       # Clientes de API
│   │   ├── db/
│   │   │   ├── entity/                # Entidades JPA
│   │   │   └── repository/            # Repositórios (MERGE/INSERT)
│   │   ├── runners/                   # Orquestradores de extração
│   │   └── comandos/                  # Comandos CLI
├── database/              # Scripts SQL (DDL)
│   ├── tabelas/          # Criação de tabelas
│   ├── views/            # Views para Power BI
│   └── views-dimensao/   # Views de dimensão
├── 01-executar_extracao_completa.bat  # Script principal
└── README.md             # Documentação completa
```

---

## 🔑 Características Principais

- ✅ **2 APIs**: GraphQL e Data Export
- ✅ **9 Entidades** extraídas
- ✅ **Execução Paralela** (2 threads)
- ✅ **Sistema MERGE Robusto** (previne duplicados)
- ✅ **Validação Automática** (completude, gaps, temporal)
- ✅ **Schema Versionado** (scripts SQL, não código Java)
- ✅ **Logs Estruturados** (SLF4J/Logback)
- ✅ **Reconciliação Automática no Loop** (recuperação de lacunas)
- ✅ **Exportação CSV** automática

---

## ⚙️ Configuração Mínima

### Variáveis de Ambiente Obrigatórias

```bash
# Banco de Dados
DB_URL=jdbc:sqlserver://servidor:1433;databaseName=SeuBanco
DB_USER=usuario
DB_PASSWORD=senha

# APIs
API_GRAPHQL_TOKEN=seu_token_graphql
API_DATAEXPORT_TOKEN=seu_token_dataexport
API_GRAPHQL_URL=https://api.eslcloud.com/graphql
API_DATAEXPORT_URL=https://api.eslcloud.com/data-export
```

---

## 📝 Scripts Principais

| Script | Descrição |
|--------|-----------|
| `01-executar_extracao_completa.bat` | Extração completa de todas as entidades |
| `03-validar_config.bat` | Valida configuração e variáveis de ambiente |
| `04-extracao_por_intervalo.bat` | Extração por período com divisão em blocos |
| `05-loop_extracao_30min.bat` | Loop contínuo com reconciliação automática |
| `06-relatorio-completo-validacao.bat` | Relatório unificado de validação e auditoria |
| `07-exportar_csv.bat` | Exporta dados para CSV |
| `08-auditar_api.bat` | Audita estrutura das APIs |
| `09-gerenciar_usuarios.bat` | Gerencia usuários de acesso |
| `00-PRODUCAO_START.bat` | Menu único de operação em produção |
| `limpar_logs.bat` | Limpa apenas `.log` e preserva históricos `.csv` |

---

## 🔧 Tecnologias

- **Java 17**
- **Maven**
- **SQL Server** (mssql-jdbc)
- **Jackson** (JSON)
- **SLF4J/Logback** (Logging)
- **HikariCP** (Pool de conexões)

---

## 📚 Documentação Completa

Para documentação detalhada, consulte:
- **README.md** - Documentação completa
- **docs/README.md** - Índice da documentação
- **docs/DER-CLASSES-JAVA-COMPLETO.md** - DER de classes Java
- **docs/DER-COMPLETO-BANCO-DADOS.md** - DER completo de banco
- **docs/FLUXOGRAMA-COMPLETO-SISTEMA.md** - Fluxo completo do sistema
- **database/README.md** - Guia de scripts SQL

---

## 🆕 Novidades 2.3.4 (21/02/2026)

- ✅ **Reconciliação Automática do Loop**: agenda diária de `D-1` e reprocessamento de pendências após falha
- ✅ **Histórico de Reconciliação**: geração mensal em `logs/daemon/reconciliacao/reconciliacao_daemon_YYYY_MM.csv`
- ✅ **Estado Persistente de Reconciliação**: `logs/daemon/loop_reconciliation.state`
- ✅ **Limpeza de Logs Segura**: `limpar_logs.bat` preserva arquivos `.csv` de histórico

## 🆕 Novidades 2.3.3 (14/02/2026)

- ✅ **ThreadUtil**: Utilitário centralizado para pausas (substitui `Thread.sleep` direto)
- ✅ **Validação de Completude**: Inclusão de `usuarios_sistema`/dim_usuarios na "Prova dos 9"
- ✅ **README Resumido**: Alinhado a 9 entidades e fluxo com Usuários do Sistema e Fase 3 Faturas GraphQL

## 🆕 Novidades 2.3.2 (03/02/2026)

- ✅ **Documentação Atualizada**: Alinhamento completo entre documentação e código
- ✅ **Deduplicação Documentada**: Estratégia "Keep Last" documentada corretamente
- ✅ **Database README Melhorado**: Documentação completa de todos os scripts SQL (001-029)
- ✅ **Análise de Deduplicação**: Marcada como "Correções Aplicadas"

## 🆕 Novidades 2.2.1 (12/01/2026)

- ✅ **Sistema de Auditoria Corrigido**: Comparação histórica usando dados do log_extracoes
- ✅ **Correção CONTAS_A_PAGAR**: Contagem usando issue_date (mesma lógica da API)
- ✅ **Relatórios Melhorados**: Relatórios de auditoria mais detalhados e organizados

## 🆕 Novidades 2.2 (12/01/2026)

- ✅ **Refatoração de Repositórios**: Separação completa DDL/DML
- ✅ **Schema Versionado**: Estrutura gerenciada via scripts SQL
- ✅ **Padronização**: Todos os repositórios seguem padrão consistente
- ✅ **Melhorias de Código**: Logging, validações e tratamento de erros

---

## ⚠️ Importante

- **Schema do Banco**: Deve ser criado via scripts SQL (`database/`) ANTES de executar
- **Tokens e Credenciais**: Nunca commitar no Git - use variáveis de ambiente
- **Java 17+**: Requerido para compilação e execução

---

**Para mais detalhes, consulte [README.md](README.md) - Documentação Completa**
