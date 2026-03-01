-- ============================================
-- Script de VALIDAÇÃO: Verificar se destroy_user_id é do tipo Individual
-- Execute este script para validar se o JOIN está correto
-- ============================================

-- Verificar quantos destroy_user_id existem na tabela coletas
SELECT 
    COUNT(*) AS total_coletas_com_destroy_user_id,
    COUNT(DISTINCT destroy_user_id) AS ids_unicos_destroy_user_id
FROM dbo.coletas
WHERE destroy_user_id IS NOT NULL;

-- Verificar quantos desses IDs existem na tabela dim_usuarios (Individual)
SELECT 
    COUNT(DISTINCT c.destroy_user_id) AS total_ids_coletas,
    COUNT(DISTINCT u.user_id) AS total_ids_encontrados_dim_usuarios,
    COUNT(DISTINCT c.destroy_user_id) - COUNT(DISTINCT u.user_id) AS ids_nao_encontrados,
    CAST(COUNT(DISTINCT u.user_id) * 100.0 / NULLIF(COUNT(DISTINCT c.destroy_user_id), 0) AS DECIMAL(5,2)) AS percentual_match
FROM dbo.coletas c
LEFT JOIN dbo.dim_usuarios u ON c.destroy_user_id = u.user_id
WHERE c.destroy_user_id IS NOT NULL;

-- Listar alguns exemplos de IDs que NÃO foram encontrados (se houver)
-- Se muitos IDs não forem encontrados, provavelmente destroy_user_id NÃO é do tipo Individual
SELECT TOP 20
    c.destroy_user_id,
    COUNT(*) AS quantidade_coletas
FROM dbo.coletas c
LEFT JOIN dbo.dim_usuarios u ON c.destroy_user_id = u.user_id
WHERE c.destroy_user_id IS NOT NULL
  AND u.user_id IS NULL
GROUP BY c.destroy_user_id
ORDER BY quantidade_coletas DESC;

-- Verificar também cancellation_user_id
SELECT 
    COUNT(DISTINCT c.cancellation_user_id) AS total_ids_cancellation,
    COUNT(DISTINCT u.user_id) AS total_ids_encontrados_dim_usuarios,
    COUNT(DISTINCT c.cancellation_user_id) - COUNT(DISTINCT u.user_id) AS ids_nao_encontrados,
    CAST(COUNT(DISTINCT u.user_id) * 100.0 / NULLIF(COUNT(DISTINCT c.cancellation_user_id), 0) AS DECIMAL(5,2)) AS percentual_match
FROM dbo.coletas c
LEFT JOIN dbo.dim_usuarios u ON c.cancellation_user_id = u.user_id
WHERE c.cancellation_user_id IS NOT NULL;

PRINT '========================================';
PRINT 'INTERPRETAÇÃO DOS RESULTADOS:';
PRINT '========================================';
PRINT 'Se percentual_match for próximo de 100%, destroy_user_id é provavelmente do tipo Individual';
PRINT 'Se percentual_match for muito baixo (< 50%), destroy_user_id pode ser de outro tipo';
PRINT 'Se muitos IDs não forem encontrados, revisar a implementação';
PRINT '========================================';
GO
