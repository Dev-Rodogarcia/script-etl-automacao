-- ============================================
-- Publica wrappers locais do Dashboard PROD com colunas explicitas.
--
-- Uso:
--   sqlcmd -S <server> -d master -i database\views\024_publicar_wrappers_dashboard_prod_explicitos.sql ^
--     -v DashboardDb="DASHBOARDS" EtlDb="ETL_SISTEMA"
--
-- Este script deve ser executado pelo owner/DBA do ETL, nunca pelo backend do Dashboard.
-- A view criada no banco do Dashboard fica imune a deslocamento de ordinal porque lista cada coluna.
-- ============================================

:setvar DashboardDb "DASHBOARDS"
:setvar EtlDb "ETL_SISTEMA"

SET NOCOUNT ON;

IF DB_ID(N'$(DashboardDb)') IS NULL
    THROW 51230, 'Database de destino do Dashboard nao encontrado.', 1;

IF DB_ID(N'$(EtlDb)') IS NULL
    THROW 51231, 'Database fonte do ETL nao encontrado.', 1;

USE [$(DashboardDb)];
GO

DECLARE @views TABLE (
    nome SYSNAME NOT NULL PRIMARY KEY
);

INSERT INTO @views (nome)
VALUES
    (N'vw_faturas_por_cliente_powerbi'),
    (N'vw_fretes_powerbi'),
    (N'vw_coletas_powerbi'),
    (N'vw_cotacoes_powerbi'),
    (N'vw_contas_a_pagar_powerbi'),
    (N'vw_localizacao_cargas_powerbi'),
    (N'vw_manifestos_powerbi'),
    (N'vw_fato_manifestos_dash'),
    (N'vw_bi_monitoramento'),
    (N'vw_inventario_powerbi'),
    (N'vw_sinistros_powerbi'),
    (N'vw_raster_sm_transit_time'),
    (N'vw_dim_filiais'),
    (N'vw_dim_clientes'),
    (N'vw_dim_veiculos'),
    (N'vw_dim_motoristas'),
    (N'vw_dim_planocontas'),
    (N'vw_dim_usuarios');

DECLARE @nome SYSNAME;
DECLARE @colunas NVARCHAR(MAX);
DECLARE @sql NVARCHAR(MAX);

DECLARE views_cursor CURSOR LOCAL FAST_FORWARD FOR
    SELECT nome
    FROM @views
    ORDER BY nome;

OPEN views_cursor;
FETCH NEXT FROM views_cursor INTO @nome;

WHILE @@FETCH_STATUS = 0
BEGIN
    SELECT @colunas = STRING_AGG(CONVERT(NVARCHAR(MAX), QUOTENAME(c.name)), N',' + CHAR(13) + CHAR(10) + N'    ')
        WITHIN GROUP (ORDER BY c.column_id)
    FROM [$(EtlDb)].sys.columns c
    INNER JOIN [$(EtlDb)].sys.objects o ON o.object_id = c.object_id
    INNER JOIN [$(EtlDb)].sys.schemas s ON s.schema_id = o.schema_id
    WHERE s.name = N'dbo'
      AND o.name = @nome
      AND o.type = N'V';

    IF @colunas IS NULL
    BEGIN
        DECLARE @erro NVARCHAR(4000) = N'View fonte ausente no ETL: dbo.' + @nome;
        THROW 51232, @erro, 1;
    END;

    SET @sql = N'CREATE OR ALTER VIEW dbo.' + QUOTENAME(@nome) + N' AS
SELECT
    ' + @colunas + N'
FROM [$(EtlDb)].dbo.' + QUOTENAME(@nome) + N';';

    EXEC sys.sp_executesql @sql;

    PRINT N'Wrapper PROD atualizado com colunas explicitas: dbo.' + @nome;

    SET @colunas = NULL;
    FETCH NEXT FROM views_cursor INTO @nome;
END;

CLOSE views_cursor;
DEALLOCATE views_cursor;

PRINT N'Wrappers explicitos do Dashboard PROD publicados com sucesso.';
GO
