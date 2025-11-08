# Análise de Alinhamento - Classes de API

## Data: 2025-11-06

## Objetivo
Verificar se todas as classes de API (`ClienteApiGraphQL.java`, `ClienteApiDataExport.java`, `ClienteApiRest.java`) estão 100% alinhadas com:
1. Documentação de descobertas-endpoints (`docs/descobertas-endpoints/`)
2. Technical Specification (`.kiro/specs/implementacao-apis-funcionais/technical-specification.md`)

---

## 1. CLIENTE API GRAPHQL

### 1.1. Método `buscarFretes(LocalDate dataReferencia)`

**Status:** ✅ **100% ALINHADO**

**Query GraphQL:** `BuscarFretesExpandidaV3` ✅

**Campos Expandidos Conforme Documentação (`fretes.md` linhas 150-173):**

| # | Campo CSV | Query GraphQL | Status na Query |
|---|-----------|---------------|----------------|
| 1 | Filial | `corporation { name }` | ✅ Mapeado (linha 391-393) |
| 2 | Pagador | `payer { name }` | ✅ Mapeado (linha 363-366) |
| 3 | Remetente | `sender { name }` | ✅ Mapeado (linha 367-378) |
| 4 | Origem | `sender { mainAddress { city { name } } }` | ✅ Mapeado (linha 370-377) |
| 5 | UF Origem | `sender { mainAddress { city { state { code } } } }` | ✅ Mapeado (linha 373-375) |
| 6 | Destinatario | `receiver { name }` | ✅ Mapeado (linha 379-390) |
| 7 | Destino | `receiver { mainAddress { city { name } } }` | ✅ Mapeado (linha 382-389) |
| 8 | UF Destino | `receiver { mainAddress { city { state { code } } } }` | ✅ Mapeado (linha 385-387) |
| 9 | Data frete | `serviceAt` | ✅ Mapeado (linha 355) |
| 10 | Nº CT-e | `referenceNumber` | ✅ Mapeado (linha 354) |
| 11 | NF | `freightInvoices { number }` | ✅ Mapeado (linha 394-396) |
| 12 | Volumes | `invoicesTotalVolumes` | ✅ Mapeado (linha 359) |
| 13 | Kg Taxado | `taxedWeight` | ✅ Mapeado (linha 360) |
| 14 | Kg Real | `realWeight` | ✅ Mapeado (linha 361) |
| 15 | M3 | `totalCubicVolume` | ✅ Mapeado (linha 362) |
| 16 | Valor NF | `invoicesValue` | ✅ Mapeado (linha 358) |
| 17 | Valor Frete | `subtotal` | ✅ Mapeado (linha 357) |
| 18 | Valor Total do Serviço | `total` | ✅ Mapeado (linha 356) |
| 19 | Tabela de Preço | `customerPriceTable { name }` | ✅ Mapeado (linha 397-399) |
| 20 | Classificação | `freightClassification { name }` | ✅ Mapeado (linha 400-402) |
| 21 | Centro de Custo | `costCenter { name }` | ✅ Mapeado (linha 403-405) |
| 22 | Usuário | `user { name }` | ✅ Mapeado (linha 406-408) |

**Campos Adicionais:** A query inclui mais de 60 campos adicionais (linhas 409-466) que não estão no CSV mas são úteis para completude.

**Filtro de Data:** ✅ Correto
- Formato: `"{{data_inicio}} - {{data_fim}}"` (linha 481)
- Campo: `serviceAt` (linha 485)

**Paginação:** ✅ Correto
- `first: 100` (linha 350)
- `after: $after` (linha 350)
- `hasNextPage` e `endCursor` (linhas 470-471)

**Conclusão:** ✅ Query 100% alinhada com a documentação `fretes.md`.

---

### 1.2. Método `buscarColetas(LocalDate dataReferencia)`

**Status:** ✅ **100% ALINHADO**

**Query GraphQL:** `BuscarColetasExpandidaV2` ✅

