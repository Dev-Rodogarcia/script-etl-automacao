PRINT 'Migration 049: criar dimensao de regras de regiao logistica';
GO

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET XACT_ABORT ON;
GO

DECLARE @MigrationId NVARCHAR(255) = N'049_criar_dim_regiao_logistica_rules';

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
    PRINT 'Migracao 049_criar_dim_regiao_logistica_rules ja aplicada. Nenhuma acao necessaria.';
    RETURN;
END;

BEGIN TRY
BEGIN TRANSACTION;

IF OBJECT_ID(N'dbo.dim_regiao_logistica_rules', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.dim_regiao_logistica_rules (
        id INT IDENTITY(1,1) NOT NULL,
        cep_inicio VARCHAR(8) NULL,
        cep_fim VARCHAR(8) NULL,
        cidade NVARCHAR(255) NULL,
        uf VARCHAR(2) NULL,
        regiao_logistica NVARCHAR(100) NOT NULL,
        CONSTRAINT PK_dim_regiao_logistica_rules PRIMARY KEY (id),
        CONSTRAINT CK_dim_regiao_logistica_rules_regiao
            CHECK (LTRIM(RTRIM(regiao_logistica)) <> N''),
        CONSTRAINT CK_dim_regiao_logistica_rules_cep_par
            CHECK ((cep_inicio IS NULL AND cep_fim IS NULL) OR (cep_inicio IS NOT NULL AND cep_fim IS NOT NULL)),
        CONSTRAINT CK_dim_regiao_logistica_rules_cidade_uf_par
            CHECK ((cidade IS NULL AND uf IS NULL) OR (cidade IS NOT NULL AND uf IS NOT NULL))
    );

    PRINT 'Tabela dbo.dim_regiao_logistica_rules criada.';
END
ELSE
BEGIN
    PRINT 'Tabela dbo.dim_regiao_logistica_rules ja existe.';
END;

IF OBJECT_ID(N'dbo.dim_regiao_logistica_rules', N'U') IS NOT NULL
   AND NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = N'IX_dim_regiao_logistica_rules_cep_range'
          AND object_id = OBJECT_ID(N'dbo.dim_regiao_logistica_rules')
   )
BEGIN
    CREATE NONCLUSTERED INDEX IX_dim_regiao_logistica_rules_cep_range
        ON dbo.dim_regiao_logistica_rules(cep_inicio, cep_fim)
        INCLUDE (regiao_logistica)
        WHERE cep_inicio IS NOT NULL AND cep_fim IS NOT NULL;

    PRINT 'Indice IX_dim_regiao_logistica_rules_cep_range criado.';
END;

IF OBJECT_ID(N'dbo.dim_regiao_logistica_rules', N'U') IS NOT NULL
   AND NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = N'IX_dim_regiao_logistica_rules_cidade_uf'
          AND object_id = OBJECT_ID(N'dbo.dim_regiao_logistica_rules')
   )
BEGIN
    CREATE NONCLUSTERED INDEX IX_dim_regiao_logistica_rules_cidade_uf
        ON dbo.dim_regiao_logistica_rules(cidade, uf)
        INCLUDE (regiao_logistica)
        WHERE cidade IS NOT NULL AND uf IS NOT NULL;

    PRINT 'Indice IX_dim_regiao_logistica_rules_cidade_uf criado.';
END;

INSERT INTO dbo.schema_migrations (migration_id, notes)
VALUES (
    @MigrationId,
    N'Cria dimensao de regras para resolver regiao logistica de coletas por faixa de CEP ou Cidade/UF.'
);

COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0
        ROLLBACK TRANSACTION;
    THROW;
END CATCH;

PRINT 'Migration 049_criar_dim_regiao_logistica_rules concluida com sucesso.';
GO
