# 📈 Pipeline de Extração de Dados ETL - ESL Cloud & Raster API

> **Atenção:** Este repositório contém o mecanismo CLI de ETL (Extract, Transform, Load) de alta performance da **RodoGarcia**, desenvolvido em **Java 17**. Ele é o único responsável pela ingestão, processamento, integridade e enriquecimento dos dados analíticos estruturados oriundos da **ESL Cloud** e da **Raster API**, persistindo-os no banco de dados central SQL Server e publicando as views de negócio consumidas de forma somente leitura pelo Portal de Dashboards e soluções de BI (Power BI).

---

## 🗺️ Visão Geral & Arquitetura do Sistema

O ETL opera como um executável de linha de comando (CLI) ou serviço daemon contínuo. Ele abstrai a complexidade de consumo de múltiplos endpoints e protocolos externos, aplicando regras estritas de resiliência, validação de integridade referencial, e auditoria de qualidade de dados (*Data Quality*) antes de materializar os dados operacionais.

### Fluxo de Comunicação e Fronteiras de Dados

```mermaid
graph TD
    subgraph Fontes Externas
        ESL_GQL[ESL Cloud - GraphQL API<br/>Coletas, Fretes, Faturas, Usuários]
        ESL_DE[ESL Cloud - Data Export<br/>Manifestos, Contas a Pagar, Cotações...]
        Raster[Raster API<br/>Rastreamento de Viagens & Paradas]
    end

    subgraph Core ETL (Java 17 CLI)
        Orchestrator[PipelineOrchestrator<br/>Parallel Execution & Failures]
        GraphQLGateway[GraphQL Gateway]
        DataExportGateway[Data Export Gateway]
        QualityCheck[Data Quality Suite<br/>Unicidade, Completude, Freshness]
        AuthEngine[Security Engine<br/>SQLite Authentication]
    end

    subgraph Armazenamento & Consumo
        LocalSQLite[(SQLite - users.db<br/>CLI ACL & Credentials)]
        SqlServer[(SQL Server - ETL_SISTEMA / esl_cloud)]
        DashboardAPI[Spring Boot 3.2 API]
        PowerBI[Power BI Reports]
    end

    %% Integrações de Entrada
    ESL_GQL <-->|HTTPS / GraphQL Queries| GraphQLGateway
    ESL_DE -->|HTTPS / CSV Data Export| DataExportGateway
    Raster -->|HTTPS / REST Client| Orchestrator

    %% Fluxo Interno
    GraphQLGateway --> Orchestrator
    DataExportGateway --> Orchestrator
    Orchestrator --> QualityCheck
    AuthEngine <-->|Local JDBC| LocalSQLite

    %% Fluxo de Saída / Persistência
    QualityCheck -->|MERGE & Batch Commits| SqlServer
    SqlServer -->|Views: dbo.vw_*_powerbi<br/>dbo.vw_dim_*| PowerBI
    SqlServer -->|SELECT Read-Only| DashboardAPI
```

> [!IMPORTANT]
> **Fronteira com o Portal de Dashboards:**
> O projeto `etl-extracao-dados` é o **único owner estrutural** do schema `ETL_SISTEMA` (`esl_cloud`) e das views analíticas.
> * Alterações em tabelas analíticas, índices de performance, views `vw_*_powerbi` ou views dimensionais `vw_dim_*` **devem nascer e ser catalogadas neste repositório** (dentro do diretório `database/`).
> * O projeto de Dashboards **nunca** deve aplicar migrations DDL contra o banco do ETL. Ele é estritamente um consumidor read-only.

---

## 🏛️ Banco de Dados e SSOT (Single Source of Truth)

O banco de dados SQL Server do ETL (`ETL_SISTEMA` / `esl_cloud`) tem seu ciclo de vida e estrutura gerenciados de forma automatizada pelo script `database/executar_database.bat`. 

### Modos de Sincronização do Banco

1. **Modo Padrão (Seguro/Idempotente):** Executado sem parâmetros. Cria tabelas e índices ausentes, aplica scripts de migrations e atualiza as views operacionais e analíticas sem apagar os dados existentes.
   ```powershell
   cd .\database
   .\executar_database.bat
   ```
