PRINT 'Migration 007: adicionar FK seletiva manifestos.pick_sequence_code -> coletas.sequence_code';
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'IX_manifestos_pick_sequence_code'
      AND object_id = OBJECT_ID('dbo.manifestos')
)
BEGIN
    CREATE INDEX IX_manifestos_pick_sequence_code ON dbo.manifestos(pick_sequence_code);
    PRINT 'Indice IX_manifestos_pick_sequence_code criado.';
END
ELSE
BEGIN
    PRINT 'Indice IX_manifestos_pick_sequence_code ja existe.';
END
GO

IF EXISTS (
    SELECT 1
    FROM sys.foreign_keys
    WHERE name = 'FK_manifestos_pick_sequence_code_coletas'
      AND parent_object_id = OBJECT_ID('dbo.manifestos')
)
BEGIN
    PRINT 'FK_manifestos_pick_sequence_code_coletas ja existe. Nada a fazer.';
END
ELSE IF EXISTS (
    SELECT 1
    FROM dbo.manifestos m
    WHERE m.pick_sequence_code IS NOT NULL
      AND NOT EXISTS (
          SELECT 1
          FROM dbo.coletas c
          WHERE c.sequence_code = m.pick_sequence_code
      )
)
BEGIN
    PRINT 'AVISO: FK seletiva nao criada porque ainda existem manifestos orfaos sem coleta correspondente.';
END
ELSE
BEGIN
    ALTER TABLE dbo.manifestos WITH CHECK
        ADD CONSTRAINT FK_manifestos_pick_sequence_code_coletas
            FOREIGN KEY (pick_sequence_code)
            REFERENCES dbo.coletas(sequence_code);

    ALTER TABLE dbo.manifestos CHECK CONSTRAINT FK_manifestos_pick_sequence_code_coletas;
    PRINT 'FK_manifestos_pick_sequence_code_coletas criada com sucesso.';
END
GO
