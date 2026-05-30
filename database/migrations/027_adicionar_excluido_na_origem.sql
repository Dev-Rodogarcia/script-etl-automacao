PRINT 'Migration 027: adicionar soft delete por origem nas tabelas de dominio';
GO

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;
GO

IF OBJECT_ID(N'dbo.coletas', N'U') IS NULL
   OR OBJECT_ID(N'dbo.fretes', N'U') IS NULL
   OR OBJECT_ID(N'dbo.manifestos', N'U') IS NULL
   OR OBJECT_ID(N'dbo.cotacoes', N'U') IS NULL
   OR OBJECT_ID(N'dbo.localizacao_cargas', N'U') IS NULL
   OR OBJECT_ID(N'dbo.contas_a_pagar', N'U') IS NULL
   OR OBJECT_ID(N'dbo.faturas_por_cliente', N'U') IS NULL
   OR OBJECT_ID(N'dbo.inventario', N'U') IS NULL
   OR OBJECT_ID(N'dbo.sinistros', N'U') IS NULL
   OR OBJECT_ID(N'dbo.dim_usuarios', N'U') IS NULL
   OR OBJECT_ID(N'dbo.raster_viagens', N'U') IS NULL
   OR OBJECT_ID(N'dbo.raster_viagem_paradas', N'U') IS NULL
BEGIN
    RAISERROR('Uma ou mais tabelas de dominio nao foram encontradas. Execute os scripts-base antes da migration 027.', 16, 1);
    RETURN;
END;
GO

IF COL_LENGTH(N'dbo.coletas', N'excluido_na_origem') IS NULL
BEGIN
    ALTER TABLE dbo.coletas
    ADD excluido_na_origem BIT NOT NULL
        CONSTRAINT DF_coletas_excluido_na_origem DEFAULT (0) WITH VALUES;
    PRINT 'Coluna dbo.coletas.excluido_na_origem criada.';
END;
GO

IF COL_LENGTH(N'dbo.fretes', N'excluido_na_origem') IS NULL
BEGIN
    ALTER TABLE dbo.fretes
    ADD excluido_na_origem BIT NOT NULL
        CONSTRAINT DF_fretes_excluido_na_origem DEFAULT (0) WITH VALUES;
    PRINT 'Coluna dbo.fretes.excluido_na_origem criada.';
END;
GO

IF COL_LENGTH(N'dbo.manifestos', N'excluido_na_origem') IS NULL
BEGIN
    ALTER TABLE dbo.manifestos
    ADD excluido_na_origem BIT NOT NULL
        CONSTRAINT DF_manifestos_excluido_na_origem DEFAULT (0) WITH VALUES;
    PRINT 'Coluna dbo.manifestos.excluido_na_origem criada.';
END;
GO

IF COL_LENGTH(N'dbo.cotacoes', N'excluido_na_origem') IS NULL
BEGIN
    ALTER TABLE dbo.cotacoes
    ADD excluido_na_origem BIT NOT NULL
        CONSTRAINT DF_cotacoes_excluido_na_origem DEFAULT (0) WITH VALUES;
    PRINT 'Coluna dbo.cotacoes.excluido_na_origem criada.';
END;
GO

IF COL_LENGTH(N'dbo.localizacao_cargas', N'excluido_na_origem') IS NULL
BEGIN
    ALTER TABLE dbo.localizacao_cargas
    ADD excluido_na_origem BIT NOT NULL
        CONSTRAINT DF_localizacao_cargas_excluido_na_origem DEFAULT (0) WITH VALUES;
    PRINT 'Coluna dbo.localizacao_cargas.excluido_na_origem criada.';
END;
GO

IF COL_LENGTH(N'dbo.contas_a_pagar', N'excluido_na_origem') IS NULL
BEGIN
    ALTER TABLE dbo.contas_a_pagar
    ADD excluido_na_origem BIT NOT NULL
        CONSTRAINT DF_contas_a_pagar_excluido_na_origem DEFAULT (0) WITH VALUES;
    PRINT 'Coluna dbo.contas_a_pagar.excluido_na_origem criada.';
END;
GO

IF COL_LENGTH(N'dbo.faturas_por_cliente', N'excluido_na_origem') IS NULL
BEGIN
    ALTER TABLE dbo.faturas_por_cliente
    ADD excluido_na_origem BIT NOT NULL
        CONSTRAINT DF_faturas_por_cliente_excluido_na_origem DEFAULT (0) WITH VALUES;
    PRINT 'Coluna dbo.faturas_por_cliente.excluido_na_origem criada.';
END;
GO

IF COL_LENGTH(N'dbo.inventario', N'excluido_na_origem') IS NULL
BEGIN
    ALTER TABLE dbo.inventario
    ADD excluido_na_origem BIT NOT NULL
        CONSTRAINT DF_inventario_excluido_na_origem DEFAULT (0) WITH VALUES;
    PRINT 'Coluna dbo.inventario.excluido_na_origem criada.';
END;
GO

IF COL_LENGTH(N'dbo.sinistros', N'excluido_na_origem') IS NULL
BEGIN
    ALTER TABLE dbo.sinistros
    ADD excluido_na_origem BIT NOT NULL
        CONSTRAINT DF_sinistros_excluido_na_origem DEFAULT (0) WITH VALUES;
    PRINT 'Coluna dbo.sinistros.excluido_na_origem criada.';
END;
GO

IF COL_LENGTH(N'dbo.dim_usuarios', N'excluido_na_origem') IS NULL
BEGIN
    ALTER TABLE dbo.dim_usuarios
    ADD excluido_na_origem BIT NOT NULL
        CONSTRAINT DF_dim_usuarios_excluido_na_origem DEFAULT (0) WITH VALUES;
    PRINT 'Coluna dbo.dim_usuarios.excluido_na_origem criada.';
