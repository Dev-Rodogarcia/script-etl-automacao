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
# ✅ Correções CSV - v2.1

## 🎯 Problemas Identificados e Corrigidos

### ❌ PROBLEMA 1: Colunas com JSON completo
**Antes:**
```csv
header_metadata;installments_metadata
"{"id":7247604,"document":"029043653475",...}";{}
```

**Solução:**
- ✅ Mantidos apenas para metadados internos (não exportar para CSV)
- ✅ CSV deve conter apenas campos de negócio

---

### ❌ PROBLEMA 2: Campos disponíveis mas não mapeados

| Campo | API REST | Status Antes | Status Agora |
|-------|----------|--------------|--------------|
| sequencia | `sequence_code` | ❌ Vazio | ✅ Mapeado |
| observacoes | `comments` ou `installments[].comments` | ❌ Vazio | ✅ Prioriza parcela |
| forma_pagamento | `installments[].payment_method` | ❌ Vazio | ✅ Traduzido |
| competencia | Calculado de `issue_date` | ❌ Vazio | ✅ Formato YYYY-MM |

---

### ❌ PROBLEMA 3: Campos futuros sem estrutura

**Campos preparados para GraphQL:**
- cheque
- vencimento_original
- data_baixa
- data_liquidacao
- banco_pagamento
- conta_pagamento
- descricao_despesa

**Status:** ✅ Estrutura preparada com placeholders

---

## 🔧 Alterações Implementadas

### 1️⃣ FaturaAPagarDTO.java

**Adicionado:**
```java
// Fonte: sequence_code
@JsonProperty("sequence_code")
private String sequenceCode;
```

### 2️⃣ FaturaAPagarMapper.java

**Adicionado:**
```java
// Mapear sequencia
entity.setSequencia(dto.getSequenceCode());

// Calcular competência (YYYY-MM)
final String competencia = String.format("%04d-%02d", 
    issueDate.getYear(), 
    issueDate.getMonthValue());
entity.setCompetencia(competencia);

// Processar parcelas
processarParcelas(entity, installmentsJson);
```

**Novos métodos:**
- `processarParcelas()` - Extrai dados das parcelas
- `traduzirFormaPagamento()` - Traduz códigos para português

**Traduções de forma de pagamento:**
- `BANK_SLIP` → "Boleto Bancário"
- `DEBIT` → "Débito em Conta"
- `CREDIT_CARD` → "Cartão de Crédito"
- `CASH` → "Dinheiro"
- `CHECK` → "Cheque"
- `PIX` → "PIX"
- `TRANSFER` → "Transferência"

### 3️⃣ FaturaAPagarEntity.java

**Adicionado:**
```java
private String formaPagamento; // Fonte: installments[].payment_method (traduzido)
```

### 4️⃣ FaturaAPagarRepository.java

**Adicionado:**
- Coluna `forma_pagamento NVARCHAR(100)` no CREATE TABLE
- Campo `forma_pagamento` no SQL MERGE
- Parâmetro no PreparedStatement

---

## 📊 Resultado Esperado

### Antes (v2.0):
```csv
id;sequencia;observacoes;forma_pagamento;competencia
7247604;;;
```

### Depois (v2.1):
```csv
id;sequencia;observacoes;forma_pagamento;competencia
7247604;82619;Pedágio;Débito em Conta;2025-11
```

---

## 🎯 Campos Mapeados (15/24)

### Disponíveis Agora (15):
1. ✅ id
2. ✅ sequence_code → sequencia
3. ✅ document → numeroDocumento
4. ✅ issue_date → dataEmissao
5. ✅ due_date → dataVencimento
6. ✅ value → valorTotal
7. ✅ type → tipoFatura
8. ✅ corporation.cnpj → cnpjFilial
9. ✅ corporation.nickname → filial
10. ✅ receiver.cnpjCpf → cnpjFornecedor
11. ✅ receiver.name → fornecedor
12. ✅ comments / installments[].comments → observacoes
13. ✅ accounting_planning_management.name → contaContabil
14. ✅ cost_centers[].name → centroCusto
15. ✅ installments[].payment_method → formaPagamento (traduzido)

### Calculados (2):
16. ✅ status (Pendente/Vencido)
17. ✅ competencia (YYYY-MM)

### Futuros (9):
- cheque
- vencimentoOriginal
- dataBaixa
- dataLiquidacao
- bancoPagamento
- contaPagamento
- descricaoDespesa
- jurosDesconto (calculado de interest_value + discount_value)
- valorPrincipal (installments[].value)

---

## 🚀 Próximos Passos

### 1. Compilar
```bash
mvn clean package
```

### 2. Executar Extração
```bash
01-executar_extracao_completa.bat
```

### 3. Validar Dados
```sql
SELECT TOP 10
    id,
    sequencia,           -- ✅ NOVO
    document_number,
    filial,
    forma_pagamento,     -- ✅ NOVO
    competencia,         -- ✅ NOVO
    observacoes,
    status,
    total_value
FROM faturas_a_pagar
ORDER BY data_extracao DESC;
```

### 4. Exportar CSV
```bash
07-exportar_csv_rapido.bat
```

### 5. Verificar CSV
- ✅ Sem colunas `header_metadata` e `installments_metadata`
- ✅ Campo `sequencia` preenchido
- ✅ Campo `forma_pagamento` traduzido
- ✅ Campo `competencia` no formato YYYY-MM
- ✅ Campo `observacoes` preenchido quando disponível

---

## 📝 Notas Importantes

### Observações
- Prioriza `installments[].comments` sobre `comments` do cabeçalho
- Se ambos vazios, campo fica NULL

### Forma de Pagamento
- Traduzido automaticamente para português
- Se código desconhecido, mantém o código original

### Competência
- Calculado automaticamente de `issue_date`
- Formato: YYYY-MM (ex: 2025-11)

### Juros/Desconto
- Atualmente incluído nas observações
- Futura implementação: campo separado

---

## ✅ Checklist de Validação

- [ ] Código compilado sem erros
- [ ] Extração executada com sucesso
- [ ] Campo `sequencia` preenchido
- [ ] Campo `forma_pagamento` traduzido
- [ ] Campo `competencia` calculado
- [ ] Campo `observacoes` preenchido
- [ ] CSV sem colunas de metadados
- [ ] Todos os campos fazem sentido

---

**Versão:** 2.1.0  
**Data:** 04/11/2025  
**Status:** ✅ Correções Implementadas

