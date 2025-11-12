-- ================================================================
-- Script: validar-correcao-identificador-unico.sql
-- Finalidade: Validar que a correção do identificador único funcionou
-- 
-- Execute este script APÓS:
-- 1. Aplicar a correção no código
-- 2. Executar nova extração completa
-- 3. Limpar duplicados falsos (se necessário)
-- ================================================================

-- ================================================================
-- TESTE 1: Verificar se ainda há duplicados falsos
-- ================================================================
PRINT '========================================';
PRINT 'TESTE 1: Verificar duplicados falsos';
PRINT '========================================';

SELECT 
    sequence_code,
    COUNT(*) as total
FROM manifestos
WHERE pick_sequence_code IS NULL
GROUP BY sequence_code
HAVING COUNT(*) > 1;

-- Resultado esperado: 0 linhas (sem duplicados falsos)

-- ================================================================
-- TESTE 2: Verificar identificadores únicos NULL
-- ================================================================
PRINT '';
PRINT '========================================';
PRINT 'TESTE 2: Verificar identificadores NULL';
PRINT '========================================';

SELECT 
    COUNT(*) as total_com_identificador_null
FROM manifestos
WHERE identificador_unico IS NULL;

-- Resultado esperado: 0 (todos devem ter identificador_unico)

-- ================================================================
-- TESTE 3: Comparar contagem com log_extracoes
-- ================================================================
PRINT '';
PRINT '========================================';
PRINT 'TESTE 3: Comparar com log_extracoes';
PRINT '========================================';

SELECT 
    'Log Extrações' as origem,
    entidade,
    registros_extraidos as registros,
    timestamp_fim as data
FROM log_extracoes
WHERE entidade = 'manifestos'
AND timestamp_fim >= DATEADD(HOUR, -1, GETDATE())
ORDER BY timestamp_fim DESC;

SELECT 
    'Banco de Dados' as origem,
    'manifestos' as entidade,
    COUNT(*) as registros,
    MAX(data_extracao) as data
FROM manifestos
WHERE data_extracao >= DATEADD(HOUR, -1, GETDATE());

-- Resultado esperado: Números devem ser próximos (diferença pode ser UPDATEs)

-- ================================================================
-- TESTE 4: Verificar distribuição de pick_sequence_code
-- ================================================================
PRINT '';
PRINT '========================================';
PRINT 'TESTE 4: Distribuição de pick_sequence_code';
PRINT '========================================';

SELECT 
    CASE 
        WHEN pick_sequence_code IS NOT NULL THEN 'Com pick_sequence_code'
        ELSE 'Sem pick_sequence_code (usa hash)'
    END as tipo,
    COUNT(*) as total,
    CAST(COUNT(*) * 100.0 / (SELECT COUNT(*) FROM manifestos) AS DECIMAL(5,2)) as percentual
FROM manifestos
GROUP BY 
    CASE 
        WHEN pick_sequence_code IS NOT NULL THEN 'Com pick_sequence_code'
        ELSE 'Sem pick_sequence_code (usa hash)'
    END;

-- ================================================================
-- TESTE 5: Verificar integridade de chave composta
-- ================================================================
PRINT '';
PRINT '========================================';
PRINT 'TESTE 5: Integridade de chave composta';
PRINT '========================================';

SELECT 
    sequence_code,
    identificador_unico,
    COUNT(*) as total
FROM manifestos
GROUP BY sequence_code, identificador_unico
HAVING COUNT(*) > 1;

-- Resultado esperado: 0 linhas (chave composta deve ser única)

-- ================================================================
-- RESUMO FINAL
-- ================================================================
PRINT '';
PRINT '========================================';
PRINT 'RESUMO FINAL';
PRINT '========================================';

SELECT 
    'Total de manifestos' as metrica,
    COUNT(*) as valor
FROM manifestos
UNION ALL
SELECT 
    'Com pick_sequence_code' as metrica,
    COUNT(*) as valor
FROM manifestos
WHERE pick_sequence_code IS NOT NULL
UNION ALL
SELECT 
    'Sem pick_sequence_code (usa hash)' as metrica,
    COUNT(*) as valor
FROM manifestos
WHERE pick_sequence_code IS NULL
UNION ALL
SELECT 
    'Com identificador_unico NULL' as metrica,
    COUNT(*) as valor
FROM manifestos
WHERE identificador_unico IS NULL
UNION ALL
SELECT 
    'Duplicados falsos (mesmo sequence_code, sem pick)' as metrica,
    COUNT(*) as valor
FROM (
    SELECT sequence_code
    FROM manifestos
    WHERE pick_sequence_code IS NULL
    GROUP BY sequence_code
    HAVING COUNT(*) > 1
) as duplicados;

