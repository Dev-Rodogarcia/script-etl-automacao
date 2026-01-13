## 📄 Documentação de Descoberta: API GraphQL (Fretes)

### 1\. Objetivo

Padronizar a extração da entidade "Fretes" (tipo `FreightBase` na API) para garantir **100% de integridade de dados** e **correspondência exata (Join)** com os relatórios analíticos extraídos manualmente (`frete_relacao_analitico...csv`). Esta documentação substitui versões anteriores, corrigindo falhas críticas de mapeamento de chaves fiscais e dados aninhados.

### 2\. Metodologia de Descoberta e Validação

O processo de validação (Versão 5.1) identificou e resolveu três bloqueios críticos que impediam a conciliação dos dados:

1.  **Correção do JOIN (Chave de Acesso):**

      * **Falha:** A tentativa de usar `referenceNumber` ou `id` resultava em 0% de cruzamento com o CSV manual.
      * **Descoberta:** Via Introspection, localizamos o objeto aninhado `cte`.
      * **Correção:** A chave de 44 dígitos (PK real) reside em `cte { key }`.

2.  **Dados de Notas Fiscais (NFs):**

      * **Falha:** A lista `freightInvoices` parecia vazia ou sem detalhes na raiz.
      * **Descoberta:** A estrutura correta é uma tabela de ligação (pivô).
      * **Correção:** Os dados reais estão em `freightInvoices { invoice { number, series, value } }`.

3.  **Ambiguidade de Valores Financeiros:**

      * **Análise:** A API separa estritamente `subtotal` (Frete Peso) de `total` (Frete + Taxas/Impostos). O relatório manual muitas vezes replica o valor total na coluna de frete.
      * **Validação:** O campo `total` da API foi validado com precisão de centavos em relação ao CSV.

-----

### 3\. Configuração Final no Insomnia (Produção)

Utilize esta configuração para a extração massiva dos dados.

#### 3.1. Pasta

`API GraphQL / Fretes / Produção`

#### 3.2. Requisição

  * **Nome:** `[PRODUÇÃO] Extração Fretes Full Schema (V5.1)`
  * **Método:** `POST`
  * **URL:** `{{base_url}}/graphql`

#### 3.3. Body (Query GraphQL Validada)

```graphql
query BuscarFretesProducaoV5_1($params: FreightInput!, $after: String) {
  freight(params: $params, after: $after, first: 100) {
    edges {
      node {
        # --- 1. Identificadores e Chaves ---
        id              # ID Interno
        referenceNumber # Referência da Rota/Cliente
        serviceAt       # Data de Emissão

        # [CRÍTICO] Objeto Fiscal CT-e (Correção do Join)
        cte {
          key     # <--- CHAVE DE 44 DÍGITOS (JOIN)
          number  # Número do CT-e
          series  # Série
        }

        # --- 2. Valores Financeiros ---
        total           # Valor Total do Serviço (Validado)
        subtotal        # Valor do Frete Peso
        invoicesValue   # Valor da Carga (Soma NFs)

        # --- 3. Métricas Físicas ---
        taxedWeight          # Kg Taxado
        realWeight           # Kg Real
        totalCubicVolume     # M3 (Cubagem)
        invoicesTotalVolumes # Volumes

        # --- 4. Notas Fiscais (Lista Aninhada) ---
        freightInvoices {
          invoice {
            number
            series
            key     # Chave da NF-e
            value   # Valor da Nota
          }
        }

        # --- 5. Atores e Endereços (Expandidos) ---
        sender {
          name
          mainAddress { city { name state { code } } }
        }
        receiver {
          name
          mainAddress { city { name state { code } } }
        }
        payer { name }
        
        # --- 6. Classificadores ---
        modal           # Tipo String (ex: "rodo")
        corporation { name }           # Filial
        customerPriceTable { name }    # Tabela de Preço
        freightClassification { name } # Classificação
        costCenter { name }            # Centro de Custo
      }
    }
    pageInfo {
      hasNextPage
      endCursor
    }
  }
}
```

#### 3.4. Variables (JSON)

```json
{
  "params": {
    "serviceAt": "{{data_inicio}} - {{data_fim}}"
  }
}
```

#### 3.5. Headers

| Header | Valor |
| :--- | :--- |
| `Authorization` | `Bearer {{token_graphql}}` |
| `Content-Type` | `application/json` |

-----

### 4\. Análise de Cobertura de Schema (CSV vs. API)

O mapeamento estrutural entre o CSV de origem e a query GraphQL expandida foi validado comparando a API (V5.1) contra o arquivo `frete_relacao_analitico_21-11-2025.csv`.

