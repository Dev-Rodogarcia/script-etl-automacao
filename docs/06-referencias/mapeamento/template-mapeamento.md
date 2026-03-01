# Mapeamento: [Nome da Entidade]

**Data:** DD/MM/YYYY  
**Responsável:** [Seu nome]  
**Status:** 🔴 Em Progresso / 🟡 Parcial / 🟢 Completo

---

## 📄 Fonte de Dados

### CSV/XLSX de Referência

- **Arquivo:** `nome-do-arquivo.csv`
- **Localização:** `docs/arquivos-csv/`
- **Data do Download:** DD/MM/YYYY
- **Total de Colunas:** XX
- **Total de Linhas:** YY (excluindo cabeçalho)

---

## 🔗 Endpoint da API

### Informações do Endpoint

- **URL:** `[MÉTODO] {{base_url}}/api/...`
- **Tipo de API:** REST / GraphQL / Data Export
- **Autenticação:** Bearer Token ({{token_xxx}})
- **Paginação:** Sim / Não
- **Documentação:** https://...

### Request de Teste

```http
GET /api/...?since=2025-11-03T00:00:00-03:00&per=100
Headers:
  Authorization: Bearer {{token_xxx}}
  Accept: application/json
```

### Response Estrutura

```json
{
  "data": [
    {
      "campo1": "valor",
      "campo2": 123,
      ...
    }
  ],
  "paging": {...}
}
```

---

## 🗺️ Mapeamento Completo

### Tabela de Mapeamento

| # | Coluna CSV | Campo API | Tipo | Aninhado | Status | Notas |
|---|------------|-----------|------|----------|--------|-------|
| 1 | CNPJ Filial | branch.document | string | Sim | ✅ | Dentro do objeto `branch` |
| 2 | Filial | branch.name | string | Sim | ✅ | Mesmo objeto |
| 3 | Fornecedor | supplier.name | string | Sim | ✅ | Objeto `supplier` |
| 4 | Valor Principal | principal_value | decimal | Não | ✅ | Campo direto |
| 5 | Coluna X | - | - | - | ❌ | NÃO ENCONTRADO - CRÍTICO |
| 6 | Coluna Y | field_a + field_b | calculated | - | ⚠️ | Soma de dois campos |
| ... | ... | ... | ... | ... | ... | ... |

**Legenda Status:**
- ✅ **Mapeado** - Campo existe na API e corresponde diretamente
- ❌ **Não Encontrado** - Campo NÃO existe na API
- ⚠️ **Derivado** - Pode ser calculado/derivado de outros campos
- 🔄 **Relacionamento** - Precisa buscar em outro endpoint

**Legenda Aninhado:**
- **Sim** - Campo está dentro de um objeto (ex: `branch.name`)
- **Não** - Campo está no nível raiz

---

## 📊 Estatísticas do Mapeamento

### Resumo Geral

- **Total de Colunas CSV:** XX
- **Mapeadas Diretamente:** YY (ZZ%)
- **Derivadas/Calculadas:** AA (BB%)
- **Não Encontradas:** CC (DD%)
- **Relacionamentos Externos:** EE (FF%)

### Gráfico de Completude

```
████████████████████████░░░░  92% Completo
```

**Meta:** ≥95% de completude

---

## 🆕 Campos Extras da API

Campos que a API retorna mas o CSV manual NÃO possui:

| Campo API | Tipo | Descrição | Útil? |
|-----------|------|-----------|-------|
| created_at | datetime | Data de criação do registro | ✅ Sim, para auditoria |
| updated_at | datetime | Última atualização | ✅ Sim |
| internal_id | integer | ID interno do sistema | ⚠️ Talvez |
| ... | ... | ... | ... |

**Recomendação:** Incluir campos úteis na extração para enriquecer dados.

---

## ❌ Campos Faltantes (CRÍTICO)

Campos que existem no CSV mas NÃO foram encontrados na API:

