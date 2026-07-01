PRINT 'Migration 041: adicionar chave pick_item entre coletas e fretes';
GO

SET NOCOUNT ON;
SET XACT_ABORT ON;
GO

DECLARE @MigrationId NVARCHAR(255) = N'041_adicionar_chave_pick_item_coletas_fretes';

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
    PRINT 'Migracao 041_adicionar_chave_pick_item_coletas_fretes ja aplicada. Nenhuma acao necessaria.';
    RETURN;
END;

IF OBJECT_ID(N'dbo.fretes', N'U') IS NULL
    THROW 51080, 'Tabela dbo.fretes nao encontrada. Execute os scripts-base antes da migration 041.', 1;

IF OBJECT_ID(N'dbo.coletas', N'U') IS NULL
    THROW 51081, 'Tabela dbo.coletas nao encontrada. Execute os scripts-base antes da migration 041.', 1;

BEGIN TRY
BEGIN TRANSACTION;

IF COL_LENGTH(N'dbo.fretes', N'pick_item_id') IS NULL
BEGIN
    ALTER TABLE dbo.fretes
    ADD pick_item_id BIGINT NULL;
    PRINT 'Coluna dbo.fretes.pick_item_id criada.';
END
ELSE
BEGIN
    PRINT 'Coluna dbo.fretes.pick_item_id ja existe.';
END;

IF COL_LENGTH(N'dbo.coletas', N'pick_items_ids') IS NULL
BEGIN
    ALTER TABLE dbo.coletas
    ADD pick_items_ids NVARCHAR(MAX) NULL;
    PRINT 'Coluna dbo.coletas.pick_items_ids criada.';
END
ELSE
BEGIN
    PRINT 'Coluna dbo.coletas.pick_items_ids ja existe.';
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_fretes_pick_item_id'
      AND object_id = OBJECT_ID(N'dbo.fretes')
)
BEGIN
    CREATE NONCLUSTERED INDEX IX_fretes_pick_item_id
        ON dbo.fretes(pick_item_id)
        INCLUDE (id, valor_total, excluido_na_origem)
        WHERE pick_item_id IS NOT NULL;
    PRINT 'Indice IX_fretes_pick_item_id criado.';
END
ELSE
BEGIN
    PRINT 'Indice IX_fretes_pick_item_id ja existe.';
END;

INSERT INTO dbo.schema_migrations (migration_id, notes)
VALUES (
    @MigrationId,
    N'Adiciona dbo.fretes.pick_item_id e dbo.coletas.pick_items_ids para ligar Freight.pickItemId a Pick.pickItems[].id.'
);

COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0
        ROLLBACK TRANSACTION;
    THROW;
END CATCH;

PRINT 'Migration 041_adicionar_chave_pick_item_coletas_fretes concluida com sucesso.';
GO
