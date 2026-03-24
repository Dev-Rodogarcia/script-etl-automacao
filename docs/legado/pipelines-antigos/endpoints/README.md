---
context:
  - Documentacao
  - Legado
  - ArquivoHistorico
updated_at: 2026-03-18T00:00:32.1501533-03:00
source_of_truth: docs
classification: perigoso
related_files:
  - docs/index.md
  - docs/legado/index.md
  - docs/legado/classificacao.md
---
# 📡 Documentação de Endpoints - ESL Cloud APIs

Este diretório contém a documentação completa e validada de todos os endpoints descobertos nos testes.

---

## 🎯 Objetivo

Documentar cada endpoint de forma detalhada para:
1. Facilitar manutenção futura
2. Servir de referência para atualizar código Java
3. Permitir que outros desenvolvedores entendam as APIs
4. Registrar parâmetros, filtros e respostas

---

## 📂 Estrutura de Arquivos

### API REST
- `rest-lancamentos-a-pagar.md` - **PRIORIDADE**
- `rest-lancamentos-a-receber.md`
- `rest-ocorrencias.md`

### API GraphQL
- `graphql-coletas.md`
- `graphql-fretes.md`
- `graphql-introspection.md` - Schema completo

### API Data Export
- `dataexport-manifestos.md`
- `dataexport-cotacoes.md`
- `dataexport-localizador.md`

---

## 📋 Template de Documentação

Cada endpoint deve ser documentado usando este template:

```markdown
# Endpoint: [Nome da Entidade]

## 📌 Informações Básicas

- **URL:** `[MÉTODO] {{base_url}}/api/...`
- **Autenticação:** Bearer Token
- **Token Variable:** `{{token_xxx}}`
- **Tipo de API:** REST / GraphQL / Data Export
- **Status:** ✅ Validado / ⚠️ Parcial / ❌ Não Funciona

---

## 🔐 Autenticação

### Headers Obrigatórios

```
Authorization: Bearer {{token_xxx}}
Content-Type: application/json
Accept: application/json
```

---

## 📥 Request

### Método HTTP
`GET` / `POST`

### URL Completa
```
{{base_url}}/api/accounting/debit/entries?since={{timestamp_inicio}}&per=100
```

### Parâmetros de Query

| Parâmetro | Tipo | Obrigatório | Descrição | Exemplo |
|-----------|------|-------------|-----------|---------|
| since | DateTime ISO 8601 | Sim | Data/hora de início | 2025-11-03T00:00:00-03:00 |
| per | Integer | Não | Registros por página | 100 (padrão: 25, max: 100) |
| start | String | Não | ID para paginação | "12345" |
| type | String | Não | Filtro de tipo | "all", "billing", "tax" |

### Body (se POST)

```json
{
  "campo1": "valor1",
  "campo2": 123
}
```

---

## 📤 Response

### Status Codes

- **200 OK** - Sucesso
- **401 Unauthorized** - Token inválido/expirado
- **403 Forbidden** - Sem permissões
- **404 Not Found** - Endpoint não existe
- **406 Not Acceptable** - Formato de data incorreto
- **500 Internal Server Error** - Erro no servidor

### Estrutura da Resposta (200 OK)

```json
{
  "data": [
    {
      "id": 123,
      "campo1": "valor1",
      "campo2": 456.78,
      "campo3": {
        "subcampo": "valor"
      }
    }
  ],
  "paging": {
    "next_id": "456",
    "total": 1000,
    "per_page": 100,
    "current_page": 1
  }
}
```

### Campos Retornados

Lista completa de campos (nível raiz do objeto `data`):

| Campo | Tipo | Obrigatório | Descrição | Exemplo |
|-------|------|-------------|-----------|---------|
| id | Integer | Sim | ID único do registro | 123 |
| created_at | DateTime | Sim | Data de criação | "2025-11-03T10:30:00-03:00" |
| ... | ... | ... | ... | ... |

---

## 🔄 Paginação

### Como Funciona

1. Primeira página: usar `?since=...&per=100`
2. Próximas páginas: usar `next_id` da resposta anterior
3. URL: `?start={{next_id}}&per=100`
4. Continuar até `paging.next_id` ser `null`

### Exemplo de Loop

```
1ª Request: /api/...?since=2025-11-03&per=100
Response: { "paging": { "next_id": "456" } }

