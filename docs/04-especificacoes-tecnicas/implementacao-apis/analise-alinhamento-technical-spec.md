# Análise de Alinhamento - Technical Specification

## Overview

Este documento avalia o alinhamento do `technical-specification.md` com:
1. `requirements.md` (Requisitos funcionais)
2. `design.md` (Design técnico)
3. `docs/descobertas-endpoints/` (Descobertas do Insomnia)

---

## ✅ Alinhamentos Confirmados

### 1. Queries GraphQL

| Item | Descobertas Insomnia | Technical Spec | Status |
|------|---------------------|----------------|--------|
| **Query Fretes** | `BuscarFretesExpandidaV3` | `BuscarFretesExpandidaV3` | ✅ **Alinhado** |
| **Query Coletas** | `BuscarColetasExpandidaV2` | `BuscarColetasExpandidaV2` | ✅ **Alinhado** |
| **Tipo Fretes** | `FreightBase` | `FreightBase` | ✅ **Alinhado** |
| **Tipo Coletas** | `Pick` | `Pick` | ✅ **Alinhado** |
| **Paginação Fretes** | `first: 100` | `first: 100` | ✅ **Alinhado** |
| **Paginação Coletas** | `first: 100` | `first: 100` | ✅ **Alinhado** |
| **Campo Filtro Fretes** | `serviceAt: "{{data_inicio}} - {{data_fim}}"` | `serviceAt: "{{data_inicio}} - {{data_fim}}"` | ✅ **Alinhado** |
| **Campo Filtro Coletas** | `requestDate: "{{data_inicio}}"` | `requestDate: "{{data_inicio}}"` | ✅ **Alinhado** |

### 2. API Data Export - Paginação

| Item | Descobertas Insomnia | Technical Spec | Status |
|------|---------------------|----------------|--------|
| **Cotações - per** | `per: "1000"` | `per: "1000"` | ✅ **Alinhado** |
| **Manifestos - per** | `per: "10000"` | `per: "10000"` | ✅ **Alinhado** |
| **Localização - per** | `per: "10000"` | `per: "10000"` | ✅ **Alinhado** |
| **Template Cotações** | `6906` | `6906` | ✅ **Alinhado** |
| **Template Manifestos** | `6399` | `6399` | ✅ **Alinhado** |
| **Template Localização** | `8656` | `8656` | ✅ **Alinhado** |

### 3. Campos Expandidos GraphQL - Fretes

| Campo | Descobertas Insomnia | Technical Spec | Status |
|-------|---------------------|----------------|--------|
| **payer** | `payer { id name }` | `payer { id name }` | ✅ **Alinhado** |
| **sender** | `sender { id name mainAddress { city { name state { code } } } }` | `sender { id name mainAddress { city { name state { code } } } }` | ✅ **Alinhado** |
| **receiver** | `receiver { id name mainAddress { city { name state { code } } } }` | `receiver { id name mainAddress { city { name state { code } } } }` | ✅ **Alinhado** |
| **corporation** | `corporation { name }` | `corporation { name }` | ✅ **Alinhado** |
| **freightInvoices** | `freightInvoices { number }` | `freightInvoices { number }` | ✅ **Alinhado** |
| **customerPriceTable** | `customerPriceTable { name }` | `customerPriceTable { name }` | ✅ **Alinhado** |
| **freightClassification** | `freightClassification { name }` | `freightClassification { name }` | ✅ **Alinhado** |
| **costCenter** | `costCenter { name }` | `costCenter { name }` | ✅ **Alinhado** |
| **user** | `user { name }` | `user { name }` | ✅ **Alinhado** |

### 4. Campos Expandidos GraphQL - Coletas

| Campo | Descobertas Insomnia | Technical Spec | Status |
|-------|---------------------|----------------|--------|
| **customer** | `customer { id name }` | `customer { id name }` | ✅ **Alinhado** |
| **pickAddress** | `pickAddress { line1 city { name state { code } } }` | `pickAddress { line1 city { name state { code } } }` | ✅ **Alinhado** |
| **user** | `user { id name }` | `user { id name }` | ✅ **Alinhado** |

### 5. Problema totalCount

