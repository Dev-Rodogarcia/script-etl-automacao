# sinistros

## Objetivo de negocio

Registrar eventos de sinistro e tratamento de ocorrencias para analise de impacto financeiro, responsaveis, veiculo, filial e status de solucao.

## Chaves e deduplicacao

- Grao: 1 linha por sinistro Data Export.
- Chave primaria: `identificador_unico`.
- Chaves de busca: `sequence_code`, `corporation_sequence_number`.
- Deduplicacao: `identificador_unico` e SHA-256 truncado de `sequence_code`, `insurance_occurrence_number`, `corporation_sequence_number` e `metadata`.

## De/Para JSON API -> SQL

| JSON API | Coluna SQL |
|---|---|
| hash calculado | `identificador_unico` |
| `sequence_code` | `sequence_code` |
| `opening_at_date` | `opening_at_date` |
| `occurrence_at_date` | `occurrence_at_date` |
| `occurrence_at_time` | `occurrence_at_time` |
| `expected_solution_date` | `expected_solution_date` |
| `insurance_claim_location` | `insurance_claim_location` |
| `informed_by` | `informed_by` |
| `finished_at_date` | `finished_at_date` |
| `finished_at_time` | `finished_at_time` |
| `invoices_count` | `invoices_count` |
| `icm_fis_fit_corporation_sequence_number` | `corporation_sequence_number` |
| `icm_fis_ioe_number` | `insurance_occurrence_number` |
| `invoices_volumes` | `invoices_volumes` |
| `invoices_weight` | `invoices_weight` |
| `invoices_value` | `invoices_value` |
| `icm_fis_fit_pyr_nickname` | `payer_nickname` |
| `customer_debits_subtotal` | `customer_debits_subtotal` |
| `customer_credit_entries_subtotal` | `customer_credit_entries_subtotal` |
| `responsible_credits_subtotal` | `responsible_credits_subtotal` |
| `responsible_debit_entries_subtotal` | `responsible_debit_entries_subtotal` |
| `insurer_credits_subtotal` | `insurer_credits_subtotal` |
| `insurance_claim_total` | `insurance_claim_total` |
| `icm_crn_psn_nickname` | `branch_nickname` |
| `icm_dvr_iil_name` | `event_name` |
| `icm_fer_name` | `user_name` |
| `icm_vie_license_plate` | `vehicle_plate` |
| `icm_ttt_ore_description` | `occurrence_description` |
| `icm_ttt_ore_code` | `occurrence_code` |
| `icm_ttt_treatment_at` | `treatment_at` |
| `icm_ttt_dealing_type` | `dealing_type` |
| `icm_ttt_solution_type` | `solution_type` |
| payload completo | `metadata` |
