# Divergências entre código e schema

Este arquivo registra o que foi encontrado ao comparar entidades/repositórios Java com os scripts SQL canônicos em `database/`.

## Regra canônica

- Toda estrutura persistente de banco deve existir em `database/*.sql`.
- DDL embutido no Java pode continuar como fallback defensivo/idempotente, mas não deve ser a única fonte de criação.
- O `README.md` descreve a estrutura física efetiva das tabelas; campos lidos pela aplicação e mantidos só em `metadata` precisam ser sinalizados explicitamente.

## Divergências estruturais encontradas e tratadas

### `dbo.schema_migrations`

- Situação encontrada: a tabela existia como migration (`database/migrations/001_criar_tabela_schema_migrations.sql`), mas não tinha script-base em `database/tabelas/`.
- Ajuste aplicado:
  - criado `database/tabelas/018_criar_tabela_schema_migrations.sql`
  - incluído no fluxo de `database/executar_database.bat`
- Estado atual: recriação manual do schema pela pasta `database/tabelas` já materializa a tabela.

### `dbo.sys_execution_history`

- Origem da divergência: `src/main/java/br/com/extrator/persistencia/repositorio/ExecutionHistoryRepository.java`
- Divergências encontradas entre runtime e script-base antigo:
  - `start_time` e `end_time` em `DATETIME2` no Java, mas `DATETIME` no `.sql`
  - `created_at` existia no Java e não existia no script-base
  - índice `IX_sys_execution_history_start_time` existia no Java e não existia no script-base
  - `total_records` tinha `DEFAULT 0` no Java e não tinha default no script-base
- Ajuste aplicado:
  - alinhado `database/tabelas/012_criar_tabela_sys_execution_history.sql`
  - criada `database/migrations/005_alinhar_sys_execution_history_schema.sql`
- Estado atual:
  - recriação do zero já nasce correta pelo script-base
  - bancos existentes podem ser normalizados pela migration `005`

### `dbo.etl_invalid_records`

- Origem da divergência: `src/main/java/br/com/extrator/persistencia/repositorio/InvalidRecordAuditRepository.java`
- Situação encontrada: a tabela só era criada sob demanda pelo runtime, fora de `database/tabelas/`
- Ajuste aplicado:
  - criado `database/tabelas/019_criar_tabela_etl_invalid_records.sql`
  - incluído no fluxo de `database/executar_database.bat`
- Estado atual: a tabela já faz parte do schema canônico em `database/`, mantendo o fallback Java apenas como proteção adicional.

## DDL defensivo que continua no Java, mas já está coberto por SQL canônico

### SQL Server

- `ExecutionHistoryRepository` continua com DDL idempotente para `dbo.sys_execution_history`, mas a fonte canônica agora está em `database/tabelas/012_criar_tabela_sys_execution_history.sql` e `database/migrations/005_alinhar_sys_execution_history_schema.sql`.
- `AuditorEstruturaApi` continua com DDL idempotente para `dbo.sys_auditoria_temp`, já coberta por `database/tabelas/013_criar_tabela_sys_auditoria_temp.sql`.
- `InvalidRecordAuditRepository` continua com DDL idempotente para `dbo.etl_invalid_records`, já coberta por `database/tabelas/019_criar_tabela_etl_invalid_records.sql`.

### SQLite local

- `SegurancaRepository` inicializa `users` e `auth_audit` no runtime, mas o schema equivalente está em `database/security_sqlite/001_init_auth_schema.sqlite.sql`.

## Divergências de modelagem ainda importantes para pesquisa

### `dbo.coletas`: entidade carrega mais do que a tabela promove

- Origem principal:
  - `src/main/java/br/com/extrator/persistencia/entidade/ColetaEntity.java`
  - `src/main/java/br/com/extrator/integracao/mapeamento/graphql/coletas/ColetaMapper.java`
  - `src/main/java/br/com/extrator/persistencia/repositorio/ColetaRepository.java`
- Campos carregados pela entidade/API, mas não promovidos a colunas próprias hoje:
  - `clienteId`
  - `filialCnpj`
  - `usuarioId`
  - `requester`
  - `comments`
  - `agentId`
  - `serviceStartHour`
  - `serviceEndHour`
  - `cargoClassificationId`
  - `costCenterId`
  - `invoicesCubedWeight`
  - `lunchBreakEndHour`
  - `lunchBreakStartHour`
  - `notificationEmail`
  - `notificationPhone`
  - `pickTypeId`
  - `pickupLocationId`
- Estado atual: esses dados seguem acessíveis só dentro de `metadata` na tabela `dbo.coletas`.

### `dbo.dim_usuarios`: dois caminhos de escrita com profundidade diferente

- Origem principal:
  - `src/main/java/br/com/extrator/persistencia/repositorio/UsuarioSistemaRepository.java`
  - `src/main/java/br/com/extrator/features/usuarios/persistencia/sqlserver/SqlServerUsuariosEstadoRepository.java`
- Comportamento observado:
  - `UsuarioSistemaRepository` atualiza apenas `user_id`, `nome` e `data_atualizacao`
  - `SqlServerUsuariosEstadoRepository` trata a estrutura completa atual, incluindo `ativo`, `origem_atualizado_em`, `ultima_extracao_em` e histórico em `dbo.dim_usuarios_historico`
- Estado atual: para documentação e pesquisa, considerar a forma expandida da tabela, que é a que representa o estado atual do sistema.

## Pontos conferidos sem divergência estrutural relevante

- `dbo.sys_auditoria_temp`: runtime e `database/tabelas/013_criar_tabela_sys_auditoria_temp.sql` estão coerentes.
- SQLite `users` e `auth_audit`: `SegurancaRepository` e `database/security_sqlite/001_init_auth_schema.sqlite.sql` estão coerentes.
