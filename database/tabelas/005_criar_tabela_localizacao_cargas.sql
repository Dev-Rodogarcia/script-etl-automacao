-- ============================================
-- Script de criação da tabela 'localizacao_cargas'
-- Execute este script UMA VEZ antes de colocar o sistema em produção
-- ============================================

IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'dbo.localizacao_cargas') AND type in (N'U'))
BEGIN
    CREATE TABLE dbo.localizacao_cargas (
        -- Coluna de Chave Primária (Chave de Negócio)
        sequence_number BIGINT PRIMARY KEY, -- N° Minuta

        -- Colunas Essenciais para Indexação e Relatórios
        type NVARCHAR(100), -- Tipo
        service_at DATETIMEOFFSET, -- Data do frete
        invoices_volumes INT, -- Volumes
        taxed_weight NVARCHAR(50), -- Peso Taxado
        taxed_weight_decimal DECIMAL(18, 3), -- Peso Taxado materializado para BI
        invoices_value NVARCHAR(50), -- Valor NF
        invoices_value_decimal DECIMAL(18, 2), -- Valor NF materializado para BI
        total_value DECIMAL(18, 2), -- Valor Frete
        service_type NVARCHAR(50), -- Tipo Serviço
        branch_nickname NVARCHAR(255), -- Filial Emissora
        predicted_delivery_at DATETIMEOFFSET, -- Previsão Entrega/Previsão de entrega
        destination_location_name NVARCHAR(255), -- Região Destino
        destination_branch_nickname NVARCHAR(255), -- Filial Destino
        destination_branch_key AS NULLIF(LOWER(LTRIM(RTRIM(destination_branch_nickname))), N'') PERSISTED,
        classification NVARCHAR(255), -- Classificação
        status NVARCHAR(50), -- Status Carga
        status_normalized NVARCHAR(50), -- Status normalizado para regras de painel
        status_branch_nickname NVARCHAR(255), -- Filial Atual (Nao encontrado no excel)...
        origin_location_name NVARCHAR(255), -- Região Origem
        origin_branch_nickname NVARCHAR(255), -- Filial Origem
        fit_fln_cln_nickname NVARCHAR(255), -- Localização Atual

        -- Coluna de Metadados para Resiliência e Completude
        metadata NVARCHAR(MAX), -- Dados brutos do JSON...
        localizacao_hash CHAR(64), -- Hash operacional para UPSERT sem refresh destrutivo

        -- Coluna de Auditoria
        data_extracao DATETIME2 DEFAULT GETDATE(), -- Dados da extracao...
        excluido_na_origem BIT NOT NULL CONSTRAINT DF_localizacao_cargas_excluido_na_origem DEFAULT (0)
    );
    
    PRINT 'Tabela localizacao_cargas criada com sucesso!';
END
ELSE
BEGIN
    PRINT 'Tabela localizacao_cargas já existe. Pulando criação.';
END
GO

IF COL_LENGTH(N'dbo.localizacao_cargas', N'excluido_na_origem') IS NULL
BEGIN
    ALTER TABLE dbo.localizacao_cargas
    ADD excluido_na_origem BIT NOT NULL
        CONSTRAINT DF_localizacao_cargas_excluido_na_origem DEFAULT (0) WITH VALUES;
    PRINT 'Coluna localizacao_cargas.excluido_na_origem adicionada em tabela existente.';
END
GO
