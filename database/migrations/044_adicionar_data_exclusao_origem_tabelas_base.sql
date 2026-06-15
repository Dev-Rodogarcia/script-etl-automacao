PRINT 'Migration 044: adiciona data_exclusao_origem nas tabelas base com soft delete';
GO

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET XACT_ABORT ON;
GO

DECLARE @MigrationId NVARCHAR(255) = N'044_adicionar_data_exclusao_origem_tabelas_base';

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
    PRINT 'Migracao 044_adicionar_data_exclusao_origem_tabelas_base ja aplicada. Nenhuma acao necessaria.';
END;

DECLARE @TabelasSoftDelete TABLE (
    schema_name SYSNAME NOT NULL,
    table_name SYSNAME NOT NULL
);

INSERT INTO @TabelasSoftDelete (schema_name, table_name) VALUES
    (N'dbo', N'coletas'),
    (N'dbo', N'fretes'),
    (N'dbo', N'manifestos'),
    (N'dbo', N'cotacoes'),
    (N'dbo', N'localizacao_cargas'),
    (N'dbo', N'contas_a_pagar'),
    (N'dbo', N'faturas_por_cliente'),
    (N'dbo', N'inventario'),
    (N'dbo', N'sinistros'),
    (N'dbo', N'dim_usuarios'),
    (N'dbo', N'raster_viagens'),
    (N'dbo', N'raster_viagem_paradas');

IF EXISTS (
    SELECT 1
    FROM @TabelasSoftDelete t
    WHERE OBJECT_ID(t.schema_name + N'.' + t.table_name, N'U') IS NULL
)
BEGIN
    THROW 51100, 'Uma ou mais tabelas com soft delete nao foram encontradas. Execute as migrations base antes da 044.', 1;
END;

DECLARE @schemaName SYSNAME;
DECLARE @tableName SYSNAME;
DECLARE @objectName NVARCHAR(300);
DECLARE @qualifiedName NVARCHAR(300);
DECLARE @sql NVARCHAR(MAX);

DECLARE tabelas_cursor CURSOR LOCAL FAST_FORWARD FOR
SELECT schema_name, table_name
FROM @TabelasSoftDelete;

OPEN tabelas_cursor;
FETCH NEXT FROM tabelas_cursor INTO @schemaName, @tableName;

WHILE @@FETCH_STATUS = 0
BEGIN
    SET @objectName = @schemaName + N'.' + @tableName;
    SET @qualifiedName = QUOTENAME(@schemaName) + N'.' + QUOTENAME(@tableName);

    IF COL_LENGTH(@objectName, N'data_exclusao_origem') IS NULL
    BEGIN
        SET @sql = N'ALTER TABLE ' + @qualifiedName + N' ADD data_exclusao_origem DATETIME2(0) NULL;';
        EXEC sys.sp_executesql @sql;
        PRINT N'Coluna ' + @qualifiedName + N'.data_exclusao_origem adicionada.';
    END;

    FETCH NEXT FROM tabelas_cursor INTO @schemaName, @tableName;
END;

CLOSE tabelas_cursor;
DEALLOCATE tabelas_cursor;

IF NOT EXISTS (SELECT 1 FROM dbo.schema_migrations WHERE migration_id = @MigrationId)
BEGIN
    INSERT INTO dbo.schema_migrations (migration_id, notes)
    VALUES (
        @MigrationId,
        N'Adiciona data_exclusao_origem nas tabelas de dominio com soft delete para expurgo logico auditavel.'
    );
END;

PRINT 'Migration 044_adicionar_data_exclusao_origem_tabelas_base concluida com sucesso.';
GO