2. **Modo de Desenvolvimento (`--recriar`):** Destrutivo. Apaga o banco de dados por completo e o recria do zero, executando sequencialmente os scripts de tabelas, migrations, índices, views e validações estruturais.
   ```powershell
   .\executar_database.bat --recriar
   ```

### Estrutura de Objetos do Banco
* **Tabelas Operacionais:** Concentram os dados puros das entidades após mapeamento e sanitização.
* **Views de Negócio (`dbo.vw_*_powerbi`):** Camada de abstração otimizada com projeções de KPIs operacionais (Faturas, Tracking, Manifestos, Fretes, etc.).
* **Views Dimensionais (`dbo.vw_dim_*`):** Dimensões compartilhadas (Clientes, Veículos, Motoristas, Rotas).
* **Auditoria Estruturada (`log_extracoes`):** Registro persistente de telemetria, volumetria por entidade, logs de erros e uuid de execução.

---

## 🔌 Ingestão de Dados & Modos de Ingestão

O sistema processa a ingestão através de dois canais sob um `PipelineOrchestrator` inteligente que detecta dependências e otimiza a concorrência:

1. **Ingestão em Paralelo (Core):** Quando o pipeline inicia, as etapas de `graphql` (Coletas, Fretes, Usuários) e `dataexport` (Manifestos, Cotações, etc.) são enviadas em paralelo através de um pool de **threads daemon dedicadas**, reduzindo pela metade o tempo total da rodada diária.
2. **Step Isolado (`faturas_graphql`):** Por ser uma entidade massiva e cara na API de origem, a extração de Faturas via GraphQL possui isolamento temporal e timeout próprio, rodando após a finalização do bloco em paralelo.
3. **Data Quality Suite (Validação Final):** O pipeline conclui aplicando 5 checks automáticos sobre os dados persistidos:
   * **Unicidade:** Ausência de registros duplicados em chaves primárias de negócio.
   * **Completude:** Integridade referencial interna (ex: Manifestos com Coletas válidas).
   * **Freshness (Frescor):** Janelas operacionais e data de atualização dos registros.
   * **Integridade Referencial:** Órfãos e inconsistências de FK analíticas.
   * **Validação de Schema:** Conformidade de tipos e constraints.

---

## 🧭 Escopo das Entidades Ingeridas

O ETL divide suas operações por duas grandes trilhas de APIs da ESL Cloud, além da integração dedicada de rastreamento:

### ⚛️ Entidades via GraphQL
* **`usuarios_sistema`:** Usuários e permissões operacionais da ESL Cloud.
* **`coletas`:** Agendamentos de coletas de cargas.
* **`fretes`:** Registros financeiros e contratações de fretes.
* **`faturas_graphql`:** Emissões e liquidações de faturas operacionais detalhadas.

### 📄 Entidades via DataExport
* **`manifestos`:** Documentos de expedição e transporte.
* **`cotacoes`:** Cotações e propostas comerciais submetidas.
* **`localizacao_cargas`:** Rastreamento do status geográfico de cargas em trânsito.
* **`contas_a_pagar`:** Provisões financeiras de saídas operacionais.
* **`faturas_por_cliente`:** Faturamento consolidado agrupado por tomador de serviço.
* **`inventario`:** Posição física e controle de estoque de armazém.
* **`sinistros`:** Ocorrências de sinistros, perdas e avarias de cargas.

### 🛰️ Rastreamento Complementar (Raster API)
* **`raster_viagens` & `raster_viagem_paradas`:** Ingestão de telemetria física e SLA de trânsito de motoristas terceiros ou frota própria diretamente da Raster (quando `RASTER_ENABLED=true` ou `auto`).

---

## 🛠️ Tecnologias Utilizadas (Tech Stack)

