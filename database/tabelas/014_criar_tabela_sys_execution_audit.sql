-- ============================================
-- Script de criacao da tabela 'sys_execution_audit'
-- Auditoria estruturada autorizativa por execution_uuid e entidade
-- ============================================

IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'dbo.sys_execution_audit') AND type in (N'U'))
BEGIN
    CREATE TABLE dbo.sys_execution_audit (
        execution_uuid NVARCHAR(36) NOT NULL,
        entidade NVARCHAR(50) NOT NULL,
        janela_consulta_inicio DATETIME2 NULL,
        janela_consulta_fim DATETIME2 NULL,
        janela_confirmacao_inicio DATETIME2 NULL,
        janela_confirmacao_fim DATETIME2 NULL,
        status_execucao NVARCHAR(30) NOT NULL,
        api_total_bruto INT NOT NULL CONSTRAINT DF_sys_execution_audit_api_total_bruto DEFAULT (0),
        api_total_unico INT NOT NULL CONSTRAINT DF_sys_execution_audit_api_total_unico DEFAULT (0),
        db_upserts INT NOT NULL CONSTRAINT DF_sys_execution_audit_db_upserts DEFAULT (0),
        db_persistidos INT NOT NULL CONSTRAINT DF_sys_execution_audit_db_persistidos DEFAULT (0),
        api_completa BIT NOT NULL CONSTRAINT DF_sys_execution_audit_api_completa DEFAULT (0),
        motivo_incompletude NVARCHAR(200) NULL,
        paginas_processadas INT NOT NULL CONSTRAINT DF_sys_execution_audit_paginas DEFAULT (0),
        noop_count INT NOT NULL CONSTRAINT DF_sys_execution_audit_noop DEFAULT (0),
        invalid_count INT NOT NULL CONSTRAINT DF_sys_execution_audit_invalid DEFAULT (0),
        started_at DATETIME2 NULL,
        finished_at DATETIME2 NULL,
        command_name NVARCHAR(100) NULL,
        cycle_id NVARCHAR(100) NULL,
        detalhe NVARCHAR(MAX) NULL,
        updated_at DATETIME2 NOT NULL CONSTRAINT DF_sys_execution_audit_updated_at DEFAULT SYSDATETIME(),
        CONSTRAINT PK_sys_execution_audit PRIMARY KEY CLUSTERED (execution_uuid, entidade)
    );

    PRINT 'Tabela sys_execution_audit criada com sucesso!';
END
ELSE
BEGIN
    PRINT 'Tabela sys_execution_audit ja existe. Pulando criacao.';
END
GO

IF NOT EXISTS (
    SELECT * FROM sys.indexes
    WHERE name = 'IX_sys_execution_audit_finished_at'
      AND object_id = OBJECT_ID('dbo.sys_execution_audit')
)
BEGIN
    CREATE INDEX IX_sys_execution_audit_finished_at
        ON dbo.sys_execution_audit (finished_at DESC, entidade);
    PRINT 'Indice IX_sys_execution_audit_finished_at criado com sucesso!';
END
ELSE
BEGIN
    PRINT 'Indice IX_sys_execution_audit_finished_at ja existe. Pulando criacao.';
END
GO

IF NOT EXISTS (
    SELECT * FROM sys.indexes
    WHERE name = 'IX_sys_execution_audit_command_cycle'
      AND object_id = OBJECT_ID('dbo.sys_execution_audit')
)
BEGIN
    CREATE INDEX IX_sys_execution_audit_command_cycle
        ON dbo.sys_execution_audit (command_name, cycle_id, finished_at DESC);
    PRINT 'Indice IX_sys_execution_audit_command_cycle criado com sucesso!';
END
ELSE
BEGIN
    PRINT 'Indice IX_sys_execution_audit_command_cycle ja existe. Pulando criacao.';
END
GO
