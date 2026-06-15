SET NOCOUNT ON;
SET TRANSACTION ISOLATION LEVEL READ UNCOMMITTED;
SET LOCK_TIMEOUT 5000;
GO

-- Ajuste o intervalo sem aplicar funcoes sobre dbo.manifestos.created_at.
DECLARE @inicio DATETIMEOFFSET = '2026-03-01T00:00:00-03:00';
DECLARE @fim_exclusivo DATETIMEOFFSET = '2026-07-01T00:00:00-03:00';

WITH comparacao AS (
    SELECT
        CASE
            WHEN COALESCE(m.trailer1_weight_capacity, 0) = 0
                THEN N'A - Apenas veiculo trator'
            WHEN COALESCE(m.trailer2_weight_capacity, 0) = 0
                THEN N'B - Carreta 1 ativa, sem Carreta 2'
            ELSE N'C - Bitrem/Rodotrem'
        END AS cenario,
        m.sequence_code,
        m.identificador_unico,
        m.mdfe_number,
        m.created_at,
        m.vehicle_plate,
        m.trailer1_license_plate,
        m.trailer2_license_plate,
        m.vehicle_weight_capacity,
        m.trailer1_weight_capacity,
        m.trailer2_weight_capacity,
        m.capacidade_kg AS capacidade_legada_origem,
        COALESCE(m.vehicle_weight_capacity, 0)
            + COALESCE(m.trailer1_weight_capacity, 0)
            + COALESCE(m.trailer2_weight_capacity, 0) AS capacidade_esperada,
        TRY_CONVERT(DECIMAL(18, 2), v.[Capacidade Lotação Kg]) AS capacidade_view,
        m.total_taxed_weight
    FROM dbo.manifestos m
    INNER JOIN dbo.vw_manifestos_powerbi v
        ON v.[Número] = m.sequence_code
       AND v.[Identificador Único] = m.identificador_unico
       AND COALESCE(v.[MDFe], -1) = COALESCE(m.mdfe_number, -1)
    WHERE m.excluido_na_origem = 0
      AND m.created_at >= @inicio
      AND m.created_at < @fim_exclusivo
)
SELECT
    cenario,
    COUNT_BIG(*) AS quantidade_linhas,
    CAST(AVG(CONVERT(DECIMAL(19, 4), capacidade_esperada)) AS DECIMAL(19, 2))
        AS media_capacidade_esperada_kg,
    CAST(AVG(CONVERT(DECIMAL(19, 4), capacidade_view)) AS DECIMAL(19, 2))
        AS media_capacidade_view_kg,
    SUM(CASE WHEN COALESCE(vehicle_weight_capacity, 0) = 0 THEN 1 ELSE 0 END)
        AS trator_capacidade_zero_ou_nula,
    SUM(CASE WHEN COALESCE(trailer1_weight_capacity, 0) = 0 THEN 1 ELSE 0 END)
        AS carreta1_capacidade_zero_ou_nula,
    SUM(CASE WHEN COALESCE(trailer2_weight_capacity, 0) = 0 THEN 1 ELSE 0 END)
        AS carreta2_capacidade_zero_ou_nula,
    SUM(CASE
        WHEN COALESCE(capacidade_view, 0) <> COALESCE(capacidade_esperada, 0) THEN 1
        ELSE 0
    END) AS divergencias_view
FROM comparacao
GROUP BY cenario
ORDER BY cenario;

WITH comparacao AS (
    SELECT
        CASE
            WHEN COALESCE(m.trailer1_weight_capacity, 0) = 0
                THEN N'A - Apenas veiculo trator'
            WHEN COALESCE(m.trailer2_weight_capacity, 0) = 0
                THEN N'B - Carreta 1 ativa, sem Carreta 2'
            ELSE N'C - Bitrem/Rodotrem'
        END AS cenario,
        m.sequence_code,
        m.identificador_unico,
        m.mdfe_number,
        m.created_at,
        m.vehicle_plate,
        m.trailer1_license_plate,
        m.trailer2_license_plate,
        m.vehicle_weight_capacity,
        m.trailer1_weight_capacity,
        m.trailer2_weight_capacity,
        COALESCE(m.vehicle_weight_capacity, 0)
            + COALESCE(m.trailer1_weight_capacity, 0)
            + COALESCE(m.trailer2_weight_capacity, 0) AS capacidade_esperada,
        TRY_CONVERT(DECIMAL(18, 2), v.[Capacidade Lotação Kg]) AS capacidade_view,
        m.total_taxed_weight
    FROM dbo.manifestos m
    INNER JOIN dbo.vw_manifestos_powerbi v
        ON v.[Número] = m.sequence_code
       AND v.[Identificador Único] = m.identificador_unico
       AND COALESCE(v.[MDFe], -1) = COALESCE(m.mdfe_number, -1)
    WHERE m.excluido_na_origem = 0
      AND m.created_at >= @inicio
      AND m.created_at < @fim_exclusivo
),
amostra AS (
    SELECT
        *,
        ROW_NUMBER() OVER (
            PARTITION BY cenario
            ORDER BY created_at DESC, sequence_code DESC, identificador_unico
        ) AS ordem_cenario
    FROM comparacao
)
SELECT
    cenario,
    sequence_code,
    identificador_unico,
    mdfe_number,
    created_at,
    vehicle_plate,
    trailer1_license_plate,
    trailer2_license_plate,
    vehicle_weight_capacity,
    trailer1_weight_capacity,
    trailer2_weight_capacity,
    capacidade_esperada,
    capacidade_view,
    total_taxed_weight,
    CAST(
        CASE
            WHEN capacidade_esperada > 0
                THEN total_taxed_weight * 100.0 / capacidade_esperada
        END
        AS DECIMAL(18, 2)
    ) AS aproveitamento_esperado_pct
FROM amostra
WHERE ordem_cenario <= 10
ORDER BY cenario, ordem_cenario;
GO
