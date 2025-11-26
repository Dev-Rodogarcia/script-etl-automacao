WITH logs AS (
  SELECT le.entidade,
         MAX(le.timestamp_fim) AS ultima_execucao,
         MAX(CASE WHEN le.timestamp_fim = (SELECT MAX(le2.timestamp_fim) FROM dbo.log_extracoes le2 WHERE le2.entidade = le.entidade) THEN le.status_final END) AS status_final,
         MAX(CASE WHEN le.timestamp_fim = (SELECT MAX(le2.timestamp_fim) FROM dbo.log_extracoes le2 WHERE le2.entidade = le.entidade) THEN le.registros_extraidos END) AS registros_extraidos,
         MAX(CASE WHEN le.timestamp_fim = (SELECT MAX(le2.timestamp_fim) FROM dbo.log_extracoes le2 WHERE le2.entidade = le.entidade) THEN le.paginas_processadas END) AS paginas_processadas
  FROM dbo.log_extracoes le
  GROUP BY le.entidade
),
auditoria AS (
  SELECT entidade, COUNT(*) AS campos_mapeados
  FROM dbo.sys_auditoria_temp
  WHERE data_auditoria >= DATEADD(hour, -24, GETDATE())
  GROUP BY entidade
)
SELECT
  'cotacoes' AS entidade,
  CASE WHEN COL_LENGTH('dbo.cotacoes','data_extracao') IS NOT NULL
       THEN (SELECT COUNT(*) FROM dbo.cotacoes WHERE data_extracao >= DATEADD(hour,-24,GETDATE()))
       ELSE (SELECT COUNT(*) FROM dbo.cotacoes WHERE requested_at >= DATEADD(hour,-24,GETDATE()))
  END AS registros_24h_banco,
  ISNULL((SELECT a.campos_mapeados FROM auditoria a WHERE a.entidade = 'cotacoes'),0) AS campos_mapeados_auditoria,
  l.ultima_execucao, l.status_final AS ultima_execucao_status, l.registros_extraidos AS ultima_execucao_registros, l.paginas_processadas AS ultima_execucao_paginas,
  CASE WHEN (
    CASE WHEN COL_LENGTH('dbo.cotacoes','data_extracao') IS NOT NULL
         THEN (SELECT COUNT(*) FROM dbo.cotacoes WHERE data_extracao >= DATEADD(hour,-24,GETDATE()))
         ELSE (SELECT COUNT(*) FROM dbo.cotacoes WHERE requested_at >= DATEADD(hour,-24,GETDATE()))
    END
  ) > 0 THEN 1 ELSE 0 END AS ok_banco,
  CASE WHEN ISNULL((SELECT a.campos_mapeados FROM auditoria a WHERE a.entidade = 'cotacoes'),0) > 0 THEN 1 ELSE 0 END AS ok_auditoria_campos,
  CASE WHEN l.status_final = 'COMPLETO' THEN 1 ELSE 0 END AS ok_log
FROM logs l WHERE l.entidade = 'cotacoes'
UNION ALL
SELECT
  'coletas',
  CASE WHEN COL_LENGTH('dbo.coletas','data_extracao') IS NOT NULL
       THEN (SELECT COUNT(*) FROM dbo.coletas WHERE data_extracao >= DATEADD(hour,-24,GETDATE()))
       ELSE (SELECT COUNT(*) FROM dbo.coletas WHERE request_date >= DATEADD(hour,-24,GETDATE()))
  END,
  ISNULL((SELECT a.campos_mapeados FROM auditoria a WHERE a.entidade = 'coletas'),0),
  l.ultima_execucao, l.status_final, l.registros_extraidos, l.paginas_processadas,
  CASE WHEN (
    CASE WHEN COL_LENGTH('dbo.coletas','data_extracao') IS NOT NULL
         THEN (SELECT COUNT(*) FROM dbo.coletas WHERE data_extracao >= DATEADD(hour,-24,GETDATE()))
         ELSE (SELECT COUNT(*) FROM dbo.coletas WHERE request_date >= DATEADD(hour,-24,GETDATE()))
    END
  ) > 0 THEN 1 ELSE 0 END,
  CASE WHEN ISNULL((SELECT a.campos_mapeados FROM auditoria a WHERE a.entidade = 'coletas'),0) > 0 THEN 1 ELSE 0 END,
  CASE WHEN l.status_final = 'COMPLETO' THEN 1 ELSE 0 END
