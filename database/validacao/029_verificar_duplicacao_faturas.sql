-- ============================================================================
-- Script de Diagnóstico: Verificação de Duplicação em Faturas
-- Data: 15/01/2026
-- Objetivo: Verificar se há duplicações de Fatura/Emissão Fatura nas tabelas
--           faturas_graphql e faturas_por_cliente
-- ============================================================================

PRINT '============================================================================';
PRINT 'VERIFICAÇÃO DE DUPLICAÇÃO EM FATURAS';
PRINT '============================================================================';
PRINT '';

-- ============================================================================
-- 1. ESTATÍSTICAS GERAIS
-- ============================================================================

PRINT '1. ESTATÍSTICAS GERAIS';
PRINT '----------------------------------------';

-- Total de registros em cada tabela
SELECT 
    'faturas_graphql' AS tabela,
    COUNT(*) AS total_registros,
    COUNT(DISTINCT id) AS ids_unicos,
    COUNT(*) - COUNT(DISTINCT id) AS ids_duplicados
FROM dbo.faturas_graphql

UNION ALL

SELECT 
    'faturas_por_cliente' AS tabela,
    COUNT(*) AS total_registros,
    COUNT(DISTINCT unique_id) AS ids_unicos,
    COUNT(*) - COUNT(DISTINCT unique_id) AS ids_duplicados
FROM dbo.faturas_por_cliente;

PRINT '';

-- ============================================================================
-- 2. DUPLICAÇÕES EM FATURAS_GRAPHQL
-- ============================================================================

PRINT '2. DUPLICAÇÕES EM FATURAS_GRAPHQL';
PRINT '----------------------------------------';

-- 2.1. Duplicações por document + issue_date
PRINT '2.1. Duplicações por document + issue_date:';
SELECT 
    document,
    issue_date,
    COUNT(*) AS quantidade,
    STUFF((
        SELECT ', ' + CAST(id AS VARCHAR)
        FROM dbo.faturas_graphql fg2
        WHERE fg2.document = fg.document 
          AND fg2.issue_date = fg.issue_date
        FOR XML PATH(''), TYPE
    ).value('.', 'NVARCHAR(MAX)'), 1, 2, '') AS ids_duplicados
FROM dbo.faturas_graphql fg
WHERE document IS NOT NULL 
  AND issue_date IS NOT NULL
GROUP BY document, issue_date
HAVING COUNT(*) > 1
ORDER BY quantidade DESC, document;

PRINT '';

-- 2.2. Duplicações por document (sem considerar data)
PRINT '2.2. Duplicações por document (sem considerar data):';
SELECT 
    document,
    COUNT(*) AS quantidade,
    COUNT(DISTINCT issue_date) AS datas_diferentes,
    STUFF((
        SELECT ', ' + CAST(id AS VARCHAR)
        FROM dbo.faturas_graphql fg2
        WHERE fg2.document = fg.document
        FOR XML PATH(''), TYPE
    ).value('.', 'NVARCHAR(MAX)'), 1, 2, '') AS ids_duplicados
FROM dbo.faturas_graphql fg
WHERE document IS NOT NULL
GROUP BY document
HAVING COUNT(*) > 1
ORDER BY quantidade DESC;

PRINT '';

-- 2.3. Exemplos de registros duplicados (primeiros 10)
PRINT '2.3. Exemplos de registros duplicados (primeiros 10):';
WITH Duplicados AS (
    SELECT 
        document,
        issue_date,
        COUNT(*) AS quantidade
    FROM dbo.faturas_graphql
    WHERE document IS NOT NULL 
      AND issue_date IS NOT NULL
    GROUP BY document, issue_date
    HAVING COUNT(*) > 1
)
SELECT TOP 10
    fg.id,
    fg.document,
    fg.issue_date,
    fg.due_date,
    fg.value,
    fg.corporation_name,
    fg.data_extracao
FROM dbo.faturas_graphql fg
INNER JOIN Duplicados d ON fg.document = d.document AND fg.issue_date = d.issue_date
ORDER BY fg.document, fg.issue_date, fg.id;

PRINT '';

-- ============================================================================
-- 3. DUPLICAÇÕES EM FATURAS_POR_CLIENTE
-- ============================================================================

PRINT '3. DUPLICAÇÕES EM FATURAS_POR_CLIENTE';
PRINT '----------------------------------------';

