-- ============================================
-- Script de diagnóstico: Campos NULL em Coletas
-- Verifica se cancellation_user_id, destroy_user_id e vehicle_type_id
-- estão sendo extraídos corretamente da API
-- ============================================

PRINT '============================================';
PRINT 'DIAGNÓSTICO: Campos NULL em Coletas';
PRINT '============================================';
PRINT '';

-- 1. Verificar se os dados existem no metadata JSON
PRINT '1. Verificando se dados existem no metadata JSON...';
PRINT '';

SELECT TOP 10
    id,
    sequence_code,
    status,
    cancellation_reason,
    cancellation_user_id,
    destroy_reason,
    destroy_user_id,
    vehicle_type_id,
    -- Extrair do JSON metadata
    JSON_VALUE(metadata, '$.cancellationUserId') AS cancellationUserId_metadata,
    JSON_VALUE(metadata, '$.destroyUserId') AS destroyUserId_metadata,
    JSON_VALUE(metadata, '$.vehicleTypeId') AS vehicleTypeId_metadata,
    JSON_VALUE(metadata, '$.cancellationReason') AS cancellationReason_metadata,
    JSON_VALUE(metadata, '$.destroyReason') AS destroyReason_metadata
FROM dbo.coletas
WHERE metadata IS NOT NULL
ORDER BY data_extracao DESC;

PRINT '';
PRINT 'Interpretação:';
PRINT '  - Se cancellationUserId_metadata for NULL → API não retornou (normal para coletas não canceladas)';
PRINT '  - Se cancellationUserId_metadata tiver valor mas cancellation_user_id for NULL → Problema no mapeamento';
PRINT '  - Se ambos forem NULL → Dados realmente não existem na API';
PRINT '';

-- 2. Verificar se há coletas canceladas/destruídas
PRINT '2. Verificando estatísticas de coletas canceladas/destruídas...';
PRINT '';

SELECT 
    COUNT(*) AS total_coletas,
    COUNT(cancellation_reason) AS coletas_canceladas,
    COUNT(cancellation_user_id) AS com_usuario_cancelamento,
    COUNT(destroy_reason) AS coletas_destruidas,
    COUNT(destroy_user_id) AS com_usuario_destruicao,
    COUNT(vehicle_type_id) AS com_veiculo
FROM dbo.coletas
WHERE data_extracao >= DATEADD(day, -7, GETDATE()); -- Últimos 7 dias

PRINT '';
PRINT 'Interpretação:';
PRINT '  - Se coletas_canceladas = 0 → Nenhuma coleta foi cancelada (normal)';
PRINT '  - Se coletas_destruidas = 0 → Nenhuma coleta foi destruída (normal)';
PRINT '  - Se houver coletas canceladas mas com_usuario_cancelamento = 0 → Problema no mapeamento';
PRINT '';

-- 3. Verificar se dim_usuarios tem os IDs corretos
PRINT '3. Verificando se user_ids existem em dim_usuarios...';
PRINT '';

SELECT 
    'cancellation_user_id' AS tipo_campo,
    COUNT(DISTINCT c.cancellation_user_id) AS total_ids_distintos,
    COUNT(DISTINCT u.user_id) AS ids_encontrados,
    COUNT(DISTINCT c.cancellation_user_id) - COUNT(DISTINCT u.user_id) AS ids_nao_encontrados
FROM dbo.coletas c
LEFT JOIN dbo.dim_usuarios u ON c.cancellation_user_id = u.user_id
WHERE c.cancellation_user_id IS NOT NULL

UNION ALL

SELECT 
    'destroy_user_id' AS tipo_campo,
    COUNT(DISTINCT c.destroy_user_id) AS total_ids_distintos,
    COUNT(DISTINCT u.user_id) AS ids_encontrados,
    COUNT(DISTINCT c.destroy_user_id) - COUNT(DISTINCT u.user_id) AS ids_nao_encontrados
FROM dbo.coletas c
LEFT JOIN dbo.dim_usuarios u ON c.destroy_user_id = u.user_id
WHERE c.destroy_user_id IS NOT NULL;

PRINT '';
PRINT 'Interpretação:';
PRINT '  - Se ids_nao_encontrados > 0 → Alguns IDs não estão em dim_usuarios';
PRINT '  - Solução: Re-executar extração de usuarios_sistema ou verificar se IDs são de outro tipo';
PRINT '';

-- 4. Exemplos de coletas com dados preenchidos (se existirem)
PRINT '4. Exemplos de coletas com cancellation_user_id ou destroy_user_id preenchidos...';
PRINT '';

SELECT TOP 5
    c.id,
    c.sequence_code,
    c.status,
    c.cancellation_reason,
    c.cancellation_user_id,
    u_cancel.nome AS cancellation_user_nome,
    c.destroy_reason,
    c.destroy_user_id,
    u_destroy.nome AS destroy_user_nome,
    c.vehicle_type_id
FROM dbo.coletas c
LEFT JOIN dbo.dim_usuarios u_cancel ON c.cancellation_user_id = u_cancel.user_id
LEFT JOIN dbo.dim_usuarios u_destroy ON c.destroy_user_id = u_destroy.user_id
WHERE c.cancellation_user_id IS NOT NULL 
   OR c.destroy_user_id IS NOT NULL
   OR c.vehicle_type_id IS NOT NULL
ORDER BY c.data_extracao DESC;

PRINT '';
PRINT '============================================';
PRINT 'DIAGNÓSTICO CONCLUÍDO';
PRINT '============================================';
PRINT '';
PRINT 'Próximos passos:';
PRINT '  1. Se dados estão no metadata mas não nas colunas → Verificar mapeamento';
PRINT '  2. Se dados não estão no metadata → API não retorna (comportamento normal)';
PRINT '  3. Se IDs não estão em dim_usuarios → Re-executar extração de usuarios_sistema';
PRINT '  4. Atualizar view PowerBI executando: database/views/013_criar_view_coletas_powerbi.sql';
PRINT '';
