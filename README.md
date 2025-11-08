# Extrator de Dados ESL Cloud

Script de automação em Java que extrai dados de múltiplas APIs do ESL Cloud e carrega em SQL Server, com coleta automática de métricas de execução.

**Versão:** 2.1.0 | **Última Atualização:** 07/11/2025

## 🚀 Início Rápido

### 1. Compilar
```bash
mvn clean package
```

### 2. Executar Extração
```bash
# Extração completa (todas as APIs)
01-executar_extracao_completa.bat

# Ou executar APIs específicas
02-testar_api_especifica.bat
```

### 3. Validar Dados
```sql
-- Verificar faturas a pagar (com novos campos v2.0)
SELECT TOP 10 
    id, document_number, filial, cnpj_filial,
    conta_contabil, centro_custo, status, total_value
FROM faturas_a_pagar
ORDER BY data_extracao DESC;
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
- **Paginação Completa**: Sistema robusto de paginação que garante 100% de cobertura
- **Exportação CSV**: Exportação completa de todos os dados extraídos

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

**⚠️ Segurança:** Tokens e credenciais nunca devem ser commitados no Git. Use variáveis de ambiente ou arquivos `.env` (adicionados ao `.gitignore`).

## 🏗️ Arquitetura

### APIs Suportadas
- **API REST**: Faturas e Ocorrências
- **API GraphQL**: Coletas e Fretes  
- **API Data Export**: Manifestos, Cotações e Localização da Carga

### Componentes
- **Runners Independentes** (`runners/*.java`): Execução desacoplada por API
- **Script de Entrada** (`Main.java`): Apenas despacha por `--testar-api`
- **Serviços de Logging** (`LoggingService.java`): Sistema de logs estruturados
- **Clientes de API**: Integração com REST, GraphQL e Data Export
- **Repositories**: Persistência robusta com validação e tratamento de erros
- **Exportador CSV**: Exportação completa de dados para análise

## 📚 Documentação

### 📖 Documentação Principal

A documentação foi **reorganizada** (07/11/2025) para melhor navegação. Consulte:

- **[📚 Documentação Completa](docs/README.md)** - Índice principal da documentação
- **[🚀 Início Rápido](docs/01-inicio-rapido/)** - Guias de início rápido
- **[🔌 APIs](docs/02-apis/)** - Documentação completa das APIs
- **[⚙️ Configuração](docs/03-configuracao/)** - Guias de configuração e troubleshooting
- **[📋 Especificações Técnicas](docs/04-especificacoes-tecnicas/)** - Detalhes técnicos de implementação

### 📁 Estrutura da Documentação

```
docs/
├── 00-documentos-gerais/           # Documentos gerais do projeto
├── 01-inicio-rapido/              # Guias de início rápido
├── 02-apis/                       # Documentação de APIs (REST, GraphQL, DataExport)
├── 03-configuracao/               # Configuração e troubleshooting
├── 04-especificacoes-tecnicas/    # Especificações técnicas
├── 05-versoes/                    # Documentação de versões
├── 06-referencias/                # Referências e templates
├── 07-ideias-futuras/             # Ideias e melhorias futuras
├── 08-arquivos-secretos/          # Arquivos sensíveis (NÃO versionados no Git)
└── relatorios-diarios/            # Relatórios diários de desenvolvimento
```

### 🔒 Segurança

**Verificação de Segurança Realizada (07/11/2025):**
- ✅ Todos os tokens de API foram censurados na documentação
- ✅ Credenciais AWS foram censuradas
- ✅ Senhas foram censuradas
- ✅ Arquivos sensíveis movidos para `docs/08-arquivos-secretos/` (no `.gitignore`)

**Importante:** A pasta `docs/08-arquivos-secretos/` contém informações sensíveis e **não é versionada no GitHub**. Sempre verifique credenciais antes de compartilhar documentação.

### 📊 Documentação por Versão

- **[Versão 2.1](docs/05-versoes/v2.1/)** - Melhorias de paginação e exportação CSV
- **[Versão 2.0](docs/05-versoes/v2.0/)** - Expansão de campos REST

## 📊 Monitoramento e Logs

- **Sistema de Auditoria**: Validação automática de integridade de dados
- **Logs Estruturados**: Rastreamento detalhado de execuções
- **Relatórios**: Geração automática de relatórios de execução
- **Exportação CSV**: Exportação completa de dados para análise externa

### Logs Disponíveis

- **Logs de Extração**: `logs/extracao_dados_YYYY-MM-DD_HH-MM-SS.log`
- **Logs de Exportação**: `logs/exportacao_csv_YYYY-MM-DD_HH-MM-SS.log`
- **Métricas**: `metricas/metricas-YYYY-MM-DD.json`

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
- Cobertura de dados (porcentagem de registros extraídos)

## 🆘 Suporte e Troubleshooting

Para problemas ou dúvidas:
1. **Verifique os logs** em `logs/`
2. **Consulte a documentação** em `docs/`
3. **Execute o teste de validação**: `java -jar target/extrator-script.jar --validar`
4. **Use os scripts .bat** para operações padronizadas
5. **Monitore as métricas** para identificar problemas de performance
6. **Consulte a seção de Troubleshooting**: `docs/03-configuracao/troubleshooting/`

## ✨ Novidades v2.1 - Melhorias de Extração e Exportação

### Melhorias de Paginação
- ✅ **Paginação Completa**: Sistema robusto que garante 100% de cobertura
- ✅ **Timeout Dinâmico**: Timeouts específicos por template (120s para Manifestos)
- ✅ **Logs Detalhados**: Rastreamento completo de cada página processada
- ✅ **Tratamento de Erros**: Recuperação automática de falhas intermediárias

### Melhorias de Exportação CSV
- ✅ **Exportação Completa**: Garante que todos os registros do banco sejam exportados
- ✅ **Ordenação Consistente**: Ordenação por chave primária para consistência
- ✅ **Validação de Integridade**: Verificação automática de discrepâncias
- ✅ **Logs de Progresso**: Acompanhamento detalhado da exportação

### Melhorias de Persistência
- ✅ **Validação Robusta**: Validação completa de dados antes de salvar
- ✅ **Truncamento Inteligente**: Truncamento automático de strings longas
- ✅ **Logs Detalhados**: Rastreamento completo de operações de salvamento
- ✅ **Tratamento de Erros**: Recuperação automática de falhas individuais

## ✨ Novidades v2.0 - Expansão de Campos REST

### Faturas a Pagar - Novos Campos (14/24)
- ✅ **+27% mais dados** extraídos (14 vs 11 campos)
- ✅ **Status automático** calculado (Pendente/Vencido/Indefinido)
- ✅ **Análise por filial** (CNPJ + nome da filial)
- ✅ **Dados contábeis** (conta contábil + centros de custo)
- ✅ **Observações** (comentários das faturas)
- ✅ **Preparado para o futuro** (10 campos placeholder)

### Novos Campos Disponíveis
1. `corporation.cnpj` → cnpjFilial
2. `corporation.nickname` → filial
3. `receiver.cnpjCpf` → cnpjFornecedor
4. `comments` → observacoes
5. `accounting_planning_management.name` → contaContabil
6. `cost_centers[].name` → centroCusto (concatenado)
7. `[CALCULADO]` → status (Pendente/Vencido)

**Documentação completa:** `docs/05-versoes/v2.0/`

## 🔄 Changelog

### v2.1.0 (07/11/2025)
- ✅ **Reorganização da Documentação**: Nova estrutura hierárquica mais clara
- ✅ **Verificação de Segurança**: Censura de todos os tokens e credenciais
- ✅ **Melhorias de Paginação**: Sistema robusto que garante 100% de cobertura
- ✅ **Melhorias de Exportação CSV**: Exportação completa e validada
- ✅ **Melhorias de Persistência**: Validação robusta e tratamento de erros

### v2.0.0 (04/11/2025)
- ✅ **Expansão de Campos REST**: +27% mais dados extraídos
- ✅ **Refatoração para Script de Linha de Comando** (remoção do dashboard web)
- ✅ **Remoção de dependências web** (Spring Boot Web, Actuator)
- ✅ **Otimização do JAR final** (redução significativa do tamanho)
- ✅ **Métricas aprimoradas** com salvamento automático em JSON
- ✅ **Scripts de automação** (.bat) para facilitar operações
- ✅ **Validação e introspecção** de APIs integradas

## 📝 Notas Importantes

### Segurança
- ⚠️ **NUNCA** commite tokens ou credenciais no Git
- ⚠️ **SEMPRE** use variáveis de ambiente ou arquivos `.env`
- ⚠️ **VERIFIQUE** a documentação antes de compartilhar publicamente
- ✅ Arquivos sensíveis estão em `docs/08-arquivos-secretos/` (no `.gitignore`)

### Performance
- 📊 **Cobertura Alvo**: 100% dos registros disponíveis
- 📊 **Performance Alvo**: >1000 registros/segundo
- 📊 **Taxa de Sucesso Alvo**: 100%

### Manutenção
- 📚 **Documentação**: Mantida atualizada em `docs/`
- 🔄 **Versionamento**: Sempre documente mudanças importantes
- 📝 **Logs**: Sempre verifique os logs para debugging

## 🤝 Contribuindo

Para contribuir com o projeto:
1. **Leia a documentação** completa em `docs/`
2. **Siga os padrões** de código estabelecidos
3. **Teste suas alterações** antes de commitar
4. **Documente** mudanças importantes
5. **Verifique segurança** antes de commitar credenciais

## 📄 Licença

Este projeto é interno e proprietário. Todos os direitos reservados.

---

**Última Atualização:** 07/11/2025  
**Versão:** 2.1.0  
**Status:** ✅ Estável
