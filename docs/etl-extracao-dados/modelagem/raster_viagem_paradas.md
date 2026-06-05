# raster_viagem_paradas

## Objetivo de negocio

Registrar paradas/coletas/entregas vinculadas a uma viagem Raster para medir cumprimento de janela, localizacao e ocorrencias de chegada/saida.

## Chaves e deduplicacao

- Grao: 1 linha por parada da viagem Raster.
- Chave primaria composta: `(cod_solicitacao, ordem)`.
- Chave estrangeira: `cod_solicitacao -> raster_viagens.cod_solicitacao`.
- Deduplicacao: quando `Ordem` nao vem no payload, o mapper usa a posicao da lista como fallback.

## De/Para JSON API -> SQL

| JSON API | Coluna SQL |
|---|---|
| `viagem.CodSolicitacao` | `cod_solicitacao` |
| `Ordem` / posicao no array | `ordem` |
| `Tipo` | `tipo` |
| `CodIBGECidade` | `cod_ibge_cidade` |
| `CNPJCliente` | `cnpj_cliente` |
| `CodigoCliente` | `codigo_cliente` |
| `DataHoraPrevChegada` | `data_hora_prev_chegada` |
| `DataHoraPrevSaida` | `data_hora_prev_saida` |
| `DataHoraRealChegada` | `data_hora_real_chegada` |
| `DataHoraRealSaida` | `data_hora_real_saida` |
| `Latitude` | `latitude` |
| `Longitude` | `longitude` |
| `DentroPrazo` | `dentro_prazo_raster` |
| `DiferencaTempo` | `diferenca_tempo_raster` |
| `KmPercorridoEntrega` | `km_percorrido_entrega` |
| `KmRestanteEntrega` | `km_restante_entrega` |
| `ChegouNaEntrega` | `chegou_na_entrega` |
| `DataHoraUltimaPosicao` | `data_hora_ultima_posicao` |
| `LatitudeUltimaPosicao` | `latitude_ultima_posicao` |
| `LongitudeUltimaPosicao` | `longitude_ultima_posicao` |
| `ReferenciaUltimaPosicao` | `referencia_ultima_posicao` |
| payload completo | `metadata` |
