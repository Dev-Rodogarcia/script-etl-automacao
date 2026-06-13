-- ============================================================================
-- Indices da fato materializada de Manifestos para o Dashboard
-- ============================================================================

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;
GO

IF OBJECT_ID(N'dbo.fato_gestao_vista_manifestos', N'U') IS NULL
BEGIN
    PRINT 'Tabela dbo.fato_gestao_vista_manifestos ausente. Indices ignorados.';
    RETURN;
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_fato_manifestos_data_filial'
      AND object_id = OBJECT_ID(N'dbo.fato_gestao_vista_manifestos')
)
BEGIN
    CREATE NONCLUSTERED INDEX IX_fato_manifestos_data_filial
        ON dbo.fato_gestao_vista_manifestos(data_criacao, filial_key)
        INCLUDE (
            identificador_unico, status, status_key, classificacao,
            motorista, motorista_key, placa_veiculo, placa_veiculo_key,
            tipo_veiculo, tipo_motorista, tipo_carga_key, tipo_contrato_key,
            custo_total, valor_frete, combustivel, pedagio, saldo_pagar,
            km_total, receita_total, peso_taxado, total_m3, capacidade_lotacao_kg,
            itens_total, itens_finalizados, data_extracao
        )
        WHERE excluido_na_origem = 0
        WITH (DATA_COMPRESSION = PAGE);

    PRINT 'Indice IX_fato_manifestos_data_filial criado.';
END
ELSE
BEGIN
    PRINT 'Indice IX_fato_manifestos_data_filial ja existe.';
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_fato_manifestos_filtros'
      AND object_id = OBJECT_ID(N'dbo.fato_gestao_vista_manifestos')
)
BEGIN
    CREATE NONCLUSTERED INDEX IX_fato_manifestos_filtros
        ON dbo.fato_gestao_vista_manifestos(filial_key, status_key, data_criacao)
        INCLUDE (
            classificacao_bucket, motorista_key, placa_veiculo_key,
            tipo_carga_key, tipo_contrato_key, tipo_motorista_key, custo_total,
            receita_total, peso_taxado, capacidade_lotacao_kg, data_extracao
        )
        WHERE excluido_na_origem = 0
        WITH (DATA_COMPRESSION = PAGE);

    PRINT 'Indice IX_fato_manifestos_filtros criado.';
END
ELSE
BEGIN
    PRINT 'Indice IX_fato_manifestos_filtros ja existe.';
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_fato_manifestos_motorista'
      AND object_id = OBJECT_ID(N'dbo.fato_gestao_vista_manifestos')
)
BEGIN
    CREATE NONCLUSTERED INDEX IX_fato_manifestos_motorista
        ON dbo.fato_gestao_vista_manifestos(motorista_key, data_criacao)
        INCLUDE (motorista, filial_key, custo_total, km_total)
        WHERE excluido_na_origem = 0
          AND motorista_key IS NOT NULL
        WITH (DATA_COMPRESSION = PAGE);

    PRINT 'Indice IX_fato_manifestos_motorista criado.';
END
ELSE
BEGIN
    PRINT 'Indice IX_fato_manifestos_motorista ja existe.';
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_fato_manifestos_veiculo'
      AND object_id = OBJECT_ID(N'dbo.fato_gestao_vista_manifestos')
)
BEGIN
    CREATE NONCLUSTERED INDEX IX_fato_manifestos_veiculo
        ON dbo.fato_gestao_vista_manifestos(placa_veiculo_key, data_criacao)
        INCLUDE (placa_veiculo, filial_key, tipo_veiculo, custo_total)
        WHERE excluido_na_origem = 0
          AND placa_veiculo_key IS NOT NULL
        WITH (DATA_COMPRESSION = PAGE);

    PRINT 'Indice IX_fato_manifestos_veiculo criado.';
END
ELSE
BEGIN
    PRINT 'Indice IX_fato_manifestos_veiculo ja existe.';
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_fato_manifestos_merge_hash'
      AND object_id = OBJECT_ID(N'dbo.fato_gestao_vista_manifestos')
)
BEGIN
    CREATE NONCLUSTERED INDEX IX_fato_manifestos_merge_hash
        ON dbo.fato_gestao_vista_manifestos(sequence_code)
        INCLUDE (hash_linha, excluido_na_origem, data_criacao_date, snapshot_em)
        WITH (DATA_COMPRESSION = PAGE);

    PRINT 'Indice IX_fato_manifestos_merge_hash criado.';
END
ELSE
BEGIN
    PRINT 'Indice IX_fato_manifestos_merge_hash ja existe.';
END;
GO