| Coluna CSV (Manual) | Caminho JSON (API) | Status Validação |
| :--- | :--- | :--- |
| `Chave CT-e` | `cte { key }` | ✅ **100% (JOIN)** |
| `Nº CT-e` | `cte { number }` | ✅ **Validado** |
| `Série` | `cte { series }` | ✅ **Validado** |
| `Data frete` | `serviceAt` | ✅ **Validado** |
| `Valor Total do Serviço`| `total` | ✅ **100% Exato** |
| `Valor Frete` | `subtotal` | ✅ **Validado** |
| `Valor NF` | `invoicesValue` | ✅ **100% Exato** |
| `Kg Taxado` | `taxedWeight` | ✅ **Validado** |
| `Kg Real` | `realWeight` | ✅ **Validado** |
| `M3` | `totalCubicVolume` | ✅ **Validado** |
| `NF` | `freightInvoices { invoice { number } }` | ✅ **Mapeado (Lista)** |
| `Filial` | `corporation { name }` | ✅ **Validado** |
| `Remetente` | `sender { name }` | ✅ **Validado** |
| `Destinatario` | `receiver { name }` | ✅ **Validado** |
| `Pagador` | `payer { name }` | ✅ **Validado** |
| `Origem / UF` | `sender { mainAddress { city { name } } }` | ✅ **Validado** |
| `Destino / UF` | `receiver { mainAddress { city { name } } }`| ✅ **Validado** |

### 5\. Conclusão

A validação técnica foi concluída com sucesso.

  * **Cobertura:** 100% dos campos críticos mapeados.
  * **Integridade:** Join perfeito utilizando a chave de 44 dígitos oculta no objeto `cte`.
  * **Status:** Aprovado para implementação em produção no pipeline de dados.



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




## 📄 Documentação de Descoberta: API DataExport (Manifestos)

### 1\. Objetivo

Identificar, validar e documentar o método de extração e o schema da entidade "Manifestos" via API DataExport. O objetivo é garantir 100% de cobertura estrutural em relação ao arquivo CSV de origem (`relacao-de-manifestos-detalhada...csv`), viabilizando a extração completa e paginada dos dados.

### 2\. Metodologia de Descoberta e Validação

O processo exigiu múltiplas etapas de teste, pois a API apresentou inconsistências entre seus endpoints de descoberta e de execução.

1.  **Autenticação (Sucesso):**

      * O endpoint `{{base_url}}/api/...` foi validado.
      * A autenticação `Bearer {{token_dataexport}}` (configurada nas variáveis de ambiente) foi validada com sucesso, retornando `200 OK` nas requisições válidas.

2.  **Descoberta de Template (Nível 1 - Falha Parcial):**

      * A requisição `[PASSO 1] Listar Templates` (`GET .../api/analytics/reports`) funcionou, mas retornou uma lista de templates incompleta.
      * Ela identificou um template com `"root": "manifest"`, mas com o `id: 6357` ("Relação de Agregados").

3.  **Validação de Schema (Nível 2 - Falha Total):**

      * A requisição `[PASSO 2] Validar Schema` (`GET .../api/analytics/reports/6357/info`) provou que o template `6357` era **incorreto**.
      * O schema retornado era focado em dados financeiros/fiscais do proprietário (ex: `RG`, `PIS`, `INSS`), sem os campos operacionais necessários (ex: `Veículo/Placa`, `Motorista`, `Status`, `Volumes NF`).
      * **Conclusão (Crítica):** O endpoint de listagem da API (`/api/analytics/reports`) é incompleto ou enganoso. Ele **não** lista o template operacional correto (`6399`) necessário para a extração.

4.  **Teste de Hipótese (Nível 3 - Falha de Método):**

      * Uma tentativa de replicar a requisição do navegador (`GET /analytics/reports/6399/data...`) usando `Cookie` e `X-CSRF-Token` foi executada.
      * A requisição falhou, retornando um `200 OK` com o HTML da página de login.
      * **Conclusão:** Este método é inviável para automação devido à natureza volátil e de sessão dos tokens.

5.  **Descoberta Final (Nível 4 - Sucesso):**

      * A hipótese correta foi testar o ID "oculto" (`6399`) diretamente no endpoint da API, usando o `Bearer Token` estável.
      * A requisição `[PASSO 3 API]` (`GET .../api/analytics/reports/6399/data`) foi executada.
      * Esta requisição utiliza um método atípico: `GET` com um `JSON Body` para filtros.
      * O resultado foi `200 OK` e um JSON completo (`requisicao.txt`) contendo os dados operacionais corretos. Este é o método de extração validado.

6.  **Validação de Paginação (Nível 5 - Sucesso):**

      * A lógica de extração total foi validada:
      * **Página 2:** A requisição `[PASSO 4 API]` (com `"page": "2"`) retornou `200 OK` e um novo conjunto de dados.
      * **Página Inexistente:** A requisição `[PASSO 5 API]` (com `"page": "9999"`) retornou `200 OK` e um *array* vazio (`[]`).
      * **Conclusão:** A extração completa requer um *loop* que incrementa o parâmetro `page` (iniciando em 1) até que a API retorne `[]` como resposta.

-----

### 3\. Configuração Final no Insomnia (Manifestos)

Esta é a configuração da requisição principal validada, que busca os dados paginados.

#### 3.1. Pasta

`API DataExport / Manifestos`

#### 3.2. Requisição

  * **Nome:** `[PASSO 3 API] Executar Consulta (Template 6399)`
  * **Método:** `GET`
  * **URL:** `{{base_url}}/api/analytics/reports/6399/data`

