# fato_fretes_faturamento

## Objetivo de negocio

Materializar uma fato granular de fretes elegiveis ou inelegiveis para faturamento, com status real de CT-e e receita ajustada.

## Chaves e deduplicacao

- Grao: 1 linha por frete e data de referencia de faturamento ajustada para dia util.
- Chave unica principal: `UX_fato_ff_frete_data (frete_id, data_referencia_faturamento_date)`.
- Chave auxiliar: indices por `frete_id`, data, filial e status.
- Deduplicacao: `dbo.sp_carga_fato_fretes_faturamento` rankeia faturas por `chave_cte`, pega a melhor evidencia fiscal e faz `MERGE` por frete/data com `hash_linha`.

## Carga e orquestracao

- Procedure de carga: `dbo.sp_carga_fato_fretes_faturamento`.
- Orquestracao intradia: incluida em `etl.bi.procedures.target` e executada pelo proprio `--loop-daemon-run` apos cada ciclo de extracao bem-sucedido.
- Orquestracao complementar: tambem pode ser chamada pelo `MATERIALIZAR_FATOS_BI_POST_RUN`, por `database/executar_database.bat --com-cargas` e pela janela noturna resiliente de `10-expurgo-orfaos-noturno.ps1`.

## De/Para JSON API -> SQL

Esta fato e derivada de `dbo.fretes`, `dbo.localizacao_cargas` e `dbo.faturas_por_cliente`.

| JSON API / origem bruta | Coluna SQL da fato |
|---|---|
| GraphQL `fretes.id` | `frete_id` |
| GraphQL `fretes.corporationSequenceNumber` | `numero_minuta` |
| GraphQL `fretes.serviceAt` | `data_frete`, `data_frete_date` |
| GraphQL `fretes.data_referencia_faturamento` materializada | `data_referencia_faturamento_real`, `data_referencia_faturamento_real_date`, `data_referencia_faturamento_real_yyyymm` |
| `dbo.dim_calendario.data_referencia_faturamento` | `data_referencia_faturamento`, `data_referencia_faturamento_date`, `data_referencia_faturamento_yyyymm`, `is_data_faturamento_retroagida` |
| GraphQL `fretes.cte.*` | `chave_cte`, `numero_cte`, `serie_cte`, `data_emissao_cte` |
| DataExport `faturas_por_cliente.fit_fhe_cte_status*` por `chave_cte` | `status_cte_real`, `status_cte_result`, `is_cte_cancelado` |
| GraphQL `fretes.corporation.*` | `filial_id`, `filial_nome`, `filial_key`, `filial_cnpj` |
| GraphQL `fretes.payer/sender/receiver.*` | participantes e documentos |
| GraphQL `fretes.total`, `subtotal`, componentes financeiros | `valor_frete_original`, `receita_bruta_original`, `valor_frete`, `receita_bruta` |
| GraphQL `fretes.status`, `courtesy`, `type`, CT-e e regras da procedure | flags `is_documento_cte`, `is_cortesia`, `is_bloqueio_faturamento`, `is_elegivel_origem`, `is_elegivel_faturamento` |
| DataExport `localizacao_cargas.destination_*` | `responsavel_regiao_destino`, `regiao_destino` |
| snapshot da carga | `snapshot_em`, `hash_linha` |
