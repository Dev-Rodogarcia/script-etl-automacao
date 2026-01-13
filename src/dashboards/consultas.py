CONTAS_PAGAR_TOP = """
SELECT *
FROM dbo.vw_contas_a_pagar_powerbi
ORDER BY 1 DESC
OFFSET 0 ROWS FETCH NEXT :n ROWS ONLY
"""

FATURAS_CLIENTE_TOP = """
SELECT *
FROM dbo.vw_faturas_por_cliente_powerbi
ORDER BY [Parcelas / Vencimento] DESC
OFFSET 0 ROWS FETCH NEXT :n ROWS ONLY
"""

FATURAS_CLIENTE_BASE = """
SELECT *
FROM dbo.faturas_por_cliente
ORDER BY data_vencimento_fatura DESC
OFFSET 0 ROWS FETCH NEXT :n ROWS ONLY
"""

MANIFESTOS_RESUMO = """
SELECT
  sequence_code,
  branch_nickname,
  invoices_value,
  total_cost,
  paying_total,
  created_at
FROM manifestos
WHERE invoices_value IS NOT NULL
ORDER BY created_at DESC
OFFSET 0 ROWS FETCH NEXT :n ROWS ONLY
"""

# Consultas genéricas para demais views (top N, ordena pela primeira coluna)
COLETAS_TOP = """
SELECT *
FROM dbo.vw_coletas_powerbi
ORDER BY 1 DESC
OFFSET 0 ROWS FETCH NEXT :n ROWS ONLY
"""

FRETES_TOP = """
SELECT *
FROM dbo.vw_fretes_powerbi
ORDER BY 1 DESC
OFFSET 0 ROWS FETCH NEXT :n ROWS ONLY
"""

FRETES_BASE = """
SELECT *
FROM dbo.fretes
ORDER BY servico_em DESC
OFFSET 0 ROWS FETCH NEXT :n ROWS ONLY
"""

COTACOES_TOP = """
SELECT *
FROM dbo.vw_cotacoes_powerbi
ORDER BY 1 DESC
OFFSET 0 ROWS FETCH NEXT :n ROWS ONLY
"""

LOCALIZACAO_TOP = """
SELECT *
FROM dbo.vw_localizacao_cargas_powerbi
ORDER BY 1 DESC
OFFSET 0 ROWS FETCH NEXT :n ROWS ONLY
"""

LOCALIZACAO_BASE = """
SELECT *
FROM dbo.localizacao_cargas
ORDER BY service_at DESC
OFFSET 0 ROWS FETCH NEXT :n ROWS ONLY
"""

MANIFESTOS_VIEW_TOP = """
SELECT *
FROM dbo.vw_manifestos_powerbi
ORDER BY 1 DESC
OFFSET 0 ROWS FETCH NEXT :n ROWS ONLY
"""
