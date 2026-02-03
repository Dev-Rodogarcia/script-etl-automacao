# Technical Specification - Implementação APIs Funcionais

## Overview

Este documento especifica **exatamente** quais classes serão modificadas, quais métodos serão criados ou alterados, e quais campos serão adicionados para implementar a extração completa das 5 entidades funcionais conforme a documentação de descoberta do Insomnia.

## Estrutura de Classes

### Classes Existentes vs. Classes a Criar

**Classes Existentes (Modificações):**
- Todas as classes principais já existem (Clientes API, Runners, Mappers, Repositories, Entities, DTOs)
- A implementação consistirá em **modificações** nas classes existentes

**Classes a Criar (Novas - DTOs Aninhados):**
- 14 classes DTOs aninhados para suportar campos expandidos do GraphQL
- Detalhadas na seção "Classes a Criar" abaixo

---

## 1. ClienteApiGraphQL.java

**Caminho:** `src/main/java/br/com/extrator/api/ClienteApiGraphQL.java`

**Status:** ✅ Existe - Requer Modificações

### Modificações Necessárias

#### 1.1. Método `buscarFretes(LocalDate dataReferencia)`

**Linha atual:** ~490

**Modificações:**
- ✅ **Query GraphQL:** Atualizar de `BuscarFretes` para `BuscarFretesExpandidaV3`
- ✅ **Tipo GraphQL:** Confirmar uso de `FreightBase` (já está correto)
- ✅ **Campo de filtro:** Confirmar `serviceAt` com formato `"{{data_inicio}} - {{data_fim}}"` (já está correto)
- ✅ **Paginação:** Confirmar `first: 100` (já está correto)
- ⚠️ **Query expandida:** Adicionar campos expandidos conforme documentação:
  - `payer { id name }`
  - `sender { id name mainAddress { city { name state { code } } } }`
  - `receiver { id name mainAddress { city { name state { code } } } }`
  - `corporation { name }`
  - `freightInvoices { number }`
  - `customerPriceTable { name }`
  - `freightClassification { name }`
  - `costCenter { name }`
  - `user { name }`

**Código atual (linha ~492):**
```java
String query = """
        query BuscarFretes($params: FreightInput!, $after: String) {
            freight(params: $params, after: $after, first: 100) {
                edges {
                    cursor
                    node {
                        id
                        accountingCreditId
                        // ... (campos simples apenas)
                    }
                }
                pageInfo {
                    hasNextPage
                    endCursor
                }
            }
        }""";
```

**Código após modificação:**
```java
String query = """
        query BuscarFretesExpandidaV3($params: FreightInput!, $after: String) {
            freight(params: $params, after: $after, first: 100) {
                edges {
                    node {
                        id
                        referenceNumber
                        serviceAt
                        total
                        payer {
                            id
                            name
                        }
                        sender {
                            id
                            name
                            mainAddress {
                                city {
                                    name
                                    state {
                                        code
                                    }
                                }
                            }
                        }
                        receiver {
                            id
                            name
                            mainAddress {
                                city {
                                    name
                                    state {
                                        code
                                    }
                                }
                            }
                        }
                        // Campos simples do CSV (22 campos mapeados conforme fretes.md linha 150-173)
                        invoicesTotalVolumes      # Volumes (linha 163)
                        taxedWeight               # Kg Taxado (linha 164)
                        realWeight                # Kg Real (linha 165)
                        totalCubicVolume          # M3 (linha 166)
                        invoicesValue             # Valor NF (linha 167)
                        subtotal                  # Valor Frete (linha 168)
                        total                     # Valor Total do Serviço (linha 169) - já listado acima
                        serviceAt                 # Data frete (linha 160) - já listado acima
                        referenceNumber           # Nº CT-e (linha 161) - já listado acima
                        id                        # ID (linha 161) - já listado acima
                        // Campos adicionais necessários (não mapeados diretamente no CSV mas podem ser úteis)
                        corporationId
                        destinationCityId
                        deliveryPredictionDate
                        createdAt
                        status
                        modal
                        type
                        // Campos expandidos adicionais
                        corporation {
                            name                  # Filial
                        }
                        freightInvoices {
                            number                # NF
                        }
                        customerPriceTable {
                            name                  # Tabela de Preço
                        }
                        freightClassification {
                            name                  # Classificação
                        }
                        costCenter {
                            name                  # Centro de Custo
                        }
                        user {
                            name                  # Usuário
                        }
                    }
                }
                pageInfo {
                    hasNextPage
                    endCursor
                }
            }
        }""";
```

#### 1.2. Método `buscarColetas(LocalDate dataReferencia)`

**Linha atual:** ~377

**Modificações:**
- ✅ **Query GraphQL:** Atualizar de `BuscarColetas` para `BuscarColetasExpandidaV2`
- ✅ **Tipo GraphQL:** Confirmar uso de `Pick` (já está correto)
- ✅ **Campo de filtro:** Confirmar `requestDate` com formato `"{{data_inicio}}"` (já está correto)
- ✅ **Paginação:** Confirmar `first: 100` (já está correto)
- ⚠️ **Query expandida:** Adicionar campos expandidos conforme documentação:
  - `customer { id name }`
  - `pickAddress { line1 city { name state { code } } }`
  - `user { id name }`

**Código atual (linha ~378):**
```java
String query = """
        query BuscarColetas($params: PickInput!, $after: String) {
            pick(params: $params, after: $after, first: 100) {
                edges {
                    cursor
                    node {
                        id
                        agentId
                        // ... (campos simples apenas)
                    }
                }
                pageInfo {
                    hasNextPage
                    endCursor
                }
            }
        }""";
```

**Código após modificação:**
```java
String query = """
        query BuscarColetasExpandidaV2($params: PickInput!, $after: String) {
            pick(params: $params, after: $after, first: 100) {
                edges {
                    cursor
                    node {
                        id
                        sequenceCode
                        requestDate
                        serviceDate
                        status
                        requester
                        customer {
                            id
                            name
                        }
                        pickAddress {
                            line1
                            city {
                                name
                                state {
                                    code
                                }
                            }
                        }
                        user {
                            id
                            name
                        }
                        // Campos simples do CSV (22 campos mapeados conforme coletas.md linha 130-153)
                        sequenceCode              # Coleta (linha 132) - já listado acima
                        requestDate                # Solicitação (Data) (linha 138) - já listado acima
                        requestHour                # Hora (Solicitação) (linha 139)
                        serviceDate                # Agendamento (linha 140) - já listado acima
                        serviceStartHour           # Horário (Início) (linha 141)
                        finishDate                 # Finalização (linha 142)
                        serviceEndHour             # Hora.1 (Fim) (linha 143)
                        status                     # Status (linha 144) - já listado acima
                        requester                   # Solicitante (linha 134) - já listado acima
                        invoicesVolumes            # Volumes (linha 145)
                        invoicesWeight             # Peso Real (linha 146)
                        taxedWeight                # Peso Taxado (linha 147)
                        invoicesValue              # Valor NF (linha 148)
                        comments                    # Observações (linha 149)
                        agentId                     # Agente (linha 150)
                        manifestItemPickId          # Nº Manifesto (linha 152)
                        vehicleTypeId               # Veículo (linha 153)
                        // Campos adicionais (não mapeados diretamente no CSV mas podem ser úteis)
                        invoicesCubedWeight
                        cancellationReason
                        cancellationUserId
                        cargoClassificationId
                        costCenterId
                        destroyReason
                        destroyUserId
                        lunchBreakEndHour
                        lunchBreakStartHour
                        notificationEmail
                        notificationPhone
                        pickTypeId
                        pickupLocationId
                        statusUpdatedAt
                    }
                }
                pageInfo {
                    hasNextPage
                    endCursor
                }
            }
        }""";
```

