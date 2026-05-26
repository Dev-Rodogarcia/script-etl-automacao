-- ============================================================================
-- Localizacao de Cargas: contrato analitico, hash operacional e indices
-- ============================================================================

IF COL_LENGTH('dbo.localizacao_cargas', 'taxed_weight_decimal') IS NULL
BEGIN
    ALTER TABLE dbo.localizacao_cargas ADD taxed_weight_decimal DECIMAL(18, 3) NULL;
END
GO

IF COL_LENGTH('dbo.localizacao_cargas', 'invoices_value_decimal') IS NULL
BEGIN
    ALTER TABLE dbo.localizacao_cargas ADD invoices_value_decimal DECIMAL(18, 2) NULL;
END
GO

IF COL_LENGTH('dbo.localizacao_cargas', 'status_normalized') IS NULL
BEGIN
    ALTER TABLE dbo.localizacao_cargas ADD status_normalized NVARCHAR(50) NULL;
END
GO

IF COL_LENGTH('dbo.localizacao_cargas', 'localizacao_hash') IS NULL
BEGIN
    ALTER TABLE dbo.localizacao_cargas ADD localizacao_hash CHAR(64) NULL;
END
GO

UPDATE dbo.localizacao_cargas
   SET taxed_weight_decimal = TRY_CONVERT(DECIMAL(18, 3), REPLACE(NULLIF(LTRIM(RTRIM(taxed_weight)), ''), ',', '')),
       invoices_value_decimal = TRY_CONVERT(DECIMAL(18, 2), REPLACE(NULLIF(LTRIM(RTRIM(invoices_value)), ''), ',', '')),
       status_normalized = COALESCE(NULLIF(LOWER(LTRIM(RTRIM(status))), ''), N'sem_status')
 WHERE taxed_weight_decimal IS NULL
    OR invoices_value_decimal IS NULL
    OR status_normalized IS NULL;
GO

UPDATE dbo.localizacao_cargas
   SET localizacao_hash = LOWER(CONVERT(VARCHAR(64), HASHBYTES('SHA2_256', CONCAT_WS(N'|',
       COALESCE(CONVERT(NVARCHAR(30), sequence_number), N'__NULL__'),
       COALESCE(type, N'__NULL__'),
       COALESCE(CONVERT(NVARCHAR(40), service_at, 127), N'__NULL__'),
       COALESCE(CONVERT(NVARCHAR(20), invoices_volumes), N'__NULL__'),
       COALESCE(CONVERT(NVARCHAR(40), taxed_weight_decimal), N'__NULL__'),
       COALESCE(CONVERT(NVARCHAR(40), invoices_value_decimal), N'__NULL__'),
       COALESCE(CONVERT(NVARCHAR(40), total_value), N'__NULL__'),
       COALESCE(service_type, N'__NULL__'),
       COALESCE(branch_nickname, N'__NULL__'),
       COALESCE(CONVERT(NVARCHAR(40), predicted_delivery_at, 127), N'__NULL__'),
       COALESCE(destination_location_name, N'__NULL__'),
       COALESCE(destination_branch_nickname, N'__NULL__'),
       COALESCE(classification, N'__NULL__'),
       COALESCE(status, N'__NULL__'),
       COALESCE(status_normalized, N'__NULL__'),
       COALESCE(status_branch_nickname, N'__NULL__'),
       COALESCE(origin_location_name, N'__NULL__'),
       COALESCE(origin_branch_nickname, N'__NULL__'),
       COALESCE(fit_fln_cln_nickname, N'__NULL__')
   )), 2))
 WHERE localizacao_hash IS NULL;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.objects
    WHERE object_id = OBJECT_ID(N'dbo.localizacao_cargas_regiao_destino_alias')
      AND type = N'U'
)
BEGIN
    CREATE TABLE dbo.localizacao_cargas_regiao_destino_alias (
        nome_responsavel NVARCHAR(255) NOT NULL,
        sigla NVARCHAR(32) NOT NULL,
        ativo BIT NOT NULL CONSTRAINT DF_localizacao_regiao_alias_ativo DEFAULT 1,
        criado_em DATETIME2 NOT NULL CONSTRAINT DF_localizacao_regiao_alias_criado DEFAULT SYSUTCDATETIME(),
        atualizado_em DATETIME2 NOT NULL CONSTRAINT DF_localizacao_regiao_alias_atualizado DEFAULT SYSUTCDATETIME(),
        CONSTRAINT PK_localizacao_regiao_alias PRIMARY KEY (nome_responsavel)
    );
END
GO

MERGE dbo.localizacao_cargas_regiao_destino_alias AS target
USING (VALUES
    (N'AGU - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA', N'AGU'),
    (N'CAS - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA', N'CAS'),
    (N'CPQ - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA', N'CPQ'),
    (N'CWB - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA', N'CWB'),
    (N'NHB - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA', N'NHB'),
    (N'REC - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA', N'REC'),
    (N'RJR - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA', N'RJR'),
    (N'SPO - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA', N'SPO')
) AS source (nome_responsavel, sigla)
ON target.nome_responsavel = source.nome_responsavel
WHEN MATCHED THEN
    UPDATE SET sigla = source.sigla, ativo = 1, atualizado_em = SYSUTCDATETIME()
