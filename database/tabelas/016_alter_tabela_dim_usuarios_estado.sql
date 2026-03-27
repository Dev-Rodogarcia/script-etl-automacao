-- ============================================
-- Evolucao da tabela 'dim_usuarios' para estado rastreavel
-- ============================================

IF EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'dbo.dim_usuarios') AND type in (N'U'))
BEGIN
    IF COL_LENGTH('dbo.dim_usuarios', 'ativo') IS NULL
    BEGIN
        ALTER TABLE dbo.dim_usuarios
            ADD ativo BIT NOT NULL
                CONSTRAINT DF_dim_usuarios_ativo DEFAULT (1) WITH VALUES;
        PRINT 'Coluna dim_usuarios.ativo adicionada com sucesso!';
    END
    ELSE
    BEGIN
        PRINT 'Coluna dim_usuarios.ativo ja existe. Pulando alteracao.';
    END

    IF COL_LENGTH('dbo.dim_usuarios', 'origem_atualizado_em') IS NULL
    BEGIN
        ALTER TABLE dbo.dim_usuarios
            ADD origem_atualizado_em DATETIME2 NULL;
        PRINT 'Coluna dim_usuarios.origem_atualizado_em adicionada com sucesso!';
    END
    ELSE
    BEGIN
        PRINT 'Coluna dim_usuarios.origem_atualizado_em ja existe. Pulando alteracao.';
    END

    IF COL_LENGTH('dbo.dim_usuarios', 'ultima_extracao_em') IS NULL
    BEGIN
        ALTER TABLE dbo.dim_usuarios
            ADD ultima_extracao_em DATETIME2 NULL;
        PRINT 'Coluna dim_usuarios.ultima_extracao_em adicionada com sucesso!';
    END
    ELSE
    BEGIN
        PRINT 'Coluna dim_usuarios.ultima_extracao_em ja existe. Pulando alteracao.';
    END

    EXEC('
        UPDATE dbo.dim_usuarios
        SET ativo = ISNULL(ativo, 1),
            ultima_extracao_em = COALESCE(ultima_extracao_em, data_atualizacao)
        WHERE ativo IS NULL
           OR ultima_extracao_em IS NULL;
    ');

    IF NOT EXISTS (
        SELECT * FROM sys.indexes
        WHERE name = 'IX_dim_usuarios_ativo'
          AND object_id = OBJECT_ID('dbo.dim_usuarios')
    )
    BEGIN
        EXEC('CREATE INDEX IX_dim_usuarios_ativo ON dbo.dim_usuarios (ativo, user_id);');
        PRINT 'Indice IX_dim_usuarios_ativo criado com sucesso!';
    END
    ELSE
    BEGIN
        PRINT 'Indice IX_dim_usuarios_ativo ja existe. Pulando criacao.';
    END
END
ELSE
BEGIN
    PRINT 'Tabela dim_usuarios nao encontrada. Execute 011_criar_tabela_dim_usuarios.sql antes deste script.';
END
GO
