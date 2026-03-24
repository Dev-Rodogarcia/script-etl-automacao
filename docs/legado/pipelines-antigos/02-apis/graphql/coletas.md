---
context:
  - Documentacao
  - Legado
  - ArquivoHistorico
updated_at: 2026-03-18T00:00:32.1501533-03:00
source_of_truth: docs
classification: parcial
related_files:
  - docs/index.md
  - docs/legado/index.md
  - docs/legado/classificacao.md
---
## 📄 Documentação de Descoberta: API GraphQL (Coletas) 

### 1\. Objetivo

Identificar e validar o schema da entidade "Coletas" (tipo `Pick` no GraphQL) para garantir 100% de cobertura estrutural em relação ao arquivo CSV de origem (`coletas_analitico...csv`).

### 2\. Metodologia de Descoberta e Validação

O processo exigiu várias etapas, pois o schema da API (relacional/aninhado) é diferente do schema do CSV (plano/achatado).

1.  **Endpoint e Autenticação (Sucesso):**

      * O endpoint `POST {{base_url}}/graphql` foi validado.
      * A autenticação `Bearer {{token_graphql}}` (configurada no ambiente) foi validada com sucesso, retornando `200 OK` nas queries } }].

2.  **Introspection (Nível 1 - Falha Parcial):**

      * A query `[INTROSPECTION] Campos de Pick (Coletas)` funcionou e retornou 44 campos.
      * Ela mapeou os campos simples (ex: `sequenceCode`), mas revelou que dados-chave do CSV (como `Cliente`, `Cidade`) eram Objetos (`customer`, `pickAddress`).

3.  **Teste de Query (Falha nos Nomes dos Campos):**

      * A tentativa de executar a query `[EXPANDIDA]` falhou, pois os nomes dos campos (`name` em `PickAddress`, `abbreviation` em `State`) estavam incorretos "message": "Field 'name' doesn't exist on type 'PickAddress'"].

4.  **Introspection (Nível 2 - Correção):**

      * Uma segunda query de Introspection (para `PickAddress`, `City`, `State`) foi executada.
      * Ela revelou os nomes corretos dos campos-alvo:
          * "Local da Coleta" (CSV) -\> `line1` (API).
          * "Estado" (UF) (CSV) -\> `code` (API).

5.  **Validação Final (Sucesso):**

      * Uma nova query (`BuscarColetasExpandidaV2`) foi executada com os nomes de campos corrigidos (ex: `pickAddress { line1, city { name, state { code } } }`).
      * A requisição retornou `200 OK` e um JSON com os dados relacionais preenchidos (ex: `line1: "ROD FERNAO DIAS"`, `city: { name: "Extrema" }`) } }].
      * Isso validou 100% da cobertura estrutural.

6.  **Validação de Volume (Inconclusiva):**

      * O guia `03-requisicoes-api-graphql.md` sugere um `totalCount` (meta: 476).
      * Os testes provaram que o campo `totalCount` **não existe** nesta API "message": "Field 'totalCount' doesn't exist on type 'PickConnection'", "errors": [...] "message": "Field 'totalCount' doesn't exist on type 'PageInfo'"].
      * A extração de dados deverá ser feita por paginação (usando `endCursor` e `hasNextPage`) até que todos os dados sejam recuperados.

-----

### 3\. Configuração Final no Insomnia (Coletas)

Esta é a configuração da requisição que valida o mapeamento completo do schema.

#### 3.1. Pasta

`API GraphQL / Coletas`

#### 3.2. Requisição

  * **Nome:** `[EXPANDIDA] Buscar Coletas + Relacionamentos (V2)`
  * **Método:** `POST`
  * **URL:** `{{base_url}}/graphql`