| # | Coluna CSV | Tipo Esperado | Possível Solução |
|---|------------|---------------|------------------|
| 5 | Coluna X | string | Verificar se está em endpoint relacionado |
| 12 | Coluna Y | decimal | Pode ser calculado: campo_a + campo_b |
| 18 | Coluna Z | date | Pode estar com nome diferente: `service_date` |

### Ações Necessárias

- [ ] Testar endpoint relacionado para Coluna X
- [ ] Implementar cálculo para Coluna Y
- [ ] Verificar aliases para Coluna Z

---

## 🔍 Evidências de Validação

### Valores Específicos Testados

Valores do CSV que foram encontrados na resposta da API:

| Valor no CSV | Linha CSV | Campo API | Localização JSON |
|--------------|-----------|-----------|------------------|
| "BANCO BRADESCO" | 2 | supplier.name | data[5].supplier.name |
| "60746948002590" | 2 | supplier.document | data[5].supplier.document |
| 275.00 | 3 | principal_value | data[2].principal_value |
| "JOÃO DA SILVA" | 3 | supplier.name | data[2].supplier.name |

✅ **Conclusão:** Endpoint retorna dados completos!

---

## 📝 Observações e Notas

### Particularidades Descobertas

1. **Datas:** API retorna em ISO 8601 com timezone, CSV tem formato BR (DD/MM/YYYY)
2. **Valores Monetários:** API em centavos (1500 = R$15,00), CSV formatado (15,00)
3. **CNPJs:** API sem formatação (12345678000199), CSV formatado (12.345.678/0001-99)
4. **Nulos:** API usa `null`, CSV pode ter célula vazia ou "-"

### Transformações Necessárias

Para replicar exatamente o CSV via API:

```java
// Exemplo de transformação
String cnpjFormatado = formatarCnpj(api.getSupplier().getDocument());
String dataFormatada = converterIso8601ParaBr(api.getCreatedAt());
BigDecimal valorReais = api.getPrincipalValue().divide(100); // Centavos → Reais
```

---

## 🔄 Mapeamento de Objetos Aninhados

### Estrutura Hierárquica

```
Response JSON:
{
  "id": 123,
  "branch": {              ← Objeto Filial
    "id": 1,
    "name": "MTZ - RODOGARCIA",
    "document": "60960473000162"
  },
  "supplier": {            ← Objeto Fornecedor
    "id": 456,
    "name": "BANCO BRADESCO S.A",
    "document": "60746948002590",
    "person_type": "legal_entity"
  },
  "principal_value": 1512, ← Campo direto (centavos)
  "issue_date": "2025-11-02T00:00:00-03:00",
  ...
}
```

**Mapeamento:**
```
CSV: CNPJ Filial       → API: branch.document
CSV: Filial            → API: branch.name
CSV: CNPJ Fornecedor   → API: supplier.document
CSV: Fornecedor        → API: supplier.name
CSV: Valor Principal   → API: principal_value / 100
```

---

## ✅ Checklist de Completude

Antes de marcar mapeamento como completo:

- [ ] Todos os campos do CSV analisados
- [ ] Campos da API documentados
- [ ] Evidências validadas (valores específicos encontrados)
- [ ] Campos faltantes identificados e soluções propostas
- [ ] Transformações necessárias documentadas
- [ ] Objetos aninhados mapeados
- [ ] Percentual de completude ≥95%
- [ ] Revisado por outro desenvolvedor (opcional)

---

## 🔄 Próximas Ações

Com base neste mapeamento:

1. **Atualizar DTOs no código Java:**
   - Adicionar campos faltantes
   - Mapear objetos aninhados
   - Implementar transformações

2. **Atualizar Repository:**
   - Criar/modificar colunas no banco
   - Adicionar campos à query de INSERT/UPDATE

3. **Testar Extração:**
   - Executar script com novo endpoint
   - Validar dados extraídos vs CSV
   - Verificar completude

4. **Documentar Endpoint:**
   - Criar documentação final em `docs/endpoints/`
   - Incluir exemplos de request/response
   - Adicionar à wiki do projeto

---

**Status do Mapeamento:** 🔴 Em Progresso

**Última Atualização:** DD/MM/YYYY

**Próxima Revisão:** DD/MM/YYYY

