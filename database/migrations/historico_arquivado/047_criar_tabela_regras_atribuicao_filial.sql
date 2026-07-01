PRINT 'Migration 047: criar tabela de regras de atribuicao de filial';
GO

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET XACT_ABORT ON;
GO

DECLARE @MigrationId NVARCHAR(255) = N'047_criar_tabela_regras_atribuicao_filial';

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
    PRINT 'Migracao 047_criar_tabela_regras_atribuicao_filial ja aplicada. Nenhuma acao necessaria.';
    RETURN;
END;

BEGIN TRY
BEGIN TRANSACTION;

IF OBJECT_ID(N'dbo.regras_atribuicao_filial', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.regras_atribuicao_filial (
        id INT IDENTITY(1,1) NOT NULL,
        pagador_documento_key NVARCHAR(50) NOT NULL,
        filial_destino_nome NVARCHAR(255) NOT NULL,
        filial_destino_key NVARCHAR(255) NOT NULL,
        ativo BIT NOT NULL CONSTRAINT DF_regras_atribuicao_filial_ativo DEFAULT (1),
        motivo NVARCHAR(255) NULL,
        CONSTRAINT PK_regras_atribuicao_filial PRIMARY KEY (id),
        CONSTRAINT CK_regras_atribuicao_filial_pagador_documento_key
            CHECK (LTRIM(RTRIM(pagador_documento_key)) <> N''),
        CONSTRAINT CK_regras_atribuicao_filial_destino_nome
            CHECK (LTRIM(RTRIM(filial_destino_nome)) <> N''),
        CONSTRAINT CK_regras_atribuicao_filial_destino_key
            CHECK (LTRIM(RTRIM(filial_destino_key)) <> N'')
    );

    PRINT 'Tabela dbo.regras_atribuicao_filial criada.';
END
ELSE
BEGIN
    PRINT 'Tabela dbo.regras_atribuicao_filial ja existe.';
END;

IF OBJECT_ID(N'dbo.regras_atribuicao_filial', N'U') IS NOT NULL
   AND NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = N'UX_regras_atribuicao_filial_pagador_ativo'
          AND object_id = OBJECT_ID(N'dbo.regras_atribuicao_filial')
   )
BEGIN
    CREATE UNIQUE NONCLUSTERED INDEX UX_regras_atribuicao_filial_pagador_ativo
        ON dbo.regras_atribuicao_filial(pagador_documento_key)
        WHERE ativo = 1;

    PRINT 'Indice UX_regras_atribuicao_filial_pagador_ativo criado.';
END;

INSERT INTO dbo.schema_migrations (migration_id, notes)
VALUES (
    @MigrationId,
    N'Cria tabela parametrica para regras comerciais de atribuicao de filial por documento do pagador.'
);

COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0
        ROLLBACK TRANSACTION;
    THROW;
END CATCH;

PRINT 'Migration 047_criar_tabela_regras_atribuicao_filial concluida com sucesso.';
GO
