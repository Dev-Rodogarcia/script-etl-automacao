PRINT 'Migration 039: criar dim_calendario e preparar retroacao de faturamento';
GO

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;
SET XACT_ABORT ON;

DECLARE @MigrationId NVARCHAR(255) = N'039_criar_dim_calendario_referencia_faturamento';

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
    PRINT 'Migracao 039_criar_dim_calendario_referencia_faturamento ja aplicada. Nenhuma acao necessaria.';
    RETURN;
END;

DECLARE @DataInicio DATE = '2019-12-01';
DECLARE @DataFim DATE = '2032-12-31';

IF OBJECT_ID(N'dbo.dim_calendario', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.dim_calendario (
        data DATE NOT NULL,
        data_key INT NOT NULL,
        ano SMALLINT NOT NULL,
        mes TINYINT NOT NULL,
        dia TINYINT NOT NULL,
        trimestre TINYINT NOT NULL,
        semana_iso TINYINT NOT NULL,
        dia_semana_iso TINYINT NOT NULL,
        nome_dia_semana NVARCHAR(20) NOT NULL,
        is_fim_semana BIT NOT NULL,
        is_feriado_nacional BIT NOT NULL CONSTRAINT DF_dim_cal_is_feriado_nacional DEFAULT (0),
        nome_feriado_nacional NVARCHAR(100) NULL,
        is_ponto_facultativo BIT NOT NULL CONSTRAINT DF_dim_cal_is_ponto_facultativo DEFAULT (0),
        nome_ponto_facultativo NVARCHAR(100) NULL,
        is_dia_util BIT NOT NULL,
        data_referencia_faturamento DATE NOT NULL,
        data_referencia_faturamento_key INT NOT NULL,
        ano_mes INT NOT NULL,
        atualizado_em DATETIME2(0) NOT NULL CONSTRAINT DF_dim_cal_atualizado_em DEFAULT SYSUTCDATETIME(),

        CONSTRAINT PK_dim_calendario PRIMARY KEY CLUSTERED (data),
        CONSTRAINT UX_dim_calendario_data_key UNIQUE (data_key),
        CONSTRAINT CK_dim_cal_data_referencia
            CHECK (data_referencia_faturamento <= data),
        CONSTRAINT CK_dim_cal_dia_semana_iso
            CHECK (dia_semana_iso BETWEEN 1 AND 7),
        CONSTRAINT CK_dim_cal_ano_mes
            CHECK (ano_mes BETWEEN 201912 AND 203212)
    );

    PRINT 'Tabela dbo.dim_calendario criada.';
END
ELSE
BEGIN
    PRINT 'Tabela dbo.dim_calendario ja existe.';
END;

;WITH numeros AS (
    SELECT TOP (DATEDIFF(DAY, @DataInicio, @DataFim) + 1)
        ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) - 1 AS n
    FROM sys.all_objects a
    CROSS JOIN sys.all_objects b
),
datas AS (
    SELECT DATEADD(DAY, n, @DataInicio) AS data
    FROM numeros
),
base AS (
    SELECT
        data,
        YEAR(data) * 10000 + MONTH(data) * 100 + DAY(data) AS data_key,
        YEAR(data) AS ano,
        MONTH(data) AS mes,
        DAY(data) AS dia,
        DATEPART(QUARTER, data) AS trimestre,
        DATEPART(ISO_WEEK, data) AS semana_iso,
        (DATEDIFF(DAY, CONVERT(DATE, '19000101'), data) % 7) + 1 AS dia_semana_iso,
        CASE (DATEDIFF(DAY, CONVERT(DATE, '19000101'), data) % 7) + 1
            WHEN 1 THEN N'Segunda-feira'
            WHEN 2 THEN N'Terca-feira'
            WHEN 3 THEN N'Quarta-feira'
            WHEN 4 THEN N'Quinta-feira'
            WHEN 5 THEN N'Sexta-feira'
            WHEN 6 THEN N'Sabado'
            ELSE N'Domingo'
        END AS nome_dia_semana,
        CASE WHEN (DATEDIFF(DAY, CONVERT(DATE, '19000101'), data) % 7) + 1 IN (6, 7) THEN 1 ELSE 0 END AS is_fim_semana,
        YEAR(data) * 100 + MONTH(data) AS ano_mes
    FROM datas
)
MERGE dbo.dim_calendario AS target
USING base AS source
   ON target.data = source.data
