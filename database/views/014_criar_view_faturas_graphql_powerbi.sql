-- ============================================
-- Script de criação da view 'vw_faturas_graphql_powerbi'
-- Execute este script UMA VEZ antes de colocar o sistema em produção
-- ============================================

CREATE OR ALTER VIEW dbo.vw_faturas_graphql_powerbi AS
SELECT
    id                 AS [ID],
    document           AS [Fatura/N° Documento],
    issue_date         AS [Emissão],
    due_date           AS [Vencimento],
    original_due_date  AS [Vencimento Original],
    value              AS [Valor],
    paid_value         AS [Valor Pago],
    value_to_pay       AS [Valor a Pagar],
    discount_value     AS [Valor Desconto],
    interest_value     AS [Valor Juros],
    CASE 
        WHEN paid = 1 THEN N'Pago'
        WHEN paid = 0 THEN N'Não Pago'
        ELSE N'Indefinido'
    END AS [Pago],

    CASE LOWER(status)
        WHEN 'paid' THEN N'Pago'
        WHEN 'pending' THEN N'Pendente'
        ELSE CONCAT(N'Não mapeado: ', ISNULL(status, 'NULL'))
    END AS [Status],


    CASE type
        WHEN 'Accounting::Credit::CustomerBilling' THEN N'Fatura de cliente'
        ELSE CONCAT(N'Não mapeado: ', type)
    END AS [Tipo],

    comments           AS [Observações],
    sequence_code      AS [Código Sequencial],
    competence_month   AS [Mês Competência],
    competence_year    AS [Ano Competência],
    created_at         AS [Data Criação],
    updated_at         AS [Data Atualização],
    corporation_id     AS [Filial/ID],
    corporation_name   AS [Filial/Nome],
    corporation_cnpj   AS [Filial/CNPJ],
    nfse_numero        AS [NFS-e/Número],
    carteira_banco     AS [Banco/Carteira],
    instrucao_boleto   AS [Banco/Instrução Boleto],
    banco_nome         AS [Banco/Nome],

    CASE metodo_pagamento
        WHEN 'credit_in_account' THEN N'Crédito em conta'
        ELSE CONCAT(N'Não mapeado: ', metodo_pagamento)
    END AS [Método Pagamento],

    metadata           AS [Metadata],
    data_extracao      AS [Data de extracao]
FROM dbo.faturas_graphql;
GO

PRINT 'View vw_faturas_graphql_powerbi criada/atualizada com sucesso!';
GO
