-- ============================================================================
-- Procedure de carga incremental da fato granular de Faturamento de Fretes
-- ============================================================================

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;
GO

CREATE OR ALTER PROCEDURE dbo.sp_carga_fato_fretes_faturamento
    @DataInicio DATE = NULL,
    @DataFimExclusivo DATE = NULL,
    @MarcarAusentesComoExcluidos BIT = 1,
    @SnapshotEm DATETIME2(0) = NULL
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    IF OBJECT_ID(N'dbo.fato_fretes_faturamento', N'U') IS NULL
        THROW 51041, 'Tabela dbo.fato_fretes_faturamento nao encontrada. Execute a migration 031 antes da carga.', 1;

    IF OBJECT_ID(N'dbo.fretes', N'U') IS NULL
        THROW 51042, 'Tabela dbo.fretes nao encontrada. Carga da fato de faturamento abortada.', 1;

    IF OBJECT_ID(N'dbo.faturas_por_cliente', N'U') IS NULL
        THROW 51043, 'Tabela dbo.faturas_por_cliente nao encontrada. A carga precisa dela para validar status real de CT-e.', 1;

    IF OBJECT_ID(N'dbo.dim_calendario', N'U') IS NULL
        THROW 51048, 'Tabela dbo.dim_calendario nao encontrada. Execute a migration 039 antes da carga.', 1;

    IF (@DataInicio IS NULL AND @DataFimExclusivo IS NOT NULL)
       OR (@DataInicio IS NOT NULL AND @DataFimExclusivo IS NULL)
        THROW 51044, 'Informe @DataInicio e @DataFimExclusivo juntos ou deixe ambos nulos para carga completa.', 1;

    IF @DataInicio IS NOT NULL AND @DataInicio >= @DataFimExclusivo
        THROW 51045, '@DataInicio deve ser menor que @DataFimExclusivo.', 1;

    DECLARE @cargaCompleta BIT = CASE WHEN @DataInicio IS NULL AND @DataFimExclusivo IS NULL THEN 1 ELSE 0 END;
    DECLARE @InicioOffset DATETIMEOFFSET = CASE WHEN @DataInicio IS NULL THEN NULL ELSE CONVERT(DATETIMEOFFSET, @DataInicio) END;
    DECLARE @FimOffset DATETIMEOFFSET = CASE WHEN @DataFimExclusivo IS NULL THEN NULL ELSE CONVERT(DATETIMEOFFSET, @DataFimExclusivo) END;

    SET @SnapshotEm = COALESCE(@SnapshotEm, SYSUTCDATETIME());

    DECLARE @lockResult INT;
    EXEC @lockResult = sys.sp_getapplock
        @Resource = N'dbo.sp_carga_fato_fretes_faturamento',
        @LockMode = N'Exclusive',
        @LockOwner = N'Session',
        @LockTimeout = 0;

    IF @lockResult < 0
        THROW 51046, 'Carga da fato de faturamento ja esta em execucao por outra sessao.', 1;

    DECLARE @resultado TABLE (
        merge_action NVARCHAR(10) NOT NULL
    );

    BEGIN TRY
        ;WITH faturas_cte_ranked AS (
            SELECT
                chave.cte_chave_key,
                NULLIF(LTRIM(RTRIM(fpc.status_cte)), N'') AS status_cte_real,
                LEFT(NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(MAX), fpc.status_cte_result))), N''), 4000) AS status_cte_result,
                fpc.data_emissao_cte,
                fpc.data_extracao,
                ROW_NUMBER() OVER (
                    PARTITION BY chave.cte_chave_key
                    ORDER BY
                        CASE
                            WHEN NULLIF(LTRIM(RTRIM(fpc.status_cte)), N'') IS NOT NULL THEN 0
                            WHEN NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(MAX), fpc.status_cte_result))), N'') IS NOT NULL THEN 1
                            ELSE 2
                        END,
                        fpc.data_extracao DESC,
                        fpc.data_emissao_cte DESC,
                        fpc.unique_id DESC
                ) AS rn
            FROM dbo.faturas_por_cliente AS fpc
            CROSS APPLY (
                SELECT UPPER(NULLIF(REPLACE(REPLACE(LTRIM(RTRIM(COALESCE(fpc.chave_cte, N''))), N' ', N''), CHAR(9), N''), N'')) AS cte_chave_key
            ) AS chave
            WHERE chave.cte_chave_key IS NOT NULL
              AND COALESCE(fpc.excluido_na_origem, 0) = 0
        ),
        faturas_cte AS (
            SELECT
                cte_chave_key,
                status_cte_real,
                status_cte_result,
                data_extracao
            FROM faturas_cte_ranked
            WHERE rn = 1
        ),
        fretes_fonte AS (
            SELECT
                f.id AS frete_id,
                f.corporation_sequence_number AS numero_minuta,
                TODATETIMEOFFSET(
                    CAST(cal.data_referencia_faturamento AS DATETIME2(0)),
                    DATEPART(TZOFFSET, f.data_referencia_faturamento)
                ) AS data_referencia_faturamento,
                cal.data_referencia_faturamento AS data_referencia_faturamento_date,
                YEAR(cal.data_referencia_faturamento) * 100
                    + MONTH(cal.data_referencia_faturamento) AS data_referencia_faturamento_yyyymm,
                f.data_referencia_faturamento AS data_referencia_faturamento_real,
                CAST(f.data_referencia_faturamento AS DATE) AS data_referencia_faturamento_real_date,
                YEAR(CAST(f.data_referencia_faturamento AS DATE)) * 100
                    + MONTH(CAST(f.data_referencia_faturamento AS DATE)) AS data_referencia_faturamento_real_yyyymm,
                CAST(CASE WHEN cal.data_referencia_faturamento <> CAST(f.data_referencia_faturamento AS DATE) THEN 1 ELSE 0 END AS BIT)
                    AS is_data_faturamento_retroagida,
                f.servico_em AS data_frete,
                CAST(f.servico_em AS DATE) AS data_frete_date,
                f.cte_issued_at AS data_emissao_cte,
                f.criado_em,
                f.id_corporacao AS filial_id,
                NULLIF(LTRIM(RTRIM(f.filial_nome)), N'') AS filial_nome,
                COALESCE(f.filial_nome_key, NULLIF(LOWER(LTRIM(RTRIM(f.filial_nome))), N'')) AS filial_key,
                NULLIF(LTRIM(RTRIM(f.filial_apelido)), N'') AS filial_apelido,
                NULLIF(LTRIM(RTRIM(f.filial_cnpj)), N'') AS filial_cnpj,
                NULLIF(LTRIM(RTRIM(lc.destination_branch_nickname)), N'') AS responsavel_regiao_destino,
                COALESCE(lc.destination_branch_key, N'sem_responsavel') AS responsavel_regiao_destino_key,
                COALESCE(NULLIF(LTRIM(RTRIM(lc.destination_location_name)), N''), f.destino_uf, N'SEM_REGIAO') AS regiao_destino,
                f.pagador_id,
                NULLIF(LTRIM(RTRIM(f.pagador_nome)), N'') AS pagador_nome,
                NULLIF(LTRIM(RTRIM(f.pagador_documento)), N'') AS pagador_documento,
                docs.pagador_documento_key,
                f.remetente_id,
                NULLIF(LTRIM(RTRIM(f.remetente_nome)), N'') AS remetente_nome,
                NULLIF(LTRIM(RTRIM(f.remetente_documento)), N'') AS remetente_documento,
                f.destinatario_id,
                NULLIF(LTRIM(RTRIM(f.destinatario_nome)), N'') AS destinatario_nome,
                NULLIF(LTRIM(RTRIM(f.destinatario_documento)), N'') AS destinatario_documento,
                NULLIF(LTRIM(RTRIM(f.origem_cidade)), N'') AS origem_cidade,
                NULLIF(LTRIM(RTRIM(f.origem_uf)), N'') AS origem_uf,
                NULLIF(LTRIM(RTRIM(f.destino_cidade)), N'') AS destino_cidade,
                NULLIF(LTRIM(RTRIM(f.destino_uf)), N'') AS destino_uf,
                NULLIF(LTRIM(RTRIM(f.status)), N'') AS status_frete,
                status_norm.status_frete_norm,
                NULLIF(LTRIM(RTRIM(f.classificacao_nome)), N'') AS classificacao_nome,
                NULLIF(LTRIM(RTRIM(f.centro_custo_nome)), N'') AS centro_custo_nome,
                NULLIF(LTRIM(RTRIM(REPLACE(COALESCE(f.tipo_frete, N''), N'Freight::', N''))), N'') AS tipo_frete,
                NULLIF(LTRIM(RTRIM(f.modal)), N'') AS modal,
                NULLIF(LTRIM(RTRIM(f.modal_cte)), N'') AS modal_cte,
                NULLIF(LTRIM(RTRIM(f.tabela_preco_nome)), N'') AS tabela_preco_nome,
                NULLIF(LTRIM(RTRIM(f.usuario_nome)), N'') AS usuario_nome,
                NULLIF(LTRIM(RTRIM(f.reference_number)), N'') AS reference_number,
                f.cte_id,
                NULLIF(LTRIM(RTRIM(f.chave_cte)), N'') AS chave_cte,
                f.numero_cte,
                f.serie_cte,
                NULLIF(LTRIM(RTRIM(f.cte_emission_type)), N'') AS cte_emission_type,
                f.cte_created_at,
                fpc.status_cte_real,
                fpc.status_cte_result,
                LOWER(NULLIF(LTRIM(RTRIM(fpc.status_cte_real)), N'')) AS status_cte_norm,
                CAST(CASE
                    WHEN f.cte_id IS NOT NULL
                      OR NULLIF(LTRIM(RTRIM(f.chave_cte)), N'') IS NOT NULL
                      OR f.numero_cte IS NOT NULL
                      OR f.serie_cte IS NOT NULL THEN 1
                    ELSE 0
                END AS BIT) AS is_documento_cte,
                f.nfse_number,
                NULLIF(LTRIM(RTRIM(f.nfse_series)), N'') AS nfse_series,
                NULLIF(LTRIM(RTRIM(f.nfse_status)), N'') AS nfse_status,
                f.nfse_issued_at,
                CAST(CASE WHEN COALESCE(f.cortesia, 0) = 1 THEN 1 ELSE 0 END AS BIT) AS is_cortesia,
                CAST(CASE WHEN COALESCE(f.is_elegivel_faturamento, 1) = 1 THEN 1 ELSE 0 END AS BIT) AS is_elegivel_origem,
                COALESCE(f.subtotal, 0) AS valor_frete_original,
                COALESCE(f.valor_total, 0) AS receita_bruta_original,
                f.valor_notas,
                f.peso_notas,
                f.taxed_weight AS peso_taxado,
                f.real_weight AS peso_real,
                f.cubages_cubed_weight AS peso_cubado,
                f.total_cubic_volume AS total_m3,
                COALESCE(lc.invoices_volumes, f.invoices_total_volumes, 0) AS volumes,
                CAST(COALESCE(f.excluido_na_origem, 0) AS BIT) AS excluido_na_origem,
                data_atualizacao.data_extracao_origem,
                fpc.data_extracao AS data_extracao_faturas
            FROM dbo.fretes AS f
            LEFT JOIN dbo.localizacao_cargas AS lc
                ON lc.sequence_number = f.corporation_sequence_number
               AND COALESCE(lc.excluido_na_origem, 0) = 0
            INNER JOIN dbo.dim_calendario AS cal
                ON cal.data = CAST(f.data_referencia_faturamento AS DATE)
            CROSS APPLY (
                SELECT UPPER(NULLIF(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
                    LTRIM(RTRIM(COALESCE(f.pagador_documento, N''))),
                    N'.', N''), N'-', N''), N'/', N''), N' ', N''), CHAR(9), N''), N'')) AS pagador_documento_key
            ) AS docs
            CROSS APPLY (
                SELECT LOWER(NULLIF(LTRIM(RTRIM(
                    CASE f.status
                        WHEN N'pending' THEN N'pendente'
                        WHEN N'finished' THEN N'finalizado'
                        WHEN N'in_transit' THEN N'em transito'
                        WHEN N'standby' THEN N'aguardando'
                        WHEN N'manifested' THEN N'registrado'
                        WHEN N'occurrence_treatment' THEN N'tratamento de ocorrencia'
                        ELSE f.status
                    END
                )), N'')) AS status_frete_norm
            ) AS status_norm
            OUTER APPLY (
                SELECT MAX(valor) AS data_extracao_origem
                FROM (VALUES (f.data_extracao), (lc.data_extracao)) AS atualizacoes(valor)
            ) AS data_atualizacao
            LEFT JOIN faturas_cte AS fpc
                ON fpc.cte_chave_key = UPPER(NULLIF(REPLACE(REPLACE(LTRIM(RTRIM(COALESCE(f.chave_cte, N''))), N' ', N''), CHAR(9), N''), N''))
            WHERE f.id IS NOT NULL
              AND f.data_referencia_faturamento IS NOT NULL
              AND (@InicioOffset IS NULL OR f.data_referencia_faturamento >= @InicioOffset)
              AND (@FimOffset IS NULL OR f.data_referencia_faturamento < @FimOffset)
        ),
        fretes_regras AS (
            SELECT
                ff.*,
                CAST(CASE
                    WHEN CONVERT(NVARCHAR(MAX), ISNULL(ff.classificacao_nome, N'')) COLLATE Latin1_General_CI_AI LIKE N'%bloqueio%'
                     AND (
                        CONVERT(NVARCHAR(MAX), ISNULL(ff.classificacao_nome, N'')) COLLATE Latin1_General_CI_AI LIKE N'%anulacao%'
                        OR CONVERT(NVARCHAR(MAX), ISNULL(ff.classificacao_nome, N'')) COLLATE Latin1_General_CI_AI LIKE N'%isolamento%'
                     )
                    THEN 1
                    ELSE 0
                END AS BIT) AS is_bloqueio_faturamento,
                CAST(CASE
                    WHEN ff.status_cte_norm COLLATE Latin1_General_CI_AI IN (N'cancelado', N'cancelada', N'canceled', N'cancelled') THEN 1
                    WHEN ff.status_cte_norm COLLATE Latin1_General_CI_AI LIKE N'%cancelad%' THEN 1
                    WHEN ff.status_cte_norm COLLATE Latin1_General_CI_AI LIKE N'%cancell%' THEN 1
                    WHEN CONVERT(NVARCHAR(MAX), ISNULL(ff.status_cte_result, N'')) COLLATE Latin1_General_CI_AI LIKE N'%cancelad%' THEN 1
                    WHEN CONVERT(NVARCHAR(MAX), ISNULL(ff.status_cte_result, N'')) COLLATE Latin1_General_CI_AI LIKE N'%cancell%' THEN 1
                    WHEN ff.is_documento_cte = 1
                     AND ff.status_frete_norm COLLATE Latin1_General_CI_AI IN (N'cancelado', N'cancelada', N'canceled', N'cancelled') THEN 1
                    ELSE 0
                END AS BIT) AS is_cte_cancelado,
                CASE
                    WHEN ff.status_cte_norm COLLATE Latin1_General_CI_AI LIKE N'%cancelad%'
                      OR ff.status_cte_norm COLLATE Latin1_General_CI_AI LIKE N'%cancell%' THEN N'faturas_por_cliente.status_cte'
                    WHEN CONVERT(NVARCHAR(MAX), ISNULL(ff.status_cte_result, N'')) COLLATE Latin1_General_CI_AI LIKE N'%cancelad%'
                      OR CONVERT(NVARCHAR(MAX), ISNULL(ff.status_cte_result, N'')) COLLATE Latin1_General_CI_AI LIKE N'%cancell%' THEN N'faturas_por_cliente.status_cte_result'
                    WHEN ff.is_documento_cte = 1
                     AND ff.status_frete_norm COLLATE Latin1_General_CI_AI IN (N'cancelado', N'cancelada', N'canceled', N'cancelled') THEN N'fretes.status'
                    ELSE NULL
                END AS cte_cancelado_evidencia
            FROM fretes_fonte AS ff
        ),
        origem_regrada AS (
            SELECT
                fr.*,
                CAST(CASE
                    WHEN fr.excluido_na_origem = 1 THEN 0
                    WHEN fr.data_referencia_faturamento IS NULL THEN 0
                    WHEN fr.is_cte_cancelado = 1 THEN 0
                    WHEN fr.is_cortesia = 1 THEN 0
                    WHEN fr.is_bloqueio_faturamento = 1 THEN 0
                    WHEN fr.is_elegivel_origem = 0 THEN 0
                    ELSE 1
                END AS BIT) AS is_elegivel_faturamento
            FROM fretes_regras AS fr
        ),
        origem AS (
            SELECT
                o.*,
                CASE WHEN o.is_elegivel_faturamento = 1 THEN o.valor_frete_original ELSE CONVERT(DECIMAL(18, 2), 0) END AS valor_frete,
                CASE WHEN o.is_elegivel_faturamento = 1 THEN o.receita_bruta_original ELSE CONVERT(DECIMAL(18, 2), 0) END AS receita_bruta,
                CASE WHEN o.excluido_na_origem = 1 THEN @SnapshotEm ELSE NULL END AS data_exclusao_origem,
                @SnapshotEm AS ultima_reconciliacao_origem_em,
                @SnapshotEm AS snapshot_em,
                CONVERT(BINARY(32), HASHBYTES('SHA2_256', CONCAT_WS(N'|',
                    COALESCE(CONVERT(NVARCHAR(30), o.frete_id), N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(30), o.numero_minuta), N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(48), o.data_referencia_faturamento, 127), N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(30), o.data_referencia_faturamento_date, 126), N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(10), o.data_referencia_faturamento_yyyymm), N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(48), o.data_referencia_faturamento_real, 127), N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(30), o.data_referencia_faturamento_real_date, 126), N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(10), o.data_referencia_faturamento_real_yyyymm), N'__NULL__'),
                    CONVERT(NVARCHAR(1), o.is_data_faturamento_retroagida),
                    COALESCE(CONVERT(NVARCHAR(48), o.data_frete, 127), N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(30), o.data_frete_date, 126), N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(48), o.data_emissao_cte, 127), N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(48), o.criado_em, 127), N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(30), o.filial_id), N'__NULL__'),
                    COALESCE(o.filial_nome, N'__NULL__'),
                    COALESCE(o.filial_key, N'__NULL__'),
                    COALESCE(o.filial_apelido, N'__NULL__'),
                    COALESCE(o.filial_cnpj, N'__NULL__'),
                    COALESCE(o.responsavel_regiao_destino, N'__NULL__'),
                    COALESCE(o.responsavel_regiao_destino_key, N'__NULL__'),
                    COALESCE(o.regiao_destino, N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(30), o.pagador_id), N'__NULL__'),
                    COALESCE(o.pagador_nome, N'__NULL__'),
                    COALESCE(o.pagador_documento, N'__NULL__'),
                    COALESCE(o.pagador_documento_key, N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(30), o.remetente_id), N'__NULL__'),
                    COALESCE(o.remetente_nome, N'__NULL__'),
                    COALESCE(o.remetente_documento, N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(30), o.destinatario_id), N'__NULL__'),
                    COALESCE(o.destinatario_nome, N'__NULL__'),
                    COALESCE(o.destinatario_documento, N'__NULL__'),
                    COALESCE(o.origem_cidade, N'__NULL__'),
                    COALESCE(o.origem_uf, N'__NULL__'),
                    COALESCE(o.destino_cidade, N'__NULL__'),
                    COALESCE(o.destino_uf, N'__NULL__'),
                    COALESCE(o.status_frete, N'__NULL__'),
                    COALESCE(o.status_frete_norm, N'__NULL__'),
                    COALESCE(o.classificacao_nome, N'__NULL__'),
                    COALESCE(o.centro_custo_nome, N'__NULL__'),
                    COALESCE(o.tipo_frete, N'__NULL__'),
                    COALESCE(o.modal, N'__NULL__'),
                    COALESCE(o.modal_cte, N'__NULL__'),
                    COALESCE(o.tabela_preco_nome, N'__NULL__'),
                    COALESCE(o.usuario_nome, N'__NULL__'),
                    COALESCE(o.reference_number, N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(30), o.cte_id), N'__NULL__'),
                    COALESCE(o.chave_cte, N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(30), o.numero_cte), N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(30), o.serie_cte), N'__NULL__'),
                    COALESCE(o.cte_emission_type, N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(48), o.cte_created_at, 127), N'__NULL__'),
                    COALESCE(o.status_cte_real, N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(MAX), o.status_cte_result), N'__NULL__'),
                    COALESCE(o.status_cte_norm, N'__NULL__'),
                    COALESCE(o.cte_cancelado_evidencia, N'__NULL__'),
                    CONVERT(NVARCHAR(1), o.is_documento_cte),
                    CONVERT(NVARCHAR(1), o.is_cte_cancelado),
                    COALESCE(CONVERT(NVARCHAR(30), o.nfse_number), N'__NULL__'),
                    COALESCE(o.nfse_series, N'__NULL__'),
                    COALESCE(o.nfse_status, N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(30), o.nfse_issued_at, 126), N'__NULL__'),
                    CONVERT(NVARCHAR(1), o.is_cortesia),
                    CONVERT(NVARCHAR(1), o.is_bloqueio_faturamento),
                    CONVERT(NVARCHAR(1), o.is_elegivel_origem),
                    CONVERT(NVARCHAR(1), o.is_elegivel_faturamento),
                    COALESCE(CONVERT(NVARCHAR(40), o.valor_frete_original), N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(40), o.receita_bruta_original), N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(40), CASE WHEN o.is_elegivel_faturamento = 1 THEN o.valor_frete_original ELSE CONVERT(DECIMAL(18, 2), 0) END), N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(40), CASE WHEN o.is_elegivel_faturamento = 1 THEN o.receita_bruta_original ELSE CONVERT(DECIMAL(18, 2), 0) END), N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(40), o.valor_notas), N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(40), o.peso_notas), N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(40), o.peso_taxado), N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(40), o.peso_real), N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(40), o.peso_cubado), N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(40), o.total_m3), N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(20), o.volumes), N'__NULL__'),
                    CONVERT(NVARCHAR(1), o.excluido_na_origem),
                    COALESCE(CONVERT(NVARCHAR(30), o.data_extracao_origem, 126), N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(30), o.data_extracao_faturas, 126), N'__NULL__')
                ))) AS hash_linha
            FROM origem_regrada AS o
        )
        MERGE dbo.fato_fretes_faturamento WITH (HOLDLOCK) AS target
        USING origem AS source
           ON target.frete_id = source.frete_id
        WHEN MATCHED
         AND (
                target.hash_linha IS NULL
             OR source.hash_linha IS NULL
             OR target.hash_linha <> source.hash_linha
         )
            THEN UPDATE SET
                numero_minuta = source.numero_minuta,
                data_referencia_faturamento = source.data_referencia_faturamento,
                data_referencia_faturamento_date = source.data_referencia_faturamento_date,
                data_referencia_faturamento_yyyymm = source.data_referencia_faturamento_yyyymm,
                data_referencia_faturamento_real = source.data_referencia_faturamento_real,
                data_referencia_faturamento_real_date = source.data_referencia_faturamento_real_date,
                data_referencia_faturamento_real_yyyymm = source.data_referencia_faturamento_real_yyyymm,
                is_data_faturamento_retroagida = source.is_data_faturamento_retroagida,
                data_frete = source.data_frete,
                data_frete_date = source.data_frete_date,
                data_emissao_cte = source.data_emissao_cte,
                criado_em = source.criado_em,
                filial_id = source.filial_id,
                filial_nome = source.filial_nome,
                filial_key = source.filial_key,
                filial_apelido = source.filial_apelido,
                filial_cnpj = source.filial_cnpj,
                responsavel_regiao_destino = source.responsavel_regiao_destino,
                responsavel_regiao_destino_key = source.responsavel_regiao_destino_key,
                regiao_destino = source.regiao_destino,
                pagador_id = source.pagador_id,
                pagador_nome = source.pagador_nome,
                pagador_documento = source.pagador_documento,
                pagador_documento_key = source.pagador_documento_key,
                remetente_id = source.remetente_id,
                remetente_nome = source.remetente_nome,
                remetente_documento = source.remetente_documento,
                destinatario_id = source.destinatario_id,
                destinatario_nome = source.destinatario_nome,
                destinatario_documento = source.destinatario_documento,
                origem_cidade = source.origem_cidade,
                origem_uf = source.origem_uf,
                destino_cidade = source.destino_cidade,
                destino_uf = source.destino_uf,
                status_frete = source.status_frete,
                status_frete_norm = source.status_frete_norm,
                classificacao_nome = source.classificacao_nome,
                centro_custo_nome = source.centro_custo_nome,
                tipo_frete = source.tipo_frete,
                modal = source.modal,
                modal_cte = source.modal_cte,
                tabela_preco_nome = source.tabela_preco_nome,
                usuario_nome = source.usuario_nome,
                reference_number = source.reference_number,
                cte_id = source.cte_id,
                chave_cte = source.chave_cte,
                numero_cte = source.numero_cte,
                serie_cte = source.serie_cte,
                cte_emission_type = source.cte_emission_type,
                cte_created_at = source.cte_created_at,
                status_cte_real = source.status_cte_real,
                status_cte_result = source.status_cte_result,
                status_cte_norm = source.status_cte_norm,
                cte_cancelado_evidencia = source.cte_cancelado_evidencia,
                is_documento_cte = source.is_documento_cte,
                is_cte_cancelado = source.is_cte_cancelado,
                nfse_number = source.nfse_number,
                nfse_series = source.nfse_series,
                nfse_status = source.nfse_status,
                nfse_issued_at = source.nfse_issued_at,
                is_cortesia = source.is_cortesia,
                is_bloqueio_faturamento = source.is_bloqueio_faturamento,
                is_elegivel_origem = source.is_elegivel_origem,
                is_elegivel_faturamento = source.is_elegivel_faturamento,
                valor_frete_original = source.valor_frete_original,
                receita_bruta_original = source.receita_bruta_original,
                valor_frete = source.valor_frete,
                receita_bruta = source.receita_bruta,
                valor_notas = source.valor_notas,
                peso_notas = source.peso_notas,
                peso_taxado = source.peso_taxado,
                peso_real = source.peso_real,
                peso_cubado = source.peso_cubado,
                total_m3 = source.total_m3,
                volumes = source.volumes,
                excluido_na_origem = source.excluido_na_origem,
                data_exclusao_origem = source.data_exclusao_origem,
                ultima_reconciliacao_origem_em = source.ultima_reconciliacao_origem_em,
                data_extracao_origem = source.data_extracao_origem,
                data_extracao_faturas = source.data_extracao_faturas,
                snapshot_em = source.snapshot_em,
                hash_linha = source.hash_linha
        WHEN NOT MATCHED BY TARGET
            THEN INSERT (
                frete_id, numero_minuta, data_referencia_faturamento,
                data_referencia_faturamento_date, data_referencia_faturamento_yyyymm,
                data_referencia_faturamento_real, data_referencia_faturamento_real_date,
                data_referencia_faturamento_real_yyyymm, is_data_faturamento_retroagida,
                data_frete, data_frete_date, data_emissao_cte, criado_em,
                filial_id, filial_nome, filial_key, filial_apelido, filial_cnpj,
                responsavel_regiao_destino, responsavel_regiao_destino_key, regiao_destino,
                pagador_id, pagador_nome, pagador_documento, pagador_documento_key,
                remetente_id, remetente_nome, remetente_documento,
                destinatario_id, destinatario_nome, destinatario_documento,
                origem_cidade, origem_uf, destino_cidade, destino_uf,
                status_frete, status_frete_norm, classificacao_nome, centro_custo_nome,
                tipo_frete, modal, modal_cte, tabela_preco_nome, usuario_nome, reference_number,
                cte_id, chave_cte, numero_cte, serie_cte, cte_emission_type, cte_created_at,
                status_cte_real, status_cte_result, status_cte_norm, cte_cancelado_evidencia,
                is_documento_cte, is_cte_cancelado, nfse_number, nfse_series, nfse_status, nfse_issued_at,
                is_cortesia, is_bloqueio_faturamento, is_elegivel_origem, is_elegivel_faturamento,
                valor_frete_original, receita_bruta_original, valor_frete, receita_bruta,
                valor_notas, peso_notas, peso_taxado, peso_real, peso_cubado, total_m3, volumes,
                excluido_na_origem, data_exclusao_origem, ultima_reconciliacao_origem_em,
                data_extracao_origem, data_extracao_faturas, snapshot_em, hash_linha
            )
            VALUES (
                source.frete_id, source.numero_minuta, source.data_referencia_faturamento,
                source.data_referencia_faturamento_date, source.data_referencia_faturamento_yyyymm,
                source.data_referencia_faturamento_real, source.data_referencia_faturamento_real_date,
                source.data_referencia_faturamento_real_yyyymm, source.is_data_faturamento_retroagida,
                source.data_frete, source.data_frete_date, source.data_emissao_cte, source.criado_em,
                source.filial_id, source.filial_nome, source.filial_key, source.filial_apelido, source.filial_cnpj,
                source.responsavel_regiao_destino, source.responsavel_regiao_destino_key, source.regiao_destino,
                source.pagador_id, source.pagador_nome, source.pagador_documento, source.pagador_documento_key,
                source.remetente_id, source.remetente_nome, source.remetente_documento,
                source.destinatario_id, source.destinatario_nome, source.destinatario_documento,
                source.origem_cidade, source.origem_uf, source.destino_cidade, source.destino_uf,
                source.status_frete, source.status_frete_norm, source.classificacao_nome, source.centro_custo_nome,
                source.tipo_frete, source.modal, source.modal_cte, source.tabela_preco_nome, source.usuario_nome, source.reference_number,
                source.cte_id, source.chave_cte, source.numero_cte, source.serie_cte, source.cte_emission_type, source.cte_created_at,
                source.status_cte_real, source.status_cte_result, source.status_cte_norm, source.cte_cancelado_evidencia,
                source.is_documento_cte, source.is_cte_cancelado, source.nfse_number, source.nfse_series, source.nfse_status, source.nfse_issued_at,
                source.is_cortesia, source.is_bloqueio_faturamento, source.is_elegivel_origem, source.is_elegivel_faturamento,
                source.valor_frete_original, source.receita_bruta_original, source.valor_frete, source.receita_bruta,
                source.valor_notas, source.peso_notas, source.peso_taxado, source.peso_real, source.peso_cubado, source.total_m3, source.volumes,
                source.excluido_na_origem, source.data_exclusao_origem, source.ultima_reconciliacao_origem_em,
                source.data_extracao_origem, source.data_extracao_faturas, source.snapshot_em, source.hash_linha
            )
        WHEN NOT MATCHED BY SOURCE
         AND @MarcarAusentesComoExcluidos = 1
         AND target.excluido_na_origem = 0
         AND (
                @cargaCompleta = 1
             OR (
                    target.data_referencia_faturamento_real_date IS NOT NULL
                AND target.data_referencia_faturamento_real_date >= @DataInicio
                AND target.data_referencia_faturamento_real_date < @DataFimExclusivo
             )
             OR (
                    target.data_referencia_faturamento_real_date IS NULL
                AND target.data_referencia_faturamento_date >= @DataInicio
                AND target.data_referencia_faturamento_date < @DataFimExclusivo
             )
         )
            THEN UPDATE SET
                excluido_na_origem = 1,
                is_elegivel_faturamento = 0,
                valor_frete = 0,
                receita_bruta = 0,
                data_exclusao_origem = COALESCE(target.data_exclusao_origem, @SnapshotEm),
                ultima_reconciliacao_origem_em = @SnapshotEm,
                snapshot_em = @SnapshotEm,
                hash_linha = NULL
        OUTPUT $action
        INTO @resultado (merge_action);

        EXEC sys.sp_releaseapplock
            @Resource = N'dbo.sp_carga_fato_fretes_faturamento',
            @LockOwner = N'Session';

        SELECT
            SUM(CASE WHEN merge_action = N'INSERT' THEN 1 ELSE 0 END) AS linhas_inseridas,
            SUM(CASE WHEN merge_action = N'UPDATE' THEN 1 ELSE 0 END) AS linhas_atualizadas,
            @SnapshotEm AS snapshot_em
        FROM @resultado;
    END TRY
    BEGIN CATCH
        EXEC sys.sp_releaseapplock
            @Resource = N'dbo.sp_carga_fato_fretes_faturamento',
            @LockOwner = N'Session';
        THROW;
    END CATCH;
END;
GO

PRINT 'Procedure dbo.sp_carga_fato_fretes_faturamento criada/atualizada com sucesso.';
GO
