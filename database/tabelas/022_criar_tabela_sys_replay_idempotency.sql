-- Script de criacao da tabela 'sys_replay_idempotency'

IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'dbo.sys_replay_idempotency') AND type in (N'U'))
BEGIN
    CREATE TABLE dbo.sys_replay_idempotency (
        idempotency_key NVARCHAR(255) NOT NULL,
        api NVARCHAR(100) NOT NULL CONSTRAINT DF_sys_replay_idempotency_api DEFAULT (''),
        entidade NVARCHAR(100) NOT NULL CONSTRAINT DF_sys_replay_idempotency_entidade DEFAULT (''),
        data_inicio DATE NOT NULL,
        data_fim DATE NOT NULL,
        modo NVARCHAR(50) NOT NULL CONSTRAINT DF_sys_replay_idempotency_modo DEFAULT ('replay'),
        status NVARCHAR(20) NOT NULL,
        execution_uuid NVARCHAR(100) NULL,
        started_at DATETIME2 NOT NULL,
        finished_at DATETIME2 NULL,
        expires_at DATETIME2 NULL,
        last_error NVARCHAR(1000) NULL,
        updated_at DATETIME2 NOT NULL CONSTRAINT DF_sys_replay_idempotency_updated_at DEFAULT SYSDATETIME(),
        CONSTRAINT PK_sys_replay_idempotency PRIMARY KEY CLUSTERED (idempotency_key)
    );

    PRINT 'Tabela sys_replay_idempotency criada com sucesso!';
END
ELSE
BEGIN
    PRINT 'Tabela sys_replay_idempotency ja existe. Pulando criacao.';
END
GO

IF NOT EXISTS (
    SELECT 1
      FROM sys.indexes
     WHERE name = 'IX_sys_replay_idempotency_status_expires'
       AND object_id = OBJECT_ID('dbo.sys_replay_idempotency')
)
BEGIN
    CREATE INDEX IX_sys_replay_idempotency_status_expires
        ON dbo.sys_replay_idempotency (status, expires_at, updated_at DESC);
    PRINT 'Indice IX_sys_replay_idempotency_status_expires criado com sucesso!';
END
ELSE
BEGIN
    PRINT 'Indice IX_sys_replay_idempotency_status_expires ja existe. Pulando criacao.';
END
GO
