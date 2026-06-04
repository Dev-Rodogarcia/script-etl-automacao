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

DROP VIEW IF EXISTS dbo.vw_faturas_por_cliente_powerbi;
PRINT 'View dbo.vw_faturas_por_cliente_powerbi removida quando existente.';

DROP VIEW IF EXISTS dbo.vw_fretes_powerbi;
PRINT 'View dbo.vw_fretes_powerbi removida quando existente.';

DROP VIEW IF EXISTS dbo.vw_coletas_powerbi;
PRINT 'View dbo.vw_coletas_powerbi removida quando existente.';

DROP VIEW IF EXISTS dbo.vw_cotacoes_powerbi;
PRINT 'View dbo.vw_cotacoes_powerbi removida quando existente.';

DROP VIEW IF EXISTS dbo.vw_contas_a_pagar_powerbi;
PRINT 'View dbo.vw_contas_a_pagar_powerbi removida quando existente.';

DROP VIEW IF EXISTS dbo.vw_localizacao_cargas_powerbi;
PRINT 'View dbo.vw_localizacao_cargas_powerbi removida quando existente.';

DROP VIEW IF EXISTS dbo.vw_manifestos_powerbi;
PRINT 'View dbo.vw_manifestos_powerbi removida quando existente.';

DROP VIEW IF EXISTS dbo.vw_inventario_powerbi;
PRINT 'View dbo.vw_inventario_powerbi removida quando existente.';

DROP VIEW IF EXISTS dbo.vw_sinistros_powerbi;
PRINT 'View dbo.vw_sinistros_powerbi removida quando existente.';

INSERT INTO dbo.schema_migrations (migration_id, notes)
VALUES (
    @MigrationId,
    N'Remove views legadas de Power BI substituidas pelo dashboard React.'
);

COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0
        ROLLBACK TRANSACTION;
    THROW;
END CATCH;

PRINT 'Migracao 035_drop_views_legadas_powerbi concluida com sucesso.';
GO
