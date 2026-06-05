# cotacoes

## Objetivo de negocio

Registrar cotacoes Data Export para analise comercial, conversao em fretes e acompanhamento de usuarios, filiais, origem/destino e componentes de preco.

## Chaves e deduplicacao

- Grao: 1 linha por cotacao.
- Chave primaria: `sequence_code`.
- Deduplicacao: `sequence_code` e a chave de negocio; quando a API retorna duplicatas, o deduplicador mantem a versao mais recente por `requested_at`.

## De/Para JSON API -> SQL

| JSON API | Coluna SQL |
|---|---|
| `sequence_code` | `sequence_code` |
| `requested_at` | `requested_at` |
| `qoe_qes_fon_name` | `operation_type` |
| `qoe_cor_document` | `customer_doc` |
| `qoe_cor_name` | `customer_name` |
| `qoe_qes_ony_name` | `origin_city` |
| `qoe_qes_ony_sae_code` | `origin_state` |
| `qoe_qes_diy_name` | `destination_city` |
| `qoe_qes_diy_sae_code` | `destination_state` |
| `qoe_qes_cre_name` | `price_table` |
| `qoe_qes_invoices_volumes` | `volumes` |
| `qoe_qes_taxed_weight` | `taxed_weight` |
| `qoe_qes_invoices_value` | `invoices_value` |
| `qoe_qes_total` | `total_value` |
| `qoe_uer_name` | `user_name` |
| `qoe_crn_psn_nickname` | `branch_nickname` |
| `qoe_crn_psn_name` | `company_name` |
| `requester_name` | `requester_name` |
| `qoe_qes_real_weight` | `real_weight` |
| `qoe_qes_origin_postal_code` | `origin_postal_code` |
| `qoe_qes_destination_postal_code` | `destination_postal_code` |
| `qoe_cor_nickname` | `customer_nickname` |
| `qoe_qes_sdr_document` | `sender_document` |
| `qoe_qes_sdr_nickname` | `sender_nickname` |
| `qoe_qes_rpt_document` | `receiver_document` |
| `qoe_qes_rpt_nickname` | `receiver_nickname` |
| `qoe_qes_disapprove_comments` | `disapprove_comments` |
| `qoe_qes_freight_comments` | `freight_comments` |
| `qoe_qes_fit_fdt_subtotal` | `discount_subtotal` |
| `qoe_qes_itr_subtotal` | `itr_subtotal` |
| `qoe_qes_tde_subtotal` | `tde_subtotal` |
| `qoe_qes_collect_subtotal` | `collect_subtotal` |
| `qoe_qes_delivery_subtotal` | `delivery_subtotal` |
| `qoe_qes_other_fees` | `other_fees` |
| `qoe_qes_fit_fhe_cte_issued_at` | `cte_issued_at` |
| `qoe_qes_fit_nse_issued_at` | `nfse_issued_at` |
| payload completo | `metadata` |