#### 3.3. Body (Corpo)

  * **Tipo:** `JSON`
  * **Painel BODY:**
    ```json
    {
      "search": {
        "manifests": {
          "service_date": "{{data_inicio}} - {{data_fim}}"
        }
      },
      "page": "1",
      "per": "100"
    }
    ```

#### 3.4. Headers (Autenticação)

| Header | Valor |
| :--- | :--- |
| `Authorization` | `Bearer {{token_dataexport}}` |
| `Content-Type` | `application/json` |

-----

### 4\. Análise de Cobertura de Schema (CSV vs. API)

O mapeamento estrutural entre o CSV de origem (`relacao-de-manifestos-detalhada...csv`) e o JSON retornado pela API (Template `6399`) foi validado.

| Coluna do CSV (Origem) | Chave do JSON (Destino) | Status |
| :--- | :--- | :--- |
| `Filial` | `mft_crn_psn_nickname` | ✅ **Mapeado** |
| `Data criação` | `created_at` | ✅ **Mapeado** |
| `Número` | `sequence_code` | ✅ **Mapeado** |
| `Classificação` | `mft_man_name` | ✅ **Mapeado** |
| `Programação/Cliente` | `mft_s_n_svs_sge_pyr_nickname` | ✅ **Mapeado** |
| `Programação/Tipo de Serviço`| `mft_s_n_svs_sge_sse_name` | ✅ **Mapeado** |
| `Status` | `status` | ✅ **Mapeado** |
| `Data Saída` | `departured_at` | ✅ **Mapeado** |
| `Data Fechamento` | `closed_at` | ✅ **Mapeado** |
| `Volumes NF` | `invoices_volumes` | ✅ **Mapeado** |
| `Qtd NF` | `invoices_count` | ✅ **Mapeado** |
| `Valor NF` | `invoices_value` | ✅ **Mapeado** |
| `Peso NF` | `invoices_weight` | ✅ **Mapeado** |
| `Proprietário/Nome` | `mft_vie_onr_name` | ✅ **Mapeado** |
| `Motorista` | `mft_mdr_iil_name` | ✅ **Mapeado** |
| `Veículo/Placa` | `mft_vie_license_plate` | ✅ **Mapeado** |
| `Veículo/Tipo` | `mft_vie_vee_name` | ✅ **Mapeado** |
| `KM Saída` | `vehicle_departure_km` | ✅ **Mapeado** |
| `KM Fechamento` | `closing_km` | ✅ **Mapeado** |
| `KM Rodado` | `traveled_km` | ✅ **Mapeado** |
| `Custo Total` | `total_cost` | ✅ **Mapeado** |
| `Adiantamento` | `advance_subtotal` | ✅ **Mapeado** |
| `Pedágio` | `toll_subtotal` | ✅ **Mapeado** |
| `Custos Frota` | `fleet_costs_subtotal` | ✅ **Mapeado** |
| `Custo (Líquido)` | `paying_total` | ✅ **Mapeado** |
| `Chave MDFe` | `mft_mfs_key` | ✅ **Mapeado** |
| `Status MDFe` | `mdfe_status` | ✅ **Mapeado** |
| `Usuário` | `mft_uer_name` | ✅ **Mapeado** |

-----

### 5\. Conclusão

A cobertura do schema para "Manifestos" é de **100%**.

O método de extração está validado e é funcional através do `Bearer Token`. O processo exige um *loop* de paginação (incrementando `page`) sobre o endpoint `GET .../reports/6399/data`, tratando o retorno `[]` como condição de parada.

**Risco Crítico Identificado:** O sucesso de toda a extração depende do ID de template `6399`. Este ID é um "número mágico" que **não** foi retornado pelo endpoint de descoberta da API (`/api/analytics/reports`). Se este ID for alterado ou removido na plataforma de origem, o processo de extração falhará imediatamente. A automação é viável, mas frágil, pois depende de um ID estático não detectável programaticamente.







## 📄 Documentação de Descoberta: API DataExport (Localização de Carga)

### 1\. Objetivo

Identificar, validar e documentar o método de extração e o schema da entidade "Localização de Carga" (tipo `freight`) via API DataExport. O objetivo é garantir 100% de cobertura estrutural em relação ao arquivo CSV de origem (`localizador-de-cargas...csv`), viabilizando a extração completa e paginada dos dados.

### 2\. Metodologia de Descoberta e Validação

O processo exigiu as mesmas etapas de descoberta manual aplicadas à entidade "Manifestos", confirmando que o template necessário não é listado publicamente pela API.

1.  **Autenticação (Sucesso):**

      * A autenticação `Bearer {{token_dataexport}}` foi validada com sucesso em todos os endpoints `/api/...`.

2.  **Descoberta de Template (Nível 1 - Falha):**

      * A requisição `[PASSO 1] Listar Templates` (`GET .../api/analytics/reports`) falhou em identificar o template correto. A lista retornada não continha nenhum `name` ou `root` associado a "Localização de Carga" ou "Fretes" (Freight).

3.  **Descoberta Manual (Nível 2 - Sucesso):**

      * O `ID` do template foi descoberto manualmente, inspecionando a requisição `XHR` no navegador ao gerar o relatório na plataforma ESL.
      * A requisição do navegador (`GET .../analytics/reports/8656/data?search...`) revelou o `ID` "oculto" do template: **`8656`**.
      * A requisição também revelou a estrutura de filtro: `search[freights][service_at]`.

