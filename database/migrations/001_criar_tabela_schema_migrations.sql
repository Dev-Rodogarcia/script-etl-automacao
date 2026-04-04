-- ============================================================================
-- MIGRACAO 001: criar tabela de controle de migracoes aplicadas
-- ============================================================================

SET NOCOUNT ON;

DECLARE @MigrationId NVARCHAR(255) = N'001_criar_tabela_schema_migrations';

IF OBJECT_ID(N'dbo.schema_migrations', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.schema_migrations (
        migration_id NVARCHAR(255) NOT NULL,
        applied_at DATETIME2(0) NOT NULL CONSTRAINT DF_schema_migrations_applied_at DEFAULT SYSUTCDATETIME(),
        checksum_sha256 VARCHAR(64) NULL,
        notes NVARCHAR(500) NULL,
        CONSTRAINT PK_schema_migrations PRIMARY KEY (migration_id)
    );
    PRINT 'Tabela dbo.schema_migrations criada com sucesso.';
END
ELSE
BEGIN
    PRINT 'Tabela dbo.schema_migrations ja existe. Pulando criacao.';
END

IF NOT EXISTS (SELECT 1 FROM dbo.schema_migrations WHERE migration_id = @MigrationId)
BEGIN
    INSERT INTO dbo.schema_migrations (migration_id, notes)
    VALUES (@MigrationId, N'Bootstrap da tabela de controle de migrations.');
    PRINT 'Migracao 001 registrada com sucesso.';
END
ELSE
BEGIN
    PRINT 'Migracao 001_criar_tabela_schema_migrations ja registrada. Pulando.';
END
GO
