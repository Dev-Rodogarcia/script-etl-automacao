-- ============================================
-- Script de criação da view 'vw_dim_motoristas'
-- Execute este script UMA VEZ antes de colocar o sistema em produção
-- NOTA: Esta view depende da view vw_manifestos_powerbi
-- Execute APÓS as views principais (011-018)
-- 
-- IMPORTANTE: Garante unicidade por nome (normalizado - UPPER + TRIM)
-- O nome normalizado é usado como chave primária da dimensão
-- ============================================

CREATE OR ALTER VIEW dbo.vw_dim_motoristas AS
SELECT DISTINCT UPPER(LTRIM(RTRIM([Motorista]))) AS NomeMotorista
FROM dbo.vw_manifestos_powerbi
WHERE [Motorista] IS NOT NULL
  AND LTRIM(RTRIM([Motorista])) <> ''
  AND [Motorista] NOT LIKE '%MOTORISTA%';
GO

PRINT 'View vw_dim_motoristas criada/atualizada com sucesso!';
PRINT 'NOTA: Garante unicidade por nome normalizado (UPPER + TRIM)';
GO
