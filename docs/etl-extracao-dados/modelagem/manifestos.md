# manifestos

## Objetivo de negocio

Controlar manifestos operacionais retornados pelo DataExport ESL Cloud, com foco em viagem, custos, MDF-e, frota, motorista, coletas vinculadas e indicadores de coletores/performance.

## Evidencia auditada

- Contrato real capturado via DataExport template `6399`, tabela API `manifests`, filtro `service_date`.
- Amostra isolada: `logs/relatorios/auditoria-modelagem/payloads/manifestos-primeiro-registro-2026-06-05.json`.
- Observacao operacional: a janela cheia de 30 dias retornou `422 timeout`; o fluxo produtivo particiona DataExport por dia, portanto a amostra foi capturada em subjanela diaria dentro da carga validada.
- Camada Java: `ManifestoDTO`, `ManifestoMapper`, `ManifestoEntity`.
- Camada SQL: `dbo.manifestos`, base `database/tabelas/003_criar_tabela_manifestos.sql` e migrations `027`, `028`, `036`.

## Chaves e deduplicacao reais

- Grao fisico: uma linha por item/logica de manifesto materializada pelo DataExport.
- PK fisica SQL: `id BIGINT IDENTITY`.
- Chave de negocio SQL: `UQ_manifestos_chave_composta` sobre `chave_merge_hash`.
- `chave_merge_hash`: `sequence_code | COALESCE(pick_sequence_code, identificador_unico, -1) | COALESCE(mdfe_number, -1)`.
- `identificador_unico` e calculado no Java depois do `metadata`:
  - `pick_sequence_code + "_MDFE_" + mdfe_number` quando ambos existem.
  - `pick_sequence_code` quando somente a coleta existe.
  - `sequence_code + "_MDFE_" + mdfe_number` quando somente MDF-e existe.
  - SHA-256 canonico do `metadata` estavel quando ambos faltam.
- O hash por `metadata` remove campos volateis antes de calcular identidade: `mobile_read_at`, `departured_at`, `closed_at`, `finished_at`, `vehicle_departure_km`, `closing_km`, `traveled_km`, `finalized_manifest_items_count`, `mft_mfs_number`, `mft_mfs_key`, `mdfe_status`, `mft_aoe_comments`, `mft_aoe_rer_name`.
- Soft delete logico: `excluido_na_origem BIT NOT NULL DEFAULT 0`.

## Matriz de rastreabilidade

