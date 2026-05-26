-- Valida que o KPI Volumes publicado para Faturamento usa a fonte real de Localizador de Cargas.
-- Seguro em banco vazio: a comparacao fica 0 x 0 e passa.

SET NOCOUNT ON;

IF OBJECT_ID(N'dbo.fretes', N'U') IS NULL
   OR OBJECT_ID(N'dbo.localizacao_cargas', N'U') IS NULL
   OR OBJECT_ID(N'dbo.vw_fretes_powerbi', N'V') IS NULL
BEGIN
    PRINT 'Validacao de volumes ignorada: tabelas/views necessarias ausentes.';
    RETURN;
END;

DECLARE @volumesEsperados BIGINT;
DECLARE @volumesPublicados BIGINT;
DECLARE @volumesNulos BIGINT;

SELECT @volumesEsperados = SUM(COALESCE(lc.invoices_volumes, f.invoices_total_volumes, 0))
FROM dbo.fretes AS f
LEFT JOIN dbo.localizacao_cargas AS lc
    ON lc.sequence_number = f.corporation_sequence_number;

SELECT @volumesPublicados = SUM(COALESCE(TRY_CONVERT(BIGINT, [Volumes]), 0))
FROM dbo.vw_fretes_powerbi;

SELECT @volumesNulos = COUNT_BIG(1)
FROM dbo.vw_fretes_powerbi
WHERE [Volumes] IS NULL;

IF COALESCE(@volumesNulos, 0) > 0
BEGIN
    DECLARE @mensagemNulos NVARCHAR(500) = CONCAT(
        N'Volumes nulos em dbo.vw_fretes_powerbi. linhas=',
        CONVERT(NVARCHAR(30), @volumesNulos)
    );
    THROW 51036, @mensagemNulos, 1;
END;

IF COALESCE(@volumesPublicados, 0) <> COALESCE(@volumesEsperados, 0)
BEGIN
    DECLARE @mensagem NVARCHAR(500) = CONCAT(
        N'Volumes divergentes em dbo.vw_fretes_powerbi. esperado=',
        COALESCE(CONVERT(NVARCHAR(30), @volumesEsperados), N'NULL'),
        N' publicado=',
        COALESCE(CONVERT(NVARCHAR(30), @volumesPublicados), N'NULL')
    );
    THROW 51036, @mensagem, 1;
END;

PRINT 'Volumes de Faturamento validados com sucesso.';
