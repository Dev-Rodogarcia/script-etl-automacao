-- ============================================================================
-- Reprocessamento manual da fato de Manifestos para Competencia Operacional
-- ============================================================================
-- Uso:
-- 1. Publique antes a procedure atualizada:
--    database/procedures/005_criar_sp_carga_fato_gestao_vista_manifestos.sql
-- 2. Execute este script no banco ETL_SISTEMA/esl_cloud.
--
-- A carga e idempotente e nao faz DELETE/TRUNCATE. Ela recalcula a fato inteira,
-- atualizando data_criacao/data_criacao_date/data_criacao_yyyymm para:
-- COALESCE(manifestos.departured_at, manifestos.created_at).

SET NOCOUNT ON;
SET XACT_ABORT ON;

IF OBJECT_ID(N'dbo.sp_carga_fato_gestao_vista_manifestos', N'P') IS NULL
    THROW 51110, 'Procedure dbo.sp_carga_fato_gestao_vista_manifestos nao encontrada.', 1;

IF OBJECT_ID(N'dbo.fato_gestao_vista_manifestos', N'U') IS NULL
    THROW 51111, 'Tabela dbo.fato_gestao_vista_manifestos nao encontrada.', 1;

IF OBJECT_ID(N'dbo.manifestos', N'U') IS NULL
    THROW 51112, 'Tabela dbo.manifestos nao encontrada.', 1;

DECLARE @SnapshotEm DATETIME2(0) = SYSUTCDATETIME();

PRINT 'Iniciando reprocessamento completo de dbo.fato_gestao_vista_manifestos...';

EXEC dbo.sp_carga_fato_gestao_vista_manifestos
    @DataInicio = NULL,
    @DataFimExclusivo = NULL,
    @MarcarAusentesComoExcluidos = 1,
    @SnapshotEm = @SnapshotEm;

PRINT 'Validando competencia operacional dos manifestos ativos...';

DECLARE @divergencias TABLE (
    sequence_code BIGINT NOT NULL,
    data_fato DATETIMEOFFSET NULL,
    data_esperada DATETIMEOFFSET NULL
);

;WITH esperado AS (
    SELECT
        m.sequence_code,
        CAST(MAX(COALESCE(m.departured_at, m.created_at)) AS DATETIMEOFFSET(3)) AS data_competencia_operacional
    FROM dbo.manifestos AS m
    WHERE COALESCE(m.excluido_na_origem, 0) = 0
      AND (m.vehicle_plate IS NULL OR m.vehicle_plate <> N'ACM0000')
    GROUP BY m.sequence_code
)
INSERT INTO @divergencias (sequence_code, data_fato, data_esperada)
    SELECT TOP (20)
        f.sequence_code,
        f.data_criacao AS data_fato,
        e.data_competencia_operacional AS data_esperada
    FROM dbo.fato_gestao_vista_manifestos AS f
    JOIN esperado AS e
        ON e.sequence_code = f.sequence_code
    WHERE f.excluido_na_origem = 0
      AND (
            (f.data_criacao <> e.data_competencia_operacional)
         OR (f.data_criacao IS NULL AND e.data_competencia_operacional IS NOT NULL)
         OR (f.data_criacao IS NOT NULL AND e.data_competencia_operacional IS NULL)
      )
    ORDER BY f.sequence_code
;

SELECT
    sequence_code,
    data_fato,
    data_esperada
FROM @divergencias
ORDER BY sequence_code;

IF EXISTS (SELECT 1 FROM @divergencias)
    THROW 51113, 'Reprocessamento concluido com divergencias de competencia operacional.', 1;

SELECT
    COUNT_BIG(1) AS manifestos_ativos,
    MIN(data_criacao) AS menor_competencia,
    MAX(data_criacao) AS maior_competencia,
    @SnapshotEm AS snapshot_em
FROM dbo.fato_gestao_vista_manifestos
WHERE excluido_na_origem = 0;

PRINT 'Reprocessamento completo de manifestos finalizado com sucesso.';
GO