-- 3.1. Duplicações por numero_fatura + data_emissao_fatura
PRINT '3.1. Duplicações por numero_fatura + data_emissao_fatura:';
SELECT 
    numero_fatura,
    data_emissao_fatura,
    COUNT(*) AS quantidade,
    STUFF((
        SELECT ', ' + unique_id
        FROM dbo.faturas_por_cliente fpc2
        WHERE fpc2.numero_fatura = fpc.numero_fatura 
          AND fpc2.data_emissao_fatura = fpc.data_emissao_fatura
        FOR XML PATH(''), TYPE
    ).value('.', 'NVARCHAR(MAX)'), 1, 2, '') AS unique_ids_duplicados
FROM dbo.faturas_por_cliente fpc
WHERE numero_fatura IS NOT NULL 
  AND data_emissao_fatura IS NOT NULL
GROUP BY numero_fatura, data_emissao_fatura
HAVING COUNT(*) > 1
ORDER BY quantidade DESC, numero_fatura;

PRINT '';

-- 3.2. Duplicações por fit_ant_document + fit_ant_issue_date
PRINT '3.2. Duplicações por fit_ant_document + fit_ant_issue_date:';
SELECT 
    fit_ant_document,
    fit_ant_issue_date,
    COUNT(*) AS quantidade,
    STUFF((
        SELECT ', ' + unique_id
        FROM dbo.faturas_por_cliente fpc2
        WHERE fpc2.fit_ant_document = fpc.fit_ant_document 
          AND fpc2.fit_ant_issue_date = fpc.fit_ant_issue_date
        FOR XML PATH(''), TYPE
    ).value('.', 'NVARCHAR(MAX)'), 1, 2, '') AS unique_ids_duplicados
FROM dbo.faturas_por_cliente fpc
WHERE fit_ant_document IS NOT NULL 
  AND fit_ant_issue_date IS NOT NULL
GROUP BY fit_ant_document, fit_ant_issue_date
HAVING COUNT(*) > 1
ORDER BY quantidade DESC, fit_ant_document;

PRINT '';

-- 3.3. Duplicações por numero_fatura (sem considerar data)
PRINT '3.3. Duplicações por numero_fatura (sem considerar data):';
SELECT 
    numero_fatura,
    COUNT(*) AS quantidade,
    COUNT(DISTINCT data_emissao_fatura) AS datas_diferentes,
    STUFF((
        SELECT ', ' + unique_id
        FROM dbo.faturas_por_cliente fpc2
        WHERE fpc2.numero_fatura = fpc.numero_fatura
        FOR XML PATH(''), TYPE
    ).value('.', 'NVARCHAR(MAX)'), 1, 2, '') AS unique_ids_duplicados
FROM dbo.faturas_por_cliente fpc
WHERE numero_fatura IS NOT NULL
GROUP BY numero_fatura
HAVING COUNT(*) > 1
ORDER BY quantidade DESC;

PRINT '';

-- 3.4. Duplicações por fit_ant_document (sem considerar data)
PRINT '3.4. Duplicações por fit_ant_document (sem considerar data):';
SELECT 
    fit_ant_document,
    COUNT(*) AS quantidade,
    COUNT(DISTINCT fit_ant_issue_date) AS datas_diferentes,
    STUFF((
        SELECT ', ' + unique_id
        FROM dbo.faturas_por_cliente fpc2
        WHERE fpc2.fit_ant_document = fpc.fit_ant_document
        FOR XML PATH(''), TYPE
    ).value('.', 'NVARCHAR(MAX)'), 1, 2, '') AS unique_ids_duplicados
FROM dbo.faturas_por_cliente fpc
WHERE fit_ant_document IS NOT NULL
GROUP BY fit_ant_document
HAVING COUNT(*) > 1
ORDER BY quantidade DESC;

PRINT '';

-- 3.5. Exemplos de registros duplicados (primeiros 10)
PRINT '3.5. Exemplos de registros duplicados por numero_fatura + data_emissao_fatura (primeiros 10):';
WITH Duplicados AS (
    SELECT 
        numero_fatura,
        data_emissao_fatura,
        COUNT(*) AS quantidade
    FROM dbo.faturas_por_cliente
    WHERE numero_fatura IS NOT NULL 
      AND data_emissao_fatura IS NOT NULL
    GROUP BY numero_fatura, data_emissao_fatura
    HAVING COUNT(*) > 1
)
SELECT TOP 10
    fpc.unique_id,
    fpc.numero_fatura,
    fpc.data_emissao_fatura,
    fpc.data_vencimento_fatura,
    fpc.valor_fatura,
    fpc.filial,
    fpc.pagador_nome,
    fpc.data_extracao
