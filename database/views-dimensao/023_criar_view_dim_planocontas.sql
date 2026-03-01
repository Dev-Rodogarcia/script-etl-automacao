-- ============================================
-- Script de criação da view 'vw_dim_planocontas'
-- Execute este script UMA VEZ antes de colocar o sistema em produção
-- NOTA: Esta view depende da view vw_contas_a_pagar_powerbi
-- Execute APÓS as views principais (011-018)
-- 
-- IMPORTANTE: Garante unicidade por Descricao (chave primária da dimensão)
-- Usa GROUP BY para garantir uma única linha por descrição, agrega Classificacao com MAX
-- ============================================

IF OBJECT_ID('dbo.vw_dim_planocontas', 'V') IS NOT NULL 
    DROP VIEW dbo.vw_dim_planocontas;
GO

CREATE VIEW dbo.vw_dim_planocontas AS
SELECT
    UPPER(LTRIM(RTRIM([Conta Contábil/Descrição]))) AS Descricao,
    ISNULL(MAX([Conta Contábil/Classificação]), 'OUTROS / NÃO CLASSIFICADO') AS Classificacao,
    CAST('00:00:00' AS TIME(0)) AS [Hora (Solicitacao)]
FROM dbo.vw_contas_a_pagar_powerbi
WHERE [Conta Contábil/Descrição] IS NOT NULL
  AND LTRIM(RTRIM([Conta Contábil/Descrição])) <> ''
GROUP BY UPPER(LTRIM(RTRIM([Conta Contábil/Descrição])));
GO

PRINT 'View vw_dim_planocontas criada/atualizada com sucesso!';
PRINT 'NOTA: Garante unicidade por Descricao (chave primária) usando GROUP BY';
GO
