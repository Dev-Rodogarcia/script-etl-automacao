# Modelagem do ETL

Documentacao tecnica oficial das entidades transacionais e fatos principais do pipeline `etl-extracao-dados`.

Arquivos:

- `coletas.md`
- `fretes.md`
- `manifestos.md`
- `cotacoes.md`
- `localizacao_cargas.md`
- `contas_a_pagar.md`
- `faturas_por_cliente.md`
- `inventario.md`
- `sinistros.md`
- `raster_viagens.md`
- `raster_viagem_paradas.md`
- `fato_gestao_vista_fretes.md`
- `fato_gestao_vista_coletores.md`
- `fato_fretes_faturamento.md`
- `fato_gestao_vista_faturas.md`

As colunas `metadata`, `data_extracao` e `excluido_na_origem` sao padroes tecnicos: `metadata` guarda o payload completo ou propriedades nao promovidas, `data_extracao` registra a materializacao local, e `excluido_na_origem` preserva historico para expurgo logico.
