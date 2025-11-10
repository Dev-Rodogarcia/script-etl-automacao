-- ==============================================================================
-- VALIDAÇÃO RÁPIDA DE MANIFESTOS
-- Compara registros extraídos vs salvos
-- ==============================================================================

USE esl_cloud;

PRINT '===============================================================================';
PRINT '                    VALIDAÇÃO DE MANIFESTOS';
PRINT '===============================================================================';
PRINT '';

-- 1. ÚLTIMA EXTRAÇÃO
PRINT '📋 ÚLTIMA EXTRAÇÃO:';
SELECT TOP 1
    data_extracao,
    registros_extraidos,
    registros_salvos,
    status
FROM log_extracoes
WHERE tipo_extracao = 'manifestos'
ORDER BY data_extracao DESC;

PRINT '';

-- 2. CONTAGEM NO BANCO
PRINT '📊 CONTAGEM NO BANCO:';
DECLARE @TotalBanco INT;
SELECT @TotalBanco = COUNT(*) FROM manifestos;
PRINT 'Total de registros na tabela manifestos: ' + CAST(@TotalBanco AS NVARCHAR(20));

PRINT '';

-- 3. COMPARAÇÃO
PRINT '🔍 COMPARAÇÃO:';
DECLARE @Extraidos INT, @Salvos INT;
SELECT TOP 1
    @Extraidos = registros_extraidos,
    @Salvos = registros_salvos
FROM log_extracoes
WHERE tipo_extracao = 'manifestos'
ORDER BY data_extracao DESC;

IF @Extraidos IS NOT NULL
BEGIN
    PRINT 'Registros extraídos (API): ' + CAST(@Extraidos AS NVARCHAR(20));
    PRINT 'Registros salvos (log): ' + CAST(@Salvos AS NVARCHAR(20));
    PRINT 'Registros no banco: ' + CAST(@TotalBanco AS NVARCHAR(20));
    PRINT '';
    
    DECLARE @Diff INT = @Extraidos - @TotalBanco;
    IF @Diff = 0
        PRINT '✅ OK - Números coincidem!';
    ELSE IF @Diff > 0
        PRINT '❌ PROBLEMA - Faltam ' + CAST(@Diff AS NVARCHAR(20)) + ' registros no banco!';
    ELSE
        PRINT '⚠️ ATENÇÃO - Há ' + CAST(ABS(@Diff) AS NVARCHAR(20)) + ' registros a mais no banco!';
END

PRINT '';

-- 4. DUPLICADOS
PRINT '🔍 DUPLICADOS (por sequence_code):';
SELECT 
    sequence_code,
    COUNT(*) as quantidade
FROM manifestos
GROUP BY sequence_code
HAVING COUNT(*) > 1
ORDER BY quantidade DESC;

PRINT '';

