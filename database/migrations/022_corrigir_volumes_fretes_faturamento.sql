PRINT 'Migration 022: corrigir Volumes em vw_fretes_powerbi usando Localizador de Cargas';
GO

IF OBJECT_ID(N'dbo.fretes', N'U') IS NULL
BEGIN
    RAISERROR('Tabela dbo.fretes nao encontrada. Execute os scripts-base antes da migration 022.', 16, 1);
    RETURN;
END;
GO

IF OBJECT_ID(N'dbo.localizacao_cargas', N'U') IS NULL
BEGIN
    RAISERROR('Tabela dbo.localizacao_cargas nao encontrada. Execute os scripts-base antes da migration 022.', 16, 1);
    RETURN;
END;
GO

:r views\012_criar_view_fretes_powerbi.sql

IF OBJECT_ID(N'dbo.schema_migrations', N'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM dbo.schema_migrations WHERE migration_id = N'022_corrigir_volumes_fretes_faturamento')
BEGIN
    INSERT INTO dbo.schema_migrations (migration_id, notes)
    VALUES (
        N'022_corrigir_volumes_fretes_faturamento',
        N'Publica Volumes em vw_fretes_powerbi pela fonte oficial localizacao_cargas.invoices_volumes, com fallback para fretes e zero analitico.'
    );
END;
GO

PRINT 'Volumes da view vw_fretes_powerbi corrigidos com sucesso.';
GO
