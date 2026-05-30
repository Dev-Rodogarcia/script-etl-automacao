PRINT 'Migration 026: materializar chave de usuario emissor de cotacoes';
GO

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;
GO

IF OBJECT_ID(N'dbo.cotacoes', N'U') IS NULL
BEGIN
    RAISERROR('Tabela dbo.cotacoes nao encontrada. Execute os scripts-base antes da migration 026.', 16, 1);
    RETURN;
END;
GO

IF COL_LENGTH(N'dbo.cotacoes', N'user_name_key') IS NULL
BEGIN
    ALTER TABLE dbo.cotacoes
    ADD user_name_key AS NULLIF(LOWER(LTRIM(RTRIM(user_name))), N'') PERSISTED;

    PRINT 'Coluna computada dbo.cotacoes.user_name_key criada.';
END
ELSE
BEGIN
    PRINT 'Coluna dbo.cotacoes.user_name_key ja existe.';
END;
GO

IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_cotacoes_usuario_key_requested_at' AND object_id = OBJECT_ID(N'dbo.cotacoes'))
BEGIN
    DROP INDEX IX_cotacoes_usuario_key_requested_at ON dbo.cotacoes;
    PRINT 'Indice legado IX_cotacoes_usuario_key_requested_at removido.';
END;
GO

-- O dashboard sempre filtra Cotações por periodo antes de aplicar Usuario.
-- requested_at fica como chave lider para reduzir a maior volumetria primeiro;
-- user_name_key permanece como chave secundaria para refinar o conjunto do periodo.
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_cotacoes_requested_at_usuario_key' AND object_id = OBJECT_ID(N'dbo.cotacoes'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_cotacoes_requested_at_usuario_key
    ON dbo.cotacoes(requested_at DESC, user_name_key)
    INCLUDE (sequence_code, user_name, customer_name, branch_nickname, total_value, taxed_weight, cte_issued_at, nfse_issued_at);

    PRINT 'Indice IX_cotacoes_requested_at_usuario_key criado.';
END
ELSE
BEGIN
    PRINT 'Indice IX_cotacoes_requested_at_usuario_key ja existe.';
END;
GO

:r views\015_criar_view_cotacoes_powerbi.sql

IF OBJECT_ID(N'dbo.schema_migrations', N'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM dbo.schema_migrations WHERE migration_id = N'026_materializar_chave_usuario_cotacoes')
BEGIN
    INSERT INTO dbo.schema_migrations (migration_id, notes)
    VALUES (
        N'026_materializar_chave_usuario_cotacoes',
        N'Materializa chave normalizada do usuario emissor de cotacoes e publica Usuario Key na view de cotacoes.'
    );
END;
GO

PRINT 'Chave de usuario emissor de cotacoes materializada com sucesso.';
GO
