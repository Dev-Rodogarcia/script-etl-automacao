PRINT 'Validacao 032: manifestos orfaos sem coleta correspondente';
GO

SELECT
    COUNT(*) AS manifestos_orfaos,
    COUNT(DISTINCT m.pick_sequence_code) AS pick_sequence_codes_orfaos
FROM dbo.manifestos m
WHERE m.pick_sequence_code IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM dbo.coletas c
      WHERE c.sequence_code = m.pick_sequence_code
  );
GO

SELECT TOP (20)
    m.sequence_code,
    m.pick_sequence_code,
    m.mdfe_number,
    m.data_extracao
FROM dbo.manifestos m
WHERE m.pick_sequence_code IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM dbo.coletas c
      WHERE c.sequence_code = m.pick_sequence_code
  )
ORDER BY m.data_extracao DESC, m.pick_sequence_code;
GO
