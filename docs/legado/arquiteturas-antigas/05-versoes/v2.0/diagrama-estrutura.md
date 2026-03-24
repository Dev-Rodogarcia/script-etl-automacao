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
# 🏗️ Diagrama da Estrutura Atualizada

## 📊 Visão Geral da Arquitetura

```
┌─────────────────────────────────────────────────────────────────┐
│                    API REST ESL Cloud                            │
│              /api/accounting/debit/billings                      │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         │ JSON Response
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                   FaturaAPagarDTO                                │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │ CAMPOS DISPONÍVEIS (14/24)                                │  │
│  │ • id, document, issue_date, due_date, value, type         │  │
│  │ • corporation → CorporationDTO                            │  │
│  │ • receiver → ReceiverDTO                                  │  │
│  │ • comments                                                │  │
│  │ • accounting_planning_management → AccountingPlanningDTO │  │
│  │ • cost_centers[] → List<CostCenterDTO>                   │  │
│  └───────────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │ CAMPOS FUTUROS (10/24) - @JsonIgnore                      │  │
│  │ • sequencia, cheque, vencimentoOriginal                   │  │
│  │ • competencia, dataBaixa, dataLiquidacao                  │  │
│  │ • bancoPagamento, contaPagamento, descricaoDespesa        │  │
│  └───────────────────────────────────────────────────────────┘  │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         │ Mapeamento
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                  FaturaAPagarMapper                              │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │ TRANSFORMAÇÕES                                            │  │
│  │ • Extrai dados de objetos aninhados                       │  │
│  │ • Converte String → LocalDate, BigDecimal                 │  │
│  │ • Concatena múltiplos centros de custo                    │  │
│  │ • CALCULA STATUS:                                         │  │
│  │   if (dueDate < hoje) → "Vencido"                         │  │
│  │   else → "Pendente"                                       │  │
│  │ • Serializa metadados JSON                                │  │
│  └───────────────────────────────────────────────────────────┘  │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         │ Entity
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                 FaturaAPagarEntity                               │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │ CAMPOS MAPEADOS (14)                                      │  │
│  │ • id, documentNumber, issueDate, dueDate, totalValue      │  │
│  │ • receiverCnpj, receiverName, invoiceType                 │  │
│  │ • cnpjFilial, filial                                      │  │
│  │ • observacoes, contaContabil, centroCusto                 │  │
│  │ • status (calculado)                                      │  │
│  └───────────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │ CAMPOS FUTUROS (9)                                        │  │
│  │ • sequencia, cheque, vencimentoOriginal, competencia      │  │
│  │ • dataBaixa, dataLiquidacao, bancoPagamento               │  │
│  │ • contaPagamento, descricaoDespesa                        │  │
│  └───────────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │ METADADOS                                                 │  │
│  │ • headerMetadata (JSON completo do cabeçalho)             │  │
│  │ • installmentsMetadata (JSON das parcelas)                │  │
│  └───────────────────────────────────────────────────────────┘  │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         │ Persistência
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│              FaturaAPagarRepository                              │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │ SQL MERGE (UPSERT)                                        │  │
│  │ • 22 parâmetros no PreparedStatement                      │  │
│  │ • ON target.id = source.id                                │  │
│  │ • WHEN MATCHED → UPDATE                                   │  │
│  │ • WHEN NOT MATCHED → INSERT                               │  │
│  └───────────────────────────────────────────────────────────┘  │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         │ Persistido
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│              SQL Server - Tabela faturas_a_pagar                 │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │ COLUNAS (24 + metadados + auditoria)                     │  │
│  │ • Chaves: id (PK), document_number (UK)                   │  │
│  │ • Essenciais: 8 colunas                                   │  │
│  │ • Novos: 6 colunas                                        │  │
│  │ • Futuros: 9 colunas (preparadas)                         │  │
│  │ • Metadados: 2 colunas NVARCHAR(MAX)                      │  │
│  │ • Auditoria: data_extracao                                │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🔄 Fluxo de Dados Detalhado

### 1. Extração da API

```
API Response (JSON)
├── id: 12345
├── document: "NF-2025-001"
├── issue_date: "2025-11-01"
├── due_date: "2025-11-30"
├── value: "1500.00"
├── type: "CiotBilling"
├── corporation: {
│   ├── cnpj: "12.345.678/0001-90"
│   └── nickname: "Filial SP"
│   }
├── receiver: {
│   ├── cnpjCpf: "98.765.432/0001-10"
│   └── name: "Fornecedor XYZ Ltda"
│   }
├── comments: "Pagamento urgente"
├── accounting_planning_management: {
│   └── name: "Despesas Operacionais"
│   }
└── cost_centers: [
    { name: "Centro A" },
    { name: "Centro B" }
    ]
