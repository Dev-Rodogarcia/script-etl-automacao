PRINT 'Migration 046: reclassificar tipo de contrato de manifestos';
GO

SET NOCOUNT ON;
SET XACT_ABORT ON;
GO

DECLARE @MigrationId NVARCHAR(255) = N'046_reclassificar_tipo_contrato_manifestos';

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
    PRINT 'Migracao 046_reclassificar_tipo_contrato_manifestos ja aplicada. Nenhuma acao necessaria.';
    RETURN;
END;

IF OBJECT_ID(N'dbo.fato_gestao_vista_manifestos', N'U') IS NULL
    THROW 51131, 'Tabela dbo.fato_gestao_vista_manifestos nao encontrada. Execute a migration 042 antes da 046.', 1;

IF COL_LENGTH(N'dbo.fato_gestao_vista_manifestos', N'proprietario_nome') IS NULL
    THROW 51132, 'Coluna dbo.fato_gestao_vista_manifestos.proprietario_nome nao encontrada.', 1;

IF COL_LENGTH(N'dbo.fato_gestao_vista_manifestos', N'tipo_contrato_veiculo_key') IS NULL
    THROW 51133, 'Coluna dbo.fato_gestao_vista_manifestos.tipo_contrato_veiculo_key nao encontrada. Execute a migration 043 antes da 046.', 1;

IF COL_LENGTH(N'dbo.fato_gestao_vista_manifestos', N'tipo_contrato_motorista_key') IS NULL
    THROW 51134, 'Coluna dbo.fato_gestao_vista_manifestos.tipo_contrato_motorista_key nao encontrada. Execute a migration 043 antes da 046.', 1;

IF COL_LENGTH(N'dbo.fato_gestao_vista_manifestos', N'tipo_contrato_key') IS NULL
    THROW 51135, 'Coluna dbo.fato_gestao_vista_manifestos.tipo_contrato_key nao encontrada. Execute a migration 043 antes da 046.', 1;

BEGIN TRY
BEGIN TRANSACTION;

DECLARE @LinhasAfetadas INT = 0;

;WITH candidatos AS (
    SELECT
        f.sequence_code,
        CASE
            WHEN f.tipo_contrato_veiculo = N'Agregado'
             AND (
                    f.proprietario_nome COLLATE Latin1_General_CI_AI LIKE N'%DALGA%'
                 OR f.proprietario_nome COLLATE Latin1_General_CI_AI LIKE N'%LM TRANSPORTES%'
                 )
                THEN N'Frota + PX'
            WHEN f.tipo_contrato_veiculo = N'Agregado'
             AND (
                    f.tipo_contrato_motorista IS NULL
                 OR LTRIM(RTRIM(f.tipo_contrato_motorista)) = N''
                 )
                THEN N'Terceiro'
            WHEN f.tipo_contrato_veiculo = N'Motorista'
             AND (
                    f.tipo_contrato_motorista IS NULL
                 OR LTRIM(RTRIM(f.tipo_contrato_motorista)) = N''
                 )
                THEN N'Frota + PX'
            ELSE NULL
        END AS novo_tipo_contrato
    FROM dbo.fato_gestao_vista_manifestos AS f
    WHERE f.excluido_na_origem = 0
      AND (
            f.tipo_contrato_veiculo_key = N'agregado'
         OR (
                f.tipo_contrato_veiculo_key = N'motorista'
            AND f.tipo_contrato_motorista_key IS NULL
            )
      )
)
UPDATE f
   SET tipo_contrato = c.novo_tipo_contrato,
       tipo_contrato_key = CASE c.novo_tipo_contrato
           WHEN N'Frota + PX' THEN N'frota + px'
           WHEN N'Terceiro' THEN N'terceiro'
           ELSE NULL
       END,
       hash_linha = NULL,
       snapshot_em = SYSUTCDATETIME()
  FROM dbo.fato_gestao_vista_manifestos AS f
  JOIN candidatos AS c
    ON c.sequence_code = f.sequence_code
 WHERE c.novo_tipo_contrato IS NOT NULL
   AND (
        f.tipo_contrato IS NULL
     OR f.tipo_contrato <> c.novo_tipo_contrato
     OR f.tipo_contrato_key IS NULL
     OR f.tipo_contrato_key <> CASE c.novo_tipo_contrato
            WHEN N'Frota + PX' THEN N'frota + px'
            WHEN N'Terceiro' THEN N'terceiro'
            ELSE NULL
        END
   );

SET @LinhasAfetadas = @@ROWCOUNT;

INSERT INTO dbo.schema_migrations (migration_id, notes)
VALUES (
    @MigrationId,
    N'Reclassifica Tipo de contrato para excecoes DALGA/LM e fallbacks de motorista vazio.'
);

COMMIT TRANSACTION;

PRINT CONCAT('Migration 046: ', @LinhasAfetadas, ' linhas reclassificadas.');
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0
        ROLLBACK TRANSACTION;
    THROW;
END CATCH;

PRINT 'Migration 046_reclassificar_tipo_contrato_manifestos concluida com sucesso.';
GO