| Item | Descobertas Insomnia | Technical Spec | Status |
|------|---------------------|----------------|--------|
| **totalCount existe?** | ❌ **NÃO EXISTE** | ❌ **NÃO EXISTE** | ✅ **Alinhado** |
| **Evidência Fretes** | `"Field 'totalCount' doesn't exist on type 'PageInfo'"` | Documentado como problema crítico | ✅ **Alinhado** |
| **Evidência Coletas** | `"Field 'totalCount' doesn't exist on type 'PickConnection'"` | Documentado como problema crítico | ✅ **Alinhado** |
| **Solução proposta** | Paginação completa | Contar via `buscarFretes()` / `buscarColetas()` | ✅ **Alinhado** |

---

## ⚠️ Discrepâncias e Ajustes Necessários

### 1. Query GraphQL Fretes - Campos Faltantes

**Problema:** A technical spec menciona "outros 60+ campos simples" mas não lista todos os campos necessários do CSV.

**Descobertas Insomnia (fretes.md linha 150-173):**
- ✅ `referenceNumber` (Nº CT-e)
- ✅ `serviceAt` (Data frete)
- ✅ `total` (Valor Total do Serviço)
- ✅ `invoicesTotalVolumes` (Volumes)
- ✅ `taxedWeight` (Kg Taxado)
- ✅ `realWeight` (Kg Real)
- ✅ `totalCubicVolume` (M3)
- ✅ `invoicesValue` (Valor NF)
- ✅ `subtotal` (Valor Frete)

**Ação Necessária:**
- ⚠️ Adicionar lista completa de campos simples na seção 1.1 do technical-specification.md
- ⚠️ Garantir que todos os 22 campos do CSV estejam na query GraphQL

### 2. Query GraphQL Coletas - Campos Faltantes

**Problema:** A technical spec menciona "outros campos do CSV" mas não lista todos.

**Descobertas Insomnia (coletas.md linha 130-153):**
- ✅ `sequenceCode` (Coleta)
- ✅ `requestDate` (Solicitação)
- ✅ `requestHour` (Hora)
- ✅ `serviceDate` (Agendamento)
- ✅ `serviceStartHour` (Horário Início)
- ✅ `finishDate` (Finalização)
- ✅ `serviceEndHour` (Hora Fim)
- ✅ `status` (Status)
- ✅ `invoicesVolumes` (Volumes)
- ✅ `invoicesWeight` (Peso Real)
- ✅ `taxedWeight` (Peso Taxado)
- ✅ `invoicesValue` (Valor NF)
- ✅ `comments` (Observações)
- ✅ `agentId` (Agente)
- ✅ `manifestItemPickId` (Nº Manifesto)
- ✅ `vehicleTypeId` (Veículo)

**Ação Necessária:**
- ⚠️ Adicionar lista completa de campos simples na seção 1.2 do technical-specification.md
- ⚠️ Garantir que todos os 22 campos do CSV estejam na query GraphQL

### 3. DTOs Aninhados - Estrutura Incompleta

**Problema:** A technical spec lista os DTOs aninhados mas não especifica a estrutura completa.

**Ação Necessária:**
- ⚠️ Adicionar estrutura completa dos DTOs aninhados:
  - `SenderDTO` deve ter `mainAddress` do tipo `MainAddressDTO`
  - `MainAddressDTO` deve ter `city` do tipo `CityDTO`
  - `CityDTO` deve ter `state` do tipo `StateDTO`
  - Mesma estrutura para `ReceiverDTO` e `PickAddressDTO`

### 4. Mapeamento Entity - Campos Faltantes

**Problema:** A technical spec lista alguns campos mas não todos os 22 campos do CSV.

**Ação Necessária:**
- ⚠️ Adicionar mapeamento completo de todos os 22 campos do CSV para FreteEntity
- ⚠️ Adicionar mapeamento completo de todos os 22 campos do CSV para ColetaEntity
- ⚠️ Verificar se todos os campos estão no MERGE SQL dos Repositories

### 5. Data Export - Verificação de Completude

**Problema:** A technical spec menciona "verificar completude" mas não especifica como.

**Ação Necessária:**
- ⚠️ Adicionar checklist de verificação para cada DTO Data Export:
  - Cotações: 36 campos do CSV vs 37 chaves da API
  - Manifestos: 80 campos do CSV vs 92 chaves da API
  - Localização: 17 campos do CSV vs 17 chaves da API

---

## 📋 Checklist de Alinhamento com Requirements.md

