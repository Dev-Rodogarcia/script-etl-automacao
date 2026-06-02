-- ============================================================================
-- Validacao estrutural da fato materializada de Gestao a Vista
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

IF OBJECT_ID(N'dbo.fato_gestao_vista_fretes', N'U') IS NULL
BEGIN
    INSERT INTO @falhas VALUES (N'TABELA', N'dbo.fato_gestao_vista_fretes', N'Tabela fato ausente');
END
ELSE
BEGIN
    DECLARE @colunas TABLE (nome SYSNAME NOT NULL);
    INSERT INTO @colunas (nome) VALUES
        (N'indicador_codigo'),
        (N'data_referencia'),
        (N'numero_minuta'),
        (N'filial_performance_key'),
        (N'filial_emissora_key'),
        (N'performance_diferenca_dias'),
        (N'performance_status_codigo'),
        (N'is_no_prazo'),
        (N'is_cubado'),
        (N'is_linha_valida_indicador'),
        (N'excluido_na_origem'),
        (N'hash_linha');

    INSERT INTO @falhas (tipo, nome, detalhe)
    SELECT N'COLUNA', N'dbo.fato_gestao_vista_fretes.' + c.nome, N'Coluna essencial da fato ausente'
    FROM @colunas c
    WHERE COL_LENGTH(N'dbo.fato_gestao_vista_fretes', c.nome) IS NULL;

    IF NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = N'CCI_fato_gv_fretes'
          AND object_id = OBJECT_ID(N'dbo.fato_gestao_vista_fretes')
          AND type_desc = N'CLUSTERED COLUMNSTORE'
    )
        INSERT INTO @falhas VALUES (N'INDICE', N'CCI_fato_gv_fretes', N'Clustered Columnstore Index ausente');

    IF NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = N'UX_fato_gv_fretes_indicador_minuta'
          AND object_id = OBJECT_ID(N'dbo.fato_gestao_vista_fretes')
          AND is_unique = 1
    )
        INSERT INTO @falhas VALUES (N'INDICE', N'UX_fato_gv_fretes_indicador_minuta', N'Indice unico da chave de MERGE ausente');

    IF NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = N'IX_fato_gv_fretes_pe_periodo_filial'
          AND object_id = OBJECT_ID(N'dbo.fato_gestao_vista_fretes')
          AND filter_definition LIKE N'%indicador_codigo%'
          AND filter_definition LIKE N'%PE%'
          AND filter_definition LIKE N'%excluido_na_origem%'
    )
        INSERT INTO @falhas VALUES (N'INDICE', N'IX_fato_gv_fretes_pe_periodo_filial', N'Indice filtrado de Performance ausente ou sem filtro esperado');

    IF NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = N'IX_fato_gv_fretes_cb_periodo_filial'
          AND object_id = OBJECT_ID(N'dbo.fato_gestao_vista_fretes')
          AND filter_definition LIKE N'%indicador_codigo%'
          AND filter_definition LIKE N'%CB%'
          AND filter_definition LIKE N'%excluido_na_origem%'
    )
        INSERT INTO @falhas VALUES (N'INDICE', N'IX_fato_gv_fretes_cb_periodo_filial', N'Indice filtrado de Cubagem ausente ou sem filtro esperado');
END;

IF OBJECT_ID(N'dbo.sp_carga_fato_gestao_vista_fretes', N'P') IS NULL
    INSERT INTO @falhas VALUES (N'PROCEDURE', N'dbo.sp_carga_fato_gestao_vista_fretes', N'Procedure de carga da fato ausente');

SELECT tipo, nome, detalhe
FROM @falhas
ORDER BY tipo, nome;

IF EXISTS (SELECT 1 FROM @falhas)
BEGIN
    THROW 51038, 'Fato de Gestao a Vista invalida. Verifique database/validacao/038_validar_fato_gestao_vista_fretes.sql.', 1;
END;

PRINT 'Fato de Gestao a Vista validada com sucesso.';
GO
