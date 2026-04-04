-- ============================================
-- Script de criacao da tabela 'etl_invalid_records'
-- ============================================

IF OBJECT_ID(N'dbo.etl_invalid_records', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.etl_invalid_records (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        created_at DATETIME2 NOT NULL CONSTRAINT DF_etl_invalid_records_created_at DEFAULT SYSDATETIME(),
        entidade NVARCHAR(80) NOT NULL,
        reason_code NVARCHAR(50) NOT NULL,
        detalhe NVARCHAR(500) NULL,
        chave_referencia NVARCHAR(150) NULL,
        payload_json NVARCHAR(MAX) NULL
    );
    PRINT 'Tabela etl_invalid_records criada com sucesso!';
END
ELSE
BEGIN
    PRINT 'Tabela etl_invalid_records ja existe. Pulando criacao.';
END
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_etl_invalid_records_entidade_data'
      AND object_id = OBJECT_ID(N'dbo.etl_invalid_records')
)
BEGIN
    CREATE INDEX IX_etl_invalid_records_entidade_data
        ON dbo.etl_invalid_records (entidade, created_at DESC);
    PRINT 'Indice IX_etl_invalid_records_entidade_data criado com sucesso!';
END
ELSE
BEGIN
    PRINT 'Indice IX_etl_invalid_records_entidade_data ja existe. Pulando criacao.';
END
GO