| Campo API | Tipo API observado | Propriedade Java | Coluna SQL | Tipo SQL |
|---|---|---|---|---|
| `created_at` | datetime string | `createdAt` | `created_at` | `DATETIMEOFFSET` |
| `sequence_code` | integer | `sequenceCode` | `sequence_code` | `BIGINT` |
| derivado no Java | string | `identificadorUnico` | `identificador_unico` | `NVARCHAR(100)` |
| derivado no SQL | string | n/a | `chave_merge_hash` | `VARCHAR(142)` computada |
| `mobile_read_at` | datetime string/null | `mobileReadAt` | `mobile_read_at` | `DATETIMEOFFSET` |
| `departured_at` | datetime string/null | `departuredAt` | `departured_at` | `DATETIMEOFFSET` |
| `closed_at` | datetime string/null | `closedAt` | `closed_at` | `DATETIMEOFFSET` |
| `finished_at` | datetime string/null | `finishedAt` | `finished_at` | `DATETIMEOFFSET` |
| `km` | string decimal | `km` | `km` | `DECIMAL(18,2)` |
| `vehicle_departure_km` | integer | `vehicleDepartureKm` | `vehicle_departure_km` | `INT` |
| `closing_km` | integer | `closingKm` | `closing_km` | `INT` |
| `traveled_km` | integer | `traveledKm` | `traveled_km` | `INT` |
| `manual_km` | boolean | `manualKm` | `manual_km` | `BIT` |
| `status` | string | `status` | `status` | `NVARCHAR(50)` |
| `generate_mdfe` | boolean | `generateMdfe` | `generate_mdfe` | `BIT` |
| `mdfe_status` | string | `mdfeStatus` | `mdfe_status` | `NVARCHAR(50)` |
| `monitoring_request` | boolean | `monitoringRequest` | `monitoring_request` | `BIT` |
| `invoices_volumes` | integer | `invoicesVolumes` | `invoices_volumes` | `INT` |
| `invoices_count` | integer | `invoicesCount` | `invoices_count` | `INT` |
| `invoices_value` | string decimal | `invoicesValue` | `invoices_value` | `DECIMAL(18,2)` |
| `invoices_weight` | string decimal | `invoicesWeight` | `invoices_weight` | `DECIMAL(18,3)` |
| `total_cubic_volume` | string decimal | `totalCubicVolume` | `total_cubic_volume` | `DECIMAL(18,6)` |
| `total_taxed_weight` | string decimal | `totalTaxedWeight` | `total_taxed_weight` | `DECIMAL(18,3)` |
| `manifest_freights_total` | string decimal | `manifestFreightsTotal` | `manifest_freights_total` | `DECIMAL(18,2)` |
| `delivery_manifest_items_count` | integer | `deliveryManifestItemsCount` | `delivery_manifest_items_count` | `INT` |
| `transfer_manifest_items_count` | integer | `transferManifestItemsCount` | `transfer_manifest_items_count` | `INT` |
| `pick_manifest_items_count` | integer | `pickManifestItemsCount` | `pick_manifest_items_count` | `INT` |
| `dispatch_draft_manifest_items_count` | integer | `dispatchDraftManifestItemsCount` | `dispatch_draft_manifest_items_count` | `INT` |
| `consolidation_manifest_items_count` | integer | `consolidationManifestItemsCount` | `consolidation_manifest_items_count` | `INT` |
| `reverse_pick_manifest_items_count` | integer | `reversePickManifestItemsCount` | `reverse_pick_manifest_items_count` | `INT` |
| `manifest_items_count` | integer | `manifestItemsCount` | `manifest_items_count` | `INT` |
| `finalized_manifest_items_count` | integer | `finalizedManifestItemsCount` | `finalized_manifest_items_count` | `INT` |
| `uniq_destinations_count` | integer | `uniqDestinationsCount` | `uniq_destinations_count` | `INT` |
| `contract_type` | string | `contractType` | `contract_type` | `NVARCHAR(50)` |
| `calculation_type` | string | `calculationType` | `calculation_type` | `NVARCHAR(50)` |
| `cargo_type` | string | `cargoType` | `cargo_type` | `NVARCHAR(255)` |
| `calculated_pick_count` | integer | `calculatedPickCount` | `calculated_pick_count` | `INT` |
| `calculated_delivery_count` | integer | `calculatedDeliveryCount` | `calculated_delivery_count` | `INT` |
| `calculated_dispatch_count` | integer | `calculatedDispatchCount` | `calculated_dispatch_count` | `INT` |
| `calculated_consolidation_count` | integer | `calculatedConsolidationCount` | `calculated_consolidation_count` | `INT` |
| `calculated_reverse_pick_count` | integer | `calculatedReversePickCount` | `calculated_reverse_pick_count` | `INT` |
| `freight_subtotal` | string decimal | `freightSubtotal` | `freight_subtotal` | `DECIMAL(18,2)` |
| `fuel_subtotal` | string decimal | `fuelSubtotal` | `fuel_subtotal` | `DECIMAL(18,2)` |
| `toll_subtotal` | string decimal | `tollSubtotal` | `toll_subtotal` | `DECIMAL(18,2)` |
| `daily_subtotal` | string decimal | `dailySubtotal` | `daily_subtotal` | `DECIMAL(18,2)` |
| `pick_subtotal` | string decimal | `pickSubtotal` | `pick_subtotal` | `DECIMAL(18,2)` |
| `delivery_subtotal` | string decimal | `deliverySubtotal` | `delivery_subtotal` | `DECIMAL(18,2)` |
| `consolidation_subtotal` | string decimal | `consolidationSubtotal` | `consolidation_subtotal` | `DECIMAL(18,2)` |
| `dispatch_subtotal` | string decimal | `dispatchSubtotal` | `dispatch_subtotal` | `DECIMAL(18,2)` |
| `reverse_pick_subtotal` | string decimal | `reversePickSubtotal` | `reverse_pick_subtotal` | `DECIMAL(18,2)` |
| `advance_subtotal` | string decimal | `advanceSubtotal` | `advance_subtotal` | `DECIMAL(18,2)` |
| `fleet_costs_subtotal` | string decimal | `fleetCostsSubtotal` | `fleet_costs_subtotal` | `DECIMAL(18,2)` |
| `additionals_subtotal` | string decimal | `additionalsSubtotal` | `additionals_subtotal` | `DECIMAL(18,2)` |
| `discounts_subtotal` | string decimal | `discountsSubtotal` | `discounts_subtotal` | `DECIMAL(18,2)` |
| `discount_value` | string decimal | `discountValue` | `discount_value` | `DECIMAL(18,2)` |
| `operational_expenses_total` | string decimal | `operationalExpensesTotal` | `operational_expenses_total` | `DECIMAL(18,2)` |
| `total_cost` | string decimal | `totalCost` | `total_cost` | `DECIMAL(18,2)` |
| `paying_total` | string decimal | `payingTotal` | `paying_total` | `DECIMAL(18,2)` |
| `driver_services_total` | string decimal | `driverServicesTotal` | `driver_services_total` | `DECIMAL(18,2)` |
| `mft_aoe_comments` | string/null | `adjustmentComments` | `adjustment_comments` | `NVARCHAR(MAX)` |
| `mft_aoe_rer_name` | string/null | `adjustmentUserName` | `adjustment_user_name` | `NVARCHAR(255)` |
| `mft_a_t_inss_value` | string decimal | `inssValue` | `inss_value` | `DECIMAL(18,2)` |
| `mft_a_t_sest_senat_value` | string decimal | `sestSenatValue` | `sest_senat_value` | `DECIMAL(18,2)` |
| `mft_a_t_ir_value` | string decimal | `irValue` | `ir_value` | `DECIMAL(18,2)` |
| `mft_ape_name` | string | `distributionPole` | `distribution_pole` | `NVARCHAR(255)` |
| `mft_cat_cot_number` | string/null | `contractNumber` | `contract_number` | `NVARCHAR(50)` |
| `mft_cat_cot_status` | string/null | `contractStatus` | `contract_status` | `NVARCHAR(50)` |
| `mft_crn_psn_nickname` | string | `branchNickname` | `branch_nickname` | `NVARCHAR(255)` |
| `mft_fis_fit_mey_mobile_receipt` | string/null | `otherProperties` | `metadata` | `NVARCHAR(MAX)` |
| `mft_iks_id` | string/null | `iksId` | `iks_id` | `NVARCHAR(100)` |
| `mft_mdr_iil_name` | string | `driverName` | `driver_name` | `NVARCHAR(255)` |
| `mft_mte_unloading_recipient_names` | array | `otherProperties` | `unloading_recipient_names` | `NVARCHAR(MAX)` |
| `mft_mte_delivery_region_names` | array | `otherProperties` | `delivery_region_names` | `NVARCHAR(MAX)` |
| `mft_man_name` | string | `classification` | `classification` | `NVARCHAR(255)` |
| `mft_mfs_number` | integer/null | `mdfeNumber` | `mdfe_number` | `INT` |
| `mft_mfs_key` | string/null | `mdfeKey` | `mdfe_key` | `NVARCHAR(100)` |
| `mft_pfs_pck_sequence_code` | integer/null | `pickSequenceCode` | `pick_sequence_code` | `BIGINT` |
| `mft_s_n_sequence_code` | string/null | `programacaoSequenceCode` | `programacao_sequence_code` | `NVARCHAR(50)` |
| `mft_s_n_starting_at` | datetime string/null | `programacaoStartingAt` | `programacao_starting_at` | `DATETIMEOFFSET` |
| `mft_s_n_ending_at` | datetime string/null | `programacaoEndingAt` | `programacao_ending_at` | `DATETIMEOFFSET` |
| `mft_s_n_svs_sge_pyr_nickname` | string/null | `programacaoCliente` | `programacao_cliente` | `NVARCHAR(255)` |
| `mft_s_n_svs_sge_sse_name` | string/null | `programacaoTipoServico` | `programacao_tipo_servico` | `NVARCHAR(255)` |
| `mft_tl1_license_plate` | string/null | `trailer1LicensePlate` | `trailer1_license_plate` | `NVARCHAR(10)` |
| `mft_tl1_weight_capacity` | string decimal/null | `trailer1WeightCapacity` | `trailer1_weight_capacity` | `DECIMAL(18,2)` |
| `mft_tl2_license_plate` | string/null | `trailer2LicensePlate` | `trailer2_license_plate` | `NVARCHAR(10)` |
| `mft_tl2_weight_capacity` | string decimal/null | `trailer2WeightCapacity` | `trailer2_weight_capacity` | `DECIMAL(18,2)` |
| `mft_uer_name` | string | `creationUserName` | `creation_user_name` | `NVARCHAR(255)` |
| `mft_vie_license_plate` | string | `vehiclePlate` | `vehicle_plate` | `NVARCHAR(10)` |
| `mft_vie_weight_capacity` | string decimal | `vehicleWeightCapacity` | `vehicle_weight_capacity`, `capacidade_kg` | `DECIMAL(18,2)` |
| `mft_vie_cubic_weight` | string decimal | `vehicleCubicWeight` | `vehicle_cubic_weight` | `DECIMAL(18,2)` |
| `mft_vie_onr_name` | string | `vehicleOwner` | `vehicle_owner` | `NVARCHAR(255)` |
| `mft_vie_vee_name` | string | `vehicleType` | `vehicle_type` | `NVARCHAR(255)` |
| payload completo | object | `getAllProperties()` | `metadata` | `NVARCHAR(MAX)` |

## Campos previstos fora da amostra

`operational_comments` e `closing_comments` existem no DTO/mapper e alimentam `obs_operacional` e `obs_financeira`, mas nao apareceram no payload real capturado. A documentacao deve trata-los como campos opcionais do template, nao como garantidos na resposta diaria.
