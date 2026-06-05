# inventario

## Objetivo de negocio

Representar conferencias e inventario operacional de volumes por minuta, incluindo leitura, comprovante, ocorrencia e previsoes de entrega.

## Chaves e deduplicacao

- Grao: 1 linha por conferencia/inventario Data Export.
- Chave primaria: `identificador_unico`.
- Chaves de busca: `sequence_code`, `numero_minuta`.
- Deduplicacao: `identificador_unico` e SHA-256 truncado de `sequence_code`, `numero_minuta`, `invoices_mapping`, `metadata` e `started_at`; isso diferencia leituras de inventario legitimas no mesmo frete.

## De/Para JSON API -> SQL

| JSON API | Coluna SQL |
|---|---|
| hash calculado | `identificador_unico` |
| `sequence_code` | `sequence_code` |
| `cnr_c_s_fit_corporation_sequence_number` | `numero_minuta` |
| `cnr_c_s_fit_pyr_nickname` | `pagador_nome` |
| `cnr_c_s_fit_sdr_nickname` | `remetente_nome` |
| `cnr_c_s_fit_sdr_ads_cty_name` | `origem_cidade` |
| `cnr_c_s_fit_rpt_nickname` | `destinatario_nome` |
| `cnr_c_s_fit_rpt_ads_cty_name` | `destino_cidade` |
| `cnr_c_s_fit_dyn_name` | `regiao_entrega` |
| `cnr_c_s_fit_dyn_drt_nickname` | `filial_entregadora` |
| `cnr_crn_psn_nickname` | `branch_nickname` |
| `type` | `type` |
| `started_at` | `started_at` |
| `finished_at` | `finished_at` |
| `status` | `status` |
| `cnr_cis_eoe_psn_name` | `conferente_nome` |
| `cnr_c_s_fit_invoices_mapping` | `invoices_mapping` |
| `cnr_c_s_fit_invoices_value` | `invoices_value` |
| `cnr_c_s_fit_real_weight` | `real_weight` |
| `cnr_c_s_fit_total_cubic_volume` | `total_cubic_volume` |
| `cnr_c_s_fit_taxed_weight` | `taxed_weight` |
| `cnr_c_s_fit_invoices_volumes` | `invoices_volumes` |
| `cnr_c_s_read_volumes` | `read_volumes` |
| `cnr_c_s_fit_dpn_delivery_prediction_at` | `predicted_delivery_at` |
| `cnr_c_s_fit_dpn_performance_finished_at` | `performance_finished_at` |
| `cnr_c_s_fit_fte_lce_occurrence_at` | `ultima_ocorrencia_at` |
| `cnr_c_s_fit_fte_lce_ore_description` | `ultima_ocorrencia_descricao`, `flag_comprovante_anexado` |
| payload completo | `metadata` |
