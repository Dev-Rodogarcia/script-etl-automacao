# Requisições API GraphQL - Insomnia

## 🎯 Objetivo

Usar **GraphQL Introspection** para descobrir TODOS os campos disponíveis e garantir que a extração está completa.

---

## 🔧 Configuração Inicial GraphQL

### Criar Request Base no Insomnia

**Pasta:** API GraphQL

**Nome:** `[BASE] GraphQL Endpoint`

**Método:** POST

**URL:**
```
{{base_url}}/graphql
```

**Headers:**
```
Authorization: Bearer {{token_graphql}}
Content-Type: application/json
```

**Tipo de Body:** GraphQL

---

## 🔍 Passo 1: Introspection - Descobrir Schema Completo

### Pasta: API GraphQL > Introspection

#### 1️⃣ Listar Todos os Types Disponíveis

**Nome:** `[INTROSPECTION] Listar Types`

**Query:**
```graphql
query ListarTypes {
  __schema {
    types {
      name
      kind
      description
    }
  }
}
```

**Objetivo:** Ver todos os tipos disponíveis (Pick, Freight, etc.)

---

#### 2️⃣ Descobrir TODOS os Campos de "Pick" (Coletas)

**Nome:** `[INTROSPECTION] Campos de Pick (Coletas)`

**Query:**
```graphql
query CamposPick {
  __type(name: "Pick") {
    name
    fields {
      name
      description
      type {
        name
        kind
        ofType {
          name
          kind
        }
      }
    }
  }
}
```

**O que fazer com o resultado:**
1. Copiar todos os nomes de campos listados
2. Comparar com os 24 campos atuais no código Java
3. Identificar campos FALTANTES
4. Criar lista completa em `docs/descobertas/campos-coletas.md`

---

#### 3️⃣ Descobrir TODOS os Campos de "Freight" (Fretes)

**Nome:** `[INTROSPECTION] Campos de Freight`

**Query:**
```graphql
query CamposFreight {
  __type(name: "Freight") {
    name
    fields {
      name
      description
      type {
        name
        kind
        ofType {
          name
          kind
        }
      }
    }
  }
}
```

**Comparação:**
- CSV tem: **49 colunas**
- Código Java atual: **66 campos**
- Introspection vai mostrar: **TODOS os campos possíveis**

---

#### 4️⃣ Descobrir Campos de PickInput (Parâmetros de Filtro)

**Nome:** `[INTROSPECTION] PickInput Params`

**Query:**
```graphql
query ParamsPickInput {
  __type(name: "PickInput") {
    name
    inputFields {
      name
      description
      type {
        name
        kind
      }
    }
  }
}
```

**Objetivo:** Descobrir TODOS os filtros possíveis para refinar buscas

---

#### 5️⃣ Descobrir Campos de FreightInput

**Nome:** `[INTROSPECTION] FreightInput Params`

**Query:**
```graphql
query ParamsFreightInput {
  __type(name: "FreightInput") {
    name
    inputFields {
      name
      description
      type {
        name
        kind
      }
    }
  }
}
```

---

## 📊 Passo 2: Queries Completas de Coletas

### Pasta: API GraphQL > Coletas

#### 1️⃣ Query Atual (Do Código Java)

**Nome:** `[ATUAL] Buscar Coletas (24 campos)`

**Query:**
```graphql
query BuscarColetas($params: PickInput!, $after: String) {
  pick(params: $params, after: $after, first: 100) {
    totalCount
    edges {
      cursor
      node {
        id
        agentId
        cancellationReason
        cancellationUserId
        cargoClassificationId
        comments
        costCenterId
        destroyReason
        destroyUserId
        invoicesCubedWeight
        invoicesValue
        invoicesVolumes
        invoicesWeight
        lunchBreakEndHour
        lunchBreakStartHour
        notificationEmail
        notificationPhone
        pickTypeId
        pickupLocationId
        requestDate
        requestHour
        requester
        sequenceCode
        serviceDate
        serviceEndHour
        serviceStartHour
        status
        statusUpdatedAt
        taxedWeight
        vehicleTypeId
      }
    }
    pageInfo {
      hasNextPage
      endCursor
    }
  }
}
```

**Variables:**
```json
{
  "params": {
    "requestDate": "2025-11-03"
  }
}
```

**Validação:**
- Total esperado: **476 coletas** (do CSV)
- Verificar se `totalCount` bate com CSV

---

#### 2️⃣ Query EXPANDIDA (Com Campos Relacionados)

**Nome:** `[EXPANDIDA] Buscar Coletas + Relacionamentos`

Após a introspection, adicionar campos relacionados que podem estar faltando:

