# localizacao_cargas

## Objetivo de negocio

Materializar a posicao operacional da minuta no Data Export para alimentar status de entrega, previsao, filial/regiao de destino e regras das fatos de fretes.

## Chaves e deduplicacao

- Grao: 1 linha por minuta/localizacao.
- Chave primaria: `sequence_number`.
- Deduplicacao: `sequence_number` e usado no upsert; `localizacao_hash` identifica mudancas operacionais relevantes.

## De/Para JSON API -> SQL

| JSON API | Coluna SQL |
|---|---|
| `corporation_sequence_number` | `sequence_number` |
| `type` | `type` |
| `service_at` | `service_at` |
| `invoices_volumes` | `invoices_volumes` |
| `taxed_weight` | `taxed_weight`, `taxed_weight_decimal` |
| `invoices_value` | `invoices_value`, `invoices_value_decimal` |
| `total` | `total_value` |
| `service_type` | `service_type` |
| `fit_crn_psn_nickname` | `branch_nickname` |
| `fit_dpn_delivery_prediction_at` | `predicted_delivery_at` |
| `fit_dyn_name` | `destination_location_name` |
| `fit_dyn_drt_nickname` | `destination_branch_nickname` |
| `fit_fsn_name` | `classification` |
| `fit_fln_status` | `status`, `status_normalized` |
| `fit_fln_cln_nickname` | `fit_fln_cln_nickname` |
| `fit_o_n_name` | `origin_location_name` |
| `fit_o_n_drt_nickname` | `origin_branch_nickname` |
| campos operacionais promovidos | `localizacao_hash` |
| payload completo | `metadata` |
