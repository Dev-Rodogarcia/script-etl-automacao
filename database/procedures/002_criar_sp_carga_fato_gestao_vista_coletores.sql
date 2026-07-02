-- ============================================================================
-- Procedure de carga idempotente da fato de Gestao a Vista para coletores
-- ============================================================================

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;
GO

CREATE OR ALTER PROCEDURE dbo.sp_carga_fato_gestao_vista_coletores
    @DataInicio DATE = NULL,
    @DataFimExclusivo DATE = NULL,
    @MarcarAusentesComoExcluidos BIT = 0,
    @SnapshotEm DATETIME2(0) = NULL
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    IF OBJECT_ID(N'dbo.fato_gestao_vista_coletores', N'U') IS NULL
        THROW 51040, 'Tabela dbo.fato_gestao_vista_coletores nao encontrada. Execute a migration 030 antes da carga.', 1;

    IF OBJECT_ID(N'dbo.manifestos', N'U') IS NULL
        THROW 51041, 'Tabela dbo.manifestos nao encontrada. Carga da fato de coletores abortada.', 1;

    IF OBJECT_ID(N'dbo.inventario', N'U') IS NULL
        THROW 51042, 'Tabela dbo.inventario nao encontrada. Carga da fato de coletores abortada.', 1;

    IF OBJECT_ID(N'dbo.fretes', N'U') IS NULL
        THROW 51043, 'Tabela dbo.fretes nao encontrada. Carga da fato de coletores abortada.', 1;

    SET @DataInicio = COALESCE(@DataInicio, CAST(DATEADD(DAY, -3, SYSUTCDATETIME()) AS DATE));
    SET @DataFimExclusivo = COALESCE(@DataFimExclusivo, CAST(DATEADD(DAY, 1, SYSUTCDATETIME()) AS DATE));

    IF @DataInicio >= @DataFimExclusivo
        THROW 51045, '@DataInicio deve ser menor que @DataFimExclusivo.', 1;

    DECLARE @cargaCompleta BIT = 0;
    SET @SnapshotEm = COALESCE(@SnapshotEm, SYSUTCDATETIME());

    DECLARE @lockResult INT;
    EXEC @lockResult = sys.sp_getapplock
        @Resource = N'dbo.sp_carga_fato_gestao_vista_coletores',
        @LockMode = N'Exclusive',
        @LockOwner = N'Session',
        @LockTimeout = 0;

    IF @lockResult < 0
        THROW 51046, 'Carga da fato de coletores ja esta em execucao por outra sessao.', 1;

    DECLARE @resultado TABLE (
        merge_action NVARCHAR(10) NOT NULL
    );

    BEGIN TRY
        ;WITH filiais_alias(alias_key, filial) AS (
            SELECT N'agu', N'AGU - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
            SELECT N'agu - rodogarcia transportes rodoviarios ltda', N'AGU - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
            SELECT N'tr rodogarcia | agu', N'AGU - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
            SELECT N'rodogarcia filial agu', N'AGU - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
            SELECT N'cas', N'CAS - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
            SELECT N'cas - rodogarcia transportes rodoviarios ltda', N'CAS - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
            SELECT N'tr rodogarcia | cas', N'CAS - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
            SELECT N'rodogarcia filial cas', N'CAS - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
            SELECT N'cpq', N'CPQ - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
            SELECT N'cpq - rodogarcia transportes rodoviarios ltda', N'CPQ - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
            SELECT N'tr rodogarcia | cpq', N'CPQ - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
            SELECT N'rodogarcia filial cpq', N'CPQ - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
            SELECT N'cwb', N'CWB - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
            SELECT N'cwb - rodogarcia transportes rodoviarios ltda', N'CWB - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
            SELECT N'tr rodogarcia | cwb', N'CWB - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
            SELECT N'rodogarcia filial cwb', N'CWB - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
            SELECT N'nhb', N'NHB - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
            SELECT N'nhb - rodogarcia transportes rodoviarios ltda', N'NHB - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
            SELECT N'tr rodogarcia | nhb', N'NHB - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
            SELECT N'rodogarcia filial nhb', N'NHB - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
            SELECT N'rec', N'REC - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
            SELECT N'rec - rodogarcia transportes rodoviarios ltda', N'REC - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
            SELECT N'tr rodogarcia | rec', N'REC - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
            SELECT N'rodogarcia filial rec', N'REC - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
            SELECT N'rjr', N'RJR - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
            SELECT N'rjr - rodogarcia transportes rodoviarios ltda', N'RJR - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
            SELECT N'tr rodogarcia | rjr', N'RJR - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
            SELECT N'rodogarcia filial rjr', N'RJR - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
            SELECT N'spo', N'SPO - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
            SELECT N'spo - rodogarcia transportes rodoviarios ltda', N'SPO - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
            SELECT N'tr rodogarcia | spo', N'SPO - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
            SELECT N'rodogarcia filial spo', N'SPO - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA'
        ),
        filiais_operacionais(filial) AS (
            SELECT N'AGU - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
            SELECT N'CAS - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
            SELECT N'CPQ - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
            SELECT N'CWB - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
            SELECT N'NHB - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
            SELECT N'REC - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
            SELECT N'RJR - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA' UNION ALL
            SELECT N'SPO - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA'
        ),
        fretes_filial AS (
            SELECT
                f.corporation_sequence_number,
                f.filial_nome,
                f.data_extracao,
                ROW_NUMBER() OVER (
                    PARTITION BY f.corporation_sequence_number
                    ORDER BY f.data_extracao DESC, f.id DESC
                ) AS rn
            FROM dbo.fretes AS f
            WHERE f.corporation_sequence_number IS NOT NULL
              AND f.excluido_na_origem = 0
        ),
        manifestos_fonte AS (
            SELECT
                COALESCE(
                    CONVERT(NVARCHAR(64), m.sequence_code),
                    NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), m.identificador_unico))), N'')
                ) AS chave_manifesto,
                CAST(m.created_at AS DATE) AS data_referencia,
                NULLIF(LTRIM(RTRIM(
                    REPLACE(REPLACE(REPLACE(COALESCE(m.unloading_recipient_names, N''), N'[', N''), N']', N''), N'"', N'')
                )), N'') AS local_descarregamento,
                COALESCE(alias_emitida.filial, filial_raw.valor, N'Filial nao informada') AS filial_emitida,
                LOWER(COALESCE(alias_emitida.filial, filial_raw.valor, N'Filial nao informada')) COLLATE Latin1_General_CI_AI AS filial_emitida_key,
                LOWER(NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), m.classification))), N'')) COLLATE Latin1_General_CI_AI AS classificacao_key,
                m.data_extracao
            FROM dbo.manifestos AS m
            CROSS APPLY (
                SELECT NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), m.branch_nickname))), N'') AS valor
            ) AS filial_raw
            OUTER APPLY (
                SELECT TOP (1) a.filial
                FROM filiais_alias AS a
                WHERE LOWER(filial_raw.valor) COLLATE Latin1_General_CI_AI = a.alias_key COLLATE Latin1_General_CI_AI
            ) AS alias_emitida
            WHERE m.created_at IS NOT NULL
              AND m.excluido_na_origem = 0
              AND (@DataInicio IS NULL OR m.created_at >= @DataInicio)
              AND (@DataFimExclusivo IS NULL OR m.created_at < @DataFimExclusivo)
        ),
        manifestos_deduplicados AS (
            SELECT
                mf.*,
                ROW_NUMBER() OVER (
                    PARTITION BY mf.chave_manifesto
                    ORDER BY mf.data_extracao DESC, mf.chave_manifesto
                ) AS rn
            FROM manifestos_fonte AS mf
            WHERE mf.chave_manifesto IS NOT NULL
              AND mf.data_referencia IS NOT NULL
              AND (mf.classificacao_key IS NULL OR mf.classificacao_key NOT LIKE N'carga fechada%')
              AND (mf.classificacao_key IS NULL OR mf.classificacao_key NOT LIKE N'acerto de motorista%')
              AND (mf.classificacao_key IS NULL OR mf.classificacao_key NOT LIKE N'frete retorno%')
              AND (mf.classificacao_key IS NULL OR mf.classificacao_key NOT LIKE N'viagem vazia%')
        ),
        manifestos AS (
            SELECT *
            FROM manifestos_deduplicados
            WHERE rn = 1
        ),
        manifestos_emitidos AS (
            SELECT
                data_referencia,
                filial_emitida AS filial,
                filial_emitida_key AS filial_key,
                COUNT_BIG(1) AS manifestos_emitidos,
                MAX(data_extracao) AS updated_at
            FROM manifestos
            GROUP BY data_referencia, filial_emitida, filial_emitida_key
        ),
        descarregamento_partes AS (
            SELECT
                m.chave_manifesto,
                m.data_referencia,
                COALESCE(alias_descarga.filial, parte.valor, N'Filial nao informada') AS filial,
                LOWER(COALESCE(alias_descarga.filial, parte.valor, N'Filial nao informada')) COLLATE Latin1_General_CI_AI AS filial_key,
                m.data_extracao
            FROM manifestos AS m
            CROSS APPLY STRING_SPLIT(
                REPLACE(REPLACE(REPLACE(COALESCE(m.local_descarregamento, N''), CHAR(13), N';'), CHAR(10), N';'), N',', N';'),
                N';'
            ) AS split
            CROSS APPLY (
                SELECT NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), split.value))), N'') AS valor
            ) AS parte
            OUTER APPLY (
                SELECT TOP (1) a.filial
                FROM filiais_alias AS a
                WHERE LOWER(parte.valor) COLLATE Latin1_General_CI_AI = a.alias_key COLLATE Latin1_General_CI_AI
            ) AS alias_descarga
            WHERE parte.valor IS NOT NULL
              AND LOWER(parte.valor) COLLATE Latin1_General_CI_AI <> N'null'
        ),
        descarregamento_elegivel AS (
            SELECT
                dp.*,
                ROW_NUMBER() OVER (
                    PARTITION BY dp.chave_manifesto
                    ORDER BY dp.filial_key, dp.filial
                ) AS rn
            FROM descarregamento_partes AS dp
        ),
        manifestos_descarregamento AS (
            SELECT
                data_referencia,
                filial,
                filial_key,
                COUNT_BIG(1) AS manifestos_descarregamento,
                MAX(data_extracao) AS updated_at
            FROM descarregamento_elegivel
            WHERE rn = 1
            GROUP BY data_referencia, filial, filial_key
        ),
        ordens_fonte AS (
            SELECT
                i.sequence_code AS numero_ordem,
                CAST(i.started_at AS DATE) AS data_referencia,
                CASE WHEN i.finished_at IS NULL THEN CAST(1 AS BIGINT) ELSE CAST(0 AS BIGINT) END AS incompleta,
                COALESCE(alias_ordem.filial, filial_raw.valor, N'Filial nao informada') AS filial,
                LOWER(COALESCE(alias_ordem.filial, filial_raw.valor, N'Filial nao informada')) COLLATE Latin1_General_CI_AI AS filial_key,
                data_atualizacao.updated_at
            FROM dbo.inventario AS i
            LEFT JOIN fretes_filial AS ff
                ON ff.corporation_sequence_number = i.numero_minuta
               AND ff.rn = 1
            CROSS APPLY (
                SELECT CASE i.type
                    WHEN N'CheckIn::Order::Loading' THEN N'carregamento'
                    WHEN N'CheckIn::Order::Unloading' THEN N'descarregamento'
                    WHEN N'CheckIn::Order::Picking' THEN N'picking'
                    WHEN N'CheckIn::Order::Receipt' THEN N'recebimento'
                    WHEN N'CheckIn::Order::Return' THEN N'retorno'
                    ELSE LOWER(LTRIM(RTRIM(CONVERT(NVARCHAR(100), i.type))))
                END COLLATE Latin1_General_CI_AI AS tipo_normalizado
            ) AS tipo
            CROSS APPLY (
                SELECT COALESCE(
                    NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), i.branch_nickname))), N''),
                    NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), ff.filial_nome))), N'')
                ) AS valor
            ) AS filial_raw
            OUTER APPLY (
                SELECT TOP (1) a.filial
                FROM filiais_alias AS a
                WHERE LOWER(filial_raw.valor) COLLATE Latin1_General_CI_AI = a.alias_key COLLATE Latin1_General_CI_AI
            ) AS alias_ordem
            CROSS APPLY (
                SELECT MAX(valor) AS updated_at
                FROM (VALUES (i.data_extracao), (ff.data_extracao)) AS atualizacoes(valor)
            ) AS data_atualizacao
            WHERE i.started_at IS NOT NULL
              AND i.excluido_na_origem = 0
              AND (@DataInicio IS NULL OR i.started_at >= @DataInicio)
              AND (@DataFimExclusivo IS NULL OR i.started_at < @DataFimExclusivo)
              AND tipo.tipo_normalizado IN (
                    N'picking',
                    N'retorno',
                    N'recebimento',
                    N'carregamento',
                    N'descarregamento'
              )
        ),
        ordens_deduplicadas AS (
            SELECT
                ofn.*,
                ROW_NUMBER() OVER (
                    PARTITION BY ofn.numero_ordem
                    ORDER BY ofn.updated_at DESC, ofn.numero_ordem DESC
                ) AS rn
            FROM ordens_fonte AS ofn
            WHERE ofn.numero_ordem IS NOT NULL
              AND ofn.data_referencia IS NOT NULL
        ),
        ordens AS (
            SELECT *
            FROM ordens_deduplicadas
            WHERE rn = 1
        ),
        ordens_agrupadas AS (
            SELECT
                data_referencia,
                filial,
                filial_key,
                COUNT_BIG(1) AS manifestos_bipados,
                SUM(incompleta) AS manifestos_incompletos,
                MAX(updated_at) AS updated_at
            FROM ordens
            GROUP BY data_referencia, filial, filial_key
        ),
        pontos_union AS (
            SELECT
                data_referencia,
                filial,
                filial_key,
                CAST(0 AS BIGINT) AS manifestos_bipados,
                manifestos_emitidos,
                CAST(0 AS BIGINT) AS manifestos_descarregamento,
                CAST(0 AS BIGINT) AS manifestos_incompletos,
                updated_at
            FROM manifestos_emitidos
            UNION ALL
            SELECT
                data_referencia,
                filial,
                filial_key,
                CAST(0 AS BIGINT),
                CAST(0 AS BIGINT),
                manifestos_descarregamento,
                CAST(0 AS BIGINT),
                updated_at
            FROM manifestos_descarregamento
            UNION ALL
            SELECT
                data_referencia,
                filial,
                filial_key,
                manifestos_bipados,
                CAST(0 AS BIGINT),
                CAST(0 AS BIGINT),
                manifestos_incompletos,
                updated_at
            FROM ordens_agrupadas
        ),
        pontos_agregados AS (
            SELECT
                pu.data_referencia,
                COALESCE(pu.filial, N'Filial nao informada') AS filial,
                COALESCE(pu.filial_key, N'filial nao informada') AS filial_key,
                N'Geral' AS classificacao,
                SUM(pu.manifestos_bipados) AS manifestos_bipados,
                SUM(pu.manifestos_emitidos) AS manifestos_emitidos,
                SUM(pu.manifestos_descarregamento) AS manifestos_descarregamento,
                SUM(pu.manifestos_incompletos) AS manifestos_incompletos,
                MAX(pu.updated_at) AS updated_at
            FROM pontos_union AS pu
            GROUP BY
                pu.data_referencia,
                COALESCE(pu.filial, N'Filial nao informada'),
                COALESCE(pu.filial_key, N'filial nao informada')
            HAVING SUM(pu.manifestos_bipados) > 0
                OR SUM(pu.manifestos_emitidos) > 0
                OR SUM(pu.manifestos_descarregamento) > 0
        ),
        origem_base AS (
            SELECT
                pa.data_referencia,
                YEAR(pa.data_referencia) * 100 + MONTH(pa.data_referencia) AS data_referencia_yyyymm,
                pa.filial,
                pa.filial_key,
                pa.classificacao,
                pa.manifestos_bipados,
                pa.manifestos_emitidos,
                pa.manifestos_descarregamento,
                totais.total_manifestos,
                pa.manifestos_incompletos,
                CAST(CASE
                    WHEN totais.total_manifestos > 0
                        THEN (CONVERT(DECIMAL(19, 4), pa.manifestos_bipados) * CONVERT(DECIMAL(19, 4), 100.0))
                             / CONVERT(DECIMAL(19, 4), totais.total_manifestos)
                    ELSE 0
                END AS DECIMAL(9, 4)) AS pct_utilizacao,
                CAST(CASE WHEN pa.manifestos_bipados > 0 THEN 1 ELSE 0 END AS BIT) AS tem_ordens_conferencia,
                CAST(CASE WHEN totais.total_manifestos > 0 THEN 1 ELSE 0 END AS BIT) AS tem_manifestos_bipaveis,
                CAST(CASE WHEN EXISTS (
                    SELECT 1
                    FROM filiais_operacionais AS fo
                    WHERE fo.filial = pa.filial
                ) THEN 1 ELSE 0 END AS BIT) AS is_filial_operacional,
                CAST(1 AS BIT) AS is_linha_valida_indicador,
                CAST(0 AS BIT) AS excluido_na_origem,
                CAST(NULL AS DATETIME2(0)) AS data_exclusao_origem,
                @SnapshotEm AS ultima_reconciliacao_origem_em,
                pa.updated_at,
                @SnapshotEm AS snapshot_em
            FROM pontos_agregados AS pa
            CROSS APPLY (
                SELECT pa.manifestos_emitidos + pa.manifestos_descarregamento AS total_manifestos
            ) AS totais
        ),
        origem AS (
            SELECT
                ob.*,
                CONVERT(BINARY(32), HASHBYTES('SHA2_256', CONCAT_WS(N'|',
                    COALESCE(CONVERT(NVARCHAR(30), ob.data_referencia, 126), N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(20), ob.data_referencia_yyyymm), N'__NULL__'),
                    COALESCE(ob.filial, N'__NULL__'),
                    COALESCE(ob.filial_key, N'__NULL__'),
                    COALESCE(ob.classificacao, N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(30), ob.manifestos_bipados), N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(30), ob.manifestos_emitidos), N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(30), ob.manifestos_descarregamento), N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(30), ob.total_manifestos), N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(30), ob.manifestos_incompletos), N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(30), ob.pct_utilizacao), N'__NULL__'),
                    CONVERT(NVARCHAR(1), ob.tem_ordens_conferencia),
                    CONVERT(NVARCHAR(1), ob.tem_manifestos_bipaveis),
                    CONVERT(NVARCHAR(1), ob.is_filial_operacional),
                    CONVERT(NVARCHAR(1), ob.is_linha_valida_indicador),
                    CONVERT(NVARCHAR(1), ob.excluido_na_origem),
                    COALESCE(CONVERT(NVARCHAR(30), ob.updated_at, 126), N'__NULL__')
                ))) AS hash_linha
            FROM origem_base AS ob
        )
        MERGE dbo.fato_gestao_vista_coletores WITH (HOLDLOCK) AS target
        USING origem AS source
           ON target.data_referencia = source.data_referencia
          AND target.filial_key = source.filial_key
          AND target.classificacao = source.classificacao
        WHEN MATCHED
         AND (
                target.hash_linha IS NULL
             OR source.hash_linha IS NULL
             OR target.hash_linha <> source.hash_linha
             OR target.excluido_na_origem = 1
         )
            THEN UPDATE SET
                data_referencia_yyyymm = source.data_referencia_yyyymm,
                filial = source.filial,
                manifestos_bipados = source.manifestos_bipados,
                manifestos_emitidos = source.manifestos_emitidos,
                manifestos_descarregamento = source.manifestos_descarregamento,
                total_manifestos = source.total_manifestos,
                manifestos_incompletos = source.manifestos_incompletos,
                pct_utilizacao = source.pct_utilizacao,
                tem_ordens_conferencia = source.tem_ordens_conferencia,
                tem_manifestos_bipaveis = source.tem_manifestos_bipaveis,
                is_filial_operacional = source.is_filial_operacional,
                is_linha_valida_indicador = source.is_linha_valida_indicador,
                excluido_na_origem = source.excluido_na_origem,
                data_exclusao_origem = source.data_exclusao_origem,
                ultima_reconciliacao_origem_em = source.ultima_reconciliacao_origem_em,
                updated_at = source.updated_at,
                snapshot_em = source.snapshot_em,
                hash_linha = source.hash_linha
        WHEN NOT MATCHED BY TARGET
            THEN INSERT (
                data_referencia,
                data_referencia_yyyymm,
                filial,
                filial_key,
                classificacao,
                manifestos_bipados,
                manifestos_emitidos,
                manifestos_descarregamento,
                total_manifestos,
                manifestos_incompletos,
                pct_utilizacao,
                tem_ordens_conferencia,
                tem_manifestos_bipaveis,
                is_filial_operacional,
                is_linha_valida_indicador,
                excluido_na_origem,
                data_exclusao_origem,
                ultima_reconciliacao_origem_em,
                updated_at,
                snapshot_em,
                hash_linha
            )
            VALUES (
                source.data_referencia,
                source.data_referencia_yyyymm,
                source.filial,
                source.filial_key,
                source.classificacao,
                source.manifestos_bipados,
                source.manifestos_emitidos,
                source.manifestos_descarregamento,
                source.total_manifestos,
                source.manifestos_incompletos,
                source.pct_utilizacao,
                source.tem_ordens_conferencia,
                source.tem_manifestos_bipaveis,
                source.is_filial_operacional,
                source.is_linha_valida_indicador,
                source.excluido_na_origem,
                source.data_exclusao_origem,
                source.ultima_reconciliacao_origem_em,
                source.updated_at,
                source.snapshot_em,
                source.hash_linha
            )
        WHEN NOT MATCHED BY SOURCE
         AND @MarcarAusentesComoExcluidos = 1
         AND target.excluido_na_origem = 0
         AND (
                @cargaCompleta = 1
             OR (
                    target.data_referencia >= @DataInicio
                AND target.data_referencia < @DataFimExclusivo
             )
         )
            THEN UPDATE SET
                is_linha_valida_indicador = 0,
                tem_ordens_conferencia = 0,
                tem_manifestos_bipaveis = 0,
                excluido_na_origem = 1,
                data_exclusao_origem = COALESCE(target.data_exclusao_origem, @SnapshotEm),
                ultima_reconciliacao_origem_em = @SnapshotEm,
                snapshot_em = @SnapshotEm,
                hash_linha = NULL
        OUTPUT $action
        INTO @resultado (merge_action);

        EXEC sys.sp_releaseapplock
            @Resource = N'dbo.sp_carga_fato_gestao_vista_coletores',
            @LockOwner = N'Session';

        SELECT
            COALESCE(SUM(CASE WHEN merge_action = N'INSERT' THEN 1 ELSE 0 END), 0) AS linhas_inseridas,
            COALESCE(SUM(CASE WHEN merge_action = N'UPDATE' THEN 1 ELSE 0 END), 0) AS linhas_atualizadas,
            @SnapshotEm AS snapshot_em
        FROM @resultado;
    END TRY
    BEGIN CATCH
        EXEC sys.sp_releaseapplock
            @Resource = N'dbo.sp_carga_fato_gestao_vista_coletores',
            @LockOwner = N'Session';
        THROW;
    END CATCH;
END;
GO

PRINT 'Procedure dbo.sp_carga_fato_gestao_vista_coletores criada/atualizada com sucesso.';
GO
