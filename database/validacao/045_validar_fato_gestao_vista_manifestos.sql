-- ============================================================================
-- Validacao estrutural da fato materializada de Manifestos
-- ============================================================================

SET NOCOUNT ON;

DECLARE @falhas TABLE (
    tipo NVARCHAR(50) NOT NULL,
    nome NVARCHAR(255) NOT NULL,
    detalhe NVARCHAR(500) NULL
);

IF OBJECT_ID(N'dbo.fato_gestao_vista_manifestos', N'U') IS NULL
BEGIN
    INSERT INTO @falhas VALUES (N'TABELA', N'dbo.fato_gestao_vista_manifestos', N'Tabela fato ausente');
END
ELSE
BEGIN
    DECLARE @colunas TABLE (nome SYSNAME NOT NULL);
    INSERT INTO @colunas (nome) VALUES
        (N'sequence_code'),
        (N'numero_manifesto'),
        (N'identificador_unico'),
        (N'data_criacao'),
        (N'data_criacao_date'),
        (N'filial_key'),
        (N'status_key'),
        (N'tipo_motorista'),
        (N'placa_veiculo'),
        (N'tipo_veiculo'),
        (N'custo_total'),
        (N'receita_total'),
        (N'capacidade_lotacao_kg'),
        (N'peso_taxado'),
        (N'hash_linha'),
        (N'excluido_na_origem');

    INSERT INTO @falhas (tipo, nome, detalhe)
    SELECT N'COLUNA', N'dbo.fato_gestao_vista_manifestos.' + c.nome, N'Coluna essencial da fato ausente'
    FROM @colunas c
    WHERE COL_LENGTH(N'dbo.fato_gestao_vista_manifestos', c.nome) IS NULL;

    IF NOT EXISTS (
        SELECT 1
        FROM sys.key_constraints
        WHERE name = N'PK_fato_gv_manifestos'
          AND parent_object_id = OBJECT_ID(N'dbo.fato_gestao_vista_manifestos')
          AND type = N'PK'
    )
        INSERT INTO @falhas VALUES (N'PK', N'PK_fato_gv_manifestos', N'PK por sequence_code ausente');

    IF NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = N'IX_fato_manifestos_data_filial'
          AND object_id = OBJECT_ID(N'dbo.fato_gestao_vista_manifestos')
          AND filter_definition LIKE N'%excluido_na_origem%'
    )
        INSERT INTO @falhas VALUES (N'INDICE', N'IX_fato_manifestos_data_filial', N'Indice de periodo/filial ausente ou sem filtro de ativos');

    IF NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = N'IX_fato_manifestos_filtros'
          AND object_id = OBJECT_ID(N'dbo.fato_gestao_vista_manifestos')
          AND filter_definition LIKE N'%excluido_na_origem%'
    )
        INSERT INTO @falhas VALUES (N'INDICE', N'IX_fato_manifestos_filtros', N'Indice de filtros do Dashboard ausente ou sem filtro de ativos');

    IF EXISTS (
        SELECT 1
        FROM dbo.fato_gestao_vista_manifestos
        GROUP BY sequence_code
        HAVING COUNT_BIG(1) > 1
    )
        INSERT INTO @falhas VALUES (N'GRAO', N'dbo.fato_gestao_vista_manifestos.sequence_code', N'Ha mais de uma linha por sequence_code');
END;

IF OBJECT_ID(N'dbo.sp_carga_fato_gestao_vista_manifestos', N'P') IS NULL
    INSERT INTO @falhas VALUES (N'PROCEDURE', N'dbo.sp_carga_fato_gestao_vista_manifestos', N'Procedure de carga da fato ausente');

IF OBJECT_ID(N'dbo.vw_fato_manifestos_dash', N'V') IS NULL
    INSERT INTO @falhas VALUES (N'VIEW', N'dbo.vw_fato_manifestos_dash', N'View leve de consumo do Dashboard ausente');

IF OBJECT_ID(N'dbo.vw_manifestos_powerbi', N'V') IS NULL
    INSERT INTO @falhas VALUES (N'VIEW', N'dbo.vw_manifestos_powerbi', N'View de compatibilidade de manifestos ausente');

IF OBJECT_ID(N'dbo.vw_fato_manifestos_dash', N'V') IS NOT NULL
   AND OBJECT_DEFINITION(OBJECT_ID(N'dbo.vw_fato_manifestos_dash')) NOT LIKE N'%fato_gestao_vista_manifestos%'
    INSERT INTO @falhas VALUES (N'VIEW_DEFINICAO', N'dbo.vw_fato_manifestos_dash', N'View deve ler da fato materializada');

IF OBJECT_ID(N'dbo.vw_fato_manifestos_dash', N'V') IS NOT NULL
   AND OBJECT_DEFINITION(OBJECT_ID(N'dbo.vw_fato_manifestos_dash')) LIKE N'%FROM dbo.manifestos%'
    INSERT INTO @falhas VALUES (N'VIEW_DEFINICAO', N'dbo.vw_fato_manifestos_dash', N'View leve nao deve consultar dbo.manifestos diretamente');

IF OBJECT_ID(N'dbo.vw_manifestos_powerbi', N'V') IS NOT NULL
   AND OBJECT_DEFINITION(OBJECT_ID(N'dbo.vw_manifestos_powerbi')) LIKE N'%OPENJSON%'
    INSERT INTO @falhas VALUES (N'VIEW_DEFINICAO', N'dbo.vw_manifestos_powerbi', N'View de compatibilidade nao deve executar OPENJSON sob demanda');

SELECT tipo, nome, detalhe
FROM @falhas
ORDER BY tipo, nome;

IF EXISTS (SELECT 1 FROM @falhas)
BEGIN
    THROW 51077, 'Fato de Manifestos invalida. Verifique database/validacao/045_validar_fato_gestao_vista_manifestos.sql.', 1;
END;

PRINT 'Fato de Manifestos validada com sucesso.';
GO
