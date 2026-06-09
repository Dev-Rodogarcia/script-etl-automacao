# dim_calendario

## Objetivo de negocio

Centralizar regras de dias uteis para fatos analiticos, especialmente a retroacao do faturamento de finais de semana, feriados nacionais e pontos facultativos operacionais para o ultimo dia util anterior.

## Grao e chaves

- Grao: 1 linha por data civil.
- Chave primaria: `data`.
- Chave numerica: `data_key` no formato `yyyyMMdd`.
- Campo de negocio principal: `data_referencia_faturamento`, que aponta para a propria data quando `is_dia_util = 1` ou para o ultimo dia util anterior quando a data nao e util.

## Manutencao

- A carga estatica cobre `2019-12-01` a `2032-12-31`.
- Feriados nacionais fixos e moveis brasileiros sao populados por SQL em `database/tabelas/008_criar_tabela_dim_calendario.sql` e `database/migrations/039_criar_dim_calendario_referencia_faturamento.sql`.
- `Carnaval` e `Corpus Christi` ficam separados como `is_ponto_facultativo`, permitindo ajustar a politica operacional sem confundir com feriado nacional.
- Sustentacao deve revisar anualmente os pontos facultativos e feriados novos por lei federal antes de dezembro, recalculando `is_dia_util` e `data_referencia_faturamento` apos qualquer alteracao.

## Consumo

`dbo.sp_carga_fato_fretes_faturamento` consulta a dimensao durante a carga. O Dashboard consome a fato ja materializada, agrupando por `data_referencia_faturamento_date` sem executar regra de calendario em runtime.
