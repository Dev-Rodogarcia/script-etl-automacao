-- ============================================================================
-- MIGRACAO: alinhar schema de sys_execution_history ao contrato do runtime
-- ============================================================================
-- Arquivo: 005_alinhar_sys_execution_history_schema.sql
-- Descricao: garante que dbo.sys_execution_history tenha DATETIME2 nas colunas
--            temporais, default em total_records, coluna created_at e indice
--            IX_sys_execution_history_start_time.
-- ============================================================================

SET NOCOUNT ON;

DECLARE @MigrationId NVARCHAR(255) = N'005_alinhar_sys_execution_history_schema';
DECLARE @NeedsStartTimeAlign BIT = 0;
DECLARE @NeedsEndTimeAlign BIT = 0;

IF OBJECT_ID(N'dbo.schema_migrations', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.schema_migrations (
        migration_id NVARCHAR(255) NOT NULL,
        applied_at DATETIME2(0) NOT NULL CONSTRAINT DF_schema_migrations_applied_at DEFAULT SYSUTCDATETIME(),
        checksum_sha256 VARCHAR(64) NULL,
        notes NVARCHAR(500) NULL,
        CONSTRAINT PK_schema_migrations PRIMARY KEY (migration_id)
    );
END

IF EXISTS (SELECT 1 FROM dbo.schema_migrations WHERE migration_id = @MigrationId)
BEGIN
    PRINT 'Migracao 005_alinhar_sys_execution_history_schema ja aplicada. Nenhuma acao necessaria.';
    RETURN;
END

PRINT 'Iniciando alinhamento da tabela dbo.sys_execution_history...';

IF OBJECT_ID(N'dbo.sys_execution_history', N'U') IS NULL
BEGIN
    PRINT 'Tabela dbo.sys_execution_history nao encontrada. O script-base 012 ja cria a estrutura alinhada.';
    GOTO RegistrarMigracao;
END

IF EXISTS (
    SELECT 1
    FROM sys.columns c
    JOIN sys.types t ON c.user_type_id = t.user_type_id
    WHERE c.object_id = OBJECT_ID(N'dbo.sys_execution_history')
      AND c.name = N'start_time'
      AND (t.name <> N'datetime2' OR c.is_nullable = 1)
)
BEGIN
    SET @NeedsStartTimeAlign = 1;
END

IF EXISTS (
    SELECT 1
    FROM sys.columns c
    JOIN sys.types t ON c.user_type_id = t.user_type_id
    WHERE c.object_id = OBJECT_ID(N'dbo.sys_execution_history')
      AND c.name = N'end_time'
      AND (t.name <> N'datetime2' OR c.is_nullable = 1)
)
BEGIN
    SET @NeedsEndTimeAlign = 1;
END

IF (@NeedsStartTimeAlign = 1 OR @NeedsEndTimeAlign = 1)
BEGIN
    IF EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = N'IX_sys_execution_history_start_time'
          AND object_id = OBJECT_ID(N'dbo.sys_execution_history')
    )
    BEGIN
        DROP INDEX IX_sys_execution_history_start_time ON dbo.sys_execution_history;
        PRINT 'Indice IX_sys_execution_history_start_time removido temporariamente para ajuste das colunas.';
    END
    ELSE
    BEGIN
        PRINT 'Indice IX_sys_execution_history_start_time nao existia. Pulando remocao.';
    END
END
ELSE
BEGIN
    PRINT 'Tipos de start_time/end_time ja estao alinhados. Nenhuma remocao de indice foi necessaria.';
END

IF @NeedsStartTimeAlign = 1
BEGIN
    ALTER TABLE dbo.sys_execution_history
        ALTER COLUMN start_time DATETIME2 NOT NULL;
    PRINT 'Coluna start_time alinhada para DATETIME2 NOT NULL.';
END
ELSE
BEGIN
    PRINT 'Coluna start_time ja estava alinhada.';
END

IF @NeedsEndTimeAlign = 1
BEGIN
    ALTER TABLE dbo.sys_execution_history
        ALTER COLUMN end_time DATETIME2 NOT NULL;
    PRINT 'Coluna end_time alinhada para DATETIME2 NOT NULL.';
END
ELSE
BEGIN
    PRINT 'Coluna end_time ja estava alinhada.';
END