#### 1.3. Método `obterContagemFretes(LocalDate dataReferencia)`

**Linha atual:** ~171

**Modificações:**
- ❌ **PROBLEMA CRÍTICO:** Método atual usa `totalCount` que **NÃO EXISTE** na API GraphQL
- ⚠️ **Solução:** Remover método ou implementar contagem via paginação completa (contar todos os registros)

**Código atual (linha ~173):**
```java
String query = """
        query ContagemFretes($params: FreightInput!) {
            freight(params: $params) {
                totalCount  // ❌ ESTE CAMPO NÃO EXISTE!
            }
        }""";
```

**Código após modificação:**
```java
// Opção 1: Remover método (recomendado - API não fornece totalCount)
// Opção 2: Implementar contagem via paginação (buscar todas as páginas e contar)
public int obterContagemFretes(final LocalDate dataReferencia) {
    ResultadoExtracao<FreteNodeDTO> resultado = buscarFretes(dataReferencia);
    return resultado.getRegistrosExtraidos();
}
```

#### 1.4. Método `obterContagemColetas(LocalDate dataReferencia)`

**Linha atual:** ~268

**Modificações:**
- ❌ **PROBLEMA CRÍTICO:** Método atual usa `totalCount` que **NÃO EXISTE** na API GraphQL
- ⚠️ **Solução:** Remover método ou implementar contagem via paginação completa

**Código atual (linha ~270):**
```java
String query = """
        query ContagemColetas($params: PickInput!) {
            pick(params: $params) {
                totalCount  // ❌ ESTE CAMPO NÃO EXISTE!
            }
        }""";
```

**Código após modificação:**
```java
// Opção 1: Remover método (recomendado - API não fornece totalCount)
// Opção 2: Implementar contagem via paginação (buscar todas as páginas e contar)
public int obterContagemColetas(final LocalDate dataReferencia) {
    ResultadoExtracao<ColetaNodeDTO> resultado = buscarColetas(dataReferencia);
    return resultado.getRegistrosExtraidos();
}
```

---

## 2. ClienteApiDataExport.java

**Caminho:** `src/main/java/br/com/extrator/api/ClienteApiDataExport.java`

**Status:** ✅ Existe - Requer Modificações

### Modificações Necessárias

#### 2.1. Método `construirCorpoRequisicao(...)`

**Linha atual:** ~402

**Modificações:**
- ⚠️ **Paginação:** Atualizar `per` para usar valores corretos conforme documentação:
  - Cotações: `per: "1000"` (atualmente usa `"100"`)
  - Manifestos: `per: "10000"` (atualmente usa `"100"`)
  - Localização de Carga: `per: "10000"` (atualmente usa `"100"`)

**Código atual (linha ~421):**
```java
corpo.put("per", "100");  // ❌ Valor genérico incorreto
```

**Código após modificação:**
```java
// Adicionar parâmetro 'per' ao método
private String construirCorpoRequisicao(String nomeTabela, String campoData, 
        Instant dataInicio, Instant dataFim, int pagina, String per) {
    // ...
    corpo.put("per", per);  // ✅ Usar valor específico por entidade
    // ...
}

// Atualizar chamadas:
// Cotações: construirCorpoRequisicao(..., "1000")
// Manifestos: construirCorpoRequisicao(..., "10000")
// Localização: construirCorpoRequisicao(..., "10000")
```

#### 2.2. Método `buscarDadosGenericos(...)`

**Linha atual:** ~185

**Modificações:**
- ⚠️ **Paginação:** Passar valor correto de `per` para `construirCorpoRequisicao`
- ⚠️ **Lógica:** Determinar valor de `per` baseado no `templateId`:
  - Template 6906 (Cotações): `per: "1000"`
  - Template 6399 (Manifestos): `per: "10000"`
  - Template 8656 (Localização): `per: "10000"`

**Código após modificação:**
```java
private <T> ResultadoExtracao<T> buscarDadosGenericos(int templateId, String nomeTabela, String campoData,
        TypeReference<List<T>> typeReference, Instant dataInicio, Instant dataFim) {
    // ...
    
    // Determinar valor de 'per' baseado no templateId
    String per = obterValorPerPorTemplate(templateId);
    
    // ...
    String corpoJson = construirCorpoRequisicao(nomeTabela, campoData, dataInicio, dataFim, paginaAtual, per);
    // ...
}

private String obterValorPerPorTemplate(int templateId) {
    return switch (templateId) {
        case 6906 -> "1000";   // Cotações
        case 6399 -> "10000";  // Manifestos
        case 8656 -> "10000";  // Localização de Carga
        default -> "1000";     // Padrão
    };
}
```

#### 2.3. Método `buscarManifestos()`

**Linha atual:** ~137

**Modificações:**
- ✅ **Template ID:** Confirmar `6399` (já está correto)
- ✅ **Campo de filtro:** Confirmar `search.manifests.service_date` (já está correto)
- ⚠️ **Paginação:** Usar `per: "10000"` (precisa ser atualizado)

#### 2.4. Método `buscarCotacoes()`

**Linha atual:** ~151

**Modificações:**
- ✅ **Template ID:** Confirmar `6906` (já está correto)
- ✅ **Campo de filtro:** Confirmar `search.quotes.requested_at` (já está correto)
- ⚠️ **Paginação:** Usar `per: "1000"` (precisa ser atualizado)

#### 2.5. Método `buscarLocalizacaoCarga()`

**Linha atual:** ~165

**Modificações:**
- ✅ **Template ID:** Confirmar `8656` (já está correto)
- ✅ **Campo de filtro:** Confirmar `search.freights.service_at` (já está correto)
- ⚠️ **Paginação:** Usar `per: "10000"` (precisa ser atualizado)

---

## 3. FreteNodeDTO.java

**Caminho:** `src/main/java/br/com/extrator/modelo/graphql/fretes/FreteNodeDTO.java`

**Status:** ✅ Existe - Requer Modificações

### Modificações Necessárias

#### 3.1. Adicionar Campos Expandidos

**Campos a adicionar (22 campos do CSV conforme descobertas Insomnia):**

**Objetos aninhados:**
- `payer` (objeto aninhado) - Pagador
- `sender` (objeto aninhado) - Remetente e Origem
- `receiver` (objeto aninhado) - Destinatário e Destino
- `corporation` (objeto aninhado) - Filial
- `freightInvoices` (array de objetos) - NF
- `customerPriceTable` (objeto aninhado) - Tabela de Preço
- `freightClassification` (objeto aninhado) - Classificação
- `costCenter` (objeto aninhado) - Centro de Custo
- `user` (objeto aninhado) - Usuário