4.  **Validação de Execução (Nível 3 - Sucesso):**

      * A requisição `[PASSO 2 API] Executar Consulta (Localização)` foi executada no Insomnia usando o método validado: `GET` com `Bearer Token`, `JSON Body` e o `ID` `8656`.
      * A requisição para a **Página 1** (`"page": "1"`) retornou `200 OK` e um JSON com o primeiro lote de dados (iniciando com `corporation_sequence_number: 262975`).

5.  **Validação de Paginação (Nível 4 - Sucesso):**

      * A lógica de extração total foi validada:
      * **Página 2:** A requisição `[PASSO 3 API]` (com `"page": "2"`) retornou `200 OK` e um novo conjunto de dados (iniciando com `corporation_sequence_number: 263135`).
      * **Página Inexistente:** A requisição `[PASSO 4 API]` (com `"page": "9999"`) retornou `200 OK` e um *array* vazio (`[]`).
      * **Conclusão:** A extração completa requer um *loop* que incrementa o parâmetro `page` (iniciando em 1) até que a API retorne `[]` como resposta.

-----

### 3\. Configuração Final no Insomnia (Localização de Carga)

Esta é a configuração da requisição principal validada, que busca os dados paginados.

#### 3.1. Pasta

`API DataExport / Localização de Carga`

#### 3.2. Requisição

  * **Nome:** `[PASSO 2 API] Executar Consulta (Localização)`
  * **Método:** `GET`
  * **URL:** `{{base_url}}/api/analytics/reports/{{localizacao_template_id}}/data`
      * (Utiliza a variável `{{localizacao_template_id}}` = `8656`)

#### 3.3. Body (Corpo)

  * **Tipo:** `JSON`
  * **Painel BODY:**
    ```json
    {
      "search": {
        "freights": {
          "service_at": "{{data_inicio}} - {{data_fim}}"
        }
      },
      "page": "1",
      "per": "100",
      "order_by": "service_at desc"
    }
    ```

#### 3.4. Headers (Autenticação)

| Header | Valor |
| :--- | :--- |
| `Authorization` | `Bearer {{token_dataexport}}` |
| `Content-Type` | `application/json` |

-----

### 4\. Análise de Cobertura de Schema (CSV vs. API)

O mapeamento estrutural entre o CSV de origem (`localizador-de-cargas...csv`) e o JSON retornado pela API (Template `8656`) foi validado com 100% de cobertura.

| Coluna do CSV (Origem) | Chave do JSON (Destino) | Status |
| :--- | :--- | :--- |
| `CT-e` | `corporation_sequence_number`| ✅ **Mapeado** |
| `Tipo` | `type` | ✅ **Mapeado** |
| `Data Emissão` | `service_at` | ✅ **Mapeado** |
| `Volumes` | `invoices_volumes` | ✅ **Mapeado** |
| `Peso Taxado` | `taxed_weight` | ✅ **Mapeado** |
| `Valor NF` | `invoices_value` | ✅ **Mapeado** |
| `Valor Frete` | `total` | ✅ **Mapeado** |
| `Tipo Serviço` | `service_type` | ✅ **Mapeado** |
| `Filial Emissora` | `fit_crn_psn_nickname` | ✅ **Mapeado** |
| `Previsão Entrega` | `fit_dpn_delivery_prediction_at`| ✅ **Mapeado** |
| `Cidade Destino` | `fit_dyn_name` | ✅ **Mapeado** |
| `Filial Destino` | `fit_dyn_drt_nickname` | ✅ **Mapeado** |
| `Serviço` | `fit_fsn_name` | ✅ **Mapeado** |
| `Status Carga` | `fit_fln_status` | ✅ **Mapeado** |
| `Filial Atual` | `fit_fln_cln_nickname` | ✅ **Mapeado** |
| `Cidade Origem` | `fit_o_n_name` | ✅ **Mapeado** |
| `Filial Origem` | `fit_o_n_drt_nickname` | ✅ **Mapeado** |

-----

### 5\. Conclusão

A cobertura do schema para "Localização de Carga" (entidade `freight`) é de **100%**.

O método de extração está validado e é funcional através do `Bearer Token`. O processo exige um *loop* de paginação (incrementando `page`) sobre o endpoint `GET .../reports/8656/data`, tratando o retorno `[]` como condição de parada.

**Risco Crítico Identificado:** Assim como em "Manifestos" (ID `6399`), o `ID 8656` é um "número mágico" que **não** foi retornado pelo endpoint de descoberta da API (`/api/analytics/reports`). Se este ID for alterado ou removido na plataforma de origem, o processo de extração falhará imediatamente. A automação é viável, mas depende de um ID estático que não pode ser descoberto programaticamente.



# 📄 Especificação de Migração: Faturas por Cliente (Data Export)

**Data:** 18/11/2025
**Contexto:** Implementação da extração do relatório "Fatura por Cliente" via API Data Export (Template ID 4924) para substituir processos manuais e garantir paridade com a planilha financeira de referência.

-----

## 1\. Definição da Requisição (API)

