# 📚 Exemplos de Uso - Novos Campos REST

## 🎯 Consultas SQL Úteis

### 1. Relatório Completo por Filial
```sql
SELECT 
    f.filial,
    f.cnpj_filial,
    COUNT(*) as total_faturas,
    SUM(f.total_value) as valor_total,
    COUNT(CASE WHEN f.status = 'Vencido' THEN 1 END) as qtd_vencidas,
    SUM(CASE WHEN f.status = 'Vencido' THEN f.total_value ELSE 0 END) as valor_vencido,
    COUNT(CASE WHEN f.status = 'Pendente' THEN 1 END) as qtd_pendentes,
    SUM(CASE WHEN f.status = 'Pendente' THEN f.total_value ELSE 0 END) as valor_pendente
FROM faturas_a_pagar f
WHERE f.data_extracao >= CAST(GETDATE() AS DATE)
GROUP BY f.filial, f.cnpj_filial
ORDER BY valor_total DESC;
```

### 2. Análise por Conta Contábil
```sql
SELECT 
    f.conta_contabil,
    COUNT(*) as quantidade,
    SUM(f.total_value) as valor_total,
    AVG(f.total_value) as valor_medio,
    MIN(f.due_date) as vencimento_proximo,
    MAX(f.due_date) as vencimento_distante
FROM faturas_a_pagar f
WHERE f.conta_contabil IS NOT NULL
  AND f.data_extracao >= CAST(GETDATE() AS DATE)
GROUP BY f.conta_contabil
ORDER BY valor_total DESC;
```

### 3. Faturas Vencidas por Fornecedor
```sql
SELECT 
    f.receiver_name as fornecedor,
    f.receiver_cnpj as cnpj,
    COUNT(*) as qtd_faturas_vencidas,
    SUM(f.total_value) as valor_total_vencido,
    MIN(f.due_date) as vencimento_mais_antigo,
    DATEDIFF(DAY, MIN(f.due_date), GETDATE()) as dias_atraso_maximo
FROM faturas_a_pagar f
WHERE f.status = 'Vencido'
  AND f.data_extracao >= CAST(GETDATE() AS DATE)
GROUP BY f.receiver_name, f.receiver_cnpj
ORDER BY valor_total_vencido DESC;
```

### 4. Distribuição por Centro de Custo
```sql
-- Para centros de custo únicos
SELECT 
    f.centro_custo,
    COUNT(*) as quantidade,
    SUM(f.total_value) as valor_total,
    AVG(f.total_value) as valor_medio
FROM faturas_a_pagar f
WHERE f.centro_custo IS NOT NULL
  AND f.centro_custo NOT LIKE '%,%'  -- Sem múltiplos centros
  AND f.data_extracao >= CAST(GETDATE() AS DATE)
GROUP BY f.centro_custo
ORDER BY valor_total DESC;

-- Para faturas com múltiplos centros de custo
SELECT 
    f.id,
    f.document_number,
    f.centro_custo,
    f.total_value,
    (LEN(f.centro_custo) - LEN(REPLACE(f.centro_custo, ',', '')) + 1) as qtd_centros
FROM faturas_a_pagar f
WHERE f.centro_custo LIKE '%,%'
  AND f.data_extracao >= CAST(GETDATE() AS DATE)
ORDER BY qtd_centros DESC;
```

### 5. Dashboard Executivo
```sql
SELECT 
    -- Totalizadores
    COUNT(*) as total_faturas,
    SUM(f.total_value) as valor_total,
    
    -- Por Status
    COUNT(CASE WHEN f.status = 'Vencido' THEN 1 END) as vencidas,
    SUM(CASE WHEN f.status = 'Vencido' THEN f.total_value ELSE 0 END) as valor_vencido,
    COUNT(CASE WHEN f.status = 'Pendente' THEN 1 END) as pendentes,
    SUM(CASE WHEN f.status = 'Pendente' THEN f.total_value ELSE 0 END) as valor_pendente,
    
    -- Médias
    AVG(f.total_value) as valor_medio,
    AVG(DATEDIFF(DAY, f.issue_date, f.due_date)) as prazo_medio_dias,
    
    -- Próximos Vencimentos
    COUNT(CASE WHEN f.due_date BETWEEN GETDATE() AND DATEADD(DAY, 7, GETDATE()) THEN 1 END) as vence_7_dias,
    SUM(CASE WHEN f.due_date BETWEEN GETDATE() AND DATEADD(DAY, 7, GETDATE()) THEN f.total_value ELSE 0 END) as valor_vence_7_dias,
    
    -- Filiais
    COUNT(DISTINCT f.filial) as qtd_filiais,
    COUNT(DISTINCT f.receiver_cnpj) as qtd_fornecedores
FROM faturas_a_pagar f
WHERE f.data_extracao >= CAST(GETDATE() AS DATE);
```

