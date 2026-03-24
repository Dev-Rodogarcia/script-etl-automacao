CREATE OR ALTER VIEW dbo.vw_bi_monitoramento AS
SELECT
    id AS [Id],
    start_time AS [Inicio],
    end_time AS [Fim],
    duration_seconds AS [Duracao (s)],
    CAST(start_time AS DATE) AS [Data],
    status AS [Status],
    total_records AS [Total Registros],
    error_category AS [Categoria Erro],
    error_message AS [Mensagem Erro]
FROM dbo.sys_execution_history;
GO