FROM dbo.faturas_por_cliente fpc
INNER JOIN Duplicados d ON fpc.numero_fatura = d.numero_fatura 
                        AND fpc.data_emissao_fatura = d.data_emissao_fatura
ORDER BY fpc.numero_fatura, fpc.data_emissao_fatura, fpc.unique_id;

PRINT '';

-- ============================================================================
-- 4. ANÁLISE CRUZADA: FATURAS QUE APARECEM EM AMBAS AS TABELAS
-- ============================================================================

PRINT '4. ANÁLISE CRUZADA: FATURAS QUE APARECEM EM AMBAS AS TABELAS';
PRINT '----------------------------------------';

-- 4.1. Faturas GraphQL que também aparecem em Faturas por Cliente (por document)
PRINT '4.1. Faturas GraphQL que também aparecem em Faturas por Cliente (por document):';
SELECT 
    fg.document,
    fg.issue_date,
    COUNT(DISTINCT fg.id) AS qtd_graphql,
    COUNT(DISTINCT fpc.unique_id) AS qtd_por_cliente,
    STUFF((
        SELECT DISTINCT ', ' + CAST(fg2.id AS VARCHAR)
        FROM dbo.faturas_graphql fg2
        INNER JOIN dbo.faturas_por_cliente fpc2 
            ON fg2.document = fpc2.numero_fatura 
            AND fg2.issue_date = fpc2.data_emissao_fatura
        WHERE fg2.document = fg.document 
          AND fg2.issue_date = fg.issue_date
        FOR XML PATH(''), TYPE
    ).value('.', 'NVARCHAR(MAX)'), 1, 2, '') AS ids_graphql,
    STUFF((
        SELECT DISTINCT ', ' + fpc2.unique_id
        FROM dbo.faturas_por_cliente fpc2
        INNER JOIN dbo.faturas_graphql fg2 
            ON fg2.document = fpc2.numero_fatura 
            AND fg2.issue_date = fpc2.data_emissao_fatura
        WHERE fpc2.numero_fatura = fg.document 
          AND fpc2.data_emissao_fatura = fg.issue_date
        FOR XML PATH(''), TYPE
    ).value('.', 'NVARCHAR(MAX)'), 1, 2, '') AS unique_ids_cliente
FROM dbo.faturas_graphql fg
INNER JOIN dbo.faturas_por_cliente fpc 
    ON fg.document = fpc.numero_fatura 
    AND fg.issue_date = fpc.data_emissao_fatura
WHERE fg.document IS NOT NULL 
  AND fg.issue_date IS NOT NULL
GROUP BY fg.document, fg.issue_date
ORDER BY qtd_graphql DESC, qtd_por_cliente DESC;

PRINT '';

-- 4.2. Faturas GraphQL que também aparecem em Faturas por Cliente (por fit_ant_document)
PRINT '4.2. Faturas GraphQL que também aparecem em Faturas por Cliente (por fit_ant_document):';
SELECT 
    fg.document AS document_graphql,
    fpc.fit_ant_document AS document_cliente,
    fg.issue_date AS issue_date_graphql,
    fpc.fit_ant_issue_date AS issue_date_cliente,
    COUNT(DISTINCT fg.id) AS qtd_graphql,
    COUNT(DISTINCT fpc.unique_id) AS qtd_por_cliente,
    STUFF((
        SELECT DISTINCT ', ' + CAST(fg2.id AS VARCHAR)
        FROM dbo.faturas_graphql fg2
        INNER JOIN dbo.faturas_por_cliente fpc2 
            ON fg2.document = fpc2.fit_ant_document 
            AND fg2.issue_date = fpc2.fit_ant_issue_date
        WHERE fg2.document = fg.document 
          AND fg2.issue_date = fg.issue_date
          AND fpc2.fit_ant_document = fpc.fit_ant_document
          AND fpc2.fit_ant_issue_date = fpc.fit_ant_issue_date
        FOR XML PATH(''), TYPE
    ).value('.', 'NVARCHAR(MAX)'), 1, 2, '') AS ids_graphql,
    STUFF((
        SELECT DISTINCT ', ' + fpc2.unique_id
        FROM dbo.faturas_por_cliente fpc2
        INNER JOIN dbo.faturas_graphql fg2 
            ON fg2.document = fpc2.fit_ant_document 
            AND fg2.issue_date = fpc2.fit_ant_issue_date
        WHERE fpc2.fit_ant_document = fg.document 
          AND fpc2.fit_ant_issue_date = fg.issue_date
        FOR XML PATH(''), TYPE
    ).value('.', 'NVARCHAR(MAX)'), 1, 2, '') AS unique_ids_cliente