2ª Request: /api/...?start=456&per=100
Response: { "paging": { "next_id": "789" } }

3ª Request: /api/...?start=789&per=100
Response: { "paging": { "next_id": null } } ← FIM
```

---

## 📊 Validação com CSV

### CSV de Referência
- **Arquivo:** `lancamentos-a-pagar_03-11-2025_17-55.csv`
- **Total de Linhas:** 74
- **Total de Colunas:** 24

### Comparação de Volumes

| Métrica | CSV | API | Match |
|---------|-----|-----|-------|
| Total de Registros | 74 | 72 | 97% ✅ |
| Período | 03/11/2025 | 02-03/11/2025 | ✅ |

**Observações:** Diferença de 2 registros é aceitável (dados podem ter sido atualizados).

### Evidências Validadas

Valores específicos do CSV encontrados na resposta da API:

- [x] "BANCO BRADESCO S.A" - encontrado em `data[5].supplier.name`
- [x] CNPJ "60746948002590" - encontrado em `data[5].supplier.document`
- [x] Valor 275.00 - encontrado em `data[2].principal_value`
- [x] "JOÃO DA SILVA" - encontrado em `data[2].supplier.name`

### Campos Mapeados

Ver mapeamento completo em: `../mapeamento/[entidade]-csv-api.md`

**Resumo:**
- ✅ Mapeados: 22/24 campos (92%)
- ❌ Não encontrados: 2 campos
- ⚠️ Derivados: 0 campos

---

## 💡 Observações Importantes

### Rate Limiting
- API tem limite de **2 segundos** entre requisições
- Respeitar para evitar bloqueios
- Código Java já implementa delay de 2.2s

### Filtros de Data
- Aceita ISO 8601 com timezone: `2025-11-03T00:00:00-03:00`
- Não aceita apenas data: `2025-11-03` ❌
- Timezone obrigatório (Brasil: -03:00 ou -02:00)

### Tipos Especiais
- Dinheiro: sempre em centavos (1500 = R$ 15,00)
- Datas: sempre em UTC com offset
- IDs: sempre strings, mesmo números

---

## 🔍 Troubleshooting

### Problema: Response vazio `"data": []`

**Possíveis causas:**
1. Filtro de data muito restrito → Ampliar intervalo
2. Campo de data incorreto → Verificar documentação
3. Sem dados no período → Testar outro período

### Problema: Erro 406 Not Acceptable

**Causa:** Formato de data incorreto

**Solução:** 
```
❌ Errado: since=2025-11-03
✅ Correto: since=2025-11-03T00:00:00-03:00
```

### Problema: Campos faltando

**Causa:** Campos aninhados em objetos relacionados

**Solução:** Expandir objetos relacionados na documentação:
```json
{
  "id": 123,
  "supplier": {  ← Objeto aninhado
    "name": "Fornecedor XYZ",
    "document": "12345678000199"
  }
}
```

---

## ✅ Checklist de Validação

Antes de considerar um endpoint como "completo":

- [ ] Request executada com sucesso (200 OK)
- [ ] Paginação testada e funcionando
- [ ] Total de registros compatível com CSV (±10%)
- [ ] Evidências específicas encontradas (nomes, CNPJs, valores)
- [ ] Todos os campos listados e tipados
- [ ] Mapeamento CSV ↔ API criado
- [ ] Exemplos de request/response documentados
- [ ] Problemas conhecidos documentados

---

## 🔄 Atualização do Código Java

Com base nesta documentação:

1. Atualizar URL do endpoint em `ClienteApiRest.java`
2. Atualizar/criar DTOs com todos os campos
3. Ajustar parâmetros de filtro se necessário
4. Validar paginação
5. Testar extração completa

---

**Template criado em:** `template-endpoint.md`
**Última atualização:** -
**Status geral:** 🔴 Aguardando testes
```

---

Preencha este template para cada endpoint validado!

