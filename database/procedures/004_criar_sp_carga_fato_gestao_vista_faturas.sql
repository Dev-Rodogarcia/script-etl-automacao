-- ============================================================================
-- Procedure de carga incremental da fato materializada de Faturas por Cliente
-- ============================================================================

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;
GO

CREATE OR ALTER PROCEDURE dbo.sp_carga_fato_gestao_vista_faturas
    @DataInicio DATE = NULL,
    @DataFimExclusivo DATE = NULL,
    @MarcarAusentesComoExcluidos BIT = 1,
    @SnapshotEm DATETIME2(0) = NULL
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    IF OBJECT_ID(N'dbo.fato_gestao_vista_faturas', N'U') IS NULL
        THROW 51050, 'Tabela dbo.fato_gestao_vista_faturas nao encontrada. Execute a migration 032 antes da carga.', 1;

    IF OBJECT_ID(N'dbo.faturas_por_cliente', N'U') IS NULL
        THROW 51051, 'Tabela dbo.faturas_por_cliente nao encontrada. Carga da fato de faturas abortada.', 1;

    IF (@DataInicio IS NULL AND @DataFimExclusivo IS NOT NULL)
       OR (@DataInicio IS NOT NULL AND @DataFimExclusivo IS NULL)
        THROW 51052, 'Informe @DataInicio e @DataFimExclusivo juntos ou deixe ambos nulos para carga completa.', 1;

    IF @DataInicio IS NOT NULL AND @DataInicio >= @DataFimExclusivo
        THROW 51053, '@DataInicio deve ser menor que @DataFimExclusivo.', 1;

    DECLARE @cargaCompleta BIT = CASE WHEN @DataInicio IS NULL AND @DataFimExclusivo IS NULL THEN 1 ELSE 0 END;
    SET @SnapshotEm = COALESCE(@SnapshotEm, SYSUTCDATETIME());

    DECLARE @hoje DATE = CAST(@SnapshotEm AS DATE);
    DECLARE @lockResult INT;

    EXEC @lockResult = sys.sp_getapplock
        @Resource = N'dbo.sp_carga_fato_gestao_vista_faturas',
        @LockMode = N'Exclusive',
        @LockOwner = N'Session',
        @LockTimeout = 0;

    IF @lockResult < 0
        THROW 51054, 'Carga da fato de faturas ja esta em execucao por outra sessao.', 1;

    DECLARE @resultado TABLE (
        merge_action NVARCHAR(10) NOT NULL
    );

    BEGIN TRY
        ;WITH faturas_fonte AS (
            SELECT
                NULLIF(LTRIM(RTRIM(fpc.unique_id)), N'') AS unique_id,
                N'linha|' + NULLIF(LTRIM(RTRIM(fpc.unique_id)), N'') AS chave_normalizacao,

                documento.documento_fatura,
                NULLIF(LTRIM(RTRIM(fpc.numero_fatura)), N'') AS numero_fatura,
                CASE
                    WHEN fpc.numero_cte IS NOT NULL THEN CONVERT(NVARCHAR(50), fpc.numero_cte)
                    WHEN fpc.numero_nfse IS NOT NULL THEN CONVERT(NVARCHAR(50), fpc.numero_nfse)
                    ELSE NULL
                END AS numero_documento,
                fpc.numero_cte,
                NULLIF(LTRIM(RTRIM(fpc.chave_cte)), N'') AS chave_cte,
                fpc.numero_nfse,
                NULLIF(LTRIM(RTRIM(fpc.serie_nfse)), N'') AS serie_nfse,

                fpc.data_emissao_cte,
                CAST(fpc.data_emissao_cte AS DATE) AS data_emissao_cte_date,
                fpc.data_emissao_fatura,
                CASE
                    WHEN fpc.data_emissao_fatura IS NULL THEN NULL
                    ELSE YEAR(fpc.data_emissao_fatura) * 100 + MONTH(fpc.data_emissao_fatura)
                END AS data_emissao_fatura_yyyymm,
                fpc.data_vencimento_fatura,
                fpc.data_baixa_fatura,
                COALESCE(fpc.fit_ant_issue_date, fpc.data_emissao_fatura) AS data_base_prazo,
                COALESCE(fpc.fit_ant_issue_date, fpc.data_emissao_fatura, CAST(fpc.data_emissao_cte AS DATE)) AS data_referencia_mensal,
                CASE
                    WHEN COALESCE(fpc.fit_ant_issue_date, fpc.data_emissao_fatura, CAST(fpc.data_emissao_cte AS DATE)) IS NULL THEN NULL
                    ELSE YEAR(COALESCE(fpc.fit_ant_issue_date, fpc.data_emissao_fatura, CAST(fpc.data_emissao_cte AS DATE))) * 100
                       + MONTH(COALESCE(fpc.fit_ant_issue_date, fpc.data_emissao_fatura, CAST(fpc.data_emissao_cte AS DATE)))
                END AS data_referencia_mensal_yyyymm,
                fpc.fit_ant_ils_original_due_date,

                NULLIF(LTRIM(RTRIM(fpc.filial)), N'') AS filial,
                NULLIF(LOWER(LTRIM(RTRIM(fpc.filial))), N'') AS filial_key,
                NULLIF(LTRIM(RTRIM(fpc.estado)), N'') AS estado,
                NULLIF(LTRIM(RTRIM(fpc.pagador_nome)), N'') AS pagador_nome,
                NULLIF(LTRIM(RTRIM(fpc.pagador_documento)), N'') AS pagador_documento,
                documentos.pagador_documento_key,
                cliente.cliente_nome,
                cliente.cliente_cnpj,
                cliente.cliente_cnpj_key,
                cliente.cliente_chave,
                NULLIF(LTRIM(RTRIM(fpc.remetente_nome)), N'') AS remetente_nome,
                NULLIF(LTRIM(RTRIM(fpc.remetente_documento)), N'') AS remetente_documento,
                NULLIF(LTRIM(RTRIM(fpc.destinatario_nome)), N'') AS destinatario_nome,
                NULLIF(LTRIM(RTRIM(fpc.destinatario_documento)), N'') AS destinatario_documento,
                NULLIF(LTRIM(RTRIM(fpc.vendedor_nome)), N'') AS vendedor_nome,

                CASE
                    WHEN documento.documento_fatura IS NOT NULL THEN N'Faturado'
                    ELSE N'Aguardando Faturamento'
                END AS status_processo,
                CASE
                    WHEN documento.documento_fatura IS NULL THEN N'sem_fatura'
                    WHEN fpc.data_baixa_fatura IS NOT NULL THEN N'baixado'
                    WHEN fpc.data_vencimento_fatura IS NULL THEN N'sem_vencimento'
                    WHEN fpc.data_vencimento_fatura < @hoje THEN N'vencido'
                    ELSE N'a_vencer'
                END AS status_pagamento,
                NULLIF(LTRIM(RTRIM(fpc.status_cte)), N'') AS status_cte,
                LEFT(NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(MAX), fpc.status_cte_result))), N''), 4000) AS status_cte_result,
                NULLIF(LTRIM(RTRIM(fpc.tipo_frete)), N'') AS tipo_frete,
                NULLIF(LTRIM(RTRIM(fpc.classificacao)), N'') AS classificacao,

                fpc.valor_frete,
                fpc.valor_fatura,
                fpc.fit_ant_value AS valor_fit_ant,
                fpc.third_party_ctes_value,
                COALESCE(fpc.fit_ant_value, fpc.valor_fatura, fpc.valor_frete, CONVERT(DECIMAL(19, 4), 0)) AS valor_operacional,

                LEFT(NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(MAX), fpc.notas_fiscais))), N''), 4000) AS notas_fiscais,
                LEFT(NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(MAX), fpc.pedidos_cliente))), N''), 4000) AS pedidos_cliente,
                LEFT(NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(MAX), fpc.metadata))), N''), 4000) AS metadata,

                CAST(COALESCE(fpc.excluido_na_origem, 0) AS BIT) AS excluido_na_origem,
                fpc.data_extracao AS data_extracao_origem
            FROM dbo.faturas_por_cliente AS fpc
            CROSS APPLY (
                SELECT UPPER(NULLIF(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
                    LTRIM(RTRIM(COALESCE(CONVERT(NVARCHAR(50), fpc.pagador_documento), N''))),
                    N'.', N''), N'-', N''), N'/', N''), N' ', N''), CHAR(9), N''), CHAR(160), N''), N'')) AS pagador_documento_key
            ) AS documentos
            CROSS APPLY (
                SELECT NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(100), fpc.fit_ant_document))), N'') AS documento_raw
            ) AS documento_raw
            CROSS APPLY (
                SELECT CASE
                    WHEN LOWER(documento_raw.documento_raw) IN (N'faturado', N'aguardando faturamento') THEN NULL
                    ELSE documento_raw.documento_raw
                END AS documento_fatura
            ) AS documento
            CROSS APPLY (
                SELECT COALESCE(
                    NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(50), fpc.cliente_cnpj))), N''),
                    CASE
                        WHEN documentos.pagador_documento_key IS NOT NULL
                         AND documentos.pagador_documento_key NOT LIKE N'%[^0-9]%'
                         AND LEN(documentos.pagador_documento_key) = 14
                        THEN documentos.pagador_documento_key
                        ELSE NULL
                    END
                ) AS cliente_cnpj_raw
            ) AS cliente_raw
            CROSS APPLY (
                SELECT UPPER(NULLIF(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
                    LTRIM(RTRIM(COALESCE(cliente_raw.cliente_cnpj_raw, N''))),
                    N'.', N''), N'-', N''), N'/', N''), N' ', N''), CHAR(9), N''), CHAR(160), N''), N'')) AS cliente_cnpj_key
            ) AS cliente_docs
            CROSS APPLY (
                SELECT
                    CASE
                        WHEN cliente_docs.cliente_cnpj_key IS NOT NULL
                         AND cliente_docs.cliente_cnpj_key NOT LIKE N'%[^0-9]%'
                         AND LEN(cliente_docs.cliente_cnpj_key) = 14
                        THEN cliente_docs.cliente_cnpj_key
                        ELSE cliente_raw.cliente_cnpj_raw
                    END AS cliente_cnpj,
                    CASE
                        WHEN cliente_docs.cliente_cnpj_key IS NOT NULL
                         AND cliente_docs.cliente_cnpj_key NOT LIKE N'%[^0-9]%'
                         AND LEN(cliente_docs.cliente_cnpj_key) = 14
                        THEN cliente_docs.cliente_cnpj_key
                        ELSE NULL
                    END AS cliente_cnpj_key,
                    CASE
                        WHEN cliente_docs.cliente_cnpj_key IS NOT NULL
                         AND cliente_docs.cliente_cnpj_key NOT LIKE N'%[^0-9]%'
                         AND LEN(cliente_docs.cliente_cnpj_key) = 14
                        THEN N'cnpj:' + cliente_docs.cliente_cnpj_key
                        WHEN NULLIF(LTRIM(RTRIM(fpc.pagador_nome)), N'') IS NOT NULL
                        THEN N'nome:' + LOWER(NULLIF(LTRIM(RTRIM(fpc.pagador_nome)), N''))
                        ELSE NULL
                    END AS cliente_chave,
                    CASE
                        WHEN NULLIF(LTRIM(RTRIM(fpc.pagador_nome)), N'') IS NOT NULL
                        THEN NULLIF(LTRIM(RTRIM(fpc.pagador_nome)), N'')
                        ELSE cliente_raw.cliente_cnpj_raw
                    END AS cliente_nome
            ) AS cliente
            WHERE NULLIF(LTRIM(RTRIM(fpc.unique_id)), N'') IS NOT NULL
              AND (
                    @cargaCompleta = 1
                 OR (
                        fpc.data_emissao_fatura >= @DataInicio
                    AND fpc.data_emissao_fatura < @DataFimExclusivo
                 )
              )
        ),
        origem AS (
            SELECT
                ff.*,
                CASE WHEN ff.excluido_na_origem = 1 THEN @SnapshotEm ELSE NULL END AS data_exclusao_origem,
                @SnapshotEm AS ultima_reconciliacao_origem_em,
                @SnapshotEm AS snapshot_em,
                CONVERT(BINARY(32), HASHBYTES('SHA2_256', CONCAT_WS(N'|',
                    COALESCE(ff.unique_id, N'__NULL__'),
                    COALESCE(ff.chave_normalizacao, N'__NULL__'),
                    COALESCE(ff.documento_fatura, N'__NULL__'),
                    COALESCE(ff.numero_fatura, N'__NULL__'),
                    COALESCE(ff.numero_documento, N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(30), ff.numero_cte), N'__NULL__'),
                    COALESCE(ff.chave_cte, N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(30), ff.numero_nfse), N'__NULL__'),
                    COALESCE(ff.serie_nfse, N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(48), ff.data_emissao_cte, 127), N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(30), ff.data_emissao_cte_date, 126), N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(30), ff.data_emissao_fatura, 126), N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(10), ff.data_emissao_fatura_yyyymm), N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(30), ff.data_vencimento_fatura, 126), N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(30), ff.data_baixa_fatura, 126), N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(30), ff.data_base_prazo, 126), N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(30), ff.data_referencia_mensal, 126), N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(10), ff.data_referencia_mensal_yyyymm), N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(30), ff.fit_ant_ils_original_due_date, 126), N'__NULL__'),
                    COALESCE(ff.filial, N'__NULL__'),
                    COALESCE(ff.filial_key, N'__NULL__'),
                    COALESCE(ff.estado, N'__NULL__'),
                    COALESCE(ff.pagador_nome, N'__NULL__'),
                    COALESCE(ff.pagador_documento, N'__NULL__'),
                    COALESCE(ff.pagador_documento_key, N'__NULL__'),
                    COALESCE(ff.cliente_nome, N'__NULL__'),
                    COALESCE(ff.cliente_cnpj, N'__NULL__'),
                    COALESCE(ff.cliente_cnpj_key, N'__NULL__'),
                    COALESCE(ff.cliente_chave, N'__NULL__'),
                    COALESCE(ff.remetente_nome, N'__NULL__'),
                    COALESCE(ff.remetente_documento, N'__NULL__'),
                    COALESCE(ff.destinatario_nome, N'__NULL__'),
                    COALESCE(ff.destinatario_documento, N'__NULL__'),
                    COALESCE(ff.vendedor_nome, N'__NULL__'),
                    COALESCE(ff.status_processo, N'__NULL__'),
                    COALESCE(ff.status_pagamento, N'__NULL__'),
                    COALESCE(ff.status_cte, N'__NULL__'),
                    COALESCE(ff.status_cte_result, N'__NULL__'),
                    COALESCE(ff.tipo_frete, N'__NULL__'),
                    COALESCE(ff.classificacao, N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(40), ff.valor_frete), N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(40), ff.valor_fatura), N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(40), ff.valor_fit_ant), N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(40), ff.third_party_ctes_value), N'__NULL__'),
                    COALESCE(CONVERT(NVARCHAR(40), ff.valor_operacional), N'__NULL__'),
                    COALESCE(ff.notas_fiscais, N'__NULL__'),
                    COALESCE(ff.pedidos_cliente, N'__NULL__'),
                    COALESCE(ff.metadata, N'__NULL__'),
                    CONVERT(NVARCHAR(1), ff.excluido_na_origem),
                    COALESCE(CONVERT(NVARCHAR(30), ff.data_extracao_origem, 126), N'__NULL__')
                ))) AS hash_linha
            FROM faturas_fonte AS ff
        )
        MERGE dbo.fato_gestao_vista_faturas WITH (HOLDLOCK) AS target
        USING origem AS source
           ON target.unique_id = source.unique_id
        WHEN MATCHED
         AND (
                target.hash_linha IS NULL
             OR source.hash_linha IS NULL
             OR target.hash_linha <> source.hash_linha
         )
            THEN UPDATE SET
                chave_normalizacao = source.chave_normalizacao,
                documento_fatura = source.documento_fatura,
                numero_fatura = source.numero_fatura,
                numero_documento = source.numero_documento,
                numero_cte = source.numero_cte,
                chave_cte = source.chave_cte,
                numero_nfse = source.numero_nfse,
                serie_nfse = source.serie_nfse,
                data_emissao_cte = source.data_emissao_cte,
                data_emissao_cte_date = source.data_emissao_cte_date,
                data_emissao_fatura = source.data_emissao_fatura,
                data_emissao_fatura_yyyymm = source.data_emissao_fatura_yyyymm,
                data_vencimento_fatura = source.data_vencimento_fatura,
                data_baixa_fatura = source.data_baixa_fatura,
                data_base_prazo = source.data_base_prazo,
                data_referencia_mensal = source.data_referencia_mensal,
                data_referencia_mensal_yyyymm = source.data_referencia_mensal_yyyymm,
                fit_ant_ils_original_due_date = source.fit_ant_ils_original_due_date,
                filial = source.filial,
                filial_key = source.filial_key,
                estado = source.estado,
                pagador_nome = source.pagador_nome,
                pagador_documento = source.pagador_documento,
                pagador_documento_key = source.pagador_documento_key,
                cliente_nome = source.cliente_nome,
                cliente_cnpj = source.cliente_cnpj,
                cliente_cnpj_key = source.cliente_cnpj_key,
                cliente_chave = source.cliente_chave,
                remetente_nome = source.remetente_nome,
                remetente_documento = source.remetente_documento,
                destinatario_nome = source.destinatario_nome,
                destinatario_documento = source.destinatario_documento,
                vendedor_nome = source.vendedor_nome,
                status_processo = source.status_processo,
                status_pagamento = source.status_pagamento,
                status_cte = source.status_cte,
                status_cte_result = source.status_cte_result,
                tipo_frete = source.tipo_frete,
                classificacao = source.classificacao,
                valor_frete = source.valor_frete,
                valor_fatura = source.valor_fatura,
                valor_fit_ant = source.valor_fit_ant,
                third_party_ctes_value = source.third_party_ctes_value,
                valor_operacional = source.valor_operacional,
                notas_fiscais = source.notas_fiscais,
                pedidos_cliente = source.pedidos_cliente,
                metadata = source.metadata,
                excluido_na_origem = source.excluido_na_origem,
                data_exclusao_origem = source.data_exclusao_origem,
                ultima_reconciliacao_origem_em = source.ultima_reconciliacao_origem_em,
                data_extracao_origem = source.data_extracao_origem,
                snapshot_em = source.snapshot_em,
                hash_linha = source.hash_linha
        WHEN NOT MATCHED BY TARGET
            THEN INSERT (
                unique_id, chave_normalizacao, documento_fatura, numero_fatura,
                numero_documento, numero_cte, chave_cte, numero_nfse, serie_nfse,
                data_emissao_cte, data_emissao_cte_date, data_emissao_fatura,
                data_emissao_fatura_yyyymm, data_vencimento_fatura, data_baixa_fatura,
                data_base_prazo, data_referencia_mensal, data_referencia_mensal_yyyymm,
                fit_ant_ils_original_due_date, filial, filial_key, estado,
                pagador_nome, pagador_documento, pagador_documento_key,
                cliente_nome, cliente_cnpj, cliente_cnpj_key, cliente_chave,
                remetente_nome, remetente_documento, destinatario_nome,
                destinatario_documento, vendedor_nome, status_processo,
                status_pagamento, status_cte, status_cte_result, tipo_frete,
                classificacao, valor_frete, valor_fatura, valor_fit_ant,
                third_party_ctes_value, valor_operacional, notas_fiscais,
                pedidos_cliente, metadata, excluido_na_origem, data_exclusao_origem,
                ultima_reconciliacao_origem_em, data_extracao_origem, snapshot_em,
                hash_linha
            )
            VALUES (
                source.unique_id, source.chave_normalizacao, source.documento_fatura, source.numero_fatura,
                source.numero_documento, source.numero_cte, source.chave_cte, source.numero_nfse, source.serie_nfse,
                source.data_emissao_cte, source.data_emissao_cte_date, source.data_emissao_fatura,
                source.data_emissao_fatura_yyyymm, source.data_vencimento_fatura, source.data_baixa_fatura,
                source.data_base_prazo, source.data_referencia_mensal, source.data_referencia_mensal_yyyymm,
                source.fit_ant_ils_original_due_date, source.filial, source.filial_key, source.estado,
                source.pagador_nome, source.pagador_documento, source.pagador_documento_key,
                source.cliente_nome, source.cliente_cnpj, source.cliente_cnpj_key, source.cliente_chave,
                source.remetente_nome, source.remetente_documento, source.destinatario_nome,
                source.destinatario_documento, source.vendedor_nome, source.status_processo,
                source.status_pagamento, source.status_cte, source.status_cte_result, source.tipo_frete,
                source.classificacao, source.valor_frete, source.valor_fatura, source.valor_fit_ant,
                source.third_party_ctes_value, source.valor_operacional, source.notas_fiscais,
                source.pedidos_cliente, source.metadata, source.excluido_na_origem, source.data_exclusao_origem,
                source.ultima_reconciliacao_origem_em, source.data_extracao_origem, source.snapshot_em,
                source.hash_linha
            )
        WHEN NOT MATCHED BY SOURCE
         AND @MarcarAusentesComoExcluidos = 1
         AND target.excluido_na_origem = 0
         AND (
                @cargaCompleta = 1
             OR (
                    target.data_emissao_fatura >= @DataInicio
                AND target.data_emissao_fatura < @DataFimExclusivo
             )
         )
            THEN UPDATE SET
                excluido_na_origem = 1,
                status_processo = N'Aguardando Faturamento',
                status_pagamento = N'sem_fatura',
                data_exclusao_origem = COALESCE(target.data_exclusao_origem, @SnapshotEm),
                ultima_reconciliacao_origem_em = @SnapshotEm,
                snapshot_em = @SnapshotEm,
                hash_linha = NULL
        OUTPUT $action
        INTO @resultado (merge_action);

        EXEC sys.sp_releaseapplock
            @Resource = N'dbo.sp_carga_fato_gestao_vista_faturas',
            @LockOwner = N'Session';

        SELECT
            SUM(CASE WHEN merge_action = N'INSERT' THEN 1 ELSE 0 END) AS linhas_inseridas,
            SUM(CASE WHEN merge_action = N'UPDATE' THEN 1 ELSE 0 END) AS linhas_atualizadas,
            @SnapshotEm AS snapshot_em
        FROM @resultado;
    END TRY
    BEGIN CATCH
        EXEC sys.sp_releaseapplock
            @Resource = N'dbo.sp_carga_fato_gestao_vista_faturas',
            @LockOwner = N'Session';
        THROW;
    END CATCH;
END;
GO

PRINT 'Procedure dbo.sp_carga_fato_gestao_vista_faturas criada/atualizada com sucesso.';
GO