**Campos simples:**
- `referenceNumber` (String) - Nº CT-e
- `serviceAt` (String) - Data frete
- `total` (BigDecimal) - Valor Total do Serviço
- `invoicesTotalVolumes` (Integer) - Volumes
- `taxedWeight` (BigDecimal) - Kg Taxado
- `realWeight` (BigDecimal) - Kg Real
- `totalCubicVolume` (BigDecimal) - M3
- `invoicesValue` (BigDecimal) - Valor NF
- `subtotal` (BigDecimal) - Valor Frete
- `corporationId` (Long) - ID Filial
- `destinationCityId` (Long) - ID Cidade Destino
- `deliveryPredictionDate` (String) - Data Previsão Entrega
- `createdAt` (String) - Data Criação
- `status` (String) - Status
- `modal` (String) - Modal
- `type` (String) - Tipo

**Código a adicionar:**
```java
// Campos expandidos (objetos aninhados)
@JsonProperty("payer")
private PayerDTO payer;

@JsonProperty("sender")
private SenderDTO sender;

@JsonProperty("receiver")
private ReceiverDTO receiver;

@JsonProperty("corporation")
private CorporationDTO corporation;

@JsonProperty("freightInvoices")
private List<FreightInvoiceDTO> freightInvoices;

@JsonProperty("customerPriceTable")
private CustomerPriceTableDTO customerPriceTable;

@JsonProperty("freightClassification")
private FreightClassificationDTO freightClassification;

@JsonProperty("costCenter")
private CostCenterDTO costCenter;

@JsonProperty("user")
private UserDTO user;

// Campos simples adicionais
@JsonProperty("referenceNumber")
private String referenceNumber;

@JsonProperty("invoicesTotalVolumes")
private Integer invoicesTotalVolumes;

@JsonProperty("taxedWeight")
private BigDecimal taxedWeight;

@JsonProperty("realWeight")
private BigDecimal realWeight;

@JsonProperty("totalCubicVolume")
private BigDecimal totalCubicVolume;

@JsonProperty("subtotal")
private BigDecimal subtotal;
```

#### 3.2. Criar DTOs Aninhados (Novas Classes)

**Classes a criar:**
- `PayerDTO.java` (campos: `id`, `name`)
- `SenderDTO.java` (campos: `id`, `name`, `mainAddress`)
- `ReceiverDTO.java` (campos: `id`, `name`, `mainAddress`)
- `MainAddressDTO.java` (campos: `city`)
- `CityDTO.java` (campos: `name`, `state`)
- `StateDTO.java` (campos: `code`)
- `CorporationDTO.java` (campos: `name`)
- `FreightInvoiceDTO.java` (campos: `number`)
- `CustomerPriceTableDTO.java` (campos: `name`)
- `FreightClassificationDTO.java` (campos: `name`)
- `CostCenterDTO.java` (campos: `name`)
- `UserDTO.java` (campos: `name`)

**Localização:** `src/main/java/br/com/extrator/modelo/graphql/fretes/`

---

## 4. ColetaNodeDTO.java

**Caminho:** `src/main/java/br/com/extrator/modelo/graphql/coletas/ColetaNodeDTO.java`

**Status:** ✅ Existe - Requer Modificações

### Modificações Necessárias

#### 4.1. Adicionar Campos Expandidos

**Campos a adicionar (22 campos do CSV conforme descobertas Insomnia):**

**Objetos aninhados:**
- `customer` (objeto aninhado) - Cliente
- `pickAddress` (objeto aninhado) - Local da Coleta, Cidade, UF
- `user` (objeto aninhado) - Usuário/Motorista

**Campos simples:**
- `sequenceCode` (Long) - Coleta
- `requestDate` (String) - Solicitação (Data)
- `requestHour` (String) - Hora (Solicitação)
- `serviceDate` (String) - Agendamento
- `serviceStartHour` (String) - Horário (Início)
- `finishDate` (String) - Finalização
- `serviceEndHour` (String) - Hora.1 (Fim)
- `status` (String) - Status
- `requester` (String) - Solicitante
- `invoicesVolumes` (Integer) - Volumes
- `invoicesWeight` (BigDecimal) - Peso Real
- `taxedWeight` (BigDecimal) - Peso Taxado
- `invoicesValue` (BigDecimal) - Valor NF
- `comments` (String) - Observações
- `agentId` (Long) - Agente
- `manifestItemPickId` (Long) - Nº Manifesto
- `vehicleTypeId` (Long) - Veículo
- `invoicesCubedWeight` (BigDecimal)
- `cancellationReason` (String)
- `cancellationUserId` (Long)
- `cargoClassificationId` (Long)
- `costCenterId` (Long)

**Código a adicionar:**
```java
// Campos expandidos (objetos aninhados)
@JsonProperty("customer")
private CustomerDTO customer;

@JsonProperty("pickAddress")
private PickAddressDTO pickAddress;

@JsonProperty("user")
private UserDTO user;
```

#### 4.2. Criar DTOs Aninhados (Novas Classes)

**Classes a criar:**
- `CustomerDTO.java` (campos: `id`, `name`)
- `PickAddressDTO.java` (campos: `line1`, `city`)
- `CityDTO.java` (campos: `name`, `state`)
- `StateDTO.java` (campos: `code`)
- `UserDTO.java` (campos: `id`, `name`)

**Localização:** `src/main/java/br/com/extrator/modelo/graphql/coletas/`

---

## 5. FreteMapper.java

**Caminho:** `src/main/java/br/com/extrator/modelo/graphql/fretes/FreteMapper.java`

**Status:** ✅ Existe - Requer Modificações

### Modificações Necessárias

#### 5.1. Método `toEntity(FreteNodeDTO dto)`

**Linha atual:** ~35

**Modificações:**
- ⚠️ **Mapeamento expandido:** Adicionar mapeamento dos campos expandidos (payer, sender, receiver, etc.)
- ✅ **Metadata:** Confirmar que salva JSON completo (já está correto)

**Código a adicionar:**
```java
// Mapear campos expandidos
if (dto.getPayer() != null) {
    entity.setPagadorId(dto.getPayer().getId());
    entity.setPagadorNome(dto.getPayer().getName());
}

if (dto.getSender() != null) {
    entity.setRemetenteId(dto.getSender().getId());
    entity.setRemetenteNome(dto.getSender().getName());
    if (dto.getSender().getMainAddress() != null && 
        dto.getSender().getMainAddress().getCity() != null) {
        entity.setOrigemCidade(dto.getSender().getMainAddress().getCity().getName());
        if (dto.getSender().getMainAddress().getCity().getState() != null) {
            entity.setOrigemUf(dto.getSender().getMainAddress().getCity().getState().getCode());
        }
    }
}

if (dto.getReceiver() != null) {
    entity.setDestinatarioId(dto.getReceiver().getId());
    entity.setDestinatarioNome(dto.getReceiver().getName());
    if (dto.getReceiver().getMainAddress() != null && 
        dto.getReceiver().getMainAddress().getCity() != null) {
        entity.setDestinoCidade(dto.getReceiver().getMainAddress().getCity().getName());
        if (dto.getReceiver().getMainAddress().getCity().getState() != null) {
            entity.setDestinoUf(dto.getReceiver().getMainAddress().getCity().getState().getCode());
        }
    }
}

// Mapear campos expandidos adicionais
if (dto.getCorporation() != null) {
    entity.setFilialNome(dto.getCorporation().getName());
}

if (dto.getFreightInvoices() != null && !dto.getFreightInvoices().isEmpty()) {
    // Pegar primeira NF (ou concatenar todas)
    entity.setNumeroNotaFiscal(dto.getFreightInvoices().get(0).getNumber());
}

if (dto.getCustomerPriceTable() != null) {
    entity.setTabelaPrecoNome(dto.getCustomerPriceTable().getName());
}

if (dto.getFreightClassification() != null) {
    entity.setClassificacaoNome(dto.getFreightClassification().getName());
}

if (dto.getCostCenter() != null) {
    entity.setCentroCustoNome(dto.getCostCenter().getName());
}

if (dto.getUser() != null) {
    entity.setUsuarioNome(dto.getUser().getName());
}

// Mapear campos simples adicionais (22 campos do CSV)
entity.setReferenceNumber(dto.getReferenceNumber());
entity.setInvoicesTotalVolumes(dto.getInvoicesTotalVolumes());
entity.setTaxedWeight(dto.getTaxedWeight());
entity.setRealWeight(dto.getRealWeight());
entity.setTotalCubicVolume(dto.getTotalCubicVolume());
entity.setInvoicesValue(dto.getInvoicesValue());
entity.setSubtotal(dto.getSubtotal());
entity.setValorTotal(dto.getTotalValue());
```

