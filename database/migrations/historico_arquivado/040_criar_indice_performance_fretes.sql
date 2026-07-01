PRINT 'Migration 040: criar indice de performance por minuta em fretes';
GO

SET NOCOUNT ON;
SET XACT_ABORT ON;
GO

DECLARE @MigrationId NVARCHAR(255) = N'040_criar_indice_performance_fretes';

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
    PRINT 'Migracao 040_criar_indice_performance_fretes ja aplicada. Nenhuma acao necessaria.';
    RETURN;
END;

IF OBJECT_ID(N'dbo.fretes', N'U') IS NULL
    THROW 51070, 'Tabela dbo.fretes nao encontrada. Execute os scripts-base antes da migration 040.', 1;

DECLARE @ColunasObrigatorias TABLE (nome SYSNAME NOT NULL);
INSERT INTO @ColunasObrigatorias (nome) VALUES
    (N'corporation_sequence_number'),
    (N'data_previsao_entrega'),
    (N'finished_at'),
    (N'fit_dpn_performance_finished_at'),
    (N'pagador_nome'),
    (N'filial_nome'),
    (N'filial_nome_key'),
    (N'destino_cidade'),
    (N'destino_uf'),
    (N'status'),
    (N'taxed_weight'),
    (N'valor_notas'),
    (N'data_extracao'),
    (N'excluido_na_origem');

IF EXISTS (
    SELECT 1
    FROM @ColunasObrigatorias c
    WHERE COL_LENGTH(N'dbo.fretes', c.nome) IS NULL
)
    THROW 51071, 'dbo.fretes nao possui todas as colunas exigidas pelo indice de Performance.', 1;

BEGIN TRY
BEGIN TRANSACTION;

IF EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_fretes_performance_minuta_cobertura'
      AND object_id = OBJECT_ID(N'dbo.fretes')
)
BEGIN
    DROP INDEX IX_fretes_performance_minuta_cobertura ON dbo.fretes;
    PRINT 'Indice IX_fretes_performance_minuta_cobertura removido para recriacao controlada.';
END;

CREATE NONCLUSTERED INDEX IX_fretes_performance_minuta_cobertura
    ON dbo.fretes(corporation_sequence_number)
    INCLUDE (
        data_previsao_entrega,
        finished_at,
        fit_dpn_performance_finished_at,
        pagador_nome,
        filial_nome,
        filial_nome_key,
        destino_cidade,
        destino_uf,
        status,
        taxed_weight,
        valor_notas,
        data_extracao,
        excluido_na_origem
    )
    WITH (DATA_COMPRESSION = PAGE);

INSERT INTO dbo.schema_migrations (migration_id, notes)
VALUES (
    @MigrationId,
    N'Cria indice por corporation_sequence_number cobrindo filtros e ROW_NUMBER do Dashboard de Performance sobre vw_fretes_powerbi.'
);

COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0
        ROLLBACK TRANSACTION;
    THROW;
END CATCH;

PRINT 'Migration 040_criar_indice_performance_fretes concluida com sucesso.';
GO
