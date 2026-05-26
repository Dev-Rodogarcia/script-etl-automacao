/*
  Valida se os campos que alimentam Performance de Veiculos / Manifestos
  chegaram ate a tabela fisica e a view consumida pelos dashboards.

  A validacao nao cria fallback: ela falha quando a API deixou valores reais
  no metadata, mas as colunas dedicadas permaneceram nulas ou zeradas.
*/

SET NOCOUNT ON;

PRINT '============================================================';
PRINT 'Validacao 037 - Manifestos: metricas financeiras/operacionais';
PRINT '============================================================';

IF OBJECT_ID('dbo.manifestos', 'U') IS NULL
BEGIN
    THROW 51036, 'Tabela dbo.manifestos nao encontrada.', 1;
END;

IF OBJECT_ID('dbo.vw_manifestos_powerbi', 'V') IS NULL
BEGIN
    THROW 51036, 'View dbo.vw_manifestos_powerbi nao encontrada.', 1;
END;

DECLARE
    @total BIGINT,
    @kmNaoZero BIGINT,
    @custoNaoZero BIGINT,
    @freteNaoZero BIGINT,
    @pesoNaoZero BIGINT,
    @capacidadeNaoZero BIGINT,
    @itensNaoZero BIGINT,
    @metadadosComValorColunaZerada BIGINT;

SELECT
    @total = COUNT_BIG(1),
    @kmNaoZero = SUM(CASE WHEN ISNULL(km, 0) <> 0 THEN 1 ELSE 0 END),
    @custoNaoZero = SUM(CASE WHEN ISNULL(total_cost, 0) <> 0 THEN 1 ELSE 0 END),
    @freteNaoZero = SUM(CASE WHEN ISNULL(manifest_freights_total, 0) <> 0 THEN 1 ELSE 0 END),
    @pesoNaoZero = SUM(CASE WHEN ISNULL(total_taxed_weight, 0) <> 0 THEN 1 ELSE 0 END),
    @capacidadeNaoZero = SUM(CASE WHEN ISNULL(capacidade_kg, 0) <> 0 THEN 1 ELSE 0 END),
    @itensNaoZero = SUM(CASE WHEN ISNULL(manifest_items_count, 0) <> 0 THEN 1 ELSE 0 END)
FROM dbo.manifestos;

SELECT
    @metadadosComValorColunaZerada = COUNT_BIG(1)
FROM dbo.manifestos
WHERE
    (
        TRY_CONVERT(DECIMAL(18, 6), JSON_VALUE(metadata, '$.km')) <> 0
        AND ISNULL(km, 0) = 0
    )
    OR (
        TRY_CONVERT(DECIMAL(18, 6), JSON_VALUE(metadata, '$.total_cost')) <> 0
        AND ISNULL(total_cost, 0) = 0
    )
    OR (
        TRY_CONVERT(DECIMAL(18, 6), JSON_VALUE(metadata, '$.manifest_freights_total')) <> 0
        AND ISNULL(manifest_freights_total, 0) = 0
    )
    OR (
        TRY_CONVERT(DECIMAL(18, 6), JSON_VALUE(metadata, '$.total_taxed_weight')) <> 0
        AND ISNULL(total_taxed_weight, 0) = 0
    )
    OR (
        TRY_CONVERT(DECIMAL(18, 6), JSON_VALUE(metadata, '$.mft_vie_weight_capacity')) <> 0
        AND ISNULL(capacidade_kg, 0) = 0
    );

SELECT
    @total AS total_manifestos,
    @kmNaoZero AS km_nao_zero,
    @custoNaoZero AS custo_nao_zero,
    @freteNaoZero AS frete_nao_zero,
    @pesoNaoZero AS peso_taxado_nao_zero,
    @capacidadeNaoZero AS capacidade_nao_zero,
    @itensNaoZero AS itens_nao_zero,
    @metadadosComValorColunaZerada AS metadados_com_valor_coluna_zerada;

IF @total > 0
   AND @kmNaoZero = 0
   AND @custoNaoZero = 0
   AND @freteNaoZero = 0
   AND @pesoNaoZero = 0
BEGIN
    THROW 51036, 'Manifestos existem, mas km/custo/frete/peso estao todos zerados nas colunas dedicadas.', 1;
END;

IF @metadadosComValorColunaZerada > 0
BEGIN
    THROW 51036, 'Manifestos possuem metricas no metadata, mas as colunas dedicadas estao nulas/zeradas. Corrija mapper/deduplicacao/persistencia.', 1;
END;

IF NOT EXISTS (
    SELECT 1
    FROM dbo.vw_manifestos_powerbi
    WHERE
        ISNULL([KM Total], 0) <> 0
        OR ISNULL([Custo total], 0) <> 0
        OR ISNULL([Fretes/Total], 0) <> 0
        OR ISNULL([Total peso taxado], 0) <> 0
)
AND @total > 0
BEGIN
    THROW 51036, 'View dbo.vw_manifestos_powerbi nao expoe metricas nao zeradas para Manifestos.', 1;
END;

PRINT 'OK - Metricas de Manifestos preservadas na tabela fisica e expostas pela view.';
