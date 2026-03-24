-- ============================================
-- Script de criação da view 'vw_dim_veiculos'
-- Execute este script UMA VEZ antes de colocar o sistema em produção
-- NOTA: Esta view depende da view vw_manifestos_powerbi
-- Execute APÓS as views principais (011-018)
-- 
-- IMPORTANTE: Garante unicidade por Placa (chave primária da dimensão)
-- Usa GROUP BY para garantir uma única linha por placa, agrega outros campos com MAX
-- ============================================

CREATE OR ALTER VIEW dbo.vw_dim_veiculos AS
SELECT 
    UPPER(LTRIM(RTRIM([Veículo/Placa]))) AS Placa,
    MAX(UPPER(LTRIM(RTRIM([Tipo Veículo])))) AS TipoVeiculo,
    MAX(UPPER(LTRIM(RTRIM([Proprietário/Nome])))) AS Proprietario
FROM dbo.vw_manifestos_powerbi
WHERE [Veículo/Placa] IS NOT NULL AND LTRIM(RTRIM([Veículo/Placa])) <> ''
GROUP BY UPPER(LTRIM(RTRIM([Veículo/Placa])));
GO

PRINT 'View vw_dim_veiculos criada/atualizada com sucesso!';
PRINT 'NOTA: Garante unicidade por Placa (chave primária) usando GROUP BY';
GO
