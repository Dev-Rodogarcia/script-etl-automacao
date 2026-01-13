-- ============================================
-- Script de criação da view 'vw_dim_clientes'
-- Execute este script UMA VEZ antes de colocar o sistema em produção
-- NOTA: Esta view depende das views principais (vw_fretes_powerbi, vw_coletas_powerbi)
-- Execute APÓS as views principais (011-018)
-- 
-- IMPORTANTE: Esta view garante unicidade por ID (chave primária da dimensão)
-- Se o mesmo ID aparecer em múltiplas fontes, mantém apenas um registro (prioriza nome não-nulo)
-- ============================================

CREATE OR ALTER VIEW dbo.vw_dim_clientes AS
WITH ClientesUnificados AS (
    -- Clientes vindos de Fretes (Pagador)
    SELECT [Pagador ID] AS [ID], [Pagador] AS [Nome], 'Fretes' AS Fonte
    FROM dbo.vw_fretes_powerbi
    WHERE [Pagador ID] IS NOT NULL
    
    UNION ALL
    
    -- Clientes vindos de Coletas
    SELECT [Cliente ID] AS [ID], [Cliente] AS [Nome], 'Coletas' AS Fonte
    FROM dbo.vw_coletas_powerbi
    WHERE [Cliente ID] IS NOT NULL
)
SELECT 
    [ID],
    -- Prioriza nome não-nulo, se ambos forem não-nulos usa MAX (mais comum/consistente)
    MAX([Nome]) AS [Nome]
FROM ClientesUnificados
WHERE [ID] IS NOT NULL
GROUP BY [ID];
GO

PRINT 'View vw_dim_clientes criada/atualizada com sucesso!';
PRINT 'NOTA: Garante unicidade por ID - mesmo ID em múltiplas fontes resulta em uma única linha';
GO