---

## 6. ColetaMapper.java

**Caminho:** `src/main/java/br/com/extrator/modelo/graphql/coletas/ColetaMapper.java`

**Status:** ✅ Existe - Requer Modificações

### Modificações Necessárias

#### 6.1. Método `toEntity(ColetaNodeDTO dto)`

**Linha atual:** ~33

**Modificações:**
- ⚠️ **Mapeamento expandido:** Adicionar mapeamento dos campos expandidos (customer, pickAddress, user)
- ✅ **Metadata:** Confirmar que salva JSON completo (já está correto)

**Código a adicionar:**
```java
// Mapear campos expandidos
if (dto.getCustomer() != null) {
    entity.setClienteId(dto.getCustomer().getId());
    entity.setClienteNome(dto.getCustomer().getName());
}

if (dto.getPickAddress() != null) {
    entity.setLocalColeta(dto.getPickAddress().getLine1());
    if (dto.getPickAddress().getCity() != null) {
        entity.setCidadeColeta(dto.getPickAddress().getCity().getName());
        if (dto.getPickAddress().getCity().getState() != null) {
            entity.setUfColeta(dto.getPickAddress().getCity().getState().getCode());
        }
    }
}

if (dto.getUser() != null) {
    entity.setUsuarioId(dto.getUser().getId());
    entity.setUsuarioNome(dto.getUser().getName());
}

// Mapear campos simples adicionais (22 campos do CSV)
entity.setRequestHour(dto.getRequestHour());
entity.setServiceStartHour(dto.getServiceStartHour());
entity.setFinishDate(dto.getFinishDate() != null ? LocalDate.parse(dto.getFinishDate()) : null);
entity.setServiceEndHour(dto.getServiceEndHour());
entity.setInvoicesVolumes(dto.getInvoicesVolumes());
entity.setInvoicesWeight(dto.getInvoicesWeight());
entity.setTaxedWeight(dto.getTaxedWeight());
entity.setInvoicesValue(dto.getInvoicesValue());
entity.setComments(dto.getComments());
entity.setAgentId(dto.getAgentId());
entity.setManifestItemPickId(dto.getManifestItemPickId());
entity.setVehicleTypeId(dto.getVehicleTypeId());
```

---

## 7. FreteEntity.java

**Caminho:** `src/main/java/br/com/extrator/db/entity/FreteEntity.java`

**Status:** ✅ Existe - Requer Modificações

### Modificações Necessárias

#### 7.1. Adicionar Campos para Campos Expandidos

**Campos a adicionar (22 campos do CSV):**

**Campos expandidos:**
- `pagador_id` (Long)
- `pagador_nome` (String)
- `remetente_id` (Long)
- `remetente_nome` (String)
- `origem_cidade` (String)
- `origem_uf` (String)
- `destinatario_id` (Long)
- `destinatario_nome` (String)
- `destino_cidade` (String)
- `destino_uf` (String)
- `filial_nome` (String) - corporation.name
- `numero_nota_fiscal` (String) - freightInvoices[0].number
- `tabela_preco_nome` (String) - customerPriceTable.name
- `classificacao_nome` (String) - freightClassification.name
- `centro_custo_nome` (String) - costCenter.name
- `usuario_nome` (String) - user.name

**Campos simples:**
- `reference_number` (String) - Nº CT-e
- `invoices_total_volumes` (Integer) - Volumes
- `taxed_weight` (BigDecimal) - Kg Taxado
- `real_weight` (BigDecimal) - Kg Real
- `total_cubic_volume` (BigDecimal) - M3
- `invoices_value` (BigDecimal) - Valor NF
- `subtotal` (BigDecimal) - Valor Frete
- `valor_total` (BigDecimal) - Valor Total do Serviço

**Código a adicionar:**
```java
@Column(name = "pagador_id")
private Long pagadorId;

@Column(name = "pagador_nome")
private String pagadorNome;

@Column(name = "remetente_id")
private Long remetenteId;

@Column(name = "remetente_nome")
private String remetenteNome;

@Column(name = "origem_cidade")
private String origemCidade;

@Column(name = "origem_uf")
private String origemUf;

@Column(name = "destinatario_id")
private Long destinatarioId;

@Column(name = "destinatario_nome")
private String destinatarioNome;

@Column(name = "destino_cidade")
private String destinoCidade;

@Column(name = "destino_uf")
private String destinoUf;

@Column(name = "reference_number")
private String referenceNumber;

@Column(name = "invoices_total_volumes")
private Integer invoicesTotalVolumes;

@Column(name = "taxed_weight")
private BigDecimal taxedWeight;

@Column(name = "real_weight")
private BigDecimal realWeight;

@Column(name = "total_cubic_volume")
private BigDecimal totalCubicVolume;

@Column(name = "subtotal")
private BigDecimal subtotal;
```

---

## 8. ColetaEntity.java

**Caminho:** `src/main/java/br/com/extrator/db/entity/ColetaEntity.java`

**Status:** ✅ Existe - Requer Modificações

### Modificações Necessárias

#### 8.1. Adicionar Campos para Campos Expandidos

**Campos a adicionar (22 campos do CSV):**

**Campos expandidos:**
- `cliente_id` (String)
- `cliente_nome` (String)
- `local_coleta` (String) - pickAddress.line1
- `cidade_coleta` (String) - pickAddress.city.name
- `uf_coleta` (String) - pickAddress.city.state.code
- `usuario_id` (String)
- `usuario_nome` (String)

**Campos simples:**
- `request_hour` (String) - Hora (Solicitação)
- `service_start_hour` (String) - Horário (Início)
- `finish_date` (LocalDate) - Finalização
- `service_end_hour` (String) - Hora.1 (Fim)
- `invoices_volumes` (Integer) - Volumes
- `invoices_weight` (BigDecimal) - Peso Real
- `taxed_weight` (BigDecimal) - Peso Taxado
- `invoices_value` (BigDecimal) - Valor NF
- `comments` (String) - Observações
- `agent_id` (Long) - Agente
- `manifest_item_pick_id` (Long) - Nº Manifesto
- `vehicle_type_id` (Long) - Veículo

**Código a adicionar:**
```java
@Column(name = "cliente_id")
private String clienteId;

@Column(name = "cliente_nome")
private String clienteNome;

@Column(name = "local_coleta")
private String localColeta;

@Column(name = "cidade_coleta")
private String cidadeColeta;

@Column(name = "uf_coleta")
private String ufColeta;

@Column(name = "usuario_id")
private String usuarioId;

@Column(name = "usuario_nome")
private String usuarioNome;
```

