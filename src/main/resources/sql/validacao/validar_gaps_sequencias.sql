-- ==============================================================================
-- VALIDAÇÃO DE GAPS (BURACOS) EM SEQUÊNCIAS
-- Identifica saltos grandes em sequence_code (indicam possíveis falhas)
-- Versão simplificada para JDBC
-- ==============================================================================

-- GAPS em Cotações (TOP 20 gaps maiores)
SELECT TOP 20
    'cotacoes' AS tabela,
    sequence_code AS atual,
    LAG(sequence_code) OVER (ORDER BY sequence_code) AS anterior,
    sequence_code - LAG(sequence_code) OVER (ORDER BY sequence_code) AS diferenca,
    CASE 
        WHEN sequence_code - LAG(sequence_code) OVER (ORDER BY sequence_code) > 10 
        THEN 'GAP_GRANDE'
        WHEN sequence_code - LAG(sequence_code) OVER (ORDER BY sequence_code) > 1 
        THEN 'Gap_pequeno'
        ELSE 'OK'
    END AS status
FROM cotacoes
WHERE requested_at >= DATEADD(DAY, -30, GETDATE())
ORDER BY diferenca DESC;
