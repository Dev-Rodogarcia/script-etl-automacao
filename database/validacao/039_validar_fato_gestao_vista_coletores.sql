-- ============================================================================
-- Validacao estrutural da fato materializada de Utilizacao de Coletores
-- ============================================================================

SET NOCOUNT ON;

DECLARE @falhas TABLE (
    tipo NVARCHAR(50) NOT NULL,
    nome NVARCHAR(255) NOT NULL,
    detalhe NVARCHAR(500) NULL
);

IF NOT EXISTS (SELECT 1 FROM sys.partition_functions WHERE name = N'PF_fato_gv_data_referencia_mes')
    INSERT INTO @falhas VALUES (N'PARTITION_FUNCTION', N'PF_fato_gv_data_referencia_mes', N'Partition Function mensal da fato ausente');

IF NOT EXISTS (SELECT 1 FROM sys.partition_schemes WHERE name = N'PS_fato_gv_data_referencia_mes')
    INSERT INTO @falhas VALUES (N'PARTITION_SCHEME', N'PS_fato_gv_data_referencia_mes', N'Partition Scheme mensal da fato ausente');

IF OBJECT_ID(N'dbo.fato_gestao_vista_coletores', N'U') IS NULL
BEGIN
    INSERT INTO @falhas VALUES (N'TABELA', N'dbo.fato_gestao_vista_coletores', N'Tabela fato ausente');
END
ELSE
BEGIN
    DECLARE @colunas TABLE (nome SYSNAME NOT NULL);
    INSERT INTO @colunas (nome) VALUES
        (N'data_referencia'),
        (N'data_referencia_yyyymm'),
        (N'filial'),
        (N'filial_key'),
        (N'classificacao'),
        (N'manifestos_bipados'),
        (N'manifestos_emitidos'),
        (N'manifestos_descarregamento'),
        (N'total_manifestos'),
        (N'manifestos_incompletos'),
        (N'pct_utilizacao'),
        (N'is_filial_operacional'),
        (N'is_linha_valida_indicador'),
        (N'excluido_na_origem'),
        (N'hash_linha');

    INSERT INTO @falhas (tipo, nome, detalhe)
    SELECT N'COLUNA', N'dbo.fato_gestao_vista_coletores.' + c.nome, N'Coluna essencial da fato ausente'
    FROM @colunas c
    WHERE COL_LENGTH(N'dbo.fato_gestao_vista_coletores', c.nome) IS NULL;

    IF NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = N'CCI_fato_gv_coletores'
          AND object_id = OBJECT_ID(N'dbo.fato_gestao_vista_coletores')
          AND type_desc = N'CLUSTERED COLUMNSTORE'
    )
        INSERT INTO @falhas VALUES (N'INDICE', N'CCI_fato_gv_coletores', N'Clustered Columnstore Index ausente');

    IF NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = N'UX_fato_gv_coletores_data_filial_classif'
          AND object_id = OBJECT_ID(N'dbo.fato_gestao_vista_coletores')
          AND is_unique = 1
    )
        INSERT INTO @falhas VALUES (N'INDICE', N'UX_fato_gv_coletores_data_filial_classif', N'Indice unico da chave de MERGE ausente');

    IF NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = N'IX_fato_gv_coletores_merge'
          AND object_id = OBJECT_ID(N'dbo.fato_gestao_vista_coletores')
    )
        INSERT INTO @falhas VALUES (N'INDICE', N'IX_fato_gv_coletores_merge', N'Indice auxiliar de MERGE ausente');

    IF NOT EXISTS (
        SELECT 1
        FROM sys.indexes i
        JOIN sys.index_columns k1
          ON k1.object_id = i.object_id
         AND k1.index_id = i.index_id
         AND k1.key_ordinal = 1
        JOIN sys.columns c1
          ON c1.object_id = k1.object_id
         AND c1.column_id = k1.column_id
        JOIN sys.index_columns k2
          ON k2.object_id = i.object_id
         AND k2.index_id = i.index_id
         AND k2.key_ordinal = 2
        JOIN sys.columns c2
          ON c2.object_id = k2.object_id
         AND c2.column_id = k2.column_id
        WHERE i.name = N'IX_fato_gv_coletores_periodo_filial'
          AND i.object_id = OBJECT_ID(N'dbo.fato_gestao_vista_coletores')
          AND c1.name = N'data_referencia'
          AND k1.is_descending_key = 1
          AND c2.name = N'filial_key'
          AND k2.is_descending_key = 0
          AND i.filter_definition LIKE N'%is_linha_valida_indicador%'
          AND i.filter_definition LIKE N'%excluido_na_origem%'
          AND (
              SELECT COUNT(1)
              FROM sys.index_columns kc
              WHERE kc.object_id = i.object_id
                AND kc.index_id = i.index_id
                AND kc.key_ordinal > 0
          ) = 2
          AND (
              SELECT COUNT(1)
              FROM sys.index_columns ic
              JOIN sys.columns c
                ON c.object_id = ic.object_id
               AND c.column_id = ic.column_id
              WHERE ic.object_id = i.object_id
                AND ic.index_id = i.index_id
                AND ic.is_included_column = 1
                AND c.name IN (
                    N'manifestos_bipados',
                    N'manifestos_emitidos',
                    N'manifestos_descarregamento',
                    N'total_manifestos',
                    N'manifestos_incompletos',
                    N'is_filial_operacional'
                )
          ) = 6
          AND (
              SELECT COUNT(1)
              FROM sys.index_columns ic
              WHERE ic.object_id = i.object_id
                AND ic.index_id = i.index_id
                AND ic.is_included_column = 1
          ) = 6
    )
        INSERT INTO @falhas VALUES (N'INDICE', N'IX_fato_gv_coletores_periodo_filial', N'Indice de Ranking ausente ou com chave/INCLUDE/filtro fora do contrato');
END;

IF OBJECT_ID(N'dbo.sp_carga_fato_gestao_vista_coletores', N'P') IS NULL
    INSERT INTO @falhas VALUES (N'PROCEDURE', N'dbo.sp_carga_fato_gestao_vista_coletores', N'Procedure de carga da fato ausente');

SELECT tipo, nome, detalhe
FROM @falhas
ORDER BY tipo, nome;

IF EXISTS (SELECT 1 FROM @falhas)
BEGIN
    THROW 51039, 'Fato de Utilizacao de Coletores invalida. Verifique database/validacao/039_validar_fato_gestao_vista_coletores.sql.', 1;
END;

PRINT 'Fato de Utilizacao de Coletores validada com sucesso.';
GO