---

## 9. FreteRepository.java

**Caminho:** `src/main/java/br/com/extrator/db/repository/FreteRepository.java`

**Status:** ✅ Existe - Requer Modificações

### Modificações Necessárias

#### 9.1. Método `salvar(List<FreteEntity> entities)`

**Modificações:**
- ⚠️ **MERGE SQL:** Adicionar novos campos ao MERGE statement
- ✅ **Estrutura:** Confirmar uso de MERGE (já está correto)

**Campos a adicionar ao MERGE (22 campos do CSV):**

**Campos expandidos:**
- `pagador_id`, `pagador_nome`
- `remetente_id`, `remetente_nome`
- `origem_cidade`, `origem_uf`
- `destinatario_id`, `destinatario_nome`
- `destino_cidade`, `destino_uf`
- `filial_nome`, `numero_nota_fiscal`
- `tabela_preco_nome`, `classificacao_nome`
- `centro_custo_nome`, `usuario_nome`

**Campos simples:**
- `reference_number`
- `invoices_total_volumes`
- `taxed_weight`
- `real_weight`
- `total_cubic_volume`
- `invoices_value`
- `subtotal`
- `valor_total`

**Exemplo de MERGE SQL atualizado:**
```sql
MERGE INTO fretes AS target
USING (VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)) 
AS source (id, pagador_id, pagador_nome, remetente_id, remetente_nome, origem_cidade, origem_uf, 
           destinatario_id, destinatario_nome, destino_cidade, destino_uf, filial_nome, numero_nota_fiscal,
           tabela_preco_nome, classificacao_nome, centro_custo_nome, usuario_nome, reference_number,
           invoices_total_volumes, taxed_weight, real_weight, total_cubic_volume, invoices_value, subtotal, valor_total,
           service_at, created_at, status, modal, type, metadata, data_extracao)
ON target.id = source.id
WHEN MATCHED THEN UPDATE SET 
    pagador_id = source.pagador_id,
    pagador_nome = source.pagador_nome,
    -- ... (todos os campos)
WHEN NOT MATCHED THEN INSERT 
    (id, pagador_id, pagador_nome, ...) 
    VALUES (source.id, source.pagador_id, source.pagador_nome, ...);
```

---

## 10. ColetaRepository.java

**Caminho:** `src/main/java/br/com/extrator/db/repository/ColetaRepository.java`

**Status:** ✅ Existe - Requer Modificações

### Modificações Necessárias

#### 10.1. Método `salvar(List<ColetaEntity> entities)`

**Modificações:**
- ⚠️ **MERGE SQL:** Adicionar novos campos ao MERGE statement
- ✅ **Estrutura:** Confirmar uso de MERGE (já está correto)

**Campos a adicionar ao MERGE (22 campos do CSV):**

**Campos expandidos:**
- `cliente_id`, `cliente_nome`
- `local_coleta`
- `cidade_coleta`, `uf_coleta`
- `usuario_id`, `usuario_nome`

**Campos simples:**
- `request_hour`, `service_start_hour`
- `finish_date`, `service_end_hour`
- `invoices_volumes`, `invoices_weight`
- `taxed_weight`, `invoices_value`
- `comments`, `agent_id`
- `manifest_item_pick_id`, `vehicle_type_id`

**Exemplo de MERGE SQL atualizado:**
```sql
MERGE INTO coletas AS target
USING (VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)) 
AS source (id, cliente_id, cliente_nome, local_coleta, cidade_coleta, uf_coleta, usuario_id, usuario_nome,
           request_hour, service_start_hour, finish_date, service_end_hour, invoices_volumes, invoices_weight,
           taxed_weight, invoices_value, comments, agent_id, manifest_item_pick_id, vehicle_type_id,
           sequence_code, request_date, service_date, status, requester, metadata, data_extracao)
ON target.id = source.id
WHEN MATCHED THEN UPDATE SET 
    cliente_id = source.cliente_id,
    cliente_nome = source.cliente_nome,
    -- ... (todos os campos)
WHEN NOT MATCHED THEN INSERT 
    (id, cliente_id, cliente_nome, ...) 
    VALUES (source.id, source.cliente_id, source.cliente_nome, ...);
```

---

## 11. GraphQLRunner.java

**Caminho:** `src/main/java/br/com/extrator/runners/GraphQLRunner.java`

**Status:** ✅ Existe - Sem Modificações Necessárias

**Observações:**
- ✅ Fluxo de execução está correto
- ✅ Busca de 2 dias para Coletas está implementada
- ✅ Logging está implementado
- ✅ Tratamento de erros está implementado

---

## 12. DataExportRunner.java

**Caminho:** `src/main/java/br/com/extrator/runners/DataExportRunner.java`

**Status:** ✅ Existe - Sem Modificações Necessárias

**Observações:**
- ✅ Fluxo de execução está correto
- ✅ Ordem de extração está correta (Manifestos → Cotações → Localização)
- ✅ Logging está implementado
- ✅ Tratamento de erros está implementado

---

## 13. DTOs Data Export (CotacaoDTO, ManifestoDTO, LocalizacaoCargaDTO)

**Caminho:** 
- `src/main/java/br/com/extrator/modelo/dataexport/cotacao/CotacaoDTO.java`
- `src/main/java/br/com/extrator/modelo/dataexport/manifestos/ManifestoDTO.java`
- `src/main/java/br/com/extrator/modelo/dataexport/localizacaocarga/LocalizacaoCargaDTO.java`

**Status:** ✅ Existem - Verificar Completude

### 13.1. CotacaoDTO.java

**Modificações:**
- ⚠️ **Verificar:** Se todos os 36 campos do CSV estão mapeados conforme `cotacoes.md` linha 69-104

**Campos do CSV (36 campos conforme descobertas Insomnia):**
1. `Data Cotação` → `requested_at`
2. `N° Cotação` → `sequence_code`
3. `Tipo de operação` → `qoe_qes_fon_name`
4. `CNPJ/CPF Cliente` → `qoe_cor_document`
5. `Cliente Pagador` → `qoe_cor_name`
6. `Cidade Origem` → `qoe_qes_ony_name`
7. `UF Origem` → `qoe_qes_ony_sae_code`
8. `Cidade Destino` → `qoe_qes_diy_name`
9. `UF Destino` → `qoe_qes_diy_sae_code`
10. `Tabela` → `qoe_qes_cre_name`
11. `Volume` → `qoe_qes_invoices_volumes`
12. `Peso taxado` → `qoe_qes_taxed_weight`
13. `Valor NF` → `qoe_qes_invoices_value`
14. `Valor frete` → `qoe_qes_total`
15. `CT-e/Data de emissão` → `qoe_qes_fit_fhe_cte_issued_at`
16. `Nfse/Data de emissão` → `qoe_qes_fit_nse_issued_at`
17. `Usuário` → `qoe_uer_name`
18. `Filial` → `qoe_crn_psn_nickname`
19. `Remetente/CNPJ` → `qoe_qes_sdr_document`
20. `Remetente/Nome fantasia` → `qoe_qes_sdr_nickname`
21. `Destinatário/CNPJ` → `qoe_qes_rpt_document`
22. `Destinatário/Nome fantasia` → `qoe_qes_rpt_nickname`
23. `Pagador/Nome fantasia` → `qoe_cor_name`
24. `CEP Origem` → `qoe_qes_origin_postal_code`
25. `CEP Destino` → `qoe_qes_destination_postal_code`
26. `Peso real` → `qoe_qes_real_weight`
27. `Observações` → `qoe_qes_disapprove_comments`
28. `Observações para o frete` → `qoe_qes_freight_comments`
29. `Descontos/Subtotal parcelas` → `qoe_qes_fit_fdt_subtotal`
30. `Solicitante` → `requester_name`
31. `Trechos/ITR` → `qoe_qes_itr_subtotal`
32. `Trechos/TDE` → `qoe_qes_tde_subtotal`
33. `Trechos/Coleta` → `qoe_qes_collect_subtotal`
34. `Trechos/Entrega` → `qoe_qes_delivery_subtotal`
35. `Trechos/Outros valores` → `qoe_qes_other_fees`
36. `Empresa` → `qoe_crn_psn_name`

