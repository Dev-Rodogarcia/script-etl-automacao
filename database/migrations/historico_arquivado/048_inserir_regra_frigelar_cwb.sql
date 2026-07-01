PRINT 'Migration 048: inserir regra FRIGELAR para CWB';
GO

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET XACT_ABORT ON;
GO

DECLARE @MigrationId NVARCHAR(255) = N'048_inserir_regra_frigelar_cwb';

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
    PRINT 'Migracao 048_inserir_regra_frigelar_cwb ja aplicada. Nenhuma acao necessaria.';
    RETURN;
END;

IF OBJECT_ID(N'dbo.regras_atribuicao_filial', N'U') IS NULL
    THROW 51140, 'Tabela dbo.regras_atribuicao_filial nao encontrada. Execute a migration 047 antes da 048.', 1;

BEGIN TRY
BEGIN TRANSACTION;

MERGE dbo.regras_atribuicao_filial AS target
USING (VALUES (
    N'92660406007040',
    N'CWB - RODOGARCIA',
    N'cwb - rodogarcia',
    CAST(1 AS BIT),
    N'Migração financeira solicitada pela diretoria (emissão original na NHB)'
)) AS source (pagador_documento_key, filial_destino_nome, filial_destino_key, ativo, motivo)
ON target.pagador_documento_key = source.pagador_documento_key
WHEN MATCHED THEN
    UPDATE SET
        filial_destino_nome = source.filial_destino_nome,
        filial_destino_key = source.filial_destino_key,
        ativo = source.ativo,
        motivo = source.motivo
WHEN NOT MATCHED THEN
    INSERT (pagador_documento_key, filial_destino_nome, filial_destino_key, ativo, motivo)
    VALUES (source.pagador_documento_key, source.filial_destino_nome, source.filial_destino_key, source.ativo, source.motivo);

INSERT INTO dbo.schema_migrations (migration_id, notes)
VALUES (
    @MigrationId,
    N'Insere regra FRIGELAR 92.660.406/0070-40 para atribuicao financeira em CWB.'
);

COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0
        ROLLBACK TRANSACTION;
    THROW;
END CATCH;

PRINT 'Migration 048_inserir_regra_frigelar_cwb concluida com sucesso.';
GO