* **Runtime & Compilação:** Java 17 (JDK 17) & Apache Maven.
* **Build Artifact:** `maven-shade-plugin` configurado para gerar um executável auto-contido (*Fat JAR* / `target/extrator.jar`).
* **Conexão & Pool:** HikariCP 5.1.0 para conexão otimizada de alta disponibilidade com SQL Server.
* **Drivers de Banco:**
  * `mssql-jdbc` (Driver JDBC oficial da Microsoft para SQL Server).
  * `sqlite-jdbc` (SQLite local para gerenciamento rápido de credenciais operacionais).
* **Processamento JSON:** Jackson Databind 2.17.2 com suporte nativo a tipos Java 8 Date/Time (`jackson-datatype-jsr310`).
* **Logging & Observabilidade:** SLF4J 2.0 & Logback Classic 1.5.

---

## ⚙️ Variáveis de Ambiente & Configurações

O ETL consome variáveis de ambiente a partir do arquivo `.env` localizado na pasta `config/` (ou na raiz do projeto dependendo da esteira de execução). Copie de `config/.env.example` para iniciar o desenvolvimento.

### Template de Configurações (`config/.env`)

```properties
# --- Configurações das APIs ---
API_BASEURL=https://rodogarcia.eslcloud.com.br
API_REST_TOKEN=token_aqui_para_apis_rest_legadas
API_GRAPHQL_TOKEN=token_aqui_para_graphql_coletas_e_fretes
API_DATAEXPORT_TOKEN=token_aqui_para_dataexport_manifestos

# --- API Raster (Rastreamento) ---
RASTER_ENABLED=auto
RASTER_LOGIN=usuario_raster
RASTER_SENHA=senha_raster
RASTER_AMBIENTE=Producao
RASTER_BASE_URL=https://integra.rastergr.com.br:8443/datasnap/rest/TWebService
RASTER_TIMEOUT_SECONDS=120
RASTER_LOOKBACK_DAYS=1

# --- Configurações do Banco de Dados Principal ---
DB_URL=jdbc:sqlserver://127.0.0.1:1433;databaseName=ETL_SISTEMA;encrypt=false;
DB_USER=usuario_seguro_etl
DB_PASSWORD=senha_segura_sql

# --- Segurança CLI ---
# Path para o banco SQLite de credenciais locais de operadores
EXTRATOR_SECURITY_DB_PATH=C:\ProgramData\ExtratorESL\security\users.db
EXTRATOR_AUTH_PEPPER=pepper_criptografico_unico_para_senhas
EXTRATOR_AUTH_MAX_TENTATIVAS=3
EXTRATOR_AUTH_BLOQUEIO_MINUTOS=5

# --- Tolerâncias Operacionais de Data Quality ---
# Se os registros inválidos ficarem abaixo disto, a carga é considerada COMPLETA com avisos
ETL_INVALIDOS_QUANTIDADE_MAX=500
ETL_INVALIDOS_PERCENTUAL_MAX=2.5

# Limite tolerável de órfãos na relação Manifestos -> Coletas
ETL_REFERENCIAL_MANIFESTOS_ORFAOS_QUANTIDADE_MAX=500
ETL_REFERENCIAL_MANIFESTOS_ORFAOS_PERCENTUAL_MAX=35.0

# --- Parâmetros de Performance e Pooling ---
DB_BATCH_SIZE=100
DB_POOL_MAX_SIZE=10
DB_POOL_MIN_IDLE=2
DB_POOL_CONN_TIMEOUT=30000
```

> [!WARNING]
> **Segurança de Credenciais:**
> * Nunca versionar o arquivo `.env` configurado.
> * Não use o usuário administrador `sa` do SQL Server em produção. Crie uma role dedicada ao ETL com privilégios estruturais DDL de atualização de views e gravação (DML) restritos ao schema do ETL.

---

## 📁 Estrutura de Pastas do Repositório

