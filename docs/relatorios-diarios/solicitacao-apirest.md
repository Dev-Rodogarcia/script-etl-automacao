### Especificação Técnica: Padronização de Endpoints de Relatório para Automação

**Para:** Equipe Técnica / Engenharia de API da ESL Cloud
**Assunto:** Especificação para Habilitação de `Bearer Token` em Endpoints de Relatório (`/report/.../analytical`)

### 1\. Resumo Técnico da Investigação

Após análise da plataforma, identificamos uma inconsistência crítica que impede a automação (via servidor-para-servidor) da extração de dados:

1.  **APIs `GET /api/...` (Via `Bearer Token`):** A autenticação funciona, mas os endpoints são **incompletos**. Eles retornam apenas uma fração dos dados necessários (ex: 30-40% de cobertura de schema em comparação com os relatórios CSV).
2.  **APIs `POST /report/...` (Via `Cookie`/`CSRF`):** Estes endpoints, descobertos via inspeção da interface web, são **completos** e retornam 100% dos campos necessários.
3.  **O Bloqueio:** Os endpoints `/report` (os únicos completos) não aceitam `Bearer Token`. Eles exigem autenticação de sessão (`Cookie` + `X-CSRF-Token`), o que é inviável para um *cron job* automatizado, pois requer login manual e intervenção humana para renovar sessões expiradas.

### 2\. Solicitação de Padronização

Para viabilizar a integração automatizada, solicitamos que os endpoints de relatório (`/report/.../analytical`) sejam padronizados para aceitar a mesma autenticação `Authorization: Bearer Token` já utilizada nos endpoints `/api/...`.

A autenticação via `Cookie`/`CSRF` não é uma solução viável para processos de servidor.

### 3\. Especificação Técnica Requerida

Abaixo estão as especificações exatas de como os três endpoints de relatório devem funcionar para atender aos nossos requisitos de extração.

-----

#### 3.1. Entidade: Lançamentos a Pagar

  * **Endpoint (Método `POST`):**

    ```
    {{base_url}}/report/accounting/debits/analytical
    ```

  * **Autenticação Requerida (Header):**

    ```
    Authorization: Bearer {{token_rest}}
    ```

  * **Headers Requeridos:**

    ```
    Content-Type: application/x-www-form-urlencoded
    Accept: application/json
    ```

  * **Parâmetros de Corpo (Form Data):**

      * `search[accounting_debits][issue_date]` (Ex: `29/10/2025 - 05/11/2025`)
      * `search[accounting_debits][corporation_id][]` (Ex: `385129`)
      * `page` (Ex: `1`)
      * `per` (Ex: `500`)
      * `order_by` (Ex: `report_issue_date desc`)

  * **Schema de Resposta (JSON) Requerido:**
    A resposta deve conter a estrutura `collection` com **todos** os 29 campos identificados (100% de cobertura do CSV), incluindo, mas não se limitando a:

      * `id`
      * `report_receiver_cnpj`
      * `report_receiver_name`
      * `report_document`
      * `report_installment_document`
      * `report_sequence_code`
      * `report_issue_date`
      * `report_original_due_date`
      * `report_competence`
      * `report_corporations_name`
      * `report_corporations_cnpj`
      * `report_value`
      * `report_interest_or_discount`
      * `report_value_to_pay`
      * `report_transaction_date`
      * `report_liquidation_date`
      * `report_payment_method`
      * `report_planning_managements_names` (Array)
      * `report_cost_centers_names` (Array)
      * `report_status`
      * `report_translated_status`
      * `report_expense_description`

-----

#### 3.2. Entidade: Lançamentos a Receber

  * **Endpoint (Método `POST`):**

    ```
    {{base_url}}/report/accounting/credits/analytical
    ```

  * **Autenticação Requerida (Header):**

    ```
    Authorization: Bearer {{token_rest}}
    ```

  * **Headers Requeridos:**

    ```
    Content-Type: application/x-www-form-urlencoded
    Accept: application/json
    ```

  * **Parâmetros de Corpo (Form Data):**

      * `search[accounting_credits][issue_date]` (Ex: `02/11/2025 - 03/11/2025`)
      * `search[accounting_credits][customer_id]` (Opcional)
      * `page` (Ex: `1`)
      * `per` (Ex: `600`)
      * `order_by` (Ex: `report_issue_date desc`)

  * **Schema de Resposta (JSON) Requerido:**
    A resposta deve conter a estrutura `collection` com **todos** os 28 campos identificados (100% de cobertura do CSV), incluindo:

      * `id`
      * `report_customer_cnpj`
      * `report_customer_name`
      * `report_billing_customer_email`
      * `report_document`
      * `report_installment_document`
      * `report_sequence_code`
      * `report_original_due_date`
      * `report_competence`
      * `report_issue_date`
      * `report_corporations_name`
      * `report_corporations_cnpj`
      * `report_check_number`
      * `report_due_date`
      * `report_value`
      * `report_interest_or_discount`
      * `report_value_to_pay`
      * `report_transaction_date`
      * `report_liquidation_date`
      * `report_accounting_transaction_bank`
      * `report_accounting_transaction_bank_account`
      * `report_payment_method`
      * `report_planning_managements_names` (Array)
      * `report_status`
      * `report_translated_status`

-----

#### 3.3. Entidade: Ocorrências

  * **Endpoint (Método `POST`):**

    ```
    {{base_url}}/report/invoice_occurrence/histories/analytical
    ```

  * **Autenticação Requerida (Header):**

    ```
    Authorization: Bearer {{token_rest}}
    ```

  * **Headers Requeridos:**

    ```
    Content-Type: application/x-www-form-urlencoded
    Accept: application/json
    ```

  * **Parâmetros de Corpo (Form Data):**

      * `search[date_type]` (Ex: `occurrence_at`)
      * `search[date_search]` (Ex: `27/10/2025 - 03/11/2025`)
      * `page` (Ex: `1`)
      * `per` (Ex: `4300`)
      * `order_by` (Ex: `report_occurrence_at desc`)

  * **Schema de Resposta (JSON) Requerido:**
    A resposta deve conter a estrutura `collection` com **todos** os 30 campos identificados (100% de cobertura do CSV), incluindo:

      * `id`
      * `report_senders_name`
      * `report_origin_city_name`
      * `report_recipients_name`
      * `report_destination_city_name`
      * `report_cte_number`
      * `report_draft_number`
      * `report_service_at`
      * `report_invoice_number`
      * `report_manifest_sequence_code`
      * `report_occurrence_description`
      * `report_occurrence_at`
      * `report_comments`
      * `report_corporation_name`
      * `report_payers_name`
      * `report_delivery_agent_name`
      * `report_pick_agent_name`
      * `report_freight_id`