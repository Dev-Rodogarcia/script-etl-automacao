-- ============================================
-- Script de criação da view 'vw_dim_filiais'
-- Execute este script UMA VEZ antes de colocar o sistema em produção
-- NOTA: Esta view depende das views principais (vw_fretes_powerbi, vw_manifestos_powerbi, etc.)
-- Execute APÓS as views principais (011-018)
-- 
-- IMPORTANTE: Garante unicidade por nome (normalizado - trim e case-insensitive para comparação)
-- ============================================

CREATE OR ALTER VIEW dbo.vw_dim_filiais AS
WITH FiliaisUnificadas AS (
    SELECT DISTINCT LTRIM(RTRIM([Filial])) AS [NomeFilial] 
    FROM dbo.vw_fretes_powerbi
    WHERE [Filial] IS NOT NULL AND LTRIM(RTRIM([Filial])) <> ''
    
    UNION
    
    SELECT DISTINCT LTRIM(RTRIM([Filial])) AS [NomeFilial]
    FROM dbo.vw_manifestos_powerbi
    WHERE [Filial] IS NOT NULL AND LTRIM(RTRIM([Filial])) <> ''
    
    UNION
    
    SELECT DISTINCT LTRIM(RTRIM([Filial])) AS [NomeFilial]
    FROM dbo.vw_contas_a_pagar_powerbi
    WHERE [Filial] IS NOT NULL AND LTRIM(RTRIM([Filial])) <> ''
    
    UNION
    
    SELECT DISTINCT LTRIM(RTRIM([Filial])) AS [NomeFilial]
    FROM dbo.vw_faturas_por_cliente_powerbi
    WHERE [Filial] IS NOT NULL AND LTRIM(RTRIM([Filial])) <> ''
)
SELECT 
    [NomeFilial],
    CAST('00:00:00' AS TIME(0)) AS [Hora (Solicitacao)]
FROM FiliaisUnificadas
GO

PRINT 'View vw_dim_filiais criada/atualizada com sucesso!';
PRINT 'NOTA: Garante unicidade por nome (normalizado com TRIM)';
GO
