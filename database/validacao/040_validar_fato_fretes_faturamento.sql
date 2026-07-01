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

IF OBJECT_ID(N'dbo.dim_calendario', N'U') IS NULL
BEGIN
    INSERT INTO @falhas VALUES (N'TABELA', N'dbo.dim_calendario', N'Dimensao calendario ausente');
END
ELSE
BEGIN
    DECLARE @colunasCalendario TABLE (nome SYSNAME NOT NULL);
    INSERT INTO @colunasCalendario (nome) VALUES
        (N'data'),
        (N'is_dia_util'),
        (N'is_feriado_nacional'),
        (N'is_ponto_facultativo'),
        (N'data_referencia_faturamento'),
        (N'data_referencia_faturamento_key');

    INSERT INTO @falhas (tipo, nome, detalhe)
    SELECT N'COLUNA', N'dbo.dim_calendario.' + c.nome, N'Coluna essencial da dimensao calendario ausente'
    FROM @colunasCalendario c
    WHERE COL_LENGTH(N'dbo.dim_calendario', c.nome) IS NULL;

    IF NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = N'IX_dim_calendario_referencia_faturamento'
          AND object_id = OBJECT_ID(N'dbo.dim_calendario')
    )
        INSERT INTO @falhas VALUES (N'INDICE', N'IX_dim_calendario_referencia_faturamento', N'Indice de referencia de faturamento do calendario ausente');
END;

IF OBJECT_ID(N'dbo.regras_atribuicao_filial', N'U') IS NULL
BEGIN
    INSERT INTO @falhas VALUES (N'TABELA', N'dbo.regras_atribuicao_filial', N'Tabela parametrica de atribuicao de filial ausente');
END
ELSE
BEGIN
    DECLARE @colunasRegras TABLE (nome SYSNAME NOT NULL);
    INSERT INTO @colunasRegras (nome) VALUES
        (N'pagador_documento_key'),
        (N'filial_destino_nome'),
        (N'filial_destino_key'),
        (N'ativo');

    INSERT INTO @falhas (tipo, nome, detalhe)
    SELECT N'COLUNA', N'dbo.regras_atribuicao_filial.' + c.nome, N'Coluna essencial da regra de atribuicao ausente'
    FROM @colunasRegras c
    WHERE COL_LENGTH(N'dbo.regras_atribuicao_filial', c.nome) IS NULL;

    IF NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = N'UX_regras_atribuicao_filial_pagador_ativo'
          AND object_id = OBJECT_ID(N'dbo.regras_atribuicao_filial')
          AND is_unique = 1
    )
        INSERT INTO @falhas VALUES (N'INDICE', N'UX_regras_atribuicao_filial_pagador_ativo', N'Indice unico de regra ativa por pagador ausente');
END;

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
        (N'data_referencia_faturamento_real'),
        (N'data_referencia_faturamento_real_date'),
        (N'data_referencia_faturamento_real_yyyymm'),
        (N'is_data_faturamento_retroagida'),
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

    IF OBJECT_ID(N'dbo.dim_calendario', N'U') IS NOT NULL
       AND COL_LENGTH(N'dbo.fato_fretes_faturamento', N'data_referencia_faturamento_real_date') IS NOT NULL
    BEGIN
        IF EXISTS (
            SELECT 1
            FROM dbo.fato_fretes_faturamento ff
            INNER JOIN dbo.dim_calendario cal
                ON cal.data = ff.data_referencia_faturamento_real_date
            WHERE ff.excluido_na_origem = 0
              AND cal.data_referencia_faturamento <> ff.data_referencia_faturamento_date
        )
            INSERT INTO @falhas VALUES (N'REGRA', N'RETROACAO_FATURAMENTO', N'Data de referencia da fato diverge da dimensao calendario');
    END;
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
