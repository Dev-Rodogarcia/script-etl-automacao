-- ==============================================================================
-- VALIDAÇÃO DE INTERVALO DE DATAS DE NEGÓCIO - TODAS AS ENTIDADES
-- Observação:
--   - Usa colunas de data de negócio (não data_extracao).
--   - data_extracao representa quando o registro foi salvo no banco.
-- ==============================================================================

DECLARE @inicio DATE = '2024-01-01';
DECLARE @fim_exclusivo DATE = '2025-01-01';

SELECT
    v.tabela,
    v.total_registros,
    v.dentro_intervalo,
    v.antes_intervalo,
    v.depois_intervalo,
    v.min_data_negocio,
    v.max_data_negocio,
    v.min_data_extracao,
    v.max_data_extracao
FROM (
    SELECT
        'dbo.coletas' AS tabela,
        COUNT_BIG(*) AS total_registros,
        SUM(CASE WHEN CAST(request_date AS DATE) >= @inicio AND CAST(request_date AS DATE) < @fim_exclusivo THEN 1 ELSE 0 END) AS dentro_intervalo,
        SUM(CASE WHEN CAST(request_date AS DATE) < @inicio THEN 1 ELSE 0 END) AS antes_intervalo,
        SUM(CASE WHEN CAST(request_date AS DATE) >= @fim_exclusivo THEN 1 ELSE 0 END) AS depois_intervalo,
        MIN(CAST(request_date AS DATETIME2)) AS min_data_negocio,
        MAX(CAST(request_date AS DATETIME2)) AS max_data_negocio,
        MIN(CAST(data_extracao AS DATETIME2)) AS min_data_extracao,
        MAX(CAST(data_extracao AS DATETIME2)) AS max_data_extracao
    FROM dbo.coletas

    UNION ALL

    SELECT
        'dbo.fretes' AS tabela,
        COUNT_BIG(*) AS total_registros,
        SUM(CASE WHEN CAST(servico_em AS DATE) >= @inicio AND CAST(servico_em AS DATE) < @fim_exclusivo THEN 1 ELSE 0 END) AS dentro_intervalo,
        SUM(CASE WHEN CAST(servico_em AS DATE) < @inicio THEN 1 ELSE 0 END) AS antes_intervalo,
        SUM(CASE WHEN CAST(servico_em AS DATE) >= @fim_exclusivo THEN 1 ELSE 0 END) AS depois_intervalo,
        MIN(CAST(servico_em AS DATETIME2)) AS min_data_negocio,
        MAX(CAST(servico_em AS DATETIME2)) AS max_data_negocio,
        MIN(CAST(data_extracao AS DATETIME2)) AS min_data_extracao,
        MAX(CAST(data_extracao AS DATETIME2)) AS max_data_extracao
    FROM dbo.fretes

    UNION ALL

    SELECT
        'dbo.manifestos' AS tabela,
        COUNT_BIG(*) AS total_registros,
        SUM(CASE WHEN CAST(created_at AS DATE) >= @inicio AND CAST(created_at AS DATE) < @fim_exclusivo THEN 1 ELSE 0 END) AS dentro_intervalo,
        SUM(CASE WHEN CAST(created_at AS DATE) < @inicio THEN 1 ELSE 0 END) AS antes_intervalo,
        SUM(CASE WHEN CAST(created_at AS DATE) >= @fim_exclusivo THEN 1 ELSE 0 END) AS depois_intervalo,
        MIN(CAST(created_at AS DATETIME2)) AS min_data_negocio,
        MAX(CAST(created_at AS DATETIME2)) AS max_data_negocio,
        MIN(CAST(data_extracao AS DATETIME2)) AS min_data_extracao,
        MAX(CAST(data_extracao AS DATETIME2)) AS max_data_extracao
    FROM dbo.manifestos

    UNION ALL

    SELECT
        'dbo.cotacoes' AS tabela,
        COUNT_BIG(*) AS total_registros,
        SUM(CASE WHEN CAST(requested_at AS DATE) >= @inicio AND CAST(requested_at AS DATE) < @fim_exclusivo THEN 1 ELSE 0 END) AS dentro_intervalo,
        SUM(CASE WHEN CAST(requested_at AS DATE) < @inicio THEN 1 ELSE 0 END) AS antes_intervalo,
        SUM(CASE WHEN CAST(requested_at AS DATE) >= @fim_exclusivo THEN 1 ELSE 0 END) AS depois_intervalo,
        MIN(CAST(requested_at AS DATETIME2)) AS min_data_negocio,
        MAX(CAST(requested_at AS DATETIME2)) AS max_data_negocio,
        MIN(CAST(data_extracao AS DATETIME2)) AS min_data_extracao,
        MAX(CAST(data_extracao AS DATETIME2)) AS max_data_extracao
    FROM dbo.cotacoes

    UNION ALL

    SELECT
        'dbo.localizacao_cargas' AS tabela,
        COUNT_BIG(*) AS total_registros,
        SUM(CASE WHEN CAST(service_at AS DATE) >= @inicio AND CAST(service_at AS DATE) < @fim_exclusivo THEN 1 ELSE 0 END) AS dentro_intervalo,
        SUM(CASE WHEN CAST(service_at AS DATE) < @inicio THEN 1 ELSE 0 END) AS antes_intervalo,
        SUM(CASE WHEN CAST(service_at AS DATE) >= @fim_exclusivo THEN 1 ELSE 0 END) AS depois_intervalo,
        MIN(CAST(service_at AS DATETIME2)) AS min_data_negocio,
        MAX(CAST(service_at AS DATETIME2)) AS max_data_negocio,
        MIN(CAST(data_extracao AS DATETIME2)) AS min_data_extracao,
        MAX(CAST(data_extracao AS DATETIME2)) AS max_data_extracao
    FROM dbo.localizacao_cargas

    UNION ALL

    SELECT
        'dbo.contas_a_pagar' AS tabela,
        COUNT_BIG(*) AS total_registros,
        SUM(CASE WHEN CAST(issue_date AS DATE) >= @inicio AND CAST(issue_date AS DATE) < @fim_exclusivo THEN 1 ELSE 0 END) AS dentro_intervalo,
        SUM(CASE WHEN CAST(issue_date AS DATE) < @inicio THEN 1 ELSE 0 END) AS antes_intervalo,
        SUM(CASE WHEN CAST(issue_date AS DATE) >= @fim_exclusivo THEN 1 ELSE 0 END) AS depois_intervalo,
        MIN(CAST(issue_date AS DATETIME2)) AS min_data_negocio,
        MAX(CAST(issue_date AS DATETIME2)) AS max_data_negocio,
        MIN(CAST(data_extracao AS DATETIME2)) AS min_data_extracao,
        MAX(CAST(data_extracao AS DATETIME2)) AS max_data_extracao
    FROM dbo.contas_a_pagar

    UNION ALL

    SELECT
        'dbo.faturas_por_cliente' AS tabela,
        COUNT_BIG(*) AS total_registros,
        SUM(CASE WHEN CAST(data_emissao_fatura AS DATE) >= @inicio AND CAST(data_emissao_fatura AS DATE) < @fim_exclusivo THEN 1 ELSE 0 END) AS dentro_intervalo,
        SUM(CASE WHEN CAST(data_emissao_fatura AS DATE) < @inicio THEN 1 ELSE 0 END) AS antes_intervalo,
        SUM(CASE WHEN CAST(data_emissao_fatura AS DATE) >= @fim_exclusivo THEN 1 ELSE 0 END) AS depois_intervalo,
        MIN(CAST(data_emissao_fatura AS DATETIME2)) AS min_data_negocio,
        MAX(CAST(data_emissao_fatura AS DATETIME2)) AS max_data_negocio,
        MIN(CAST(data_extracao AS DATETIME2)) AS min_data_extracao,
        MAX(CAST(data_extracao AS DATETIME2)) AS max_data_extracao
    FROM dbo.faturas_por_cliente

    UNION ALL

    SELECT
        'dbo.faturas_graphql' AS tabela,
        COUNT_BIG(*) AS total_registros,
        SUM(CASE WHEN CAST(issue_date AS DATE) >= @inicio AND CAST(issue_date AS DATE) < @fim_exclusivo THEN 1 ELSE 0 END) AS dentro_intervalo,
        SUM(CASE WHEN CAST(issue_date AS DATE) < @inicio THEN 1 ELSE 0 END) AS antes_intervalo,
        SUM(CASE WHEN CAST(issue_date AS DATE) >= @fim_exclusivo THEN 1 ELSE 0 END) AS depois_intervalo,
        MIN(CAST(issue_date AS DATETIME2)) AS min_data_negocio,
        MAX(CAST(issue_date AS DATETIME2)) AS max_data_negocio,
        MIN(CAST(data_extracao AS DATETIME2)) AS min_data_extracao,
        MAX(CAST(data_extracao AS DATETIME2)) AS max_data_extracao
    FROM dbo.faturas_graphql
) v
ORDER BY v.tabela;
