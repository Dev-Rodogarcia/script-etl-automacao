-- ============================================
-- Script de criação da tabela 'contas_a_pagar'
-- Execute este script UMA VEZ antes de colocar o sistema em produção
-- ============================================

IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'dbo.contas_a_pagar') AND type in (N'U'))
BEGIN
    CREATE TABLE dbo.contas_a_pagar (
        sequence_code BIGINT PRIMARY KEY,
        document_number VARCHAR(100),
        issue_date DATE,
        tipo_lancamento NVARCHAR(100),
        valor_original DECIMAL(18,2),
        valor_juros DECIMAL(18,2),
        valor_desconto DECIMAL(18,2),
        valor_a_pagar DECIMAL(18,2),
        valor_pago DECIMAL(18,2),
        status_pagamento NVARCHAR(50),
        mes_competencia INT,
        ano_competencia INT,
        data_criacao DATETIMEOFFSET,
        data_liquidacao DATE,
        data_transacao DATE,
        nome_fornecedor NVARCHAR(255),
        nome_filial NVARCHAR(255),
        nome_centro_custo NVARCHAR(255),
        valor_centro_custo DECIMAL(18,2),
        classificacao_contabil NVARCHAR(100),
        descricao_contabil NVARCHAR(255),
        valor_contabil DECIMAL(18,2),
        area_lancamento NVARCHAR(255),
        observacoes NVARCHAR(MAX),
        descricao_despesa NVARCHAR(MAX),
        nome_usuario NVARCHAR(255),
        reconciliado BIT,
        metadata NVARCHAR(MAX),
        data_extracao DATETIME2 DEFAULT GETDATE()
    );
    
    PRINT 'Tabela contas_a_pagar criada com sucesso!';
END
ELSE
BEGIN
    PRINT 'Tabela contas_a_pagar já existe. Pulando criação.';
END
GO

-- Criar índices (idempotente - só cria se não existir)
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_fp_data_export_issue_date' AND object_id = OBJECT_ID('dbo.contas_a_pagar'))
BEGIN
    CREATE INDEX IX_fp_data_export_issue_date ON dbo.contas_a_pagar(issue_date);
    PRINT 'Índice IX_fp_data_export_issue_date criado com sucesso!';
END
ELSE
BEGIN
    PRINT 'Índice IX_fp_data_export_issue_date já existe. Pulando criação.';
END
GO

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_fp_data_export_status' AND object_id = OBJECT_ID('dbo.contas_a_pagar'))
BEGIN
    CREATE INDEX IX_fp_data_export_status ON dbo.contas_a_pagar(status_pagamento);
    PRINT 'Índice IX_fp_data_export_status criado com sucesso!';
END
ELSE
BEGIN
    PRINT 'Índice IX_fp_data_export_status já existe. Pulando criação.';
END
GO

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_fp_data_export_fornecedor' AND object_id = OBJECT_ID('dbo.contas_a_pagar'))
BEGIN
    CREATE INDEX IX_fp_data_export_fornecedor ON dbo.contas_a_pagar(nome_fornecedor);
    PRINT 'Índice IX_fp_data_export_fornecedor criado com sucesso!';
END
ELSE
BEGIN
    PRINT 'Índice IX_fp_data_export_fornecedor já existe. Pulando criação.';
END
GO

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_fp_data_export_filial' AND object_id = OBJECT_ID('dbo.contas_a_pagar'))
BEGIN
    CREATE INDEX IX_fp_data_export_filial ON dbo.contas_a_pagar(nome_filial);
    PRINT 'Índice IX_fp_data_export_filial criado com sucesso!';
END
ELSE
BEGIN
    PRINT 'Índice IX_fp_data_export_filial já existe. Pulando criação.';
END
GO

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_fp_data_export_competencia' AND object_id = OBJECT_ID('dbo.contas_a_pagar'))
BEGIN
    CREATE INDEX IX_fp_data_export_competencia ON dbo.contas_a_pagar(ano_competencia, mes_competencia);
    PRINT 'Índice IX_fp_data_export_competencia criado com sucesso!';
END
ELSE
BEGIN
    PRINT 'Índice IX_fp_data_export_competencia já existe. Pulando criação.';
END
GO
