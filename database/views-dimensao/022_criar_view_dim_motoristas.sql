-- ============================================
-- Script de criação da view 'vw_dim_motoristas'
-- Execute este script UMA VEZ antes de colocar o sistema em produção
-- NOTA: Esta view lê diretamente da tabela física dbo.manifestos
-- 
-- IMPORTANTE: Garante unicidade por nome (normalizado - UPPER + TRIM) e filial
-- ============================================

CREATE OR ALTER VIEW dbo.vw_dim_motoristas AS
SELECT DISTINCT
    UPPER(LTRIM(RTRIM(driver_name))) AS NomeMotorista,
    branch_nickname AS Filial
FROM dbo.manifestos
WHERE driver_name IS NOT NULL
  AND LTRIM(RTRIM(driver_name)) <> ''
  AND excluido_na_origem = 0
  AND driver_name NOT LIKE '%MOTORISTA%';
GO

PRINT 'View vw_dim_motoristas criada/atualizada com sucesso!';
PRINT 'NOTA: Garante unicidade por nome normalizado (UPPER + TRIM) e filial';
GO
