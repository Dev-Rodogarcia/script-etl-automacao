PRINT 'Migration 020: preparar CNPJs de frota propria; view Power BI vetada';
GO

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;
SET XACT_ABORT ON;
GO

IF OBJECT_ID(N'dbo.schema_migrations', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.schema_migrations (
        migration_id NVARCHAR(255) NOT NULL,
        applied_at DATETIME2(0) NOT NULL CONSTRAINT DF_schema_migrations_applied_at DEFAULT SYSUTCDATETIME(),
        notes NVARCHAR(MAX) NULL,
        CONSTRAINT PK_schema_migrations PRIMARY KEY (migration_id)
    );
END;
GO

IF OBJECT_ID(N'dbo.manifestos', N'U') IS NULL
BEGIN
    RAISERROR('Tabela dbo.manifestos nao encontrada. Execute os scripts-base antes da migration 020.', 16, 1);
    RETURN;
END;
GO

IF OBJECT_ID(N'dbo.manifestos_frota_propria_cnpjs', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.manifestos_frota_propria_cnpjs (
        cnpj NVARCHAR(14) NOT NULL,
        descricao NVARCHAR(255) NULL,
        ativo BIT NOT NULL CONSTRAINT DF_manifestos_frota_propria_cnpjs_ativo DEFAULT 1,
        criado_em DATETIME2(0) NOT NULL CONSTRAINT DF_manifestos_frota_propria_cnpjs_criado_em DEFAULT SYSUTCDATETIME(),
        atualizado_em DATETIME2(0) NOT NULL CONSTRAINT DF_manifestos_frota_propria_cnpjs_atualizado_em DEFAULT SYSUTCDATETIME(),
        CONSTRAINT PK_manifestos_frota_propria_cnpjs PRIMARY KEY (cnpj)
    );
    PRINT 'Tabela dbo.manifestos_frota_propria_cnpjs criada.';
END
ELSE
BEGIN
    PRINT 'Tabela dbo.manifestos_frota_propria_cnpjs ja existe.';
END;
GO

-- [VETADO] Codigo morto: view do Power BI expurgada da arquitetura na migration 035.
--          Recriacao de dbo.vw_manifestos_powerbi removida desta migration historica.
GO

IF NOT EXISTS (SELECT 1 FROM dbo.schema_migrations WHERE migration_id = N'020_adicionar_tipo_motorista_manifestos')
BEGIN
    INSERT INTO dbo.schema_migrations (migration_id, notes)
    VALUES (
        N'020_adicionar_tipo_motorista_manifestos',
        N'Cria cadastro configuravel de CNPJs de frota propria. Recriacao da view Power BI vetada pela migration 035.'
    );
END;
GO

PRINT 'Migration 020_adicionar_tipo_motorista_manifestos concluida com sucesso.';
GO
