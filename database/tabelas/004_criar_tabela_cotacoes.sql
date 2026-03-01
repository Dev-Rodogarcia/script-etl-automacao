-- ============================================
-- Script de criação da tabela 'cotacoes'
-- Execute este script UMA VEZ antes de colocar o sistema em produção
-- ============================================

IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'dbo.cotacoes') AND type in (N'U'))
BEGIN
    CREATE TABLE dbo.cotacoes (
        -- Coluna de Chave Primária (Chave de Negócio)
        sequence_code BIGINT PRIMARY KEY,

        -- Colunas Essenciais para Indexação e Relatórios
        requested_at DATETIMEOFFSET,
        operation_type NVARCHAR(100),
        customer_doc NVARCHAR(14),
        customer_name NVARCHAR(255),
        origin_city NVARCHAR(100),
        origin_state NVARCHAR(2),
        destination_city NVARCHAR(100),
        destination_state NVARCHAR(2),
        price_table NVARCHAR(255),
        volumes INT,
        taxed_weight DECIMAL(18, 3),
        invoices_value DECIMAL(18, 2),
        total_value DECIMAL(18, 2),
        user_name NVARCHAR(255),
        branch_nickname NVARCHAR(255),
        company_name NVARCHAR(255),
        requester_name NVARCHAR(255),
        real_weight NVARCHAR(50),
        origin_postal_code NVARCHAR(10),
        destination_postal_code NVARCHAR(10),
        customer_nickname NVARCHAR(255),
        sender_document NVARCHAR(14),
        sender_nickname NVARCHAR(255),
        receiver_document NVARCHAR(14),
        receiver_nickname NVARCHAR(255),
        disapprove_comments NVARCHAR(MAX),
        freight_comments NVARCHAR(MAX),
        discount_subtotal DECIMAL(18, 6),
        itr_subtotal DECIMAL(18, 6),
        tde_subtotal DECIMAL(18, 6),
        collect_subtotal DECIMAL(18, 6),
        delivery_subtotal DECIMAL(18, 6),
        other_fees DECIMAL(18, 6),
        cte_issued_at DATETIMEOFFSET,
        nfse_issued_at DATETIMEOFFSET,

        -- Coluna de Metadados para Resiliência e Completude
        metadata NVARCHAR(MAX),

        -- Coluna de Auditoria
        data_extracao DATETIME2 DEFAULT GETDATE()
    );
    
    PRINT 'Tabela cotacoes criada com sucesso!';
END
ELSE
BEGIN
    PRINT 'Tabela cotacoes já existe. Pulando criação.';
END
GO
