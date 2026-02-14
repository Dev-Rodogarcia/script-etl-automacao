package br.com.extrator.api.graphql;

/**
 * Classe que centraliza todas as queries GraphQL utilizadas no sistema.
 * Isso evita duplicaÃ§Ã£o e facilita manutenÃ§Ã£o.
 */
public final class GraphQLQueries {
    
    private GraphQLQueries() {
        // Construtor privado para classe utilitÃ¡ria
    }
    
    /**
     * Query para buscar Coletas (Pick)
     * Tipo GraphQL: Pick
     * Campo de filtro: requestDate (aceita apenas uma data especÃ­fica)
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
                    taxedWeight
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
     * Campo de filtro: dueDate, issueDate ou originalDueDate (aceita apenas uma data especÃ­fica)
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
                    ticketAccountId
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
                      paymentMethod
                      accountingCredit {
                        document
                      }
                      accountingBankAccount {
                        bankName
                        portfolioVariation
                        customInstruction
                      }
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

    /**
     * Query de introspection para descobrir campos de PickInput.
     * Usada para decidir dinamicamente filtros vÃ¡lidos (requestDate/serviceDate).
     */
    public static final String INTROSPECTION_PICK_INPUT = """
            query CamposPickInput {
              __type(name: "PickInput") {
                inputFields { name }
              }
            }""";
    
    /**
     * Query de introspection para descobrir o tipo de destroyUserId e cancellationUserId em Pick
     * IMPORTANTE: Usar para validar se sÃ£o do tipo Individual antes de fazer JOIN
     */
    public static final String INTROSPECTION_PICK_FIELDS = """
            query IntrospectPickFields {
              __type(name: "Pick") {
                fields {
                  name
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
            }""";
    
    /**
     * Query para buscar UsuÃ¡rios do Sistema (Individual)
     * Tipo GraphQL: Individual
     * Filtro: enabled: true (obrigatÃ³rio)
     * PaginaÃ§Ã£o: cursor-based (first: 100, after: $cursor)
     * 
     * âš ï¸ ATENÃ‡ÃƒO: Esta query busca todos os usuÃ¡rios do tipo Individual.
     * Validar se destroyUserId e cancellationUserId em Pick sÃ£o do mesmo tipo Individual
     * antes de fazer JOIN na view de coletas. Se forem de outro tipo (ex: User, Driver),
     * o cruzamento retornarÃ¡ dados incorretos.
     */
    public static final String QUERY_USUARIOS_SISTEMA = """
            query ExtrairUsuariosSistema($params: IndividualInput!, $cursor: String) {
              individual(params: $params, first: 100, after: $cursor) {
                pageInfo {
                  hasNextPage
                  endCursor
                }
                edges {
                  node {
                    id
                    name
                  }
                }
              }
            }""";
    
    /**
     * Query para enriquecer Faturas por Cliente com dados financeiros.
     * Busca NÂ° NFS-e, Carteira e InstruÃ§Ã£o Customizada via creditCustomerBilling.
     * 
     * Tipo GraphQL: CreditCustomerBilling
     * ParÃ¢metro: id (ID da cobranÃ§a)
     * 
     * Campos extraÃ­dos:
     * - nfse_numero: accountingCredit.document (da primeira parcela)
     * - carteira_banco: accountingBankAccount.portfolioVariation (da primeira parcela)
     * - instrucao_boleto: accountingBankAccount.customInstruction (da primeira parcela)
     */
    public static final String QUERY_ENRIQUECER_FATURAS = """
            query EnriquecerFaturas($id: ID!) {
              creditCustomerBilling(params: { id: $id }) {
                edges {
                  node {
                    id
                    installments {
                      accountingCredit {
                        document
                      }
                      accountingBankAccount {
                        bankName
                        portfolioVariation
                        customInstruction
                      }
                    }
                  }
                }
              }
            }""";
    
    /**
     * Query para enriquecer faturas por nÃºmero do documento (fallback quando billingId nÃ£o estÃ¡ disponÃ­vel).
     * ParÃ¢metro: document (nÃºmero do documento da fatura, ex: "112025/1-3")
     * 
     * Campos extraÃ­dos:
     * - nfse_numero: accountingCredit.document (da primeira parcela)
     * - carteira_banco: accountingBankAccount.portfolioVariation (da primeira parcela)
     * - instrucao_boleto: accountingBankAccount.customInstruction (da primeira parcela)
     */
    public static final String QUERY_ENRIQUECER_FATURAS_POR_DOCUMENTO = """
            query EnriquecerFaturasPorDocumento($document: String!) {
              creditCustomerBilling(params: { document: $document }, first: 1) {
                edges {
                  node {
                    id
                    installments {
                      accountingCredit {
                        document
                      }
                      accountingBankAccount {
                        bankName
                        portfolioVariation
                        customInstruction
                      }
                    }
                  }
                }
              }
            }""";
    
    /**
     * Query para enriquecer cobranÃ§a individual com NFS-e e ID do banco.
     * Usada dentro do loop de enriquecimento para cada fatura.
     * 
     * ParÃ¢metro: id (ID da cobranÃ§a - creditCustomerBilling)
     * 
     * Campos extraÃ­dos:
     * - ticketAccountId: ID para buscar detalhes do banco depois
     * - nfse_numero: accountingCredit.document (da primeira parcela)
     * - metodo_pagamento: installments[0].paymentMethod
     */
    public static final String QUERY_ENRIQUECER_COBRANCA_NFSE = """
            query EnriquecerCobranca_Nfse($id: ID!) {
              creditCustomerBilling(params: { id: $id }) {
                edges {
                  node {
                    id
                    ticketAccountId
                    installments {
                      id
                      paymentMethod
                      accountingCredit {
                        document
                      }
                    }
                  }
                }
              }
            }""";
    
    /**
     * Query para resolver detalhes de conta bancÃ¡ria via ID.
     * Usada para buscar dados do banco de forma otimizada (cache).
     * 
     * ParÃ¢metro: id (ID da conta bancÃ¡ria - ticketAccountId)
     * 
     * Campos extraÃ­dos:
     * - bankName: Nome do banco
     * - portfolioVariation: Carteira/DescriÃ§Ã£o (pode vir vazio se nÃ£o cadastrado)
     * - customInstruction: InstruÃ§Ã£o customizada (pode vir vazio se nÃ£o cadastrado)
     */
    public static final String QUERY_RESOLVER_CONTA_BANCARIA = """
            query ResolverContaBancaria($id: Int!) {
              bankAccount(params: { id: $id }) {
                edges {
                  node {
                    id
                    bankName
                    portfolioVariation
                    customInstruction
                  }
                }
              }
            }""";
}
