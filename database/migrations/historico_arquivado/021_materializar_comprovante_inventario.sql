PRINT 'Migration 021: materializar comprovante anexado no inventario';
GO

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;
GO

IF OBJECT_ID(N'dbo.inventario', N'U') IS NULL
BEGIN
    RAISERROR('Tabela dbo.inventario nao encontrada. Execute os scripts-base antes da migration 021.', 16, 1);
    RETURN;
END;
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

-- A view e publicada somente pelo artefato canonico
-- database/views/012_criar_view_fretes_powerbi.sql. Migrations historicas
-- nao podem sobrescrever o contrato analitico com uma definicao antiga.

IF OBJECT_ID(N'dbo.schema_migrations', N'U') IS NOT NULL
   AND NOT EXISTS (
       SELECT 1
       FROM dbo.schema_migrations
       WHERE migration_id = N'021_materializar_comprovante_inventario'
   )
BEGIN
    INSERT INTO dbo.schema_migrations (migration_id, notes)
    VALUES (
        N'021_materializar_comprovante_inventario',
        N'Materializa flag de comprovante no inventario; view publicada exclusivamente pelo script canonico.'
    );
END;
GO

PRINT 'Flag de comprovante materializada; view Power BI preservada pelo artefato canonico.';
GO
