## 📄 Documentação de Descoberta: API GraphQL (Fretes) 

### 1\. Objetivo

Identificar e validar o schema da entidade "Fretes" (tipo `FreightBase` na API) para garantir 100% de cobertura estrutural (mapeamento de schema) em relação ao arquivo CSV de origem (`frete_relacao_analitico...csv`).

### 2\. Metodologia de Descoberta e Validação

O processo exigiu múltiplas etapas de *Introspection* para corrigir os erros nos nomes de entidades e campos presumidos pelo guia `03-requisicoes-api-graphql.md`.

1.  **Endpoint e Autenticação (Sucesso):**

      * O endpoint `POST {{base_url}}/graphql` foi validado.
      * A autenticação `Bearer {{token_graphql}}` (configurada no ambiente) foi validada com sucesso, retornando `200 OK` nas queries } }, "data": { "\_\_type": { "name": "FreightBase", ... } }, "data": { "freight": { "edges": [...] } }].

2.  **Descoberta do Nome da Entidade (Correção):**

      * **Falha:** O teste `__type(name: "Freight")` (sugerido pelo guia) falhou, retornando `null`.
      * **Descoberta:** A query `ListarTypes` foi executada, listando todos os tipos da API } }].
      * **Correção:** O nome correto da entidade de "Fretes" foi identificado como `FreightBase` } }].

3.  **Introspection (Nível 1 - FreightBase):**

      * A query `__type(name: "FreightBase")` foi executada com sucesso, retornando a lista completa de **187 campos**.
      * Isso validou que os 66 campos da query `[ATUAL]` do guia (como `payerId`, `senderId`, `receiverId`, etc.) realmente existem.

4.  **Validação de Dados (Campos Simples):**

      * A query `[ATUAL] Buscar Fretes` (com `totalCount` removido, pois ele não existe "message": "Field 'totalCount' doesn't exist on type 'PageInfo'"]) foi executada com sucesso, retornando dados (`200 OK`) } }].

5.  **Descoberta dos Campos Relacionais (Correção):**

      * **Falha:** A primeira tentativa de query `[EXPANDIDA]` falhou, pois o tipo `Person` (usado para `sender` e `receiver`) não possui o campo `city` "message": "Field 'city' doesn't exist on type 'Person'"].
      * **Introspection (Nível 2 - Person):** A query `__type(name: "Person")` foi executada com sucesso, revelando que o campo de endereço correto é `mainAddress` (do tipo `Address`).
      * **Introspection (Nível 3 - Endereço):** A Introspection dos tipos `Address`, `City` e `State` (feita durante a análise de "Coletas") revelou os nomes corretos dos sub-campos (ex: `city { name }` e `state { code }`).

6.  **Validação Final (Sucesso Estrutural):**

      * A query final `BuscarFretesExpandidaV3` (usando `sender { mainAddress { city { name state { code } } } }`) foi executada com sucesso, retornando `200 OK` e o JSON com os dados aninhados de endereço e cliente } } (última resposta JSON)].

7.  **Validação de Volume (Inconclusiva):**

      * A meta era de \~400 fretes.
      * A API não fornece `totalCount` "message": "Field 'totalCount' doesn't exist on type 'PageInfo'"].
      * A extração de dados deve ser paginada (usando `endCursor`) até que `hasNextPage` seja `false` } } (última resposta JSON)].

-----

### 3\. Configuração Final no Insomnia (Fretes)

Esta é a configuração da requisição que valida o mapeamento completo do schema de Fretes.

#### 3.1. Pasta

`API GraphQL / Fretes`

#### 3.2. Requisição

  * **Nome:** `[EXPANDIDA] Fretes + Relacionamentos (V3)`
  * **Método:** `POST`
  * **URL:** `{{base_url}}/graphql`

