-- ==============================================================================
-- VALIDAÇÃO DIÁRIA ESL CLOUD EXTRACTOR
-- Script SQL para validar integridade dos dados extraídos
-- ==============================================================================

USE esl_cloud;

PRINT '===============================================================================';
PRINT '                    VALIDAÇÃO DIÁRIA - ESL CLOUD EXTRACTOR';
PRINT '                           ' + CONVERT(VARCHAR, GETDATE(), 120);
PRINT '===============================================================================';
PRINT '';

-- ==============================================================================
-- 1. RESUMO GERAL DOS DADOS
-- ==============================================================================

PRINT '📊 RESUMO GERAL DOS DADOS:';
PRINT '';

SELECT 
    'cotacoes' as tabela,
    COUNT(*) as total_registros,
    COUNT(DISTINCT data_referencia) as dias_distintos,
    MIN(data_referencia) as data_mais_antiga,
    MAX(data_referencia) as data_mais_recente
FROM cotacoes
UNION ALL
SELECT 
    'coletas' as tabela,
    COUNT(*) as total_registros,
    COUNT(DISTINCT data_referencia) as dias_distintos,
    MIN(data_referencia) as data_mais_antiga,
    MAX(data_referencia) as data_mais_recente
FROM coletas
UNION ALL
SELECT 
    'ocorrencias' as tabela,
    COUNT(*) as total_registros,
    COUNT(DISTINCT CAST(created_at AS DATE)) as dias_distintos,
    MIN(CAST(created_at AS DATE)) as data_mais_antiga,
    MAX(CAST(created_at AS DATE)) as data_mais_recente
FROM ocorrencias;

PRINT '';

-- ==============================================================================
-- 2. VALIDAÇÃO CONTRA LOG_EXTRACOES (FONTE DE VERDADE)
-- ==============================================================================

PRINT '🔍 VALIDAÇÃO CONTRA LOG_EXTRACOES:';
PRINT '';

-- Buscar última extração de cada tipo
WITH ultima_extracao AS (
    SELECT 
        tipo_extracao,
        MAX(data_extracao) as ultima_data,
        data_referencia,
        registros_extraidos
    FROM log_extracoes 
    WHERE status = 'SUCESSO'
    GROUP BY tipo_extracao, data_referencia, registros_extraidos
),
contagens_banco AS (
    SELECT 'cotacoes' as tipo, COUNT(*) as total FROM cotacoes
    UNION ALL
    SELECT 'coletas' as tipo, COUNT(*) as total FROM coletas  
    UNION ALL
    SELECT 'ocorrencias' as tipo, COUNT(*) as total FROM ocorrencias
)
SELECT 
    COALESCE(l.tipo_extracao, c.tipo) as tipo_dados,
    COALESCE(l.registros_extraidos, 0) as log_extracoes,
    COALESCE(c.total, 0) as banco_atual,
    CASE 
        WHEN l.registros_extraidos = c.total THEN '✅ OK'
        WHEN l.registros_extraidos > c.total THEN '⚠️ BANCO MENOR'
        WHEN l.registros_extraidos < c.total THEN '⚠️ BANCO MAIOR'
        ELSE '❌ SEM DADOS'
    END as status_validacao,
    ABS(COALESCE(l.registros_extraidos, 0) - COALESCE(c.total, 0)) as diferenca
FROM ultima_extracao l
FULL OUTER JOIN contagens_banco c ON l.tipo_extracao = c.tipo
ORDER BY tipo_dados;

PRINT '';

-- ==============================================================================
-- 3. DETECÇÃO DE DUPLICATAS
-- ==============================================================================

PRINT '🔍 VERIFICAÇÃO DE DUPLICATAS:';
PRINT '';

-- Cotações duplicadas
SELECT 'cotacoes' as tabela, COUNT(*) as duplicatas_encontradas
FROM (
    SELECT codigo_produto, data_referencia, COUNT(*) as qtd
    FROM cotacoes
    GROUP BY codigo_produto, data_referencia
    HAVING COUNT(*) > 1
) dup_cotacoes

UNION ALL

-- Coletas duplicadas  
SELECT 'coletas' as tabela, COUNT(*) as duplicatas_encontradas
FROM (
    SELECT codigo_produto, data_referencia, COUNT(*) as qtd
    FROM coletas
    GROUP BY codigo_produto, data_referencia
    HAVING COUNT(*) > 1
) dup_coletas

UNION ALL

-- Ocorrências duplicadas
SELECT 'ocorrencias' as tabela, COUNT(*) as duplicatas_encontradas
FROM (
    SELECT id_ocorrencia, COUNT(*) as qtd
    FROM ocorrencias
    GROUP BY id_ocorrencia
    HAVING COUNT(*) > 1
) dup_ocorrencias;

PRINT '';

-- ==============================================================================
-- 4. ANÁLISE DE VOLUME POR DIA (ÚLTIMOS 7 DIAS)
-- ==============================================================================

PRINT '📈 VOLUME DOS ÚLTIMOS 7 DIAS:';
PRINT '';

-- Cotações por dia
SELECT 
    'cotacoes' as tipo,
    data_referencia,
    COUNT(*) as registros,
    CASE 
        WHEN COUNT(*) < 50 THEN '⚠️ BAIXO'
        WHEN COUNT(*) > 500 THEN '⚠️ ALTO'
        ELSE '✅ NORMAL'
    END as status_volume
