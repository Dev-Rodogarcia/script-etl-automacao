# 📊 Mapeamento de Campos CSV ↔ API

Este diretório contém o mapeamento detalhado entre os campos dos CSVs manuais e os campos retornados pelas APIs.

---

## 🎯 Objetivo

Garantir que **100% dos campos** dos relatórios manuais sejam extraídos via API.

---

## 📂 Arquivos de Mapeamento

### API REST

- `lancamentos-a-pagar-csv-api.md` (24 colunas)
- `lancamentos-a-receber-csv-api.md` (23 colunas)
- `ocorrencias-csv-api.md` (18 colunas)

### API GraphQL

- `coletas-csv-graphql.md` (22 colunas CSV vs campos GraphQL)
- `fretes-csv-graphql.md` (49 colunas CSV vs 66+ campos GraphQL)

### API Data Export

- `manifestos-xlsx-api.md`
- `cotacoes-xlsx-api.md`
- `localizador-xlsx-api.md`

---

## 📋 Template de Mapeamento

Ao criar um arquivo de mapeamento, usar esta estrutura:

```markdown
# Mapeamento: [Entidade]

## CSV de Referência
- **Arquivo:** `nome-do-arquivo.csv`
- **Total de Colunas:** XX
- **Total de Linhas:** YY

## Endpoint da API
`[MÉTODO] {{base_url}}/api/...`

## Mapeamento Completo

| # | Coluna CSV | Campo API | Tipo | Status | Observações |
|---|------------|-----------|------|--------|-------------|
| 1 | CNPJ Filial | branch.document | string | ✅ | Mapeado direto |
| 2 | Filial | branch.name | string | ✅ | Mapeado direto |
| 3 | Fornecedor | supplier.name | string | ✅ | Dentro de objeto aninhado |
| 4 | Valor Principal | principal_value | decimal | ✅ | Campo numérico |
| 5 | Coluna X | ? | ? | ❌ | NÃO ENCONTRADO - CRÍTICO |
| 6 | Coluna Y | - | - | ⚠️ | Pode ser calculado (soma de A+B) |
| ... | ... | ... | ... | ... | ... |

**Legenda Status:**
- ✅ Mapeado (campo existe na API)
- ❌ Não Encontrado (campo NÃO existe na API)
- ⚠️ Derivado (pode ser calculado a partir de outros campos)
- 🔄 Relacionamento (precisa buscar em outro endpoint/tabela)

## Estatísticas

- **Total de Colunas CSV:** XX
- **Mapeadas Diretas:** YY (ZZ%)
- **Não Encontradas:** AA (BB%)
- **Derivadas/Calculadas:** CC (DD%)

## Campos da API NÃO Presentes no CSV

Lista de campos que a API retorna mas o CSV não tem:

1. campo_extra_1
2. campo_extra_2
...

(Estes campos podem ser úteis para enriquecer a extração)

## Campos CSV NÃO Disponíveis na API

⚠️ **ATENÇÃO:** Estes campos existem no CSV manual mas NÃO foram encontrados na API:

1. campo_faltante_1 - [Possível solução: ...]
2. campo_faltante_2 - [Alternativa: ...]
...

## Recomendações

Com base no mapeamento:

1. [Ação necessária 1]
2. [Ação necessária 2]
...

## Próximos Passos

- [ ] Validar campos derivados
- [ ] Testar relacionamentos
- [ ] Atualizar DTOs no código Java
- [ ] Adicionar novos campos à extração
```

---

## 🔍 Como Fazer o Mapeamento

### Passo 1: Listar Colunas do CSV

Abrir CSV no Excel ou editor de texto:
```csv
Coluna1,Coluna2,Coluna3,...
```

### Passo 2: Obter Campos da API

No Insomnia, após executar request:
1. Copiar um objeto da resposta
2. Listar todas as keys (campos)

Exemplo de resposta API:
```json
{
  "data": [
    {
      "id": 123,
      "branch": {
        "document": "12345678000199",
        "name": "Filial XYZ"
      },
      "supplier": {
        "name": "Fornecedor ABC"
      },
      "principal_value": 1500.50
    }
  ]
}
```

### Passo 3: Comparar e Mapear

Linha por linha do CSV, encontrar o campo correspondente na API.

**Exemplo:**
- CSV: "CNPJ Filial" → API: `branch.document`
- CSV: "Filial" → API: `branch.name`
- CSV: "Fornecedor" → API: `supplier.name`

### Passo 4: Identificar Gaps

Listar campos que NÃO têm correspondência.

### Passo 5: Documentar

Preencher o template de mapeamento.

---

## 📈 Meta de Completude

**Objetivo:** ≥95% de campos mapeados para TODAS as 8 entidades

**Status Atual:**

| Entidade | % Mapeado | Status |
|----------|-----------|--------|
| Lançamentos a Pagar | 0% | 🔴 Não iniciado |
| Lançamentos a Receber | 0% | 🔴 Não iniciado |
| Ocorrências | 0% | 🔴 Não iniciado |
| Coletas | 0% | 🔴 Não iniciado |
| Fretes | 0% | 🔴 Não iniciado |
| Manifestos | 0% | 🔴 Não iniciado |
| Cotações | 0% | 🔴 Não iniciado |
| Localizador | 0% | 🔴 Não iniciado |

**Legenda:**
- 🔴 0-50%: Crítico
- 🟡 51-90%: Em progresso
- 🟢 91-100%: Excelente

---

Atualize os arquivos conforme o mapeamento for sendo realizado!