```

### 2. Mapeamento DTO → Entity

```
FaturaAPagarDTO                    FaturaAPagarEntity
─────────────────                  ──────────────────
id                        →        id
document                  →        documentNumber
issue_date                →        issueDate (LocalDate)
due_date                  →        dueDate (LocalDate)
value                     →        totalValue (BigDecimal)
type                      →        invoiceType
corporation.cnpj          →        cnpjFilial
corporation.nickname      →        filial
receiver.cnpjCpf          →        receiverCnpj
receiver.name             →        receiverName
comments                  →        observacoes
accounting_planning...    →        contaContabil
cost_centers[].name       →        centroCusto (concatenado)
[CALCULADO]               →        status ("Pendente"/"Vencido")
[JSON COMPLETO]           →        headerMetadata
[JSON PARCELAS]           →        installmentsMetadata
```

### 3. Cálculo de Status

```
┌─────────────────────────────────────┐
│  LocalDate dueDate = entity.getDueDate()
│  LocalDate hoje = LocalDate.now()
│
│  if (dueDate == null)
│      status = "Indefinido"
│  else if (dueDate.isBefore(hoje))
│      status = "Vencido"
│  else
│      status = "Pendente"
└─────────────────────────────────────┘
```

---

## 🗂️ Estrutura de Classes

```
src/main/java/br/com/extrator/modelo/rest/faturaspagar/
│
├── FaturaAPagarDTO.java                    [PRINCIPAL]
│   ├── @JsonProperty para campos da API
│   ├── @JsonIgnore para campos futuros
│   └── @JsonIgnoreProperties(ignoreUnknown = true)
│
├── CorporationDTO.java                     [NOVO]
│   ├── cnpj
│   └── nickname
│
├── ReceiverDTO.java                        [ATUALIZADO]
│   ├── cnpj (mantido para compatibilidade)
│   ├── cnpjCpf (novo, prioritário)
│   └── name
│
├── AccountingPlanningManagementDTO.java    [NOVO]
│   └── name
│
├── CostCenterDTO.java                      [NOVO]
│   └── name
│
├── InstallmentDTO.java                     [NOVO]
│   ├── value
│   ├── interestValue
│   ├── discountValue
│   ├── paymentMethod
│   └── comments
│
└── FaturaAPagarMapper.java                 [ATUALIZADO]
    └── toEntity(dto, installmentsJson)
```

---

## 📊 Mapeamento de Campos

### Campos Disponíveis (14/24)

| # | Campo API | Campo DTO | Campo Entity | Tipo | Observação |
|---|-----------|-----------|--------------|------|------------|
| 1 | id | id | id | Long | Chave primária |
| 2 | document | document | documentNumber | String | Chave de negócio |
| 3 | issue_date | issueDate | issueDate | LocalDate | Convertido |
| 4 | due_date | dueDate | dueDate | LocalDate | Convertido |
| 5 | value | value | totalValue | BigDecimal | Convertido |
| 6 | type | type | invoiceType | String | - |
| 7 | corporation.cnpj | corporation.cnpj | cnpjFilial | String | Objeto aninhado |
| 8 | corporation.nickname | corporation.nickname | filial | String | Objeto aninhado |
| 9 | receiver.cnpjCpf | receiver.cnpjCpf | receiverCnpj | String | Priorizado |
| 10 | receiver.name | receiver.name | receiverName | String | - |
| 11 | comments | comments | observacoes | String | - |
| 12 | accounting_planning_management.name | accountingPlanningManagement.name | contaContabil | String | Objeto aninhado |
| 13 | cost_centers[].name | costCenters[].name | centroCusto | String | Array concatenado |
| 14 | [CALCULADO] | - | status | String | Lógica local |

### Campos Futuros (10/24)

| # | Campo Futuro | Tipo | Fonte Esperada | Status |
|---|--------------|------|----------------|--------|
| 1 | sequencia | String | GraphQL/Export | Placeholder |
| 2 | cheque | String | GraphQL/Export | Placeholder |
| 3 | vencimentoOriginal | LocalDate | GraphQL/Export | Placeholder |
| 4 | competencia | String | GraphQL/Export | Placeholder |
| 5 | dataBaixa | LocalDate | GraphQL/Export | Placeholder |
| 6 | dataLiquidacao | LocalDate | GraphQL/Export | Placeholder |
| 7 | bancoPagamento | String | GraphQL/Export | Placeholder |
| 8 | contaPagamento | String | GraphQL/Export | Placeholder |
| 9 | descricaoDespesa | String | GraphQL/Export | Placeholder |
| 10 | installments[].* | Vários | /installments endpoint | JSON metadata |

---

## 🎯 Pontos de Atenção

### ✅ Implementado
- [x] Mapeamento de 14 campos disponíveis
- [x] Cálculo local de status
- [x] Concatenação de múltiplos centros de custo
- [x] Compatibilidade com cnpj e cnpjCpf
- [x] Placeholders para 10 campos futuros
- [x] Metadados JSON completos
- [x] SQL MERGE com 22 parâmetros

### ⚠️ Limitações Conhecidas
- Campos futuros retornam NULL (esperado)
- Dados de parcelas em JSON separado (installments_metadata)
- Centro de custo concatenado (não normalizado)
- Status calculado localmente (não vem da API)

### 🔮 Próximas Evoluções
- Integração com GraphQL para campos futuros
- Normalização de centros de custo (tabela separada)
- Processamento de parcelas (installments)
- Cálculo de juros e descontos
- Integração com sistema de pagamentos

---

## 📚 Referências

- **Documentação API:** `/api/accounting/debit/billings`
- **Endpoint Parcelas:** `/api/accounting/debit/billings/{id}/installments`
- **Código Fonte:** `src/main/java/br/com/extrator/modelo/rest/faturaspagar/`
- **Testes:** `docs/CHECKLIST_VALIDACAO_CAMPOS.md`
- **Exemplos:** `docs/EXEMPLOS_USO_NOVOS_CAMPOS.md`

---

**Última Atualização:** 04/11/2025  
**Versão:** 2.0 - Expansão para 14 campos disponíveis

