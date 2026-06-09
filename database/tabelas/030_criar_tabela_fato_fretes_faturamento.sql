-- ============================================================================
-- Tabela fato granular para Faturamento de Fretes
-- Grao: 1 linha por frete validado, materializada a partir de dbo.fretes.
-- ============================================================================

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;
GO

IF NOT EXISTS (SELECT 1 FROM sys.partition_functions WHERE name = N'PF_fato_ff_data_referencia_mes')
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
        CREATE PARTITION FUNCTION PF_fato_ff_data_referencia_mes (DATE)
        AS RANGE RIGHT FOR VALUES (' + @limites + N');
    ');

    PRINT 'Partition Function PF_fato_ff_data_referencia_mes criada.';
END
ELSE
BEGIN
    PRINT 'Partition Function PF_fato_ff_data_referencia_mes ja existe.';
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.partition_schemes WHERE name = N'PS_fato_ff_data_referencia_mes')
BEGIN
    EXEC(N'
        CREATE PARTITION SCHEME PS_fato_ff_data_referencia_mes
        AS PARTITION PF_fato_ff_data_referencia_mes
        ALL TO ([PRIMARY]);
    ');

    PRINT 'Partition Scheme PS_fato_ff_data_referencia_mes criado.';
END
ELSE
BEGIN
    PRINT 'Partition Scheme PS_fato_ff_data_referencia_mes ja existe.';
END;
GO

IF OBJECT_ID(N'dbo.fato_fretes_faturamento', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.fato_fretes_faturamento (
        frete_id BIGINT NOT NULL,
        numero_minuta BIGINT NULL,

        data_referencia_faturamento DATETIMEOFFSET NOT NULL,
        data_referencia_faturamento_date DATE NOT NULL,
        data_referencia_faturamento_yyyymm INT NOT NULL,
        data_referencia_faturamento_real DATETIMEOFFSET NULL,
        data_referencia_faturamento_real_date DATE NULL,
        data_referencia_faturamento_real_yyyymm INT NULL,
        is_data_faturamento_retroagida BIT NOT NULL CONSTRAINT DF_fato_ff_is_data_retroagida DEFAULT (0),
        data_frete DATETIMEOFFSET NULL,
        data_frete_date DATE NULL,
        data_emissao_cte DATETIMEOFFSET NULL,
        criado_em DATETIMEOFFSET NULL,

        filial_id BIGINT NULL,
        filial_nome NVARCHAR(255) NULL,
        filial_key NVARCHAR(255) NULL,
        filial_apelido NVARCHAR(255) NULL,
        filial_cnpj NVARCHAR(50) NULL,
        responsavel_regiao_destino NVARCHAR(255) NULL,
        responsavel_regiao_destino_key NVARCHAR(255) NULL,
        regiao_destino NVARCHAR(255) NULL,

        pagador_id BIGINT NULL,
        pagador_nome NVARCHAR(255) NULL,
        pagador_documento NVARCHAR(50) NULL,
        pagador_documento_key NVARCHAR(50) NULL,
        remetente_id BIGINT NULL,
        remetente_nome NVARCHAR(255) NULL,
        remetente_documento NVARCHAR(50) NULL,
        destinatario_id BIGINT NULL,
        destinatario_nome NVARCHAR(255) NULL,
        destinatario_documento NVARCHAR(50) NULL,

        origem_cidade NVARCHAR(255) NULL,
        origem_uf NVARCHAR(10) NULL,
        destino_cidade NVARCHAR(255) NULL,
        destino_uf NVARCHAR(10) NULL,

        status_frete NVARCHAR(50) NULL,
        status_frete_norm NVARCHAR(50) NULL,
        classificacao_nome NVARCHAR(255) NULL,
        centro_custo_nome NVARCHAR(255) NULL,
        tipo_frete NVARCHAR(100) NULL,
        modal NVARCHAR(50) NULL,
        modal_cte NVARCHAR(50) NULL,
        tabela_preco_nome NVARCHAR(255) NULL,
        usuario_nome NVARCHAR(255) NULL,
        reference_number NVARCHAR(100) NULL,

        cte_id BIGINT NULL,
        chave_cte NVARCHAR(100) NULL,
        numero_cte INT NULL,
        serie_cte INT NULL,
        cte_emission_type NVARCHAR(50) NULL,
        cte_created_at DATETIMEOFFSET NULL,
        status_cte_real NVARCHAR(255) NULL,
        status_cte_result NVARCHAR(4000) NULL,
        status_cte_norm NVARCHAR(255) NULL,
        cte_cancelado_evidencia NVARCHAR(255) NULL,
        is_documento_cte BIT NOT NULL CONSTRAINT DF_fato_ff_is_documento_cte DEFAULT (0),
        is_cte_cancelado BIT NOT NULL CONSTRAINT DF_fato_ff_is_cte_cancelado DEFAULT (0),

        nfse_number INT NULL,
        nfse_series NVARCHAR(50) NULL,
        nfse_status NVARCHAR(50) NULL,
        nfse_issued_at DATE NULL,

        is_cortesia BIT NOT NULL CONSTRAINT DF_fato_ff_is_cortesia DEFAULT (0),
        is_bloqueio_faturamento BIT NOT NULL CONSTRAINT DF_fato_ff_is_bloqueio DEFAULT (0),
        is_elegivel_origem BIT NOT NULL CONSTRAINT DF_fato_ff_is_eleg_origem DEFAULT (0),
        is_elegivel_faturamento BIT NOT NULL CONSTRAINT DF_fato_ff_is_eleg_faturamento DEFAULT (0),

        valor_frete_original DECIMAL(18, 2) NOT NULL CONSTRAINT DF_fato_ff_valor_frete_original DEFAULT (0),
        receita_bruta_original DECIMAL(18, 2) NOT NULL CONSTRAINT DF_fato_ff_receita_original DEFAULT (0),
        valor_frete DECIMAL(18, 2) NOT NULL CONSTRAINT DF_fato_ff_valor_frete DEFAULT (0),
        receita_bruta DECIMAL(18, 2) NOT NULL CONSTRAINT DF_fato_ff_receita_bruta DEFAULT (0),
        valor_notas DECIMAL(18, 2) NULL,
        peso_notas DECIMAL(18, 3) NULL,
        peso_taxado DECIMAL(18, 3) NULL,
        peso_real DECIMAL(18, 3) NULL,
        peso_cubado DECIMAL(18, 6) NULL,
        total_m3 DECIMAL(18, 6) NULL,
        volumes INT NULL,

        excluido_na_origem BIT NOT NULL CONSTRAINT DF_fato_ff_excluido_na_origem DEFAULT (0),
        data_exclusao_origem DATETIME2(0) NULL,
        ultima_reconciliacao_origem_em DATETIME2(0) NULL,
        data_extracao_origem DATETIME2(0) NULL,
        data_extracao_faturas DATETIME2(0) NULL,
        snapshot_em DATETIME2(0) NOT NULL CONSTRAINT DF_fato_ff_snapshot_em DEFAULT SYSUTCDATETIME(),
        hash_linha BINARY(32) NULL,

        CONSTRAINT CK_fato_ff_yyyymm
            CHECK (data_referencia_faturamento_yyyymm BETWEEN 201912 AND 203212),
        CONSTRAINT CK_fato_ff_cte_cancelado_sem_receita
            CHECK (is_cte_cancelado = 0 OR (is_elegivel_faturamento = 0 AND receita_bruta = 0 AND valor_frete = 0)),
        CONSTRAINT CK_fato_ff_inelegivel_sem_receita
            CHECK (is_elegivel_faturamento = 1 OR (receita_bruta = 0 AND valor_frete = 0))
    )
    ON PS_fato_ff_data_referencia_mes(data_referencia_faturamento_date);

    PRINT 'Tabela dbo.fato_fretes_faturamento criada.';
END
ELSE
BEGIN
    PRINT 'Tabela dbo.fato_fretes_faturamento ja existe.';
END;
GO

IF OBJECT_ID(N'dbo.fato_fretes_faturamento', N'U') IS NOT NULL
   AND NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = N'CCI_fato_ff'
          AND object_id = OBJECT_ID(N'dbo.fato_fretes_faturamento')
   )
BEGIN
    CREATE CLUSTERED COLUMNSTORE INDEX CCI_fato_ff
        ON dbo.fato_fretes_faturamento
        ON PS_fato_ff_data_referencia_mes(data_referencia_faturamento_date);

    PRINT 'Clustered Columnstore Index CCI_fato_ff criado.';
END;
GO

IF OBJECT_ID(N'dbo.fato_fretes_faturamento', N'U') IS NOT NULL
   AND NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = N'UX_fato_ff_frete_data'
          AND object_id = OBJECT_ID(N'dbo.fato_fretes_faturamento')
   )