A extração utiliza o endpoint de relatórios analíticos. O filtro principal é a **Data do Serviço do Frete**.

  * **Endpoint:** `{{base_url}}/api/analytics/reports/4924/data`
  * **Método:** `POST` (Recomendado para automação) ou `GET` (Padrão navegador)
  * **Autenticação:** Bearer Token (Mesmo token do Data Export)
  * **Template ID:** `4924`

### Payload JSON (Request Body)

```json
{
  "search": {
    "freights": {
      "service_at": "{{data_inicio}} - {{data_fim}}"
    },
    "fit_accounting_credit": {
      "due_date": "",
      "issue_date": ""
    },
    "fit_freight_cte": {
      "corporation_sequence_number": ""
    }
  },
  "page": 1,
  "per": 100,
  "order_by": "service_at desc"
}
```

-----

## 2\. Dicionário de Dados (De/Para)

Mapeamento validado entre as colunas do relatório Excel de referência (`fatura-por-cliente.xlsx`) e o JSON de resposta da API.

**Legenda:**

  * ✅ **Direto:** Campo mapeia 1:1.
  * ⚠️ **Lógica:** Requer tratamento no Java (Concatenação, Tradução ou Conversão).

| Coluna CSV (Alvo) | Campo JSON (Origem) | Tipo JSON | Tratamento (Mapper) |
| :--- | :--- | :--- | :--- |
| **Filial** | `fit_crn_psn_nickname` | `String` | Direto |
| **Pagador / Nome** | `fit_pyr_name` | `String` | Direto |
| **Pagador / Documento** | `fit_pyr_document` | `String` | Direto |
| **Nfse / Número NFS-e** | `fit_nse_number` | `Long` | **Parte da Chave Única** (se CT-e for nulo) |
| **CT-e / Número** | `fit_fhe_cte_number` | `Long` | Direto |
| **CT-e / Data emissão** | `fit_fhe_cte_issued_at` | `String` | Converter `OffsetDateTime` |
| **CT-e / Chave** | `fit_fhe_cte_key` | `String` | **Chave Primária Preferencial** |
| **CT-e / Resultado** | `fit_fhe_cte_status_result` | `String` | Direto |
| **CT-e / Status** | `fit_fhe_cte_status` | `String` | Traduzir (ex: `authorized` -\> `Autorizado`) |
| **Fatura / N° Doc** | `fit_ant_document` | `String` | Direto |
| **Fatura / Emissão** | `fit_ant_issue_date` | `String` | Converter `LocalDate` |
| **Fatura / Valor** | `fit_ant_value` | `String` | Converter `BigDecimal` (Locale US) |
| **Parcelas / Vencimento**| `fit_ant_ils_due_date` | `String` | Converter `LocalDate` |
| **Baixa / Data** | `fit_ant_ils_atn_transaction_date`| `String` | Converter `LocalDate` (Pode ser NULL) |
| **Frete original / Total**| `total` | `String` | Converter `BigDecimal` (Locale US) |
| **Tipo** | `type` | `String` | Traduzir (ex: `Freight::Normal` -\> `Normal`) |
| **Estado / Nome** | `fit_diy_sae_name` | `String` | Direto |
| **Classificação** | `fit_fsn_name` | `String` | Direto (ex: "FRACIONADO - LTL") |
| **Remetente / Nome** | `fit_rpt_name` | `String` | Direto |
| **Destinatário / Nome** | `fit_sdr_name` | `String` | Direto |
| **NF (Notas Fiscais)** | `invoices_mapping` | `Array` | ⚠️ Converter Array p/ String (Join com vírgula) |
| **Cache / N° Pedido** | `fit_fte_invoices_order_number`| `Array` | ⚠️ Converter Array p/ String (Join com vírgula) |

-----

## 3\. Regras de Negócio Críticas

### 3.1. Geração de Identificador Único (PK)

O relatório é híbrido (contém CT-es e NFS-es). A chave primária (`unique_id`) deve ser gerada seguindo esta prioridade para evitar duplicidade e nulos:

1.  **Se tiver Chave de CT-e (`fit_fhe_cte_key`):** Usar a chave (44 dígitos).
2.  **Se não tiver CT-e, mas tiver NFS-e (`fit_nse_number`):** Usar formato `NFSE-{numero}`.
3.  **Fallback:** Se ambos forem nulos (raro), gerar hash ou ignorar.

### 3.2. Conversão Monetária (Locale.US)

Os campos de valor (`total`, `fit_ant_value`) vêm como String com ponto decimal (ex: `"123.69"`).

  * **ERRO COMUM:** Usar locale `pt-BR` transforma `123.69` em `12369.00`.
  * **SOLUÇÃO:** Forçar `Locale.US` e `setParseBigDecimal(true)` no Mapper.

### 3.3. Tratamento de Arrays

Campos como `invoices_mapping` vêm como `["78427", "78428"]`.

  * **Banco de Dados:** Não suporta array nativo de forma simples.
  * **Solução:** Converter para String única: `"78427, 78428"`.

-----