WHEN MATCHED THEN UPDATE SET
    data_key = source.data_key,
    ano = source.ano,
    mes = source.mes,
    dia = source.dia,
    trimestre = source.trimestre,
    semana_iso = source.semana_iso,
    dia_semana_iso = source.dia_semana_iso,
    nome_dia_semana = source.nome_dia_semana,
    is_fim_semana = source.is_fim_semana,
    ano_mes = source.ano_mes,
    atualizado_em = SYSUTCDATETIME()
WHEN NOT MATCHED BY TARGET THEN INSERT (
    data, data_key, ano, mes, dia, trimestre, semana_iso, dia_semana_iso,
    nome_dia_semana, is_fim_semana, is_feriado_nacional, nome_feriado_nacional,
    is_ponto_facultativo, nome_ponto_facultativo, is_dia_util,
    data_referencia_faturamento, data_referencia_faturamento_key, ano_mes
)
VALUES (
    source.data, source.data_key, source.ano, source.mes, source.dia,
    source.trimestre, source.semana_iso, source.dia_semana_iso,
    source.nome_dia_semana, source.is_fim_semana, 0, NULL, 0, NULL,
    CASE WHEN source.is_fim_semana = 1 THEN 0 ELSE 1 END,
    source.data, source.data_key, source.ano_mes
);

DECLARE @Feriados TABLE (
    data DATE NOT NULL PRIMARY KEY,
    nome NVARCHAR(100) NOT NULL,
    is_feriado_nacional BIT NOT NULL,
    is_ponto_facultativo BIT NOT NULL
);

;WITH anos AS (
    SELECT YEAR(@DataInicio) AS ano
    UNION ALL
    SELECT ano + 1
    FROM anos
    WHERE ano < YEAR(@DataFim)
),
pascoa AS (
    SELECT
        ano,
        DATEFROMPARTS(
            ano,
            ((h + l - 7 * m + 114) / 31),
            ((h + l - 7 * m + 114) % 31) + 1
        ) AS data_pascoa
    FROM (
        SELECT
            ano,
            a,
            b,
            c,
            d,
            e,
            f,
            g,
            h,
            i,
            k,
            l,
            (a + 11 * h + 22 * l) / 451 AS m
        FROM (
            SELECT
                ano,
                a,
                b,
                c,
                d,
                e,
                f,
                g,
                h,
                i,
                k,
                (32 + 2 * e + 2 * i - h - k) % 7 AS l
            FROM (
                SELECT
                    ano,
                    a,
                    b,
                    c,
                    d,
                    e,
                    f,
                    g,
                    (19 * a + b - d - g + 15) % 30 AS h,
                    c / 4 AS i,
                    c % 4 AS k
                FROM (
                    SELECT
                        ano,
                        ano % 19 AS a,
                        ano / 100 AS b,
                        ano % 100 AS c,
                        (ano / 100) / 4 AS d,
                        (ano / 100) % 4 AS e,
                        ((ano / 100) + 8) / 25 AS f,
                        ((ano / 100) - (((ano / 100) + 8) / 25) + 1) / 3 AS g
                    FROM anos
                ) s1
            ) s2
        ) s3
    ) s4
)
INSERT INTO @Feriados (data, nome, is_feriado_nacional, is_ponto_facultativo)
SELECT data, nome, is_feriado_nacional, is_ponto_facultativo
FROM (
    SELECT DATEFROMPARTS(ano, 1, 1), N'Confraternizacao Universal', 1, 0 FROM anos
    UNION ALL SELECT DATEFROMPARTS(ano, 4, 21), N'Tiradentes', 1, 0 FROM anos
    UNION ALL SELECT DATEFROMPARTS(ano, 5, 1), N'Dia do Trabalho', 1, 0 FROM anos
    UNION ALL SELECT DATEFROMPARTS(ano, 9, 7), N'Independencia do Brasil', 1, 0 FROM anos
    UNION ALL SELECT DATEFROMPARTS(ano, 10, 12), N'Nossa Senhora Aparecida', 1, 0 FROM anos
    UNION ALL SELECT DATEFROMPARTS(ano, 11, 2), N'Finados', 1, 0 FROM anos
    UNION ALL SELECT DATEFROMPARTS(ano, 11, 15), N'Proclamacao da Republica', 1, 0 FROM anos
    UNION ALL SELECT DATEFROMPARTS(ano, 11, 20), N'Consciencia Negra', 1, 0 FROM anos WHERE ano >= 2024
    UNION ALL SELECT DATEFROMPARTS(ano, 12, 25), N'Natal', 1, 0 FROM anos
    UNION ALL SELECT DATEADD(DAY, -2, data_pascoa), N'Sexta-feira Santa', 1, 0 FROM pascoa
    UNION ALL SELECT DATEADD(DAY, -48, data_pascoa), N'Carnaval - segunda-feira', 0, 1 FROM pascoa
    UNION ALL SELECT DATEADD(DAY, -47, data_pascoa), N'Carnaval - terca-feira', 0, 1 FROM pascoa
    UNION ALL SELECT DATEADD(DAY, 60, data_pascoa), N'Corpus Christi', 0, 1 FROM pascoa
) feriados(data, nome, is_feriado_nacional, is_ponto_facultativo)
WHERE data BETWEEN @DataInicio AND @DataFim
OPTION (MAXRECURSION 0);

