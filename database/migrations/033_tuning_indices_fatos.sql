PRINT 'Migration 033: tuning de indices B-Tree das fatos';
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

DECLARE @MigrationId NVARCHAR(255) = N'033_tuning_indices_fatos';

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
    PRINT 'Migracao 033_tuning_indices_fatos ja aplicada. Nenhuma acao necessaria.';
    RETURN;
END;

IF OBJECT_ID(N'dbo.fato_gestao_vista_faturas', N'U') IS NULL
    THROW 51056, 'Tabela dbo.fato_gestao_vista_faturas nao encontrada. Execute a migration 032 antes da 033.', 1;

IF OBJECT_ID(N'dbo.fato_gestao_vista_coletores', N'U') IS NULL
    THROW 51057, 'Tabela dbo.fato_gestao_vista_coletores nao encontrada. Execute a migration 030 antes da 033.', 1;

BEGIN TRY
BEGIN TRANSACTION;

IF EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_fato_gvf_aging'
      AND object_id = OBJECT_ID(N'dbo.fato_gestao_vista_faturas')
)
BEGIN
    DROP INDEX IX_fato_gvf_aging ON dbo.fato_gestao_vista_faturas;
    PRINT 'Indice IX_fato_gvf_aging removido para recriacao.';
END;

CREATE NONCLUSTERED INDEX IX_fato_gvf_aging
    ON dbo.fato_gestao_vista_faturas(data_emissao_cte DESC, unique_id DESC)
    WITH (DATA_COMPRESSION = PAGE)
    ON PS_fato_gvf_data_emissao_mes(data_emissao_fatura);
PRINT 'Indice IX_fato_gvf_aging recriado para Aging/Paginacao por data_emissao_cte.';

IF EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_fato_gv_coletores_periodo_filial'
      AND object_id = OBJECT_ID(N'dbo.fato_gestao_vista_coletores')
)
BEGIN
    DROP INDEX IX_fato_gv_coletores_periodo_filial ON dbo.fato_gestao_vista_coletores;
    PRINT 'Indice IX_fato_gv_coletores_periodo_filial removido para recriacao.';
END;

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
PRINT 'Indice IX_fato_gv_coletores_periodo_filial recriado para Ranking.';

INSERT INTO dbo.schema_migrations (migration_id, notes)
VALUES (
    @MigrationId,
    N'Tuning B-Tree: IX_fato_gvf_aging por data_emissao_cte e IX_fato_gv_coletores_periodo_filial cobrindo Ranking.'
);

COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0
        ROLLBACK TRANSACTION;
    THROW;
END CATCH;

PRINT 'Migration 033_tuning_indices_fatos concluida com sucesso.';
GO
