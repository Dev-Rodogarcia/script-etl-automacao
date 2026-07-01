SET NOCOUNT ON;

DECLARE @MigrationId NVARCHAR(255) = N'035_drop_views_legadas_powerbi';

IF OBJECT_ID(N'dbo.schema_migrations', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.schema_migrations (
        migration_id    NVARCHAR(255) NOT NULL,
        applied_at      DATETIME2(0)  NOT NULL CONSTRAINT DF_schema_migrations_applied_at DEFAULT SYSUTCDATETIME(),
        checksum_sha256 VARCHAR(64)   NULL,
        notes           NVARCHAR(500) NULL,
        CONSTRAINT PK_schema_migrations PRIMARY KEY (migration_id)
    );
END;

IF EXISTS (SELECT 1 FROM dbo.schema_migrations WHERE migration_id = @MigrationId)
BEGIN
    PRINT 'Migracao 035_drop_views_legadas_powerbi ja aplicada. Nenhuma acao necessaria.';
    RETURN;
END;

BEGIN TRY
BEGIN TRANSACTION;

INSERT INTO dbo.schema_migrations (migration_id, notes)
VALUES (
    @MigrationId,
    N'Migration historica convertida em no-op: Shadow Deployment preserva views legadas Power BI em paralelo com a nova API.'
);

COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0
        ROLLBACK TRANSACTION;
    THROW;
END CATCH;

PRINT 'Migracao 035_drop_views_legadas_powerbi registrada como no-op; views legadas Power BI preservadas.';
GO