FROM logs l WHERE l.entidade = 'coletas'
UNION ALL
SELECT
  'contas_a_pagar',
  CASE WHEN COL_LENGTH('dbo.contas_a_pagar','data_extracao') IS NOT NULL
       THEN (SELECT COUNT(*) FROM dbo.contas_a_pagar WHERE data_extracao >= DATEADD(hour,-24,GETDATE()))
       ELSE (SELECT COUNT(*) FROM dbo.contas_a_pagar WHERE issue_date >= DATEADD(hour,-24,GETDATE()))
  END,
  ISNULL((SELECT a.campos_mapeados FROM auditoria a WHERE a.entidade = 'contas_a_pagar'),0),
  l.ultima_execucao, l.status_final, l.registros_extraidos, l.paginas_processadas,
  CASE WHEN (
    CASE WHEN COL_LENGTH('dbo.contas_a_pagar','data_extracao') IS NOT NULL
         THEN (SELECT COUNT(*) FROM dbo.contas_a_pagar WHERE data_extracao >= DATEADD(hour,-24,GETDATE()))
         ELSE (SELECT COUNT(*) FROM dbo.contas_a_pagar WHERE issue_date >= DATEADD(hour,-24,GETDATE()))
    END
  ) > 0 THEN 1 ELSE 0 END,
  CASE WHEN ISNULL((SELECT a.campos_mapeados FROM auditoria a WHERE a.entidade = 'contas_a_pagar'),0) > 0 THEN 1 ELSE 0 END,
  CASE WHEN l.status_final = 'COMPLETO' THEN 1 ELSE 0 END
FROM logs l WHERE l.entidade = 'contas_a_pagar'
UNION ALL
SELECT
  'faturas_por_cliente',
  CASE WHEN COL_LENGTH('dbo.faturas_por_cliente','data_extracao') IS NOT NULL
       THEN (SELECT COUNT(*) FROM dbo.faturas_por_cliente WHERE data_extracao >= DATEADD(hour,-24,GETDATE()))
       ELSE (SELECT COUNT(*) FROM dbo.faturas_por_cliente WHERE data_emissao_fatura >= DATEADD(hour,-24,GETDATE()))
  END,
  ISNULL((SELECT a.campos_mapeados FROM auditoria a WHERE a.entidade = 'faturas_por_cliente'),0),
  l.ultima_execucao, l.status_final, l.registros_extraidos, l.paginas_processadas,
  CASE WHEN (
    CASE WHEN COL_LENGTH('dbo.faturas_por_cliente','data_extracao') IS NOT NULL
         THEN (SELECT COUNT(*) FROM dbo.faturas_por_cliente WHERE data_extracao >= DATEADD(hour,-24,GETDATE()))
         ELSE (SELECT COUNT(*) FROM dbo.faturas_por_cliente WHERE data_emissao_fatura >= DATEADD(hour,-24,GETDATE()))
    END
  ) > 0 THEN 1 ELSE 0 END,
  CASE WHEN ISNULL((SELECT a.campos_mapeados FROM auditoria a WHERE a.entidade = 'faturas_por_cliente'),0) > 0 THEN 1 ELSE 0 END,
  CASE WHEN l.status_final = 'COMPLETO' THEN 1 ELSE 0 END
