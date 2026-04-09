PRINT 'Validacao 033: fretes orfaos sem faturas_graphql correspondente';
GO

SELECT
    COUNT(*) AS fretes_orfaos,
    COUNT(DISTINCT f.accounting_credit_id) AS accounting_credit_ids_orfaos
FROM dbo.fretes f
WHERE f.accounting_credit_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM dbo.faturas_graphql fg
      WHERE fg.id = f.accounting_credit_id
  );
GO

SELECT TOP (20)
    f.id,
    f.accounting_credit_id,
    f.reference_number,
    f.service_date,
    f.data_extracao
FROM dbo.fretes f
WHERE f.accounting_credit_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM dbo.faturas_graphql fg
      WHERE fg.id = f.accounting_credit_id
  )
ORDER BY f.data_extracao DESC, f.accounting_credit_id;
GO
