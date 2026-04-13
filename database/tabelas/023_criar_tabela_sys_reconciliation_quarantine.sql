-- Script de criacao da tabela 'sys_reconciliation_quarantine'

IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'dbo.sys_reconciliation_quarantine') AND type in (N'U'))
BEGIN
    CREATE TABLE dbo.sys_reconciliation_quarantine (
        entity_name NVARCHAR(100) NOT NULL,
        record_id BIGINT NOT NULL,
        window_start DATE NOT NULL,
        window_end DATE NOT NULL,
        first_seen_absent_at DATETIME2 NOT NULL,
        last_seen_absent_at DATETIME2 NOT NULL,
        absence_hits INT NOT NULL CONSTRAINT DF_sys_reconciliation_quarantine_absence_hits DEFAULT (0),
        first_execution_uuid NVARCHAR(100) NULL,
        last_execution_uuid NVARCHAR(100) NULL,
        first_cycle_id NVARCHAR(100) NULL,
        last_cycle_id NVARCHAR(100) NULL,
        released_at DATETIME2 NULL,
        release_reason NVARCHAR(50) NULL,
        last_guardrail_reason NVARCHAR(100) NULL,
        updated_at DATETIME2 NOT NULL CONSTRAINT DF_sys_reconciliation_quarantine_updated_at DEFAULT SYSDATETIME(),
        CONSTRAINT PK_sys_reconciliation_quarantine
            PRIMARY KEY CLUSTERED (entity_name, record_id, window_start, window_end)
    );

    PRINT 'Tabela sys_reconciliation_quarantine criada com sucesso!';
END
ELSE
BEGIN
    PRINT 'Tabela sys_reconciliation_quarantine ja existe. Pulando criacao.';
END
GO

IF NOT EXISTS (
    SELECT 1
      FROM sys.indexes
     WHERE name = 'IX_sys_reconciliation_quarantine_lookup'
       AND object_id = OBJECT_ID('dbo.sys_reconciliation_quarantine')
)
BEGIN
    CREATE INDEX IX_sys_reconciliation_quarantine_lookup
        ON dbo.sys_reconciliation_quarantine (entity_name, window_start, window_end, released_at, absence_hits);
    PRINT 'Indice IX_sys_reconciliation_quarantine_lookup criado com sucesso!';
END
ELSE
BEGIN
    PRINT 'Indice IX_sys_reconciliation_quarantine_lookup ja existe. Pulando criacao.';
END
GO
