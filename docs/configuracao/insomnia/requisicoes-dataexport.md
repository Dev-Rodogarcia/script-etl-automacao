# Requisições API Data Export - Insomnia

## 🎯 Objetivo

Testar a API Data Export para Manifestos, Cotações e Localizador de Cargas, validando se todos os campos estão sendo extraídos.

---

## 🔧 Informações da API Data Export

**Endpoint Base:** `{{base_url}}/api/analytics/reports/{template_id}/data`

**Método:** GET (com corpo JSON)

**Autenticação:** Bearer Token (token_dataexport)

---

## 📋 Templates Configurados

| Entidade | Template ID | Tabela | Campo de Data |
|----------|-------------|--------|---------------|
| Manifestos | 6399 | manifests | service_date |
| Cotações | 6906 | quotes | requested_at |
| Localizador | 8656 | freights | service_at |

---

## 🚀 Requisições para Criar

### Pasta: API Data Export > Manifestos

#### 1️⃣ Buscar Manifestos (JSON)

**Nome:** `[JSON] GET Manifestos`

**Método:** GET

**URL:**
```
{{base_url}}/api/analytics/reports/6399/data
```

**Headers:**
```
Authorization: Bearer {{token_dataexport}}
Content-Type: application/json
Accept: application/json
```

**Body (Raw - JSON):**
```json
{
  "search": {
    "manifests": {
      "service_date": "{{data_inicio}} - {{data_fim}}"
    }
  },
  "page": "1",
  "per": "100"
}
```

**Validação:**
- Status: 200 OK
- Formato: JSON com array de manifestos
- Total esperado: **~293 manifestos** (do XLSX)
- Verificar se campo `data` tem registros

---

#### 2️⃣ Buscar Manifestos (CSV) - Para Comparação

**Nome:** `[CSV] GET Manifestos`

**Método:** GET

**URL:**
```
{{base_url}}/api/analytics/reports/6399/data
```

**Headers:**
```
Authorization: Bearer {{token_dataexport}}
Content-Type: application/json
Accept: text/csv
```

**Body:** (mesmo do anterior)
```json
{
  "search": {
    "manifests": {
      "service_date": "{{data_inicio}} - {{data_fim}}"
    }
  },
  "page": "1",
  "per": "10000"
}
```

**Validação:**
- Deve retornar CSV completo
- Salvar resposta em arquivo temporário
- Comparar colunas com XLSX original

---

#### 3️⃣ Paginação - Página 2

**Nome:** `[JSON] Manifestos - Página 2`

**Body:**
```json
{
  "search": {
    "manifests": {
      "service_date": "{{data_inicio}} - {{data_fim}}"
    }
  },
  "page": "2",
  "per": "100"
}
```

**Objetivo:** Testar se paginação está funcionando

---

### Pasta: API Data Export > Cotações

#### 1️⃣ Buscar Cotações (JSON)

**Nome:** `[JSON] GET Cotações`

**URL:**
```
{{base_url}}/api/analytics/reports/6906/data
```

**Headers:**
```
Authorization: Bearer {{token_dataexport}}
Content-Type: application/json
Accept: application/json
```

**Body:**
```json
{
  "search": {
    "quotes": {
      "requested_at": "{{data_inicio}} - {{data_fim}}"
    }
  },
  "page": "1",
  "per": "100"
}
```

**Validação:**
- Total esperado: **~276 cotações** (do XLSX)

---

#### 2️⃣ Buscar Cotações (CSV Completo)

**Nome:** `[CSV] GET Cotações Completo`

**Headers:**
```
Authorization: Bearer {{token_dataexport}}
Content-Type: application/json
Accept: text/csv
```

**Body:**
```json
{
  "search": {
    "quotes": {
      "requested_at": "{{data_inicio}} - {{data_fim}}"
    }
  },
  "page": "1",
  "per": "10000"
}
```

---

### Pasta: API Data Export > Localizador de Cargas

#### 1️⃣ Buscar Localizações (JSON)

**Nome:** `[JSON] GET Localizador de Cargas`

**URL:**
```
{{base_url}}/api/analytics/reports/8656/data
```

**Headers:**
```
Authorization: Bearer {{token_dataexport}}
Content-Type: application/json
Accept: application/json
```

**Body:**
```json
{
  "search": {
    "freights": {
      "service_at": "{{data_inicio}} - {{data_fim}}"
    }
  },
  "page": "1",
  "per": "100"
}
```

**Validação:**
- Total esperado: **~120 localizações** (do XLSX)

---

#### 2️⃣ Buscar Localizações (CSV Completo)