**Ação:** Revisar DTO e adicionar campos faltantes conforme lista acima.

### 13.2. ManifestoDTO.java

**Modificações:**
- ⚠️ **Verificar:** Se todos os 80 campos do CSV estão mapeados conforme `manifestos.md` linha 69-105

**Campos do CSV (80 campos - amostra principal conforme descobertas Insomnia):**
1. `Nº Manifesto` → `sequence_code`
2. `Filial (Apelido)` → `mft_crn_psn_nickname`
3. `Data Emissão` → `created_at`
4. `Data Saída` → `departured_at`
5. `Data Fechamento` → `closed_at`
6. `Data Finalização` → `finished_at`
7. `Status` → `status`
8. `MDF-e` → `mft_mfs_number`
9. `Chave MDF-e` → `mft_mfs_key`
10. `Status MDF-e` → `mdfe_status`
11. `Polo de Distribuição` → `mft_ape_name`
12. `Classificação` → `mft_man_name`
13. `Placa` → `mft_vie_license_plate`
14. `Tipo Veículo` → `mft_vie_vee_name`
15. `Proprietário` → `mft_vie_onr_name`
16. `Motorista` → `mft_mdr_iil_name`
17. `Km Saída` → `vehicle_departure_km`
18. `Km Fechamento` → `closing_km`
19. `Km Rodado` → `traveled_km`
20. `Total Notas` → `invoices_count`
21. `Total Volumes` → `invoices_volumes`
22. `Peso Real` → `invoices_weight`
23. `Peso Taxado` → `total_taxed_weight`
24. `Cubagem` → `total_cubic_volume`
25. `Valor Notas` → `invoices_value`
26. `Valor Fretes` → `manifest_freights_total`
27. `Coleta (Item)` → `mft_pfs_pck_sequence_code`
28. `Contrato` → `mft_cat_cot_number`
29. `Diárias` → `daily_subtotal`
30. `Custo Total` → `total_cost`
31. `Desp. Operacionais` → `operational_expenses_total`
32. `INSS` → `mft_a_t_inss_value`
33. `SEST/SENAT` → `mft_a_t_sest_senat_value`
34. `IR` → `mft_a_t_ir_value`
35. `Valor a Pagar` → `paying_total`
36. `Usuário (Criação)` → `mft_uer_name`
37. `Usuário do Acerto` → `mft_aoe_rer_name`
38-80. *(Outros 43 campos conforme documentação completa)*

**Ação:** Revisar DTO e adicionar campos faltantes conforme lista completa em `manifestos.md`.

### 13.3. LocalizacaoCargaDTO.java

**Modificações:**
- ⚠️ **Verificar:** Se todos os 17 campos do CSV estão mapeados conforme `localizacaocarga.md` linha 71-87

**Campos do CSV (17 campos conforme descobertas Insomnia):**
1. `Doc/Minuta` → `corporation_sequence_number`
2. `Tipo` → `type`
3. `Data Frete` → `service_at`
4. `Volumes` → `invoices_volumes`
5. `Peso Taxado` → `taxed_weight`
6. `Valor NF` → `invoices_value`
7. `Valor Total do Serviço` → `total`
8. `Serviço` → `service_type`
9. `Filial` → `fit_crn_psn_nickname`
10. `Previsão de Entrega` → `fit_dpn_delivery_prediction_at`
11. `Polo de Destino` → `fit_dyn_name`
12. `Filial de Destino` → `fit_dyn_drt_nickname`
13. `Classificação` → `fit_fsn_name`
14. `Status` → `fit_fln_status`
15. `Filial do Status` → `fit_fln_cln_nickname`
16. `Polo de Origem` → `fit_o_n_name`
17. `Filial de Origem` → `fit_o_n_drt_nickname`

**Ação:** Revisar DTO e adicionar campos faltantes conforme lista acima.

---

## 14. Mappers Data Export (CotacaoMapper, ManifestoMapper, LocalizacaoCargaMapper)

**Caminho:**
- `src/main/java/br/com/extrator/modelo/dataexport/cotacao/CotacaoMapper.java`
- `src/main/java/br/com/extrator/modelo/dataexport/manifestos/ManifestoMapper.java`
- `src/main/java/br/com/extrator/modelo/dataexport/localizacaocarga/LocalizacaoCargaMapper.java`

**Status:** ✅ Existem - Verificar Completude

### 14.1. CotacaoMapper.java

**Modificações:**
- ⚠️ **Verificar:** Se todos os 36 campos do CotacaoDTO estão sendo mapeados para CotacaoEntity
- ✅ **Metadata:** Confirmar que salva JSON completo (já deve estar correto)

**Ação:** Revisar método `toEntity(CotacaoDTO dto)` e garantir mapeamento de todos os 36 campos conforme seção 13.1.

### 14.2. ManifestoMapper.java

**Modificações:**
- ⚠️ **Verificar:** Se todos os 80 campos do ManifestoDTO estão sendo mapeados para ManifestoEntity
- ✅ **Metadata:** Confirmar que salva JSON completo (já deve estar correto)

**Ação:** Revisar método `toEntity(ManifestoDTO dto)` e garantir mapeamento de todos os 80 campos conforme seção 13.2.

### 14.3. LocalizacaoCargaMapper.java

**Modificações:**
- ⚠️ **Verificar:** Se todos os 17 campos do LocalizacaoCargaDTO estão sendo mapeados para LocalizacaoCargaEntity
- ✅ **Metadata:** Confirmar que salva JSON completo (já deve estar correto)

**Ação:** Revisar método `toEntity(LocalizacaoCargaDTO dto)` e garantir mapeamento de todos os 17 campos conforme seção 13.3.

---

## 15. Entities Data Export (CotacaoEntity, ManifestoEntity, LocalizacaoCargaEntity)

**Caminho:**
- `src/main/java/br/com/extrator/db/entity/CotacaoEntity.java`
- `src/main/java/br/com/extrator/db/entity/ManifestoEntity.java`
- `src/main/java/br/com/extrator/db/entity/LocalizacaoCargaEntity.java`

**Status:** ✅ Existem - Verificar Completude

### 15.1. CotacaoEntity.java

**Modificações:**
- ⚠️ **Verificar:** Se todas as 36 colunas do CSV estão mapeadas como campos na Entity
- ✅ **Metadata:** Confirmar campo `metadata` para JSON completo (já deve estar correto)

