-- ============================================
-- Script de criação da tabela 'log_extracoes'
-- Execute este script UMA VEZ antes de colocar o sistema em produção
-- ============================================

IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'dbo.log_extracoes') AND type in (N'U'))
BEGIN
    CREATE TABLE dbo.log_extracoes (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        entidade NVARCHAR(50) NOT NULL,
        timestamp_inicio DATETIME2 NOT NULL,
        timestamp_fim DATETIME2 NOT NULL,
        status_final NVARCHAR(20) NOT NULL,
        registros_extraidos INT NOT NULL,
        paginas_processadas INT NOT NULL,
        mensagem NVARCHAR(MAX)
    );
    
    PRINT 'Tabela log_extracoes criada com sucesso!';
END
ELSE
BEGIN
    PRINT 'Tabela log_extracoes já existe. Pulando criação.';
END
GO

-- Criar índices (idempotente - só cria se não existir)
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_entidade_timestamp' AND object_id = OBJECT_ID('dbo.log_extracoes'))
BEGIN
    CREATE INDEX idx_entidade_timestamp ON dbo.log_extracoes (entidade, timestamp_fim DESC);
    PRINT 'Índice idx_entidade_timestamp criado com sucesso!';
END
ELSE
BEGIN
    PRINT 'Índice idx_entidade_timestamp já existe. Pulando criação.';
END
GO
