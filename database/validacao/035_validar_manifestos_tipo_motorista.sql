SELECT
    c.name AS coluna,
    t.name AS tipo_sql
FROM sys.views v
INNER JOIN sys.columns c ON c.object_id = v.object_id
INNER JOIN sys.types t ON t.user_type_id = c.user_type_id
WHERE v.object_id = OBJECT_ID(N'dbo.vw_manifestos_powerbi')
  AND c.name IN (N'Tipo Motorista', N'Proprietário/Documento')
ORDER BY c.name;
GO

SELECT TOP (20)
    [Tipo Motorista],
    COUNT_BIG(1) AS total
FROM dbo.vw_manifestos_powerbi
GROUP BY [Tipo Motorista]
ORDER BY total DESC, [Tipo Motorista];
GO
