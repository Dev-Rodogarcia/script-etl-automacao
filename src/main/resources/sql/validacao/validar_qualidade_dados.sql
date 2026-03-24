-- ==============================================================================
-- VALIDAÇÃO DE QUALIDADE DOS DADOS (últimas 24h)
-- Versão simplificada para JDBC
-- ==============================================================================

-- Registros com campos críticos NULL
SELECT 
    'cotacoes' AS tabela,
    'requested_at' AS campo,
    COUNT(*) AS total_null
FROM cotacoes
WHERE requested_at IS NULL AND data_extracao >= DATEADD(HOUR, -24, GETDATE())
UNION ALL
SELECT 'manifestos', 'created_at', COUNT(*)
FROM manifestos WHERE created_at IS NULL AND data_extracao >= DATEADD(HOUR, -24, GETDATE())
UNION ALL
SELECT 'manifestos', 'status', COUNT(*)
FROM manifestos WHERE status IS NULL AND data_extracao >= DATEADD(HOUR, -24, GETDATE())
UNION ALL
SELECT 'fretes', 'criado_em', COUNT(*)
FROM fretes WHERE criado_em IS NULL AND data_extracao >= DATEADD(HOUR, -24, GETDATE())
UNION ALL
SELECT 'fretes', 'status', COUNT(*)
FROM fretes WHERE status IS NULL AND data_extracao >= DATEADD(HOUR, -24, GETDATE());

-- Metadata NULL (problema grave - perdemos backup do JSON)
SELECT 
    'cotacoes' AS tabela,
    COUNT(*) AS total_metadata_null
FROM cotacoes
WHERE metadata IS NULL AND data_extracao >= DATEADD(HOUR, -24, GETDATE())
UNION ALL
SELECT 'manifestos', COUNT(*)
FROM manifestos WHERE metadata IS NULL AND data_extracao >= DATEADD(HOUR, -24, GETDATE())
UNION ALL
SELECT 'fretes', COUNT(*)
FROM fretes WHERE metadata IS NULL AND data_extracao >= DATEADD(HOUR, -24, GETDATE())
UNION ALL
SELECT 'contas_a_pagar', COUNT(*)
FROM contas_a_pagar WHERE metadata IS NULL AND data_extracao >= DATEADD(HOUR, -24, GETDATE());

-- data_extracao NULL (problema crítico)
SELECT 
    'cotacoes' AS tabela,
    COUNT(*) AS total_data_extracao_null
FROM cotacoes WHERE data_extracao IS NULL
UNION ALL
SELECT 'manifestos', COUNT(*) FROM manifestos WHERE data_extracao IS NULL
UNION ALL
SELECT 'fretes', COUNT(*) FROM fretes WHERE data_extracao IS NULL
UNION ALL
SELECT 'contas_a_pagar', COUNT(*) FROM contas_a_pagar WHERE data_extracao IS NULL;