**Nome:** `[CSV] GET Localizador Completo`

**Headers:**
```
Authorization: Bearer {{token_dataexport}}
Content-Type: application/json
Accept: text/csv
```

**Body:**
```json
{
  "search": {
    "freights": {
      "service_at": "{{data_inicio}} - {{data_fim}}"
    }
  },
  "page": "1",
  "per": "10000"
}
```

---

## 🔍 Testar Outros Filtros Possíveis

### Pasta: API Data Export > Testes Avançados

#### 1️⃣ Manifestos - Filtro por Status

**Nome:** `[TESTE] Manifestos com Filtro Status`

**Body:**
```json
{
  "search": {
    "manifests": {
      "service_date": "{{data_inicio}} - {{data_fim}}",
      "status": "closed"
    }
  },
  "page": "1",
  "per": "100"
}
```

---

#### 2️⃣ Consultar Metadados do Template

**Nome:** `[METADATA] Informações Template Manifestos`

**Método:** GET

**URL:**
```
{{base_url}}/api/analytics/reports/6399
```

**Headers:**
```
Authorization: Bearer {{token_dataexport}}
Accept: application/json
```

**Objetivo:** Descobrir todos os campos disponíveis e filtros possíveis

---

## 📊 Processo de Validação

### Para Cada Requisição:

1. **Executar Request** no Insomnia

2. **Verificar Status Code:**
   - ✅ 200 OK: Sucesso
   - ❌ 401: Token inválido/expirado
   - ❌ 404: Template ID incorreto
   - ❌ 400: Formato do body incorreto

3. **Analisar Estrutura da Resposta JSON:**
   ```json
   {
     "data": [
       {
         "campo1": "valor1",
         "campo2": "valor2",
         ...
       }
     ],
     "meta": {
       "total_count": 293,
       "page": 1,
       "per": 100
     }
   }
   ```

4. **Extrair Lista de Campos:**
   - Copiar um objeto do array `data`
   - Listar todos os campos (keys) presentes
   - Salvar em `docs/descobertas/campos-{entidade}.md`

5. **Comparar com XLSX Original:**
   - Converter XLSX para CSV (se ainda não fez)
   - Comparar colunas CSV vs campos JSON
   - Identificar campos faltantes

---

## 📝 Template de Documentação

Para cada entidade testada, criar: `docs/descobertas/dataexport-{entidade}.md`

```markdown
# Data Export - {Entidade}

## Informações do Template

- **Template ID:** {id}
- **Tabela:** {nome_tabela}
- **Campo de Data:** {campo_data}

## Request Testada

### URL
`GET {{base_url}}/api/analytics/reports/{id}/data`

### Body
```json
{...}
```

### Status Code
200 OK

## Campos Retornados (JSON)

Total de campos: XX

Lista completa:
1. campo1 (tipo: string)
2. campo2 (tipo: number)
...

## Comparação com XLSX

| Campo XLSX | Campo JSON | Status | Observações |
|------------|------------|--------|-------------|
| Coluna A | campo1 | ✅ | Mapeado |
| Coluna B | campo2 | ✅ | Mapeado |
| Coluna C | - | ❌ | NÃO ENCONTRADO |

## Total de Registros

- **XLSX Original:** X linhas
- **API JSON:** Y registros
- **Match:** Z%

## Observações

...
```

---

## 🔄 Converter XLSX para CSV (Para Comparação)

**Método Manual:**
1. Abrir arquivo XLSX no Excel
2. Salvar Como → CSV (delimitado por vírgula)
3. Salvar em `docs/arquivos-csv-convertidos/`

**Nomes dos arquivos convertidos:**
- `manifestos-convertido.csv`
- `cotacoes-convertido.csv`
- `localizador-cargas-convertido.csv`

---

## ✅ Checklist de Validação

- [ ] Manifestos: Request JSON criada e testada
- [ ] Manifestos: Request CSV criada e testada
- [ ] Manifestos: XLSX convertido para CSV
- [ ] Manifestos: Mapeamento de campos completo
- [ ] Cotações: Request JSON criada e testada
- [ ] Cotações: Request CSV criada e testada
- [ ] Cotações: XLSX convertido para CSV
- [ ] Cotações: Mapeamento de campos completo
- [ ] Localizador: Request JSON criada e testada
- [ ] Localizador: Request CSV criada e testada
- [ ] Localizador: XLSX convertido para CSV
- [ ] Localizador: Mapeamento de campos completo

---

**Próximo Passo:** Consolidar todas as descobertas e atualizar código Java

