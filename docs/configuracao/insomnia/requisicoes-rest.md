# Requisições API REST - Insomnia

## 🎯 Objetivo

Testar e descobrir os endpoints corretos da API REST que retornam **TODOS** os dados dos CSVs manuais.

---

## 🚨 PRIORIDADE MÁXIMA: Lançamentos a Pagar

### Problema Identificado

O endpoint atual `/api/accounting/debit/billings` retorna apenas:
- ✅ **CiotBilling** (Faturas CIOT)
- ✅ **DriverBilling** (Faturas de motorista)

Mas o CSV manual `lancamentos-a-pagar_03-11-2025_17-55.csv` contém:
- ❌ **Taxas de Banco** (ex: "BANCO BRADESCO S.A")
- ❌ **Vales** (ex: "MTZ - RODOGARCIA")
- ✅ Faturas de Frete

**Precisamos encontrar o endpoint "genérico" que retorna TODOS os tipos!**

---

## 📋 Requisições para Criar no Insomnia

### Pasta: API REST > Lançamentos a Pagar

#### 1️⃣ Endpoint Atual (Confirmar Incompletude)

**Nome da Request:** `[ATUAL] GET billings (INCOMPLETO)`

**Método:** GET

**URL:**
```
{{base_url}}/api/accounting/debit/billings?since={{timestamp_inicio}}&per=100
```

**Headers:**
```
Authorization: Bearer {{token_rest}}
Accept: application/json
```

**Validação Esperada:**
- ✅ Deve retornar algumas faturas
- ❌ NÃO deve conter "BANCO BRADESCO S.A"
- ❌ NÃO deve conter vales de funcionários

---

#### 2️⃣ Testar: Endpoint "entries"

**Nome:** `[TESTE 1] GET debit/entries`

**Método:** GET

**URL:**
```
{{base_url}}/api/accounting/debit/entries?since={{timestamp_inicio}}&per=100
```

**Headers:**
```
Authorization: Bearer {{token_rest}}
Accept: application/json
```

**O que procurar na resposta:**
- 🔍 Buscar por "BANCO BRADESCO" ou "60746948002590" (CNPJ do banco)
- 🔍 Buscar por "JOÃO DA SILVA" ou valores como "275,00", "582,00"

---

#### 3️⃣ Testar: Endpoint "transactions"

**Nome:** `[TESTE 2] GET debit/transactions`

**URL:**
```
{{base_url}}/api/accounting/debit/transactions?since={{timestamp_inicio}}&per=100
```

---

#### 4️⃣ Testar: Endpoint "all"

**Nome:** `[TESTE 3] GET debit/all`

**URL:**
```
{{base_url}}/api/accounting/debit/all?since={{timestamp_inicio}}&per=100
```

---

#### 5️⃣ Testar: Endpoint "payables"

**Nome:** `[TESTE 4] GET accounting/payables`

**URL:**
```
{{base_url}}/api/accounting/payables?since={{timestamp_inicio}}&per=100
```

---

#### 6️⃣ Testar: Endpoint raiz "payables"

**Nome:** `[TESTE 5] GET payables (raiz)`

**URL:**
```
{{base_url}}/api/payables?since={{timestamp_inicio}}&per=100
```

---

#### 7️⃣ Testar: Com parâmetro "type"

**Nome:** `[TESTE 6] GET billings com type=all`

**URL:**
```
{{base_url}}/api/accounting/debit/billings?since={{timestamp_inicio}}&per=100&type=all
```

---

#### 8️⃣ Testar: Endpoint "installments" (parcelas)

**Nome:** `[TESTE 7] GET debit/installments`

**URL:**
```
{{base_url}}/api/accounting/debit/installments?since={{timestamp_inicio}}&per=100
```

---

### 📊 Como Validar os Resultados

Para cada requisição testada:

1. **Executar a Request** (botão "Send")

2. **Analisar a Resposta:**
   - Status Code: deve ser `200 OK`
   - Verificar se JSON tem estrutura `{"data": [...], "paging": {...}}`

3. **Procurar Evidências no JSON:**

   **Evidência de Taxas de Banco:**
   - Buscar (Ctrl+F na resposta): `"BANCO BRADESCO"`
   - Buscar: `"60746948002590"` (CNPJ)
   - Buscar: `"TARIFAS BANC"`

   **Evidência de Vales:**
   - Buscar: `"JOÃO DA SILVA"`
   - Buscar: `"VALE ALIMENTA"`
   - Buscar: valores como `"275.00"` ou `"582.00"`

4. **Contar Registros:**
   - Verificar campo `"total_count"` ou `"paging.total"`
   - CSV tem **74 linhas**
   - Endpoint correto deve ter quantidade similar ou maior

5. **Marcar Resultado:**
   - ✅ **SUCESSO:** Se encontrou Taxas de Banco + Vales + Faturas
   - ❌ **FALHOU:** Se retornou 404, 401, ou não tem os dados esperados
   - ⚠️ **PARCIAL:** Se tem alguns tipos mas não todos

---

## 📝 Registrar Descobertas

Criar arquivo: `docs/descobertas/lancamentos-a-pagar.md`

Template:
```markdown
# Descobertas - Lançamentos a Pagar

## Data do Teste: DD/MM/YYYY

### Endpoint Testado
`GET {{base_url}}/api/...`

### Status Code
200 / 404 / 401 / etc.

### Resultado
✅ SUCESSO / ❌ FALHOU / ⚠️ PARCIAL

### Evidências Encontradas
- [ ] Taxas de Banco (BANCO BRADESCO)
- [ ] Vales de funcionários (JOÃO DA SILVA)
- [ ] Faturas de Frete

### Total de Registros
- API: X registros
- CSV: 74 registros
- Match: XX%

### Campos Retornados
Listar os principais campos do JSON

### Observações
Qualquer observação relevante
```

---

## 🔄 Passos Seguintes

Após encontrar o endpoint correto de Lançamentos a Pagar:

1. **Documentar completamente** em `docs/endpoints/lancamentos-a-pagar.md`
2. **Prosseguir para:** Lançamentos a Receber
3. **Depois:** Ocorrências
4. **Por último:** Atualizar código Java com endpoint correto

---

## 🆘 Se Nenhum Endpoint Funcionar

Estratégias alternativas:

1. **Consultar Documentação Oficial:**
   - https://documenter.getpostman.com/view/20571375/2s9YXk2fj5
   - Procurar por "accounting", "debit", "payable"

2. **Inspecionar Network da Plataforma Web:**
   - Abrir DevTools (F12) no navegador
   - Acessar plataforma ESL e baixar o relatório manual
   - Aba "Network" → filtrar por "XHR" ou "Fetch"
   - Copiar exatamente o endpoint que a plataforma usa

3. **Contactar Suporte ESL:**
   - Usar o documento `docs/relatorios-diarios/pedido-endpoints.md`
   - Solicitar endpoint genérico de lançamentos a pagar

---

**Próximo Documento:** `03-requisicoes-api-graphql.md`