UPDATE c
   SET is_feriado_nacional = f.is_feriado_nacional,
       nome_feriado_nacional = CASE WHEN f.is_feriado_nacional = 1 THEN f.nome ELSE c.nome_feriado_nacional END,
       is_ponto_facultativo = f.is_ponto_facultativo,
       nome_ponto_facultativo = CASE WHEN f.is_ponto_facultativo = 1 THEN f.nome ELSE c.nome_ponto_facultativo END,
       atualizado_em = SYSUTCDATETIME()
FROM dbo.dim_calendario c
INNER JOIN @Feriados f
    ON f.data = c.data;

UPDATE c
   SET is_dia_util = CASE
            WHEN c.is_fim_semana = 1 THEN 0
            WHEN c.is_feriado_nacional = 1 THEN 0
            WHEN c.is_ponto_facultativo = 1 THEN 0
            ELSE 1
       END,
       atualizado_em = SYSUTCDATETIME()
FROM dbo.dim_calendario c
WHERE c.data BETWEEN @DataInicio AND @DataFim;

UPDATE c
   SET data_referencia_faturamento = ref.data,
       data_referencia_faturamento_key = ref.data_key,
       atualizado_em = SYSUTCDATETIME()
FROM dbo.dim_calendario c
CROSS APPLY (
    SELECT TOP (1) d.data, d.data_key
    FROM dbo.dim_calendario d
    WHERE d.data <= c.data
      AND d.is_dia_util = 1
    ORDER BY d.data DESC
) ref
WHERE c.data BETWEEN @DataInicio AND @DataFim;

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_dim_calendario_referencia_faturamento'
      AND object_id = OBJECT_ID(N'dbo.dim_calendario')
)
BEGIN
    CREATE NONCLUSTERED INDEX IX_dim_calendario_referencia_faturamento
        ON dbo.dim_calendario(data_referencia_faturamento, data)
        INCLUDE (is_dia_util, is_feriado_nacional, is_ponto_facultativo, nome_feriado_nacional, nome_ponto_facultativo)
        WITH (DATA_COMPRESSION = PAGE);

    PRINT 'Indice IX_dim_calendario_referencia_faturamento criado.';
