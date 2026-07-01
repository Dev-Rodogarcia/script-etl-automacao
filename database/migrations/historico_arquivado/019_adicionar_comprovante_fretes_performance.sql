PRINT 'Migration 019: materializar comprovante no inventario; view Power BI vetada';
GO

IF COL_LENGTH(N'dbo.inventario', N'flag_comprovante_anexado') IS NULL
BEGIN
    ALTER TABLE dbo.inventario
        ADD flag_comprovante_anexado BIT NOT NULL
            CONSTRAINT DF_inventario_flag_comprovante_anexado DEFAULT (0) WITH VALUES;
END;
GO

UPDATE dbo.inventario
   SET flag_comprovante_anexado = CASE
        WHEN ultima_ocorrencia_descricao COLLATE Latin1_General_CI_AI LIKE N'%Comprovante%Entrega%Anexado%' THEN 1
        ELSE 0
   END
 WHERE flag_comprovante_anexado <> CASE
        WHEN ultima_ocorrencia_descricao COLLATE Latin1_General_CI_AI LIKE N'%Comprovante%Entrega%Anexado%' THEN 1
        ELSE 0
   END;
GO

IF EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_inventario_comprovante_minuta'
      AND object_id = OBJECT_ID(N'dbo.inventario')
      AND (filter_definition IS NULL OR filter_definition NOT LIKE N'%flag_comprovante_anexado%')
)
BEGIN
    DROP INDEX IX_inventario_comprovante_minuta ON dbo.inventario;
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_inventario_comprovante_minuta'
      AND object_id = OBJECT_ID(N'dbo.inventario')
)
BEGIN
    CREATE NONCLUSTERED INDEX IX_inventario_comprovante_minuta
        ON dbo.inventario(numero_minuta)
        WHERE flag_comprovante_anexado = 1;
END;
GO

-- [VETADO] Codigo morto: view do Power BI expurgada da arquitetura na migration 035.
--          Recriacao de dbo.vw_fretes_powerbi removida desta migration historica.
GO

IF OBJECT_ID(N'dbo.schema_migrations', N'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM dbo.schema_migrations WHERE migration_id = N'019_adicionar_comprovante_fretes_performance')
BEGIN
    INSERT INTO dbo.schema_migrations (migration_id, notes)
    VALUES (
        N'019_adicionar_comprovante_fretes_performance',
        N'Materializa flag de comprovante no inventario. Recriacao da view Power BI vetada pela migration 035.'
    );
END;
GO

PRINT 'Flag de comprovante materializada; recriacao de view Power BI vetada.';
GO
