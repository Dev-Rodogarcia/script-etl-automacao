# 🔍 Evidências Específicas para Buscar nas Respostas das APIs

Este documento lista valores específicos dos CSVs manuais que você deve procurar (Ctrl+F) nas respostas JSON das APIs para validar se o endpoint está completo.

---

## 1. Lançamentos a Pagar (PRIORIDADE MÁXIMA)

**CSV:** `lancamentos-a-pagar_03-11-2025_17-55.csv` (74 linhas)

### 🎯 Evidências Críticas - TAXAS DE BANCO

**Linha 2 do CSV:**
```csv
"60960473000162";MTZ;""60746948002590"";BANCO BRADESCO S.A;29096;2024/12-12;;...;15,12;...
```

**Procurar no JSON:**
```
"BANCO BRADESCO"
"60746948002590"
"15.12"
"TARIFAS BANC"
"ANUIDADE"
```

### 🎯 Evidências Críticas - VALES

**Linha 3 do CSV:**
```csv
...;MTZ - RODOGARCIA;82603;"78959";;...;582,00;...;"CHAVE PIX CPF: 26502228836
CRISTINA APARECIDA BARBARA";VALE ALIMENTA��O...
```

**Procurar no JSON:**
```
"JOÃO DA SILVA"  
"27580355884"  (CPF do João)
"275"
"582"  
"VALE ALIMENTA"
"CRISTINA"
"26502228836"
"PIX"
```

### 🎯 Evidências - Outros Tipos

**Linha 7 (Folha de Pagamento):**
```
76332.00
"FOLHA DE PAGAMENTO"
"SALARIOS A PAGAR"
```

### ✅ Validação de Sucesso

Se encontrar **pelo menos 1 Taxa de Banco + 1 Vale**, o endpoint está correto!

---

## 2. Lançamentos a Receber

**CSV:** `lancamentos-a-receber_03-11-2025_17-53.csv` (533 linhas)

### 🎯 Evidências para Validar

**Linha 2:**
```csv
...;BIOFRAGANE;122150;120222/1;;...;35.571,35;...
```

**Procurar:**
```
"BIOFRAGANE"
"07668944000180"
"35571.35"
"120222"
"patricia@sollonet.com.br"
```

**Linha 3:**
```csv
...;TILIFORM INDUSTRIA GRAFICA LTDA;122151;120223/1;;...;8.805,37;...
```

**Procurar:**
```
"TILIFORM"
"54842406001465"
"8805.37"
"mateus.santos@tiliform.com.br"
```

### ✅ Total de Registros

Esperado: **533 registros** (ou próximo, ±10%)

---

## 3. Ocorrências

**CSV:** `historico_ocorrencia_analitico_03-11-2025_18-08.csv` (4213 linhas)

### 🎯 Evidências para Validar

**Linhas 2-5:**

**Frete 258117:**
```
"992836"  (NF)
"Mercadoria em Transferência"
"02/11/2025 12:14"
```

**Frete 258129:**
```
"993123" (NF)
"ATLAS MRO"
"Belford Roxo"
```

**Frete 259350:**
```
"730658" (NF)
"FRIO TEC REFRIGERACAO"
"Rio de Janeiro"
```

### ✅ Total de Registros

Esperado: **~4213 ocorrências** (ou próximo, ±10%)

---

## 4. Coletas

**CSV:** `coletas_analitico_03-11-2025_19-22.csv` (476 linhas)

### 🎯 Evidências para Validar

**Linha 2 (Coleta 71083):**
```
"71083"
"ARTANY INDUSTRIA DE MOVEIS LTDA"
"vinicius"
"São José dos Pinhais"
"144,20"
"Em trânsito"
"JO�O DA SILVA"
"ATB6928"
"48037"  (manifesto)
```

**Procurar:**
```
71083
"ARTANY"
"vinicius"
"São José dos Pinhais"
144.20
"Em trânsito"
```

