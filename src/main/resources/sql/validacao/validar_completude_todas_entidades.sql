-- ==============================================================================
-- VALIDACAO DE COMPLETUDE - TODAS AS ENTIDADES
-- Compara o ultimo log de extracao de cada entidade contra as linhas atualizadas
-- dentro da propria janela de execucao do log, evitando falso "MAIS_NO_BANCO" em
-- tabelas acumuladas e cobrindo todas as entidades do ETL operacional.
-- ==============================================================================

WITH EntidadesEsperadas AS (
    SELECT 'usuarios_sistema' AS entidade
    UNION ALL SELECT 'coletas'
    UNION ALL SELECT 'fretes'
    UNION ALL SELECT 'faturas_graphql'
    UNION ALL SELECT 'manifestos'
    UNION ALL SELECT 'cotacoes'
    UNION ALL SELECT 'localizacao_cargas'
    UNION ALL SELECT 'contas_a_pagar'
    UNION ALL SELECT 'faturas_por_cliente'
    UNION ALL SELECT 'inventario'
    UNION ALL SELECT 'sinistros'
),
LogUltimasExtracoes AS (
    SELECT
        entidade,
        registros_extraidos,
        timestamp_inicio,
        timestamp_fim,
        ROW_NUMBER() OVER (PARTITION BY entidade ORDER BY timestamp_fim DESC) AS rn
    FROM dbo.log_extracoes
    WHERE status_final = 'COMPLETO'
),
UltimasExtracoes AS (
    SELECT
        e.entidade,
        l.registros_extraidos,
        l.timestamp_inicio,
        l.timestamp_fim
    FROM EntidadesEsperadas e
    LEFT JOIN LogUltimasExtracoes l
           ON l.entidade = e.entidade
          AND l.rn = 1
),
ContagensBanco AS (
    SELECT u.entidade, COUNT(d.user_id) AS registros_banco
    FROM UltimasExtracoes u
    LEFT JOIN dbo.dim_usuarios d
           ON u.entidade = 'usuarios_sistema'
          AND COALESCE(d.ultima_extracao_em, d.data_atualizacao) >= u.timestamp_inicio
          AND COALESCE(d.ultima_extracao_em, d.data_atualizacao) <= u.timestamp_fim
    WHERE u.entidade = 'usuarios_sistema'
    GROUP BY u.entidade

    UNION ALL
    SELECT u.entidade, COUNT(c.id)
    FROM UltimasExtracoes u
    LEFT JOIN dbo.coletas c
           ON u.entidade = 'coletas'
          AND c.data_extracao >= u.timestamp_inicio
          AND c.data_extracao <= u.timestamp_fim
    WHERE u.entidade = 'coletas'
    GROUP BY u.entidade

    UNION ALL
    SELECT u.entidade, COUNT(f.id)
    FROM UltimasExtracoes u
    LEFT JOIN dbo.fretes f
           ON u.entidade = 'fretes'
          AND f.data_extracao >= u.timestamp_inicio
          AND f.data_extracao <= u.timestamp_fim
    WHERE u.entidade = 'fretes'
    GROUP BY u.entidade

    UNION ALL
    SELECT u.entidade, COUNT(fg.id)
    FROM UltimasExtracoes u
    LEFT JOIN dbo.faturas_graphql fg
           ON u.entidade = 'faturas_graphql'
          AND fg.data_extracao >= u.timestamp_inicio
          AND fg.data_extracao <= u.timestamp_fim
    WHERE u.entidade = 'faturas_graphql'
    GROUP BY u.entidade

    UNION ALL
    SELECT u.entidade, COUNT(m.identificador_unico)
    FROM UltimasExtracoes u
    LEFT JOIN dbo.manifestos m
           ON u.entidade = 'manifestos'
          AND m.data_extracao >= u.timestamp_inicio
          AND m.data_extracao <= u.timestamp_fim
    WHERE u.entidade = 'manifestos'
    GROUP BY u.entidade

    UNION ALL
    SELECT u.entidade, COUNT(cot.sequence_code)
    FROM UltimasExtracoes u
    LEFT JOIN dbo.cotacoes cot
           ON u.entidade = 'cotacoes'
          AND cot.data_extracao >= u.timestamp_inicio
          AND cot.data_extracao <= u.timestamp_fim
    WHERE u.entidade = 'cotacoes'
    GROUP BY u.entidade

    UNION ALL
    SELECT u.entidade, COUNT(lc.sequence_number)
    FROM UltimasExtracoes u
    LEFT JOIN dbo.localizacao_cargas lc
           ON u.entidade = 'localizacao_cargas'
          AND lc.data_extracao >= u.timestamp_inicio
          AND lc.data_extracao <= u.timestamp_fim
    WHERE u.entidade = 'localizacao_cargas'
    GROUP BY u.entidade

    UNION ALL
    SELECT u.entidade, COUNT(cap.sequence_code)
    FROM UltimasExtracoes u
    LEFT JOIN dbo.contas_a_pagar cap
           ON u.entidade = 'contas_a_pagar'
          AND cap.data_extracao >= u.timestamp_inicio
          AND cap.data_extracao <= u.timestamp_fim
    WHERE u.entidade = 'contas_a_pagar'
    GROUP BY u.entidade

    UNION ALL
    SELECT u.entidade, COUNT(fpc.unique_id)
    FROM UltimasExtracoes u
    LEFT JOIN dbo.faturas_por_cliente fpc
           ON u.entidade = 'faturas_por_cliente'
          AND fpc.data_extracao >= u.timestamp_inicio
          AND fpc.data_extracao <= u.timestamp_fim
    WHERE u.entidade = 'faturas_por_cliente'
    GROUP BY u.entidade

    UNION ALL
    SELECT u.entidade, COUNT(i.identificador_unico)
    FROM UltimasExtracoes u
    LEFT JOIN dbo.inventario i
           ON u.entidade = 'inventario'
          AND i.data_extracao >= u.timestamp_inicio
          AND i.data_extracao <= u.timestamp_fim
    WHERE u.entidade = 'inventario'
    GROUP BY u.entidade

    UNION ALL
    SELECT u.entidade, COUNT(s.identificador_unico)
    FROM UltimasExtracoes u
    LEFT JOIN dbo.sinistros s
           ON u.entidade = 'sinistros'
          AND s.data_extracao >= u.timestamp_inicio
          AND s.data_extracao <= u.timestamp_fim
    WHERE u.entidade = 'sinistros'
    GROUP BY u.entidade
)
SELECT
    u.entidade,
    u.registros_extraidos AS registros_api,
    b.registros_banco,
    u.registros_extraidos - b.registros_banco AS diferenca,
    CASE
        WHEN u.registros_extraidos IS NULL THEN 'SEM_LOG'
        WHEN b.registros_banco IS NULL THEN 'SEM_CONTAGEM_BANCO'
        WHEN u.registros_extraidos - b.registros_banco = 0 THEN 'OK'
        WHEN u.registros_extraidos - b.registros_banco > 0 AND u.registros_extraidos - b.registros_banco <= 5 THEN 'DIFERENCA_PEQUENA'
        WHEN u.registros_extraidos - b.registros_banco > 5 THEN 'DIFERENCA_GRANDE'
        WHEN u.registros_extraidos - b.registros_banco < 0 THEN 'MAIS_NO_BANCO'
        ELSE 'ERRO'
    END AS status,
    CONVERT(VARCHAR, u.timestamp_fim, 120) AS ultima_extracao
FROM UltimasExtracoes u
LEFT JOIN ContagensBanco b ON b.entidade = u.entidade
ORDER BY
    CASE
        WHEN u.registros_extraidos IS NULL THEN 5
        WHEN b.registros_banco IS NULL THEN 6
        WHEN u.registros_extraidos - b.registros_banco = 0 THEN 1
        WHEN u.registros_extraidos - b.registros_banco > 0 AND u.registros_extraidos - b.registros_banco <= 5 THEN 2
        WHEN u.registros_extraidos - b.registros_banco > 5 THEN 3
        WHEN u.registros_extraidos - b.registros_banco < 0 THEN 4
        ELSE 7
    END,
    u.entidade;
