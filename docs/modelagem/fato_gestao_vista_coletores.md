# fato_gestao_vista_coletores

## Objetivo de negocio

Materializar utilizacao de coletores por data, filial e classificacao com base em manifestos, inventario e fretes.

## Chaves e deduplicacao

- Grao: 1 linha por `data_referencia`, `filial_key` e `classificacao`.
- Chave unica: `UX_fato_gv_coletores_data_filial_classif (data_referencia, filial_key, classificacao)`.
- Deduplicacao: a procedure `dbo.sp_carga_fato_gestao_vista_coletores` deduplica manifestos por chave operacional, agrega inventario e faz `MERGE` idempotente com `hash_linha`.

## Carga e orquestracao

- Procedure de carga: `dbo.sp_carga_fato_gestao_vista_coletores`.
- Orquestracao intradia: procedure permitida no `FatoMaterializacaoScheduler` via `ETL_MATERIALIZACAO_FATOS_BI_PROCEDURES`; quando configurada, roda em processo paralelo a cada 60 minutos por padrao.
- Orquestracao complementar: chamada pelo `MATERIALIZAR_FATOS_BI_POST_RUN`, por `database/executar_database.bat` e pela janela noturna resiliente de `10-expurgo-orfaos-noturno.ps1`.

## De/Para JSON API -> SQL

Esta fato e derivada de `dbo.manifestos`, `dbo.inventario` e `dbo.fretes`.

| JSON API / origem bruta | Coluna SQL da fato |
|---|---|
| DataExport `manifestos.sequence_code` / chave calculada | base de deduplicacao de manifestos |
| DataExport `manifestos.finished_at`, `closed_at`, `created_at` | `data_referencia` |
| DataExport `manifestos.mft_crn_psn_nickname` e aliases SQL | `filial`, `filial_key` |
| DataExport `manifestos.mft_man_name` | `classificacao` |
| DataExport `manifestos.pick_manifest_items_count`, `manifest_items_count` | `manifestos_emitidos`, `total_manifestos` |
| DataExport `inventario.sequence_code`, `numero_minuta`, `status`, `read_volumes` | `manifestos_bipados`, `manifestos_incompletos` |
| GraphQL `fretes.corporationSequenceNumber`, `corporation.*` | filial de apoio quando manifesto/inventario precisa de minuta |
| regras da procedure | `pct_utilizacao`, `tem_ordens_conferencia`, `tem_manifestos_bipaveis`, `is_filial_operacional`, `is_linha_valida_indicador` |
| snapshot da carga | `snapshot_em`, `hash_linha` |