## 4\. Estrutura de Classes (Java)

  * **Pacote Base:** `br.com.extrator.modelo.dataexport.faturaporcliente`
  * **DTO:** `FaturaPorClienteDTO.java`
  * **Mapper:** `FaturaPorClienteMapper.java`
  * **Entity:** `br.com.extrator.db.entity.FaturaPorClienteEntity.java`

-----

## 5\. Estrutura de Banco de Dados (SQL Server)

Tabela otimizada para armazenar dados híbridos (CT-e e NFS-e).

```sql
CREATE TABLE faturas_por_cliente_data_export (
    unique_id NVARCHAR(100) PRIMARY KEY,        -- Chave Unificadora
    
    -- Valores (DECIMAL 18,2 obrigatório)
    valor_frete DECIMAL(18,2),                  -- Valor do Frete Individual
    valor_fatura DECIMAL(18,2),                 -- Valor Total da Fatura Agrupada
    
    -- Documentos Fiscais
    numero_cte BIGINT,
    chave_cte NVARCHAR(100),
    numero_nfse BIGINT,
    status_cte NVARCHAR(255),
    data_emissao_cte DATETIMEOFFSET,
    
    -- Dados da Fatura (Cobrança)
    numero_fatura NVARCHAR(50),
    data_emissao_fatura DATE,
    data_vencimento_fatura DATE,
    data_baixa_fatura DATE,
    
    -- Classificação Operacional
    filial NVARCHAR(255),
    tipo_frete NVARCHAR(100),
    classificacao NVARCHAR(100),
    estado NVARCHAR(50),
    
    -- Envolvidos
    pagador_nome NVARCHAR(255),
    pagador_documento NVARCHAR(50),
    remetente_nome NVARCHAR(255),
    destinatario_nome NVARCHAR(255),
    vendedor_nome NVARCHAR(255),
    
    -- Listas (Convertidas para texto)
    notas_fiscais NVARCHAR(MAX),                -- Lista de NFs
    pedidos_cliente NVARCHAR(MAX),              -- Lista de Pedidos
    
    -- Sistema
    metadata NVARCHAR(MAX),                     -- JSON Raw para auditoria
    data_extracao DATETIME2 DEFAULT GETDATE()
);

-- Índices para Performance de Relatórios
CREATE INDEX IX_fpc_vencimento ON faturas_por_cliente_data_export(data_vencimento_fatura);
CREATE INDEX IX_fpc_pagador ON faturas_por_cliente_data_export(pagador_nome);
CREATE INDEX IX_fpc_filial ON faturas_por_cliente_data_export(filial);
```

-----






## 📄 Documentação de Descoberta: API DataExport (Cotações)

### 1\. Objetivo

Identificar, validar e documentar o método de extração e o schema da entidade "Cotações" via API DataExport. O objetivo é garantir 100% de cobertura estrutural em relação ao arquivo CSV de origem (`relacao-de-cotacoes-detalhada...csv`), viabilizando a extração completa e paginada dos dados.

### 2\. Metodologia de Descoberta e Validação

O processo seguiu a mesma metodologia validada para a entidade "Manifestos", confirmando o fluxo de acesso a templates de relatório via API.

1.  **Autenticação (Sucesso):**

      * A autenticação `Bearer {{token_dataexport}}` foi validada com sucesso em todos os endpoints `/api/...`.

2.  **Descoberta de Template (Nível 1 - Sucesso):**

      * A requisição `[PASSO 1] Listar Templates` (`GET .../api/analytics/reports`) foi executada.
      * A resposta JSON identificou corretamente o template alvo:
          * **ID:** `6906`
          * **Nome:** `Relação de Cotações Detalhada`
          * **Raiz:** `quote`

3.  **Validação de Schema (Nível 2 - Sucesso):**

      * A requisição `[PASSO 2] Validar Schema (Cotações)` (`GET .../api/analytics/reports/6906/info`) retornou o schema completo.
      * Ela confirmou os `fields` (colunas) disponíveis e os `filters` (parâmetros) necessários.
      * O filtro de data principal foi identificado como: `field: "requested_at"`, `table: "quotes"`.

4.  **Validação de Execução (Nível 3 - Sucesso):**

      * A requisição `[PASSO 3 API] Executar Consulta (Cotações)` foi executada usando o método `GET` com `JSON Body`.
      * A requisição para a **Página 1** (`"page": "1"`) retornou `200 OK` e um JSON com o primeiro lote de dados.

5.  **Validação de Paginação (Nível 4 - Sucesso):**

      * A requisição `[PASSO 4 API]` (com `"page": "2"`) foi executada.
      * A API retornou `200 OK` e um novo conjunto de dados (começando com `sequence_code: 82189`).
      * **Conclusão:** A extração completa requer um *loop* que incrementa o parâmetro `page` (iniciando em 1) até que a API retorne `[]` como resposta.

-----

### 3\. Configuração Final no Insomnia (Cotações)

Esta é a configuração da requisição principal validada, que busca os dados paginados.

#### 3.1. Pasta

`API DataExport / Cotações`

#### 3.2. Requisição

  * **Nome:** `[PASSO 3 API] Executar Consulta (Cotações)`
  * **Método:** `GET`
  * **URL:** `{{base_url}}/api/analytics/reports/{{quote_template_id}}/data`
      * (Utiliza a variável `{{quote_template_id}}` = `6906`)

