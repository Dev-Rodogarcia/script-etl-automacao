PRINT 'Migration 029: criar fato materializada de Gestao a Vista para fretes';
GO

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;
SET XACT_ABORT ON;
GO

DECLARE @MigrationId NVARCHAR(255) = N'029_criar_fato_gestao_vista_fretes';

IF OBJECT_ID(N'dbo.schema_migrations', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.schema_migrations (
        migration_id NVARCHAR(255) NOT NULL,
        applied_at DATETIME2(0) NOT NULL CONSTRAINT DF_schema_migrations_applied_at DEFAULT SYSUTCDATETIME(),
        checksum_sha256 VARCHAR(64) NULL,
        notes NVARCHAR(500) NULL,
        CONSTRAINT PK_schema_migrations PRIMARY KEY (migration_id)
    );
END;

IF EXISTS (SELECT 1 FROM dbo.schema_migrations WHERE migration_id = @MigrationId)
BEGIN
    PRINT 'Migracao 029_criar_fato_gestao_vista_fretes ja aplicada. Nenhuma acao necessaria.';
    RETURN;
END;

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

IF OBJECT_ID(N'dbo.fato_gestao_vista_fretes', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.fato_gestao_vista_fretes (
        indicador_codigo CHAR(2) NOT NULL,
        data_referencia DATE NOT NULL,
        data_referencia_yyyymm INT NOT NULL,

        numero_minuta BIGINT NOT NULL,
        frete_id_origem BIGINT NULL,

        data_frete DATETIMEOFFSET NULL,
        data_frete_date DATE NULL,
        data_previsao_entrega DATE NULL,
        data_finalizacao_performance DATE NULL,

        filial_emissora NVARCHAR(255) NULL,
        filial_emissora_key NVARCHAR(255) NULL,
        filial_performance NVARCHAR(255) NULL,
        filial_performance_key NVARCHAR(255) NULL,
        responsavel_regiao_destino NVARCHAR(255) NULL,
        responsavel_regiao_destino_key NVARCHAR(255) NULL,
        regiao_destino NVARCHAR(255) NULL,

        pagador_nome NVARCHAR(255) NULL,
        pagador_documento NVARCHAR(50) NULL,
        pagador_documento_key NVARCHAR(50) NULL,
        remetente_documento NVARCHAR(50) NULL,
        destinatario_documento NVARCHAR(50) NULL,

        destino NVARCHAR(255) NULL,
        destino_cidade NVARCHAR(255) NULL,
        destino_uf NVARCHAR(10) NULL,

        status_norm NVARCHAR(50) NULL,
        documento_oficial_tipo NVARCHAR(50) NULL,
        tipo_frete NVARCHAR(100) NULL,
        modal NVARCHAR(50) NULL,

        valor_total DECIMAL(18, 2) NULL,
        peso_taxado DECIMAL(18, 3) NULL,
        peso_real DECIMAL(18, 3) NULL,
        peso_cubado DECIMAL(18, 6) NULL,
        total_m3 DECIMAL(18, 6) NULL,
        volumes INT NULL,

        performance_diferenca_dias SMALLINT NULL,
        performance_status NVARCHAR(50) NULL,
        performance_status_codigo TINYINT NULL,

        is_no_prazo BIT NOT NULL CONSTRAINT DF_fato_gv_fretes_is_no_prazo DEFAULT (0),
        is_fora_prazo BIT NOT NULL CONSTRAINT DF_fato_gv_fretes_is_fora_prazo DEFAULT (0),
        is_cubado BIT NOT NULL CONSTRAINT DF_fato_gv_fretes_is_cubado DEFAULT (0),
        is_peso_real_informado BIT NOT NULL CONSTRAINT DF_fato_gv_fretes_is_peso_real DEFAULT (0),
        is_cancelado BIT NOT NULL CONSTRAINT DF_fato_gv_fretes_is_cancelado DEFAULT (0),
        is_cortesia BIT NOT NULL CONSTRAINT DF_fato_gv_fretes_is_cortesia DEFAULT (0),
        is_documento_emitido BIT NOT NULL CONSTRAINT DF_fato_gv_fretes_is_doc_emitido DEFAULT (0),
        is_elegivel_operacional BIT NOT NULL CONSTRAINT DF_fato_gv_fretes_is_eleg_operacional DEFAULT (0),
        is_elegivel_operacional_com_valor BIT NOT NULL CONSTRAINT DF_fato_gv_fretes_is_eleg_valor DEFAULT (0),
        is_pagador_excluido_cubagem BIT NOT NULL CONSTRAINT DF_fato_gv_fretes_is_pagador_exc_cubagem DEFAULT (0),
        is_linha_valida_indicador BIT NOT NULL CONSTRAINT DF_fato_gv_fretes_is_linha_valida DEFAULT (0),

        excluido_na_origem BIT NOT NULL CONSTRAINT DF_fato_gv_fretes_excluido_na_origem DEFAULT (0),
        data_exclusao_origem DATETIME2(0) NULL,
        ultima_reconciliacao_origem_em DATETIME2(0) NULL,

        data_extracao DATETIME2(0) NULL,
        snapshot_em DATETIME2(0) NOT NULL CONSTRAINT DF_fato_gv_fretes_snapshot_em DEFAULT SYSUTCDATETIME(),
        hash_linha BINARY(32) NULL,

        CONSTRAINT CK_fato_gv_fretes_indicador
            CHECK (indicador_codigo IN ('PE', 'CB')),
        CONSTRAINT CK_fato_gv_fretes_status_performance
            CHECK (performance_status_codigo IS NULL OR performance_status_codigo IN (0, 1, 2)),
        CONSTRAINT CK_fato_gv_fretes_data_referencia_pe
            CHECK (indicador_codigo <> 'PE' OR data_referencia = data_previsao_entrega),
        CONSTRAINT CK_fato_gv_fretes_data_referencia_cb
            CHECK (indicador_codigo <> 'CB' OR data_referencia = data_frete_date)
    )
    ON PS_fato_gv_data_referencia_mes(data_referencia);

    PRINT 'Tabela dbo.fato_gestao_vista_fretes criada.';
END
ELSE
BEGIN
    PRINT 'Tabela dbo.fato_gestao_vista_fretes ja existe.';
END;

IF OBJECT_ID(N'dbo.fato_gestao_vista_fretes', N'U') IS NOT NULL
   AND NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = N'CCI_fato_gv_fretes'
          AND object_id = OBJECT_ID(N'dbo.fato_gestao_vista_fretes')
   )
BEGIN
    CREATE CLUSTERED COLUMNSTORE INDEX CCI_fato_gv_fretes
        ON dbo.fato_gestao_vista_fretes
        ON PS_fato_gv_data_referencia_mes(data_referencia);

    PRINT 'Clustered Columnstore Index CCI_fato_gv_fretes criado.';
END;

IF OBJECT_ID(N'dbo.fato_gestao_vista_fretes', N'U') IS NOT NULL
   AND NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = N'UX_fato_gv_fretes_indicador_minuta'
          AND object_id = OBJECT_ID(N'dbo.fato_gestao_vista_fretes')
   )