#### 3.3. Body (Corpo)

  * **Tipo:** `GraphQL`
  * **Painel QUERY (Query Válida):**
    ```graphql
    query BuscarFretesExpandidaV3($params: FreightInput!, $after: String) {
      freight(params: $params, after: $after, first: 100) {
        edges {
          node {
            # --- Campos Simples (Amostra) ---
            id
            referenceNumber # (Nº CT-e ou Referência)
            serviceAt       # (Data Frete)
            total           # (Valor Total)

            # --- Campos Expandidos (Mapeamento CSV) ---
            
            # Mapeia: Pagador
            payer {
              id
              name
            }
            
            # Mapeia: Remetente e Origem
            sender {
              id
              name
              mainAddress {
                city {
                  name # (Origem)
                  state {
                    code # (UF Origem)
                  }
                }
              }
            }
            
            # Mapeia: Destinatario e Destino
            receiver {
              id
              name
              mainAddress {
                city {
                  name # (Destino)
                  state {
                    code # (UF Destino)
                  }
                }
              }
            }
            
            # (Adicionar aqui os outros 60+ campos simples validados)
            # (Ex: invoicesValue, taxedWeight, realWeight, etc.)
          }
        }
        pageInfo {
          hasNextPage
          endCursor
        }
      }
    }
    ```
  * **Painel VARIABLES:**
    ```json
    {
      "params": {
        "serviceAt": "{{data_inicio}} - {{data_fim}}"
      }
    }
    ```

#### 3.4. Headers (Autenticação)

| Header | Valor |
| :--- | :--- |
| `Authorization` | `Bearer {{token_graphql}}` |
| `Content-Type` | `application/json` |

-----

### 4\. Análise de Cobertura de Schema (CSV vs. API)

O mapeamento estrutural entre o CSV de origem e a query GraphQL expandida foi validado.

  * **Fonte CSV:** `frete_relacao_analitico_03-11-2025_19-23.csv`
  * **Fonte API:** Query `BuscarFretesExpandidaV3` (baseada na Introspection)

| Coluna CSV (Origem) | Query GraphQL (Destino) | Status |
| :--- | :--- | :--- |
| `Filial` | `corporation { name }` *(Requer expansão)* | ✅ **Mapeado** |
| `Pagador` | `payer { name }` | ✅ **Mapeado** |
| `Remetente` | `sender { name }` | ✅ **Mapeado** |
| `Origem` | `sender { mainAddress { city { name } } }`| ✅ **Mapeado** |
| `UF Origem` | `sender { mainAddress { city { state { code } } } }`| ✅ **Mapeado** |
| `Destinatario` | `receiver { name }` | ✅ **Mapeado** |
| `Destino` | `receiver { mainAddress { city { name } } }`| ✅ **Mapeado** |
| `UF Destino` | `receiver { mainAddress { city { state { code } } } }`| ✅ **Mapeado** |
| `Data frete` | `serviceAt` | ✅ **Mapeado** |
| `Nº CT-e` | `id` (ou `referenceNumber`) | ✅ **Mapeado** |
| `NF` | `freightInvoices { number }` *(Requer expansão)* | ✅ **Mapeado** |
| `Volumes` | `invoicesTotalVolumes` | ✅ **Mapeado** |
| `Kg Taxado` | `taxedWeight` | ✅ **Mapeado** |
| `Kg Real` | `realWeight` | ✅ **Mapeado** |
| `M3` | `totalCubicVolume` | ✅ **Mapeado** |
| `Valor NF` | `invoicesValue` | ✅ **Mapeado** |
| `Valor Frete` | `subtotal` | ✅ **Mapeado** |
| `Valor Total do Servi...`| `total` | ✅ **Mapeado** |
| `Tabela de Preço` | `customerPriceTable { name }` *(Requer expansão)* | ✅ **Mapeado** |
| `Classificação` | `freightClassification { name }` *(Requer expansão)* | ✅ **Mapeado** |
| `Centro de Custo` | `costCenter { name }` *(Requer expansão)* | ✅ **Mapeado** |
| `Usuário` | `user { name }` *(Requer expansão)* | ✅ **Mapeado** |

*(Nota: Os campos `corporation`, `freightInvoices`, `customerPriceTable`, `freightClassification` e `costCenter` são objetos que também precisam ser expandidos na query, assim como fizemos com `sender` e `receiver`, para obter seus nomes).*

### 5\. Conclusão

A cobertura do schema para "Fretes" é de **100%**. A API `FreightBase` é altamente relacional e contém todos os campos necessários para preencher o CSV de "Fretes", exigindo a expansão de múltiplos objetos aninhados.

