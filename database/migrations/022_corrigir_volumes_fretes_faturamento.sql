PRINT 'Migration 022: ajuste legado de Volumes vetado; view Power BI expurgada';
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

-- [VETADO] Codigo morto: view do Power BI expurgada da arquitetura na migration 035.
--          Include removido: views\012_criar_view_fretes_powerbi.sql

IF OBJECT_ID(N'dbo.schema_migrations', N'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM dbo.schema_migrations WHERE migration_id = N'022_corrigir_volumes_fretes_faturamento')
BEGIN
    INSERT INTO dbo.schema_migrations (migration_id, notes)
    VALUES (
        N'022_corrigir_volumes_fretes_faturamento',
        N'Ajuste legado de Volumes dependia da view Power BI; recriacao vetada pela migration 035.'
    );
END;
GO

PRINT 'Ajuste legado de Volumes ignorado; recriacao de view Power BI vetada.';
GO
