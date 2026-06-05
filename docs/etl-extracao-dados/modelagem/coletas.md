# coletas

## Objetivo de negocio

Representar solicitacoes de coleta vindas do GraphQL (`pick`) para operacao, SLA de atendimento, indicadores de ocorrencia e relacionamento com manifestos.

## Chaves e deduplicacao

- Grao: 1 linha por coleta GraphQL.
- Chave primaria: `id`.
- Chave unica de negocio: `sequence_code`.
- Deduplicacao: o upsert usa `sequence_code` como chave de negocio, mantendo idempotencia entre reprocessamentos; `metadata` preserva o payload completo.

## De/Para JSON API -> SQL

| JSON API | Coluna SQL |
|---|---|
| `id` | `id` |
| `sequenceCode` | `sequence_code` |
| `requestDate` | `request_date` |
| `requestHour` | `request_hour` |
| `serviceDate` | `service_date` |
| `status` | `status` |
| `invoicesValue` | `total_value` |
| `invoicesWeight` | `total_weight` |
| `invoicesVolumes` | `total_volumes` |
| `taxedWeight` | `taxed_weight` |
| `customer.name` | `cliente_nome` |
| `customer.cnpj` | `cliente_doc` |
| `pickAddress.line1` | `local_coleta` |
| `pickAddress.number` | `numero_coleta` |
| `pickAddress.line2` | `complemento_coleta` |
| `pickAddress.city.name` | `cidade_coleta` |
| `pickAddress.neighborhood` | `bairro_coleta` |
| `pickAddress.city.state.code` | `uf_coleta` |
| `pickAddress.postalCode` | `cep_coleta` |
| `pickAddress.city.name + state.code` | `pick_region` |
| `corporation.id` | `filial_id` |
| `corporation.person.nickname` | `filial_nome` |
| `user.name` | `usuario_nome` |
| `finishDate` | `finish_date` |
| `manifestItemPickId` | `manifest_item_pick_id` |
| `vehicleTypeId` | `vehicle_type_id` |
| `cancellationReason` | `cancellation_reason` |
| `cancellationUserId` | `cancellation_user_id` |
| `destroyReason` | `destroy_reason` |
| `destroyUserId` | `destroy_user_id` |
| `statusUpdatedAt` | `status_updated_at` |
| `status` traduzido | `last_occurrence` |
| `status + cancellationReason` | `acao_ocorrencia` |
| `status` classificado | `numero_tentativas` |
| payload completo | `metadata` |
