-- ============================================
-- Script de criação da view 'vw_dim_usuarios'
-- Execute este script UMA VEZ antes de colocar o sistema em produção
-- NOTA: Esta view depende da tabela dim_usuarios (populada pela extração de usuários)
-- Execute APÓS a criação da tabela dim_usuarios (011)
-- 
-- IMPORTANTE: Garante unicidade por user_id (chave primária da dimensão)
-- Esta view expõe os usuários do sistema (Individual) para uso no PowerBI
-- ============================================

CREATE OR ALTER VIEW dbo.vw_dim_usuarios AS
SELECT 
    [user_id] AS [User ID],
    LTRIM(RTRIM([nome])) AS [Nome],
    [data_atualizacao] AS [Data Atualizacao]
FROM dbo.dim_usuarios
WHERE [user_id] IS NOT NULL;
GO

PRINT 'View vw_dim_usuarios criada/atualizada com sucesso!';
PRINT 'NOTA: Garante unicidade por user_id (chave primária da tabela dim_usuarios)';
GO
