# raster_viagens

## Objetivo de negocio

Registrar viagens Raster para acompanhamento de prazo, fim real de viagem, velocidade, desvios, rota, motorista, veiculo e clientes origem/destino.

## Chaves e deduplicacao

- Grao: 1 linha por solicitacao Raster.
- Chave primaria: `cod_solicitacao`.
- Deduplicacao: upsert por `cod_solicitacao`; paradas relacionadas usam FK para esta chave.

## De/Para JSON API -> SQL

| JSON API | Coluna SQL |
|---|---|
| `CodSolicitacao` / `codSolicitacao` | `cod_solicitacao` |
| `Sequencial` / `sequencial` | `sequencial` |
| `CodFilial` / `codFilial` | `cod_filial` |
| `StatusViagem` / `statusViagem` | `status_viagem` |
| `PlacaVeiculo` / `placaVeiculo` | `placa_veiculo` |
| `PlacaCarreta1` | `placa_carreta1` |
| `PlacaCarreta02` / `PlacaCarreta2` | `placa_carreta2` |
| `PlacaCarreta3` | `placa_carreta3` |
| `CPFMotorista1` / `CpfMotorista1` | `cpf_motorista1` |
| `CPFMotorista2` / `CpfMotorista2` | `cpf_motorista2` |
| `CNPJClienteOrig` | `cnpj_cliente_orig` |
| `CNPJClienteDest` | `cnpj_cliente_dest` |
| `CodIBGECidadeOrig` | `cod_ibge_cidade_orig` |
| `CodIBGECidadeDest` | `cod_ibge_cidade_dest` |
| `DataHoraPrevIni` | `data_hora_prev_ini` |
| `DataHoraPrevFim` | `data_hora_prev_fim` |
| `DataHoraRealIni` | `data_hora_real_ini` |
| `DataHoraRealFim` | `data_hora_real_fim` |
| `DataHoraIdentificouFimViagem` | `data_hora_identificou_fim_viagem` |
| `TempoTotalViagem` | `tempo_total_viagem_min` |
| `DentroPrazo` | `dentro_prazo_raster` |
| `PercentualAtraso` | `percentual_atraso_raster` |
| `RodouForaHorario` | `rodou_fora_horario` |
| `VelocidadeMedia` | `velocidade_media` |
| `EventosVelocidade` | `eventos_velocidade` |
| `DesviosDeRota` | `desvios_de_rota` |
| `Rota.CodRota` | `cod_rota` |
| `Rota.Descricao` | `rota_descricao` |
| `LinkTimeLine` / `LinkTimeline` | `link_timeline` |
| payload completo | `metadata` |
