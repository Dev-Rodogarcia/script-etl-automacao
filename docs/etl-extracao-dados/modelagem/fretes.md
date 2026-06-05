# fretes

## Objetivo de negocio

Armazenar fretes da API GraphQL ESL Cloud como base operacional e financeira para performance, cubagem, faturamento, emissao fiscal e fatos BI materializados.

## Evidencia auditada

- Contrato real capturado via GraphQL `freight`, query produtiva `QUERY_FRETES`, filtro `serviceAt`.
- Amostra isolada: `logs/relatorios/auditoria-modelagem/payloads/fretes-primeiro-node-2026-05-07_2026-06-05.json`.
- Camada Java: `FreteNodeDTO`, DTOs aninhados, `FreteMapper`, `FreteEntity`.
- Camada SQL: `dbo.fretes`, base `database/tabelas/002_criar_tabela_fretes.sql` e migrations `012`, `013`, `016`, `027` mais migrations analiticas relacionadas.

## Chaves e deduplicacao reais

- Grao: uma linha por `Freight` GraphQL.
- PK SQL: `id BIGINT`.
- Upsert idempotente por `id`.
- Indices analiticos atuais incluem `servico_em`, `data_extracao`, `data_referencia_faturamento/is_elegivel_faturamento`, `filial_nome_key` e ativos por origem.
- Campos de faturamento `data_referencia_faturamento` e `is_elegivel_faturamento` sao materializados no banco a partir de `cte_issued_at`/`servico_em`, cortesia e classificacao.
- Soft delete logico: `excluido_na_origem BIT NOT NULL DEFAULT 0`.

## Matriz de rastreabilidade

