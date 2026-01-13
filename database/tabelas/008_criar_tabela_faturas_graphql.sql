-- ============================================
-- Script de criação da tabela 'faturas_graphql'
-- Execute este script UMA VEZ antes de colocar o sistema em produção
-- ============================================

IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'dbo.faturas_graphql') AND type in (N'U'))
BEGIN
    CREATE TABLE dbo.faturas_graphql (
        id BIGINT PRIMARY KEY,
        document NVARCHAR(50),
        issue_date DATE,
        due_date DATE,
        original_due_date DATE,
        value DECIMAL(18,2),
        paid_value DECIMAL(18,2),
        value_to_pay DECIMAL(18,2),
        discount_value DECIMAL(18,2),
        interest_value DECIMAL(18,2),
        paid BIT,
        status NVARCHAR(50),
        type NVARCHAR(50),
        comments NVARCHAR(MAX),
        sequence_code INT,
        competence_month INT,
        competence_year INT,
        created_at DATETIME2,
        updated_at DATETIME2,
        corporation_id BIGINT,
        corporation_name NVARCHAR(255),
        corporation_cnpj NVARCHAR(50),
        metadata NVARCHAR(MAX),
        data_extracao DATETIME2 DEFAULT GETDATE()
    );
    
    PRINT 'Tabela faturas_graphql criada com sucesso!';
END
ELSE
BEGIN
    PRINT 'Tabela faturas_graphql já existe. Pulando criação.';
END
GO

-- Criar índices (idempotente - só cria se não existir)
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_fg_document' AND object_id = OBJECT_ID('dbo.faturas_graphql'))
BEGIN
    CREATE INDEX IX_fg_document ON dbo.faturas_graphql(document);
    PRINT 'Índice IX_fg_document criado com sucesso!';
END
ELSE
BEGIN
    PRINT 'Índice IX_fg_document já existe. Pulando criação.';
END
GO

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_fg_due_date' AND object_id = OBJECT_ID('dbo.faturas_graphql'))
BEGIN
    CREATE INDEX IX_fg_due_date ON dbo.faturas_graphql(due_date);
    PRINT 'Índice IX_fg_due_date criado com sucesso!';
END
ELSE
BEGIN
    PRINT 'Índice IX_fg_due_date já existe. Pulando criação.';
END
GO

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_fg_corporation_id' AND object_id = OBJECT_ID('dbo.faturas_graphql'))
BEGIN
    CREATE INDEX IX_fg_corporation_id ON dbo.faturas_graphql(corporation_id);
    PRINT 'Índice IX_fg_corporation_id criado com sucesso!';
END
ELSE
BEGIN
    PRINT 'Índice IX_fg_corporation_id já existe. Pulando criação.';
END
GO