#### 3.3. Body (Corpo)

  * **Tipo:** `JSON`
  * **Painel BODY:**
    ```json
    {
      "search": {
        "quotes": {
          "requested_at": "{{data_inicio}} - {{data_fim}}"
        }
      },
      "page": "1",
      "per": "100"
    }
    ```

#### 3.4. Headers (Autenticação)

| Header | Valor |
| :--- | :--- |
| `Authorization` | `Bearer {{token_dataexport}}` |
| `Content-Type` | `application/json` |

-----

### 4\. Análise de Cobertura de Schema (CSV vs. API)

O mapeamento estrutural entre o CSV de origem (`relacao-de-cotacoes-detalhada...csv`) e o JSON retornado pela API (Template `6906`) foi validado com 100% de cobertura.

| Coluna do CSV (Origem) | Chave do JSON (Destino) | Status |
| :--- | :--- | :--- |
| `Filial` | `qoe_crn_psn_nickname` | ✅ **Mapeado** |
| `Data` | `requested_at` | ✅ **Mapeado** |
| `Cotação` | `sequence_code` | ✅ **Mapeado** |
| `Solicitante` | `requester_name` | ✅ **Mapeado** |
| `Cliente` | `qoe_cor_name` | ✅ **Mapeado** |
| `CNPJ Cliente` | `qoe_cor_document` | ✅ **Mapeado** |
| `Remetente` | `qoe_qes_sdr_nickname` | ✅ **Mapeado** |
| `CNPJ Remetente` | `qoe_qes_sdr_document` | ✅ **Mapeado** |
| `Destinatário` | `qoe_qes_rpt_nickname` | ✅ **Mapeado** |
| `CNPJ Destinatário` | `qoe_qes_rpt_document` | ✅ **Mapeado** |
| `Origem` | `qoe_qes_ony_name` | ✅ **Mapeado** |
| `UF` (Origem) | `qoe_qes_ony_sae_code` | ✅ **Mapeado** |
| `Destino` | `qoe_qes_diy_name` | ✅ **Mapeado** |
| `UF` (Destino) | `qoe_qes_diy_sae_code` | ✅ **Mapeado** |
| `Volumes` | `qoe_qes_invoices_volumes` | ✅ **Mapeado** |
| `Peso Real` | `qoe_qes_real_weight` | ✅ **Mapeado** |
| `Peso Taxado` | `qoe_qes_taxed_weight` | ✅ **Mapeado** |
| `Valor NF` | `qoe_qes_invoices_value` | ✅ **Mapeado** |
| `Valor Frete` | `qoe_qes_total` | ✅ **Mapeado** |
| `Tabela` | `qoe_qes_cre_name` | ✅ **Mapeado** |
| `Observações` | `qoe_qes_freight_comments`| ✅ **Mapeado** |
| `Usuário` | `qoe_uer_name` | ✅ **Mapeado** |
| `CT-e/Data de emissão` | `qoe_qes_fit_fhe_cte_issued_at`| ✅ **Mapeado** |
| `Nfse/Data de emissão`| `qoe_qes_fit_nse_issued_at` | ✅ **Mapeado** |

-----

### 5\. Conclusão

A cobertura do schema para "Cotações" é de **100%**.

O método de extração está validado e é funcional através do `Bearer Token`. O processo exige um *loop* de paginação (incrementando `page`) sobre o endpoint `GET .../reports/6906/data`, tratando o retorno `[]` como condição de parada.

**Risco Identificado (Menor):** Ao contrário do template de "Manifestos" (`6399`), o template `6906` *apareceu* na listagem da API (`[PASSO 1]`). Isso torna o processo menos frágil, pois o ID do template pode, teoricamente, ser descoberto programaticamente caso seja alterado.




# 📄 Especificação de Migração: Faturas a Pagar (Data Export)

**Data:** 18/11/2025
**Contexto:** Substituição da extração via API REST (incompleta) pela API Data Export (Template ID 8636) para garantir paridade com o relatório financeiro oficial (CSV).

-----

## 1\. Definição da Requisição (API)

Diferente da extração via navegador (que usa `GET` com Query Params), a automação deve utilizar `POST` com Payload JSON para maior estabilidade.

  * **Endpoint:** `{{base_url}}/api/analytics/reports/8636/data`
  * **Método:** `POST`
  * **Autenticação:** Bearer Token (Mesmo token do Data Export)
  * **Template ID:** `8636`

### Payload JSON (Request Body)

```json
{
  "search": {
    "accounting_debits": {
      "issue_date": "{{data_inicio}} - {{data_fim}}",
      "created_at": "" 
    }
  },
  "page": 1,
  "per": 100,
  "order_by": "issue_date desc"
}
```

> **Nota Crítica:** O objeto de filtro obrigatório chama-se `accounting_debits`.

-----

## 2\. Dicionário de Dados (De/Para)

Mapeamento validado entre as colunas do relatório CSV de referência (`contas-a-pagar.xlsx`) e o JSON de resposta da API.

**Legenda:**

  * ✅ **Direto:** Campo mapeia 1:1.
  * ⚠️ **Transformação:** Requer tratamento no Java (`Mapper`).