### 6. Faturas com Observações
```sql
SELECT 
    f.id,
    f.document_number,
    f.receiver_name,
    f.total_value,
    f.due_date,
    f.status,
    f.observacoes
FROM faturas_a_pagar f
WHERE f.observacoes IS NOT NULL
  AND LEN(f.observacoes) > 0
  AND f.data_extracao >= CAST(GETDATE() AS DATE)
ORDER BY f.due_date;
```

### 7. Análise de Atraso por Filial
```sql
SELECT 
    f.filial,
    f.cnpj_filial,
    COUNT(CASE WHEN f.status = 'Vencido' THEN 1 END) as qtd_vencidas,
    SUM(CASE WHEN f.status = 'Vencido' THEN f.total_value ELSE 0 END) as valor_vencido,
    AVG(CASE 
        WHEN f.status = 'Vencido' 
        THEN DATEDIFF(DAY, f.due_date, GETDATE()) 
    END) as media_dias_atraso,
    MAX(CASE 
        WHEN f.status = 'Vencido' 
        THEN DATEDIFF(DAY, f.due_date, GETDATE()) 
    END) as maior_atraso_dias
FROM faturas_a_pagar f
WHERE f.data_extracao >= CAST(GETDATE() AS DATE)
GROUP BY f.filial, f.cnpj_filial
HAVING COUNT(CASE WHEN f.status = 'Vencido' THEN 1 END) > 0
ORDER BY valor_vencido DESC;
```

### 8. Fluxo de Caixa Projetado
```sql
SELECT 
    CAST(f.due_date AS DATE) as data_vencimento,
    COUNT(*) as qtd_faturas,
    SUM(f.total_value) as valor_dia,
    SUM(SUM(f.total_value)) OVER (ORDER BY CAST(f.due_date AS DATE)) as valor_acumulado,
    STRING_AGG(f.receiver_name, ', ') as fornecedores
FROM faturas_a_pagar f
WHERE f.status = 'Pendente'
  AND f.due_date >= GETDATE()
  AND f.due_date <= DATEADD(DAY, 30, GETDATE())
  AND f.data_extracao >= CAST(GETDATE() AS DATE)
GROUP BY CAST(f.due_date AS DATE)
ORDER BY data_vencimento;
```

---

## 🔍 Consultas de Auditoria

### 9. Verificar Completude dos Dados
```sql
SELECT 
    'Total Registros' as metrica,
    COUNT(*) as valor
FROM faturas_a_pagar
WHERE data_extracao >= CAST(GETDATE() AS DATE)

UNION ALL

SELECT 
    'Com CNPJ Filial',
    COUNT(*)
FROM faturas_a_pagar
WHERE cnpj_filial IS NOT NULL
  AND data_extracao >= CAST(GETDATE() AS DATE)

UNION ALL

SELECT 
    'Com Nome Filial',
    COUNT(*)
FROM faturas_a_pagar
WHERE filial IS NOT NULL
  AND data_extracao >= CAST(GETDATE() AS DATE)

UNION ALL

SELECT 
    'Com Conta Contábil',
    COUNT(*)
FROM faturas_a_pagar
WHERE conta_contabil IS NOT NULL
  AND data_extracao >= CAST(GETDATE() AS DATE)

UNION ALL

SELECT 
    'Com Centro de Custo',
    COUNT(*)
FROM faturas_a_pagar
WHERE centro_custo IS NOT NULL
  AND data_extracao >= CAST(GETDATE() AS DATE)

UNION ALL

SELECT 
    'Com Status Calculado',
    COUNT(*)
FROM faturas_a_pagar
WHERE status IS NOT NULL
  AND data_extracao >= CAST(GETDATE() AS DATE)

UNION ALL

SELECT 
    'Com Observações',
    COUNT(*)
FROM faturas_a_pagar
WHERE observacoes IS NOT NULL
  AND LEN(observacoes) > 0
  AND data_extracao >= CAST(GETDATE() AS DATE);
```

