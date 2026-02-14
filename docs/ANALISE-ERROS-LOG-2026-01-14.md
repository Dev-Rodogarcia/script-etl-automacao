# 🔍 Análise de Erros - Log de Extração 2026-01-14 17:21:00

## 📋 Resumo Executivo

**Status**: ❌ **CRÍTICO** - Sistema não operacional devido a tabelas faltando no banco de dados

**Duração da execução**: 4 min 57 s  
**Runners falhados**: 2/2 (GraphQL e DataExport)  
**Taxa de sucesso**: 0%

---

## 🚨 Problemas Identificados

### 1. **TABELAS FALTANDO NO BANCO DE DADOS** ⚠️ **CRÍTICO**

#### Erros Encontrados:
- ❌ `Invalid object name 'dbo.dim_usuarios'` (linha 202)
- ❌ `Invalid object name 'dbo.log_extracoes'` (linhas 213, 345, 392)
- ❌ `Invalid object name 'dbo.page_audit'` (linhas 275, 545, 608, 680)

#### Impacto:
- **Bloqueio total** das extrações
- Impossibilidade de salvar dados
- Impossibilidade de gravar logs de auditoria
- Falhas em cascata em todas as entidades

#### Causa Raiz:
Os scripts SQL existem na pasta `database/tabelas/`:
- ✅ `009_criar_tabela_log_extracoes.sql` - Existe
- ✅ `010_criar_tabela_page_audit.sql` - Existe  
- ✅ `011_criar_tabela_dim_usuarios.sql` - Existe

**Mas o banco de dados não foi executado ou as tabelas não foram criadas.**

#### Solução Imediata:
```bash
# 1. Executar o script de criação do banco
cd database
executar_database.bat

# 2. Verificar se as tabelas foram criadas
sqlcmd -S localhost -d esl_cloud -E -Q "SELECT name FROM sys.tables WHERE name IN ('log_extracoes', 'page_audit', 'dim_usuarios')"
```

#### Solução Preventiva:
Adicionar validação no início da aplicação para verificar se as tabelas essenciais existem:

```java
// Em CarregadorConfig ou GerenciadorConexao
public static void validarTabelasEssenciais() {
    final String[] tabelasEssenciais = {
        "log_extracoes",
        "page_audit", 
        "dim_usuarios"
    };
    
    for (final String tabela : tabelasEssenciais) {
        if (!tabelaExiste(tabela)) {
            throw new IllegalStateException(
                String.format("❌ ERRO CRÍTICO: Tabela '%s' não existe. Execute 'database/executar_database.bat' antes de rodar a aplicação.", tabela)
            );
        }
    }
}
```

---

### 2. **CURSOR REPETIDO NA EXTRAÇÃO DE USUÁRIOS** ⚠️ **MÉDIO**

#### Erro Encontrado:
- Linha 183: `🚨 PROTEÇÃO ATIVADA - Entidade individual: Cursor repetido detectado (MjA). A API retornou o mesmo cursor que foi enviado E indicou hasNextPage=true. Interrompendo busca para evitar loop infinito.`
- Linha 184: `⚠️ Query GraphQL INCOMPLETA - Entidade individual: 40 registros extraídos em 1 páginas (INTERROMPIDA por proteções)`

#### Análise:
1. **Primeira requisição**: `after=<inicio>` → retorna 20 registros, `endCursor=MjA`, `hasNextPage=true`
2. **Segunda requisição**: `after=MjA` → retorna 20 registros, `endCursor=MjA` (mesmo cursor!), `hasNextPage=true`
3. **Proteção ativada**: Sistema interrompe para evitar loop infinito

#### Possíveis Causas:
1. **Bug da API GraphQL**: A API pode estar retornando `hasNextPage=true` incorretamente quando não há mais páginas
2. **Comportamento esperado**: Quando há exatamente 20 registros (tamanho da página), a API pode retornar o mesmo cursor na última página
3. **Problema de paginação**: A query pode estar usando `first: 100` mas retornando apenas 20 por página

#### Solução Imediata:
Melhorar a proteção para detectar se realmente há mais dados:

```java
// Adicionar verificação adicional: se a página retornou menos registros que o esperado
// e cursor repetido, pode ser a última página
if (novoCursor != null && cursor != null && novoCursor.equals(cursor)) {
    if (resposta.getHasNextPage()) {
        // Verificar se a página retornou menos registros que o esperado
        final int registrosEsperados = 100; // first: 100
        final int registrosRecebidos = resposta.getEntidades().size();
        
        if (registrosRecebidos < registrosEsperados) {
            // Página incompleta + cursor repetido = provavelmente última página
            logger.warn("⚠️ Entidade {}: Cursor repetido ({}) mas página incompleta ({} < {}). Tratando como última página.", 
                nomeEntidade, novoCursor, registrosRecebidos, registrosEsperados);
            interrompido = false; // Não interromper, tratar como última página
        } else {
            // Página completa + cursor repetido + hasNextPage=true = loop infinito
            logger.warn("🚨 PROTEÇÃO ATIVADA - Entidade {}: Cursor repetido detectado ({}). Interrompendo busca.", 
                nomeEntidade, novoCursor);
            interrompido = true;
            break;
        }
    }
}
```

#### Solução Alternativa:
Aumentar o `first` na query de usuários para evitar paginação quando há poucos registros:

```graphql
query ExtrairUsuariosSistema($params: IndividualInput!, $cursor: String) {
  individual(params: $params, first: 1000, after: $cursor) {  # Aumentar de 100 para 1000
    # ...
  }
}
```

