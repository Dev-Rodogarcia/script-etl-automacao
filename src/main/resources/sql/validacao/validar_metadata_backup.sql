-- ==============================================================================
-- VALIDAÇÃO DE METADATA (BACKUP JSON) - Últimas 24h
-- Versão simplificada para JDBC
-- ==============================================================================

SELECT 
    'cotacoes' AS tabela,
    COUNT(*) AS total_registros,
    SUM(CASE WHEN metadata IS NOT NULL AND LEN(metadata) > 0 THEN 1 ELSE 0 END) AS com_metadata,
    CAST(SUM(CASE WHEN metadata IS NOT NULL AND LEN(metadata) > 0 THEN 1 ELSE 0 END) * 100.0 / COUNT(*) AS DECIMAL(5,2)) AS percentual
FROM cotacoes
WHERE data_extracao >= DATEADD(HOUR, -24, GETDATE())
UNION ALL
SELECT 
    'manifestos',
    COUNT(*),
    SUM(CASE WHEN metadata IS NOT NULL AND LEN(metadata) > 0 THEN 1 ELSE 0 END),
    CAST(SUM(CASE WHEN metadata IS NOT NULL AND LEN(metadata) > 0 THEN 1 ELSE 0 END) * 100.0 / COUNT(*) AS DECIMAL(5,2))
FROM manifestos
WHERE data_extracao >= DATEADD(HOUR, -24, GETDATE())
UNION ALL
SELECT 
    'fretes',
    COUNT(*),
    SUM(CASE WHEN metadata IS NOT NULL AND LEN(metadata) > 0 THEN 1 ELSE 0 END),
    CAST(SUM(CASE WHEN metadata IS NOT NULL AND LEN(metadata) > 0 THEN 1 ELSE 0 END) * 100.0 / COUNT(*) AS DECIMAL(5,2))
FROM fretes
WHERE data_extracao >= DATEADD(HOUR, -24, GETDATE())
UNION ALL
SELECT 
    'contas_a_pagar',
    COUNT(*),
    SUM(CASE WHEN metadata IS NOT NULL AND LEN(metadata) > 0 THEN 1 ELSE 0 END),
    CAST(SUM(CASE WHEN metadata IS NOT NULL AND LEN(metadata) > 0 THEN 1 ELSE 0 END) * 100.0 / COUNT(*) AS DECIMAL(5,2))
FROM contas_a_pagar
WHERE data_extracao >= DATEADD(HOUR, -24, GETDATE());