BEGIN
    CREATE UNIQUE NONCLUSTERED INDEX UX_fato_ff_frete_data
        ON dbo.fato_fretes_faturamento(frete_id, data_referencia_faturamento_date)
        WITH (DATA_COMPRESSION = PAGE)
        ON PS_fato_ff_data_referencia_mes(data_referencia_faturamento_date);

    PRINT 'Indice UX_fato_ff_frete_data criado.';
END;
GO

IF OBJECT_ID(N'dbo.fato_fretes_faturamento', N'U') IS NOT NULL
   AND NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = N'IX_fato_ff_merge'
          AND object_id = OBJECT_ID(N'dbo.fato_fretes_faturamento')
   )
BEGIN
    CREATE NONCLUSTERED INDEX IX_fato_ff_merge
        ON dbo.fato_fretes_faturamento(frete_id)
        INCLUDE (data_referencia_faturamento_date, hash_linha, excluido_na_origem, snapshot_em)
        WITH (DATA_COMPRESSION = PAGE)
        ON PS_fato_ff_data_referencia_mes(data_referencia_faturamento_date);

    PRINT 'Indice IX_fato_ff_merge criado.';
END;
GO

IF OBJECT_ID(N'dbo.fato_fretes_faturamento', N'U') IS NOT NULL
   AND NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = N'IX_fato_ff_paginacao_periodo'
          AND object_id = OBJECT_ID(N'dbo.fato_fretes_faturamento')
   )
