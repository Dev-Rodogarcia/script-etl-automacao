# ⚡ Início Rápido - Atualização v2.0

## 🎯 Começar em 3 Passos

### 1️⃣ Configurar e Compilar (2 min)

```bash
# Execute este script (não requer administrador)
00-configurar_java_e_compilar.bat
```

**O que faz:**
- ✅ Configura JAVA_HOME temporariamente
- ✅ Compila o projeto
- ✅ Gera o JAR executável

**Resultado esperado:**
```
BUILD SUCCESS
JAR gerado: target\extrator.jar
```

---

### 2️⃣ Executar Extração (2 min)

```bash
# Execute a extração completa
01-executar_extracao_completa.bat
```

**O que faz:**
- ✅ Extrai dados da API REST
- ✅ Processa os 14 campos disponíveis
- ✅ Calcula status automaticamente
- ✅ Salva no SQL Server

**Resultado esperado:**
```
Extração concluída com sucesso!
X faturas processadas
```

---

### 3️⃣ Validar Dados (1 min)

Abra o SQL Server Management Studio e execute:

```sql
-- Verificar novos campos
SELECT TOP 10
    id,
    document_number,
    filial,           -- ✨ NOVO
    cnpj_filial,      -- ✨ NOVO
    conta_contabil,   -- ✨ NOVO
    centro_custo,     -- ✨ NOVO
    status,           -- ✨ NOVO (calculado)
    observacoes,      -- ✨ NOVO
    total_value,
    due_date
FROM faturas_a_pagar
ORDER BY data_extracao DESC;
```

**Resultado esperado:**
- ✅ Novos campos populados
- ✅ Status calculado (Pendente/Vencido)
- ✅ Dados fazem sentido

---

## 🎉 Pronto!

Se você vê dados nas colunas novas, a atualização v2.0 está funcionando!

---

## 📊 Consultas Úteis

### Dashboard Executivo
```sql
SELECT 
    COUNT(*) as total_faturas,
    SUM(total_value) as valor_total,
    COUNT(CASE WHEN status = 'Vencido' THEN 1 END) as vencidas,
    COUNT(DISTINCT filial) as qtd_filiais
FROM faturas_a_pagar
WHERE data_extracao >= CAST(GETDATE() AS DATE);
```

### Faturas Vencidas por Fornecedor
```sql
SELECT 
    receiver_name,
    COUNT(*) as qtd_vencidas,
    SUM(total_value) as valor_vencido,
    DATEDIFF(DAY, MIN(due_date), GETDATE()) as dias_atraso_maximo
FROM faturas_a_pagar
WHERE status = 'Vencido'
GROUP BY receiver_name
ORDER BY valor_vencido DESC;
```

### Análise por Filial
```sql
SELECT 
    filial,
    cnpj_filial,
    COUNT(*) as qtd_faturas,
    SUM(total_value) as valor_total,
    SUM(CASE WHEN status = 'Vencido' THEN total_value ELSE 0 END) as valor_vencido
FROM faturas_a_pagar
WHERE data_extracao >= CAST(GETDATE() AS DATE)
GROUP BY filial, cnpj_filial
ORDER BY valor_total DESC;
```

---

## 🐛 Problemas?

### ❌ Erro: JAVA_HOME não configurado
**Solução:** Execute `00-configurar_java_e_compilar.bat`  
**Detalhes:** Veja `SOLUCAO_JAVA_HOME.md`

### ❌ Novos campos estão NULL
**Causa:** Normal para alguns campos  
**Solução:** Verifique `header_metadata` para confirmar dados

### ❌ Status sempre "Indefinido"
**Causa:** `due_date` está NULL  
**Solução:** Verifique se a API retorna a data de vencimento

---

## 📚 Documentação Completa

| Documento | Quando Usar |
|-----------|-------------|
| `SOLUCAO_JAVA_HOME.md` | Problemas de compilação |
| `docs/README_ATUALIZACAO_REST.md` | Visão geral completa |
| `docs/GUIA_RAPIDO_v2.0.md` | Guia detalhado |
| `docs/EXEMPLOS_USO_NOVOS_CAMPOS.md` | Consultas SQL avançadas |
| `docs/CHECKLIST_VALIDACAO_CAMPOS.md` | Testes completos |

---

## ✨ Novos Recursos v2.0

- ✅ **+27% mais dados** (14 vs 11 campos)
- ✅ **Status automático** (Pendente/Vencido)
- ✅ **Análise por filial** (CNPJ + nome)
- ✅ **Dados contábeis** (conta + centros de custo)
- ✅ **Observações** (comentários)
- ✅ **Preparado para o futuro** (10 campos placeholder)

---

## 🎯 Próximos Passos

1. ✅ Explorar consultas SQL em `docs/EXEMPLOS_USO_NOVOS_CAMPOS.md`
2. ✅ Criar relatórios personalizados
3. ✅ Integrar com sistemas contábeis
4. ✅ Configurar alertas para faturas vencidas

---

**Versão:** 2.0.0  
**Data:** 04/11/2025  
**Status:** ✅ Pronto para Uso

