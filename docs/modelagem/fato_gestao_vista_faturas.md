# fato_gestao_vista_faturas

## Objetivo de negocio

Materializar visao de Gestao a Vista de faturas por cliente, com status de processo, pagamento, vencimento e valores operacionais.

## Chaves e deduplicacao

- Grao: 1 linha por `unique_id` e `data_emissao_fatura`.
- Chave unica: `UX_fato_gvf_unique_id_data (unique_id, data_emissao_fatura)`.
- Deduplicacao: a origem `faturas_por_cliente` ja chega deduplicada por `unique_id`; a procedure aplica normalizacao, status e `MERGE` idempotente com `hash_linha`.

## Carga e orquestracao

- Procedure de carga: `dbo.sp_carga_fato_gestao_vista_faturas`.
- Orquestracao intradia: incluida em `etl.bi.procedures.target` e executada pelo proprio `--loop-daemon-run` apos cada ciclo de extracao bem-sucedido.
- Orquestracao complementar: tambem pode ser chamada pelo `MATERIALIZAR_FATOS_BI_POST_RUN`, por `database/executar_database.bat --com-cargas` e pela janela noturna resiliente de `10-expurgo-orfaos-noturno.ps1`.

## De/Para JSON API -> SQL

Esta fato e derivada de `dbo.faturas_por_cliente`.

| JSON API / origem bruta | Coluna SQL da fato |
|---|---|
| hash canonico de `faturas_por_cliente` | `unique_id` |
| `fit_ant_document` | `numero_fatura`, `documento_fatura` |
| `fit_fhe_cte_number`, `fit_nse_number` | `numero_documento`, `numero_cte`, `numero_nfse` |
| `fit_fhe_cte_key` | `chave_cte` |
| `fit_fhe_cte_issued_at` | `data_emissao_cte`, `data_emissao_cte_date` |
| `fit_ant_issue_date` | `data_emissao_fatura`, `data_emissao_fatura_yyyymm` |
| `fit_ant_ils_due_date` | `data_vencimento_fatura` |
| `fit_ant_ils_atn_transaction_date` | `data_baixa_fatura` |
| `fit_ant_ils_original_due_date` | `fit_ant_ils_original_due_date` |
| `fit_crn_psn_nickname` | `filial`, `filial_key` |
| `fit_diy_sae_name` | `estado` |
| `fit_pyr_name/document` | `pagador_nome`, `pagador_documento`, `pagador_documento_key` |
| `cliente_cnpj` derivado de `fit_pyr_document` | `cliente_cnpj`, `cliente_cnpj_key`, `cliente_chave` |
| `fit_rpt_*`, `fit_sdr_*` | `remetente_*`, `destinatario_*` |
| `fit_sps_slr_psn_name` | `vendedor_nome` |
| regras da procedure sobre documento/vencimento/baixa | `status_processo`, `status_pagamento`, `dias_ate_vencimento`, `dias_em_atraso` |
| `fit_ant_value`, `total`, `third_party_ctes_value` | `valor_fatura`, `valor_frete`, `third_party_ctes_value`, `valor_operacional` |
| `fit_fhe_cte_status*` | `status_cte`, `status_cte_result` |
| snapshot da carga | `snapshot_em`, `hash_linha` |
