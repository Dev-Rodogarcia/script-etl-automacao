-- ============================================
-- Script de criação da view 'vw_dim_veiculos'
-- Execute este script UMA VEZ antes de colocar o sistema em produção
-- NOTA: Esta view lê diretamente da tabela física dbo.manifestos
-- 
-- IMPORTANTE: Garante unicidade por placa e filial
-- Usa GROUP BY para garantir uma única linha por placa/filial, agrega outros campos com MAX
-- ============================================

CREATE OR ALTER VIEW dbo.vw_dim_veiculos AS
SELECT 
    UPPER(LTRIM(RTRIM(vehicle_plate))) AS Placa,
    MAX(UPPER(LTRIM(RTRIM(vehicle_type)))) AS TipoVeiculo,
    MAX(UPPER(LTRIM(RTRIM(vehicle_owner)))) AS Proprietario,
    branch_nickname AS Filial
FROM dbo.manifestos
WHERE vehicle_plate IS NOT NULL
  AND LTRIM(RTRIM(vehicle_plate)) <> ''
  AND excluido_na_origem = 0
GROUP BY
    UPPER(LTRIM(RTRIM(vehicle_plate))),
    branch_nickname;
GO

PRINT 'View vw_dim_veiculos criada/atualizada com sucesso!';
PRINT 'NOTA: Garante unicidade por placa e filial usando GROUP BY';
GO
