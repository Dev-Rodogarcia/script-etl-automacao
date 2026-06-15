-- ============================================
-- Script de criação da tabela 'dim_usuarios'
-- Tabela dimensão para armazenar informações de usuários (Individual)
-- Usada para enriquecer coletas e outras tabelas com nomes de usuários
-- Execute este script UMA VEZ antes de colocar o sistema em produção
-- ============================================

IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[dim_usuarios]') AND type in (N'U'))
BEGIN
    CREATE TABLE [dbo].[dim_usuarios](
        [user_id] [bigint] NOT NULL,
        [nome] [nvarchar](255) NULL,
        [data_atualizacao] [datetime] DEFAULT GETDATE(),
        [excluido_na_origem] [bit] NOT NULL CONSTRAINT DF_dim_usuarios_excluido_na_origem DEFAULT (0),
        [data_exclusao_origem] [datetime2](0) NULL,
        [hash_linha] [varbinary](32) NULL,
        PRIMARY KEY CLUSTERED ([user_id] ASC)
    );
    
    PRINT 'Tabela dim_usuarios criada com sucesso!';
END
ELSE
BEGIN
    PRINT 'Tabela dim_usuarios já existe. Pulando criação.';
END
GO

IF COL_LENGTH(N'dbo.dim_usuarios', N'excluido_na_origem') IS NULL
BEGIN
    ALTER TABLE dbo.dim_usuarios
    ADD excluido_na_origem BIT NOT NULL
        CONSTRAINT DF_dim_usuarios_excluido_na_origem DEFAULT (0) WITH VALUES;
    PRINT 'Coluna dim_usuarios.excluido_na_origem adicionada em tabela existente.';
END
GO

IF COL_LENGTH(N'dbo.dim_usuarios', N'data_exclusao_origem') IS NULL
BEGIN
    ALTER TABLE dbo.dim_usuarios
    ADD data_exclusao_origem DATETIME2(0) NULL;
    PRINT 'Coluna dim_usuarios.data_exclusao_origem adicionada em tabela existente.';
END
GO

IF COL_LENGTH(N'dbo.dim_usuarios', N'hash_linha') IS NULL
BEGIN
    ALTER TABLE dbo.dim_usuarios
    ADD hash_linha VARBINARY(32) NULL;
    PRINT 'Coluna dim_usuarios.hash_linha adicionada em tabela existente.';
END
GO
