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
        PRIMARY KEY CLUSTERED ([user_id] ASC)
    );
    
    PRINT 'Tabela dim_usuarios criada com sucesso!';
END
ELSE
BEGIN
    PRINT 'Tabela dim_usuarios já existe. Pulando criação.';
END
GO
