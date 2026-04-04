-- ============================================
-- Script de criacao da tabela 'sys_execution_history'
-- ============================================

IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'dbo.sys_execution_history') AND type in (N'U'))
BEGIN
    CREATE TABLE dbo.sys_execution_history (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        start_time DATETIME2 NOT NULL,
        end_time DATETIME2 NOT NULL,
        duration_seconds INT NOT NULL,
        status VARCHAR(20) NOT NULL,
        total_records INT NOT NULL DEFAULT 0,
        error_category VARCHAR(50) NULL,
        error_message VARCHAR(500) NULL,
        created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
    );
    PRINT 'Tabela sys_execution_history criada com sucesso!';
END
ELSE
BEGIN
    PRINT 'Tabela sys_execution_history ja existe. Pulando criacao.';
END
GO

IF NOT EXISTS (
    SELECT * FROM sys.indexes
    WHERE name = 'IX_sys_execution_history_start_time'
      AND object_id = OBJECT_ID('dbo.sys_execution_history')
)
BEGIN
    CREATE INDEX IX_sys_execution_history_start_time
        ON dbo.sys_execution_history (start_time DESC);
    PRINT 'Indice IX_sys_execution_history_start_time criado com sucesso!';
END
ELSE
BEGIN
    PRINT 'Indice IX_sys_execution_history_start_time ja existe. Pulando criacao.';
END
GO
