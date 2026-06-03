-- ============================================================================
-- Validacao estrutural e semantica da fato granular de Faturamento de Fretes
-- ============================================================================

SET NOCOUNT ON;

DECLARE @falhas TABLE (
    tipo NVARCHAR(50) NOT NULL,
    nome NVARCHAR(255) NOT NULL,
    detalhe NVARCHAR(500) NULL
);

IF NOT EXISTS (SELECT 1 FROM sys.partition_functions WHERE name = N'PF_fato_ff_data_referencia_mes')
    INSERT INTO @falhas VALUES (N'PARTITION_FUNCTION', N'PF_fato_ff_data_referencia_mes', N'Partition Function mensal da fato ausente');

IF NOT EXISTS (SELECT 1 FROM sys.partition_schemes WHERE name = N'PS_fato_ff_data_referencia_mes')
    INSERT INTO @falhas VALUES (N'PARTITION_SCHEME', N'PS_fato_ff_data_referencia_mes', N'Partition Scheme mensal da fato ausente');

IF OBJECT_ID(N'dbo.fato_fretes_faturamento', N'U') IS NULL
BEGIN
    INSERT INTO @falhas VALUES (N'TABELA', N'dbo.fato_fretes_faturamento', N'Tabela fato ausente');
END
ELSE
BEGIN
    DECLARE @colunas TABLE (nome SYSNAME NOT NULL);
    INSERT INTO @colunas (nome) VALUES
        (N'frete_id'),
        (N'numero_minuta'),
        (N'data_referencia_faturamento'),
        (N'data_referencia_faturamento_date'),
        (N'data_referencia_faturamento_yyyymm'),
        (N'filial_key'),
        (N'pagador_documento_key'),
        (N'chave_cte'),
        (N'status_cte_real'),
        (N'is_cte_cancelado'),
        (N'is_elegivel_faturamento'),
        (N'valor_frete'),
        (N'receita_bruta'),
        (N'hash_linha'),
        (N'excluido_na_origem');

    INSERT INTO @falhas (tipo, nome, detalhe)
    SELECT N'COLUNA', N'dbo.fato_fretes_faturamento.' + c.nome, N'Coluna essencial da fato ausente'
    FROM @colunas c
    WHERE COL_LENGTH(N'dbo.fato_fretes_faturamento', c.nome) IS NULL;

    IF NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = N'CCI_fato_ff'
          AND object_id = OBJECT_ID(N'dbo.fato_fretes_faturamento')
          AND type_desc = N'CLUSTERED COLUMNSTORE'
    )
        INSERT INTO @falhas VALUES (N'INDICE', N'CCI_fato_ff', N'Clustered Columnstore Index ausente');

    IF NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = N'UX_fato_ff_frete_data'
          AND object_id = OBJECT_ID(N'dbo.fato_fretes_faturamento')
          AND is_unique = 1
    )
        INSERT INTO @falhas VALUES (N'INDICE', N'UX_fato_ff_frete_data', N'Indice unico de grao frete/data ausente');

    IF NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = N'IX_fato_ff_paginacao_periodo'
          AND object_id = OBJECT_ID(N'dbo.fato_fretes_faturamento')
          AND filter_definition LIKE N'%excluido_na_origem%'
    )
        INSERT INTO @falhas VALUES (N'INDICE', N'IX_fato_ff_paginacao_periodo', N'Indice de paginacao por data/minuta ausente ou sem filtro esperado');

    IF NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = N'IX_fato_ff_periodo_faturamento'
          AND object_id = OBJECT_ID(N'dbo.fato_fretes_faturamento')
          AND filter_definition LIKE N'%excluido_na_origem%'
    )
        INSERT INTO @falhas VALUES (N'INDICE', N'IX_fato_ff_periodo_faturamento', N'Indice analitico de faturamento ausente ou sem filtro esperado');

    IF EXISTS (
        SELECT 1
        FROM dbo.fato_fretes_faturamento
        WHERE is_cte_cancelado = 1
          AND (
                is_elegivel_faturamento <> 0
             OR receita_bruta <> 0
             OR valor_frete <> 0
          )
    )
        INSERT INTO @falhas VALUES (N'REGRA', N'CTE_CANCELADO_SEM_RECEITA', N'CT-e cancelado gerando elegibilidade, receita ou valor de frete');

    IF EXISTS (
        SELECT 1
        FROM dbo.fato_fretes_faturamento
        WHERE is_elegivel_faturamento = 0
          AND (
                receita_bruta <> 0
             OR valor_frete <> 0
          )
    )
        INSERT INTO @falhas VALUES (N'REGRA', N'INELEGIVEL_SEM_RECEITA', N'Frete inelegivel com valores analiticos diferentes de zero');

    IF EXISTS (
        SELECT 1
        FROM dbo.fato_fretes_faturamento
        GROUP BY frete_id
        HAVING COUNT_BIG(1) > 1
    )
        INSERT INTO @falhas VALUES (N'GRAO', N'dbo.fato_fretes_faturamento.frete_id', N'Ha mais de uma linha por frete_id');
END;

IF OBJECT_ID(N'dbo.sp_carga_fato_fretes_faturamento', N'P') IS NULL
    INSERT INTO @falhas VALUES (N'PROCEDURE', N'dbo.sp_carga_fato_fretes_faturamento', N'Procedure de carga da fato ausente');

SELECT tipo, nome, detalhe
FROM @falhas
ORDER BY tipo, nome;

IF EXISTS (SELECT 1 FROM @falhas)
BEGIN
    THROW 51047, 'Fato de Faturamento de Fretes invalida. Verifique database/validacao/040_validar_fato_fretes_faturamento.sql.', 1;
END;

PRINT 'Fato de Faturamento de Fretes validada com sucesso.';
GO
