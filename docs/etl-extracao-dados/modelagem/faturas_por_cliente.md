# faturas_por_cliente

## Objetivo de negocio

Consolidar documentos de faturamento por cliente, incluindo CT-e, NFS-e, fatura, vencimento, baixa, pagador e documentos relacionados para as fatos de faturamento e gestao a vista.

## Chaves e deduplicacao

- Grao: 1 linha por documento canonico de faturamento.
- Chave primaria: `unique_id`.
- Deduplicacao: `unique_id` e sempre gerado como `HASH-` + SHA-256 de uma representacao canonica. A representacao prioriza NFS-e, depois CT-e, depois fatura, depois `billingId`; se nenhum identificador forte existir, usa documentos de pagador/remetente/destinatario, notas, pedidos e valores.
- Taticas de reconciliacao: aliases legados (`NFSE-*`, chave CT-e, `FATURA-*`, `BILLING-*`) sao reconhecidos para migrar registros antigos ao `unique_id` canonico.

## De/Para JSON API -> SQL

| JSON API | Coluna SQL |
|---|---|
| hash canonico do payload | `unique_id` |
| `total` | `valor_frete` |
| `fit_ant_value` | `valor_fatura`, `fit_ant_value` |
| `third_party_ctes_value` | `third_party_ctes_value` |
| `fit_fhe_cte_number` | `numero_cte` |
| `fit_fhe_cte_key` | `chave_cte` |
| `fit_nse_number` ou `nfse_number` | `numero_nfse` |
| NFS-e serie quando disponivel | `serie_nfse` |
| `fit_fhe_cte_status` | `status_cte` |
| `fit_fhe_cte_status_result` | `status_cte_result` |
| `fit_fhe_cte_issued_at` | `data_emissao_cte` |
| `fit_ant_document` | `numero_fatura`, `fit_ant_document` |
| `fit_ant_issue_date` | `data_emissao_fatura`, `fit_ant_issue_date` |
| `fit_ant_ils_due_date` | `data_vencimento_fatura` |
| `fit_ant_ils_atn_transaction_date` | `data_baixa_fatura` |
| `fit_ant_ils_original_due_date` | `fit_ant_ils_original_due_date` |
| `fit_crn_psn_nickname` | `filial` |
| `type` | `tipo_frete` |
| `fit_fsn_name` | `classificacao` |
| `fit_diy_sae_name` | `estado` |
| `fit_pyr_name` | `pagador_nome` |
| `fit_pyr_document` | `pagador_documento`, `cliente_cnpj` |
| `fit_rpt_name` | `remetente_nome` |
| `fit_rpt_document` | `remetente_documento` |
| `fit_sdr_name` | `destinatario_nome` |
| `fit_sdr_document` | `destinatario_documento` |
| `fit_sps_slr_psn_name` | `vendedor_nome` |
| `invoices_mapping` | `notas_fiscais` |
| `fit_fte_invoices_order_number` | `pedidos_cliente` |
| payload completo | `metadata` |