FROM dbo.faturas_graphql fg
INNER JOIN dbo.faturas_por_cliente fpc 
    ON fg.document = fpc.fit_ant_document 
    AND fg.issue_date = fpc.fit_ant_issue_date
WHERE fg.document IS NOT NULL 
  AND fg.issue_date IS NOT NULL
  AND fpc.fit_ant_document IS NOT NULL
  AND fpc.fit_ant_issue_date IS NOT NULL
GROUP BY fg.document, fpc.fit_ant_document, fg.issue_date, fpc.fit_ant_issue_date
ORDER BY qtd_graphql DESC, qtd_por_cliente DESC;

PRINT '';

-- ============================================================================
-- 5. RESUMO FINAL
-- ============================================================================

PRINT '5. RESUMO FINAL';
PRINT '----------------------------------------';

-- Total de duplicações encontradas
SELECT 
    'faturas_graphql (document + issue_date)' AS tipo_duplicacao,
    COUNT(*) AS total_grupos_duplicados,
    SUM(quantidade) AS total_registros_duplicados
FROM (
    SELECT 
        document,
        issue_date,
        COUNT(*) AS quantidade
    FROM dbo.faturas_graphql
    WHERE document IS NOT NULL 
      AND issue_date IS NOT NULL
    GROUP BY document, issue_date
    HAVING COUNT(*) > 1
) AS dup

UNION ALL

SELECT 
    'faturas_por_cliente (numero_fatura + data_emissao_fatura)' AS tipo_duplicacao,
    COUNT(*) AS total_grupos_duplicados,
    SUM(quantidade) AS total_registros_duplicados
FROM (
    SELECT 
        numero_fatura,
        data_emissao_fatura,
        COUNT(*) AS quantidade
    FROM dbo.faturas_por_cliente
    WHERE numero_fatura IS NOT NULL 
      AND data_emissao_fatura IS NOT NULL
    GROUP BY numero_fatura, data_emissao_fatura
    HAVING COUNT(*) > 1
) AS dup

UNION ALL

SELECT 
    'faturas_por_cliente (fit_ant_document + fit_ant_issue_date)' AS tipo_duplicacao,
    COUNT(*) AS total_grupos_duplicados,
    SUM(quantidade) AS total_registros_duplicados
FROM (
    SELECT 
        fit_ant_document,
        fit_ant_issue_date,
        COUNT(*) AS quantidade
    FROM dbo.faturas_por_cliente
    WHERE fit_ant_document IS NOT NULL 
      AND fit_ant_issue_date IS NOT NULL
    GROUP BY fit_ant_document, fit_ant_issue_date
    HAVING COUNT(*) > 1
) AS dup;

PRINT '';
PRINT '============================================================================';
PRINT 'FIM DA VERIFICAÇÃO';
PRINT '============================================================================';

-- ============================================================================
-- INTERPRETAÇÃO DOS RESULTADOS
-- ============================================================================
/*
INSTRUÇÕES PARA INTERPRETAÇÃO:

1. ESTATÍSTICAS GERAIS:
   - Se "ids_duplicados" > 0, há IDs duplicados (problema crítico - chave primária duplicada)
   - Isso NÃO deveria acontecer, pois id e unique_id são chaves primárias

2. DUPLICAÇÕES EM FATURAS_GRAPHQL:
   - Se houver registros na seção 2.1, significa que a mesma fatura (document + issue_date) 
     aparece múltiplas vezes com IDs diferentes
   - Isso pode ser normal se a API retornar a mesma fatura em diferentes extrações
   - O MERGE (UPSERT) deveria resolver isso, mas pode indicar problema na lógica de deduplicação

3. DUPLICAÇÕES EM FATURAS_POR_CLIENTE:
   - Se houver registros na seção 3.1 ou 3.2, significa que a mesma fatura aparece 
     múltiplas vezes com unique_ids diferentes
   - Isso pode ser normal se a API retornar a mesma fatura em diferentes extrações
   - O MERGE (UPSERT) deveria resolver isso, mas pode indicar problema na lógica de deduplicação

4. ANÁLISE CRUZADA:
   - Mostra faturas que aparecem em ambas as tabelas
   - Se qtd_graphql > 1 ou qtd_por_cliente > 1, há duplicação dentro da própria tabela

5. AÇÕES RECOMENDADAS:
   - Se houver duplicações significativas, revisar a lógica de deduplicação no código Java
   - Verificar se o MERGE está funcionando corretamente
   - Considerar adicionar índices únicos se necessário (após análise)
*/
