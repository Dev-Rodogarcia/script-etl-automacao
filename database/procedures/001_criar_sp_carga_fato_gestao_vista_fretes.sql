-- ============================================================================
-- Procedure de carga idempotente da fato de Gestao a Vista para fretes
-- ============================================================================

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;
GO

CREATE OR ALTER PROCEDURE dbo.sp_carga_fato_gestao_vista_fretes
    @DataInicio DATE = NULL,
    @DataFimExclusivo DATE = NULL,
    @MarcarAusentesComoExcluidos BIT = 0,
    @SnapshotEm DATETIME2(0) = NULL
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    IF OBJECT_ID(N'dbo.fato_gestao_vista_fretes', N'U') IS NULL
        THROW 51029, 'Tabela dbo.fato_gestao_vista_fretes nao encontrada. Execute a migration 029 antes da carga.', 1;

    IF OBJECT_ID(N'dbo.fretes', N'U') IS NULL
        THROW 51030, 'Tabela dbo.fretes nao encontrada. Carga da fato de Gestao a Vista abortada.', 1;

    SET @DataInicio = COALESCE(@DataInicio, CAST(DATEADD(DAY, -3, SYSUTCDATETIME()) AS DATE));
    SET @DataFimExclusivo = COALESCE(@DataFimExclusivo, CAST(DATEADD(DAY, 1, SYSUTCDATETIME()) AS DATE));

    IF @DataInicio >= @DataFimExclusivo
        THROW 51032, '@DataInicio deve ser menor que @DataFimExclusivo.', 1;

    DECLARE @cargaCompleta BIT = 0;
    SET @SnapshotEm = COALESCE(@SnapshotEm, SYSUTCDATETIME());

    DECLARE @lockResult INT;
    EXEC @lockResult = sys.sp_getapplock
        @Resource = N'dbo.sp_carga_fato_gestao_vista_fretes',
        @LockMode = N'Exclusive',
        @LockOwner = N'Session',
        @LockTimeout = 0;

    IF @lockResult < 0
        THROW 51033, 'Carga da fato de Gestao a Vista ja esta em execucao por outra sessao.', 1;

    DECLARE @resultado TABLE (
        merge_action NVARCHAR(10) NOT NULL,
        indicador_codigo CHAR(2) NULL
    );

    BEGIN TRY
        ;WITH documentos_filiais_operacionais(documento) AS (
            SELECT N'51863654000180' UNION ALL
            SELECT N'51863654000260' UNION ALL
            SELECT N'60960473000162' UNION ALL
            SELECT N'60960473000243' UNION ALL
            SELECT N'60960473000596' UNION ALL
            SELECT N'60960473000677' UNION ALL
            SELECT N'60960473000758' UNION ALL
            SELECT N'60960473000839' UNION ALL
            SELECT N'60960473001134' UNION ALL
            SELECT N'60960473001304' UNION ALL
            SELECT N'60960473001568'
        ),
        pagadores_excluidos_cubagem(documento) AS (
            SELECT N'44699346000103' UNION ALL
            SELECT N'07668944000180' UNION ALL
            SELECT N'13190609000546' UNION ALL
            SELECT N'13190609000384' UNION ALL
            SELECT N'13190609000627' UNION ALL
            SELECT N'46928552000165' UNION ALL
            SELECT N'14675270007381' UNION ALL
            SELECT N'56643018010390' UNION ALL
            SELECT N'14675270000450' UNION ALL
            SELECT N'14675270000298' UNION ALL
            SELECT N'05396883001510' UNION ALL
            SELECT N'05396883000386' UNION ALL
            SELECT N'51602373000173' UNION ALL
            SELECT N'43829282000651' UNION ALL
            SELECT N'43829282000147' UNION ALL
            SELECT N'43829282000490' UNION ALL
            SELECT N'03944724000696' UNION ALL
            SELECT N'03944724000777' UNION ALL
            SELECT N'03944724000262' UNION ALL
            SELECT N'03944724000939' UNION ALL
            SELECT N'03944724000858' UNION ALL
            SELECT N'44381747000102' UNION ALL
            SELECT N'01459630000272' UNION ALL
            SELECT N'43996693003061' UNION ALL
            SELECT N'43996693000631' UNION ALL
            SELECT N'43996693000208' UNION ALL
            SELECT N'43996693002766' UNION ALL
            SELECT N'43996693002928' UNION ALL
            SELECT N'43996693002847' UNION ALL
            SELECT N'43996693000801' UNION ALL
            SELECT N'43996693000127' UNION ALL
            SELECT N'92599901000160' UNION ALL
            SELECT N'33064262000250' UNION ALL
            SELECT N'08862530000827' UNION ALL
            SELECT N'08862530000231' UNION ALL
            SELECT N'08862530000150' UNION ALL
            SELECT N'33064262000179' UNION ALL
            SELECT N'08862530000746' UNION ALL
            SELECT N'08862530001122' UNION ALL
            SELECT N'08862530001203'
        ),
        fretes_fonte AS (
            SELECT
                f.id AS frete_id_origem,
                f.corporation_sequence_number AS numero_minuta,
                f.servico_em AS data_frete,
                CAST(f.servico_em AS DATE) AS data_frete_date,
                COALESCE(CAST(f.data_previsao_entrega AS DATE), CAST(lc.predicted_delivery_at AS DATE)) AS data_previsao_entrega,
                CAST(COALESCE(f.fit_dpn_performance_finished_at, f.finished_at) AS DATE) AS data_finalizacao_performance,
                COALESCE(NULLIF(LTRIM(RTRIM(f.filial_nome)), N''), N'Nao informado') AS filial_emissora,
                NULLIF(LOWER(LTRIM(RTRIM(f.filial_nome))), N'') AS filial_emissora_key,
                COALESCE(
                    NULLIF(LTRIM(RTRIM(lc.destination_branch_nickname)), N''),
                    NULLIF(LTRIM(RTRIM(f.filial_nome)), N'')
                ) AS filial_performance,
                COALESCE(lc.destination_branch_key, f.filial_nome_key) AS filial_performance_key,
                COALESCE(
                    NULLIF(LTRIM(RTRIM(lc.destination_branch_nickname)), N''),
                    NULLIF(LTRIM(RTRIM(f.filial_nome)), N'')
                ) AS responsavel_regiao_destino,
                COALESCE(lc.destination_branch_key, f.filial_nome_key, N'sem_responsavel') AS responsavel_regiao_destino_key,
                COALESCE(NULLIF(LTRIM(RTRIM(lc.destination_location_name)), N''), f.destino_uf, N'SEM_REGIAO') AS regiao_destino,
                NULLIF(LTRIM(RTRIM(f.pagador_nome)), N'') AS pagador_nome,
                NULLIF(LTRIM(RTRIM(f.pagador_documento)), N'') AS pagador_documento,
                docs.pagador_documento_key,
                NULLIF(LTRIM(RTRIM(f.remetente_documento)), N'') AS remetente_documento,
                NULLIF(LTRIM(RTRIM(f.destinatario_documento)), N'') AS destinatario_documento,
                NULLIF(LTRIM(RTRIM(f.destino_cidade)), N'') AS destino,
                NULLIF(LTRIM(RTRIM(f.destino_cidade)), N'') AS destino_cidade,
                NULLIF(LTRIM(RTRIM(f.destino_uf)), N'') AS destino_uf,
                LOWER(NULLIF(LTRIM(RTRIM(
                    CASE f.status
                        WHEN N'pending' THEN N'pendente'
                        WHEN N'finished' THEN N'finalizado'
                        WHEN N'in_transit' THEN N'em transito'
                        WHEN N'standby' THEN N'aguardando'
                        WHEN N'manifested' THEN N'registrado'
                        WHEN N'occurrence_treatment' THEN N'tratamento de ocorrencia'
                        ELSE f.status
                    END
                )), N'')) AS status_norm,
                CASE
                    WHEN f.cte_id IS NOT NULL
                      OR NULLIF(LTRIM(RTRIM(f.chave_cte)), N'') IS NOT NULL
                      OR f.numero_cte IS NOT NULL
                      OR f.serie_cte IS NOT NULL THEN N'CT-E'
                    WHEN f.nfse_number IS NOT NULL
                      OR NULLIF(LTRIM(RTRIM(f.nfse_series)), N'') IS NOT NULL
                      OR NULLIF(LTRIM(RTRIM(f.nfse_xml_document)), N'') IS NOT NULL
                      OR NULLIF(LTRIM(RTRIM(f.nfse_integration_id)), N'') IS NOT NULL THEN N'NFS-E'
                    ELSE N'PENDENTE/NAO EMITIDO'
                END AS documento_oficial_tipo,
                UPPER(NULLIF(LTRIM(RTRIM(REPLACE(COALESCE(f.tipo_frete, N''), N'Freight::', N''))), N'')) AS tipo_frete,
                f.modal,
                f.valor_total,
                f.taxed_weight AS peso_taxado,
                f.real_weight AS peso_real,
                f.cubages_cubed_weight AS peso_cubado,
                f.total_cubic_volume AS total_m3,
                COALESCE(lc.invoices_volumes, f.invoices_total_volumes, 0) AS volumes,
                CASE
                    WHEN COALESCE(CAST(f.data_previsao_entrega AS DATE), CAST(lc.predicted_delivery_at AS DATE)) IS NULL
                      OR COALESCE(f.fit_dpn_performance_finished_at, f.finished_at) IS NULL THEN NULL
                    ELSE DATEDIFF(
                        DAY,
                        COALESCE(CAST(f.data_previsao_entrega AS DATE), CAST(lc.predicted_delivery_at AS DATE)),
                        CAST(COALESCE(f.fit_dpn_performance_finished_at, f.finished_at) AS DATE)
                    )
                END AS performance_diferenca_dias,
                CAST(COALESCE(f.excluido_na_origem, 0) AS BIT) AS excluido_na_origem,
                CAST(CASE WHEN COALESCE(f.cortesia, 0) = 1 THEN 1 ELSE 0 END AS BIT) AS is_cortesia,
                data_atualizacao.data_extracao
            FROM dbo.fretes AS f
            LEFT JOIN dbo.localizacao_cargas AS lc
                ON lc.sequence_number = f.corporation_sequence_number
               AND COALESCE(lc.excluido_na_origem, 0) = 0
            CROSS APPLY (
                SELECT UPPER(NULLIF(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
                    LTRIM(RTRIM(COALESCE(f.pagador_documento, N''))),
                    N'.', N''), N'-', N''), N'/', N''), N' ', N''), CHAR(9), N''), N'')) AS pagador_documento_key
            ) AS docs
            CROSS APPLY (
                SELECT MAX(valor) AS data_extracao
                FROM (VALUES (f.data_extracao), (lc.data_extracao)) AS atualizacoes(valor)
            ) AS data_atualizacao
            WHERE f.corporation_sequence_number IS NOT NULL
              AND UPPER(NULLIF(LTRIM(RTRIM(REPLACE(COALESCE(f.tipo_frete, N''), N'Freight::', N''))), N'')) <> N'COMPLEMENTAR'
        ),
        fretes_regras AS (
            SELECT
                ff.*,
                CAST(CASE WHEN ff.documento_oficial_tipo IN (N'CT-E', N'NFS-E') THEN 1 ELSE 0 END AS BIT) AS is_documento_emitido,
                CAST(CASE WHEN ff.status_norm IN (N'cancelado', N'cancelada', N'canceled', N'cancelled') THEN 1 ELSE 0 END AS BIT) AS is_cancelado,
                CAST(CASE WHEN COALESCE(ff.total_m3, 0) <> 0 THEN 1 ELSE 0 END AS BIT) AS is_cubado,
                CAST(CASE WHEN COALESCE(ff.peso_real, 0) > 0 THEN 1 ELSE 0 END AS BIT) AS is_peso_real_informado,
                CAST(CASE WHEN EXISTS (
                    SELECT 1
                    FROM pagadores_excluidos_cubagem p
                    WHERE p.documento = ff.pagador_documento_key
                ) THEN 1 ELSE 0 END AS BIT) AS is_pagador_excluido_cubagem,
                CAST(CASE
                    WHEN ff.excluido_na_origem = 1 THEN 0
                    WHEN ff.is_cortesia = 1 THEN 0
                    WHEN ff.documento_oficial_tipo NOT IN (N'CT-E', N'NFS-E')
                     AND EXISTS (
                        SELECT 1
                        FROM documentos_filiais_operacionais d
                        WHERE d.documento = ff.pagador_documento_key
                     ) THEN 0
                    WHEN ff.documento_oficial_tipo NOT IN (N'CT-E', N'NFS-E')
                     AND COALESCE(ff.valor_total, 0) <= 0.01 THEN 0
                    WHEN ff.documento_oficial_tipo NOT IN (N'CT-E', N'NFS-E')
                     AND ff.tipo_frete LIKE N'%SUBSTITUTE%'
                     AND ff.status_norm LIKE N'%pendente%' THEN 0
                    ELSE 1
                END AS BIT) AS is_elegivel_operacional,
                CAST(CASE
                    WHEN ff.excluido_na_origem = 1 THEN 0
                    WHEN ff.is_cortesia = 1 THEN 0
                    WHEN ff.documento_oficial_tipo NOT IN (N'CT-E', N'NFS-E')
                     AND EXISTS (
                        SELECT 1
                        FROM documentos_filiais_operacionais d
                        WHERE d.documento = ff.pagador_documento_key
                     ) THEN 0
                    WHEN ff.documento_oficial_tipo NOT IN (N'CT-E', N'NFS-E')
                     AND COALESCE(ff.valor_total, 0) <= 0.01 THEN 0
                    WHEN ff.documento_oficial_tipo NOT IN (N'CT-E', N'NFS-E')
                     AND ff.tipo_frete LIKE N'%SUBSTITUTE%'
                     AND ff.status_norm LIKE N'%pendente%' THEN 0
                    WHEN COALESCE(ff.valor_total, 0) <= 0.01 THEN 0
                    ELSE 1
                END AS BIT) AS is_elegivel_operacional_com_valor
            FROM fretes_fonte AS ff
        ),
        fretes_ranked AS (
            SELECT
                fr.*,
                ROW_NUMBER() OVER (
                    PARTITION BY fr.numero_minuta
                    ORDER BY
                        CASE WHEN fr.data_finalizacao_performance IS NOT NULL THEN 4 ELSE 0 END
                      + CASE WHEN fr.performance_diferenca_dias IS NOT NULL THEN 3 ELSE 0 END
                      + CASE WHEN fr.filial_performance_key IS NOT NULL THEN 2 ELSE 0 END
                      + CASE WHEN fr.data_extracao IS NOT NULL THEN 1 ELSE 0 END DESC,
                        fr.data_extracao DESC,
                        fr.frete_id_origem DESC
                ) AS rn
            FROM fretes_regras AS fr
        ),
        fretes_deduplicados AS (
            SELECT *
            FROM fretes_ranked
            WHERE rn = 1
        ),
        indicadores AS (
            SELECT
                indicador.indicador_codigo,
                indicador.data_referencia,
                YEAR(indicador.data_referencia) * 100 + MONTH(indicador.data_referencia) AS data_referencia_yyyymm,
                fd.numero_minuta,
                fd.frete_id_origem,
                fd.data_frete,
                fd.data_frete_date,
                fd.data_previsao_entrega,
                fd.data_finalizacao_performance,
                fd.filial_emissora,
                fd.filial_emissora_key,
                fd.filial_performance,
                fd.filial_performance_key,
                fd.responsavel_regiao_destino,
                fd.responsavel_regiao_destino_key,
                fd.regiao_destino,
                fd.pagador_nome,
                fd.pagador_documento,
                fd.pagador_documento_key,
                fd.remetente_documento,
                fd.destinatario_documento,
                fd.destino,
                fd.destino_cidade,
                fd.destino_uf,
                fd.status_norm,
                fd.documento_oficial_tipo,
                fd.tipo_frete,
                fd.modal,
                fd.valor_total,
                fd.peso_taxado,
                fd.peso_real,
                fd.peso_cubado,
                fd.total_m3,
                fd.volumes,
                TRY_CONVERT(SMALLINT, fd.performance_diferenca_dias) AS performance_diferenca_dias,
                CASE
                    WHEN fd.performance_diferenca_dias IS NULL THEN N'EM ABERTO'
                    WHEN fd.performance_diferenca_dias <= 0 THEN N'NO PRAZO'
                    ELSE N'FORA DO PRAZO'
                END AS performance_status,
                CASE
                    WHEN fd.performance_diferenca_dias IS NULL THEN 0
                    WHEN fd.performance_diferenca_dias <= 0 THEN 1
                    ELSE 2
                END AS performance_status_codigo,
                CAST(CASE WHEN fd.performance_diferenca_dias <= 0 THEN 1 ELSE 0 END AS BIT) AS is_no_prazo,
                CAST(CASE WHEN fd.performance_diferenca_dias > 0 THEN 1 ELSE 0 END AS BIT) AS is_fora_prazo,
                fd.is_cubado,
                fd.is_peso_real_informado,
                fd.is_cancelado,
                fd.is_cortesia,
                fd.is_documento_emitido,
                fd.is_elegivel_operacional,
                fd.is_elegivel_operacional_com_valor,
                fd.is_pagador_excluido_cubagem,
                CAST(CASE
                    WHEN fd.excluido_na_origem = 1 THEN 0
                    WHEN indicador.indicador_codigo = 'PE'
                     AND fd.data_previsao_entrega IS NOT NULL
                     AND fd.filial_performance_key IS NOT NULL
                     AND fd.is_cancelado = 0
                     AND fd.is_elegivel_operacional = 1 THEN 1
                    WHEN indicador.indicador_codigo = 'CB'
                     AND fd.data_frete_date IS NOT NULL
                     AND fd.is_cancelado = 0
                     AND fd.is_elegivel_operacional_com_valor = 1
                     AND fd.is_pagador_excluido_cubagem = 0 THEN 1
                    ELSE 0
                END AS BIT) AS is_linha_valida_indicador,
                fd.excluido_na_origem,
                CASE WHEN fd.excluido_na_origem = 1 THEN @SnapshotEm ELSE NULL END AS data_exclusao_origem,
                @SnapshotEm AS ultima_reconciliacao_origem_em,
                fd.data_extracao,
                @SnapshotEm AS snapshot_em
            FROM fretes_deduplicados AS fd
            CROSS APPLY (VALUES
                (CAST('PE' AS CHAR(2)), fd.data_previsao_entrega),
                (CAST('CB' AS CHAR(2)), fd.data_frete_date)
            ) AS indicador(indicador_codigo, data_referencia)
            WHERE indicador.data_referencia IS NOT NULL
              AND (@DataInicio IS NULL OR indicador.data_referencia >= @DataInicio)
              AND (@DataFimExclusivo IS NULL OR indicador.data_referencia < @DataFimExclusivo)
        ),
        origem AS (
            SELECT
                i.*,
                CONVERT(BINARY(32), HASHBYTES('SHA2_256', CONCAT_WS(N'|',
                    COALESCE(i.indicador_codigo, N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(30), i.data_referencia, 126), N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(20), i.data_referencia_yyyymm), N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(30), i.numero_minuta), N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(30), i.frete_id_origem), N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(48), i.data_frete, 127), N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(30), i.data_frete_date, 126), N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(30), i.data_previsao_entrega, 126), N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(30), i.data_finalizacao_performance, 126), N'__NULL__'),
                    COALESCE(i.filial_emissora, N'__NULL__'),
                    COALESCE(i.filial_emissora_key, N'__NULL__'),
                    COALESCE(i.filial_performance, N'__NULL__'),
                    COALESCE(i.filial_performance_key, N'__NULL__'),
                    COALESCE(i.responsavel_regiao_destino, N'__NULL__'),
                    COALESCE(i.responsavel_regiao_destino_key, N'__NULL__'),
                    COALESCE(i.regiao_destino, N'__NULL__'),
                    COALESCE(i.pagador_nome, N'__NULL__'),
                    COALESCE(i.pagador_documento, N'__NULL__'),
                    COALESCE(i.pagador_documento_key, N'__NULL__'),
                    COALESCE(i.remetente_documento, N'__NULL__'),
                    COALESCE(i.destinatario_documento, N'__NULL__'),
                    COALESCE(i.destino, N'__NULL__'),
                    COALESCE(i.destino_cidade, N'__NULL__'),
                    COALESCE(i.destino_uf, N'__NULL__'),
                    COALESCE(i.status_norm, N'__NULL__'),
                    COALESCE(i.documento_oficial_tipo, N'__NULL__'),
                    COALESCE(i.tipo_frete, N'__NULL__'),
                    COALESCE(i.modal, N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(40), i.valor_total), N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(40), i.peso_taxado), N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(40), i.peso_real), N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(40), i.peso_cubado), N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(40), i.total_m3), N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(20), i.volumes), N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(20), i.performance_diferenca_dias), N'__NULL__'),
                    COALESCE(i.performance_status, N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(10), i.performance_status_codigo), N'__NULL__'),
                    CONVERT(NVARCHAR(1), i.is_no_prazo),
                    CONVERT(NVARCHAR(1), i.is_fora_prazo),
                    CONVERT(NVARCHAR(1), i.is_cubado),
                    CONVERT(NVARCHAR(1), i.is_peso_real_informado),
                    CONVERT(NVARCHAR(1), i.is_cancelado),
                    CONVERT(NVARCHAR(1), i.is_cortesia),
                    CONVERT(NVARCHAR(1), i.is_documento_emitido),
                    CONVERT(NVARCHAR(1), i.is_elegivel_operacional),
                    CONVERT(NVARCHAR(1), i.is_elegivel_operacional_com_valor),
                    CONVERT(NVARCHAR(1), i.is_pagador_excluido_cubagem),
                    CONVERT(NVARCHAR(1), i.is_linha_valida_indicador),
                    CONVERT(NVARCHAR(1), i.excluido_na_origem),
                    COALESCE(CONVERT(NVARCHAR(30), i.data_extracao, 126), N'__NULL__')
                ))) AS hash_linha
            FROM indicadores AS i
        )
        MERGE dbo.fato_gestao_vista_fretes WITH (HOLDLOCK) AS target
        USING origem AS source
           ON target.indicador_codigo = source.indicador_codigo
          AND target.numero_minuta = source.numero_minuta
        WHEN MATCHED
         AND (
                target.hash_linha IS NULL
             OR source.hash_linha IS NULL
             OR target.hash_linha <> source.hash_linha
         )
            THEN UPDATE SET
                data_referencia = source.data_referencia,
                data_referencia_yyyymm = source.data_referencia_yyyymm,
                frete_id_origem = source.frete_id_origem,
                data_frete = source.data_frete,
                data_frete_date = source.data_frete_date,
                data_previsao_entrega = source.data_previsao_entrega,
                data_finalizacao_performance = source.data_finalizacao_performance,
                filial_emissora = source.filial_emissora,
                filial_emissora_key = source.filial_emissora_key,
                filial_performance = source.filial_performance,
                filial_performance_key = source.filial_performance_key,
                responsavel_regiao_destino = source.responsavel_regiao_destino,
                responsavel_regiao_destino_key = source.responsavel_regiao_destino_key,
                regiao_destino = source.regiao_destino,
                pagador_nome = source.pagador_nome,
                pagador_documento = source.pagador_documento,
                pagador_documento_key = source.pagador_documento_key,
                remetente_documento = source.remetente_documento,
                destinatario_documento = source.destinatario_documento,
                destino = source.destino,
                destino_cidade = source.destino_cidade,
                destino_uf = source.destino_uf,
                status_norm = source.status_norm,
                documento_oficial_tipo = source.documento_oficial_tipo,
                tipo_frete = source.tipo_frete,
                modal = source.modal,
                valor_total = source.valor_total,
                peso_taxado = source.peso_taxado,
                peso_real = source.peso_real,
                peso_cubado = source.peso_cubado,
                total_m3 = source.total_m3,
                volumes = source.volumes,
                performance_diferenca_dias = source.performance_diferenca_dias,
                performance_status = source.performance_status,
                performance_status_codigo = source.performance_status_codigo,
                is_no_prazo = source.is_no_prazo,
                is_fora_prazo = source.is_fora_prazo,
                is_cubado = source.is_cubado,
                is_peso_real_informado = source.is_peso_real_informado,
                is_cancelado = source.is_cancelado,
                is_cortesia = source.is_cortesia,
                is_documento_emitido = source.is_documento_emitido,
                is_elegivel_operacional = source.is_elegivel_operacional,
                is_elegivel_operacional_com_valor = source.is_elegivel_operacional_com_valor,
                is_pagador_excluido_cubagem = source.is_pagador_excluido_cubagem,
                is_linha_valida_indicador = source.is_linha_valida_indicador,
                excluido_na_origem = source.excluido_na_origem,
                data_exclusao_origem = source.data_exclusao_origem,
                ultima_reconciliacao_origem_em = source.ultima_reconciliacao_origem_em,
                data_extracao = source.data_extracao,
                snapshot_em = source.snapshot_em,
                hash_linha = source.hash_linha
        WHEN NOT MATCHED BY TARGET
            THEN INSERT (
                indicador_codigo,
                data_referencia,
                data_referencia_yyyymm,
                numero_minuta,
                frete_id_origem,
                data_frete,
                data_frete_date,
                data_previsao_entrega,
                data_finalizacao_performance,
                filial_emissora,
                filial_emissora_key,
                filial_performance,
                filial_performance_key,
                responsavel_regiao_destino,
                responsavel_regiao_destino_key,
                regiao_destino,
                pagador_nome,
                pagador_documento,
                pagador_documento_key,
                remetente_documento,
                destinatario_documento,
                destino,
                destino_cidade,
                destino_uf,
                status_norm,
                documento_oficial_tipo,
                tipo_frete,
                modal,
                valor_total,
                peso_taxado,
                peso_real,
                peso_cubado,
                total_m3,
                volumes,
                performance_diferenca_dias,
                performance_status,
                performance_status_codigo,
                is_no_prazo,
                is_fora_prazo,
                is_cubado,
                is_peso_real_informado,
                is_cancelado,
                is_cortesia,
                is_documento_emitido,
                is_elegivel_operacional,
                is_elegivel_operacional_com_valor,
                is_pagador_excluido_cubagem,
                is_linha_valida_indicador,
                excluido_na_origem,
                data_exclusao_origem,
                ultima_reconciliacao_origem_em,
                data_extracao,
                snapshot_em,
                hash_linha
            )
            VALUES (
                source.indicador_codigo,
                source.data_referencia,
                source.data_referencia_yyyymm,
                source.numero_minuta,
                source.frete_id_origem,
                source.data_frete,
                source.data_frete_date,
                source.data_previsao_entrega,
                source.data_finalizacao_performance,
                source.filial_emissora,
                source.filial_emissora_key,
                source.filial_performance,
                source.filial_performance_key,
                source.responsavel_regiao_destino,
                source.responsavel_regiao_destino_key,
                source.regiao_destino,
                source.pagador_nome,
                source.pagador_documento,
                source.pagador_documento_key,
                source.remetente_documento,
                source.destinatario_documento,
                source.destino,
                source.destino_cidade,
                source.destino_uf,
                source.status_norm,
                source.documento_oficial_tipo,
                source.tipo_frete,
                source.modal,
                source.valor_total,
                source.peso_taxado,
                source.peso_real,
                source.peso_cubado,
                source.total_m3,
                source.volumes,
                source.performance_diferenca_dias,
                source.performance_status,
                source.performance_status_codigo,
                source.is_no_prazo,
                source.is_fora_prazo,
                source.is_cubado,
                source.is_peso_real_informado,
                source.is_cancelado,
                source.is_cortesia,
                source.is_documento_emitido,
                source.is_elegivel_operacional,
                source.is_elegivel_operacional_com_valor,
                source.is_pagador_excluido_cubagem,
                source.is_linha_valida_indicador,
                source.excluido_na_origem,
                source.data_exclusao_origem,
                source.ultima_reconciliacao_origem_em,
                source.data_extracao,
                source.snapshot_em,
                source.hash_linha
            )
        WHEN NOT MATCHED BY SOURCE
         AND @MarcarAusentesComoExcluidos = 1
         AND target.indicador_codigo IN ('PE', 'CB')
         AND target.excluido_na_origem = 0
         AND (
                @cargaCompleta = 1
             OR (
                    target.data_referencia >= @DataInicio
                AND target.data_referencia < @DataFimExclusivo
             )
         )
            THEN UPDATE SET
                excluido_na_origem = 1,
                is_linha_valida_indicador = 0,
                is_no_prazo = 0,
                is_fora_prazo = 0,
                is_cubado = 0,
                data_exclusao_origem = COALESCE(target.data_exclusao_origem, @SnapshotEm),
                ultima_reconciliacao_origem_em = @SnapshotEm,
                snapshot_em = @SnapshotEm,
                hash_linha = NULL
        OUTPUT $action, inserted.indicador_codigo
        INTO @resultado (merge_action, indicador_codigo);

        EXEC sys.sp_releaseapplock
            @Resource = N'dbo.sp_carga_fato_gestao_vista_fretes',
            @LockOwner = N'Session';

        SELECT
            indicador_codigo,
            SUM(CASE WHEN merge_action = N'INSERT' THEN 1 ELSE 0 END) AS linhas_inseridas,
            SUM(CASE WHEN merge_action = N'UPDATE' THEN 1 ELSE 0 END) AS linhas_atualizadas,
            @SnapshotEm AS snapshot_em
        FROM @resultado
        GROUP BY indicador_codigo
        ORDER BY indicador_codigo;
    END TRY
    BEGIN CATCH
        EXEC sys.sp_releaseapplock
            @Resource = N'dbo.sp_carga_fato_gestao_vista_fretes',
            @LockOwner = N'Session';
        THROW;
    END CATCH;
END;
GO

PRINT 'Procedure dbo.sp_carga_fato_gestao_vista_fretes criada/atualizada com sucesso.';
GO