END;
GO

IF COL_LENGTH(N'dbo.raster_viagens', N'excluido_na_origem') IS NULL
BEGIN
    ALTER TABLE dbo.raster_viagens
    ADD excluido_na_origem BIT NOT NULL
        CONSTRAINT DF_raster_viagens_excluido_na_origem DEFAULT (0) WITH VALUES;
    PRINT 'Coluna dbo.raster_viagens.excluido_na_origem criada.';
END;
GO

IF COL_LENGTH(N'dbo.raster_viagem_paradas', N'excluido_na_origem') IS NULL
BEGIN
    ALTER TABLE dbo.raster_viagem_paradas
    ADD excluido_na_origem BIT NOT NULL
        CONSTRAINT DF_raster_viagem_paradas_excluido_na_origem DEFAULT (0) WITH VALUES;
    PRINT 'Coluna dbo.raster_viagem_paradas.excluido_na_origem criada.';
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_coletas_ativos_origem' AND object_id = OBJECT_ID(N'dbo.coletas'))
    CREATE INDEX IX_coletas_ativos_origem ON dbo.coletas(id) WHERE excluido_na_origem = 0;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_fretes_ativos_origem' AND object_id = OBJECT_ID(N'dbo.fretes'))
    CREATE INDEX IX_fretes_ativos_origem ON dbo.fretes(id) WHERE excluido_na_origem = 0;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_manifestos_ativos_origem' AND object_id = OBJECT_ID(N'dbo.manifestos'))
    CREATE INDEX IX_manifestos_ativos_origem ON dbo.manifestos(chave_merge_hash) WHERE excluido_na_origem = 0;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_cotacoes_ativos_origem' AND object_id = OBJECT_ID(N'dbo.cotacoes'))
    CREATE INDEX IX_cotacoes_ativos_origem ON dbo.cotacoes(sequence_code) WHERE excluido_na_origem = 0;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_localizacao_cargas_ativos_origem' AND object_id = OBJECT_ID(N'dbo.localizacao_cargas'))
    CREATE INDEX IX_localizacao_cargas_ativos_origem ON dbo.localizacao_cargas(sequence_number) WHERE excluido_na_origem = 0;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_contas_a_pagar_ativos_origem' AND object_id = OBJECT_ID(N'dbo.contas_a_pagar'))
    CREATE INDEX IX_contas_a_pagar_ativos_origem ON dbo.contas_a_pagar(sequence_code) WHERE excluido_na_origem = 0;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_faturas_por_cliente_ativos_origem' AND object_id = OBJECT_ID(N'dbo.faturas_por_cliente'))
    CREATE INDEX IX_faturas_por_cliente_ativos_origem ON dbo.faturas_por_cliente(unique_id) WHERE excluido_na_origem = 0;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_inventario_ativos_origem' AND object_id = OBJECT_ID(N'dbo.inventario'))
    CREATE INDEX IX_inventario_ativos_origem ON dbo.inventario(identificador_unico) WHERE excluido_na_origem = 0;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_sinistros_ativos_origem' AND object_id = OBJECT_ID(N'dbo.sinistros'))
    CREATE INDEX IX_sinistros_ativos_origem ON dbo.sinistros(identificador_unico) WHERE excluido_na_origem = 0;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_dim_usuarios_ativos_origem' AND object_id = OBJECT_ID(N'dbo.dim_usuarios'))
    CREATE INDEX IX_dim_usuarios_ativos_origem ON dbo.dim_usuarios(user_id) WHERE excluido_na_origem = 0;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_raster_viagens_ativos_origem' AND object_id = OBJECT_ID(N'dbo.raster_viagens'))
    CREATE INDEX IX_raster_viagens_ativos_origem ON dbo.raster_viagens(cod_solicitacao) WHERE excluido_na_origem = 0;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_raster_viagem_paradas_ativos_origem' AND object_id = OBJECT_ID(N'dbo.raster_viagem_paradas'))
    CREATE INDEX IX_raster_viagem_paradas_ativos_origem ON dbo.raster_viagem_paradas(cod_solicitacao, ordem) WHERE excluido_na_origem = 0;
GO

:r views\011_criar_view_faturas_por_cliente_powerbi.sql
:r views\012_criar_view_fretes_powerbi.sql
:r views\013_criar_view_coletas_powerbi.sql
:r views\015_criar_view_cotacoes_powerbi.sql
:r views\016_criar_view_contas_a_pagar_powerbi.sql
:r views\017_criar_view_localizacao_cargas_powerbi.sql
:r views\018_criar_view_manifestos_powerbi.sql
:r views\020_criar_view_inventario_powerbi.sql
:r views\021_criar_view_sinistros_powerbi.sql
:r views\022_criar_view_raster_sm_transit_time.sql
:r views-dimensao\020_criar_view_dim_clientes.sql
:r views-dimensao\024_criar_view_dim_usuarios.sql

IF OBJECT_ID(N'dbo.schema_migrations', N'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM dbo.schema_migrations WHERE migration_id = N'027_adicionar_excluido_na_origem')
BEGIN
    INSERT INTO dbo.schema_migrations (migration_id, notes)
    VALUES (
        N'027_adicionar_excluido_na_origem',
        N'Adiciona flag de soft delete por origem, indices filtrados de ativos e filtros nas views analiticas.'
    );
END;
GO

PRINT 'Soft delete por origem adicionado com sucesso.';
GO
