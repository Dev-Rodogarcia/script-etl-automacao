PRINT 'Migration 037: adicionar status da fatura em faturas_por_cliente';
GO

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;
SET XACT_ABORT ON;

DECLARE @MigrationId NVARCHAR(255) = N'037_adicionar_status_fatura';

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

DECLARE @MigrationJaRegistrada BIT = CASE
    WHEN EXISTS (SELECT 1 FROM dbo.schema_migrations WHERE migration_id = @MigrationId) THEN 1
    ELSE 0
END;

IF @MigrationJaRegistrada = 1
BEGIN
    PRINT 'Migracao 037_adicionar_status_fatura ja registrada. Conferindo estado fisico da tabela.';
END;

IF OBJECT_ID(N'dbo.faturas_por_cliente', N'U') IS NULL
BEGIN
    PRINT 'Tabela dbo.faturas_por_cliente nao encontrada. Migracao nao registrada para permitir nova tentativa futura.';
    RETURN;
END;

BEGIN TRY
BEGIN TRANSACTION;

IF COL_LENGTH(N'dbo.faturas_por_cliente', N'status') IS NULL
BEGIN
    ALTER TABLE dbo.faturas_por_cliente ADD status NVARCHAR(50) NULL;
    PRINT 'Coluna dbo.faturas_por_cliente.status adicionada.';
END;
ELSE
BEGIN
    PRINT 'Coluna dbo.faturas_por_cliente.status ja existe.';
END;

IF COL_LENGTH(N'dbo.faturas_por_cliente', N'status') IS NOT NULL
   AND COL_LENGTH(N'dbo.faturas_por_cliente', N'metadata') IS NOT NULL
BEGIN
    EXEC sys.sp_executesql N'
        UPDATE dbo.faturas_por_cliente
        SET status = NULLIF(LTRIM(RTRIM(JSON_VALUE(
            CASE WHEN ISJSON(metadata) = 1 THEN metadata ELSE N''{}'' END,
            N''$.status''
        ))), N'''')
        WHERE status IS NULL
          AND metadata IS NOT NULL
          AND NULLIF(LTRIM(RTRIM(JSON_VALUE(
              CASE WHEN ISJSON(metadata) = 1 THEN metadata ELSE N''{}'' END,
              N''$.status''
          ))), N'''') IS NOT NULL;
    ';

    PRINT 'Backfill de dbo.faturas_por_cliente.status a partir do metadata concluido.';
END;

IF @MigrationJaRegistrada = 0
BEGIN
    INSERT INTO dbo.schema_migrations (migration_id, notes)
    VALUES (
        @MigrationId,
        N'Adiciona coluna status em faturas_por_cliente e popula registros existentes a partir de metadata.status.'
    );
END;

COMMIT TRANSACTION;
PRINT 'Migration 037_adicionar_status_fatura concluida com sucesso.';
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0
    BEGIN
        ROLLBACK TRANSACTION;
    END;
    THROW;
END CATCH;
GO