BEGIN
    CREATE NONCLUSTERED INDEX IX_fato_ff_paginacao_periodo
        ON dbo.fato_fretes_faturamento(
            data_referencia_faturamento DESC,
            numero_minuta DESC,
            frete_id DESC
        )
        INCLUDE (
            data_referencia_faturamento_date, filial_nome, filial_key,
            status_frete, status_frete_norm, pagador_nome, pagador_documento_key,
            responsavel_regiao_destino_key, origem_uf, destino_uf, tipo_frete,
            modal, valor_frete, receita_bruta, is_elegivel_faturamento,
            is_cte_cancelado
        )
        WHERE excluido_na_origem = 0
        WITH (DATA_COMPRESSION = PAGE)
        ON PS_fato_ff_data_referencia_mes(data_referencia_faturamento_date);

    PRINT 'Indice IX_fato_ff_paginacao_periodo criado.';
END;
GO

IF OBJECT_ID(N'dbo.fato_fretes_faturamento', N'U') IS NOT NULL
   AND NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = N'IX_fato_ff_periodo_faturamento'
          AND object_id = OBJECT_ID(N'dbo.fato_fretes_faturamento')
   )
BEGIN
    CREATE NONCLUSTERED INDEX IX_fato_ff_periodo_faturamento
        ON dbo.fato_fretes_faturamento(
            data_referencia_faturamento_date,
            is_elegivel_faturamento,
            filial_key,
            pagador_documento_key
        )
        INCLUDE (
            frete_id, numero_minuta, receita_bruta, valor_frete,
            receita_bruta_original, valor_frete_original, is_cte_cancelado,
            responsavel_regiao_destino_key, tipo_frete, modal
        )
        WHERE excluido_na_origem = 0
        WITH (DATA_COMPRESSION = PAGE)
        ON PS_fato_ff_data_referencia_mes(data_referencia_faturamento_date);

    PRINT 'Indice IX_fato_ff_periodo_faturamento criado.';
END;
GO

IF OBJECT_ID(N'dbo.fato_fretes_faturamento', N'U') IS NOT NULL
   AND NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = N'IX_fato_ff_cancelados'
          AND object_id = OBJECT_ID(N'dbo.fato_fretes_faturamento')
   )
BEGIN
    CREATE NONCLUSTERED INDEX IX_fato_ff_cancelados
        ON dbo.fato_fretes_faturamento(is_cte_cancelado, data_referencia_faturamento_date)
        INCLUDE (
            frete_id, numero_minuta, chave_cte, status_cte_real,
            status_cte_result, cte_cancelado_evidencia, receita_bruta_original
        )
        WHERE excluido_na_origem = 0
        WITH (DATA_COMPRESSION = PAGE)
        ON PS_fato_ff_data_referencia_mes(data_referencia_faturamento_date);

    PRINT 'Indice IX_fato_ff_cancelados criado.';
END;
GO
