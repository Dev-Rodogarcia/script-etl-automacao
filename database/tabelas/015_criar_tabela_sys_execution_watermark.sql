-- ============================================
-- Script de criacao da tabela 'sys_execution_watermark'
-- Watermark confirmado por entidade para overlap idempotente
-- ============================================

IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'dbo.sys_execution_watermark') AND type in (N'U'))
BEGIN
    CREATE TABLE dbo.sys_execution_watermark (
        entidade NVARCHAR(50) NOT NULL,
        watermark_confirmado DATETIME2 NOT NULL,
        updated_at DATETIME2 NOT NULL CONSTRAINT DF_sys_execution_watermark_updated_at DEFAULT SYSDATETIME(),
        CONSTRAINT PK_sys_execution_watermark PRIMARY KEY CLUSTERED (entidade)
    );

    PRINT 'Tabela sys_execution_watermark criada com sucesso!';
END
ELSE
BEGIN
    PRINT 'Tabela sys_execution_watermark ja existe. Pulando criacao.';
END
GO
