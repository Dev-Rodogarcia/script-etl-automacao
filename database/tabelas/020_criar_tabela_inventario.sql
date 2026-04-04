IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'dbo.inventario') AND type in (N'U'))
BEGIN
    CREATE TABLE dbo.inventario (
        identificador_unico NVARCHAR(64) PRIMARY KEY,
        sequence_code BIGINT NOT NULL,
        numero_minuta BIGINT NULL,
        pagador_nome NVARCHAR(255) NULL,
        remetente_nome NVARCHAR(255) NULL,
        origem_cidade NVARCHAR(255) NULL,
        destinatario_nome NVARCHAR(255) NULL,
        destino_cidade NVARCHAR(255) NULL,
        regiao_entrega NVARCHAR(255) NULL,
        filial_entregadora NVARCHAR(255) NULL,
        branch_nickname NVARCHAR(255) NULL,
        type NVARCHAR(100) NULL,
        started_at DATETIMEOFFSET NULL,
        finished_at DATETIMEOFFSET NULL,
        status NVARCHAR(100) NULL,
        conferente_nome NVARCHAR(255) NULL,
        invoices_mapping NVARCHAR(MAX) NULL,
        invoices_value DECIMAL(18, 2) NULL,
        real_weight DECIMAL(18, 3) NULL,
        total_cubic_volume DECIMAL(18, 3) NULL,
        taxed_weight DECIMAL(18, 3) NULL,
        invoices_volumes INT NULL,
        read_volumes INT NULL,
        predicted_delivery_at DATETIMEOFFSET NULL,
        performance_finished_at DATETIMEOFFSET NULL,
        ultima_ocorrencia_at DATETIMEOFFSET NULL,
        ultima_ocorrencia_descricao NVARCHAR(500) NULL,
        metadata NVARCHAR(MAX) NULL,
        data_extracao DATETIME2 DEFAULT GETDATE()
    );

    CREATE INDEX IX_inventario_sequence_code ON dbo.inventario (sequence_code);
    CREATE INDEX IX_inventario_numero_minuta ON dbo.inventario (numero_minuta);
END
GO
