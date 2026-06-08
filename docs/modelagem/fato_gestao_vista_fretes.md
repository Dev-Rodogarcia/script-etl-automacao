# fato_gestao_vista_fretes

## Objetivo de negocio

Materializar indicadores de Gestao a Vista de fretes para entrega no prazo (`PE`) e cubagem (`CB`), evitando calculos pesados em views do BI.

## Chaves e deduplicacao

- Grao: 1 linha por `indicador_codigo`, `numero_minuta` e `data_referencia`.
- Chave unica: `UX_fato_gv_fretes_indicador_minuta (indicador_codigo, numero_minuta, data_referencia)`.
- Deduplicacao: a procedure `dbo.sp_carga_fato_gestao_vista_fretes` usa `MERGE` idempotente com `hash_linha`; registros ausentes podem ser marcados com `excluido_na_origem`.

## Carga e orquestracao

- Procedure de carga: `dbo.sp_carga_fato_gestao_vista_fretes`.
- Orquestracao intradia: procedure permitida no `FatoMaterializacaoScheduler` via `ETL_MATERIALIZACAO_FATOS_BI_PROCEDURES`; quando configurada, roda em processo paralelo a cada 60 minutos por padrao.
- Orquestracao complementar: chamada pelo `MATERIALIZAR_FATOS_BI_POST_RUN`, por `database/executar_database.bat` e pela janela noturna resiliente de `10-expurgo-orfaos-noturno.ps1`.

## De/Para JSON API -> SQL

Esta fato e derivada de `dbo.fretes` e `dbo.localizacao_cargas`.

| JSON API / origem bruta | Coluna SQL da fato |
|---|---|
| GraphQL `fretes.id` | `frete_id_origem` |
| GraphQL `fretes.corporationSequenceNumber` | `numero_minuta` |
| GraphQL `fretes.serviceAt` | `data_frete`, `data_frete_date` |
| GraphQL `fretes.deliveryPredictionDate` ou DataExport `localizacao_cargas.fit_dpn_delivery_prediction_at` | `data_previsao_entrega` |
| GraphQL `fretes.finishedAt` ou `fit_dpn_performance_finished_at` | `data_finalizacao_performance` |
| GraphQL `fretes.corporation.*` | `filial_emissora`, `filial_emissora_key` |
| GraphQL `fretes.payer.*` | `pagador_nome`, `pagador_documento`, `pagador_documento_key` |
| GraphQL `fretes.receiver/destinationCity.*` | `destino_cidade`, `destino_uf` |
| GraphQL `fretes.status`, `cte.*`, `courtesy` | flags `is_cancelado`, `is_cortesia`, `is_documento_emitido` |
| GraphQL pesos/cubagem (`taxedWeight`, `realWeight`, `cubagesCubedWeight`, `totalCubicVolume`) | metricas e flags de cubagem |
| DataExport `localizacao_cargas.status`, `classification`, `destination_*` | status/regiao/filial de performance |
| regras da procedure | `indicador_codigo`, `performance_status_codigo`, flags de elegibilidade |
| snapshot da carga | `snapshot_em`, `hash_linha` |
