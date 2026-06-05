# fretes

## Objetivo de negocio

Armazenar fretes GraphQL como base operacional e financeira para indicadores de performance, cubagem, faturamento e emissao fiscal.

## Chaves e deduplicacao

- Grao: 1 linha por frete GraphQL.
- Chave primaria: `id`.
- Chaves de busca: `corporation_sequence_number`, `chave_cte`, `data_referencia_faturamento`, `filial_nome_key`.
- Deduplicacao: o upsert e idempotente por `id`; a fato de faturamento deduplica posteriormente por `frete_id`.

## De/Para JSON API -> SQL

| JSON API | Coluna SQL |
|---|---|
| `id` | `id` |
| `serviceAt` | `servico_em` |
| `createdAt` | `criado_em` |
| `status` | `status` |
| `courtesy` | `cortesia` |
| `modal` | `modal` |
| `type` | `tipo_frete` |
| `accountingCreditId` | `accounting_credit_id` |
| `accountingCreditInstallmentId` | `accounting_credit_installment_id` |
| `total` | `valor_total` |
| `invoicesValue` | `valor_notas` |
| `invoicesWeight` | `peso_notas` |
| `corporationId` | `id_corporacao` |
| `destinationCityId` | `id_cidade_destino` |
| `deliveryPredictionDate` | `data_previsao_entrega` |
| `serviceDate` | `service_date` |
| `finishedAt` | `finished_at` |
| `otherProperties.fit_dpn_performance_finished_at` | `fit_dpn_performance_finished_at` |
| `corporationSequenceNumber` | `corporation_sequence_number` |
| `payer.id` | `pagador_id` |
| `payer.name` | `pagador_nome` |
| `payer.cnpj/cpf` | `pagador_documento` |
| `sender.id` | `remetente_id` |
| `sender.name` | `remetente_nome` |
| `sender.cnpj/cpf` | `remetente_documento` |
| `originCity.name` ou `sender.mainAddress.city.name` | `origem_cidade` |
| `originCity.state.code` ou `sender.mainAddress.city.state.code` | `origem_uf` |
| `receiver.id` | `destinatario_id` |
| `receiver.name` | `destinatario_nome` |
| `receiver.cnpj/cpf` | `destinatario_documento` |
| `destinationCity.name` ou `receiver.mainAddress.city.name` | `destino_cidade` |
| `destinationCity.state.code` ou `receiver.mainAddress.city.state.code` | `destino_uf` |
| `corporation.person.nickname` ou `corporation.nickname` | `filial_nome`, `filial_apelido` |
| `corporation.person.cnpj` ou `corporation.cnpj` | `filial_cnpj` |
| `freightInvoices[].invoice.number` | `numero_nota_fiscal` |
| `customerPriceTable.name` | `tabela_preco_nome` |
| `freightClassification.name` | `classificacao_nome` |
| `costCenter.name` | `centro_custo_nome` |
| `user.name` | `usuario_nome` |
| `referenceNumber` | `reference_number` |
| `invoicesTotalVolumes` | `invoices_total_volumes` |
| `taxedWeight` | `taxed_weight` |
| `realWeight` | `real_weight` |
| `cubagesCubedWeight` | `cubages_cubed_weight` |
| `totalCubicVolume` | `total_cubic_volume` |
| `subtotal` | `subtotal` |
| `cte.key` | `chave_cte` |
| `cte.number` | `numero_cte` |
| `cte.series` | `serie_cte` |
| `cte.id` | `cte_id` |
| `cte.emissionType` | `cte_emission_type` |
| `cte.createdAt` | `cte_created_at` |
| `cte.issuedAt` | `cte_issued_at` |
| `serviceType` | `service_type` |
| `insuranceEnabled` | `insurance_enabled` |
| `grisSubtotal` | `gris_subtotal` |
| `tdeSubtotal` | `tde_subtotal` |
| `modalCte` | `modal_cte` |
| `redispatchSubtotal` | `redispatch_subtotal` |
| `suframaSubtotal` | `suframa_subtotal` |
| `paymentType` | `payment_type` |
| `previousDocumentType` | `previous_document_type` |
| `productsValue` | `products_value` |
| `trtSubtotal` | `trt_subtotal` |
| `nfseSeries` | `nfse_series` |
| `nfseNumber` | `nfse_number` |
| `insuranceId` | `insurance_id` |
| `otherFees` | `other_fees` |
| `km` | `km` |
| `fiscalDetail.*` | `fiscal_*` |
| payload completo | `metadata` |
