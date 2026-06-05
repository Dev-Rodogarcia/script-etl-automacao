# faturas_por_cliente

## Objetivo de negocio

Consolidar documentos de faturamento por cliente vindos do DataExport ESL Cloud, incluindo CT-e, NFS-e, fatura, vencimento, baixa, pagador, remetente, destinatario, vendedor, notas e pedidos para fatos de faturamento e gestao a vista.

## Evidencia auditada

- Contrato real capturado via DataExport template `4924`, tabela API `freights`, filtro `service_at`.
- Amostra isolada: `logs/relatorios/auditoria-modelagem/payloads/faturas_por_cliente-primeiro-registro-2026-06-05.json`.
- Camada Java: `FaturaPorClienteDTO`, `FaturaPorClienteMapper`, `FaturaPorClienteEntity`.
- Camada SQL: `dbo.faturas_por_cliente`, base `database/tabelas/007_criar_tabela_faturas_por_cliente.sql` e migrations `015`, `027`, `037`.

## Chaves e deduplicacao reais

- Grao: uma linha por documento canonico de faturamento calculado pelo ETL.
- PK SQL: `unique_id NVARCHAR(100)`.
- `unique_id` real: sempre `FPC-HASH-` + SHA-256 de representacao canonica estavel.
- Prioridade da identidade canonica:
  - NFS-e efetiva (`fit_nse_number`, com fallback parseavel em `nfse_number`) + documentos de pagador/remetente/destinatario.
  - CT-e por `fit_fhe_cte_number` + documentos de pagador/remetente/destinatario.
  - Fatura por `fit_ant_document` + `fit_ant_issue_date` + documentos.
  - `billingId` quando fornecido.
  - Fallback por documentos, notas, pedidos e valores.
- Aliases legados reconhecidos para reconciliacao: `NFSE-*`, chave CT-e, `FATURA-*`, `BILLING-*`. Chaves acima de 100 caracteres viram `FPC-KEYHASH-` + SHA-256.
- Campos volateis de status, baixa, nomes e totais derivados nao fazem parte da identidade canonica.
- Soft delete logico: `excluido_na_origem BIT NOT NULL DEFAULT 0`.

## Matriz de rastreabilidade

| Campo API | Tipo API observado | Propriedade Java | Coluna SQL | Tipo SQL |
|---|---|---|---|---|
| derivado no Java | string | `uniqueId` | `unique_id` | `NVARCHAR(100)` |
| `nfse_number` | string/null | `nfseNumberRaw` | `numero_nfse` quando parseavel; tambem `metadata` | `BIGINT`; `NVARCHAR(MAX)` |
| `fit_nse_number` | integer/null | `nfseNumber` | `numero_nfse` | `BIGINT` |
| `third_party_ctes_value` | string decimal | `thirdPartyCtesValue` | `third_party_ctes_value` | `DECIMAL(18,2)` |
| `type` | string | `tipoFrete` | `tipo_frete` | `NVARCHAR(100)` |
| `total` | string decimal | `valorFrete` | `valor_frete` | `DECIMAL(18,2)` |
| `invoices_mapping` | array string | `notasFiscais` | `notas_fiscais` | `NVARCHAR(MAX)` |
| `status` | string | `status`; tambem `metadata` | `status`; tambem `metadata` | `NVARCHAR(50)`; `NVARCHAR(MAX)` |
| `fit_ant_document` | string/null | `faturaDocument` | `numero_fatura`, `fit_ant_document` | `NVARCHAR(50)` |
| `fit_ant_issue_date` | date string/null | `faturaIssueDate` | `data_emissao_fatura`, `fit_ant_issue_date` | `DATE` |
| `fit_ant_value` | string decimal/null | `faturaValue` | `valor_fatura`, `fit_ant_value` | `DECIMAL(18,2)` |
| `fit_ant_ils_due_date` | date string/null | `faturaDueDate` | `data_vencimento_fatura` | `DATE` |
| `fit_ant_ils_original_due_date` | date string/null | `faturaOriginalDueDate` | `fit_ant_ils_original_due_date` | `DATE` |
| `fit_ant_ils_atn_transaction_date` | date string/null | `faturaBaixaDate` | `data_baixa_fatura` | `DATE` |
| `fit_ant_tat_custom_instruction` | string/null | `otherProperties` | `metadata` | `NVARCHAR(MAX)` |
| `fit_ant_tat_bro_description` | string/null | `otherProperties` | `metadata` | `NVARCHAR(MAX)` |
| `fit_crn_psn_nickname` | string | `filial` | `filial` | `NVARCHAR(255)` |
| `fit_diy_sae_name` | string | `estado` | `estado` | `NVARCHAR(50)` |
| `fit_fte_invoices_order_number` | array string | `pedidosCliente` | `pedidos_cliente` | `NVARCHAR(MAX)` |
| `fit_fsn_name` | string | `classificacao` | `classificacao` | `NVARCHAR(100)` |
| `fit_fhe_cte_number` | integer/null | `cteNumber` | `numero_cte` | `BIGINT` |
| `fit_fhe_cte_issued_at` | datetime string/null | `cteIssuedAt` | `data_emissao_cte` | `DATETIMEOFFSET` |
| `fit_fhe_cte_status_result` | string/null | `cteStatusResult` | `status_cte_result` | `NVARCHAR(MAX)` |
| `fit_fhe_cte_key` | string | `cteKey` | `chave_cte` | `NVARCHAR(100)` |
| `fit_fhe_cte_status` | string | `cteStatus` | `status_cte` | `NVARCHAR(255)` |
| `fit_o_t_total` | string decimal | `otherProperties` | `metadata` | `NVARCHAR(MAX)` |
| `fit_o_t_fhe_cte_fhe_cte_fit_third_party_ctes_value` | string decimal | `otherProperties` | `metadata` | `NVARCHAR(MAX)` |
| `fit_pyr_name` | string | `pagadorNome` | `pagador_nome` | `NVARCHAR(255)` |
| `fit_pyr_document` | string | `pagadorDocumento` | `pagador_documento`, `cliente_cnpj` quando CNPJ valido | `NVARCHAR(50)`, `NVARCHAR(14)` |
| `fit_rpt_document` | string | `remetenteDocumento` | `remetente_documento` | `NVARCHAR(50)` |
| `fit_rpt_name` | string | `remetenteNome` | `remetente_nome` | `NVARCHAR(255)` |
| `fit_sps_slr_psn_name` | string | `vendedorNome` | `vendedor_nome` | `NVARCHAR(255)` |
| `fit_sdr_document` | string | `destinatarioDocumento` | `destinatario_documento` | `NVARCHAR(50)` |
| `fit_sdr_name` | string | `destinatarioNome` | `destinatario_nome` | `NVARCHAR(255)` |
| payload completo | object | DTO serializado | `metadata` | `NVARCHAR(MAX)` |

## Divergencias e campos nao promovidos

- `status` vem no payload real e foi promovido para a coluna dedicada `status`; o payload completo continua preservado em `metadata`.
- `fit_ant_tat_custom_instruction` e `fit_ant_tat_bro_description` vem/estao previstos no payload, mas ficam apenas no `metadata`.
- `fit_o_t_total` e `fit_o_t_fhe_cte_fhe_cte_fit_third_party_ctes_value` parecem duplicar valores de total/terceiros no template e ficam apenas no `metadata`.
- A coluna `serie_nfse` existe no SQL, mas o DTO/mapper atual nao possui propriedade ou atribuicao para popula-la a partir do payload capturado.
