-- ============================================
-- Script de criação da tabela 'page_audit'
-- Execute este script UMA VEZ antes de colocar o sistema em produção
-- ============================================

IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'dbo.page_audit') AND type in (N'U'))
BEGIN
    CREATE TABLE dbo.page_audit (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        execution_uuid NVARCHAR(36) NOT NULL,
        run_uuid NVARCHAR(36) NOT NULL,
        template_id INT NOT NULL,
        page INT NOT NULL,
        per INT NOT NULL,
        janela_inicio DATE NULL,
        janela_fim DATE NULL,
        req_hash CHAR(64) NOT NULL,
        resp_hash CHAR(64) NOT NULL,
        total_itens INT NOT NULL,

        id_key NVARCHAR(50) NULL,
        id_min_num BIGINT NULL,
        id_max_num BIGINT NULL,
        id_min_str NVARCHAR(80) NULL,
        id_max_str NVARCHAR(80) NULL,

        status_code INT NOT NULL,
        duracao_ms INT NOT NULL,

        timestamp DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
    );
    
    PRINT 'Tabela page_audit criada com sucesso!';
END
ELSE
BEGIN
    PRINT 'Tabela page_audit já existe. Pulando criação.';
END
GO

-- Criar índices (idempotente - só cria se não existir)
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'ux_page_audit_run_template_page' AND object_id = OBJECT_ID('dbo.page_audit'))
BEGIN
    CREATE UNIQUE INDEX ux_page_audit_run_template_page
        ON dbo.page_audit (run_uuid, template_id, page);
    PRINT 'Índice ux_page_audit_run_template_page criado com sucesso!';
END
ELSE
BEGIN
    PRINT 'Índice ux_page_audit_run_template_page já existe. Pulando criação.';
END
GO

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'ix_page_audit_exec_timestamp' AND object_id = OBJECT_ID('dbo.page_audit'))
BEGIN
    CREATE INDEX ix_page_audit_exec_timestamp
        ON dbo.page_audit (execution_uuid, timestamp DESC);
    PRINT 'Índice ix_page_audit_exec_timestamp criado com sucesso!';
END
ELSE
BEGIN
    PRINT 'Índice ix_page_audit_exec_timestamp já existe. Pulando criação.';
END
GO

-- Criar constraints (idempotente - só cria se não existir)
IF NOT EXISTS (SELECT * FROM sys.objects WHERE name = 'ck_page_audit_status' AND type = 'C' AND parent_object_id = OBJECT_ID('dbo.page_audit'))
BEGIN
    ALTER TABLE dbo.page_audit ADD CONSTRAINT ck_page_audit_status
        CHECK (status_code BETWEEN 100 AND 599);
    PRINT 'Constraint ck_page_audit_status criada com sucesso!';
END
ELSE
BEGIN
    PRINT 'Constraint ck_page_audit_status já existe. Pulando criação.';
END
GO

IF NOT EXISTS (SELECT * FROM sys.objects WHERE name = 'ck_page_audit_hash_len' AND type = 'C' AND parent_object_id = OBJECT_ID('dbo.page_audit'))
BEGIN
    ALTER TABLE dbo.page_audit ADD CONSTRAINT ck_page_audit_hash_len
        CHECK (LEN(req_hash) = 64 AND LEN(resp_hash) = 64);
    PRINT 'Constraint ck_page_audit_hash_len criada com sucesso!';
END
ELSE
BEGIN
    PRINT 'Constraint ck_page_audit_hash_len já existe. Pulando criação.';
END
GO

IF NOT EXISTS (SELECT * FROM sys.objects WHERE name = 'ck_page_audit_bounds' AND type = 'C' AND parent_object_id = OBJECT_ID('dbo.page_audit'))
BEGIN
    ALTER TABLE dbo.page_audit ADD CONSTRAINT ck_page_audit_bounds
        CHECK (page >= 1 AND per >= 1 AND total_itens >= 0);
    PRINT 'Constraint ck_page_audit_bounds criada com sucesso!';
END
ELSE
BEGIN
    PRINT 'Constraint ck_page_audit_bounds já existe. Pulando criação.';
END
GO

IF NOT EXISTS (SELECT * FROM sys.objects WHERE name = 'ck_page_audit_id_range' AND type = 'C' AND parent_object_id = OBJECT_ID('dbo.page_audit'))
BEGIN
    ALTER TABLE dbo.page_audit ADD CONSTRAINT ck_page_audit_id_range
        CHECK ((id_min_num IS NULL OR id_max_num IS NULL) OR (id_min_num <= id_max_num));
    PRINT 'Constraint ck_page_audit_id_range criada com sucesso!';
END
ELSE
BEGIN
    PRINT 'Constraint ck_page_audit_id_range já existe. Pulando criação.';
END
GO
