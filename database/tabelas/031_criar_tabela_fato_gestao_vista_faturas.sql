-- ============================================================================
-- Tabela fato materializada para Faturas por Cliente
-- Grao: 1 linha por titulo/unique_id de dbo.faturas_por_cliente.
-- ============================================================================

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;
GO

IF NOT EXISTS (SELECT 1 FROM sys.partition_functions WHERE name = N'PF_fato_gvf_data_emissao_mes')
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
        CREATE PARTITION FUNCTION PF_fato_gvf_data_emissao_mes (DATE)
        AS RANGE RIGHT FOR VALUES (' + @limites + N');
    ');

    PRINT 'Partition Function PF_fato_gvf_data_emissao_mes criada.';
END
ELSE
BEGIN
    PRINT 'Partition Function PF_fato_gvf_data_emissao_mes ja existe.';
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.partition_schemes WHERE name = N'PS_fato_gvf_data_emissao_mes')
BEGIN
    EXEC(N'
        CREATE PARTITION SCHEME PS_fato_gvf_data_emissao_mes
        AS PARTITION PF_fato_gvf_data_emissao_mes
        ALL TO ([PRIMARY]);
    ');

    PRINT 'Partition Scheme PS_fato_gvf_data_emissao_mes criado.';
END
ELSE
BEGIN
    PRINT 'Partition Scheme PS_fato_gvf_data_emissao_mes ja existe.';
END;
GO

IF OBJECT_ID(N'dbo.fato_gestao_vista_faturas', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.fato_gestao_vista_faturas (
        unique_id NVARCHAR(100) NOT NULL,
        chave_normalizacao NVARCHAR(220) NOT NULL,

        documento_fatura NVARCHAR(100) NULL,
        numero_fatura NVARCHAR(50) NULL,
        numero_documento NVARCHAR(50) NULL,
        numero_cte BIGINT NULL,
        chave_cte NVARCHAR(100) NULL,
        numero_nfse BIGINT NULL,
        serie_nfse NVARCHAR(50) NULL,

        data_emissao_cte DATETIMEOFFSET NULL,
        data_emissao_cte_date DATE NULL,
        data_emissao_fatura DATE NULL,
        data_emissao_fatura_yyyymm INT NULL,
        data_vencimento_fatura DATE NULL,
        data_baixa_fatura DATE NULL,
        data_base_prazo DATE NULL,
        data_referencia_mensal DATE NULL,
        data_referencia_mensal_yyyymm INT NULL,
        fit_ant_ils_original_due_date DATE NULL,

        filial NVARCHAR(255) NULL,
        filial_key NVARCHAR(255) NULL,
        estado NVARCHAR(50) NULL,
        pagador_nome NVARCHAR(255) NULL,
        pagador_documento NVARCHAR(50) NULL,
        pagador_documento_key NVARCHAR(50) NULL,
        cliente_nome NVARCHAR(255) NULL,
        cliente_cnpj NVARCHAR(50) NULL,
        cliente_cnpj_key NVARCHAR(50) NULL,
        cliente_chave NVARCHAR(320) NULL,
        remetente_nome NVARCHAR(255) NULL,
        remetente_documento NVARCHAR(50) NULL,
        destinatario_nome NVARCHAR(255) NULL,
        destinatario_documento NVARCHAR(50) NULL,
        vendedor_nome NVARCHAR(255) NULL,

        status_processo NVARCHAR(50) NOT NULL CONSTRAINT DF_fato_gvf_status_processo DEFAULT (N'Aguardando Faturamento'),
        status_pagamento NVARCHAR(50) NOT NULL CONSTRAINT DF_fato_gvf_status_pagamento DEFAULT (N'sem_fatura'),
        status_cte NVARCHAR(255) NULL,
        status_cte_result NVARCHAR(4000) NULL,
        tipo_frete NVARCHAR(100) NULL,
        classificacao NVARCHAR(100) NULL,

        valor_frete DECIMAL(19, 4) NULL,
        valor_fatura DECIMAL(19, 4) NULL,
        valor_fit_ant DECIMAL(19, 4) NULL,
        third_party_ctes_value DECIMAL(19, 4) NULL,
        valor_operacional DECIMAL(19, 4) NOT NULL CONSTRAINT DF_fato_gvf_valor_operacional DEFAULT (0),

        notas_fiscais NVARCHAR(4000) NULL,
        pedidos_cliente NVARCHAR(4000) NULL,
        metadata NVARCHAR(4000) NULL,

        excluido_na_origem BIT NOT NULL CONSTRAINT DF_fato_gvf_excluido_na_origem DEFAULT (0),
        data_exclusao_origem DATETIME2(0) NULL,
        ultima_reconciliacao_origem_em DATETIME2(0) NULL,
        data_extracao_origem DATETIME2(0) NULL,
        snapshot_em DATETIME2(0) NOT NULL CONSTRAINT DF_fato_gvf_snapshot_em DEFAULT SYSUTCDATETIME(),
        hash_linha BINARY(32) NULL,

        CONSTRAINT CK_fato_gvf_emissao_yyyymm
            CHECK (data_emissao_fatura_yyyymm IS NULL OR data_emissao_fatura_yyyymm BETWEEN 202001 AND 203212),
        CONSTRAINT CK_fato_gvf_referencia_yyyymm
            CHECK (data_referencia_mensal_yyyymm IS NULL OR data_referencia_mensal_yyyymm BETWEEN 202001 AND 203212),
        CONSTRAINT CK_fato_gvf_status_processo
            CHECK (status_processo IN (N'Faturado', N'Aguardando Faturamento')),
        CONSTRAINT CK_fato_gvf_status_pagamento
            CHECK (status_pagamento IN (N'sem_fatura', N'a_vencer', N'vencido', N'baixado', N'sem_vencimento'))
    )
    ON PS_fato_gvf_data_emissao_mes(data_emissao_fatura);

    PRINT 'Tabela dbo.fato_gestao_vista_faturas criada.';
END
ELSE
BEGIN
    PRINT 'Tabela dbo.fato_gestao_vista_faturas ja existe.';
END;
GO

IF OBJECT_ID(N'dbo.fato_gestao_vista_faturas', N'U') IS NOT NULL
   AND NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = N'CCI_fato_gvf'
          AND object_id = OBJECT_ID(N'dbo.fato_gestao_vista_faturas')
   )
BEGIN
    CREATE CLUSTERED COLUMNSTORE INDEX CCI_fato_gvf
        ON dbo.fato_gestao_vista_faturas
        ON PS_fato_gvf_data_emissao_mes(data_emissao_fatura);

    PRINT 'Clustered Columnstore Index CCI_fato_gvf criado.';
END;
GO

IF OBJECT_ID(N'dbo.fato_gestao_vista_faturas', N'U') IS NOT NULL
   AND NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = N'UX_fato_gvf_unique_id_data'
          AND object_id = OBJECT_ID(N'dbo.fato_gestao_vista_faturas')
   )
