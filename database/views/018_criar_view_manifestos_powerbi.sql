-- ============================================
-- Script de criação da view 'vw_manifestos_powerbi'
-- Execute este script UMA VEZ antes de colocar o sistema em produção
-- ============================================

CREATE OR ALTER VIEW dbo.vw_manifestos_powerbi AS
SELECT

    CAST(created_at AS TIME(0)) AS [Hora (Solicitacao)],

    CAST(created_at AS TIME(0)) AS [Hora (Criação)],
    sequence_code                                       AS [Número],
    identificador_unico                                 AS [Identificador Único],
    CASE status
        WHEN 'closed' THEN 'encerrado'
        WHEN 'in_transit' THEN 'em trânsito'
        WHEN 'pending' THEN 'pendente'
        ELSE status
    END                                                 AS [Status],
    classification                                      AS [Classificação],
    branch_nickname                                     AS [Filial],
    branch_nickname                                     AS [Filial Emissora],
    created_at                                          AS [Data criação],
    departured_at                                       AS [Saída],
    closed_at                                           AS [Fechamento],
    finished_at                                         AS [Chegada],
    mdfe_number                                         AS [MDFe],
    mdfe_key                                            AS [MDF-es/Chave],
    CASE mdfe_status
        WHEN 'pending' THEN 'pendente'
        WHEN 'closed' THEN 'encerrado'
        WHEN 'issued' THEN 'emitido'
        WHEN 'rejected' THEN 'rejeitado'
        ELSE mdfe_status
    END                                                 AS [MDFe/Status],
    distribution_pole                                   AS [Polo de distribuição],
    vehicle_plate                                       AS [Veículo/Placa],
    vehicle_type                                        AS [Tipo Veículo],
    vehicle_owner                                       AS [Proprietário/Nome],
    proprietario_documento.proprietario_documento       AS [Proprietário/Documento],
    tipo_motorista.tipo_motorista                       AS [Tipo Motorista],
    driver_name                                         AS [Motorista],
    vehicle_departure_km                                AS [Km saída],
    closing_km                                          AS [Km chegada],
    traveled_km                                         AS [KM viagem],
    CASE WHEN manual_km = 1 THEN 'é manual'
         WHEN manual_km = 0 THEN 'não é manual'
         ELSE NULL
    END                                                 AS [Km manual],
    invoices_count                                      AS [Qtd NF],
    invoices_volumes                                    AS [Volumes NF],
    invoices_weight                                     AS [Peso NF],
    total_taxed_weight                                  AS [Total peso taxado],
    total_cubic_volume                                  AS [Total M3],
    invoices_value                                      AS [Valor NF],
    manifest_freights_total                             AS [Fretes/Total],
    (COALESCE(manifest_freights_total, 0) + COALESCE(sub_coleta.receita_coleta, 0))
                                                        AS [Receita Total Transportada],
    pick_sequence_code                                  AS [Coleta/Número],
    contract_number                                     AS [CIOT/Número],
    CASE contract_type
        WHEN 'aggregate' THEN 'prestador agregado'
        WHEN 'driver' THEN 'motorista autônomo'
        ELSE contract_type
    END                                                 AS [Tipo de contrato],
    CASE calculation_type
        WHEN 'price_table' THEN 'tabela de preço'
        WHEN 'agreed' THEN 'acordado'
        ELSE calculation_type
    END                                                 AS [Tipo de cálculo],
    CASE cargo_type
        WHEN 'fractioned' THEN 'carga fracionada'
        WHEN 'closed' THEN 'carga fechada'
        ELSE cargo_type
    END                                                 AS [Tipo de carga],
    daily_subtotal                                      AS [Diária],
    total_cost                                          AS [Custo total],
    freight_subtotal                                    AS [Valor frete],
    fuel_subtotal                                       AS [Combustível],
    toll_subtotal                                       AS [Pedágio],
    driver_services_total                               AS [Serviços motorista/Total],
    operational_expenses_total                          AS [Despesa operacional],
    inss_value                                          AS [Dados do agregado/INSS],
    sest_senat_value                                    AS [Dados do agregado/SEST/SENAT],
    ir_value                                            AS [Dados do agregado/IR],
    paying_total                                        AS [Saldo a pagar],
    uniq_destinations_count                             AS [Destinos únicos/Qtd],
    generate_mdfe                                       AS [Gerar MDF-e],
    monitoring_request                                  AS [Solicitou Monitoramento],
    CASE 
        WHEN monitoring_request = 1 THEN 'sim'
        WHEN monitoring_request = 0 THEN 'não'
        ELSE NULL
    END                                                 AS [Solicitação Monitoramento],
    mobile_read_at                                      AS [Leitura Móvel/Em],
    km                                                  AS [KM Total],
    delivery_manifest_items_count                       AS [Itens/Entrega],
    transfer_manifest_items_count                       AS [Itens/Transferência],
    pick_manifest_items_count                           AS [Itens/Coleta],
    dispatch_draft_manifest_items_count                 AS [Itens/Despacho Rascunho],
    consolidation_manifest_items_count                  AS [Itens/Consolidação],
    reverse_pick_manifest_items_count                   AS [Itens/Coleta Reversa],
    manifest_items_count                                AS [Itens/Total],
    finalized_manifest_items_count                      AS [Itens/Finalizados],
    calculated_pick_count                               AS [Calculado/Coleta],
    calculated_delivery_count                           AS [Calculado/Entrega],
    calculated_dispatch_count                           AS [Calculado/Despacho],
    calculated_consolidation_count                      AS [Calculado/Consolidação],
    calculated_reverse_pick_count                       AS [Calculado/Coleta Reversa],
    pick_subtotal                                       AS [Valor/Coletas],
    delivery_subtotal                                   AS [Valor/Entregas],
    dispatch_subtotal                                   AS [Despachos],
    consolidation_subtotal                              AS [Consolidações],
    reverse_pick_subtotal                               AS [Coleta Reversa],
    advance_subtotal                                    AS [Adiantamento],
    fleet_costs_subtotal                                AS [Custos Frota],
    additionals_subtotal                                AS [Adicionais],
    discounts_subtotal                                  AS [Descontos],
    discount_value                                      AS [Desconto/Valor],
    adjustment_comments                                 AS [Liberação de Custo de Agregado/Comentários],
    iks_id                                              AS [IKS ID],
    programacao_sequence_code                           AS [Programação/Número],
    programacao_starting_at                             AS [Programação/Início],
    programacao_ending_at                               AS [Programação/Término],
    trailer1_license_plate                              AS [Carreta 1/Placa],
    trailer1_weight_capacity                            AS [Carreta 1/Capacidade Peso],
    trailer2_license_plate                              AS [Carreta 2/Placa],
    trailer2_weight_capacity                            AS [Carreta 2/Capacidade Peso],
    vehicle_weight_capacity                             AS [Veículo/Capacidade Peso],
    vehicle_cubic_weight                                AS [Veículo/Peso Cubado],
    CASE
        WHEN COALESCE(trailer1_weight_capacity, 0) = 0
            THEN COALESCE(vehicle_weight_capacity, 0)
        ELSE
            COALESCE(trailer1_weight_capacity, 0) + COALESCE(trailer2_weight_capacity, 0)
    END                                                 AS [Capacidade Lotação Kg],
    REPLACE(REPLACE(REPLACE(unloading_recipient_names, '[', ''), ']', ''), '"', '')
                                                        AS [Descarregamento/Destinatários],
    REPLACE(REPLACE(REPLACE(unloading_recipient_names, '[', ''), ']', ''), '"', '')
                                                        AS [Local de Descarregamento],
    REPLACE(REPLACE(REPLACE(delivery_region_names, '[', ''), ']', ''), '"', '')
                                                        AS [Entrega/Regiões],
    programacao_cliente                                 AS [Programação/Cliente],
    programacao_tipo_servico                            AS [Programação/Tipo Serviço],
    creation_user_name                                  AS [Usuário/Emissor],
    adjustment_user_name                                AS [Usuário/Ajuste],
    obs_operacional                                     AS [Liberação/Comentários Operacionais],
    obs_financeira                                      AS [Comentários Fechamento],
    metadata                                            AS [Metadata],
    data_extracao                                       AS [Data de extracao]
