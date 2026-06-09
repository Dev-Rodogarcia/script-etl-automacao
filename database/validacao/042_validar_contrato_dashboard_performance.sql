SET NOCOUNT ON;
GO

IF OBJECT_ID(N'dbo.vw_fretes_powerbi', N'V') IS NULL
    THROW 53001, 'Contrato invalido: dbo.vw_fretes_powerbi ausente.', 1;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.vw_fretes_powerbi')
      AND name = N'Responsável Região Destino Key'
)
    THROW 53002, 'Contrato invalido: dbo.vw_fretes_powerbi sem Responsável Região Destino Key.', 1;
GO

PRINT 'Contrato do Dashboard Performance validado com sucesso.';
GO