FROM logs l WHERE l.entidade = 'faturas_por_cliente'
UNION ALL
SELECT
  'fretes',
  CASE WHEN COL_LENGTH('dbo.fretes','data_extracao') IS NOT NULL
       THEN (SELECT COUNT(*) FROM dbo.fretes WHERE data_extracao >= DATEADD(hour,-24,GETDATE()))
       ELSE (SELECT COUNT(*) FROM dbo.fretes WHERE criado_em >= DATEADD(hour,-24,GETDATE()))
  END,
  ISNULL((SELECT a.campos_mapeados FROM auditoria a WHERE a.entidade = 'fretes'),0),
  l.ultima_execucao, l.status_final, l.registros_extraidos, l.paginas_processadas,
  CASE WHEN (
    CASE WHEN COL_LENGTH('dbo.fretes','data_extracao') IS NOT NULL
         THEN (SELECT COUNT(*) FROM dbo.fretes WHERE data_extracao >= DATEADD(hour,-24,GETDATE()))
         ELSE (SELECT COUNT(*) FROM dbo.fretes WHERE criado_em >= DATEADD(hour,-24,GETDATE()))
    END
  ) > 0 THEN 1 ELSE 0 END,
  CASE WHEN ISNULL((SELECT a.campos_mapeados FROM auditoria a WHERE a.entidade = 'fretes'),0) > 0 THEN 1 ELSE 0 END,
  CASE WHEN l.status_final = 'COMPLETO' THEN 1 ELSE 0 END
FROM logs l WHERE l.entidade = 'fretes'
UNION ALL
SELECT
  'manifestos',
  CASE WHEN COL_LENGTH('dbo.manifestos','data_extracao') IS NOT NULL
       THEN (SELECT COUNT(*) FROM dbo.manifestos WHERE data_extracao >= DATEADD(hour,-24,GETDATE()))
       ELSE (SELECT COUNT(*) FROM dbo.manifestos WHERE created_at >= DATEADD(hour,-24,GETDATE()))
  END,
  ISNULL((SELECT a.campos_mapeados FROM auditoria a WHERE a.entidade = 'manifestos'),0),
  l.ultima_execucao, l.status_final, l.registros_extraidos, l.paginas_processadas,
  CASE WHEN (
    CASE WHEN COL_LENGTH('dbo.manifestos','data_extracao') IS NOT NULL
         THEN (SELECT COUNT(*) FROM dbo.manifestos WHERE data_extracao >= DATEADD(hour,-24,GETDATE()))
         ELSE (SELECT COUNT(*) FROM dbo.manifestos WHERE created_at >= DATEADD(hour,-24,GETDATE()))
    END
  ) > 0 THEN 1 ELSE 0 END,
  CASE WHEN ISNULL((SELECT a.campos_mapeados FROM auditoria a WHERE a.entidade = 'manifestos'),0) > 0 THEN 1 ELSE 0 END,
  CASE WHEN l.status_final = 'COMPLETO' THEN 1 ELSE 0 END
FROM logs l WHERE l.entidade = 'manifestos'
UNION ALL
SELECT
  'localizacao_cargas',
  CASE WHEN COL_LENGTH('dbo.localizacao_cargas','data_extracao') IS NOT NULL
       THEN (SELECT COUNT(*) FROM dbo.localizacao_cargas WHERE data_extracao >= DATEADD(hour,-24,GETDATE()))
       ELSE (SELECT COUNT(*) FROM dbo.localizacao_cargas WHERE service_at >= DATEADD(hour,-24,GETDATE()))
  END,
  ISNULL((SELECT a.campos_mapeados FROM auditoria a WHERE a.entidade = 'localizacao_cargas'),0),
  l.ultima_execucao, l.status_final, l.registros_extraidos, l.paginas_processadas,
  CASE WHEN (
    CASE WHEN COL_LENGTH('dbo.localizacao_cargas','data_extracao') IS NOT NULL
         THEN (SELECT COUNT(*) FROM dbo.localizacao_cargas WHERE data_extracao >= DATEADD(hour,-24,GETDATE()))
         ELSE (SELECT COUNT(*) FROM dbo.localizacao_cargas WHERE service_at >= DATEADD(hour,-24,GETDATE()))
    END
  ) > 0 THEN 1 ELSE 0 END,
  CASE WHEN ISNULL((SELECT a.campos_mapeados FROM auditoria a WHERE a.entidade = 'localizacao_cargas'),0) > 0 THEN 1 ELSE 0 END,
  CASE WHEN l.status_final = 'COMPLETO' THEN 1 ELSE 0 END
FROM logs l WHERE l.entidade = 'localizacao_cargas'
ORDER BY entidade;