**Query:**
```graphql
query BuscarColetasExpandida($params: PickInput!, $after: String) {
  pick(params: $params, after: $after, first: 100) {
    totalCount
    edges {
      node {
        # Campos básicos atuais (24 campos)
        id
        sequenceCode
        requestDate
        serviceDate
        status
        
        # Campos relacionados (DESCOBRIR VIA INTROSPECTION)
        agent {
          id
          name
        }
        customer {
          id
          name
        }
        pickupLocation {
          id
          name
          city {
            name
            state {
              abbreviation
            }
          }
        }
        # ... adicionar outros relacionamentos descobertos
      }
    }
    pageInfo {
      hasNextPage
      endCursor
    }
  }
}
```

**Importante:** Ajustar query baseado nos resultados da introspection!

---

## 📊 Passo 3: Queries Completas de Fretes

### Pasta: API GraphQL > Fretes

#### 1️⃣ Query Atual (66 campos)

**Nome:** `[ATUAL] Buscar Fretes (66 campos)`

**Query:**
```graphql
query BuscarFretes($params: FreightInput!, $after: String) {
  freight(params: $params, after: $after, first: 100) {
    totalCount
    edges {
      cursor
      node {
        id
        accountingCreditId
        accountingCreditInstallmentId
        adValoremSubtotal
        additionalsSubtotal
        admFeeSubtotal
        calculationType
        collectSubtotal
        comments
        corporationId
        costCenterId
        createdAt
        cubagesCubedWeight
        customerPriceTableId
        deliveryDeadlineInDays
        deliveryPredictionDate
        deliveryPredictionHour
        deliveryRegionId
        deliverySubtotal
        destinationCityId
        dispatchSubtotal
        draftEmissionAt
        emergencySubtotal
        emissionType
        finishedAt
        freightClassificationId
        freightCubagesCount
        freightInvoicesCount
        freightWeightSubtotal
        globalized
        globalizedType
        grisSubtotal
        insuranceAccountableType
        insuranceEnabled
        insuranceId
        insuredValue
        invoicesTotalVolumes
        invoicesValue
        invoicesWeight
        itrSubtotal
        km
        modal
        modalCte
        nfseNumber
        nfseSeries
        otherFees
        paymentAccountableType
        paymentType
        previousDocumentType
        priceTableAccountableType
        productsValue
        realWeight
        redispatchSubtotal
        referenceNumber
        secCatSubtotal
        serviceAt
        serviceDate
        serviceType
        status
        subtotal
        suframaSubtotal
        taxedWeight
        tdeSubtotal
        tollSubtotal
        total
        totalCubicVolume
        trtSubtotal
        type
      }
    }
    pageInfo {
      hasNextPage
      endCursor
    }
  }
}
```

**Variables:**
```json
{
  "params": {
    "serviceAt": "2025-11-02 - 2025-11-03"
  }
}
```

**Validação:**
- Total esperado: **~400 fretes** (do CSV)

---

#### 2️⃣ Query com Relacionamentos (Payer, Sender, Receiver)

**Nome:** `[EXPANDIDA] Fretes + Relacionamentos`

**Query:**
```graphql
query BuscarFretesExpandida($params: FreightInput!, $after: String) {
  freight(params: $params, after: $after, first: 100) {
    totalCount
    edges {
      node {
        id
        referenceNumber
        serviceAt
        total
        
        # Relacionamentos que podem explicar colunas do CSV
        payer {
          id
          name
          document
        }
        sender {
          id
          name
          city {
            name
            state {
              abbreviation
            }
          }
        }
        receiver {
          id
          name
          city {
            name
            state {
              abbreviation
            }
          }
        }
        # Adicionar outros relacionamentos via introspection
      }
    }
    pageInfo {
      hasNextPage
      endCursor
    }
  }
}
```

---

## 🗺️ Mapeamento CSV → GraphQL

### Coletas (22 colunas CSV)

Criar documento: `docs/mapeamento/coletas-csv-graphql.md`

| Campo CSV | Campo GraphQL | Status | Observações |
|-----------|---------------|--------|-------------|
| Coleta | sequenceCode | ✅ | Mapeado |
| Cliente | customer.name | ❓ | Verificar via introspection |
| Solicitante | requester | ✅ | Mapeado |
| Local da Coleta | pickupLocation.name | ❓ | Verificar |
| Cidade | pickupLocation.city.name | ❓ | Verificar |
| ... | ... | ... | ... |

### Fretes (49 colunas CSV)

Criar documento: `docs/mapeamento/fretes-csv-graphql.md`

Mapear campos como:
- **Filial** → ?
- **Pagador** → payer.name?
- **Remetente** → sender.name?
- **Origem** → sender.city.name?
- etc.

---

## ✅ Checklist de Validação

Após executar todas as introspections e queries:

- [ ] Introspection de Pick executada com sucesso
- [ ] Introspection de Freight executada com sucesso
- [ ] Lista COMPLETA de campos disponíveis documentada
- [ ] Mapeamento CSV → GraphQL criado para Coletas
- [ ] Mapeamento CSV → GraphQL criado para Fretes
- [ ] Identificados campos faltantes no código Java
- [ ] Query expandida testada e validada

---

**Próximo Documento:** `04-requisicoes-api-dataexport.md`