FROM dbo.manifestos AS m
LEFT JOIN (
    SELECT
        corporation_sequence_number,
        SUM(COALESCE(valor_total, 0)) AS receita_coleta
    FROM dbo.fretes
    WHERE COALESCE(excluido_na_origem, 0) = 0
    GROUP BY corporation_sequence_number
) sub_coleta ON sub_coleta.corporation_sequence_number = m.pick_sequence_code
OUTER APPLY OPENJSON(CASE WHEN ISJSON(metadata) = 1 THEN metadata END)
WITH (
    mft_vie_onr_document NVARCHAR(255) '$.mft_vie_onr_document',
    mft_vie_onr_cnpj NVARCHAR(255) '$.mft_vie_onr_cnpj',
    vehicle_owner_document NVARCHAR(255) '$.vehicle_owner_document',
    owner_document NVARCHAR(255) '$.owner_document',
    mft_vie_owner_document NVARCHAR(255) '$.mft_vie_owner_document'
) metadata_proprietario
OUTER APPLY (
    SELECT NULLIF(
        REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
            COALESCE(
                metadata_proprietario.mft_vie_onr_document,
                metadata_proprietario.mft_vie_onr_cnpj,
                metadata_proprietario.vehicle_owner_document,
                metadata_proprietario.owner_document,
                metadata_proprietario.mft_vie_owner_document
            ),
            '.', ''), '-', ''), '/', ''), ' ', ''), CHAR(9), ''
        ),
        ''
    ) AS proprietario_documento
) proprietario_documento
LEFT JOIN dbo.manifestos_frota_propria_cnpjs cnpj
    ON cnpj.ativo = 1
   AND cnpj.cnpj = proprietario_documento.proprietario_documento
OUTER APPLY (
    SELECT CASE
        WHEN cnpj.cnpj IS NOT NULL
          OR UPPER(LTRIM(RTRIM(COALESCE(vehicle_owner, '')))) COLLATE Latin1_General_CI_AI = N'RODOGARCIA TRANSPORTES RODOVIARIOS LTDA'
        THEN N'Frota Própria'
        WHEN LOWER(LTRIM(RTRIM(COALESCE(contract_type, '')))) COLLATE Latin1_General_CI_AI IN (
            N'aggregate',
            N'aggregated',
            N'agregado',
            N'prestador agregado',
            N'exclusive',
            N'exclusivo'
        )
          OR LOWER(LTRIM(RTRIM(COALESCE(contract_type, '')))) COLLATE Latin1_General_CI_AI LIKE N'%agreg%'
          OR LOWER(LTRIM(RTRIM(COALESCE(contract_type, '')))) COLLATE Latin1_General_CI_AI LIKE N'%exclus%'
        THEN N'Agregado'
        ELSE N'Terceiro / Autônomo'
    END AS tipo_motorista
) tipo_motorista
WHERE m.excluido_na_origem = 0;
GO

PRINT 'View vw_manifestos_powerbi criada/atualizada com sucesso!';
GO
