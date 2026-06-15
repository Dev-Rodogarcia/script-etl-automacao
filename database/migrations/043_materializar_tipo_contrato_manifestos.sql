PRINT 'Migration 043: materializar tipo de contrato de veiculo/motorista em manifestos';
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

DECLARE @MigrationId NVARCHAR(255) = N'043_materializar_tipo_contrato_manifestos';

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
    PRINT 'Migracao 043_materializar_tipo_contrato_manifestos ja aplicada. Nenhuma acao necessaria.';
END;

IF OBJECT_ID(N'dbo.manifestos', N'U') IS NULL
    THROW 51080, 'Tabela dbo.manifestos nao encontrada. Execute as migrations base antes da 043.', 1;

IF OBJECT_ID(N'dbo.fato_gestao_vista_manifestos', N'U') IS NULL
    THROW 51081, 'Tabela dbo.fato_gestao_vista_manifestos nao encontrada. Execute a migration 042 antes da 043.', 1;

IF COL_LENGTH(N'dbo.manifestos', N'driver_contract_type') IS NULL
BEGIN
    ALTER TABLE dbo.manifestos
    ADD driver_contract_type NVARCHAR(50) NULL;
    PRINT 'Coluna dbo.manifestos.driver_contract_type adicionada.';
END;

IF COL_LENGTH(N'dbo.fato_gestao_vista_manifestos', N'tipo_contrato_veiculo') IS NULL
    ALTER TABLE dbo.fato_gestao_vista_manifestos ADD tipo_contrato_veiculo NVARCHAR(100) NULL;

IF COL_LENGTH(N'dbo.fato_gestao_vista_manifestos', N'tipo_contrato_veiculo_key') IS NULL
    ALTER TABLE dbo.fato_gestao_vista_manifestos ADD tipo_contrato_veiculo_key NVARCHAR(100) NULL;

IF COL_LENGTH(N'dbo.fato_gestao_vista_manifestos', N'tipo_contrato_motorista') IS NULL
    ALTER TABLE dbo.fato_gestao_vista_manifestos ADD tipo_contrato_motorista NVARCHAR(100) NULL;

IF COL_LENGTH(N'dbo.fato_gestao_vista_manifestos', N'tipo_contrato_motorista_key') IS NULL
    ALTER TABLE dbo.fato_gestao_vista_manifestos ADD tipo_contrato_motorista_key NVARCHAR(100) NULL;
GO

IF NOT EXISTS (SELECT 1 FROM dbo.schema_migrations WHERE migration_id = N'043_materializar_tipo_contrato_manifestos')
BEGIN
    UPDATE m
       SET driver_contract_type = NULLIF(JSON_VALUE(m.metadata, '$.mft_mdr_contract_type'), N'')
      FROM dbo.manifestos AS m
     WHERE (m.driver_contract_type IS NULL OR LTRIM(RTRIM(m.driver_contract_type)) = N'')
       AND ISJSON(m.metadata) = 1
       AND JSON_VALUE(m.metadata, '$.mft_mdr_contract_type') IS NOT NULL;

    ;WITH contratos AS (
        SELECT
            m.sequence_code,
            MAX(m.contract_type) AS contract_type_vehicle_raw,
            MAX(m.driver_contract_type) AS contract_type_driver_raw
        FROM dbo.manifestos AS m
        WHERE COALESCE(m.excluido_na_origem, 0) = 0
        GROUP BY m.sequence_code
    ),
    traduzidos AS (
        SELECT
            c.sequence_code,
            CASE c.contract_type_vehicle_raw
                WHEN N'aggregate' THEN N'Agregado'
                WHEN N'driver' THEN N'Motorista'
                ELSE c.contract_type_vehicle_raw
            END AS tipo_contrato_veiculo,
            CASE c.contract_type_driver_raw
                WHEN N'company' THEN N'Próprio'
                WHEN N'aggregate' THEN N'Agregado'
                WHEN N'third_party' THEN N'Terceiro'
                ELSE c.contract_type_driver_raw
            END AS tipo_contrato_motorista
        FROM contratos AS c
    ),
    classificados AS (
        SELECT
            t.sequence_code,
            t.tipo_contrato_veiculo,
            t.tipo_contrato_motorista,
            CASE
                WHEN t.tipo_contrato_veiculo = N'Agregado' AND t.tipo_contrato_motorista = N'Agregado' THEN N'Agregado'
                WHEN t.tipo_contrato_veiculo = N'Agregado' AND t.tipo_contrato_motorista = N'Terceiro' THEN N'Terceiro'
                WHEN t.tipo_contrato_veiculo = N'Agregado' AND t.tipo_contrato_motorista = N'Próprio' THEN N'Frota'
                WHEN t.tipo_contrato_veiculo = N'Motorista' AND t.tipo_contrato_motorista = N'Agregado' THEN N'Frota + PX'
                WHEN t.tipo_contrato_veiculo = N'Motorista' AND t.tipo_contrato_motorista = N'Terceiro' THEN N'Frota + PX'
                WHEN t.tipo_contrato_veiculo = N'Motorista' AND t.tipo_contrato_motorista = N'Próprio' THEN N'Frota'
                ELSE NULL
            END AS tipo_contrato
        FROM traduzidos AS t
    )
    UPDATE f
       SET tipo_contrato_veiculo = c.tipo_contrato_veiculo,
           tipo_contrato_veiculo_key = NULLIF(LOWER(LTRIM(RTRIM(c.tipo_contrato_veiculo))), N''),
           tipo_contrato_motorista = c.tipo_contrato_motorista,
           tipo_contrato_motorista_key = NULLIF(LOWER(LTRIM(RTRIM(c.tipo_contrato_motorista))), N''),
           tipo_contrato = c.tipo_contrato,
           tipo_contrato_key = NULLIF(LOWER(LTRIM(RTRIM(c.tipo_contrato))), N''),
           hash_linha = NULL,
           snapshot_em = SYSUTCDATETIME()
      FROM dbo.fato_gestao_vista_manifestos AS f
      JOIN classificados AS c
        ON c.sequence_code = f.sequence_code;

    INSERT INTO dbo.schema_migrations (migration_id, notes)
    VALUES (
        N'043_materializar_tipo_contrato_manifestos',
        N'Adiciona contrato do motorista do Data Export 6399 e recalcula Tipo de contrato final por CASE WHEN.'
    );
END;

PRINT 'Migration 043_materializar_tipo_contrato_manifestos concluida com sucesso.';
GO
