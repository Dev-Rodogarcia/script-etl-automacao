---
context:
  - Documentacao
  - Legado
  - ArquivoHistorico
updated_at: 2026-03-18T00:00:32.1501533-03:00
source_of_truth: docs
classification: legado
related_files:
  - docs/index.md
  - docs/legado/index.md
  - docs/legado/classificacao.md
---
# ✅ Checklist de Validação - Novos Campos REST

## 🎯 Objetivo
Validar que os 14 campos disponíveis estão sendo extraídos e persistidos corretamente.

---

## 📋 Pré-requisitos

- [ ] Compilar o projeto: `05-compilar_projeto.bat`
- [ ] Verificar conexão com banco de dados
- [ ] Verificar credenciais da API no `.env`

---

## 🔍 Testes de Validação

### 1. Compilação
```bash
# Executar
05-compilar_projeto.bat

# Resultado esperado
✅ BUILD SUCCESS
✅ JAR gerado: target\extrator.jar
```

### 2. Estrutura da Tabela
```sql
-- Verificar se as novas colunas foram criadas
SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'faturas_a_pagar'
ORDER BY ORDINAL_POSITION;

-- Colunas esperadas (novas):
-- cnpj_filial, filial, observacoes, conta_contabil, centro_custo, status
-- sequencia, cheque, vencimento_original, competencia, data_baixa, 
-- data_liquidacao, banco_pagamento, conta_pagamento, descricao_despesa
```

### 3. Extração de Dados
```bash
# Executar extração completa
01-executar_extracao_completa.bat

# Verificar logs
# Procurar por: "Buscando faturas a pagar"
# Verificar se não há erros de mapeamento
```

### 4. Validação dos Novos Campos
```sql
-- Verificar se os novos campos estão sendo populados
SELECT TOP 10
    id,
    document_number,
    cnpj_filial,          -- NOVO
    filial,               -- NOVO
    receiver_cnpj,
    receiver_name,
    conta_contabil,       -- NOVO
    centro_custo,         -- NOVO
    status,               -- NOVO (calculado)
    observacoes,          -- NOVO
    due_date,
    data_extracao
FROM faturas_a_pagar
ORDER BY data_extracao DESC;
```

### 5. Validação do Status Calculado
```sql
-- Verificar se o status está sendo calculado corretamente
SELECT 
    status,
    COUNT(*) as quantidade,
    MIN(due_date) as vencimento_mais_antigo,
    MAX(due_date) as vencimento_mais_recente
FROM faturas_a_pagar
WHERE data_extracao >= CAST(GETDATE() AS DATE)
GROUP BY status;

-- Resultados esperados:
-- Vencido: due_date < GETDATE()
-- Pendente: due_date >= GETDATE()
-- Indefinido: due_date IS NULL
```

### 6. Validação de Centros de Custo
```sql
-- Verificar concatenação de múltiplos centros de custo
SELECT 
    id,
    document_number,
    centro_custo,
    LEN(centro_custo) as tamanho,
    (LEN(centro_custo) - LEN(REPLACE(centro_custo, ',', '')) + 1) as qtd_centros
FROM faturas_a_pagar
WHERE centro_custo IS NOT NULL
  AND centro_custo LIKE '%,%'
ORDER BY qtd_centros DESC;
```

### 7. Validação de Metadados JSON
```sql
-- Verificar se os metadados contêm os novos campos
SELECT TOP 5
    id,
    document_number,
    CASE 
        WHEN header_metadata LIKE '%corporation%' THEN 'OK'
        ELSE 'FALTA'
    END as tem_corporation,
    CASE 
        WHEN header_metadata LIKE '%accounting_planning_management%' THEN 'OK'
        ELSE 'FALTA'
    END as tem_conta_contabil,
    CASE 
        WHEN header_metadata LIKE '%cost_centers%' THEN 'OK'
        ELSE 'FALTA'
    END as tem_centros_custo
FROM faturas_a_pagar
ORDER BY data_extracao DESC;
```

### 8. Comparação Antes/Depois
```sql
-- Contar campos populados (antes da atualização: ~8 campos)
-- (depois da atualização: ~14 campos)
SELECT 
    COUNT(*) as total_registros,
    COUNT(cnpj_filial) as tem_cnpj_filial,
    COUNT(filial) as tem_filial,
    COUNT(conta_contabil) as tem_conta_contabil,
    COUNT(centro_custo) as tem_centro_custo,
    COUNT(status) as tem_status,
    COUNT(observacoes) as tem_observacoes,
    CAST(COUNT(cnpj_filial) * 100.0 / COUNT(*) AS DECIMAL(5,2)) as perc_cnpj_filial,
    CAST(COUNT(status) * 100.0 / COUNT(*) AS DECIMAL(5,2)) as perc_status
FROM faturas_a_pagar
WHERE data_extracao >= CAST(GETDATE() AS DATE);
```

---

## 🐛 Troubleshooting

### Problema: Novos campos estão NULL
**Causa:** API não está retornando os campos  
**Solução:** Verificar `header_metadata` para confirmar se os dados estão no JSON

```sql
SELECT TOP 1 header_metadata 
FROM faturas_a_pagar 
ORDER BY data_extracao DESC;
```

### Problema: Status sempre "Indefinido"
**Causa:** Campo `due_date` está NULL  
**Solução:** Verificar mapeamento de `due_date` no DTO

### Problema: Centro de custo vazio
**Causa:** Array `cost_centers` vazio ou NULL na API  
**Solução:** Normal, nem todas as faturas têm centro de custo

### Problema: Erro de compilação
**Causa:** JAVA_HOME não configurado  
**Solução:** 
```bash
# Verificar Java instalado
java -version

# Configurar JAVA_HOME (exemplo)
set JAVA_HOME=C:\Program Files\Java\jdk-17
```

---

## 📊 Métricas de Sucesso

- [ ] ✅ Compilação sem erros
- [ ] ✅ Tabela criada com 24+ colunas
- [ ] ✅ Extração executada sem erros
- [ ] ✅ Novos campos populados (>80% dos registros)
- [ ] ✅ Status calculado corretamente
- [ ] ✅ Centros de custo concatenados quando múltiplos
- [ ] ✅ Metadados JSON contêm novos campos
- [ ] ✅ Performance mantida (tempo de extração similar)

---

## 📝 Relatório de Validação

**Data:** ___/___/2025  
**Executado por:** _________________

### Resultados

| Item | Status | Observações |
|------|--------|-------------|
| Compilação | ⬜ OK / ⬜ ERRO | |
| Estrutura Tabela | ⬜ OK / ⬜ ERRO | |
| Extração Dados | ⬜ OK / ⬜ ERRO | |
| Campos Populados | ⬜ OK / ⬜ ERRO | |
| Status Calculado | ⬜ OK / ⬜ ERRO | |
| Centros Custo | ⬜ OK / ⬜ ERRO | |
| Metadados JSON | ⬜ OK / ⬜ ERRO | |

### Estatísticas

- Total de registros extraídos: _______
- Campos populados (média): _______/14
- Tempo de extração: _______ minutos
- Erros encontrados: _______

### Observações Adicionais

```
[Espaço para anotações]
```

---

**Status Final:** ⬜ APROVADO / ⬜ REQUER AJUSTES

