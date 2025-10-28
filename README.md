# Extrator de Dados ESL Cloud

Script de automação em Java que extrai dados de múltiplas APIs do ESL Cloud e carrega em SQL Server, com coleta automática de métricas de execução.

## 🚀 Início Rápido

### Scripts de Automação (.bat)
Para facilitar o uso e executar cada API de forma independente:

```bash
# Testar API REST (Faturas e Ocorrências)
testar_api_rest.bat

# Testar API GraphQL (Coletas e Fretes)
testar_api_graphql.bat

# Testar API Data Export (Manifestos, Cotações, Localização da Carga)
testar_api_dataexport.bat
```

### Execução Manual

#### 1. Compilar o Projeto
```bash
mvn clean package
```

#### 2. Executar Script de Extração
```bash
# Executar uma API específica (janela padrão: últimas 24 horas)
java -jar target/extrator-script.jar --testar-api rest
java -jar target/extrator-script.jar --testar-api graphql
java -jar target/extrator-script.jar --testar-api dataexport
```

## 📋 Funcionalidades

- **3 APIs Integradas**: REST, GraphQL e Data Export
- **Script de Linha de Comando**: Execução única para extração de dados
- **Scripts de Automação**: 4 scripts .bat para facilitar operações
- **Prevenção de Duplicatas**: Sistema MERGE para evitar dados duplicados
- **Logs Detalhados**: Acompanhamento completo das operações
- **Métricas Automáticas**: Coleta de performance e estatísticas salvas em JSON
- **Validação de APIs**: Teste de conectividade sem executar extração
- **Introspecção GraphQL**: Descoberta de tipos e campos disponíveis

## ⚙️ Configuração

Configure as variáveis de ambiente ou edite `src/main/resources/config.properties`:

```bash
# Variáveis de Ambiente (Recomendado)
$env:API_BASEURL="https://sua-empresa.eslcloud.com.br"
$env:API_REST_TOKEN="seu_token_rest"
$env:API_GRAPHQL_TOKEN="seu_token_graphql"
$env:API_DATAEXPORT_TOKEN="seu_token_dataexport"
$env:DB_URL="jdbc:sqlserver://localhost:1433;databaseName=esl_cloud"
$env:DB_USER="sa"
$env:DB_PASSWORD="sua_senha"
```

## 🏗️ Arquitetura

### APIs Suportadas
- **API REST**: Faturas e Ocorrências
- **API GraphQL**: Coletas e Fretes  
- **API Data Export**: Manifestos e Localização da Carga

### Componentes
- **Runners Independentes** (`runners/*.java`): Execução desacoplada por API
- **Script de Entrada** (`Main.java`): Apenas despacha por `--testar-api`
- **Serviços de Logging** (`LoggingService.java`): Sistema de logs estruturados
- **Clientes de API**: Integração com REST, GraphQL e Data Export

## 📚 Documentação

- **[Guia de Instalação](docs/INSTRUCOES.md)**: Configuração detalhada
- **[Arquitetura Técnica](docs/ARQUITETURA-TECNICA.md)**: Detalhes técnicos
## 📊 Monitoramento e Logs

- **Sistema de Auditoria**: Validação automática de integridade de dados
- **Logs Estruturados**: Rastreamento detalhado de execuções
- **Relatórios**: Geração automática de relatórios de execução

### Documentação Técnica

- **Auditoria**: `docs/auditoria/` - Validação de integridade de dados
- **Configuração**: `docs/configuracao/` - Guias de setup e configuração
- **Desenvolvimento**: `docs/desenvolvimento/` - Padrões e práticas de código

## 🔧 Requisitos Técnicos

- **Java 17** (LTS)
- **Maven 3.6+**
- **SQL Server** com JDBC Driver 13.2.0.jre11
- **Acesso às APIs ESL Cloud**

### Dependências Principais
- Jackson (processamento JSON)
- SQL Server JDBC Driver 13.2.0.jre11
- SLF4J (logging)

## 📊 Métricas e Monitoramento

O sistema gera métricas automáticas em `metricas/metricas-YYYY-MM-DD.json` com:
- Tempos de execução por API
- Quantidade de registros processados
- Taxa de sucesso/falha (objetivo: 100%)
- Performance geral (registros/segundo)
- Histórico de execuções

## 🆘 Suporte e Troubleshooting

Para problemas ou dúvidas:
1. **Verifique os logs** em `logs/`
2. **Consulte a documentação** em `docs/`
3. **Execute o teste de validação**: `java -jar target/extrator-script.jar --validar`
4. **Use os scripts .bat** para operações padronizadas
5. **Monitore as métricas** para identificar problemas de performance

## 🔄 Atualizações Recentes

- ✅ **Refatoração para Script de Linha de Comando** (remoção do dashboard web)
- ✅ **Remoção de dependências web** (Spring Boot Web, Actuator)
- ✅ **Otimização do JAR final** (redução significativa do tamanho)
- ✅ **Métricas aprimoradas** com salvamento automático em JSON
- ✅ **Scripts de automação** (.bat) para facilitar operações
- ✅ **Validação e introspecção** de APIs integradas