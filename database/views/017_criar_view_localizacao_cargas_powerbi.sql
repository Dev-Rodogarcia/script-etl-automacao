-- ============================================
-- Script de criação da view 'vw_localizacao_cargas_powerbi'
-- Execute este script UMA VEZ antes de colocar o sistema em produção
-- ============================================

CREATE OR ALTER VIEW dbo.vw_localizacao_cargas_powerbi AS
SELECT
    sequence_number AS [N° Minuta],
    REPLACE(type, 'Freight::', '') AS [Tipo],
    service_at AS [Data do frete],
    invoices_volumes AS [Volumes],
    taxed_weight AS [Peso Taxado],
    invoices_value AS [Valor NF],
    total_value AS [Valor Frete],
    service_type AS [Tipo Serviço],
    branch_nickname AS [Filial Emissora],
    predicted_delivery_at AS [Previsão Entrega/Previsão de entrega],
    destination_location_name AS [Região Destino],
    destination_branch_nickname AS [Filial Destino],
    classification AS [Classificação],
    CASE status
        WHEN 'pending' THEN 'Pendente'
        WHEN 'delivering' THEN 'Em entrega'
        WHEN 'in_warehouse' THEN 'Em armazém'
        WHEN 'in_transfer' THEN 'Em transferência'
        WHEN 'manifested' THEN 'Manifestado'
        WHEN 'finished' THEN 'Finalizado'
        ELSE status
    END AS [Status Carga],
    status_branch_nickname AS [Filial Atual],
    origin_location_name AS [Região Origem],
    origin_branch_nickname AS [Filial Origem],
    TRY_CONVERT(DECIMAL(10,6), JSON_VALUE(metadata, '$.latitude')) AS [Latitude],
    TRY_CONVERT(DECIMAL(10,6), JSON_VALUE(metadata, '$.longitude')) AS [Longitude],
    TRY_CONVERT(DECIMAL(10,2), JSON_VALUE(metadata, '$.speed')) AS [Velocidade],
    TRY_CONVERT(DECIMAL(10,2), JSON_VALUE(metadata, '$.altitude')) AS [Altitude],
    JSON_VALUE(metadata, '$.device_id') AS [Dispositivo ID],
    JSON_VALUE(metadata, '$.device_type') AS [Dispositivo Tipo],
    JSON_VALUE(metadata, '$.address') AS [Endereço],
    metadata AS [Metadata],
    data_extracao AS [Data de extracao]
FROM dbo.localizacao_cargas;
GO

PRINT 'View vw_localizacao_cargas_powerbi criada/atualizada com sucesso!';
GO
