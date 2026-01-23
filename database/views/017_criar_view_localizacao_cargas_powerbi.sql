-- ============================================
-- Script de criação da view 'vw_localizacao_cargas_powerbi'
-- Execute este script UMA VEZ antes de colocar o sistema em produção
-- ============================================

CREATE OR ALTER VIEW dbo.vw_localizacao_cargas_powerbi AS
SELECT

    CAST(service_at AS TIME(0)) AS [Hora (Solicitacao)],
    sequence_number AS [N° Minuta], -- CHECK
    REPLACE(type, 'Freight::', '') AS [Tipo], -- CHECK
    service_at AS [Data do frete], -- CHECK
    invoices_volumes AS [Volumes], -- CHECK
    taxed_weight AS [Peso Taxado], -- CHECK
    invoices_value AS [Valor NF], -- CHECK
    total_value AS [Valor Frete], -- CHECK
    service_type AS [Tipo Serviço], -- CHECK
    branch_nickname AS [Filial Emissora], -- CHECK
    predicted_delivery_at AS [Previsão Entrega/Previsão de entrega], -- CHECK
    destination_location_name AS [Região Destino], -- CHECK
    destination_branch_nickname AS [Filial Destino], -- CHECK
    classification AS [Classificação], -- CHECK
    CASE status
        WHEN 'pending' THEN 'Pendente'
        WHEN 'delivering' THEN 'Em entrega'
        WHEN 'in_warehouse' THEN 'Em armazém'
        WHEN 'in_transfer' THEN 'Em transferência'
        WHEN 'manifested' THEN 'Manifestado'
        WHEN 'finished' THEN 'Finalizado'
        ELSE status
    END AS [Status Carga], -- CHECK
    status_branch_nickname AS [Filial Atual], -- n encontrado no excel
    origin_location_name AS [Região Origem], -- CHECK
    origin_branch_nickname AS [Filial Origem], -- CHECK
    fit_fln_cln_nickname AS [Localização Atual],
    metadata AS [Metadata], -- Dados brutos do JSON...
    data_extracao AS [Data de extracao] -- Dados da extracao...
FROM dbo.localizacao_cargas;
GO

PRINT 'View vw_localizacao_cargas_powerbi criada/atualizada com sucesso!';
GO
