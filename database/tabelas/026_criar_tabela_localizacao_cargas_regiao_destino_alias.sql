-- ============================================================================
-- Alias governado para sigla do responsável pela região de destino
-- ============================================================================

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

    PRINT 'Tabela localizacao_cargas_regiao_destino_alias criada com sucesso!';
END
ELSE
BEGIN
    PRINT 'Tabela localizacao_cargas_regiao_destino_alias ja existe. Pulando criacao.';
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
