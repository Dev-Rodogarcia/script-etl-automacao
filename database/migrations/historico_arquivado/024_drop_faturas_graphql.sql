SET NOCOUNT ON;

DECLARE @MigrationId NVARCHAR(255) = N'024_drop_faturas_graphql';

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
    PRINT 'Migracao 024_drop_faturas_graphql ja aplicada. Nenhuma acao necessaria.';
    RETURN;
END;

IF OBJECT_ID(N'dbo.vw_faturas_graphql_powerbi', N'V') IS NOT NULL
BEGIN
    DROP VIEW dbo.vw_faturas_graphql_powerbi;
    PRINT 'View dbo.vw_faturas_graphql_powerbi removida.';
END;

DROP TABLE IF EXISTS dbo.faturas_graphql;
PRINT 'Tabela dbo.faturas_graphql removida quando existente.';

INSERT INTO dbo.schema_migrations (migration_id, notes)
VALUES (@MigrationId, N'Remocao definitiva da entidade ociosa faturas_graphql.');

PRINT 'Migracao 024 registrada com sucesso.';
GO