| Campo API | Tipo API observado | Propriedade Java | Coluna SQL | Tipo SQL |
|---|---|---|---|---|
| `id` | integer | `id` | `id` | `BIGINT` |
| `accountingCreditId` | integer/null | `accountingCreditId` | `accounting_credit_id` | `BIGINT` |
| `accountingCreditInstallmentId` | integer/null | `accountingCreditInstallmentId` | `accounting_credit_installment_id` | `BIGINT` |
| `referenceNumber` | string | `referenceNumber` | `reference_number` | `NVARCHAR(100)` |
| `serviceAt` | datetime string | `serviceAt` | `servico_em` | `DATETIMEOFFSET` |
| `createdAt` | datetime string | `createdAt` | `criado_em` | `DATETIMEOFFSET` |
| `finishedAt` | datetime string/null | `finishedAt` | `finished_at` | `DATETIMEOFFSET` |
| `cte.id` | string/integer | `cte.id` | `cte_id` | `BIGINT` |
| `cte.key` | string | `cte.key` | `chave_cte` | `NVARCHAR(100)` |
| `cte.number` | integer | `cte.number` | `numero_cte` | `INT` |
| `cte.series` | integer | `cte.series` | `serie_cte` | `INT` |
| `cte.issuedAt` | datetime string | `cte.issuedAt` | `cte_issued_at` | `DATETIMEOFFSET` |
| `cte.createdAt` | datetime string | `cte.createdAt` | `cte_created_at` | `DATETIMEOFFSET` |
| `cte.emissionType` | string | `cte.emissionType` | `cte_emission_type` | `NVARCHAR(50)` |
| `total` | decimal | `totalValue` | `valor_total` | `DECIMAL(18,2)` |
| `subtotal` | decimal | `subtotal` | `subtotal` | `DECIMAL(18,2)` |
| `invoicesValue` | decimal | `invoicesValue` | `valor_notas` | `DECIMAL(18,2)` |
| `invoicesWeight` | decimal | `invoicesWeight` | `peso_notas` | `DECIMAL(18,3)` |
| `taxedWeight` | decimal | `taxedWeight` | `taxed_weight` | `DECIMAL(18,3)` |
| `realWeight` | decimal | `realWeight` | `real_weight` | `DECIMAL(18,3)` |
| `cubagesCubedWeight` | decimal | `cubagesCubedWeight` | `cubages_cubed_weight` | `DECIMAL(18,6)` |
| `totalCubicVolume` | decimal | `totalCubicVolume` | `total_cubic_volume` | `DECIMAL(18,6)` |
| `invoicesTotalVolumes` | integer | `invoicesTotalVolumes` | `invoices_total_volumes` | `INT` |
| `freightInvoices[].invoice.number` | string | `freightInvoices[].invoice.number` | `numero_nota_fiscal` | `NVARCHAR(MAX)` |
| `freightInvoices[].invoice.series` | string | `freightInvoices[].invoice.series` | `metadata` | `NVARCHAR(MAX)` |
| `freightInvoices[].invoice.key` | string | `freightInvoices[].invoice.key` | `metadata` | `NVARCHAR(MAX)` |
| `freightInvoices[].invoice.value` | decimal | `freightInvoices[].invoice.value` | `metadata` | `NVARCHAR(MAX)` |
| `freightInvoices[].invoice.weight` | decimal | `freightInvoices[].invoice.weight` | `metadata` | `NVARCHAR(MAX)` |
| `sender.id` | string/integer | `sender.id` | `remetente_id` | `BIGINT` |
| `sender.name` | string | `sender.name` | `remetente_nome` | `NVARCHAR(255)` |
| `sender.cnpj`/`sender.cpf` | string/null | `sender.cnpj`, `sender.cpf` | `remetente_documento` | `NVARCHAR(50)` |
| `sender.inscricaoEstadual` | string/null | `sender.inscricaoEstadual` | `metadata` | `NVARCHAR(MAX)` |
| `sender.mainAddress.city.name` | string | `sender.mainAddress.city.name` | `origem_cidade` fallback | `NVARCHAR(255)` |
| `sender.mainAddress.city.state.code` | string | `sender.mainAddress.city.state.code` | `origem_uf` fallback | `NVARCHAR(10)` |
| `receiver.id` | string/integer | `receiver.id` | `destinatario_id` | `BIGINT` |
| `receiver.name` | string | `receiver.name` | `destinatario_nome` | `NVARCHAR(255)` |
| `receiver.cnpj`/`receiver.cpf` | string/null | `receiver.cnpj`, `receiver.cpf` | `destinatario_documento` | `NVARCHAR(50)` |
| `receiver.inscricaoEstadual` | string/null | `receiver.inscricaoEstadual` | `metadata` | `NVARCHAR(MAX)` |
| `receiver.mainAddress.city.name` | string | `receiver.mainAddress.city.name` | `destino_cidade` fallback | `NVARCHAR(255)` |
| `receiver.mainAddress.city.state.code` | string | `receiver.mainAddress.city.state.code` | `destino_uf` fallback | `NVARCHAR(10)` |
| `payer.id` | string/integer | `payer.id` | `pagador_id` | `BIGINT` |
| `payer.name` | string | `payer.name` | `pagador_nome` | `NVARCHAR(255)` |
| `payer.cnpj`/`payer.cpf` | string/null | `payer.cnpj`, `payer.cpf` | `pagador_documento` | `NVARCHAR(50)` |
| `modal` | string | `modal` | `modal` | `NVARCHAR(50)` |
| `modalCte` | string | `modalCte` | `modal_cte` | `NVARCHAR(50)` |
| `status` | string | `status` | `status` | `NVARCHAR(50)` |
| `courtesy` | boolean | `courtesy` | `cortesia` | `BIT` |
| `type` | string | `type` | `tipo_frete` | `NVARCHAR(100)` |
| `serviceDate` | date string/null | `serviceDate` | `service_date` | `DATE` |
| `serviceType` | string/integer | `serviceType` | `service_type` | `INT` |
| `deliveryPredictionDate` | date string/null | `deliveryPredictionDate` | `data_previsao_entrega` | `DATE` |
| `corporationSequenceNumber` | integer | `corporationSequenceNumber` | `corporation_sequence_number` | `BIGINT` |
| `corporation.id` | string/integer | `corporation.id` | `metadata` | `NVARCHAR(MAX)` |
| `corporation.nickname` | string | `corporation.nickname` | `filial_nome`, `filial_apelido` | `NVARCHAR(255)` |
| `corporation.cnpj` | string | `corporation.cnpj` | `filial_cnpj` | `NVARCHAR(50)` |
| `customerPriceTable.name` | string/null | `customerPriceTable.name` | `tabela_preco_nome` | `NVARCHAR(255)` |
| `freightClassification.name` | string/null | `freightClassification.name` | `classificacao_nome` | `NVARCHAR(255)` |
| `costCenter.name` | string/null | `costCenter.name` | `centro_custo_nome` | `NVARCHAR(255)` |
| `originCity.name` | string/null | `originCity.name` | `origem_cidade` | `NVARCHAR(255)` |
| `originCity.state.code` | string/null | `originCity.state.code` | `origem_uf` | `NVARCHAR(10)` |
| `destinationCity.name` | string/null | `destinationCity.name` | `destino_cidade` | `NVARCHAR(255)` |
| `destinationCity.state.code` | string/null | `destinationCity.state.code` | `destino_uf` | `NVARCHAR(10)` |
| `destinationCityId` | integer | `destinationCityId` | `id_cidade_destino` | `BIGINT` |
| `corporationId` | integer | `corporationId` | `id_corporacao` | `BIGINT` |
| `freightWeightSubtotal` | decimal | `freightWeightSubtotal` | `freight_weight_subtotal` | `DECIMAL(18,2)` |
| `globalized` | boolean | `globalized` | `globalized` | `BIT` |
| `globalizedType` | string | `globalizedType` | `globalized_type` | `NVARCHAR(50)` |
| `grisSubtotal` | decimal | `grisSubtotal` | `gris_subtotal` | `DECIMAL(18,2)` |
| `adValoremSubtotal` | decimal | `adValoremSubtotal` | `ad_valorem_subtotal` | `DECIMAL(18,2)` |
| `insuranceAccountableType` | string/integer | `insuranceAccountableType` | `insurance_accountable_type` | `INT` |
| `insuranceEnabled` | boolean | `insuranceEnabled` | `insurance_enabled` | `BIT` |
| `insuranceId` | integer | `insuranceId` | `insurance_id` | `BIGINT` |
| `insuredValue` | decimal | `insuredValue` | `insured_value` | `DECIMAL(18,2)` |
| `itrSubtotal` | decimal | `itrSubtotal` | `itr_subtotal` | `DECIMAL(18,2)` |
| `tollSubtotal` | decimal | `tollSubtotal` | `toll_subtotal` | `DECIMAL(18,2)` |
| `km` | decimal | `km` | `km` | `DECIMAL(18,2)` |
| `nfseNumber` | string/integer/null | `nfseNumber` | `nfse_number` | `INT` |
| `nfseSeries` | string/null | `nfseSeries` | `nfse_series` | `NVARCHAR(50)` |
| `otherFees` | decimal | `otherFees` | `other_fees` | `DECIMAL(18,2)` |
| `paymentAccountableType` | string/integer | `paymentAccountableType` | `payment_accountable_type` | `INT` |
| `paymentType` | string | `paymentType` | `payment_type` | `NVARCHAR(50)` |
| `previousDocumentType` | string | `previousDocumentType` | `previous_document_type` | `NVARCHAR(50)` |
| `priceTableAccountableType` | string/integer | `priceTableAccountableType` | `price_table_accountable_type` | `INT` |
| `productsValue` | decimal | `productsValue` | `products_value` | `DECIMAL(18,2)` |
| `redispatchSubtotal` | decimal | `redispatchSubtotal` | `redispatch_subtotal` | `DECIMAL(18,2)` |
| `secCatSubtotal` | decimal | `secCatSubtotal` | `sec_cat_subtotal` | `DECIMAL(18,2)` |
| `suframaSubtotal` | decimal | `suframaSubtotal` | `suframa_subtotal` | `DECIMAL(18,2)` |
| `tdeSubtotal` | decimal | `tdeSubtotal` | `tde_subtotal` | `DECIMAL(18,2)` |
| `trtSubtotal` | decimal | `trtSubtotal` | `trt_subtotal` | `DECIMAL(18,2)` |
| `fiscalDetail.cstType` | string | `fiscalDetail.cstType` | `fiscal_cst_type` | `NVARCHAR(10)` |
| `fiscalDetail.cfopCode` | string | `fiscalDetail.cfopCode` | `fiscal_cfop_code` | `NVARCHAR(10)` |
| `fiscalDetail.calculationBasis` | decimal | `fiscalDetail.calculationBasis` | `fiscal_calculation_basis` | `DECIMAL(18,2)` |
| `fiscalDetail.taxRate` | decimal | `fiscalDetail.taxRate` | `fiscal_tax_rate` | `DECIMAL(18,6)` |
| `fiscalDetail.taxValue` | decimal | `fiscalDetail.taxValue` | `fiscal_tax_value` | `DECIMAL(18,2)` |
| `fiscalDetail.pisRate` | decimal | `fiscalDetail.pisRate` | `fiscal_pis_rate` | `DECIMAL(18,6)` |
| `fiscalDetail.pisValue` | decimal | `fiscalDetail.pisValue` | `fiscal_pis_value` | `DECIMAL(18,2)` |
| `fiscalDetail.cofinsRate` | decimal | `fiscalDetail.cofinsRate` | `fiscal_cofins_rate` | `DECIMAL(18,6)` |
| `fiscalDetail.cofinsValue` | decimal | `fiscalDetail.cofinsValue` | `fiscal_cofins_value` | `DECIMAL(18,2)` |
| `fiscalDetail.hasDifal` | boolean | `fiscalDetail.hasDifal` | `fiscal_has_difal` | `BIT` |
| `fiscalDetail.difalTaxValueOrigin` | decimal | `fiscalDetail.difalTaxValueOrigin` | `fiscal_difal_origin` | `DECIMAL(18,2)` |
| `fiscalDetail.difalTaxValueDestination` | decimal | `fiscalDetail.difalTaxValueDestination` | `fiscal_difal_destination` | `DECIMAL(18,2)` |
| derivado no SQL | datetimeoffset | n/a | `data_referencia_faturamento` | `DATETIMEOFFSET` |
| derivado no SQL | boolean | n/a | `is_elegivel_faturamento` | `BIT` |
| payload completo | object | DTO serializado | `metadata` | `NVARCHAR(MAX)` |

## Divergencias e campos fora do payload freight atual

- `user.name` existe no DTO/mapper e alimentaria `usuario_nome`, mas a query produtiva `QUERY_FRETES` auditada nao seleciona `user`; a coluna tende a permanecer nula para esta origem.
- `fit_dpn_performance_finished_at` e lido apenas de `otherProperties`; a query produtiva auditada nao seleciona esse campo. A regra analitica usa `finishedAt` como fallback.
- `freightInvoices[].invoice.key`, `series`, `value` e `weight` sao preservados no `metadata`, mas somente `invoice.number` e promovido para `numero_nota_fiscal`.
- `sender.inscricaoEstadual`, `receiver.inscricaoEstadual` e `corporation.id` sao preservados no `metadata`, sem colunas dedicadas.
- Colunas `nfse_integration_id`, `nfse_status`, `nfse_issued_at`, `nfse_cancelation_reason`, `nfse_pdf_service_url`, `nfse_corporation_id`, `nfse_service_description` e `nfse_xml_document` nao vem do payload `freight`; sao de enriquecimento NFSe separado.
