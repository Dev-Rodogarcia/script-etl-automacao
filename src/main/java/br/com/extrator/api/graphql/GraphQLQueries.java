package br.com.extrator.api.graphql;

/**
 * Classe que centraliza todas as queries GraphQL utilizadas no sistema.
 * Isso evita duplicação e facilita manutenção.
 */
public final class GraphQLQueries {
    
    private GraphQLQueries() {
        // Construtor privado para classe utilitária
    }
    
    /**
     * Query para buscar Coletas (Pick)
     * Tipo GraphQL: Pick
     * Campo de filtro: requestDate (aceita apenas uma data específica)
     */
    public static final String QUERY_COLETAS = """
            query BuscarColetasExpandidaV2($params: PickInput!, $after: String) {
              pick(params: $params, after: $after, first: 100) {
                edges {
                  cursor
                  node {
                    id
                    status
                    requestDate
                    serviceDate
                    sequenceCode
                    requestHour
                    serviceStartHour
                    finishDate
                    serviceEndHour
                    requester
                    corporation {
                      id
                      person { nickname cnpj }
                    }
                    customer { id name cnpj }
                    pickAddress {
                      line1
                      line2
                      number
                      neighborhood
                      postalCode
                      city { name state { code } }
                    }
                    invoicesValue
                    invoicesWeight
                    invoicesVolumes
                    user { id name }
                    comments
                    agentId
                    manifestItemPickId
                    vehicleTypeId
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
                pageInfo { hasNextPage endCursor }
              }
            }""";
    
    /**
     * Query para buscar Fretes (Freight)
     * Tipo GraphQL: FreightBase
     * Campo de filtro: serviceAt (aceita intervalo "data_inicio - data_fim")
     */
    public static final String QUERY_FRETES = """
            query BuscarFretes_Master_V8($params: FreightInput!, $after: String) {
              freight(params: $params, after: $after, first: 100) {
                edges {
                  node {
                    id
                    accountingCreditId
                    accountingCreditInstallmentId
                    referenceNumber
                    serviceAt
                    createdAt
                    cte { id key number series issuedAt createdAt emissionType }
                    total
                    subtotal
                    invoicesValue
                    invoicesWeight
                    taxedWeight
                    realWeight
                    cubagesCubedWeight
                    totalCubicVolume
                    invoicesTotalVolumes
                    freightInvoices { invoice { number series key value weight } }
                    sender { id name cnpj cpf inscricaoEstadual mainAddress { city { name state { code } } } }
                    receiver { id name cnpj cpf inscricaoEstadual mainAddress { city { name state { code } } } }
                    payer { id name cnpj cpf }
                    modal
                    modalCte
                    status
                    type
                    serviceDate
                    serviceType
                    deliveryPredictionDate
                    corporation { id nickname cnpj }
                    customerPriceTable { name }
                    freightClassification { name }
                    costCenter { name }
                    originCity { name state { code } }
                    destinationCity { name state { code } }
                    destinationCityId
                    corporationId
                    freightWeightSubtotal
                    globalized
                    globalizedType
                    grisSubtotal
                    adValoremSubtotal
                    insuranceAccountableType
                    insuranceEnabled
                    insuranceId
                    insuredValue
                    itrSubtotal
                    tollSubtotal
                    km
                    nfseNumber
                    nfseSeries
                    otherFees
                    paymentAccountableType
                    paymentType
                    previousDocumentType
                    priceTableAccountableType
                    productsValue
                    redispatchSubtotal
                    secCatSubtotal
                    suframaSubtotal
                    tdeSubtotal
                    fiscalDetail { cstType cfopCode calculationBasis taxRate taxValue pisRate pisValue cofinsRate cofinsValue hasDifal difalTaxValueOrigin difalTaxValueDestination }
                    trtSubtotal
                  }
                }
                pageInfo { hasNextPage endCursor }
              }
            }""";
    
    /**
     * Query para buscar Capa de Faturas (CreditCustomerBilling)
     * Tipo GraphQL: CreditCustomerBilling
     * Campo de filtro: dueDate, issueDate ou originalDueDate (aceita apenas uma data específica)
     */
    public static final String QUERY_FATURAS = """
            query ExtrairFaturas_Billing_Final($params: CreditCustomerBillingInput!, $after: String) {
              creditCustomerBilling(params: $params, first: 100, after: $after) {
                pageInfo { hasNextPage endCursor }
                edges {
                  node {
                    id
                    document
                    dueDate
                    issueDate
                    value
                    paidValue
                    valueToPay
                    discountValue
                    interestValue
                    paid
                    type
                    comments
                    sequenceCode
                    competenceMonth
                    competenceYear
                    customer {
                      id
                      nickname
                      person { name cnpj }
                    }
                    corporation {
                      id
                      person { nickname cnpj }
                    }
                    installments {
                      id
                      position
                      sequenceCode
                      value
                      valueToPay
                      dueDate
                      originalDueDate
                      status
                    }
                  }
                }
              }
            }""";
    
    /**
     * Query para buscar NFSe diretamente
     * Tipo GraphQL: Nfse
     * Campo de filtro: issuedAt (aceita intervalo "data_inicio - data_fim")
     */
    public static final String QUERY_NFSE = """
            query ExtracaoNfseDireta($params: NfseInput!, $after: String) {
              nfse(params: $params, first: 100, after: $after) {
                edges {
                  node {
                    id
                    freightId
                    freight { id }
                    number
                    status
                    rpsSeries
                    issuedAt
                    cancelationReason
                    pdfServiceUrl
                    xmlDocument
                    corporationId
                    nfseService { id description }
                  }
                }
                pageInfo { hasNextPage endCursor }
              }
            }""";
    
    /**
     * Query de teste de conectividade
     */
    public static final String QUERY_TESTE = "{ __schema { queryType { name } } }";
    
    /**
     * Query de introspection para descobrir campos de CreditCustomerBillingInput
     */
    public static final String INTROSPECTION_CREDIT_CUSTOMER_BILLING = """
            query CamposCreditCustomerBillingInput {
              __type(name: "CreditCustomerBillingInput") {
                inputFields { name }
              }
            }""";
}