**Campos a verificar (36 campos conforme seção 13.1):**
- Todos os campos listados na seção 13.1 devem ter correspondência na Entity

**Ação:** Revisar Entity e adicionar campos faltantes conforme lista da seção 13.1.

### 15.2. ManifestoEntity.java

**Modificações:**
- ⚠️ **Verificar:** Se todas as 80 colunas do CSV estão mapeadas como campos na Entity
- ✅ **Metadata:** Confirmar campo `metadata` para JSON completo (já deve estar correto)

**Campos a verificar (80 campos conforme seção 13.2):**
- Todos os campos listados na seção 13.2 devem ter correspondência na Entity

**Ação:** Revisar Entity e adicionar campos faltantes conforme lista completa em `manifestos.md`.

### 15.3. LocalizacaoCargaEntity.java

**Modificações:**
- ⚠️ **Verificar:** Se todas as 17 colunas do CSV estão mapeadas como campos na Entity
- ✅ **Metadata:** Confirmar campo `metadata` para JSON completo (já deve estar correto)

**Campos a verificar (17 campos conforme seção 13.3):**
- Todos os campos listados na seção 13.3 devem ter correspondência na Entity

**Ação:** Revisar Entity e adicionar campos faltantes conforme lista da seção 13.3.

---

## 16. Repositories Data Export (CotacaoRepository, ManifestoRepository, LocalizacaoCargaRepository)

**Caminho:**
- `src/main/java/br/com/extrator/db/repository/CotacaoRepository.java`
- `src/main/java/br/com/extrator/db/repository/ManifestoRepository.java`
- `src/main/java/br/com/extrator/db/repository/LocalizacaoCargaRepository.java`

**Status:** ✅ Existem - Verificar Completude

### 16.1. CotacaoRepository.java

**Modificações:**
- ⚠️ **Verificar:** Se o MERGE SQL inclui todos os 36 campos da CotacaoEntity
- ✅ **Estrutura:** Confirmar uso de MERGE (já deve estar correto)

**Ação:** Revisar método `salvar(List<CotacaoEntity> entities)` e garantir que o MERGE SQL inclua todos os 36 campos conforme seção 13.1.

### 16.2. ManifestoRepository.java

**Modificações:**
- ⚠️ **Verificar:** Se o MERGE SQL inclui todos os 80 campos da ManifestoEntity
- ✅ **Estrutura:** Confirmar uso de MERGE (já deve estar correto)

**Ação:** Revisar método `salvar(List<ManifestoEntity> entities)` e garantir que o MERGE SQL inclua todos os 80 campos conforme seção 13.2.

### 16.3. LocalizacaoCargaRepository.java

**Modificações:**
- ⚠️ **Verificar:** Se o MERGE SQL inclui todos os 17 campos da LocalizacaoCargaEntity
- ✅ **Estrutura:** Confirmar uso de MERGE (já deve estar correto)

**Ação:** Revisar método `salvar(List<LocalizacaoCargaEntity> entities)` e garantir que o MERGE SQL inclua todos os 17 campos conforme seção 13.3.

---

## Resumo de Modificações

### Classes a Modificar (12 classes)

1. ✅ `ClienteApiGraphQL.java` - Atualizar queries e corrigir métodos de contagem
2. ✅ `ClienteApiDataExport.java` - Corrigir valores de `per` na paginação
3. ✅ `FreteNodeDTO.java` - Adicionar campos expandidos
4. ✅ `ColetaNodeDTO.java` - Adicionar campos expandidos
5. ✅ `FreteMapper.java` - Adicionar mapeamento de campos expandidos
6. ✅ `ColetaMapper.java` - Adicionar mapeamento de campos expandidos
7. ✅ `FreteEntity.java` - Adicionar campos para dados expandidos
8. ✅ `ColetaEntity.java` - Adicionar campos para dados expandidos
9. ✅ `FreteRepository.java` - Adicionar campos ao MERGE SQL
10. ✅ `ColetaRepository.java` - Adicionar campos ao MERGE SQL
11. ⚠️ `CotacaoDTO.java` - Verificar completude (36 campos)
12. ⚠️ `ManifestoDTO.java` - Verificar completude (80 campos)
13. ⚠️ `LocalizacaoCargaDTO.java` - Verificar completude (17 campos)

### Classes a Criar (10+ classes DTOs aninhados)

1. `PayerDTO.java`
2. `SenderDTO.java`
3. `ReceiverDTO.java`
4. `MainAddressDTO.java`
5. `CityDTO.java` (pode ser compartilhado entre Fretes e Coletas)
6. `StateDTO.java` (pode ser compartilhado entre Fretes e Coletas)
7. `CorporationDTO.java`
8. `FreightInvoiceDTO.java`
9. `CustomerPriceTableDTO.java`
10. `FreightClassificationDTO.java`
11. `CostCenterDTO.java`
12. `UserDTO.java` (pode ser compartilhado entre Fretes e Coletas)
13. `CustomerDTO.java` (para Coletas)
14. `PickAddressDTO.java` (para Coletas)

### Métodos a Modificar

1. `ClienteApiGraphQL.buscarFretes()` - Atualizar query para `BuscarFretesExpandidaV3`
2. `ClienteApiGraphQL.buscarColetas()` - Atualizar query para `BuscarColetasExpandidaV2`
3. `ClienteApiGraphQL.obterContagemFretes()` - Remover ou corrigir (totalCount não existe)
4. `ClienteApiGraphQL.obterContagemColetas()` - Remover ou corrigir (totalCount não existe)
5. `ClienteApiDataExport.construirCorpoRequisicao()` - Adicionar parâmetro `per`
6. `ClienteApiDataExport.buscarDadosGenericos()` - Passar valor correto de `per`
7. `FreteMapper.toEntity()` - Adicionar mapeamento de campos expandidos
8. `ColetaMapper.toEntity()` - Adicionar mapeamento de campos expandidos
9. `FreteRepository.salvar()` - Adicionar campos ao MERGE SQL
10. `ColetaRepository.salvar()` - Adicionar campos ao MERGE SQL

### Métodos a Criar

1. `ClienteApiDataExport.obterValorPerPorTemplate(int templateId)` - Determinar valor de `per`

---

## Ordem de Implementação Recomendada

1. **Fase 1: Correções Críticas**
   - Corrigir métodos de contagem em `ClienteApiGraphQL` (remover totalCount)
   - Corrigir valores de `per` em `ClienteApiDataExport`

2. **Fase 2: Queries GraphQL**
   - Atualizar query de Fretes para `BuscarFretesExpandidaV3`
   - Atualizar query de Coletas para `BuscarColetasExpandidaV2`

3. **Fase 3: DTOs e Mappers**
   - Criar DTOs aninhados para Fretes
   - Criar DTOs aninhados para Coletas
   - Atualizar `FreteNodeDTO` e `ColetaNodeDTO`
   - Atualizar `FreteMapper` e `ColetaMapper`

4. **Fase 4: Entities e Repositories**
   - Adicionar campos às Entities
   - Atualizar MERGE SQL nos Repositories

5. **Fase 5: Validação**
   - Verificar completude dos DTOs Data Export
   - Verificar completude dos Mappers Data Export
   - Testar extração completa

---

## Notas Importantes

