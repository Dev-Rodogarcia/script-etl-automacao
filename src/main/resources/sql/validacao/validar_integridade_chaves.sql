-- ==============================================================================
-- VALIDAÇÃO DE INTEGRIDADE DE CHAVES E DUPLICADOS
-- Versão simplificada para JDBC
-- ==============================================================================

-- Duplicados por sequence_code em Cotações
SELECT 
    'cotacoes' AS tabela,
    sequence_code,
    COUNT(*) AS quantidade
FROM cotacoes
GROUP BY sequence_code
HAVING COUNT(*) > 1
ORDER BY quantidade DESC;

-- Duplicados por sequence_code em Contas a Pagar
SELECT 
    'contas_a_pagar' AS tabela,
    sequence_code,
    COUNT(*) AS quantidade
FROM contas_a_pagar
GROUP BY sequence_code
HAVING COUNT(*) > 1
ORDER BY quantidade DESC;

-- Manifestos - Duplicados na chave composta (não deveria acontecer)
SELECT 
    'manifestos_chave_composta' AS tabela,
    sequence_code,
    ISNULL(CAST(pick_sequence_code AS VARCHAR(50)), '-1') AS pick_sequence_code,
    ISNULL(CAST(mdfe_number AS VARCHAR(50)), '-1') AS mdfe_number,
    COUNT(*) AS quantidade
FROM manifestos
GROUP BY
    sequence_code,
    ISNULL(CAST(pick_sequence_code AS VARCHAR(50)), '-1'),
    ISNULL(CAST(mdfe_number AS VARCHAR(50)), '-1')
HAVING COUNT(*) > 1
ORDER BY quantidade DESC;

-- Registros com chaves NULL
SELECT 'cotacoes' AS tabela, COUNT(*) AS total_com_sequence_code_null
FROM cotacoes WHERE sequence_code IS NULL
UNION ALL
SELECT 'manifestos', COUNT(*) FROM manifestos WHERE sequence_code IS NULL
UNION ALL
SELECT 'contas_a_pagar', COUNT(*) FROM contas_a_pagar WHERE sequence_code IS NULL;
