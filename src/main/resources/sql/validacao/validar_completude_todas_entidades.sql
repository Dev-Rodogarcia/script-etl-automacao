-- ==============================================================================
-- VALIDAÇÃO DE COMPLETUDE - TODAS AS ENTIDADES
-- Compara registros extraídos (API) vs salvos (Banco)
-- Versão simplificada para JDBC (sem PRINT, GO, CURSOR)
-- ==============================================================================

WITH LogUltimasExtracoes AS (
    SELECT 
        entidade,
        registros_extraidos,
        timestamp_fim,
        ROW_NUMBER() OVER (PARTITION BY entidade ORDER BY timestamp_fim DESC) AS rn
    FROM log_extracoes
),
ContagensBanco AS (
    SELECT 'usuarios_sistema' AS entidade, COUNT(*) AS registros_banco_24h
    FROM dim_usuarios
    UNION ALL
    SELECT 'coletas', COUNT(*) FROM coletas WHERE data_extracao >= DATEADD(HOUR, -24, GETDATE())
    UNION ALL
    SELECT 'fretes', COUNT(*) FROM fretes WHERE data_extracao >= DATEADD(HOUR, -24, GETDATE())
    UNION ALL
    SELECT 'manifestos', COUNT(*) FROM manifestos WHERE data_extracao >= DATEADD(HOUR, -24, GETDATE())
    UNION ALL
    SELECT 'cotacoes', COUNT(*) FROM cotacoes WHERE data_extracao >= DATEADD(HOUR, -24, GETDATE())
    UNION ALL
    SELECT 'localizacao_cargas', COUNT(*) FROM localizacao_cargas WHERE data_extracao >= DATEADD(HOUR, -24, GETDATE())
    UNION ALL
    SELECT 'contas_a_pagar', COUNT(*) FROM contas_a_pagar WHERE data_extracao >= DATEADD(HOUR, -24, GETDATE())
    UNION ALL
    SELECT 'faturas_por_cliente', COUNT(*) FROM faturas_por_cliente WHERE data_extracao >= DATEADD(HOUR, -24, GETDATE())
    UNION ALL
    SELECT 'faturas_graphql', COUNT(*) FROM faturas_graphql WHERE data_extracao >= DATEADD(HOUR, -24, GETDATE())
)
SELECT 
    COALESCE(l.entidade, b.entidade) AS entidade,
    l.registros_extraidos AS registros_api,
    b.registros_banco_24h AS registros_banco,
    l.registros_extraidos - b.registros_banco_24h AS diferenca,
    CASE
        WHEN l.registros_extraidos IS NULL THEN 'SEM_LOG'
        WHEN l.registros_extraidos - b.registros_banco_24h = 0 THEN 'OK'
        WHEN l.registros_extraidos - b.registros_banco_24h > 0 AND l.registros_extraidos - b.registros_banco_24h <= 5 THEN 'DIFERENCA_PEQUENA'
        WHEN l.registros_extraidos - b.registros_banco_24h > 5 THEN 'DIFERENCA_GRANDE'
        WHEN l.registros_extraidos - b.registros_banco_24h < 0 THEN 'MAIS_NO_BANCO'
        ELSE 'ERRO'
    END AS status,
    CONVERT(VARCHAR, l.timestamp_fim, 120) AS ultima_extracao
FROM LogUltimasExtracoes l
FULL OUTER JOIN ContagensBanco b ON l.entidade = b.entidade
WHERE l.rn = 1 OR l.entidade IS NULL
ORDER BY 
    CASE
        WHEN l.registros_extraidos IS NULL THEN 5
        WHEN l.registros_extraidos - b.registros_banco_24h = 0 THEN 1
        WHEN l.registros_extraidos - b.registros_banco_24h > 0 AND l.registros_extraidos - b.registros_banco_24h <= 5 THEN 2
        WHEN l.registros_extraidos - b.registros_banco_24h > 5 THEN 3
        WHEN l.registros_extraidos - b.registros_banco_24h < 0 THEN 4
        ELSE 6
    END,
    COALESCE(l.entidade, b.entidade);
