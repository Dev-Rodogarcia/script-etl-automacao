PRINT 'Migration 028: corrigir chave unica de manifestos para chave logica estrita';
GO

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;
SET XACT_ABORT ON;
GO

DECLARE @MigrationId NVARCHAR(255) = N'028_corrigir_chave_unica_manifestos';

IF OBJECT_ID(N'dbo.schema_migrations', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.schema_migrations (
        migration_id NVARCHAR(255) NOT NULL,
        applied_at DATETIME2(0) NOT NULL CONSTRAINT DF_schema_migrations_applied_at DEFAULT SYSUTCDATETIME(),
        checksum_sha256 VARCHAR(64) NULL,
        notes NVARCHAR(500) NULL,
        CONSTRAINT PK_schema_migrations PRIMARY KEY (migration_id)
    );
END;

IF EXISTS (SELECT 1 FROM dbo.schema_migrations WHERE migration_id = @MigrationId)
BEGIN
    PRINT 'Migracao 028_corrigir_chave_unica_manifestos ja aplicada. Nenhuma acao necessaria.';
    RETURN;
END;

IF OBJECT_ID(N'dbo.manifestos', N'U') IS NULL
BEGIN
    PRINT 'Tabela dbo.manifestos nao encontrada. Nada a fazer.';
    INSERT INTO dbo.schema_migrations (migration_id, notes)
    VALUES (@MigrationId, N'Tabela dbo.manifestos ausente no momento da migracao; nenhuma alteracao aplicada.');
    RETURN;
END;

IF EXISTS (
    SELECT 1
    FROM dbo.manifestos
    GROUP BY
        sequence_code,
        ISNULL(pick_sequence_code, CONVERT(BIGINT, -1)),
        ISNULL(mdfe_number, -1)
    HAVING COUNT_BIG(*) > 1
)
BEGIN
    THROW 51028, 'Existem manifestos duplicados pela chave logica estrita. Execute a deduplicacao fisica antes da migration 028.', 1;
END;

BEGIN TRY
BEGIN TRANSACTION;

IF EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_manifestos_ativos_origem'
      AND object_id = OBJECT_ID(N'dbo.manifestos')
)
BEGIN
    DROP INDEX IX_manifestos_ativos_origem ON dbo.manifestos;
    PRINT 'IX_manifestos_ativos_origem removido temporariamente.';
END;

IF EXISTS (
    SELECT 1
    FROM sys.key_constraints
    WHERE name = N'UQ_manifestos_chave_composta'
      AND parent_object_id = OBJECT_ID(N'dbo.manifestos')
)
BEGIN
    ALTER TABLE dbo.manifestos DROP CONSTRAINT UQ_manifestos_chave_composta;
    PRINT 'UQ_manifestos_chave_composta removida temporariamente.';
END;

IF COL_LENGTH(N'dbo.manifestos', N'chave_merge_hash') IS NOT NULL
BEGIN
    ALTER TABLE dbo.manifestos DROP COLUMN chave_merge_hash;
    PRINT 'Coluna computada chave_merge_hash removida.';
END;

ALTER TABLE dbo.manifestos
ADD chave_merge_hash AS (
    CAST(sequence_code AS VARCHAR(20)) + '|' +
    ISNULL(CAST(pick_sequence_code AS VARCHAR(20)), '-1') + '|' +
    ISNULL(CAST(mdfe_number AS VARCHAR(20)), '-1')
) PERSISTED;
PRINT 'Coluna computada chave_merge_hash recriada com chave estrita.';

ALTER TABLE dbo.manifestos
    ADD CONSTRAINT UQ_manifestos_chave_composta UNIQUE (chave_merge_hash);
PRINT 'UQ_manifestos_chave_composta recriada.';

IF COL_LENGTH(N'dbo.manifestos', N'excluido_na_origem') IS NOT NULL
BEGIN
    CREATE NONCLUSTERED INDEX IX_manifestos_ativos_origem
    ON dbo.manifestos(chave_merge_hash)
    WHERE excluido_na_origem = 0;
    PRINT 'IX_manifestos_ativos_origem recriado.';
END;

INSERT INTO dbo.schema_migrations (migration_id, notes)
VALUES (
    @MigrationId,
    N'chave_merge_hash de manifestos passa a usar apenas sequence_code, pick_sequence_code e mdfe_number.'
);

COMMIT TRANSACTION;
PRINT 'Migration 028_corrigir_chave_unica_manifestos concluida com sucesso.';
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0
    BEGIN
        ROLLBACK TRANSACTION;
    END;
    THROW;
END CATCH;
GO
