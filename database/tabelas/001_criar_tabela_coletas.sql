-- ============================================
-- Script de criação da tabela 'coletas'
-- Execute este script UMA VEZ antes de colocar o sistema em produção
-- ============================================

IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'dbo.coletas') AND type in (N'U'))
BEGIN
    CREATE TABLE dbo.coletas (
        -- Coluna de Chave Primária (String, conforme API GraphQL)
        id NVARCHAR(50) PRIMARY KEY,

        -- Colunas Essenciais para Indexação e Relatórios
        sequence_code BIGINT,
        request_date DATE,
        service_date DATE,
        status NVARCHAR(50),
        total_value DECIMAL(18, 2),
        total_weight DECIMAL(18, 3),
        total_volumes INT,

        -- Campos Expandidos (22 campos do CSV)
        cliente_id BIGINT,
        cliente_nome NVARCHAR(255),
        cliente_doc NVARCHAR(50),
        local_coleta NVARCHAR(500),
        numero_coleta NVARCHAR(50),
        complemento_coleta NVARCHAR(255),
        cidade_coleta NVARCHAR(255),
        bairro_coleta NVARCHAR(255),
        uf_coleta NVARCHAR(10),
        cep_coleta NVARCHAR(20),
        filial_id BIGINT,
        filial_nome NVARCHAR(255),
        filial_cnpj NVARCHAR(50),
        usuario_id BIGINT,
        usuario_nome NVARCHAR(255),
        request_hour NVARCHAR(20),
        service_start_hour NVARCHAR(20),
        finish_date DATE,
        service_end_hour NVARCHAR(20),
        requester NVARCHAR(255),
        comments NVARCHAR(MAX),
        agent_id BIGINT,
        manifest_item_pick_id BIGINT,
        vehicle_type_id BIGINT,

        cancellation_reason NVARCHAR(MAX),
        cancellation_user_id BIGINT,
        cargo_classification_id BIGINT,
        cost_center_id BIGINT,
        destroy_reason NVARCHAR(MAX),
        destroy_user_id BIGINT,
        invoices_cubed_weight DECIMAL(18, 3),
        lunch_break_end_hour NVARCHAR(20),
        lunch_break_start_hour NVARCHAR(20),
        notification_email NVARCHAR(255),
        notification_phone NVARCHAR(255),
        pick_type_id BIGINT,
        pickup_location_id BIGINT,
        status_updated_at NVARCHAR(50),

        -- Coluna de Metadados para Resiliência e Completude
        metadata NVARCHAR(MAX),

        -- Coluna de Auditoria
        data_extracao DATETIME2 DEFAULT GETDATE(),
        
        -- Constraint para chave de negócio
        CONSTRAINT UQ_coletas_sequence_code UNIQUE (sequence_code)
    );
    
    PRINT 'Tabela coletas criada com sucesso!';
END
ELSE
BEGIN
    PRINT 'Tabela coletas já existe. Pulando criação.';
END
GO
