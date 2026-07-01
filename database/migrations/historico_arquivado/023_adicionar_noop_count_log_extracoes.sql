-- Adiciona a metrica de no-op idempotente no log operacional de extracoes.

IF COL_LENGTH(N'dbo.log_extracoes', N'noop_count') IS NULL
BEGIN
    ALTER TABLE dbo.log_extracoes
        ADD noop_count INT NOT NULL
            CONSTRAINT DF_log_extracoes_noop_count DEFAULT (0) WITH VALUES;

    PRINT 'Coluna dbo.log_extracoes.noop_count adicionada com sucesso.';
END
ELSE
BEGIN
    PRINT 'Coluna dbo.log_extracoes.noop_count ja existe. Pulando alteracao.';
END
GO

IF OBJECT_ID(N'dbo.schema_migrations', N'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM dbo.schema_migrations WHERE migration_id = N'023_adicionar_noop_count_log_extracoes')
BEGIN
    INSERT INTO dbo.schema_migrations (migration_id, notes)
    VALUES (
        N'023_adicionar_noop_count_log_extracoes',
        N'Adiciona noop_count em dbo.log_extracoes para registrar merges idempotentes sem alteracao.'
    );
END;
GO
