-- ============================================
-- Script de criação da view 'vw_sinistros_powerbi'
-- Execute este script UMA VEZ antes de colocar o sistema em produção
-- ============================================

CREATE OR ALTER VIEW dbo.vw_sinistros_powerbi AS
SELECT
    identificador_unico AS [Identificador Único],
    sequence_code AS [Nº do Sinistro],
    opening_at_date AS [Data abertura],
    occurrence_at_date AS [Data ocorrência],
    occurrence_at_time AS [Hora ocorrência],
    expected_solution_date AS [Previsão de solução],
    insurance_claim_location AS [Local do sinistro],
    informed_by AS [Informado por],
    finished_at_date AS [Data finalização],
    finished_at_time AS [Hora finalização],
    invoices_count AS [Quantidade de documentos],
    corporation_sequence_number AS [Minuta],
    insurance_occurrence_number AS [Nota Fiscal/Número],
    invoices_volumes AS [Volumes],
    invoices_weight AS [Peso],
    invoices_value AS [Valor NF],
    payer_nickname AS [Pagador do frete/Nome fantasia],
    customer_debits_subtotal AS [valor a pagar ao cliente],
    customer_credit_entries_subtotal AS [Valor nota de crédito ao cliente],
    responsible_credits_subtotal AS [Valor a receber do responsável],
    responsible_debit_entries_subtotal AS [Valor nota de débito do responsável],
    insurer_credits_subtotal AS [Valor a receber da seguradora],
    insurance_claim_total AS [Resultado final],
    branch_nickname AS [Pessoa/Nome fantasia],
    event_name AS [Ocorrência/Ocorrência],
    user_name AS [Usuário/Nome],
    vehicle_plate AS [Veículo/Placa],
    occurrence_description AS [Ocorrência/Descrição],
    occurrence_code AS [Ocorrência/Código],
    treatment_at AS [Tratativa/Data tratativa],
    CASE dealing_type
        WHEN 'send_to_damages_dealing' THEN 'enviado para tratativa de avarias'
        WHEN 'send_to_incidents_dealing' THEN 'enviado para tratativa de incidentes'
        ELSE dealing_type
    END AS [Tratativa/Tipo tratativa],
    CASE solution_type
        WHEN 'redelivery_without_freight_charge' THEN 'redespacho sem cobrança de frete'
        ELSE solution_type
    END AS [Tratativa/Solução],
    metadata AS [Metadata],
    data_extracao AS [Data de extracao]
FROM dbo.sinistros;
GO

PRINT 'View vw_sinistros_powerbi criada/atualizada com sucesso!';
GO
