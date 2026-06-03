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
        FROM sys.indexes
        WHERE name = N'IX_fato_gv_coletores_periodo_filial'
          AND object_id = OBJECT_ID(N'dbo.fato_gestao_vista_coletores')
          AND filter_definition LIKE N'%is_linha_valida_indicador%'
          AND filter_definition LIKE N'%excluido_na_origem%'
    )
        INSERT INTO @falhas VALUES (N'INDICE', N'IX_fato_gv_coletores_periodo_filial', N'Indice filtrado periodo/filial ausente ou sem filtro esperado');
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
