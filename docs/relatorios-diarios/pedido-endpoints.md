# **Solicitação de Endpoints (Postman) para Replicar 8 Relatórios Manuais via API**

Prezada equipe de suporte ESL Cloud,

Estamos finalizando uma integração Java para extrair dados para nosso BI e precisamos de sua ajuda para garantir 100% de fidelidade de dados.

Nosso objetivo é replicar programaticamente os 8 relatórios manuais (CSV/XLSX) que extraímos do portal (lista de arquivos anexada).

Descobrimos uma incompatibilidade crítica em nossa implementação atual:

> O relatório manual `lancamentos-a-pagar_03-11-2025_17-55.csv` contém todos os tipos de lançamentos (Taxas de Banco como BANCO BRADESCO S.A, Vales como MTZ - RODOGARCIA..., e Faturas de Frete).

> No entanto, o endpoint da API REST que estamos usando (`/api/accounting/debit/billings`, conforme documentação) retorna apenas Faturas de Frete (tipo **CiotBilling** e **DriverBilling**).

Para resolver isso e garantir que nosso script puxe **absolutamente tudo**, solicitamos os detalhes técnicos exatos (Endpoints, Payloads, Nomes de Template) que o portal ESL utiliza para gerar cada um dos 8 relatórios listados abaixo.

---

## **1. Entidades da API REST**

*(Referência: `ClienteApiRest.java`)*

Para estas entidades, precisamos do endpoint **GET completo**, todos os **parâmetros de filtro** (especialmente filtros de data incremental como `since`, `issue_date_start`, `due_date_start`, etc.) e um exemplo de resposta JSON completo.

---

### **1.1 Lançamentos a Pagar**

* **Relatório Manual (Fonte da Verdade):** `lancamentos-a-pagar_03-11-2025_17-55.csv`
* **Solicitação:** Precisamos do endpoint “genérico” ou “pai” que o portal usa para gerar este relatório completo, que inclui **Taxas de Banco**, **Vales**, e **Faturas de Frete**.

**Informações solicitadas:**

* **URL do Endpoint:**
  Exemplo esperado:

  ```
  https://rodogarcia.eslcloud.com.br/api/accounting/debit/generic_entries
  ```
* **Todos os Parâmetros de Filtro:**
  Exemplo: `since`, `start_date`, `end_date`, `type`, etc.
* **Exemplo de Resposta JSON:**
  Um exemplo contendo um item de **Taxa de Banco** (ex: BANCO BRADESCO S.A), para identificar campos como `"type"` e estrutura completa.

---

### **1.2 Lançamentos a Receber**

* **Relatório Manual (Fonte da Verdade):** `lancamentos-a-receber_03-11-2025_17-53.csv`
* **Solicitação:** Suspeitamos que `/api/accounting/credit/billings` também seja incompleto. Precisamos do endpoint “genérico” usado pelo portal.

**Informações solicitadas:**

* **URL do Endpoint:**

  ```
  https://rodogarcia.eslcloud.com.br/api/...
  ```
* **Todos os Parâmetros de Filtro**
* **Exemplo de Resposta JSON** (completo, de um item do relatório)

---

### **1.3 Ocorrências**

* **Relatório Manual (Fonte da Verdade):** `historico_ocorrencia_analitico_03-11-2025_18-08.csv`
* **Solicitação:** O endpoint exato que o portal utiliza para o relatório *historico_ocorrencia_analitico*, que parece mais completo do que `/invoice_occurrences`.

**Informações solicitadas:**

* **URL do Endpoint:**

  ```
  https://rodogarcia.eslcloud.com.br/api/...
  ```
* **Todos os Parâmetros de Filtro:** `freight_id`, `cte_key`, datas, etc.
* **Exemplo de Resposta JSON**

---

## **2. Entidades da API GraphQL**

*(Referência: `ClienteApiGraphQL.java`)*

Para estas entidades, a URL é:

```
https://rodogarcia.eslcloud.com.br/graphql
```

Precisamos do **Payload completo (query + variáveis)** que o portal envia para gerar os relatórios analíticos.

---

### **2.1 Coletas**

* **Relatório Manual:** `coletas_analitico_03-11-2025_19-22.csv`
* **Informações solicitadas:**

  * **Query GraphQL exata**
  * **Variáveis da Query**
    Exemplo:

    ```graphql
    query {
      coletas(filter: {...}) {
        id
        numero_coleta
        ...
      }
    }
    ```

    ```json
    {"filter": {"requestDate": "..."}}
    ```
  * **Exemplo de Resposta JSON**

---

### **2.2 Fretes**

* **Relatório Manual:** `frete_relacao_analitico_03-11-2025_19-23.csv`
* **Informações solicitadas:**

  * **Query GraphQL exata**
  * **Variáveis da Query**
    Exemplo:

    ```graphql
    query {
      freight(filter: {...}) {
        edges {
          node {
            id
            ...
          }
        }
      }
    }
    ```

    ```json
    {"filter": {"serviceAt": "...", "corporationId": ...}}
    ```
  * **Exemplo de Resposta JSON**

---

## **3. Entidades da API Data Export**

*(Referência: `ClienteApiDataExport.java`)*

Nosso script já utiliza o fluxo de **4 passos**:

1. Consultar Template
2. Executar
3. Consultar Status
4. Baixar arquivo

Precisamos confirmar os **nomes exatos dos templates (case sensitive)** e os **endpoints desse fluxo**.

---

### **3.1 Cotações**

* **Relatório Manual:** `relacao-de-cotacoes-detalhada_...csv`
* **Solicitação:**

  * Nome exato do template (Ex: `"Relação de Cotações Detalhada"`)
  * Confirmação dos 4 endpoints do fluxo

---

### **3.2 Manifestos**

* **Relatório Manual:** `relacao-de-manifestos-detalhada_...csv`
* **Solicitação:**

  * Nome exato do template (Ex: `"Relação de Manifestos Detalhada"`)
  * Confirmação dos 4 endpoints

---

### **3.3 Localização de Cargas**

* **Relatório Manual:** `localizador-de-cargas_...csv`
* **Solicitação:**

  * Nome exato do template (Ex: `"Localizador de Cargas"`)
  * Confirmação dos 4 endpoints

---

Agradecemos antecipadamente. O fornecimento desta documentação completa com todos os endpoints e se possível coisas a mais para nos ajudar a extrair todos os dados possíveis de cada entidade.