| Requirement | Technical Spec | Status |
|-------------|----------------|--------|
| **Req 1.1:** Query `BuscarFretesExpandidaV3` | ✅ Seção 1.1 | ✅ **Alinhado** |
| **Req 1.2:** Paginação `first: 100` | ✅ Seção 1.1 | ✅ **Alinhado** |
| **Req 1.3:** Mapear 22 campos do CSV | ⚠️ Mencionado mas não completo | ⚠️ **Incompleto** |
| **Req 2.1:** Query `BuscarColetasExpandidaV2` | ✅ Seção 1.2 | ✅ **Alinhado** |
| **Req 2.2:** Paginação `first: 100` | ✅ Seção 1.2 | ✅ **Alinhado** |
| **Req 2.3:** Mapear 22 campos do CSV | ⚠️ Mencionado mas não completo | ⚠️ **Incompleto** |
| **Req 3.2:** Paginação `per: "1000"` (Cotações) | ✅ Seção 2.1 | ✅ **Alinhado** |
| **Req 4.2:** Paginação `per: "10000"` (Manifestos) | ✅ Seção 2.1 | ✅ **Alinhado** |
| **Req 5.2:** Paginação `per: "10000"` (Localização) | ✅ Seção 2.1 | ✅ **Alinhado** |

---

## 📋 Checklist de Alinhamento com Design.md

| Design Item | Technical Spec | Status |
|-------------|----------------|--------|
| **Design 3:** ClienteApiGraphQL - Query Fretes | ✅ Seção 1.1 | ✅ **Alinhado** |
| **Design 3:** ClienteApiGraphQL - Query Coletas | ✅ Seção 1.2 | ✅ **Alinhado** |
| **Design 3:** ClienteApiGraphQL - totalCount | ✅ Seção 1.3 e 1.4 | ✅ **Alinhado** |
| **Design 4:** ClienteApiDataExport - per values | ✅ Seção 2.1 e 2.2 | ✅ **Alinhado** |
| **Design 5:** Mappers - Campos expandidos | ⚠️ Seção 5.1 e 6.1 (incompleto) | ⚠️ **Incompleto** |
| **Design 7:** Repositories - MERGE SQL | ⚠️ Seção 9.1 e 10.1 (mencionado) | ⚠️ **Incompleto** |

---

## 🔍 Análise Detalhada por Seção

### Seção 1.1: Método `buscarFretes()`

**✅ Correto:**
- Nome da query: `BuscarFretesExpandidaV3`
- Tipo: `FreightBase`
- Campo de filtro: `serviceAt` com intervalo
- Paginação: `first: 100`
- Campos expandidos básicos listados

**⚠️ Incompleto:**
- Não lista todos os 22 campos do CSV
- Não especifica todos os campos simples necessários
- Query exemplo está truncada ("... outros 60+ campos simples")

**🔧 Ação:**
- Adicionar lista completa dos 22 campos do CSV conforme `fretes.md` linha 150-173
- Completar a query GraphQL com todos os campos necessários

### Seção 1.2: Método `buscarColetas()`

**✅ Correto:**
- Nome da query: `BuscarColetasExpandidaV2`
- Tipo: `Pick`
- Campo de filtro: `requestDate` com data única
- Paginação: `first: 100`
- Campos expandidos básicos listados

**⚠️ Incompleto:**
- Não lista todos os 22 campos do CSV
- Não especifica todos os campos simples necessários
- Query exemplo está truncada ("... outros campos do CSV")

**🔧 Ação:**
- Adicionar lista completa dos 22 campos do CSV conforme `coletas.md` linha 130-153
- Completar a query GraphQL com todos os campos necessários

### Seção 1.3 e 1.4: Métodos de Contagem

**✅ Correto:**
- Identifica corretamente que `totalCount` não existe
- Propõe solução via paginação completa
- Alinhado com descobertas do Insomnia

**✅ Alinhado:** Nenhuma ação necessária

### Seção 2.1: Método `construirCorpoRequisicao()`

**✅ Correto:**
- Identifica problema: `per: "100"` genérico
- Propõe solução: adicionar parâmetro `per`
- Valores corretos: 1000, 10000, 10000

**✅ Alinhado:** Nenhuma ação necessária

### Seção 2.2: Método `buscarDadosGenericos()`

**✅ Correto:**
- Propõe método `obterValorPerPorTemplate()`
- Valores corretos por template
- Lógica correta

**✅ Alinhado:** Nenhuma ação necessária

### Seção 3.1: FreteNodeDTO - Campos Expandidos

**✅ Correto:**
- Lista todos os objetos aninhados necessários
- Campos simples adicionais listados