BEGIN
    CREATE UNIQUE NONCLUSTERED INDEX UX_fato_gvf_unique_id_data
        ON dbo.fato_gestao_vista_faturas(unique_id, data_emissao_fatura)
        WITH (DATA_COMPRESSION = PAGE)
        ON PS_fato_gvf_data_emissao_mes(data_emissao_fatura);

    PRINT 'Indice UX_fato_gvf_unique_id_data criado.';
END;
GO

IF OBJECT_ID(N'dbo.fato_gestao_vista_faturas', N'U') IS NOT NULL
   AND NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = N'IX_fato_gvf_merge'
          AND object_id = OBJECT_ID(N'dbo.fato_gestao_vista_faturas')
   )
BEGIN
    CREATE NONCLUSTERED INDEX IX_fato_gvf_merge
        ON dbo.fato_gestao_vista_faturas(unique_id)
        INCLUDE (data_emissao_fatura, hash_linha, excluido_na_origem, snapshot_em)
        WITH (DATA_COMPRESSION = PAGE)
        ON PS_fato_gvf_data_emissao_mes(data_emissao_fatura);

    PRINT 'Indice IX_fato_gvf_merge criado.';
END;
GO

IF OBJECT_ID(N'dbo.fato_gestao_vista_faturas', N'U') IS NOT NULL
   AND NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = N'IX_fato_gvf_aging'
          AND object_id = OBJECT_ID(N'dbo.fato_gestao_vista_faturas')
   )
BEGIN
    CREATE NONCLUSTERED INDEX IX_fato_gvf_aging
        ON dbo.fato_gestao_vista_faturas(
            data_emissao_cte_date,
            documento_fatura,
            data_baixa_fatura,
            data_vencimento_fatura
        )
        INCLUDE (
            valor_operacional, status_pagamento, cliente_chave,
            cliente_nome, cliente_cnpj, filial, data_extracao_origem
        )
        WHERE excluido_na_origem = 0
        WITH (DATA_COMPRESSION = PAGE)
        ON PS_fato_gvf_data_emissao_mes(data_emissao_fatura);

    PRINT 'Indice IX_fato_gvf_aging criado.';
END;
GO

IF OBJECT_ID(N'dbo.fato_gestao_vista_faturas', N'U') IS NOT NULL
   AND NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = N'IX_fato_gvf_paginacao_periodo'
          AND object_id = OBJECT_ID(N'dbo.fato_gestao_vista_faturas')
   )
BEGIN
    CREATE NONCLUSTERED INDEX IX_fato_gvf_paginacao_periodo
        ON dbo.fato_gestao_vista_faturas(
            data_emissao_cte DESC,
            unique_id DESC
        )
        INCLUDE (
            data_emissao_cte_date, documento_fatura, data_emissao_fatura,
            data_vencimento_fatura, data_baixa_fatura, filial, pagador_nome,
            pagador_documento, cliente_cnpj, numero_cte, valor_operacional,
            status_processo
        )
        WHERE excluido_na_origem = 0
        WITH (DATA_COMPRESSION = PAGE)
        ON PS_fato_gvf_data_emissao_mes(data_emissao_fatura);

    PRINT 'Indice IX_fato_gvf_paginacao_periodo criado.';
END;
GO

IF OBJECT_ID(N'dbo.fato_gestao_vista_faturas', N'U') IS NOT NULL
   AND NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = N'IX_fato_gvf_cliente_cnpj'
          AND object_id = OBJECT_ID(N'dbo.fato_gestao_vista_faturas')
   )
BEGIN
    CREATE NONCLUSTERED INDEX IX_fato_gvf_cliente_cnpj
        ON dbo.fato_gestao_vista_faturas(cliente_cnpj_key, data_emissao_cte_date)
        INCLUDE (cliente_nome, cliente_cnpj, pagador_nome, pagador_documento, filial)
        WHERE excluido_na_origem = 0 AND cliente_cnpj_key IS NOT NULL
        WITH (DATA_COMPRESSION = PAGE)
        ON PS_fato_gvf_data_emissao_mes(data_emissao_fatura);

    PRINT 'Indice IX_fato_gvf_cliente_cnpj criado.';
END;
GO
