# 📁 Diretório de Descobertas

Este diretório contém os resultados dos testes realizados no Insomnia para mapear completamente as APIs da ESL Cloud.

---

## 📂 Estrutura de Arquivos

### Descobertas da API REST

- `lancamentos-a-pagar.md` - **PRIORIDADE MÁXIMA**
- `lancamentos-a-receber.md`
- `ocorrencias.md`

### Descobertas da API GraphQL

- `campos-coletas.md` - Resultado da introspection de Pick
- `campos-fretes.md` - Resultado da introspection de Freight
- `introspection-completa.json` - Schema completo exportado

### Descobertas da API Data Export

- `dataexport-manifestos.md`
- `dataexport-cotacoes.md`
- `dataexport-localizador.md`

---

## 📋 Template para Documentar Descobertas

Ao testar um endpoint no Insomnia, copiar e preencher este template:

```markdown
# Descoberta: [Nome da Entidade]

## 🗓️ Data do Teste
DD/MM/YYYY HH:MM

## 🔗 Endpoint Testado
`[MÉTODO] {{base_url}}/api/...`

## 📥 Request

### Headers
```
Authorization: Bearer {{token_xxx}}
Content-Type: application/json
```

### Parâmetros / Body
```json
{...}
```

## 📤 Response

### Status Code
[200 / 404 / 401 / etc.]

### Tempo de Resposta
XX ms

### Estrutura da Resposta
```json
{
  "data": [...],
  "paging": {...}
}
```

## 📊 Análise dos Dados

### Total de Registros
- **CSV Manual:** X registros
- **API:** Y registros
- **Match:** Z%

### Campos Retornados

Lista completa de campos (copiar de um objeto do array `data`):

1. campo1: string
2. campo2: number
3. campo3: boolean
...

### Comparação com CSV

| Campo CSV | Campo API | Status | Observações |
|-----------|-----------|--------|-------------|
| CNPJ Filial | branch_document | ✅ | Mapeado |
| Fornecedor | supplier_name | ✅ | Mapeado |
| Coluna X | ? | ❌ | NÃO ENCONTRADO |

## ✅ Validações Realizadas

- [ ] Status 200 OK recebido
- [ ] JSON bem formatado
- [ ] Paginação funcionando (testado next_id/cursor)
- [ ] Valores específicos do CSV encontrados no JSON
- [ ] Total de registros compatível

## 🔍 Evidências Específicas

**Procurado:**
- "BANCO BRADESCO S.A" → [✅ Encontrado / ❌ Não encontrado]
- CNPJ "60746948002590" → [✅ Encontrado / ❌ Não encontrado]
- Valor "275.00" → [✅ Encontrado / ❌ Não encontrado]

## 📝 Observações

Qualquer observação relevante, dúvidas, ou próximos passos.

## ⚠️ Problemas Encontrados

Lista de problemas/limitações descobertos.

## ✅ Conclusão

[SUCESSO / FALHOU / PARCIAL]

**Próxima ação:**
[O que fazer com base nesta descoberta]
```

---

## 🎯 Objetivo Final

Ao completar TODOS os testes, teremos:

1. ✅ Endpoints corretos para cada uma das 8 entidades
2. ✅ Lista completa de campos disponíveis em cada API
3. ✅ Mapeamento 100% CSV ↔ API
4. ✅ Evidências de que a extração está completa
5. ✅ Documentação para atualizar o código Java

---

## 📌 Status das Descobertas

| Entidade | Status | Arquivo | Prioridade |
|----------|--------|---------|------------|
| Lançamentos a Pagar | 🔴 Pendente | lancamentos-a-pagar.md | 🔥 MÁXIMA |
| Lançamentos a Receber | 🔴 Pendente | lancamentos-a-receber.md | Alta |
| Ocorrências | 🔴 Pendente | ocorrencias.md | Alta |
| Coletas (GraphQL) | 🔴 Pendente | campos-coletas.md | Média |
| Fretes (GraphQL) | 🔴 Pendente | campos-fretes.md | Média |
| Manifestos | 🔴 Pendente | dataexport-manifestos.md | Média |
| Cotações | 🔴 Pendente | dataexport-cotacoes.md | Média |
| Localizador | 🔴 Pendente | dataexport-localizador.md | Média |

**Legenda:**
- 🔴 Pendente
- 🟡 Em progresso
- 🟢 Completo
- ✅ Validado

---

Atualize este README conforme as descobertas forem sendo feitas!