BEGIN
    CREATE UNIQUE NONCLUSTERED INDEX UX_fato_gv_fretes_indicador_minuta
        ON dbo.fato_gestao_vista_fretes(indicador_codigo, numero_minuta, data_referencia)
        WITH (DATA_COMPRESSION = PAGE)
        ON PS_fato_gv_data_referencia_mes(data_referencia);

    PRINT 'Indice UX_fato_gv_fretes_indicador_minuta criado.';
END;

IF OBJECT_ID(N'dbo.fato_gestao_vista_fretes', N'U') IS NOT NULL
   AND NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = N'IX_fato_gv_fretes_merge'
          AND object_id = OBJECT_ID(N'dbo.fato_gestao_vista_fretes')
   )
BEGIN
    CREATE NONCLUSTERED INDEX IX_fato_gv_fretes_merge
        ON dbo.fato_gestao_vista_fretes(indicador_codigo, numero_minuta)
        INCLUDE (data_referencia, hash_linha, excluido_na_origem, snapshot_em)
        WITH (DATA_COMPRESSION = PAGE)
        ON PS_fato_gv_data_referencia_mes(data_referencia);

    PRINT 'Indice IX_fato_gv_fretes_merge criado.';
END;

IF OBJECT_ID(N'dbo.fato_gestao_vista_fretes', N'U') IS NOT NULL
   AND NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = N'IX_fato_gv_fretes_pe_periodo_filial'
          AND object_id = OBJECT_ID(N'dbo.fato_gestao_vista_fretes')
   )
BEGIN
    CREATE NONCLUSTERED INDEX IX_fato_gv_fretes_pe_periodo_filial
        ON dbo.fato_gestao_vista_fretes(data_referencia, filial_performance_key, performance_status_codigo)
        INCLUDE (
            numero_minuta, data_frete, filial_performance, filial_emissora,
            data_finalizacao_performance, performance_diferenca_dias,
            performance_status, is_no_prazo, is_fora_prazo, data_extracao
        )
        WHERE indicador_codigo = 'PE'
          AND is_linha_valida_indicador = 1
          AND excluido_na_origem = 0
        WITH (DATA_COMPRESSION = PAGE)
        ON PS_fato_gv_data_referencia_mes(data_referencia);

    PRINT 'Indice IX_fato_gv_fretes_pe_periodo_filial criado.';
END;

IF OBJECT_ID(N'dbo.fato_gestao_vista_fretes', N'U') IS NOT NULL
   AND NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = N'IX_fato_gv_fretes_cb_periodo_filial'
          AND object_id = OBJECT_ID(N'dbo.fato_gestao_vista_fretes')
   )
BEGIN
    CREATE NONCLUSTERED INDEX IX_fato_gv_fretes_cb_periodo_filial
        ON dbo.fato_gestao_vista_fretes(data_referencia, filial_emissora_key, is_cubado)
        INCLUDE (
            numero_minuta, data_frete, filial_emissora, pagador_nome,
            pagador_documento_key, destino, peso_taxado, peso_real,
            peso_cubado, total_m3, is_peso_real_informado, data_extracao
        )
        WHERE indicador_codigo = 'CB'
          AND is_linha_valida_indicador = 1
          AND is_pagador_excluido_cubagem = 0
          AND excluido_na_origem = 0
        WITH (DATA_COMPRESSION = PAGE)
        ON PS_fato_gv_data_referencia_mes(data_referencia);

    PRINT 'Indice IX_fato_gv_fretes_cb_periodo_filial criado.';
END;

INSERT INTO dbo.schema_migrations (migration_id, notes)
VALUES (
    @MigrationId,
    N'Cria tabela fato materializada para Gestao a Vista de fretes, particionada por data_referencia e otimizada com columnstore e indices filtrados.'
);

PRINT 'Migration 029_criar_fato_gestao_vista_fretes concluida com sucesso.';
GO