**Campos Expandidos Conforme Documentação (`coletas.md` linhas 130-153):**

| # | Campo CSV | Query GraphQL | Status na Query |
|---|-----------|---------------|----------------|
| 1 | Coleta | `sequenceCode` | ✅ Mapeado (linha 221) |
| 2 | Cliente | `customer { name }` | ✅ Mapeado (linha 238-241) |
| 3 | Solicitante | `requester` | ✅ Mapeado (linha 229) |
| 4 | Local da Coleta | `pickAddress { line1 }` | ✅ Mapeado (linha 242-250) |
| 5 | Cidade | `pickAddress { city { name } }` | ✅ Mapeado (linha 244-249) |
| 6 | UF / Estado | `pickAddress { city { state { code } } }` | ✅ Mapeado (linha 246-248) |
| 7 | Solicitação (Data) | `requestDate` | ✅ Mapeado (linha 222) |
| 8 | Hora (Solicitação) | `requestHour` | ✅ Mapeado (linha 223) |
| 9 | Agendamento | `serviceDate` | ✅ Mapeado (linha 224) |
| 10 | Horário (Início) | `serviceStartHour` | ✅ Mapeado (linha 225) |
| 11 | Finalização | `finishDate` | ✅ Mapeado (linha 226) |
| 12 | Hora.1 (Fim) | `serviceEndHour` | ✅ Mapeado (linha 227) |
| 13 | Status | `status` | ✅ Mapeado (linha 228) |
| 14 | Volumes | `invoicesVolumes` | ✅ Mapeado (linha 230) |
| 15 | Peso Real | `invoicesWeight` | ✅ Mapeado (linha 231) |
| 16 | Peso Taxado | `taxedWeight` | ✅ Mapeado (linha 232) |
| 17 | Valor NF | `invoicesValue` | ✅ Mapeado (linha 233) |
| 18 | Observações | `comments` | ✅ Mapeado (linha 234) |
| 19 | Agente | `agentId` | ✅ Mapeado (linha 235) |
| 20 | Usuário / Motorista | `user { name }` | ✅ Mapeado (linha 251-254) |
| 21 | Nº Manifesto | `manifestItemPickId` | ✅ Mapeado (linha 236) |
| 22 | Veículo | `vehicleTypeId` | ✅ Mapeado (linha 237) |

**Campos Adicionais:** A query inclui mais campos adicionais (linhas 255-268) que não estão no CSV mas são úteis para completude.

**Filtro de Data:** ✅ Correto
- Formato: `"{{data}}"` (linha 288, 306)
- Campo: `requestDate` (linha 290, 308)
- Busca em 2 dias: dia anterior + dia atual (linhas 279-332)

**Paginação:** ✅ Correto
- `first: 100` (linha 216)
- `after: $after` (linha 216)
- `hasNextPage` e `endCursor` (linhas 271-273)

**Conclusão:** ✅ Query 100% alinhada com a documentação `coletas.md`.

---

## 2. CLIENTE API DATA EXPORT

### 2.1. Método `buscarManifestos()`

**Status:** ✅ **100% ALINHADO**

**Template ID:** `6399` ✅ (linha 67, 111)

**Campo de Data:** `service_date` ✅ (linha 72)

**Valor `per`:** `"10000"` ✅ (linha 621-625)

**Tabela:** `manifests` ✅ (linha 77)

**Filtro de Data:** ✅ Correto
- Últimas 24 horas (linhas 139-142)
- Formato: `"yyyy-MM-dd - yyyy-MM-dd"` (linha 239)

**Conclusão:** ✅ Método 100% alinhado com a documentação `manifestos.md`.

---

### 2.2. Método `buscarCotacoes()`

**Status:** ✅ **100% ALINHADO**

**Template ID:** `6906` ✅ (linha 69, 119)

**Campo de Data:** `requested_at` ✅ (linha 73)

**Valor `per`:** `"1000"` ✅ (linha 621-625)

