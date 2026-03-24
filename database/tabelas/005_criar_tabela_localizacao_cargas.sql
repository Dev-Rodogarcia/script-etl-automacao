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
        invoices_value NVARCHAR(50), -- Valor NF
        total_value DECIMAL(18, 2), -- Valor Frete
        service_type NVARCHAR(50), -- Tipo Serviço
        branch_nickname NVARCHAR(255), -- Filial Emissora
        predicted_delivery_at DATETIMEOFFSET, -- Previsão Entrega/Previsão de entrega
        destination_location_name NVARCHAR(255), -- Região Destino
        destination_branch_nickname NVARCHAR(255), -- Filial Destino
        classification NVARCHAR(255), -- Classificação
        status NVARCHAR(50), -- Status Carga
        status_branch_nickname NVARCHAR(255), -- Filial Atual (Nao encontrado no excel)...
        origin_location_name NVARCHAR(255), -- Região Origem
        origin_branch_nickname NVARCHAR(255), -- Filial Origem
        fit_fln_cln_nickname NVARCHAR(255), -- Localização Atual

        -- Coluna de Metadados para Resiliência e Completude
        metadata NVARCHAR(MAX), -- Dados brutos do JSON...

        -- Coluna de Auditoria
        data_extracao DATETIME2 DEFAULT GETDATE() -- Dados da extracao...
    );
    
    PRINT 'Tabela localizacao_cargas criada com sucesso!';
END
ELSE
BEGIN
    PRINT 'Tabela localizacao_cargas já existe. Pulando criação.';
END
GO