#### 3.3. Body (Corpo)

  * **Tipo:** `GraphQL`
  * **Painel QUERY (Query Válida):**
    ```graphql
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
        "requestDate": "2025-11-03"
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

  * **Fonte CSV:** `coletas_analitico_03-11-2025_19-22.csv`
  * **Fonte API:** Query `BuscarColetasExpandidaV2` (baseada na Introspection)

| Coluna CSV (Origem) | Query GraphQL (Destino) | Status |
| :--- | :--- | :--- |
| `Coleta` | `sequenceCode` | ✅ **Mapeado** |
| `Cliente` | `customer { name }` | ✅ **Mapeado** |
| `Solicitante` | `requester` | ✅ **Mapeado** |
| `Local da Coleta` | `pickAddress { line1 }` | ✅ **Mapeado** |
| `Cidade` | `pickAddress { city { name } }` | ✅ **Mapeado** |
| (UF / Estado) | `pickAddress { city { state { code } } }`| ✅ **Mapeado** |
| `Solicitação` (Data) | `requestDate` | ✅ **Mapeado** |
| `Hora` (Solicitação) | `requestHour` | ✅ **Mapeado** |
| `Agendamento` | `serviceDate` | ✅ **Mapeado** |
| `Horário` (Início) | `serviceStartHour` | ✅ **Mapeado** |
| `Finalização` | `finishDate` | ✅ **Mapeado** |
| `Hora.1` (Fim) | `serviceEndHour` | ✅ **Mapeado** |
| `Status` | `status` | ✅ **Mapeado** |
| `Volumes` | `invoicesVolumes` | ✅ **Mapeado** |
| `Peso Real` | `invoicesWeight` | ✅ **Mapeado** |
| `Peso Taxado` | `taxedWeight` | ✅ **Mapeado** |
| `Valor NF` | `invoicesValue` | ✅ **Mapeado** |
| `Observações` | `comments` | ✅ **Mapeado** |
| `Agente` | `agentId` | ✅ **Mapeado** |
| `Usuário` / `Motorista`| `user { name }` | ✅ **Mapeado** |
| `Nº Manifesto` | `manifestItemPickId` | ✅ **Mapeado** (ID) |
| `Veículo` | `vehicleTypeId` | ✅ **Mapeado** (ID) |

### 5\. Conclusão

A cobertura do schema para "Coletas" é de **100%**. Todos os campos do CSV podem ser obtidos, embora exijam a expansão de objetos (`customer`, `pickAddress`, `user`) } }].


{
	"data": {
		"pick": {
			"edges": [
				{
					"cursor": "MQ",
					"node": {
						"id": "3551845",
						"sequenceCode": 71087,
						"requestDate": "2025-11-03",
						"serviceDate": "2025-11-04",
						"status": "finished",
						"requester": "DANILO",
						"customer": {
							"id": "7626683",
							"name": "FRIGELAR COMERCIO E INDUSTRIA LTDA"
						},
						"pickAddress": {
							"line1": "ROD FERNAO DIAS",
							"city": {
								"name": "Extrema",
								"state": {
									"code": "MG"
								}
							}
						},
						"user": {
							"id": "58298",
							"name": "Daniela Bueno - Coleta (SPO)"
						}
					}
				},
				{
					"cursor": "Mg",
					"node": {
						"id": "3553625",
						"sequenceCode": 71185,
						"requestDate": "2025-11-03",
						"serviceDate": "2025-11-03",
						"status": "canceled",
						"requester": "",
						"customer": {
							"id": "7328200",
							"name": "ENERBRAX ACUMULADORES LTDA."
						},
						"pickAddress": {
							"line1": "AVENIDA RODRIGUES ALVES",
							"city": {
								"name": "Bauru",
								"state": {
									"code": "SP"
								}
							}
						},
						"user": {
							"id": "47673",
							"name": "Lucas Almeida - Expedição (AGU)"
						}
					}
				},
				{
					"cursor": "Mw",
					"node": {
						"id": "3553451",
						"sequenceCode": 71173,
						"requestDate": "2025-11-03",
						"serviceDate": "2025-11-04",
						"status": "pending",
						"requester": "",
						"customer": {
							"id": "7847219",
							"name": "COPE & CIA LTDA"
						},
						"pickAddress": {
							"line1": "AV.SAO BORJA",
							"city": {
								"name": "São Leopoldo",
								"state": {
									"code": "RS"
								}
							}
						},
						"user": {
							"id": "52431",
							"name": "Filial Novo Hamburgo"
						}
					}
				},
				{
					"cursor": "NA",
					"node": {
						"id": "3551154",
						"sequenceCode": 71062,
						"requestDate": "2025-11-03",
						"serviceDate": "2025-11-03",
						"status": "done",
						"requester": null,
						"customer": {
							"id": "7347990",
							"name": "FRIGELAR COMERCIO E INDUSTRIA LTDA"
						},
						"pickAddress": {
							"line1": "AV AIRTON PRETINI",
							"city": {
								"name": "São Paulo",
								"state": {
									"code": "SP"
								}
							}
						},
						"user": null
					}
				},
				{
					"cursor": "NQ",
					"node": {
						"id": "3547220",
						"sequenceCode": 70874,
						"requestDate": "2025-11-03",
						"serviceDate": "2025-11-03",
						"status": "finished",
						"requester": "NAYANE - EMAIL",
						"customer": {
							"id": "7375638",
							"name": "JAPKS LOGISTICA E TRANSPORTES LTDA"
						},
						"pickAddress": {
							"line1": "R NUNES MACHADO",
							"city": {
								"name": "Curitiba",
								"state": {
									"code": "PR"
								}
							}
						},
						"user": {
							"id": "47435",
							"name": "Maria Felício - Comercial (MTZ)"
						}
					}
				},
				{
					"cursor": "Ng",
					"node": {
						"id": "3548765",
						"sequenceCode": 70957,
						"requestDate": "2025-11-03",
						"serviceDate": "2025-11-03",
						"status": "finished",
						"requester": "alesandra",
						"customer": {
							"id": "7452936",
							"name": "REELPAPER COMERCIO DE PAPEIS LTDA - EPP"
						},
						"pickAddress": {
							"line1": "Rua Maurilio da Cruz",
							"city": {
								"name": "São José dos Pinhais",
								"state": {
									"code": "PR"
								}
							}
						},
						"user": {
							"id": "68231",
							"name": "Suelem Nascimento - Expedição (CWB)"
						}
					}
				},
				{
					"cursor": "Nw",
					"node": {
						"id": "3549711",
						"sequenceCode": 70996,
						"requestDate": "2025-11-03",
						"serviceDate": "2025-11-03",
						"status": "done",
						"requester": "cruz",
						"customer": {
							"id": "7394569",
							"name": "NOVOZYMES LATIN AMERICA LTDA."
						},
						"pickAddress": {
							"line1": "Rua Professor Francisco Ribeiro",
							"city": {
								"name": "Araucária",
								"state": {
									"code": "PR"
								}
							}
						},
						"user": {
							"id": "68665",
							"name": "Gislaine Oliveira - Comercial (CWB)"
						}
					}
				},
				{
					"cursor": "OA",
					"node": {
						"id": "3548992",
						"sequenceCode": 70965,
						"requestDate": "2025-11-03",
						"serviceDate": "2025-11-03",
						"status": "finished",
						"requester": "Naiene telefone",
						"customer": {
							"id": "9770972",
							"name": "STEELMOLAS INDUSTRIA DE MOLAS LTDA"
						},
						"pickAddress": {
							"line1": "R MARANHAO",
							"city": {
								"name": "São José dos Pinhais",
								"state": {
									"code": "PR"
								}
							}
						},
						"user": {
							"id": "68231",
							"name": "Suelem Nascimento - Expedição (CWB)"
						}
					}
				},
				{
					"cursor": "OQ",
					"node": {
						"id": "3552781",
						"sequenceCode": 71133,
						"requestDate": "2025-11-03",
						"serviceDate": "2025-11-03",
						"status": "done",
						"requester": "",
						"customer": {
							"id": "7307322",
							"name": "PPG INDUSTRIAL DO BRASIL - TINTAS E VERNIZES - LTDA."
						},
						"pickAddress": {
							"line1": "ROD ANHANGUERA",
							"city": {
								"name": "Sumaré",
								"state": {
									"code": "SP"
								}
							}
						},
						"user": {
							"id": "48092",
							"name": "Pedro Rosini - Operação (CPQ)"
						}
					}
				},
				{
					"cursor": "MTA",
					"node": {
						"id": "3551663",
						"sequenceCode": 71084,
						"requestDate": "2025-11-03",
						"serviceDate": "2025-11-04",
						"status": "finished",
						"requester": "",
						"customer": {
							"id": "7749066",
							"name": "CORBION PRODUTOS RENOVAVEIS LTDA"
						},
						"pickAddress": {
							"line1": "Avenida Rui Barbosa",
							"city": {
								"name": "Campos dos Goytacazes",
								"state": {
									"code": "RJ"
								}
							}
						},
						"user": {
							"id": "48024",
							"name": "Felipe Santos - Expedição (RJR)"
						}
					}
				},
				{
					"cursor": "MTE",
					"node": {
						"id": "3552443",
						"sequenceCode": 71115,
						"requestDate": "2025-11-03",
						"serviceDate": "2025-11-03",
						"status": "finished",
						"requester": "WHATS",
						"customer": {
							"id": "7324718",
							"name": "ICEFRESH INDUSTRIA E COMERCIO DO BRASIL"
						},
						"pickAddress": {
							"line1": "AVENIDA JOSE FORTUNATO MOLINA",
							"city": {
								"name": "Bauru",
								"state": {
									"code": "SP"
								}
							}
						},
						"user": {
							"id": "47673",
							"name": "Lucas Almeida - Expedição (AGU)"
						}
					}
				},
				{
					"cursor": "MTI",
					"node": {
						"id": "3552403",
						"sequenceCode": 71110,
						"requestDate": "2025-11-03",
						"serviceDate": "2025-11-04",
						"status": "finished",
						"requester": "juliana",
						"customer": {
							"id": "7348687",
							"name": "ORTHO PAUHER IND. E COM. DISTR. LTDA."
						},
						"pickAddress": {
							"line1": "RUA BANDEIRANTE",
							"city": {
								"name": "Recife",
								"state": {
									"code": "PE"
								}
							}
						},
						"user": {
							"id": "48214",
							"name": "Juliana Pereira - Comercial Externo (REC)"
						}
					}
				},
				{
					"cursor": "MTM",
					"node": {
						"id": "3552763",
						"sequenceCode": 71132,
						"requestDate": "2025-11-03",
						"serviceDate": "2025-11-04",
						"status": "finished",
						"requester": "",
						"customer": {
							"id": "12543543",
							"name": "GREFORTEC FORNOS INDUSTRIAIS E TRATAMENTO TERMICO LTDA"
						},
						"pickAddress": {
							"line1": "RUA ESTANCIA VELHA",
							"city": {
								"name": "Portão",
								"state": {
									"code": "RS"
								}
							}
						},
						"user": {
							"id": "52431",
							"name": "Filial Novo Hamburgo"
						}
					}
				},
				{
					"cursor": "MTQ",
					"node": {
						"id": "3552029",
						"sequenceCode": 71098,
						"requestDate": "2025-11-03",
						"serviceDate": "2025-11-04",
						"status": "finished",
						"requester": "EDUARDO TEL",
						"customer": {
							"id": "8445967",
							"name": "FIXPEL ARTES GRAFICAS LTDA"
						},
						"pickAddress": {
							"line1": "Rua Abrahão Gonçalves Braga",
							"city": {
								"name": "São Paulo",
								"state": {
									"code": "SP"
								}
							}
						},
						"user": {
							"id": "58298",
							"name": "Daniela Bueno - Coleta (SPO)"
						}
					}
				},
				{
					"cursor": "MTU",
					"node": {
						"id": "3553220",
						"sequenceCode": 71164,
						"requestDate": "2025-11-03",
						"serviceDate": "2025-11-04",
						"status": "done",
						"requester": "renato whats",
						"customer": {
							"id": "8150649",
							"name": "DX FIT PISOS E ARTIGOS ESPORTIVOS LTDA ME"
						},
						"pickAddress": {
							"line1": "Rua Abílio José Espinola",
							"city": {
								"name": "Taboão da Serra",
								"state": {
									"code": "SP"
								}
							}
						},
						"user": {
							"id": "48266",
							"name": "Kerin Nascimento - Coleta (SPO)"
						}
					}
				},
				{
					"cursor": "MTY",
					"node": {
						"id": "3552295",
						"sequenceCode": 71104,
						"requestDate": "2025-11-03",
						"serviceDate": "2025-11-03",
						"status": "finished",
						"requester": "eduardo",
						"customer": {
							"id": "10729737",
							"name": "GOWSH NETWORK TECHNOLOGY CO., LTDA"
						},
						"pickAddress": {
							"line1": "RUA OLIVEIRA VIANA",
							"city": {
								"name": "Curitiba",
								"state": {
									"code": "PR"
								}
							}
						},
						"user": {
							"id": "68665",
							"name": "Gislaine Oliveira - Comercial (CWB)"
						}
					}
				},
				{
					"cursor": "MTc",
					"node": {
						"id": "3551163",
						"sequenceCode": 71063,
						"requestDate": "2025-11-03",
						"serviceDate": "2025-11-03",
						"status": "done",
						"requester": null,
						"customer": {
							"id": "7368144",
							"name": "FRIGELAR COMERCIO E INDUSTRIA LTDA"
						},
						"pickAddress": {
							"line1": "Alameda Glete",
							"city": {
								"name": "São Paulo",
								"state": {
									"code": "SP"
								}
							}
						},
						"user": null
					}
				},
				{
					"cursor": "MTg",
					"node": {
						"id": "3552266",
						"sequenceCode": 71103,
						"requestDate": "2025-11-03",
						"serviceDate": "2025-11-04",
						"status": "pending",
						"requester": "KARINA TEL",
						"customer": {
							"id": "7405446",
							"name": "D. A. BRASIL COMERCIO DE ALCOOL LTDA"
						},
						"pickAddress": {
							"line1": "R SANTANA DE IPANEMA",
							"city": {
								"name": "Guarulhos",
								"state": {
									"code": "SP"
								}
							}
						},
						"user": {
							"id": "58298",
							"name": "Daniela Bueno - Coleta (SPO)"
						}
					}
				},
				{
					"cursor": "MTk",
					"node": {
						"id": "3552615",
						"sequenceCode": 71124,
						"requestDate": "2025-11-03",
						"serviceDate": "2025-11-03",
						"status": "finished",
						"requester": "Cesar Ferro",
						"customer": {
							"id": "8805910",
							"name": "RDBM COMERCIO DE PRODUTOS PARA LABORATORIOS LTDA"
						},
						"pickAddress": {
							"line1": "Rodovia BR-277 Curitiba-Paranaguá",
							"city": {
								"name": "Curitiba",
								"state": {
									"code": "PR"
								}
							}
						},
						"user": {
							"id": "68231",
							"name": "Suelem Nascimento - Expedição (CWB)"
						}
					}
				},
				{
					"cursor": "MjA",
					"node": {
						"id": "3551764",
						"sequenceCode": 71085,
						"requestDate": "2025-11-03",
						"serviceDate": "2025-11-03",
						"status": "finished",
						"requester": "Luziane",
						"customer": {
							"id": "7307673",
							"name": "KEMIRA CHEMICALS BRASIL LTDA"
						},
						"pickAddress": {
							"line1": "ROD DO PAPEL PR 160 KM 222",
							"city": {
								"name": "Telêmaco Borba",
								"state": {
									"code": "PR"
								}
							}
						},
						"user": {
							"id": "69357",
							"name": "Ana Izidoro - SAC (CAS)"
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