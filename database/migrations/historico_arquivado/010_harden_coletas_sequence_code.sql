PRINT 'Migration 010: hardening de coletas.sequence_code';

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;
SET XACT_ABORT ON;

DECLARE @MigrationId NVARCHAR(255) = N'010_harden_coletas_sequence_code';

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
    PRINT 'Migracao 010_harden_coletas_sequence_code ja aplicada. Nenhuma acao necessaria.';
    RETURN;
END;

IF OBJECT_ID(N'dbo.coletas', N'U') IS NULL
BEGIN
    RAISERROR('Tabela dbo.coletas nao encontrada. Execute os scripts-base antes da migration 010.', 16, 1);
    RETURN;
END;

IF EXISTS (SELECT 1 FROM dbo.coletas WHERE sequence_code IS NULL)
BEGIN
    SELECT id, sequence_code
      FROM dbo.coletas
     WHERE sequence_code IS NULL
     ORDER BY id;

    RAISERROR('Migration 010 abortada: existem coletas com sequence_code NULL.', 16, 1);
    RETURN;
END;

IF EXISTS (
    SELECT sequence_code
      FROM dbo.coletas
     GROUP BY sequence_code
    HAVING COUNT(*) > 1
)
BEGIN
    SELECT sequence_code, COUNT(*) AS total
      FROM dbo.coletas
     GROUP BY sequence_code
    HAVING COUNT(*) > 1
     ORDER BY total DESC, sequence_code;

    RAISERROR('Migration 010 abortada: existem coletas com sequence_code duplicado.', 16, 1);
    RETURN;
END;

BEGIN TRY
BEGIN TRANSACTION;

IF EXISTS (
    SELECT 1
      FROM dbo.manifestos m
     WHERE m.pick_sequence_code IS NOT NULL
       AND NOT EXISTS (
            SELECT 1
              FROM dbo.coletas c
             WHERE c.sequence_code = m.pick_sequence_code
       )
)
BEGIN
    UPDATE m
       SET pick_sequence_code = NULL
      FROM dbo.manifestos m
     WHERE m.pick_sequence_code IS NOT NULL
       AND NOT EXISTS (
            SELECT 1
              FROM dbo.coletas c
             WHERE c.sequence_code = m.pick_sequence_code
       );

    PRINT 'Manifestos orfaos normalizados com pick_sequence_code = NULL antes do hardening.';
END;

IF EXISTS (
    SELECT 1
      FROM sys.foreign_keys
     WHERE name = 'FK_manifestos_pick_sequence_code_coletas'
       AND parent_object_id = OBJECT_ID(N'dbo.manifestos')
)
BEGIN
    ALTER TABLE dbo.manifestos DROP CONSTRAINT FK_manifestos_pick_sequence_code_coletas;
    PRINT 'FK_manifestos_pick_sequence_code_coletas removida temporariamente.';
END;

IF EXISTS (
    SELECT 1
      FROM sys.key_constraints
     WHERE name = 'UQ_coletas_sequence_code'
       AND parent_object_id = OBJECT_ID(N'dbo.coletas')
)
BEGIN
    ALTER TABLE dbo.coletas DROP CONSTRAINT UQ_coletas_sequence_code;
    PRINT 'UQ_coletas_sequence_code removida temporariamente.';
END;

IF EXISTS (
    SELECT 1
      FROM sys.indexes
     WHERE name = 'UQ_coletas_sequence_code'
       AND object_id = OBJECT_ID(N'dbo.coletas')
       AND is_unique_constraint = 0
)
BEGIN
    DROP INDEX UQ_coletas_sequence_code ON dbo.coletas;
    PRINT 'Indice legado UQ_coletas_sequence_code removido temporariamente.';
END;

IF EXISTS (
    SELECT 1
      FROM sys.indexes
     WHERE name = 'IX_coletas_data_extracao'
       AND object_id = OBJECT_ID(N'dbo.coletas')
)
BEGIN
    DROP INDEX IX_coletas_data_extracao ON dbo.coletas;
    PRINT 'IX_coletas_data_extracao removido temporariamente.';
END;

IF EXISTS (
    SELECT 1
      FROM sys.indexes
     WHERE name = 'IX_coletas_service_date'
       AND object_id = OBJECT_ID(N'dbo.coletas')
)
BEGIN
    DROP INDEX IX_coletas_service_date ON dbo.coletas;
    PRINT 'IX_coletas_service_date removido temporariamente.';
END;

ALTER TABLE dbo.coletas ALTER COLUMN sequence_code BIGINT NOT NULL;
PRINT 'Coluna dbo.coletas.sequence_code alterada para BIGINT NOT NULL.';

IF NOT EXISTS (
    SELECT 1
      FROM sys.key_constraints
     WHERE name = 'UQ_coletas_sequence_code'
       AND parent_object_id = OBJECT_ID(N'dbo.coletas')
)
BEGIN
    ALTER TABLE dbo.coletas
        ADD CONSTRAINT UQ_coletas_sequence_code UNIQUE (sequence_code);
    PRINT 'UQ_coletas_sequence_code recriada.';
END;

IF NOT EXISTS (
    SELECT 1
      FROM sys.indexes
     WHERE name = 'IX_coletas_data_extracao'
       AND object_id = OBJECT_ID(N'dbo.coletas')
)
BEGIN
    CREATE NONCLUSTERED INDEX IX_coletas_data_extracao
        ON dbo.coletas(data_extracao DESC)
        INCLUDE (id, sequence_code, status, cliente_nome);
    PRINT 'IX_coletas_data_extracao recriado.';
END;

IF NOT EXISTS (
    SELECT 1
      FROM sys.indexes
     WHERE name = 'IX_coletas_service_date'
       AND object_id = OBJECT_ID(N'dbo.coletas')
)
BEGIN
    CREATE NONCLUSTERED INDEX IX_coletas_service_date
        ON dbo.coletas(service_date DESC)
        INCLUDE (sequence_code, status, cliente_nome);
    PRINT 'IX_coletas_service_date recriado.';
END;

IF OBJECT_ID(N'dbo.manifestos', N'U') IS NOT NULL
   AND COL_LENGTH('dbo.manifestos', 'pick_sequence_code') IS NOT NULL
   AND NOT EXISTS (
        SELECT 1
          FROM sys.foreign_keys
         WHERE name = 'FK_manifestos_pick_sequence_code_coletas'
           AND parent_object_id = OBJECT_ID(N'dbo.manifestos')
   )
BEGIN
    ALTER TABLE dbo.manifestos WITH CHECK
        ADD CONSTRAINT FK_manifestos_pick_sequence_code_coletas
            FOREIGN KEY (pick_sequence_code)
            REFERENCES dbo.coletas(sequence_code);
    ALTER TABLE dbo.manifestos CHECK CONSTRAINT FK_manifestos_pick_sequence_code_coletas;
    PRINT 'FK_manifestos_pick_sequence_code_coletas recriada.';
END;

INSERT INTO dbo.schema_migrations (migration_id, notes)
VALUES (@MigrationId, N'Hardening de coletas.sequence_code para BIGINT NOT NULL com UNIQUE e FK preservada.');

COMMIT TRANSACTION;

PRINT 'Migration 010_harden_coletas_sequence_code concluida com sucesso.';
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0
    BEGIN
        ROLLBACK TRANSACTION;
    END;
    THROW;
END CATCH;