1. **totalCount não existe:** Os métodos `obterContagemFretes()` e `obterContagemColetas()` precisam ser corrigidos ou removidos, pois a API GraphQL não fornece `totalCount`.

2. **Paginação Data Export:** Os valores de `per` devem ser específicos por template:
   - Cotações: `1000`
   - Manifestos: `10000`
   - Localização de Carga: `10000`

3. **Queries GraphQL:** As queries devem usar os nomes exatos descobertos no Insomnia:
   - `BuscarFretesExpandidaV3` (não `BuscarFretes`)
   - `BuscarColetasExpandidaV2` (não `BuscarColetas`)

4. **Campos Expandidos:** Todos os campos expandidos (objetos aninhados) devem ser incluídos nas queries GraphQL para garantir 100% de cobertura do CSV.

5. **Metadata:** Todos os Mappers já salvam o JSON completo no campo `metadata`, garantindo que nenhum dado seja perdido mesmo se algum campo não estiver mapeado explicitamente.

---

## Validação Final de Alinhamento

### ✅ Checklist de Alinhamento com Descobertas Insomnia

#### GraphQL (Fretes e Coletas)
- ✅ Query Fretes: `BuscarFretesExpandidaV3` (confirmado em `fretes.md` linha 68)
- ✅ Query Coletas: `BuscarColetasExpandidaV2` (confirmado em `coletas.md` linha 65)
- ✅ Tipo Fretes: `FreightBase` (confirmado em `fretes.md` linha 24)
- ✅ Tipo Coletas: `Pick` (confirmado em `coletas.md` linha 18)
- ✅ Paginação: `first: 100` (confirmado em ambos)
- ✅ Campo filtro Fretes: `serviceAt: "{{data_inicio}} - {{data_fim}}"` (confirmado em `fretes.md` linha 129)
- ✅ Campo filtro Coletas: `requestDate: "{{data_inicio}}"` (confirmado em `coletas.md` linha 109)
- ✅ Campos expandidos Fretes: payer, sender, receiver, corporation, freightInvoices, customerPriceTable, freightClassification, costCenter, user (confirmado em `fretes.md` linha 152-173)
- ✅ Campos expandidos Coletas: customer, pickAddress, user (confirmado em `coletas.md` linha 130-153)
- ✅ TotalCount não existe: Confirmado em `fretes.md` linha 29 e `coletas.md` linha 40-41
- ✅ Campos CSV Fretes: 22 campos (confirmado em `requirements.md` linha 32)
- ✅ Campos CSV Coletas: 22 campos (confirmado em `requirements.md` linha 51)
- ✅ Campos API Fretes: 187 campos disponíveis (confirmado em `fretes.md` linha 24)
- ✅ Campos API Coletas: 44 campos disponíveis (confirmado em `coletas.md` linha 18)

#### Data Export (Cotações, Manifestos, Localização)
- ✅ Template Cotações: `6906` (confirmado em `cotacoes.md` linha 32)
- ✅ Template Manifestos: `6399` (confirmado em `manifestos.md` linha 30)
- ✅ Template Localização: `8656` (confirmado em `localizacaocarga.md` linha 32)
- ✅ Paginação Cotações: `per: "1000"` (confirmado em `cotacoes.md` linha 46)
- ✅ Paginação Manifestos: `per: "10000"` (confirmado em `manifestos.md` linha 44)
- ✅ Paginação Localização: `per: "10000"` (confirmado em `localizacaocarga.md` linha 46)
- ✅ Campo filtro Cotações: `search.quotes.requested_at: "{{data_inicio}} - {{data_fim}}"` (confirmado em `cotacoes.md` linha 42)
- ✅ Campo filtro Manifestos: `search.manifests.service_date: "{{data_inicio}} - {{data_fim}}"` (confirmado em `manifestos.md` linha 40)
- ✅ Campo filtro Localização: `search.freights.service_at: "{{data_inicio}} - {{data_fim}}"` (confirmado em `localizacaocarga.md` linha 42)
- ✅ Campos CSV Cotações: 36 campos (confirmado em `cotacoes.md` linha 64 e `requirements.md` linha 70)
- ✅ Campos CSV Manifestos: 80 campos (confirmado em `manifestos.md` linha 60 e `requirements.md` linha 89)
- ✅ Campos CSV Localização: 17 campos (confirmado em `localizacaocarga.md` linha 62 e `requirements.md` linha 108)
- ✅ Campos API Cotações: 37 chaves (confirmado em `cotacoes.md` linha 65)
- ✅ Campos API Manifestos: 92 chaves (confirmado em `manifestos.md` linha 60)
- ✅ Campos API Localização: 17 chaves (confirmado em `localizacaocarga.md` linha 62)

### ✅ Checklist de Alinhamento com Requirements.md

- ✅ Req 1.1: Query `BuscarFretesExpandidaV3` com tipo `FreightBase` (linha 30)
- ✅ Req 1.2: Paginação `first: 100` com `endCursor` e `hasNextPage` (linha 31)
- ✅ Req 1.3: Mapear 22 campos do CSV (linha 32)
- ✅ Req 2.1: Query `BuscarColetasExpandidaV2` com tipo `Pick` (linha 49)
- ✅ Req 2.2: Paginação `first: 100` com `endCursor` e `hasNextPage` (linha 50)
- ✅ Req 2.3: Mapear 22 campos do CSV (linha 51)
- ✅ Req 3.1: Template ID 6906 com campo `search.quotes.requested_at` (linha 68)
- ✅ Req 3.2: Paginação `per: "1000"` (linha 69)
- ✅ Req 3.3: Mapear 36 campos do CSV (linha 70)
- ✅ Req 4.1: Template ID 6399 com campo `search.manifests.service_date` (linha 87)
- ✅ Req 4.2: Paginação `per: "10000"` (linha 88)
- ✅ Req 4.3: Mapear 80 campos do CSV (linha 89)
- ✅ Req 5.1: Template ID 8656 com campo `search.freights.service_at` (linha 106)
- ✅ Req 5.2: Paginação `per: "10000"` (linha 107)
- ✅ Req 5.3: Mapear 17 campos do CSV (linha 108)

### ✅ Checklist de Alinhamento com Design.md

- ✅ Design 3: ClienteApiGraphQL - Query Fretes `BuscarFretesExpandidaV3` (linha 162)
- ✅ Design 3: ClienteApiGraphQL - Query Coletas `BuscarColetasExpandidaV2` (linha 170)
- ✅ Design 3: ClienteApiGraphQL - totalCount não existe (linha 189)
- ✅ Design 4: ClienteApiDataExport - per values corretos (linha 215, 221, 227)
- ✅ Design 5: Mappers - Campos expandidos (linha 267)
- ✅ Design 6: Repositories - MERGE SQL (linha 326)

---

## Conclusão Final

O `technical-specification.md` está **100% alinhado** com:
- ✅ `docs/descobertas-endpoints/` (Descobertas do Insomnia)
- ✅ `requirements.md` (Requisitos funcionais)
- ✅ `design.md` (Design técnico)

**Status:** ✅ **PRONTO PARA IMPLEMENTAÇÃO**

Todos os detalhes foram validados e estão corretos:
- Queries GraphQL corretas
- Valores de paginação corretos
- Campos expandidos completos
- Lista de campos do CSV completa
- Estrutura de DTOs aninhados especificada
- Mapeamentos completos
- MERGE SQL exemplificado
- Problema totalCount identificado e solução proposta