WHEN NOT MATCHED THEN
    INSERT (nome_responsavel, sigla)
    VALUES (source.nome_responsavel, source.sigla);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_localizacao_tracking_dashboard' AND object_id = OBJECT_ID('dbo.localizacao_cargas'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_localizacao_tracking_dashboard
    ON dbo.localizacao_cargas(status_branch_nickname, service_at DESC, status_normalized, destination_branch_nickname)
    INCLUDE (sequence_number, localizacao_hash, predicted_delivery_at, total_value, taxed_weight_decimal, invoices_value_decimal, invoices_volumes, data_extracao);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_localizacao_hash_upsert' AND object_id = OBJECT_ID('dbo.localizacao_cargas'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_localizacao_hash_upsert
    ON dbo.localizacao_cargas(sequence_number, localizacao_hash)
    INCLUDE (status_normalized, status, status_branch_nickname, fit_fln_cln_nickname, destination_branch_nickname, predicted_delivery_at, service_at);
END
GO

CREATE OR ALTER VIEW dbo.vw_localizacao_cargas_powerbi AS
SELECT
    CAST(lc.service_at AS TIME(0)) AS [Hora (Solicitacao)],
    lc.sequence_number AS [N° Minuta],
    REPLACE(lc.type, 'Freight::', '') AS [Tipo],
    lc.service_at AS [Data do frete],
    lc.invoices_volumes AS [Volumes],
    lc.taxed_weight AS [Peso Taxado],
    lc.taxed_weight_decimal AS [Peso Taxado Decimal],
    lc.invoices_value AS [Valor NF],
    lc.invoices_value_decimal AS [Valor NF Decimal],
    lc.total_value AS [Valor Frete],
    lc.service_type AS [Tipo Serviço],
    lc.branch_nickname AS [Filial Emissora],
    lc.predicted_delivery_at AS [Previsão Entrega/Previsão de entrega],
    lc.destination_location_name AS [Região Destino],
    lc.destination_branch_nickname AS [Filial Destino],
    lc.destination_branch_nickname AS [Responsável pela Região de Destino],
    COALESCE(regiao_alias.sigla, N'SEM_MAP') AS [Sigla Responsável Região Destino],
    lc.classification AS [Classificação],
    CASE lc.status
        WHEN 'pending' THEN 'Pendente'
        WHEN 'delivering' THEN 'Em entrega'
        WHEN 'in_warehouse' THEN 'Em armazém'
        WHEN 'in_transfer' THEN 'Em transferência'
        WHEN 'manifested' THEN 'Manifestado'
        WHEN 'finished' THEN 'Finalizado'
        WHEN 'delivered' THEN 'Entregue'
        WHEN 'canceled' THEN 'Cancelado'
        WHEN 'cancelled' THEN 'Cancelado'
        ELSE lc.status
    END AS [Status Carga],
    COALESCE(lc.status_normalized, NULLIF(LOWER(LTRIM(RTRIM(lc.status))), ''), N'sem_status') AS [Status Normalizado],
    CASE WHEN COALESCE(lc.status_normalized, LOWER(LTRIM(RTRIM(lc.status)))) IN (N'finished', N'delivered', N'canceled', N'cancelled', N'finalizado', N'entregue', N'cancelado') THEN 1 ELSE 0 END AS [Status Terminal],
    CASE WHEN COALESCE(lc.status_normalized, LOWER(LTRIM(RTRIM(lc.status)))) IN (N'canceled', N'cancelled', N'cancelado') THEN 1 ELSE 0 END AS [Cancelado Flag],
    lc.status_branch_nickname AS [Filial Atual],
    lc.origin_location_name AS [Região Origem],
    lc.origin_branch_nickname AS [Filial Origem],
    lc.fit_fln_cln_nickname AS [Localização Atual],
    lc.localizacao_hash AS [Hash Localização],
    lc.metadata AS [Metadata],
    lc.data_extracao AS [Data de extracao]
FROM dbo.localizacao_cargas lc
LEFT JOIN dbo.localizacao_cargas_regiao_destino_alias regiao_alias
       ON regiao_alias.nome_responsavel = lc.destination_branch_nickname
      AND regiao_alias.ativo = 1;
GO

IF OBJECT_ID(N'dbo.schema_migrations', N'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM dbo.schema_migrations WHERE migration_id = N'017_localizacao_cargas_dashboard_operacional')
BEGIN
    INSERT INTO dbo.schema_migrations (migration_id, notes)
    VALUES (
        N'017_localizacao_cargas_dashboard_operacional',
        N'Materializa campos e indices operacionais de localizacao de cargas para dashboards.'
    );
END;
GO
