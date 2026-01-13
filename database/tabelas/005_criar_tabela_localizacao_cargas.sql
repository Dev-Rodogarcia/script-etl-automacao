-- ============================================
-- Script de criação da tabela 'localizacao_cargas'
-- Execute este script UMA VEZ antes de colocar o sistema em produção
-- ============================================

IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'dbo.localizacao_cargas') AND type in (N'U'))
BEGIN
    CREATE TABLE dbo.localizacao_cargas (
        -- Coluna de Chave Primária (Chave de Negócio)
        sequence_number BIGINT PRIMARY KEY,

        -- Colunas Essenciais para Indexação e Relatórios
        type NVARCHAR(100),
        service_at DATETIMEOFFSET,
        invoices_volumes INT,
        taxed_weight NVARCHAR(50),
        invoices_value NVARCHAR(50),
        total_value DECIMAL(18, 2),
        service_type NVARCHAR(50),
        branch_nickname NVARCHAR(255),
        predicted_delivery_at DATETIMEOFFSET,
        destination_location_name NVARCHAR(255),
        destination_branch_nickname NVARCHAR(255),
        classification NVARCHAR(255),
        status NVARCHAR(50),
        status_branch_nickname NVARCHAR(255),
        origin_location_name NVARCHAR(255),
        origin_branch_nickname NVARCHAR(255),

        -- Coluna de Metadados para Resiliência e Completude
        metadata NVARCHAR(MAX),

        -- Coluna de Auditoria
        data_extracao DATETIME2 DEFAULT GETDATE()
    );
    
    PRINT 'Tabela localizacao_cargas criada com sucesso!';
END
ELSE
BEGIN
    PRINT 'Tabela localizacao_cargas já existe. Pulando criação.';
END
GO