```text
etl-extracao-dados/
├── .github/               # Workflows CI/CD de builds automatizados
├── config/                # Arquivos de propriedades e templates de ambiente
│   ├── .env.example       # Template oficial para variáveis do sistema
│   └── README.md          # Informações sobre o carregamento dinâmico
├── database/              # DDLs estruturais oficiais do banco de dados (SSOT)
│   ├── tabelas/           # Criação de tabelas operacionais puras
│   ├── migrations/        # Evoluções incrementais e versionadas
│   ├── views/             # Views Power BI de consumo analítico externo
│   ├── views-dimensao/    # Views dimensionais consolidadas
│   └── executar_database.bat # Script Windows de atualização estrutural SQL Server
├── docs/                  # Base de conhecimento canônica e relatórios técnicos
│   ├── moderno/           # Documentação vigentes dividida por subsistemas
│   ├── legado/            # Histórico documental de versões antigas
│   └── index.md           # Índice principal de documentação técnica
├── logs/                  # Logs gerados localmente em tempo de execução
├── runtime/               # Estados persistentes, watchdogs e watermarks
├── scripts/               # Automações operacionais e rotinas administrativas
│   ├── ci/                # Scripts auxiliares de esteiras CI
│   └── windows/           # Arquivos .bat e .ps1 de execução prática
├── src/                   # Código-fonte Java 17
│   ├── main/
│   │   ├── java/br/com/extrator/
│   │   │   ├── bootstrap/      # Composition Root, Main e isolamento de processos
│   │   │   ├── comandos/cli/   # CommandRegistry e roteadores CLI
│   │   │   ├── aplicacao/      # Use cases, orquestrador e políticas de retry
│   │   │   ├── integracao/     # Clientes HTTP, parsers e gateways GraphQL/DataExport
│   │   │   ├── persistencia/   # Repositories SQL Server e queries nativas
│   │   │   ├── observabilidade/# Telemetrias, validadores de completude e Data Quality
│   │   │   ├── features/       # Arquitetura incremental segmentada por entidade
│   │   │   └── plataforma/     # Infraestrutura transversal e auditorias
│   │   └── resources/          # Configurações de fallback (config.properties, logback)
│   └── test/                   # Testes unitários e de integração
├── 00-PRODUCAO_START.bat  # Ponto de entrada padrão em produção
├── limpar_logs.bat        # Limpeza ágil de arquivos .log rotacionados
├── mvn.bat                # Wrapper customizado para builds locais Maven
└── pom.xml                # Arquivo POM de dependências estruturais
```

---

## 🚀 Como Executar o Projeto

O ETL foi projetado para rodar de forma automatizada no Windows Server usando os scripts centralizados em `scripts/windows/` ou o inicializador raiz.

### Modo Automático (Scripts Windows)

#### 1. Execução de Produção Padrão (Agendada/Manual)
Para rodar o ciclo padrão de extração de dados diários (janela móvel `D-1..D`), gerando logs e validações finais de integridade:
```powershell
.\00-PRODUCAO_START.bat
```
*Este script delega a execução para `scripts/windows/00-PRODUCAO_START.bat`, que valida as configurações, compila o JAR se necessário, e executa a rotina operacional oficial.*

#### 2. Executar Extração Completa
Roda a extração de todas as entidades de todas as APIs integradas sem travas adicionais de produção:
```powershell
.\scripts\windows\01-executar_extracao_completa.bat
```

#### 3. Execução Contínua Daemon (Ciclos Periódicos)
Para manter o ETL rodando em plano de fundo a cada 30 minutos, gerenciando travamentos com watchdog e executando reconciliações de cargas:
```powershell
.\scripts\windows\05-loop_extracao_30min.bat
```

#### 4. Gerenciamento de Usuários Locais (Segurança CLI)
Para cadastrar, remover ou alterar senhas dos operadores autorizados a disparar comandos manuais no terminal:
```powershell
.\scripts\windows\09-gerenciar_usuarios.bat
```

---

### Execução Manual por Linha de Comando (CLI Flags)

Caso queira executar o JAR empacotado diretamente de forma granular, o sistema disponibiliza as seguintes flags passadas ao `extrator.jar`:

#### Roda o Fluxo Completo de Ingestão Diária
```powershell
java -jar .\target\extrator.jar --fluxo-completo
```

