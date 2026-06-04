-- ============================================
-- Script de criacao da tabela 'dim_usuarios_historico'
-- Historico simples de alteracoes do snapshot de usuarios
-- ============================================

IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'dbo.dim_usuarios_historico') AND type in (N'U'))
BEGIN
    CREATE TABLE dbo.dim_usuarios_historico (
        id BIGINT IDENTITY(1,1) NOT NULL,
        execution_uuid NVARCHAR(36) NULL,
        user_id BIGINT NOT NULL,
        nome NVARCHAR(255) NULL,
        ativo BIT NOT NULL,
        origem_atualizado_em DATETIME2 NULL,
        hash_linha VARBINARY(32) NULL,
        observado_em DATETIME2 NOT NULL CONSTRAINT DF_dim_usuarios_historico_observado_em DEFAULT SYSDATETIME(),
        tipo_alteracao NVARCHAR(30) NOT NULL,
        CONSTRAINT PK_dim_usuarios_historico PRIMARY KEY CLUSTERED (id)
    );

    PRINT 'Tabela dim_usuarios_historico criada com sucesso!';
END
ELSE
BEGIN
    PRINT 'Tabela dim_usuarios_historico ja existe. Pulando criacao.';
END
GO

IF COL_LENGTH(N'dbo.dim_usuarios_historico', N'hash_linha') IS NULL
BEGIN
    ALTER TABLE dbo.dim_usuarios_historico
    ADD hash_linha VARBINARY(32) NULL;
    PRINT 'Coluna dim_usuarios_historico.hash_linha adicionada em tabela existente.';
END
GO

EXEC('
    UPDATE dbo.dim_usuarios_historico
    SET hash_linha = HASHBYTES(''SHA2_256'', CONCAT_WS(N''|'',
        CONVERT(NVARCHAR(20), user_id),
        COALESCE(nome, N''<NULL>''),
        CONVERT(NVARCHAR(1), CONVERT(TINYINT, ativo)),
        COALESCE(CONVERT(NVARCHAR(33), origem_atualizado_em, 126), N''<NULL>'')
    ))
    WHERE hash_linha IS NULL;
');
GO

IF NOT EXISTS (
    SELECT * FROM sys.indexes
    WHERE name = 'IX_dim_usuarios_historico_user_observado'
      AND object_id = OBJECT_ID('dbo.dim_usuarios_historico')
)
BEGIN
    CREATE INDEX IX_dim_usuarios_historico_user_observado
        ON dbo.dim_usuarios_historico (user_id, observado_em DESC);
    PRINT 'Indice IX_dim_usuarios_historico_user_observado criado com sucesso!';
END
ELSE
BEGIN
    PRINT 'Indice IX_dim_usuarios_historico_user_observado ja existe. Pulando criacao.';
END
GO

IF NOT EXISTS (
    SELECT * FROM sys.indexes
    WHERE name = 'IX_dim_usuarios_historico_execution'
      AND object_id = OBJECT_ID('dbo.dim_usuarios_historico')
)
BEGIN
    CREATE INDEX IX_dim_usuarios_historico_execution
        ON dbo.dim_usuarios_historico (execution_uuid, observado_em DESC);
    PRINT 'Indice IX_dim_usuarios_historico_execution criado com sucesso!';
END
ELSE
BEGIN
    PRINT 'Indice IX_dim_usuarios_historico_execution ja existe. Pulando criacao.';
END
GO
