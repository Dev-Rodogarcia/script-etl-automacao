PRINT 'Migration 025: materializar chave de responsavel por regiao de destino';
GO

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;
GO

IF OBJECT_ID(N'dbo.fretes', N'U') IS NULL
BEGIN
    RAISERROR('Tabela dbo.fretes nao encontrada. Execute os scripts-base antes da migration 025.', 16, 1);
    RETURN;
END;
GO

IF OBJECT_ID(N'dbo.localizacao_cargas', N'U') IS NULL
BEGIN
    RAISERROR('Tabela dbo.localizacao_cargas nao encontrada. Execute os scripts-base antes da migration 025.', 16, 1);
    RETURN;
END;
GO

IF COL_LENGTH('dbo.fretes', 'filial_nome_key') IS NULL
BEGIN
    ALTER TABLE dbo.fretes
    ADD filial_nome_key AS NULLIF(LOWER(LTRIM(RTRIM(filial_nome))), N'') PERSISTED;

    PRINT 'Coluna computada dbo.fretes.filial_nome_key criada.';
END
ELSE
BEGIN
    PRINT 'Coluna dbo.fretes.filial_nome_key ja existe.';
END;
GO

IF COL_LENGTH('dbo.localizacao_cargas', 'destination_branch_key') IS NULL
BEGIN
    ALTER TABLE dbo.localizacao_cargas
    ADD destination_branch_key AS NULLIF(LOWER(LTRIM(RTRIM(destination_branch_nickname))), N'') PERSISTED;

    PRINT 'Coluna computada dbo.localizacao_cargas.destination_branch_key criada.';
END
ELSE
BEGIN
    PRINT 'Coluna dbo.localizacao_cargas.destination_branch_key ja existe.';
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_fretes_faturamento_responsavel_key' AND object_id = OBJECT_ID(N'dbo.fretes'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_fretes_faturamento_responsavel_key
    ON dbo.fretes(data_referencia_faturamento DESC, filial_nome_key, is_elegivel_faturamento)
    INCLUDE (id, corporation_sequence_number, valor_total, subtotal, status, filial_nome, pagador_nome, classificacao_nome);

    PRINT 'Indice IX_fretes_faturamento_responsavel_key criado.';
END
ELSE
BEGIN
    PRINT 'Indice IX_fretes_faturamento_responsavel_key ja existe.';
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_fretes_performance_previsao_key' AND object_id = OBJECT_ID(N'dbo.fretes'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_fretes_performance_previsao_key
    ON dbo.fretes(data_previsao_entrega, filial_nome_key)
    INCLUDE (id, corporation_sequence_number, finished_at, fit_dpn_performance_finished_at, status, valor_notas, taxed_weight, filial_nome, data_extracao);

    PRINT 'Indice IX_fretes_performance_previsao_key criado.';
END
ELSE
BEGIN
    PRINT 'Indice IX_fretes_performance_previsao_key ja existe.';
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_localizacao_destination_branch_key' AND object_id = OBJECT_ID(N'dbo.localizacao_cargas'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_localizacao_destination_branch_key
    ON dbo.localizacao_cargas(destination_branch_key, sequence_number)
    INCLUDE (destination_branch_nickname, destination_location_name, predicted_delivery_at, invoices_volumes, data_extracao);

    PRINT 'Indice IX_localizacao_destination_branch_key criado.';
END
ELSE
BEGIN
    PRINT 'Indice IX_localizacao_destination_branch_key ja existe.';
END;
GO

:r views\012_criar_view_fretes_powerbi.sql

IF OBJECT_ID(N'dbo.schema_migrations', N'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM dbo.schema_migrations WHERE migration_id = N'025_materializar_chave_responsavel_destino')
BEGIN
    INSERT INTO dbo.schema_migrations (migration_id, notes)
    VALUES (
        N'025_materializar_chave_responsavel_destino',
        N'Materializa chaves normalizadas de filial/responsavel de destino e publica Responsavel Regiao Destino Key na view de fretes.'
    );
END;
GO

PRINT 'Chave de responsavel por regiao de destino materializada com sucesso.';
GO
