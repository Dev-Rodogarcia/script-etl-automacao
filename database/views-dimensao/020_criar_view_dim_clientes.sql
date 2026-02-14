-- ============================================
-- Script de criação da view 'vw_dim_clientes'
-- Execute este script UMA VEZ antes de colocar o sistema em produção
-- Execute APÓS as tabelas principais
-- 
-- IMPORTANTE: Esta versão garante unicidade absoluta por NOME NORMALIZADO
-- (UPPER + LTRIM + RTRIM), blindando duplicatas na dimensão de clientes.
-- ============================================

IF OBJECT_ID('dbo.vw_dim_clientes', 'V') IS NOT NULL
    DROP VIEW dbo.vw_dim_clientes;
IF OBJECT_ID('dbo.vw_dim_clientes', 'U') IS NOT NULL
    DROP TABLE dbo.vw_dim_clientes;
GO

CREATE VIEW dbo.vw_dim_clientes AS
WITH ClientesBrutos AS (
    -- Fonte 1: Fretes (Remetente, Destinatario, Pagador)
    SELECT remetente_nome AS Nome FROM dbo.fretes WHERE remetente_nome IS NOT NULL
    UNION ALL
    SELECT destinatario_nome FROM dbo.fretes WHERE destinatario_nome IS NOT NULL
    UNION ALL
    SELECT pagador_nome FROM dbo.fretes WHERE pagador_nome IS NOT NULL

    UNION ALL

    -- Fonte 2: Coletas (Cliente)
    SELECT cliente_nome FROM dbo.coletas WHERE cliente_nome IS NOT NULL

    UNION ALL

    -- Fonte 3: Faturas por Cliente (Pagador, Remetente, Destinatario)
    SELECT pagador_nome FROM dbo.faturas_por_cliente WHERE pagador_nome IS NOT NULL
    UNION ALL
    SELECT remetente_nome FROM dbo.faturas_por_cliente WHERE remetente_nome IS NOT NULL
    UNION ALL
    SELECT destinatario_nome FROM dbo.faturas_por_cliente WHERE destinatario_nome IS NOT NULL
)
SELECT 
    DISTINCT UPPER(LTRIM(RTRIM(Nome))) AS [Nome]
FROM ClientesBrutos
WHERE LTRIM(RTRIM(Nome)) <> '';
GO

PRINT 'View vw_dim_clientes criada/atualizada com sucesso!';
PRINT 'NOTA: Garante unicidade por Nome normalizado (UPPER + TRIM)';
GO