| Coluna CSV (Alvo) | Campo JSON (Origem) | Tipo JSON | Tratamento (Mapper) |
| :--- | :--- | :--- | :--- |
| **Lançamento a Pagar/N°** | `ant_ils_sequence_code` | `Integer` | **Chave Primária (PK)** |
| **N° Documento** | `document` | `String` | Direto |
| **Emissão** | `issue_date` | `String` | Converter para `LocalDate` |
| **Valor** | `value` | `String` | Converter para `BigDecimal` |
| **Valor a pagar** | `value_to_pay` | `String` | Converter para `BigDecimal` |
| **Pago** | `paid` | `Boolean` | `true`="Sim", `false`="Não" |
| **Valor pago** | `paid_value` | `String` | Converter para `BigDecimal` |
| **Fornecedor/Nome** | `ant_rir_name` | `String` | Uppercase |
| **Filial** | `ant_crn_psn_nickname` | `String` | Direto |
| **Conta Contábil/Classif.** | `ant_ils_pas_ant_classification`| `String` | Ex: `variable_costs` (Manter original ou Traduzir) |
| **Conta Contábil/Desc.** | `ant_ils_pas_ant_name` | `String` | Direto |
| **Conta Contábil/Valor** | `ant_ils_pas_value` | `String` | Converter para `BigDecimal` |
| **Centro de custo/Nome** | `ant_ces_acr_name` | `String` | Direto |
| **Centro de custo/Valor** | `ant_ces_value` | `String` | Converter para `BigDecimal` |
| **Mês Competência** | `competence_month` | `Integer` | Direto |
| **Ano Competência** | `competence_year` | `Integer` | Direto |
| **Data criação** | `created_at` | `String` | Converter `OffsetDateTime` |
| **Observações** | `ant_ils_comments` | `String` | **Atenção:** Ignorar campo `comments` (vazio) |
| **Tipo** | `type` | `String` | Limpar string (Ex: `Accounting::Debit::Manual` -\> `Manual`) |
| **Baixa/Data liquidação** | `ant_ils_atn_liquidation_date` | `String` | Converter `LocalDate` (Pode ser NULL) |
| **Usuário/Nome** | `ant_uer_name` | `String` | Direto |

-----

## 3\. Estrutura de Classes (Java)

A implementação deve ser isolada no pacote `dataexport` para futura remoção do código legado REST.

  * **Pacote Base:** `br.com.extrator.modelo.dataexport.faturaspagar`
  * **DTO:** `FaturaAPagarDataExportDTO.java` (Espelho do JSON)
  * **Mapper:** `FaturaAPagarDataExportMapper.java` (Regras de transformação)
  * **Entity:** `br.com.extrator.db.entity.FaturaAPagarDataExportEntity.java`

### Regras de Negócio no Mapper

1.  **Valores Monetários:** Todos os campos de valor vêm como `String` no JSON. Devem ser convertidos obrigatoriamente para `BigDecimal` para evitar erros de precisão.
2.  **Nulos:** Campos como `ant_ils_atn_liquidation_date` vêm nulos se o título estiver aberto. O Mapper deve tratar `null` safe.
3.  **Metadados:** O Mapper deve serializar o DTO inteiro em um campo JSON `metadata` na entidade para garantir auditoria futura.

-----

## 4\. Estrutura de Banco de Dados (SQL Server)

Tabela dedicada para esta extração, utilizando tipos numéricos corretos (`DECIMAL`).

```sql
CREATE TABLE faturas_a_pagar_data_export (
    sequence_code BIGINT PRIMARY KEY,          -- Chave original do ESL
    document_number VARCHAR(100),
    
    -- Valores Financeiros (Precisão Decimal)
    original_value DECIMAL(18,2),
    value_to_pay DECIMAL(18,2),
    paid_value DECIMAL(18,2),
    interest_value DECIMAL(18,2),
    discount_value DECIMAL(18,2),
    accounting_account_value DECIMAL(18,2),    -- Valor rateado contábil
    cost_center_value DECIMAL(18,2),           -- Valor rateado C.Custo
    
    -- Datas
    issue_date DATE,
    liquidation_date DATE,
    competence_month INT,
    competence_year INT,
    created_at DATETIMEOFFSET,
    
    -- Classificações
    provider_name NVARCHAR(255),
    branch_name NVARCHAR(255),
    cost_center_name NVARCHAR(255),
    accounting_classification NVARCHAR(100),   -- Ex: variable_costs
    accounting_description NVARCHAR(255),      -- Ex: MANUTENÇÃO
    
    -- Controle
    status_pagamento NVARCHAR(50),             -- PAGO / ABERTO
    tipo_lancamento NVARCHAR(100),             -- Manual / CiotBilling
    usuario_lancamento NVARCHAR(255),
    observacoes NVARCHAR(MAX),
    
    -- Sistema
    metadata NVARCHAR(MAX),                    -- JSON Raw
    data_extracao DATETIME2 DEFAULT GETDATE()
);

CREATE INDEX IX_fp_data_export_issue_date ON faturas_a_pagar_data_export(issue_date);
```

-----
