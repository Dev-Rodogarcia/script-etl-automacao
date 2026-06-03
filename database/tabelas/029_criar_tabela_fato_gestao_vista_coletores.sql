-- ============================================================================
-- Script base de criacao da fato materializada de Gestao a Vista para coletores
-- ============================================================================

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;
GO

IF NOT EXISTS (SELECT 1 FROM sys.partition_functions WHERE name = N'PF_fato_gv_data_referencia_mes')
BEGIN
    DECLARE @limites NVARCHAR(MAX);

    ;WITH meses AS (
        SELECT CAST('2020-01-01' AS DATE) AS data_limite
        UNION ALL
        SELECT DATEADD(MONTH, 1, data_limite)
        FROM meses
        WHERE data_limite < CAST('2032-01-01' AS DATE)
    )
    SELECT @limites = STUFF((
        SELECT N',''' + CONVERT(CHAR(10), data_limite, 120) + N''''
        FROM meses
        ORDER BY data_limite
        FOR XML PATH(''), TYPE
    ).value('.', 'NVARCHAR(MAX)'), 1, 1, N'')
    OPTION (MAXRECURSION 0);

    EXEC(N'
        CREATE PARTITION FUNCTION PF_fato_gv_data_referencia_mes (DATE)
        AS RANGE RIGHT FOR VALUES (' + @limites + N');
    ');

    PRINT 'Partition Function PF_fato_gv_data_referencia_mes criada.';
END
ELSE
BEGIN
    PRINT 'Partition Function PF_fato_gv_data_referencia_mes ja existe.';
END;

IF NOT EXISTS (SELECT 1 FROM sys.partition_schemes WHERE name = N'PS_fato_gv_data_referencia_mes')
BEGIN
    EXEC(N'
        CREATE PARTITION SCHEME PS_fato_gv_data_referencia_mes
        AS PARTITION PF_fato_gv_data_referencia_mes
        ALL TO ([PRIMARY]);
    ');

    PRINT 'Partition Scheme PS_fato_gv_data_referencia_mes criado.';
END
ELSE
BEGIN
    PRINT 'Partition Scheme PS_fato_gv_data_referencia_mes ja existe.';
END;

IF OBJECT_ID(N'dbo.fato_gestao_vista_coletores', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.fato_gestao_vista_coletores (
        data_referencia DATE NOT NULL,
        data_referencia_yyyymm INT NOT NULL,

        filial NVARCHAR(255) NOT NULL,
        filial_key NVARCHAR(255) COLLATE Latin1_General_CI_AI NOT NULL,
        classificacao NVARCHAR(50) NOT NULL,

        manifestos_bipados BIGINT NOT NULL CONSTRAINT DF_fato_gv_col_manifestos_bipados DEFAULT (0),
        manifestos_emitidos BIGINT NOT NULL CONSTRAINT DF_fato_gv_col_manifestos_emitidos DEFAULT (0),
        manifestos_descarregamento BIGINT NOT NULL CONSTRAINT DF_fato_gv_col_manifestos_desc DEFAULT (0),
        total_manifestos BIGINT NOT NULL CONSTRAINT DF_fato_gv_col_total_manifestos DEFAULT (0),
        manifestos_incompletos BIGINT NOT NULL CONSTRAINT DF_fato_gv_col_manifestos_incomp DEFAULT (0),
        pct_utilizacao DECIMAL(9, 4) NOT NULL CONSTRAINT DF_fato_gv_col_pct_utilizacao DEFAULT (0),

        tem_ordens_conferencia BIT NOT NULL CONSTRAINT DF_fato_gv_col_tem_ordens DEFAULT (0),
        tem_manifestos_bipaveis BIT NOT NULL CONSTRAINT DF_fato_gv_col_tem_bipaveis DEFAULT (0),
        is_filial_operacional BIT NOT NULL CONSTRAINT DF_fato_gv_col_is_filial_operacional DEFAULT (0),
        is_linha_valida_indicador BIT NOT NULL CONSTRAINT DF_fato_gv_col_is_linha_valida DEFAULT (1),

        excluido_na_origem BIT NOT NULL CONSTRAINT DF_fato_gv_col_excluido_na_origem DEFAULT (0),
        data_exclusao_origem DATETIME2(0) NULL,
        ultima_reconciliacao_origem_em DATETIME2(0) NULL,

        updated_at DATETIME2(0) NULL,
        snapshot_em DATETIME2(0) NOT NULL CONSTRAINT DF_fato_gv_col_snapshot_em DEFAULT SYSUTCDATETIME(),
        hash_linha BINARY(32) NULL,

        CONSTRAINT CK_fato_gv_col_metricas_nao_negativas
            CHECK (
                manifestos_bipados >= 0
                AND manifestos_emitidos >= 0
                AND manifestos_descarregamento >= 0
                AND total_manifestos >= 0
                AND manifestos_incompletos >= 0
                AND pct_utilizacao >= 0
            ),
        CONSTRAINT CK_fato_gv_col_total_manifestos
            CHECK (total_manifestos = manifestos_emitidos + manifestos_descarregamento),
        CONSTRAINT CK_fato_gv_col_incompletos
            CHECK (manifestos_incompletos <= manifestos_bipados)
    )
    ON PS_fato_gv_data_referencia_mes(data_referencia);

    PRINT 'Tabela dbo.fato_gestao_vista_coletores criada.';
END
ELSE
BEGIN
    PRINT 'Tabela dbo.fato_gestao_vista_coletores ja existe.';
END;

IF OBJECT_ID(N'dbo.fato_gestao_vista_coletores', N'U') IS NOT NULL
   AND NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = N'CCI_fato_gv_coletores'
          AND object_id = OBJECT_ID(N'dbo.fato_gestao_vista_coletores')
   )
BEGIN
    CREATE CLUSTERED COLUMNSTORE INDEX CCI_fato_gv_coletores
        ON dbo.fato_gestao_vista_coletores
        ON PS_fato_gv_data_referencia_mes(data_referencia);

    PRINT 'Clustered Columnstore Index CCI_fato_gv_coletores criado.';
END;

IF OBJECT_ID(N'dbo.fato_gestao_vista_coletores', N'U') IS NOT NULL
   AND NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = N'UX_fato_gv_coletores_data_filial_classif'
          AND object_id = OBJECT_ID(N'dbo.fato_gestao_vista_coletores')
   )
BEGIN
    CREATE UNIQUE NONCLUSTERED INDEX UX_fato_gv_coletores_data_filial_classif
        ON dbo.fato_gestao_vista_coletores(data_referencia, filial_key, classificacao)
        WITH (DATA_COMPRESSION = PAGE)
        ON PS_fato_gv_data_referencia_mes(data_referencia);

    PRINT 'Indice UX_fato_gv_coletores_data_filial_classif criado.';
END;

IF OBJECT_ID(N'dbo.fato_gestao_vista_coletores', N'U') IS NOT NULL
   AND NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = N'IX_fato_gv_coletores_merge'
          AND object_id = OBJECT_ID(N'dbo.fato_gestao_vista_coletores')
   )
BEGIN
    CREATE NONCLUSTERED INDEX IX_fato_gv_coletores_merge
        ON dbo.fato_gestao_vista_coletores(data_referencia, filial_key, classificacao)
        INCLUDE (hash_linha, excluido_na_origem, snapshot_em)
        WITH (DATA_COMPRESSION = PAGE)
        ON PS_fato_gv_data_referencia_mes(data_referencia);

    PRINT 'Indice IX_fato_gv_coletores_merge criado.';
END;

IF OBJECT_ID(N'dbo.fato_gestao_vista_coletores', N'U') IS NOT NULL
   AND NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = N'IX_fato_gv_coletores_periodo_filial'
          AND object_id = OBJECT_ID(N'dbo.fato_gestao_vista_coletores')
   )
BEGIN
    CREATE NONCLUSTERED INDEX IX_fato_gv_coletores_periodo_filial
        ON dbo.fato_gestao_vista_coletores(data_referencia DESC, filial_key)
        INCLUDE (
            manifestos_bipados,
            manifestos_emitidos,
            manifestos_descarregamento,
            total_manifestos,
            manifestos_incompletos,
            is_filial_operacional
        )
        WHERE is_linha_valida_indicador = 1
          AND excluido_na_origem = 0
        WITH (DATA_COMPRESSION = PAGE)
        ON PS_fato_gv_data_referencia_mes(data_referencia);

    PRINT 'Indice IX_fato_gv_coletores_periodo_filial criado.';
END;
GO