IF COL_LENGTH(N'dbo.sys_execution_history', N'total_records') IS NOT NULL
BEGIN
    IF EXISTS (SELECT 1 FROM dbo.sys_execution_history WHERE total_records IS NULL)
    BEGIN
        UPDATE dbo.sys_execution_history
        SET total_records = 0
        WHERE total_records IS NULL;
        PRINT 'Valores nulos de total_records normalizados para 0.';
    END

    IF EXISTS (
        SELECT 1
        FROM sys.columns
        WHERE object_id = OBJECT_ID(N'dbo.sys_execution_history')
          AND name = N'total_records'
          AND is_nullable = 1
    )
    BEGIN
        ALTER TABLE dbo.sys_execution_history
            ALTER COLUMN total_records INT NOT NULL;
        PRINT 'Coluna total_records alinhada para INT NOT NULL.';
    END
    ELSE
    BEGIN
        PRINT 'Coluna total_records ja estava como NOT NULL.';
    END

    IF NOT EXISTS (
        SELECT 1
        FROM sys.default_constraints dc
        JOIN sys.columns c
          ON c.object_id = dc.parent_object_id
         AND c.column_id = dc.parent_column_id
        WHERE dc.parent_object_id = OBJECT_ID(N'dbo.sys_execution_history')
          AND c.name = N'total_records'
    )
    BEGIN
        ALTER TABLE dbo.sys_execution_history
            ADD CONSTRAINT DF_sys_execution_history_total_records DEFAULT 0 FOR total_records;
        PRINT 'Default de total_records adicionado.';
    END
    ELSE
    BEGIN
        PRINT 'Default de total_records ja existia.';
    END
END

IF COL_LENGTH(N'dbo.sys_execution_history', N'created_at') IS NULL
BEGIN
    ALTER TABLE dbo.sys_execution_history
        ADD created_at DATETIME2 NOT NULL
            CONSTRAINT DF_sys_execution_history_created_at DEFAULT SYSUTCDATETIME();
    PRINT 'Coluna created_at adicionada com default SYSUTCDATETIME().';
END
ELSE
BEGIN
    IF EXISTS (SELECT 1 FROM dbo.sys_execution_history WHERE created_at IS NULL)
    BEGIN
        UPDATE dbo.sys_execution_history
        SET created_at = SYSUTCDATETIME()
        WHERE created_at IS NULL;
        PRINT 'Valores nulos de created_at normalizados para SYSUTCDATETIME().';
    END

    IF EXISTS (
        SELECT 1
        FROM sys.columns c
        JOIN sys.types t ON c.user_type_id = t.user_type_id
        WHERE c.object_id = OBJECT_ID(N'dbo.sys_execution_history')
          AND c.name = N'created_at'
          AND (t.name <> N'datetime2' OR c.is_nullable = 1)
    )
    BEGIN
        ALTER TABLE dbo.sys_execution_history
            ALTER COLUMN created_at DATETIME2 NOT NULL;
        PRINT 'Coluna created_at alinhada para DATETIME2 NOT NULL.';
    END
    ELSE
    BEGIN
        PRINT 'Coluna created_at ja estava alinhada.';
    END

    IF NOT EXISTS (
        SELECT 1
        FROM sys.default_constraints dc
        JOIN sys.columns c
          ON c.object_id = dc.parent_object_id
         AND c.column_id = dc.parent_column_id
        WHERE dc.parent_object_id = OBJECT_ID(N'dbo.sys_execution_history')
          AND c.name = N'created_at'
    )
    BEGIN
        ALTER TABLE dbo.sys_execution_history
            ADD CONSTRAINT DF_sys_execution_history_created_at DEFAULT SYSUTCDATETIME() FOR created_at;
        PRINT 'Default de created_at adicionado.';
    END
    ELSE
    BEGIN
        PRINT 'Default de created_at ja existia.';
    END
END

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_sys_execution_history_start_time'
      AND object_id = OBJECT_ID(N'dbo.sys_execution_history')
)
BEGIN
    CREATE INDEX IX_sys_execution_history_start_time
        ON dbo.sys_execution_history (start_time DESC);
    PRINT 'Indice IX_sys_execution_history_start_time criado com sucesso.';
END
ELSE
BEGIN
    PRINT 'Indice IX_sys_execution_history_start_time ja existia.';
END

GOTO RegistrarMigracao;

RegistrarMigracao:

IF NOT EXISTS (SELECT 1 FROM dbo.schema_migrations WHERE migration_id = @MigrationId)
BEGIN
    INSERT INTO dbo.schema_migrations (migration_id, notes)
    VALUES (
        @MigrationId,
        N'Alinha dbo.sys_execution_history ao contrato atual do runtime: DATETIME2, defaults, created_at e indice por start_time.'
    );
END

PRINT 'Migracao 005 registrada com sucesso.';
GO
