PRINT 'Migration 016: materializar campos de faturamento em fretes';
GO

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;
SET XACT_ABORT ON;

DECLARE @MigrationId NVARCHAR(255) = N'016_materializar_faturamento_fretes';

IF OBJECT_ID(N'dbo.schema_migrations', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.schema_migrations (
        migration_id NVARCHAR(255) NOT NULL,
        applied_at DATETIME2(0) NOT NULL CONSTRAINT DF_schema_migrations_applied_at DEFAULT SYSUTCDATETIME(),
        notes NVARCHAR(MAX) NULL,
        CONSTRAINT PK_schema_migrations PRIMARY KEY (migration_id)
    );
END;

IF EXISTS (SELECT 1 FROM dbo.schema_migrations WHERE migration_id = @MigrationId)
BEGIN
    PRINT 'Migracao 016_materializar_faturamento_fretes ja aplicada. Nenhuma acao necessaria.';
    RETURN;
END;

IF OBJECT_ID(N'dbo.fretes', N'U') IS NULL
BEGIN
    PRINT 'Tabela dbo.fretes nao encontrada. Nada a fazer.';
    INSERT INTO dbo.schema_migrations (migration_id, notes)
    VALUES (@MigrationId, N'Tabela dbo.fretes ausente no momento da migracao; nenhuma alteracao aplicada.');
    RETURN;
END;

BEGIN TRY
BEGIN TRANSACTION;

IF COL_LENGTH(N'dbo.fretes', N'data_referencia_faturamento') IS NULL
BEGIN
    ALTER TABLE dbo.fretes ADD data_referencia_faturamento DATETIMEOFFSET NULL;
    PRINT 'Coluna dbo.fretes.data_referencia_faturamento adicionada.';
END;
ELSE
BEGIN
    PRINT 'Coluna dbo.fretes.data_referencia_faturamento ja existe.';
END;

IF COL_LENGTH(N'dbo.fretes', N'is_elegivel_faturamento') IS NULL
BEGIN
    ALTER TABLE dbo.fretes ADD is_elegivel_faturamento BIT NULL;
    PRINT 'Coluna dbo.fretes.is_elegivel_faturamento adicionada.';
END;
ELSE
BEGIN
    PRINT 'Coluna dbo.fretes.is_elegivel_faturamento ja existe.';
END;

DECLARE @RowsBackfill INT = 0;

EXEC sp_executesql N'
UPDATE f
   SET data_referencia_faturamento = calculado.data_referencia_faturamento,
       is_elegivel_faturamento = calculado.is_elegivel_faturamento
FROM dbo.fretes AS f
CROSS APPLY (
    SELECT
        COALESCE(f.cte_issued_at, f.servico_em) AS data_referencia_faturamento,
        CAST(
            CASE
                WHEN f.cortesia = 1 THEN 0
                WHEN CONVERT(NVARCHAR(MAX), ISNULL(f.classificacao_nome, N'''')) COLLATE Latin1_General_CI_AI LIKE N''%bloqueio%''
                 AND (
                    CONVERT(NVARCHAR(MAX), ISNULL(f.classificacao_nome, N'''')) COLLATE Latin1_General_CI_AI LIKE N''%anulacao%''
                    OR CONVERT(NVARCHAR(MAX), ISNULL(f.classificacao_nome, N'''')) COLLATE Latin1_General_CI_AI LIKE N''%isolamento%''
                 )
                THEN 0
                ELSE 1
            END AS bit
        ) AS is_elegivel_faturamento
) AS calculado
WHERE (f.data_referencia_faturamento IS NULL AND calculado.data_referencia_faturamento IS NOT NULL)
   OR (f.data_referencia_faturamento IS NOT NULL AND calculado.data_referencia_faturamento IS NULL)
   OR f.data_referencia_faturamento <> calculado.data_referencia_faturamento
   OR COALESCE(CAST(f.is_elegivel_faturamento AS INT), -1) <> CAST(calculado.is_elegivel_faturamento AS INT);

SET @RowsBackfill = @@ROWCOUNT;
',
N'@RowsBackfill INT OUTPUT',
@RowsBackfill = @RowsBackfill OUTPUT;

PRINT CONCAT('Fretes recalculados para faturamento: ', @RowsBackfill);

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_fretes_faturamento_data_elegivel'
      AND object_id = OBJECT_ID(N'dbo.fretes')
)
BEGIN
    EXEC sp_executesql N'
    CREATE NONCLUSTERED INDEX IX_fretes_faturamento_data_elegivel
    ON dbo.fretes(data_referencia_faturamento DESC, is_elegivel_faturamento)
    INCLUDE (id, valor_total, subtotal, status, filial_nome, pagador_nome, classificacao_nome);
    ';

    PRINT 'Indice IX_fretes_faturamento_data_elegivel criado.';
END;
ELSE
BEGIN
    PRINT 'Indice IX_fretes_faturamento_data_elegivel ja existe.';
END;

INSERT INTO dbo.schema_migrations (migration_id, notes)
VALUES (
    @MigrationId,
    N'Materializa data de referencia e elegibilidade de faturamento em dbo.fretes para consumo analitico sem calculo de string em tempo de dashboard.'
);

COMMIT TRANSACTION;
PRINT 'Migration 016_materializar_faturamento_fretes concluida com sucesso.';
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0
    BEGIN
        ROLLBACK TRANSACTION;
    END;
    THROW;
END CATCH;
GO