**Tabela:** `quotes` ✅ (linha 78)

**Filtro de Data:** ✅ Correto
- Últimas 24 horas (linhas 151-154)
- Formato: `"yyyy-MM-dd - yyyy-MM-dd"` (linha 239)

**Conclusão:** ✅ Método 100% alinhado com a documentação `cotacoes.md`.

---

### 2.3. Método `buscarLocalizacaoCarga()`

**Status:** ✅ **100% ALINHADO**

**Template ID:** `8656` ✅ (linha 68, 115)

**Campo de Data:** `service_at` ✅ (linha 74)

**Valor `per`:** `"10000"` ✅ (linha 621-625)

**Tabela:** `freights` ✅ (linha 79)

**Filtro de Data:** ✅ Correto
- Últimas 24 horas (linhas 163-166)
- Formato: `"yyyy-MM-dd - yyyy-MM-dd"` (linha 239)

**Conclusão:** ✅ Método 100% alinhado com a documentação `localizacaocarga.md`.

---

### 2.4. Método `obterValorPerPorTemplate(int templateId)`

**Status:** ✅ **100% ALINHADO**

**Valores `per` por Template:**
- Template `6906` (Cotações): `"1000"` ✅ (linha 621-625)
- Template `6399` (Manifestos): `"10000"` ✅ (linha 621-625)
- Template `8656` (Localização de Carga): `"10000"` ✅ (linha 621-625)

**Conclusão:** ✅ Método 100% alinhado com a documentação de descobertas-endpoints.

---

## 3. CLIENTE API REST

### 3.1. Status Atual

**Status:** ⏳ **AGUARDANDO RESPOSTA DO SUPORTE ESL**

**Motivo:** As APIs REST (`/report/accounting/debits/analytical`, `/report/accounting/credits/analytical`, `/report/invoice_occurrence/histories/analytical`) requerem autenticação via **Cookie + X-CSRF-Token**, que não é viável para scripts automatizados devido à expiração dos tokens.

**Documentação Criada:** `docs/solicitacao-suporte-esl-autenticacao-api.md`

**Próximos Passos:**
1. ⏳ Aguardar resposta do suporte ESL
2. ⏳ Se não houver solução via Bearer Token, implementar login programático em Java

**Conclusão:** ⏳ Implementação pendente até resolução do problema de autenticação.

---

## 4. CONCLUSÃO GERAL

### ✅ Status Final: **100% ALINHADO (GraphQL e Data Export)**

**GraphQL:**
- ✅ Fretes: Query `BuscarFretesExpandidaV3` com todos os 22 campos do CSV expandidos
- ✅ Coletas: Query `BuscarColetasExpandidaV2` com todos os 22 campos do CSV expandidos

**Data Export:**
- ✅ Manifestos: Template ID `6399`, `per: "10000"`, campo `service_date`
- ✅ Cotações: Template ID `6906`, `per: "1000"`, campo `requested_at`
- ✅ Localização de Carga: Template ID `8656`, `per: "10000"`, campo `service_at`

**REST:**
- ⏳ Aguardando resolução do problema de autenticação (Cookie + X-CSRF-Token)

**Alinhamento com Documentação:**
- ✅ 100% alinhado com `docs/descobertas-endpoints/` (GraphQL e Data Export)
- ✅ 100% alinhado com `.kiro/specs/implementacao-apis-funcionais/technical-specification.md`

---

## 5. PRÓXIMOS PASSOS

1. ✅ **GraphQL e Data Export:** Implementação completa e alinhada
2. ⏳ **REST:** Aguardar resposta do suporte ESL sobre autenticação
3. ⏳ **Testes:** Executar testes de extração para validar funcionamento
4. ⏳ **Validação:** Validar dados extraídos contra CSV de origem

---

**Data da Análise:** 2025-11-06  
**Analista:** Auto (Cursor AI)  
**Status:** ✅ **APROVADO - 100% ALINHADO (GraphQL e Data Export)**