**⚠️ Incompleto:**
- Não especifica estrutura completa dos DTOs aninhados
- Não lista todos os 22 campos do CSV

**🔧 Ação:**
- Adicionar estrutura completa dos DTOs aninhados
- Listar todos os 22 campos do CSV

### Seção 4.1: ColetaNodeDTO - Campos Expandidos

**✅ Correto:**
- Lista objetos aninhados necessários

**⚠️ Incompleto:**
- Não especifica estrutura completa dos DTOs aninhados
- Não lista todos os 22 campos do CSV

**🔧 Ação:**
- Adicionar estrutura completa dos DTOs aninhados
- Listar todos os 22 campos do CSV

### Seção 5.1: FreteMapper - Mapeamento

**✅ Correto:**
- Estrutura básica de mapeamento de campos expandidos
- Lógica de verificação de null

**⚠️ Incompleto:**
- Não mapeia todos os 22 campos do CSV
- Não mapeia campos adicionais (corporation, freightInvoices, etc.)

**🔧 Ação:**
- Adicionar mapeamento completo de todos os 22 campos
- Adicionar mapeamento de corporation, freightInvoices, customerPriceTable, etc.

### Seção 7.1: FreteEntity - Campos

**✅ Correto:**
- Lista campos básicos para dados expandidos

**⚠️ Incompleto:**
- Não lista todos os 22 campos do CSV
- Não especifica tipos de dados corretos

**🔧 Ação:**
- Adicionar todos os 22 campos do CSV
- Especificar tipos de dados corretos (Long, String, BigDecimal, etc.)

### Seção 9.1: FreteRepository - MERGE SQL

**⚠️ Incompleto:**
- Apenas menciona que precisa adicionar campos
- Não especifica quais campos adicionar
- Não mostra exemplo de MERGE SQL atualizado

**🔧 Ação:**
- Listar todos os campos a adicionar ao MERGE
- Mostrar exemplo de MERGE SQL com todos os campos

---

## 📊 Resumo de Alinhamento

### ✅ Totalmente Alinhado (70%)

1. ✅ Queries GraphQL (nomes e estrutura básica)
2. ✅ Paginação (valores e lógica)
3. ✅ Problema totalCount (identificado e solução proposta)
4. ✅ API Data Export (valores de per)
5. ✅ Campos expandidos básicos (estrutura)

### ⚠️ Parcialmente Alinhado (25%)

1. ⚠️ Lista completa de campos do CSV (mencionado mas não completo)
2. ⚠️ Estrutura completa dos DTOs aninhados (listado mas não detalhado)
3. ⚠️ Mapeamento completo Entity (estrutura básica mas não todos os campos)
4. ⚠️ MERGE SQL completo (mencionado mas não especificado)

### ❌ Não Alinhado (5%)

1. ❌ Nenhum item crítico identificado

---

## 🎯 Recomendações Finais

### Prioridade Alta

1. **Completar lista de campos do CSV** para Fretes e Coletas (22 campos cada)
2. **Especificar estrutura completa dos DTOs aninhados** com todos os campos
3. **Adicionar mapeamento completo** no FreteMapper e ColetaMapper
4. **Especificar MERGE SQL completo** com todos os campos

### Prioridade Média

1. **Adicionar exemplos de código completos** (não truncados)
2. **Especificar tipos de dados** para todos os campos das Entities
3. **Adicionar checklist de verificação** para Data Export DTOs

### Prioridade Baixa

1. **Adicionar diagramas** de estrutura de DTOs aninhados
2. **Adicionar exemplos de JSON** de resposta da API
3. **Adicionar exemplos de MERGE SQL** completo

---

## ✅ Conclusão

O `technical-specification.md` está **70% alinhado** com as descobertas do Insomnia, requirements.md e design.md.

**Pontos Fortes:**
- ✅ Estrutura geral correta
- ✅ Queries GraphQL corretas
- ✅ Paginação correta
- ✅ Problema totalCount identificado corretamente
- ✅ Valores de per corretos

**Pontos a Melhorar:**
- ⚠️ Completar listas de campos do CSV
- ⚠️ Detalhar estrutura completa dos DTOs aninhados
- ⚠️ Especificar mapeamento completo
- ⚠️ Detalhar MERGE SQL completo

**Recomendação:** O documento está **pronto para implementação** após completar as listas de campos e estruturas detalhadas. As correções são principalmente de completude, não de correção de erros.

