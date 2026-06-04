PRINT 'Migration 034: adicionar hash_linha nas dimensoes de usuarios';
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

DECLARE @MigrationId NVARCHAR(255) = N'034_adicionar_hash_linha_usuarios';

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
    PRINT 'Migracao 034_adicionar_hash_linha_usuarios ja aplicada. Nenhuma acao necessaria.';
    RETURN;
END;

IF OBJECT_ID(N'dbo.dim_usuarios', N'U') IS NULL
    THROW 51058, 'Tabela dbo.dim_usuarios nao encontrada. Execute os scripts-base antes da migration 034.', 1;

IF OBJECT_ID(N'dbo.dim_usuarios_historico', N'U') IS NULL
    THROW 51059, 'Tabela dbo.dim_usuarios_historico nao encontrada. Execute os scripts-base antes da migration 034.', 1;

BEGIN TRY
BEGIN TRANSACTION;

IF COL_LENGTH(N'dbo.dim_usuarios', N'hash_linha') IS NULL
BEGIN
    ALTER TABLE dbo.dim_usuarios
        ADD hash_linha VARBINARY(32) NULL;
    PRINT 'Coluna dbo.dim_usuarios.hash_linha criada.';
END;

IF COL_LENGTH(N'dbo.dim_usuarios_historico', N'hash_linha') IS NULL
BEGIN
    ALTER TABLE dbo.dim_usuarios_historico
        ADD hash_linha VARBINARY(32) NULL;
    PRINT 'Coluna dbo.dim_usuarios_historico.hash_linha criada.';
END;

EXEC sys.sp_executesql N'
    UPDATE dbo.dim_usuarios
    SET hash_linha = HASHBYTES(''SHA2_256'', CONCAT_WS(N''|'',
        CONVERT(NVARCHAR(20), user_id),
        COALESCE(nome, N''<NULL>''),
        CONVERT(NVARCHAR(1), CONVERT(TINYINT, ativo)),
        COALESCE(CONVERT(NVARCHAR(33), origem_atualizado_em, 126), N''<NULL>''),
        CONVERT(NVARCHAR(1), CONVERT(TINYINT, excluido_na_origem))
    ))
    WHERE hash_linha IS NULL;';
PRINT 'Backfill de dbo.dim_usuarios.hash_linha concluido.';

EXEC sys.sp_executesql N'
    UPDATE dbo.dim_usuarios_historico
    SET hash_linha = HASHBYTES(''SHA2_256'', CONCAT_WS(N''|'',
        CONVERT(NVARCHAR(20), user_id),
        COALESCE(nome, N''<NULL>''),
        CONVERT(NVARCHAR(1), CONVERT(TINYINT, ativo)),
        COALESCE(CONVERT(NVARCHAR(33), origem_atualizado_em, 126), N''<NULL>'')
    ))
    WHERE hash_linha IS NULL;';
PRINT 'Backfill de dbo.dim_usuarios_historico.hash_linha concluido.';

INSERT INTO dbo.schema_migrations (migration_id, notes)
VALUES (
    @MigrationId,
    N'Adiciona hash_linha SHA2_256 em dim_usuarios e dim_usuarios_historico para idempotencia dimensional/SCD2.'
);

COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0
        ROLLBACK TRANSACTION;
    THROW;
END CATCH;

PRINT 'Migration 034_adicionar_hash_linha_usuarios concluida com sucesso.';
GO
