-- ============================================================================
-- Validacao estrutural da fato materializada de Faturas por Cliente
-- ============================================================================

SET NOCOUNT ON;

DECLARE @falhas TABLE (
    tipo NVARCHAR(50) NOT NULL,
    nome NVARCHAR(255) NOT NULL,
    detalhe NVARCHAR(500) NULL
);

IF NOT EXISTS (SELECT 1 FROM sys.partition_functions WHERE name = N'PF_fato_gvf_data_emissao_mes')
    INSERT INTO @falhas VALUES (N'PARTITION_FUNCTION', N'PF_fato_gvf_data_emissao_mes', N'Partition Function mensal da fato ausente');

IF NOT EXISTS (SELECT 1 FROM sys.partition_schemes WHERE name = N'PS_fato_gvf_data_emissao_mes')
    INSERT INTO @falhas VALUES (N'PARTITION_SCHEME', N'PS_fato_gvf_data_emissao_mes', N'Partition Scheme mensal da fato ausente');

IF OBJECT_ID(N'dbo.fato_gestao_vista_faturas', N'U') IS NULL
BEGIN
    INSERT INTO @falhas VALUES (N'TABELA', N'dbo.fato_gestao_vista_faturas', N'Tabela fato ausente');
END
ELSE
BEGIN
    DECLARE @colunas TABLE (nome SYSNAME NOT NULL);
    INSERT INTO @colunas (nome) VALUES
        (N'unique_id'),
        (N'chave_normalizacao'),
        (N'data_emissao_fatura'),
        (N'data_vencimento_fatura'),
        (N'data_baixa_fatura'),
        (N'valor_operacional'),
        (N'status_pagamento'),
        (N'documento_fatura'),
        (N'filial'),
        (N'pagador_nome'),
        (N'pagador_documento'),
        (N'cliente_cnpj'),
        (N'cliente_chave'),
        (N'data_emissao_cte'),
        (N'hash_linha'),
        (N'excluido_na_origem');

    INSERT INTO @falhas (tipo, nome, detalhe)
    SELECT N'COLUNA', N'dbo.fato_gestao_vista_faturas.' + c.nome, N'Coluna essencial da fato ausente'
    FROM @colunas c
    WHERE COL_LENGTH(N'dbo.fato_gestao_vista_faturas', c.nome) IS NULL;

    IF NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = N'CCI_fato_gvf'
          AND object_id = OBJECT_ID(N'dbo.fato_gestao_vista_faturas')
          AND type_desc = N'CLUSTERED COLUMNSTORE'
    )
        INSERT INTO @falhas VALUES (N'INDICE', N'CCI_fato_gvf', N'Clustered Columnstore Index ausente');

    IF NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = N'UX_fato_gvf_unique_id_data'
          AND object_id = OBJECT_ID(N'dbo.fato_gestao_vista_faturas')
          AND is_unique = 1
    )
        INSERT INTO @falhas VALUES (N'INDICE', N'UX_fato_gvf_unique_id_data', N'Indice unico do grao unique_id/data_emissao_fatura ausente');

    IF NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = N'IX_fato_gvf_merge'
          AND object_id = OBJECT_ID(N'dbo.fato_gestao_vista_faturas')
    )
        INSERT INTO @falhas VALUES (N'INDICE', N'IX_fato_gvf_merge', N'Indice de suporte ao MERGE ausente');

    IF NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = N'IX_fato_gvf_aging'
          AND object_id = OBJECT_ID(N'dbo.fato_gestao_vista_faturas')
          AND filter_definition LIKE N'%excluido_na_origem%'
    )
        INSERT INTO @falhas VALUES (N'INDICE', N'IX_fato_gvf_aging', N'Indice de Aging ausente ou sem filtro esperado');

    IF NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = N'IX_fato_gvf_paginacao_periodo'
          AND object_id = OBJECT_ID(N'dbo.fato_gestao_vista_faturas')
          AND filter_definition LIKE N'%excluido_na_origem%'
    )
        INSERT INTO @falhas VALUES (N'INDICE', N'IX_fato_gvf_paginacao_periodo', N'Indice de Tabela/Paginacao ausente ou sem filtro esperado');

    IF EXISTS (
        SELECT 1
        FROM dbo.fato_gestao_vista_faturas
        GROUP BY unique_id
        HAVING COUNT_BIG(1) > 1
    )
        INSERT INTO @falhas VALUES (N'GRAO', N'dbo.fato_gestao_vista_faturas.unique_id', N'Ha mais de uma linha por unique_id');

    IF EXISTS (
        SELECT 1
        FROM dbo.fato_gestao_vista_faturas
        WHERE valor_operacional IS NULL
    )
        INSERT INTO @falhas VALUES (N'DADO', N'dbo.fato_gestao_vista_faturas.valor_operacional', N'Valor operacional nao pode ser NULL');
END;

IF OBJECT_ID(N'dbo.sp_carga_fato_gestao_vista_faturas', N'P') IS NULL
    INSERT INTO @falhas VALUES (N'PROCEDURE', N'dbo.sp_carga_fato_gestao_vista_faturas', N'Procedure de carga da fato ausente');

SELECT tipo, nome, detalhe
FROM @falhas
ORDER BY tipo, nome;

IF EXISTS (SELECT 1 FROM @falhas)
BEGIN
    THROW 51055, 'Fato de Faturas por Cliente invalida. Verifique database/validacao/041_validar_fato_gestao_vista_faturas.sql.', 1;
END;

PRINT 'Fato de Faturas por Cliente validada com sucesso.';
GO
