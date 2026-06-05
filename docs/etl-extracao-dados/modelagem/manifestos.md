# manifestos

## Objetivo de negocio

Controlar manifestos operacionais, custos de viagem, MDF-e, frota/motorista e vinculo com coletas para indicadores de coletores e performance.

## Chaves e deduplicacao

- Grao: 1 linha por manifesto item retornado pelo Data Export.
- Chave primaria fisica: `id`.
- Chave unica composta: `chave_merge_hash`, calculada por `sequence_code | pick_sequence_code ou identificador_unico | mdfe_number`.
- Taticas de deduplicacao: `identificador_unico` e calculado no mapper apos carregar o payload; quando `pick_sequence_code` vem nulo, ele evita colisao entre itens legitimos do mesmo manifesto. A deduplicacao mantem o registro mais recente por `finished_at/created_at` e consolida metricas.

## De/Para JSON API -> SQL

| JSON API | Coluna SQL |
|---|---|
| `sequence_code` | `sequence_code` |
| calculado do payload | `identificador_unico` |
| `status` | `status` |
| `created_at` | `created_at` |
| `departured_at` | `departured_at` |
| `closed_at` | `closed_at` |
| `finished_at` | `finished_at` |
| `mft_mfs_number` | `mdfe_number` |
| `mft_mfs_key` | `mdfe_key` |
| `mdfe_status` | `mdfe_status` |
| `mft_ape_name` | `distribution_pole` |
| `mft_man_name` | `classification` |
| `mft_vie_license_plate` | `vehicle_plate` |
| `mft_vie_vee_name` | `vehicle_type` |
| `mft_vie_onr_name` | `vehicle_owner` |
| `mft_mdr_iil_name` | `driver_name` |
| `mft_crn_psn_nickname` | `branch_nickname` |
| `vehicle_departure_km` | `vehicle_departure_km` |
| `closing_km` | `closing_km` |
| `traveled_km` | `traveled_km` |
| `invoices_count` | `invoices_count` |
| `invoices_volumes` | `invoices_volumes` |
| `invoices_weight` | `invoices_weight` |
| `total_taxed_weight` | `total_taxed_weight` |
| `total_cubic_volume` | `total_cubic_volume` |
| `invoices_value` | `invoices_value` |
| `manifest_freights_total` | `manifest_freights_total` |
| `mft_pfs_pck_sequence_code` | `pick_sequence_code` |
| `mft_cat_cot_number` | `contract_number` |
| `contract_type` | `contract_type` |
| `calculation_type` | `calculation_type` |
| `cargo_type` | `cargo_type` |
| `daily_subtotal` | `daily_subtotal` |
| `total_cost` | `total_cost` |
| `freight_subtotal` | `freight_subtotal` |
| `fuel_subtotal` | `fuel_subtotal` |
| `toll_subtotal` | `toll_subtotal` |
| `driver_services_total` | `driver_services_total` |
| `operational_expenses_total` | `operational_expenses_total` |
| `mft_a_t_inss_value` | `inss_value` |
| `mft_a_t_sest_senat_value` | `sest_senat_value` |
| `mft_a_t_ir_value` | `ir_value` |
| `paying_total` | `paying_total` |
| `manual_km` | `manual_km` |
| `generate_mdfe` | `generate_mdfe` |
| `monitoring_request` | `monitoring_request` |
| `delivery_manifest_items_count` | `delivery_manifest_items_count` |
| `transfer_manifest_items_count` | `transfer_manifest_items_count` |
| `pick_manifest_items_count` | `pick_manifest_items_count` |
| `dispatch_draft_manifest_items_count` | `dispatch_draft_manifest_items_count` |
| `consolidation_manifest_items_count` | `consolidation_manifest_items_count` |
| `reverse_pick_manifest_items_count` | `reverse_pick_manifest_items_count` |
| `manifest_items_count` | `manifest_items_count` |
| `finalized_manifest_items_count` | `finalized_manifest_items_count` |
| `calculated_*_count` | `calculated_*_count` |
| `pick_subtotal`, `delivery_subtotal`, `dispatch_subtotal`, `consolidation_subtotal`, `reverse_pick_subtotal` | colunas homonimas |
| `mft_uer_name` | `creation_user_name` |
| `mft_aoe_rer_name` | `adjustment_user_name` |
| `mft_aoe_comments` | `adjustment_comments` |
| `mft_cat_cot_status` | `contract_status` |
| `mft_iks_id` | `iks_id` |
| `mft_s_n_*` | `programacao_*` |
| `mft_tl1_*`, `mft_tl2_*`, `mft_vie_*` | colunas de trailer/veiculo/capacidade |
| `operational_comments` | `obs_operacional` |
| `closing_comments` | `obs_financeira` |
| `mft_mte_unloading_recipient_names` | `unloading_recipient_names` |
| `mft_mte_delivery_region_names` | `delivery_region_names` |
| payload completo | `metadata` |