### 10. Comparar Dados Novos vs Antigos
```sql
-- Registros extraídos hoje (com novos campos)
SELECT 
    'Hoje' as periodo,
    COUNT(*) as total,
    COUNT(cnpj_filial) as com_cnpj_filial,
    COUNT(conta_contabil) as com_conta_contabil,
    COUNT(status) as com_status
FROM faturas_a_pagar
WHERE data_extracao >= CAST(GETDATE() AS DATE)

UNION ALL

-- Registros antigos (antes da atualização)
SELECT 
    'Anterior',
    COUNT(*),
    COUNT(cnpj_filial),
    COUNT(conta_contabil),
    COUNT(status)
FROM faturas_a_pagar
WHERE data_extracao < CAST(GETDATE() AS DATE)
  AND data_extracao >= DATEADD(DAY, -7, GETDATE());
```

---

## 📊 Exportação para Excel/CSV

### 11. Relatório Completo para Exportação
```sql
SELECT 
    -- Identificação
    f.id,
    f.document_number as numero_documento,
    
    -- Datas
    FORMAT(f.issue_date, 'dd/MM/yyyy') as data_emissao,
    FORMAT(f.due_date, 'dd/MM/yyyy') as data_vencimento,
    
    -- Valores
    f.total_value as valor_total,
    
    -- Filial
    f.cnpj_filial,
    f.filial,
    
    -- Fornecedor
    f.receiver_cnpj as cnpj_fornecedor,
    f.receiver_name as fornecedor,
    
    -- Contabilidade
    f.conta_contabil,
    f.centro_custo,
    
    -- Status e Controle
    f.status,
    CASE 
        WHEN f.status = 'Vencido' 
        THEN DATEDIFF(DAY, f.due_date, GETDATE())
        ELSE 0
    END as dias_atraso,
    
    -- Observações
    f.observacoes,
    
    -- Tipo
    f.invoice_type as tipo_fatura,
    
    -- Auditoria
    FORMAT(f.data_extracao, 'dd/MM/yyyy HH:mm:ss') as data_extracao
FROM faturas_a_pagar f
WHERE f.data_extracao >= CAST(GETDATE() AS DATE)
ORDER BY f.due_date, f.total_value DESC;
```

---

## 🎨 Exemplos de Uso em Java

### Acessar Novos Campos no Código

```java
// No Mapper ou Service
FaturaAPagarEntity fatura = repository.buscarPorId(123L);

// Novos campos disponíveis
String cnpjFilial = fatura.getCnpjFilial();
String nomeFilial = fatura.getFilial();
String contaContabil = fatura.getContaContabil();
String centroCusto = fatura.getCentroCusto();
String status = fatura.getStatus(); // "Pendente", "Vencido" ou "Indefinido"
String observacoes = fatura.getObservacoes();

// Verificar status
if ("Vencido".equals(fatura.getStatus())) {
    logger.warn("Fatura {} está vencida!", fatura.getDocumentNumber());
}

// Processar múltiplos centros de custo
if (fatura.getCentroCusto() != null && fatura.getCentroCusto().contains(",")) {
    String[] centros = fatura.getCentroCusto().split(",\\s*");
    for (String centro : centros) {
        logger.info("Centro de custo: {}", centro);
    }
}
```

### Filtrar por Status

```java
// Buscar apenas faturas vencidas
List<FaturaAPagarEntity> vencidas = repository.buscarPorStatus("Vencido");

// Buscar faturas pendentes de uma filial específica
List<FaturaAPagarEntity> pendentesFilial = repository
    .buscarPorStatusEFilial("Pendente", "FILIAL SP");
```

---

## 📈 Indicadores de Performance (KPIs)

### 12. KPIs Financeiros
```sql
DECLARE @hoje DATE = CAST(GETDATE() AS DATE);

SELECT 
    -- Indicadores Gerais
    COUNT(*) as total_faturas,
    SUM(total_value) as valor_total,
    
    -- Taxa de Inadimplência
    CAST(COUNT(CASE WHEN status = 'Vencido' THEN 1 END) * 100.0 / COUNT(*) AS DECIMAL(5,2)) as taxa_inadimplencia_perc,
    
    -- Valor Médio
    AVG(total_value) as ticket_medio,
    
    -- Prazo Médio
    AVG(DATEDIFF(DAY, issue_date, due_date)) as prazo_medio_pagamento,
    
    -- Concentração
    COUNT(DISTINCT filial) as qtd_filiais_ativas,
    COUNT(DISTINCT receiver_cnpj) as qtd_fornecedores_ativos,
    
    -- Próximos 30 dias
    SUM(CASE WHEN due_date BETWEEN @hoje AND DATEADD(DAY, 30, @hoje) THEN total_value ELSE 0 END) as valor_vence_30_dias
FROM faturas_a_pagar
WHERE data_extracao >= @hoje;
```

---

**💡 Dica:** Salve suas consultas favoritas em arquivos `.sql` na pasta `docs/queries/` para reutilização!