FROM cotacoes
WHERE data_referencia >= DATEADD(day, -7, GETDATE())
GROUP BY data_referencia

UNION ALL

-- Coletas por dia
SELECT 
    'coletas' as tipo,
    data_referencia,
    COUNT(*) as registros,
    CASE 
        WHEN COUNT(*) < 30 THEN '⚠️ BAIXO'
        WHEN COUNT(*) > 300 THEN '⚠️ ALTO'
        ELSE '✅ NORMAL'
    END as status_volume
FROM coletas
WHERE data_referencia >= DATEADD(day, -7, GETDATE())
GROUP BY data_referencia

UNION ALL

-- Ocorrências por dia
SELECT 
    'ocorrencias' as tipo,
    CAST(created_at AS DATE) as data_referencia,
    COUNT(*) as registros,
    CASE 
        WHEN COUNT(*) < 10 THEN '⚠️ BAIXO'
        WHEN COUNT(*) > 100 THEN '⚠️ ALTO'
        ELSE '✅ NORMAL'
    END as status_volume
FROM ocorrencias
WHERE created_at >= DATEADD(day, -7, GETDATE())
GROUP BY CAST(created_at AS DATE)

ORDER BY tipo, data_referencia DESC;

PRINT '';

-- ==============================================================================
-- 5. VERIFICAÇÃO DE DADOS INVÁLIDOS
-- ==============================================================================

PRINT '🔍 VERIFICAÇÃO DE DADOS INVÁLIDOS:';
PRINT '';

-- Cotações com problemas
SELECT 'cotacoes_preco_zero' as problema, COUNT(*) as quantidade
FROM cotacoes WHERE preco <= 0

UNION ALL

SELECT 'cotacoes_sem_codigo' as problema, COUNT(*) as quantidade  
FROM cotacoes WHERE codigo_produto IS NULL OR codigo_produto = ''

UNION ALL

-- Coletas com problemas
SELECT 'coletas_quantidade_zero' as problema, COUNT(*) as quantidade
FROM coletas WHERE quantidade <= 0

UNION ALL

SELECT 'coletas_sem_codigo' as problema, COUNT(*) as quantidade
FROM coletas WHERE codigo_produto IS NULL OR codigo_produto = ''

UNION ALL

-- Ocorrências com problemas
SELECT 'ocorrencias_sem_id' as problema, COUNT(*) as quantidade
FROM ocorrencias WHERE id_ocorrencia IS NULL

UNION ALL

SELECT 'ocorrencias_data_futura' as problema, COUNT(*) as quantidade
FROM ocorrencias WHERE created_at > GETDATE();

PRINT '';

-- ==============================================================================
-- 6. HISTÓRICO DE EXTRAÇÕES (ÚLTIMAS 10)
-- ==============================================================================

PRINT '📋 HISTÓRICO DE EXTRAÇÕES (ÚLTIMAS 10):';
PRINT '';

SELECT TOP 10
    data_extracao,
    tipo_extracao,
    data_referencia,
    registros_extraidos,
    status,
    CASE 
        WHEN status = 'SUCESSO' THEN '✅'
        WHEN status = 'ERRO' THEN '❌'
        ELSE '⚠️'
    END as icone_status
FROM log_extracoes
ORDER BY data_extracao DESC;

PRINT '';

-- ==============================================================================
-- 7. RESUMO FINAL E ALERTAS
-- ==============================================================================

PRINT '🎯 RESUMO FINAL:';
PRINT '';

DECLARE @total_problemas INT = 0;

-- Contar problemas encontrados
SELECT @total_problemas = (
    -- Duplicatas
    (SELECT COUNT(*) FROM (
        SELECT codigo_produto, data_referencia FROM cotacoes GROUP BY codigo_produto, data_referencia HAVING COUNT(*) > 1
    ) dup1) +
    (SELECT COUNT(*) FROM (
        SELECT codigo_produto, data_referencia FROM coletas GROUP BY codigo_produto, data_referencia HAVING COUNT(*) > 1  
    ) dup2) +
    (SELECT COUNT(*) FROM (
        SELECT id_ocorrencia FROM ocorrencias GROUP BY id_ocorrencia HAVING COUNT(*) > 1
    ) dup3) +
    -- Dados inválidos
    (SELECT COUNT(*) FROM cotacoes WHERE preco <= 0 OR codigo_produto IS NULL OR codigo_produto = '') +
    (SELECT COUNT(*) FROM coletas WHERE quantidade <= 0 OR codigo_produto IS NULL OR codigo_produto = '') +
    (SELECT COUNT(*) FROM ocorrencias WHERE id_ocorrencia IS NULL OR created_at > GETDATE())
);

IF @total_problemas = 0
BEGIN
    PRINT '✅ VALIDAÇÃO CONCLUÍDA: Nenhum problema encontrado!';
    PRINT '✅ Sistema operando normalmente.';
END
ELSE
BEGIN
    PRINT '⚠️ ATENÇÃO: ' + CAST(@total_problemas AS VARCHAR) + ' problema(s) encontrado(s)!';
    PRINT '⚠️ Revise os detalhes acima e considere executar limpeza de dados.';
END

PRINT '';
PRINT '===============================================================================';
PRINT 'Validação concluída em: ' + CONVERT(VARCHAR, GETDATE(), 120);
PRINT '===============================================================================';