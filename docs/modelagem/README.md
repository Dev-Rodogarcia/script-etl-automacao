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

## Topologia operacional dos fatos BI

A materializacao das tabelas fato analiticas deixou de depender exclusivamente da janela noturna. O modo de loop de producao (`scripts/windows/00-PRODUCAO_START.bat`, opcao 02) executa a materializacao no proprio `--loop-daemon-run`, logo apos cada ciclo de extracao bem-sucedido, por meio de `FatoMaterializacaoService`.

A lista de fatos-alvo e controlada por `ETL_BI_PROCEDURES_TARGET` / `etl.bi.procedures.target` e cobre `fato_fretes_faturamento`, `fato_gestao_vista_faturas`, `fato_gestao_vista_fretes` e `fato_gestao_vista_coletores`. A chave legada `ETL_MATERIALIZACAO_FATOS_BI_PROCEDURES` / `etl.materializacao.fatos_bi.procedures` continua aceita para execucoes pontuais por procedure.

O script `scripts/windows/10-expurgo-orfaos-noturno.ps1` continua executando a reconciliacao noturna e dispara as cargas materializadas do BI ao final. A falha do expurgo logico, entretanto, nao bloqueia mais essa chamada: o script registra evento/marcador de falha do expurgo e segue para `Invoke-MaterializacaoFatosBi`. Sustentacao deve tratar falhas de expurgo e falhas de materializacao como incidentes separados.

Execucoes pontuais pelo menu de producao ainda podem chamar `MATERIALIZAR_FATOS_BI_POST_RUN` apos extracoes bem-sucedidas. Portanto, a topologia vigente tem tres pontos de carga: loop daemon intradia, pos-run operacional e janela noturna resiliente.

## Contrato temporal

- Fatos BI usam `snapshot_em` gerado nas stored procedures com `SYSUTCDATETIME()`. Ao expor snapshots analiticos no Dashboard, `TemporalJsonUtils` sela o timestamp em UTC e garante sufixo `Z`.
- Logs de auditoria operacional do ETL (`log_extracoes.timestamp_inicio`, `log_extracoes.timestamp_fim`, `data_extracao` das tabelas base e mensagens de scripts) permanecem em Local Time da JVM/SQL Server local, normalmente como `DATETIME2` sem offset.
- Nao compare diretamente timestamps analiticos UTC com auditoria local sem converter explicitamente a zona. Para filtros de periodo no Dashboard, a janela de negocio usa `America/Sao_Paulo` antes de consultar campos `DATETIMEOFFSET`.