**Linha 3 (Coleta 71081):**
```
"71081"
"FRIGELAR COMERCIO E INDUSTRIA LTDA"
"Garuva"
"7000,00"
"Manifestada"
"ISRAEL APARECIDO BERGAMASKI"
"48158"
```

### ✅ Total de Registros

Esperado: **476 coletas**

---

## 5. Fretes

**CSV:** `frete_relacao_analitico_03-11-2025_19-23.csv` (400 linhas)

### 🎯 Evidências para Validar

**Linha 2 (Frete 39600):**
```
"39600"
"IMPPAR"
"ALEXANDRE GIMENES"
"Santa Bárbara D'Oeste"
"88,64"
"38,92"
"1.303,35"
"43106"  (NF)
"13451090"  (CEP)
```

**Procurar:**
```
39600
"IMPPAR"
"ALEXANDRE GIMENES"
88.64
38.92
1303.35
```

**Linha 3 (Frete 31268):**
```
"31268"
"PPG SUM"
"GREAT WALL MOTOR BRASIL"
"Iracemápolis"
"37,50"
"1.858,81"
```

### ✅ Total de Registros

Esperado: **~400 fretes**

---

## 6. Manifestos

**Arquivo:** `relacao-de-manifestos-detalhada_2025_11_03_18_04.xlsx` (293 linhas)

### 🎯 Evidências (após converter para CSV)

**Valores para procurar:**
```
"48037"
"48158"
"48122"
(números de manifestos que aparecem nas coletas)
```

### ✅ Total de Registros

Esperado: **~293 manifestos**

---

## 7. Cotações

**Arquivo:** `relacao-de-cotacoes-detalhada_2025_11_03_17_58.xlsx` (276 linhas)

### 🎯 Evidências (após converter para CSV)

Procurar por valores relacionados às coletas/fretes já identificados.

### ✅ Total de Registros

Esperado: **~276 cotações**

---

## 8. Localizador de Cargas

**Arquivo:** `localizador-de-cargas_2025_11_03_17_55.xlsx` (120 linhas)

### 🎯 Evidências (após converter para CSV)

IDs de fretes que já conhecemos:
```
39600
31268
258117
```

### ✅ Total de Registros

Esperado: **~120 localizações**

---

## 💡 Como Usar Este Documento

### No Insomnia:

1. **Executar Request** → Receber response JSON
2. **Ctrl + F** na aba de response
3. **Procurar** por cada evidência listada acima
4. **Marcar ✅** as encontradas
5. **Se encontrar ≥80% das evidências** → Endpoint correto!

### Exemplo Prático:

**Request:** `GET /api/accounting/debit/entries`

**Response:** (grande JSON)

**Buscar:**
- `Ctrl+F` → "BANCO BRADESCO" → ✅ **Encontrado!**
- `Ctrl+F` → "60746948002590" → ✅ **Encontrado!**
- `Ctrl+F` → "JOÃO DA SILVA" → ✅ **Encontrado!**
- `Ctrl+F` → "275" → ✅ **Encontrado!**

**Conclusão:** 🎉 **Endpoint correto! Este retorna TODOS os tipos!**

---

## 📊 Checklist de Validação Rápida

Para cada entidade testada:

### Lançamentos a Pagar
- [ ] Encontrou "BANCO BRADESCO" (Taxa de Banco)
- [ ] Encontrou "JOÃO DA SILVA" ou "275" (Vale)
- [ ] Total ≥ 70 registros (perto de 74)

### Lançamentos a Receber
- [ ] Encontrou "BIOFRAGANE" ou "TILIFORM"
- [ ] Total ≥ 500 registros (perto de 533)

### Ocorrências
- [ ] Encontrou NF "992836" ou "993123"
- [ ] Total ≥ 4000 registros (perto de 4213)

### Coletas
- [ ] Encontrou coleta 71083 ou 71081
- [ ] Total ≥ 450 registros (perto de 476)

### Fretes
- [ ] Encontrou frete 39600 ou 31268
- [ ] Total ≥ 380 registros (perto de 400)

---

**Use este guia como referência rápida durante todos os testes!**

