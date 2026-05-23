PRINT 'Migration 018: indice de Coletas para dashboard por data de solicitacao';

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;
SET XACT_ABORT ON;

DECLARE @MigrationId NVARCHAR(255) = N'018_adicionar_indice_coletas_request_date_dashboard';

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
    PRINT 'Migracao 018_adicionar_indice_coletas_request_date_dashboard ja aplicada. Nenhuma acao necessaria.';
    RETURN;
END;

IF OBJECT_ID(N'dbo.coletas', N'U') IS NULL
BEGIN
    RAISERROR('Tabela dbo.coletas nao encontrada. Execute os scripts-base antes da migration 018.', 16, 1);
    RETURN;
END;

IF COL_LENGTH(N'dbo.coletas', N'request_date') IS NULL
BEGIN
    RAISERROR('Coluna dbo.coletas.request_date nao encontrada. O contrato de Coletas precisa publicar data nativa.', 16, 1);
    RETURN;
END;

BEGIN TRY
BEGIN TRANSACTION;

IF NOT EXISTS (
    SELECT 1
      FROM sys.indexes
     WHERE name = N'IX_coletas_request_date_dashboard'
       AND object_id = OBJECT_ID(N'dbo.coletas')
)
BEGIN
    CREATE NONCLUSTERED INDEX IX_coletas_request_date_dashboard
        ON dbo.coletas(request_date, status, pick_region, cidade_coleta)
        INCLUDE (id, sequence_code, filial_nome, cliente_nome, usuario_nome, data_extracao);
    PRINT 'IX_coletas_request_date_dashboard criado.';
END
ELSE
BEGIN
    PRINT 'IX_coletas_request_date_dashboard ja existe.';
END;

INSERT INTO dbo.schema_migrations (migration_id, notes)
VALUES (@MigrationId, N'Indice para dashboard de Coletas por request_date, status, regiao e cidade.');

COMMIT TRANSACTION;

PRINT 'Migration 018_adicionar_indice_coletas_request_date_dashboard concluida com sucesso.';
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0
    BEGIN
        ROLLBACK TRANSACTION;
    END;
    THROW;
END CATCH;