#### Executa Extração Limitada por um Intervalo Específico (Backfill)
```powershell
java -jar .\target\extrator.jar --extracao-intervalo --data-inicio 2026-05-01 --data-fim 2026-05-15 --entidade manifestos
```

#### Executa a Validação de Integridade API x Banco (Últimas 24 horas)
```powershell
java -jar .\target\extrator.jar --validar-api-banco-24h
```

#### Testa Conexão e Credenciais de uma API Específica
```powershell
java -jar .\target\extrator.jar --testar-api --api graphql
```

---

## 🛡️ Resiliência & Tolerância a Falhas

Como o ETL lida com APIs externas da ESL Cloud que sofrem com instabilidade operacional periódica, o sistema possui camadas nativas de proteção:

* **Watchdog de Timeout de Etapa:** Caso uma chamada GraphQL ou DataExport trave na API externa, o watchdog local encerra a thread após tempo limite (configurado) para evitar congelamento indefinido do processo.
* **Isolamento por Processo:** Etapas de alto risco ou muito grandes podem ser isoladas em subprocessos gerados pelo Java. Se o subprocesso cair por falta de memória (OOM) ou falhas na rede, o processo pai (CLI) captura o erro, salva a telemetria degradada e continua a fila.
* **Políticas de Retry com Backoff Exponencial:** Erros transitórios de rede (HTTP 502, 503, 504) ou limites de requisições (HTTP 429) disparam retentativas automáticas com espaçamento temporal crescente.
* **Circuit Breaker Operacional:** Se uma API externa retornar erros sequenciais por repetidas tentativas, o ETL abre o circuito para aquela API, marcando o step como `DEGRADED`, alertando os administradores e permitindo a continuidade saudável de outras fontes (ex: se DataExport cair, o GraphQL continua rodando).

---

## ❓ Resolução de Problemas (Troubleshooting)

### 1. Mensagem de Erro: `sqlcmd não encontrado no PATH` ao atualizar banco
* **Causa:** O script `executar_database.bat` depende do utilitário `sqlcmd` da Microsoft para injetar DDLs.
* **Solução:** Instale o *SQL Server Command Line Utilities* da Microsoft ou adicione o caminho do SDK do SQL Server nas Variáveis de Ambiente do Sistema (PATH).
  * Exemplo de diretório padrão: `C:\Program Files\Microsoft SQL Server\Client SDK\ODBC\170\Tools\Binn`.

### 2. Status `DEGRADED` ou Órfãos Elevados em Coletas
* **Causa:** A relação referencial entre Manifestos e Coletas ultrapassou a taxa definida de tolerância. Pode ocorrer por atraso de eventos na ESL Cloud (*late-arrival*).
* **Solução:** Execute o script `06-relatorio-completo-validacao.bat` para identificar o ID dos manifestos órfãos e dispare uma rodada de recovery para a janela afetada usando `--extracao-intervalo`.

### 3. Falha de Inicialização do Pool (HikariCP)
* **Causa:** O documento de banco de dados SQL Server rejeitou as credenciais ou a URL de conexão está incorreta.
* **Solução:** Revise o arquivo `config/.env`, garanta que o banco de dados `ETL_SISTEMA` ou `esl_cloud` foi devidamente criado e que o usuário possui as credenciais configuradas ativas. Execute `03-validar_config.bat` para diagnosticar.

---

## 📚 Documentação de Apoio Detalhada

Para aprofundar-se em aspectos de design e engenharia interna, consulte os guias em `/docs`:

* 📂 [docs/index.md](docs/index.md): O índice de entrada oficial de toda a base de conhecimento.
* 🏛️ [docs/moderno/arquitetura.md](docs/moderno/arquitetura.md): Mapa detalhado de camadas do código Java, dependências e composition root.
* 🔄 [docs/moderno/pipeline.md](docs/moderno/pipeline.md): Composição e orquestração de paralelismo dos steps.
* 🧠 [docs/decisions.md](docs/decisions.md): Registro histórico de decisões de arquitetura e refatorações aplicadas.
* 📖 [docs/glossary.md](docs/glossary.md): Dicionário de termos e regras de negócio de logística e fretes utilizados no código.