END;

IF OBJECT_ID(N'dbo.fato_fretes_faturamento', N'U') IS NOT NULL
BEGIN
    IF COL_LENGTH(N'dbo.fato_fretes_faturamento', N'data_referencia_faturamento_real') IS NULL
    BEGIN
        ALTER TABLE dbo.fato_fretes_faturamento ADD data_referencia_faturamento_real DATETIMEOFFSET NULL;
        PRINT 'Coluna dbo.fato_fretes_faturamento.data_referencia_faturamento_real adicionada.';
    END;

    IF COL_LENGTH(N'dbo.fato_fretes_faturamento', N'data_referencia_faturamento_real_date') IS NULL
    BEGIN
        ALTER TABLE dbo.fato_fretes_faturamento ADD data_referencia_faturamento_real_date DATE NULL;
        PRINT 'Coluna dbo.fato_fretes_faturamento.data_referencia_faturamento_real_date adicionada.';
    END;

    IF COL_LENGTH(N'dbo.fato_fretes_faturamento', N'data_referencia_faturamento_real_yyyymm') IS NULL
    BEGIN
        ALTER TABLE dbo.fato_fretes_faturamento ADD data_referencia_faturamento_real_yyyymm INT NULL;
        PRINT 'Coluna dbo.fato_fretes_faturamento.data_referencia_faturamento_real_yyyymm adicionada.';
    END;

    IF COL_LENGTH(N'dbo.fato_fretes_faturamento', N'is_data_faturamento_retroagida') IS NULL
    BEGIN
        ALTER TABLE dbo.fato_fretes_faturamento
            ADD is_data_faturamento_retroagida BIT NOT NULL
                CONSTRAINT DF_fato_ff_is_data_retroagida DEFAULT (0);
        PRINT 'Coluna dbo.fato_fretes_faturamento.is_data_faturamento_retroagida adicionada.';
    END;

    -- As colunas podem ter sido adicionadas neste mesmo batch. SQL dinamico
    -- adia a compilacao do UPDATE ate o catalogo refletir os ALTER TABLE.
    EXEC sys.sp_executesql N'
        UPDATE dbo.fato_fretes_faturamento
           SET data_referencia_faturamento_real =
                   COALESCE(data_referencia_faturamento_real, data_referencia_faturamento),
               data_referencia_faturamento_real_date =
                   COALESCE(data_referencia_faturamento_real_date, data_referencia_faturamento_date),
               data_referencia_faturamento_real_yyyymm =
                   COALESCE(data_referencia_faturamento_real_yyyymm, data_referencia_faturamento_yyyymm)
         WHERE data_referencia_faturamento_real IS NULL
            OR data_referencia_faturamento_real_date IS NULL
            OR data_referencia_faturamento_real_yyyymm IS NULL;
    ';

    IF EXISTS (
        SELECT 1
        FROM sys.check_constraints
        WHERE name = N'CK_fato_ff_yyyymm'
          AND parent_object_id = OBJECT_ID(N'dbo.fato_fretes_faturamento')
    )
    BEGIN
        ALTER TABLE dbo.fato_fretes_faturamento DROP CONSTRAINT CK_fato_ff_yyyymm;
    END;

    ALTER TABLE dbo.fato_fretes_faturamento WITH CHECK ADD CONSTRAINT CK_fato_ff_yyyymm
        CHECK (data_referencia_faturamento_yyyymm BETWEEN 201912 AND 203212);
END;

INSERT INTO dbo.schema_migrations (migration_id, notes)
VALUES (
    @MigrationId,
    N'Cria dimensao calendario, popula feriados brasileiros e preserva data real de faturamento na fato para retroacao ao ultimo dia util.'
);

PRINT 'Migration 039_criar_dim_calendario_referencia_faturamento concluida com sucesso.';
GO
