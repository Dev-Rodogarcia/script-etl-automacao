-- ============================================================================
-- Indices operacionais da dimensao de regiao logistica de coletas
-- ============================================================================

IF OBJECT_ID(N'dbo.dim_regiao_logistica_rules', N'U') IS NULL
BEGIN
    PRINT 'Tabela dbo.dim_regiao_logistica_rules ausente. Pulando indices.';
END;
ELSE
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = N'IX_dim_regiao_logistica_rules_cep_range'
          AND object_id = OBJECT_ID(N'dbo.dim_regiao_logistica_rules')
    )
    BEGIN
        CREATE NONCLUSTERED INDEX IX_dim_regiao_logistica_rules_cep_range
            ON dbo.dim_regiao_logistica_rules(cep_inicio, cep_fim)
            INCLUDE (regiao_logistica)
            WHERE cep_inicio IS NOT NULL AND cep_fim IS NOT NULL;

        PRINT 'Indice IX_dim_regiao_logistica_rules_cep_range criado.';
    END
    ELSE
    BEGIN
        PRINT 'Indice IX_dim_regiao_logistica_rules_cep_range ja existe.';
    END;

    IF NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = N'IX_dim_regiao_logistica_rules_cidade_uf'
          AND object_id = OBJECT_ID(N'dbo.dim_regiao_logistica_rules')
    )
    BEGIN
        CREATE NONCLUSTERED INDEX IX_dim_regiao_logistica_rules_cidade_uf
            ON dbo.dim_regiao_logistica_rules(cidade, uf)
            INCLUDE (regiao_logistica)
            WHERE cidade IS NOT NULL AND uf IS NOT NULL;

        PRINT 'Indice IX_dim_regiao_logistica_rules_cidade_uf criado.';
    END
    ELSE
    BEGIN
        PRINT 'Indice IX_dim_regiao_logistica_rules_cidade_uf ja existe.';
    END;
END;
GO