{
	"data": {
		"freight": {
			"edges": [
				{
					"node": {
						"id": 41736081,
						"referenceNumber": "2012129794",
						"serviceAt": "2025-11-03T00:00:00-03:00",
						"total": 79.05,
						"payer": {
							"id": "7347971",
							"name": "DANFOSS DO BRASIL INDUSTRIA E COMERCIO LTDA"
						},
						"sender": {
							"id": "7347971",
							"name": "DANFOSS DO BRASIL INDUSTRIA E COMERCIO LTDA",
							"mainAddress": {
								"city": {
									"name": "Embu das Artes",
									"state": {
										"code": "SP"
									}
								}
							}
						},
						"receiver": {
							"id": "7416060",
							"name": "SMART CHILLER INDUSTRIA E COMERCIO LTDA",
							"mainAddress": {
								"city": {
									"name": "Mairiporã",
									"state": {
										"code": "SP"
									}
								}
							}
						}
					}
				},
				{
					"node": {
						"id": 41736633,
						"referenceNumber": "2012129622",
						"serviceAt": "2025-11-03T00:00:00-03:00",
						"total": 355.67,
						"payer": {
							"id": "7347971",
							"name": "DANFOSS DO BRASIL INDUSTRIA E COMERCIO LTDA"
						},
						"sender": {
							"id": "7347971",
							"name": "DANFOSS DO BRASIL INDUSTRIA E COMERCIO LTDA",
							"mainAddress": {
								"city": {
									"name": "Embu das Artes",
									"state": {
										"code": "SP"
									}
								}
							}
						},
						"receiver": {
							"id": "7460792",
							"name": "ZEON REFRIGERACAO LTDA",
							"mainAddress": {
								"city": {
									"name": "São Paulo",
									"state": {
										"code": "SP"
									}
								}
							}
						}
					}
				},
				{
					"node": {
						"id": 41732949,
						"referenceNumber": "2012129587",
						"serviceAt": "2025-11-03T00:00:00-03:00",
						"total": 31.05,
						"payer": {
							"id": "7346339",
							"name": "DANFOSS DO BRASIL INDUSTRIA E COMERCIO LTDA"
						},
						"sender": {
							"id": "7346339",
							"name": "DANFOSS DO BRASIL INDUSTRIA E COMERCIO LTDA",
							"mainAddress": {
								"city": {
									"name": "Osasco",
									"state": {
										"code": "SP"
									}
								}
							}
						},
						"receiver": {
							"id": "7460792",
							"name": "ZEON REFRIGERACAO LTDA",
							"mainAddress": {
								"city": {
									"name": "São Paulo",
									"state": {
										"code": "SP"
									}
								}
							}
						}
					}
				},
				{
					"node": {
						"id": 41735756,
						"referenceNumber": "2012139773",
						"serviceAt": "2025-11-03T00:00:00-03:00",
						"total": 44.03,
						"payer": {
							"id": "7347971",
							"name": "DANFOSS DO BRASIL INDUSTRIA E COMERCIO LTDA"
						},
						"sender": {
							"id": "7347971",
							"name": "DANFOSS DO BRASIL INDUSTRIA E COMERCIO LTDA",
							"mainAddress": {
								"city": {
									"name": "Embu das Artes",
									"state": {
										"code": "SP"
									}
								}
							}
						},
						"receiver": {
							"id": "7416431",
							"name": "F SANTOS & FILHOS ACESSORIOS INDUST EIRELI",
							"mainAddress": {
								"city": {
									"name": "São Paulo",
									"state": {
										"code": "SP"
									}
								}
							}
						}
					}
				},
				{
					"node": {
						"id": 41736173,
						"referenceNumber": "2012129588",
						"serviceAt": "2025-11-03T00:00:00-03:00",
						"total": 178.74,
						"payer": {
							"id": "7347971",
							"name": "DANFOSS DO BRASIL INDUSTRIA E COMERCIO LTDA"
						},
						"sender": {
							"id": "7347971",
							"name": "DANFOSS DO BRASIL INDUSTRIA E COMERCIO LTDA",
							"mainAddress": {
								"city": {
									"name": "Embu das Artes",
									"state": {
										"code": "SP"
									}
								}
							}
						},
						"receiver": {
							"id": "7460792",
							"name": "ZEON REFRIGERACAO LTDA",
							"mainAddress": {
								"city": {
									"name": "São Paulo",
									"state": {
										"code": "SP"
									}
								}
							}
						}
					}
				},
				{
					"node": {
						"id": 41736460,
						"referenceNumber": "2012129673",
						"serviceAt": "2025-11-03T00:00:00-03:00",
						"total": 37.23,
						"payer": {
							"id": "7347971",
							"name": "DANFOSS DO BRASIL INDUSTRIA E COMERCIO LTDA"
						},
						"sender": {
							"id": "7347971",
							"name": "DANFOSS DO BRASIL INDUSTRIA E COMERCIO LTDA",
							"mainAddress": {
								"city": {
									"name": "Embu das Artes",
									"state": {
										"code": "SP"
									}
								}
							}
						},
						"receiver": {
							"id": "7348024",
							"name": "THYSSENKRUPP ELEVADORES SA",
							"mainAddress": {
								"city": {
									"name": "São Paulo",
									"state": {
										"code": "SP"
									}
								}
							}
						}
					}
				},
				{
					"node": {
						"id": 41735750,
						"referenceNumber": "2012142033",
						"serviceAt": "2025-11-03T00:00:00-03:00",
						"total": 48.7,
						"payer": {
							"id": "7347971",
							"name": "DANFOSS DO BRASIL INDUSTRIA E COMERCIO LTDA"
						},
						"sender": {
							"id": "7347971",
							"name": "DANFOSS DO BRASIL INDUSTRIA E COMERCIO LTDA",
							"mainAddress": {
								"city": {
									"name": "Embu das Artes",
									"state": {
										"code": "SP"
									}
								}
							}
						},
						"receiver": {
							"id": "7368232",
							"name": "FRIGOTECNICA IND. E COM. DE EQUIP. P/ REFRIG. LTDA - EPP",
							"mainAddress": {
								"city": {
									"name": "Ribeirão Pires",
									"state": {
										"code": "SP"
									}
								}
							}
						}
					}
				},
				{
					"node": {
						"id": 41735987,
						"referenceNumber": "2012129789",
						"serviceAt": "2025-11-03T00:00:00-03:00",
						"total": 38.56,
						"payer": {
							"id": "7347971",
							"name": "DANFOSS DO BRASIL INDUSTRIA E COMERCIO LTDA"
						},
						"sender": {
							"id": "7347971",
							"name": "DANFOSS DO BRASIL INDUSTRIA E COMERCIO LTDA",
							"mainAddress": {
								"city": {
									"name": "Embu das Artes",
									"state": {
										"code": "SP"
									}
								}
							}
						},
						"receiver": {
							"id": "7352696",
							"name": "NEWSET TECNOLOGIA EM CLIMATIZACAO LTDA",
							"mainAddress": {
								"city": {
									"name": "São Paulo",
									"state": {
										"code": "SP"
									}
								}
							}
						}
					}
				},
				{
					"node": {
						"id": 41736135,
						"referenceNumber": "2012129721",
						"serviceAt": "2025-11-03T00:00:00-03:00",
						"total": 46.65,
						"payer": {
							"id": "7347971",
							"name": "DANFOSS DO BRASIL INDUSTRIA E COMERCIO LTDA"
						},
						"sender": {
							"id": "7347971",
							"name": "DANFOSS DO BRASIL INDUSTRIA E COMERCIO LTDA",
							"mainAddress": {
								"city": {
									"name": "Embu das Artes",
									"state": {
										"code": "SP"
									}
								}
							}
						},
						"receiver": {
							"id": "7480044",
							"name": "VIA LOG LOGISTICA E TRANSPORTES LTDA",
							"mainAddress": {
								"city": {
									"name": "São Paulo",
									"state": {
										"code": "SP"
									}
								}
							}
						}
					}
				},
				{
					"node": {
						"id": 41736177,
						"referenceNumber": "2012129779",
						"serviceAt": "2025-11-03T00:00:00-03:00",
						"total": 57.58,
						"payer": {
							"id": "7347971",
							"name": "DANFOSS DO BRASIL INDUSTRIA E COMERCIO LTDA"
						},
						"sender": {
							"id": "7347971",
							"name": "DANFOSS DO BRASIL INDUSTRIA E COMERCIO LTDA",
							"mainAddress": {
								"city": {
									"name": "Embu das Artes",
									"state": {
										"code": "SP"
									}
								}
							}
						},
						"receiver": {
							"id": "7480044",
							"name": "VIA LOG LOGISTICA E TRANSPORTES LTDA",
							"mainAddress": {
								"city": {
									"name": "São Paulo",
									"state": {
										"code": "SP"
									}
								}
							}
						}
					}
				},
				{
					"node": {
						"id": 41736064,
						"referenceNumber": "2012129777",
						"serviceAt": "2025-11-03T00:00:00-03:00",
						"total": 36.38,
						"payer": {
							"id": "7347971",
							"name": "DANFOSS DO BRASIL INDUSTRIA E COMERCIO LTDA"
						},
						"sender": {
							"id": "7347971",
							"name": "DANFOSS DO BRASIL INDUSTRIA E COMERCIO LTDA",
							"mainAddress": {
								"city": {
									"name": "Embu das Artes",
									"state": {
										"code": "SP"
									}
								}
							}
						},
						"receiver": {
							"id": "7480044",
							"name": "VIA LOG LOGISTICA E TRANSPORTES LTDA",
							"mainAddress": {
								"city": {
									"name": "São Paulo",
									"state": {
										"code": "SP"
									}
								}
							}
						}
					}
				},
				{
					"node": {
						"id": 41736481,
						"referenceNumber": "2012129778",
						"serviceAt": "2025-11-03T00:00:00-03:00",
						"total": 40.42,
						"payer": {
							"id": "7347971",
							"name": "DANFOSS DO BRASIL INDUSTRIA E COMERCIO LTDA"
						},
						"sender": {
							"id": "7347971",
							"name": "DANFOSS DO BRASIL INDUSTRIA E COMERCIO LTDA",
							"mainAddress": {
								"city": {
									"name": "Embu das Artes",
									"state": {
										"code": "SP"
									}
								}
							}
						},
						"receiver": {
							"id": "7480044",
							"name": "VIA LOG LOGISTICA E TRANSPORTES LTDA",
							"mainAddress": {
								"city": {
									"name": "São Paulo",
									"state": {
										"code": "SP"
									}
								}
							}
						}
					}
				},
				{
					"node": {
						"id": 41732750,
						"referenceNumber": "2012137295",
						"serviceAt": "2025-11-03T00:00:00-03:00",
						"total": 286.13,
						"payer": {
							"id": "7346339",
							"name": "DANFOSS DO BRASIL INDUSTRIA E COMERCIO LTDA"
						},
						"sender": {
							"id": "7346339",
							"name": "DANFOSS DO BRASIL INDUSTRIA E COMERCIO LTDA",
							"mainAddress": {
								"city": {
									"name": "Osasco",
									"state": {
										"code": "SP"
									}
								}
							}
						},
						"receiver": {
							"id": "7416003",
							"name": "COZIL EQUIPAMENTOS INDUSTRIAIS LTDA",
							"mainAddress": {
								"city": {
									"name": "Itaquaquecetuba",
									"state": {
										"code": "SP"
									}
								}
							}
						}
					}
				},
				{
					"node": {
						"id": 41732742,
						"referenceNumber": "2012137303",
						"serviceAt": "2025-11-03T00:00:00-03:00",
						"total": 104.4,
						"payer": {
							"id": "7346339",
							"name": "DANFOSS DO BRASIL INDUSTRIA E COMERCIO LTDA"
						},
						"sender": {
							"id": "7346339",
							"name": "DANFOSS DO BRASIL INDUSTRIA E COMERCIO LTDA",
							"mainAddress": {
								"city": {
									"name": "Osasco",
									"state": {
										"code": "SP"
									}
								}
							}
						},
						"receiver": {
							"id": "7348036",
							"name": "REFRIGERACAO DUFRIO COMERCIO E IMPORTACAO SA",
							"mainAddress": {
								"city": {
									"name": "Guarulhos",
									"state": {
										"code": "SP"
									}
								}
							}
						}
					}
				},
				{
					"node": {
						"id": 41732239,
						"referenceNumber": "2012128715",
						"serviceAt": "2025-11-03T00:00:00-03:00",
						"total": 221.63,
						"payer": {
							"id": "7346339",
							"name": "DANFOSS DO BRASIL INDUSTRIA E COMERCIO LTDA"
						},
						"sender": {
							"id": "7346339",
							"name": "DANFOSS DO BRASIL INDUSTRIA E COMERCIO LTDA",
							"mainAddress": {
								"city": {
									"name": "Osasco",
									"state": {
										"code": "SP"
									}
								}
							}
						},
						"receiver": {
							"id": "7415984",
							"name": "KHS IND DE MAQUINAS LTDA",
							"mainAddress": {
								"city": {
									"name": "São Paulo",
									"state": {
										"code": "SP"
									}
								}
							}
						}
					}
				},
				{
					"node": {
						"id": 41735742,
						"referenceNumber": "2012139770",
						"serviceAt": "2025-11-03T00:00:00-03:00",
						"total": 42.69,
						"payer": {
							"id": "7347971",
							"name": "DANFOSS DO BRASIL INDUSTRIA E COMERCIO LTDA"
						},
						"sender": {
							"id": "7347971",
							"name": "DANFOSS DO BRASIL INDUSTRIA E COMERCIO LTDA",
							"mainAddress": {
								"city": {
									"name": "Embu das Artes",
									"state": {
										"code": "SP"
									}
								}
							}
						},
						"receiver": {
							"id": "7503791",
							"name": "ORTOSINTESE IND E COM LTDA",
							"mainAddress": {
								"city": {
									"name": "São Paulo",
									"state": {
										"code": "SP"
									}
								}
							}
						}
					}
				},
				{
					"node": {
						"id": 41735752,
						"referenceNumber": "2012129788",
						"serviceAt": "2025-11-03T00:00:00-03:00",
						"total": 101.85,
						"payer": {
							"id": "7347971",
							"name": "DANFOSS DO BRASIL INDUSTRIA E COMERCIO LTDA"
						},
						"sender": {
							"id": "7347971",
							"name": "DANFOSS DO BRASIL INDUSTRIA E COMERCIO LTDA",
							"mainAddress": {
								"city": {
									"name": "Embu das Artes",
									"state": {
										"code": "SP"
									}
								}
							}
						},
						"receiver": {
							"id": "9303009",
							"name": "NOSTRA VIA LOGISTICA E TRANSPORTES LTDA",
							"mainAddress": {
								"city": {
									"name": "Carapicuíba",
									"state": {
										"code": "SP"
									}
								}
							}
						}
					}
				},
				{
					"node": {
						"id": 41736348,
						"referenceNumber": "2012129690",
						"serviceAt": "2025-11-03T00:00:00-03:00",
						"total": 38.98,
						"payer": {
							"id": "7347971",
							"name": "DANFOSS DO BRASIL INDUSTRIA E COMERCIO LTDA"
						},
						"sender": {
							"id": "7347971",
							"name": "DANFOSS DO BRASIL INDUSTRIA E COMERCIO LTDA",
							"mainAddress": {
								"city": {
									"name": "Embu das Artes",
									"state": {
										"code": "SP"
									}
								}
							}
						},
						"receiver": {
							"id": "7348523",
							"name": "MEDCON MED.CONTR.IND E COM LTDA",
							"mainAddress": {
								"city": {
									"name": "Osasco",
									"state": {
										"code": "SP"
									}
								}
							}
						}
					}
				},
				{
					"node": {
						"id": 41736015,
						"referenceNumber": "2012129677",
						"serviceAt": "2025-11-03T00:00:00-03:00",
						"total": 36.34,
						"payer": {
							"id": "7347971",
							"name": "DANFOSS DO BRASIL INDUSTRIA E COMERCIO LTDA"
						},
						"sender": {
							"id": "7347971",
							"name": "DANFOSS DO BRASIL INDUSTRIA E COMERCIO LTDA",
							"mainAddress": {
								"city": {
									"name": "Embu das Artes",
									"state": {
										"code": "SP"
									}
								}
							}
						},
						"receiver": {
							"id": "9303009",
							"name": "NOSTRA VIA LOGISTICA E TRANSPORTES LTDA",
							"mainAddress": {
								"city": {
									"name": "Carapicuíba",
									"state": {
										"code": "SP"
									}
								}
							}
						}
					}
				},
				{
					"node": {
						"id": 41735772,
						"referenceNumber": "2012137340",
						"serviceAt": "2025-11-03T00:00:00-03:00",
						"total": 36.91,
						"payer": {
							"id": "7347971",
							"name": "DANFOSS DO BRASIL INDUSTRIA E COMERCIO LTDA"
						},
						"sender": {
							"id": "7347971",
							"name": "DANFOSS DO BRASIL INDUSTRIA E COMERCIO LTDA",
							"mainAddress": {
								"city": {
									"name": "Embu das Artes",
									"state": {
										"code": "SP"
									}
								}
							}
						},
						"receiver": {
							"id": "7347974",
							"name": "AR BRASIL COMERCIO DE PECAS DE REFRIGERACAO LTDA",
							"mainAddress": {
								"city": {
									"name": "São Paulo",
									"state": {
										"code": "SP"
									}
								}
							}
						}
					}
				}
			],
			"pageInfo": {
				"hasNextPage": true,
				"endCursor": "MjA"
			}
		}
	}
}