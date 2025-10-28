# Testes Isolados das APIs ESL Cloud

Este documento descreve como usar os arquivos `.bat` criados para testar cada API da ESL Cloud de forma isolada, verificando a conformidade com as regras estabelecidas.

## 📋 Arquivos de Teste Disponíveis

### 1. `testar_api_rest.bat`
- **Função**: Testa apenas a API REST
- **Endpoints testados**:
  - Faturas a Receber
  - Faturas a Pagar
  - Ocorrências
- **Características**: Paginação automática, throttling configurável

### 2. `testar_api_graphql.bat`
- **Função**: Testa apenas a API GraphQL
- **Endpoints testados**:
  - Coletas (Conhecimentos)
  - Fretes
- **Características**: Paginação cursor-based, introspecção de schema

### 3. `testar_api_dataexport.bat`
- **Função**: Testa apenas a API Data Export
- **Endpoints testados**:
  - Manifestos
  - Cotações
  - Localização de Carga
- **Características**: Processamento de arquivos Excel/CSV

## 🚀 Como Executar os Testes

### Pré-requisitos
1. **Compilar o projeto**:
   ```bash
   mvn clean package
   ```

2. **Verificar configurações** em `config.properties`:
   - `api.throttling.padrao_ms=5000` (padrão: 5 segundos)
   - `api.retry.max_tentativas=5`
   - `api.retry.delay_base_ms=2000`

### Execução
1. **Duplo clique** no arquivo `.bat` desejado
2. **Ou via linha de comando**:
   ```cmd
   testar_api_rest.bat
   testar_api_graphql.bat
   testar_api_dataexport.bat
   ```

## ✅ Conformidade com Regras ESL Cloud

### 1. Rate Limit (Aplicado a todas as APIs)
- ✅ **Intervalo mínimo**: 2 segundos entre requisições
- ✅ **Escopo**: Por tenant (rodogarcia) e IP público
- ✅ **Capacidade**: ~30 req/min, ~1.800 req/hora
- ✅ **Tratamento HTTP 429**: Retry automático com backoff

### 2. Implementação de Boas Práticas
- ✅ **Throttling obrigatório**: Configurado para 5000ms (acima do mínimo)
- ✅ **Retry para erro 429**: Implementado com backoff exponencial
- ✅ **Controle de concorrência**: Serialização automática
- ✅ **Timeout**: 30 segundos por requisição

### 3. Regras Específicas da API Data Export
- ✅ **< 31 dias**: Sem limite de horas entre extrações
- ✅ **31 dias - 6 meses**: Limite de 1 hora entre extrações
- ✅ **> 6 meses**: Limite de 12 horas entre extrações

## 📊 Monitoramento e Logs

### Arquivos Gerados
- **Logs**: `logs/` - Logs detalhados de execução
- **Métricas**: `metricas/` - Estatísticas de performance
- **Dados**: `dados/` - Arquivos baixados (Data Export)

### Verificação na Plataforma TMS
- Acesse: `Parametrizações → Consumo`
- Monitore: Número de chamadas por rota
- Verifique: Conformidade com limites

## 🔧 Configurações Avançadas

### Variáveis de Ambiente (Opcionais)
```cmd
set API_THROTTLING_PADRAO_MS=2000
set API_RETRY_MAX_TENTATIVAS=3
set API_TIMEOUT_MS=30000
```

### Parâmetros JVM
```cmd
-Dfile.encoding=UTF-8
-Djava.util.logging.config.file=src/main/resources/logging.properties
```

## 🐛 Solução de Problemas

### Erro: JAR não encontrado
```cmd
mvn clean package
```

### Erro HTTP 429 (Too Many Requests)
- ✅ **Automático**: O sistema já implementa retry
- ⚠️ **Manual**: Aumente `api.throttling.padrao_ms` se necessário

### Timeout de Conexão
- Verifique conectividade com ESL Cloud
- Considere aumentar `api.timeout.ms`

### Arquivos não baixados (Data Export)
- Verifique permissões na pasta `dados/`
- Confirme formato do arquivo (Excel/CSV)

## 📈 Métricas Importantes

### Performance Esperada
- **REST**: ~100-500 registros por requisição
- **GraphQL**: ~50-200 registros por página
- **Data Export**: Arquivos completos (tamanho variável)

### Indicadores de Sucesso
- ✅ Zero erros HTTP 429 após retry
- ✅ Tempo médio entre requisições ≥ 2 segundos
- ✅ Taxa de sucesso > 95%
- ✅ Logs sem erros críticos

## Estrutura de Testes

### Testes de Integração
- Validação de conectividade com APIs
- Testes de fluxo completo de extração
- Verificação de integridade de dados

### Testes Unitários
- Validação de componentes individuais
- Testes de lógica de negócio
- Verificação de utilitários e helpers

## 🔍 Funcionalidades Especiais

### API GraphQL - Introspecção
```cmd
java -jar target/extrator-1.0-SNAPSHOT.jar --introspeccao
```

### Teste de Conectividade
```cmd
java -jar target/extrator-1.0-SNAPSHOT.jar --test-connection
```

## 📞 Suporte

Em caso de problemas:
1. Verifique os logs em `logs/`
2. Consulte métricas em `metricas/`
3. Monitore consumo na plataforma TMS
4. Ajuste configurações em `config.properties`

---

**Última atualização**: Janeiro 2025  
**Versão**: 1.0  
**Compatibilidade**: ESL Cloud APIs v2024