IF COL_LENGTH('dbo.fretes', 'finished_at') IS NULL
BEGIN
    ALTER TABLE dbo.fretes ADD finished_at DATETIMEOFFSET NULL;
END
GO

IF COL_LENGTH('dbo.fretes', 'fit_dpn_performance_finished_at') IS NULL
BEGIN
    ALTER TABLE dbo.fretes ADD fit_dpn_performance_finished_at DATETIMEOFFSET NULL;
END
GO

IF COL_LENGTH('dbo.fretes', 'corporation_sequence_number') IS NULL
BEGIN
    ALTER TABLE dbo.fretes ADD corporation_sequence_number BIGINT NULL;
END
GO