---

### 3. **FALHAS EM CASCATA** ⚠️ **ALTO**

#### Problema:
Quando uma tabela não existe, todas as operações subsequentes falham:
1. ❌ Falha ao salvar `usuarios_sistema` → `dim_usuarios` não existe
2. ❌ Falha ao gravar log → `log_extracoes` não existe
3. ❌ Falha ao gravar page_audit → `page_audit` não existe
4. ❌ Falha na validação de completude → `page_audit` não existe

#### Impacto:
- **0% de sucesso** nas extrações
- Impossibilidade de diagnosticar problemas (sem logs)
- Impossibilidade de auditar requisições (sem page_audit)

#### Solução:
Implementar tratamento gracioso de erros:

```java
// Em LogExtracaoRepository
public void gravarLogExtracao(final LogExtracaoEntity logExtracao) {
    try {
        // Tentar gravar normalmente
        // ...
    } catch (final SQLException e) {
        if (e.getMessage().contains("Invalid object name")) {
            // Tabela não existe - logar erro mas não interromper execução
            logger.error("❌ ERRO CRÍTICO: Tabela 'log_extracoes' não existe. Execute 'database/executar_database.bat' antes de rodar a aplicação.");
            logger.error("   Log de extração não foi gravado, mas a execução continuará.");
            // Não lançar exceção - permitir que a extração continue
            return;
        }
        throw new RuntimeException("Falha ao gravar log de extração", e);
    }
}
```

**⚠️ NOTA**: Esta solução permite que a extração continue, mas os logs não serão salvos. É uma solução temporária até que o banco seja configurado corretamente.

---

## 📊 Estatísticas dos Erros

| Tipo de Erro | Quantidade | Severidade | Impacto |
|--------------|------------|------------|---------|
| Tabela `dim_usuarios` não existe | 1 | 🔴 CRÍTICO | Bloqueia extração de usuários |
| Tabela `log_extracoes` não existe | 3 | 🔴 CRÍTICO | Bloqueia gravação de logs |
| Tabela `page_audit` não existe | 4 | 🔴 CRÍTICO | Bloqueia auditoria de páginas |
| Cursor repetido (usuários) | 1 | 🟡 MÉDIO | Interrompe extração prematuramente |
| **TOTAL** | **9** | - | **Sistema não operacional** |

---

## ✅ Plano de Ação Recomendado

### Prioridade 1 - CRÍTICO (Executar Imediatamente):
1. ✅ **Executar `database/executar_database.bat`** para criar todas as tabelas
2. ✅ **Verificar criação das tabelas** via SQL:
   ```sql
   SELECT name FROM sys.tables 
   WHERE name IN ('log_extracoes', 'page_audit', 'dim_usuarios')
   ORDER BY name;
   ```
3. ✅ **Re-executar extração** após criação das tabelas

### Prioridade 2 - ALTO (Implementar em Breve):
1. ✅ **Adicionar validação de tabelas essenciais** no início da aplicação
2. ✅ **Melhorar tratamento de erros** para não interromper execução quando tabelas não existem (com avisos claros)
3. ✅ **Adicionar verificação no `executar_database.bat`** para garantir que todas as tabelas foram criadas

### Prioridade 3 - MÉDIO (Melhorias):
1. ✅ **Melhorar proteção de cursor repetido** para detectar última página corretamente
2. ✅ **Aumentar `first` na query de usuários** para evitar paginação desnecessária
3. ✅ **Adicionar logs mais informativos** sobre o estado do banco de dados

---

## 🔧 Comandos de Diagnóstico

### Verificar se tabelas existem:
```sql
-- Verificar tabelas essenciais
SELECT 
    CASE WHEN EXISTS (SELECT 1 FROM sys.tables WHERE name = 'log_extracoes') 
         THEN '✅ Existe' ELSE '❌ Não existe' END AS log_extracoes,
    CASE WHEN EXISTS (SELECT 1 FROM sys.tables WHERE name = 'page_audit') 
         THEN '✅ Existe' ELSE '❌ Não existe' END AS page_audit,
    CASE WHEN EXISTS (SELECT 1 FROM sys.tables WHERE name = 'dim_usuarios') 
         THEN '✅ Existe' ELSE '❌ Não existe' END AS dim_usuarios;
```

### Verificar estrutura das tabelas:
```sql
-- Verificar colunas de log_extracoes
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_NAME = 'log_extracoes'
ORDER BY ORDINAL_POSITION;

-- Verificar colunas de page_audit
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_NAME = 'page_audit'
ORDER BY ORDINAL_POSITION;

-- Verificar colunas de dim_usuarios
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_NAME = 'dim_usuarios'
ORDER BY ORDINAL_POSITION;
```

---

## 📝 Notas Importantes

1. **O banco de dados precisa ser executado ANTES de rodar a aplicação**
2. **As tabelas `log_extracoes` e `page_audit` são essenciais** para o funcionamento do sistema
3. **A tabela `dim_usuarios` é necessária** para enriquecer dados de coletas
4. **O problema do cursor repetido** pode ser resolvido melhorando a lógica de detecção de última página

---

## 🎯 Conclusão

O sistema está **não operacional** devido a **tabelas faltando no banco de dados**. Este é um problema de **configuração/inicialização** e não um bug do código.

**Ação imediata necessária**: Executar `database/executar_database.bat` para criar todas as tabelas necessárias.

Após criar as tabelas, o sistema deve funcionar normalmente, exceto pelo problema do cursor repetido que pode ser resolvido com as melhorias sugeridas.
