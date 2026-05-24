IF OBJECT_ID(N'dbo.manifestos_frota_propria_cnpjs', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.manifestos_frota_propria_cnpjs (
        cnpj NVARCHAR(14) NOT NULL,
        descricao NVARCHAR(255) NULL,
        ativo BIT NOT NULL CONSTRAINT DF_manifestos_frota_propria_cnpjs_ativo DEFAULT 1,
        criado_em DATETIME2(0) NOT NULL CONSTRAINT DF_manifestos_frota_propria_cnpjs_criado_em DEFAULT SYSUTCDATETIME(),
        atualizado_em DATETIME2(0) NOT NULL CONSTRAINT DF_manifestos_frota_propria_cnpjs_atualizado_em DEFAULT SYSUTCDATETIME(),
        CONSTRAINT PK_manifestos_frota_propria_cnpjs PRIMARY KEY (cnpj)
    );
END;
GO

PRINT 'Tabela manifestos_frota_propria_cnpjs criada/validada com sucesso.';
GO
