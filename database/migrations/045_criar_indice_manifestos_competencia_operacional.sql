PRINT 'Migration 045: criar indice de competencia operacional em manifestos';
GO

SET NOCOUNT ON;
SET XACT_ABORT ON;
GO

DECLARE @MigrationId NVARCHAR(255) = N'045_criar_indice_manifestos_competencia_operacional';

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
    PRINT 'Migracao 045_criar_indice_manifestos_competencia_operacional ja aplicada. Nenhuma acao necessaria.';
    RETURN;
END;

IF OBJECT_ID(N'dbo.manifestos', N'U') IS NULL
    THROW 51120, 'Tabela dbo.manifestos nao encontrada. Execute os scripts-base antes da migration 045.', 1;

IF COL_LENGTH(N'dbo.manifestos', N'departured_at') IS NULL
    THROW 51121, 'Coluna dbo.manifestos.departured_at nao encontrada.', 1;

IF COL_LENGTH(N'dbo.manifestos', N'created_at') IS NULL
    THROW 51122, 'Coluna dbo.manifestos.created_at nao encontrada.', 1;

IF COL_LENGTH(N'dbo.manifestos', N'excluido_na_origem') IS NULL
    THROW 51123, 'Coluna dbo.manifestos.excluido_na_origem nao encontrada. Execute a migration 027 antes da 045.', 1;

BEGIN TRY
BEGIN TRANSACTION;

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_manifestos_competencia_operacional'
      AND object_id = OBJECT_ID(N'dbo.manifestos')
)
BEGIN
    CREATE NONCLUSTERED INDEX IX_manifestos_competencia_operacional
        ON dbo.manifestos(departured_at, created_at)
        INCLUDE (
            sequence_code,
            identificador_unico,
            pick_sequence_code,
            vehicle_plate,
            excluido_na_origem,
            data_extracao
        )
        WITH (DATA_COMPRESSION = PAGE);

    PRINT 'Indice IX_manifestos_competencia_operacional criado.';
END
ELSE
BEGIN
    PRINT 'Indice IX_manifestos_competencia_operacional ja existe.';
END;

INSERT INTO dbo.schema_migrations (migration_id, notes)
VALUES (
    @MigrationId,
    N'Cria indice para carga incremental da fato de manifestos por competencia operacional: saida com fallback para criacao.'
);

COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0
        ROLLBACK TRANSACTION;
    THROW;
END CATCH;

PRINT 'Migration 045_criar_indice_manifestos_competencia_operacional concluida com sucesso.';
GO
