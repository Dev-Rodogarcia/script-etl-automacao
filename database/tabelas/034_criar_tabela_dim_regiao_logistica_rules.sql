-- ============================================================================
-- Dimensao de regras para resolucao de regiao logistica de coletas
-- ============================================================================

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

IF OBJECT_ID(N'dbo.dim_regiao_logistica_rules', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.dim_regiao_logistica_rules (
        id INT IDENTITY(1,1) NOT NULL,
        cep_inicio VARCHAR(8) NULL,
        cep_fim VARCHAR(8) NULL,
        cidade NVARCHAR(255) NULL,
        uf VARCHAR(2) NULL,
        regiao_logistica NVARCHAR(100) NOT NULL,
        CONSTRAINT PK_dim_regiao_logistica_rules PRIMARY KEY (id),
        CONSTRAINT CK_dim_regiao_logistica_rules_regiao
            CHECK (LTRIM(RTRIM(regiao_logistica)) <> N''),
        CONSTRAINT CK_dim_regiao_logistica_rules_cep_par
            CHECK ((cep_inicio IS NULL AND cep_fim IS NULL) OR (cep_inicio IS NOT NULL AND cep_fim IS NOT NULL)),
        CONSTRAINT CK_dim_regiao_logistica_rules_cidade_uf_par
            CHECK ((cidade IS NULL AND uf IS NULL) OR (cidade IS NOT NULL AND uf IS NOT NULL))
    );

    PRINT 'Tabela dbo.dim_regiao_logistica_rules criada.';
END
ELSE
BEGIN
    PRINT 'Tabela dbo.dim_regiao_logistica_rules ja existe.';
END;
GO
