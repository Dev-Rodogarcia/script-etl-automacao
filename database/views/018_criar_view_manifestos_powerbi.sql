-- ============================================
-- Script de criação da view 'vw_manifestos_powerbi'
-- Execute este script UMA VEZ antes de colocar o sistema em produção
-- ============================================

CREATE OR ALTER VIEW dbo.vw_manifestos_powerbi AS
SELECT

    CAST(m.created_at AS TIME(0)) AS [Hora (Solicitacao)],

    CAST(m.created_at AS TIME(0)) AS [Hora (Criação)],
    m.sequence_code                                     AS [Número],
    m.identificador_unico                               AS [Identificador Único],
    CASE m.status
        WHEN 'closed' THEN 'encerrado'
        WHEN 'in_transit' THEN 'em trânsito'
        WHEN 'pending' THEN 'pendente'
        ELSE m.status
    END                                                 AS [Status],
    m.classification                                    AS [Classificação],
    m.branch_nickname                                   AS [Filial],
    m.branch_nickname                                   AS [Filial Emissora],
    m.created_at                                        AS [Data criação],
    m.departured_at                                     AS [Saída],
    m.closed_at                                         AS [Fechamento],
    m.finished_at                                       AS [Chegada],
    m.mdfe_number                                       AS [MDFe],
    m.mdfe_key                                          AS [MDF-es/Chave],
    CASE m.mdfe_status
        WHEN 'pending' THEN 'pendente'
        WHEN 'closed' THEN 'encerrado'
        WHEN 'issued' THEN 'emitido'
        WHEN 'rejected' THEN 'rejeitado'
        ELSE m.mdfe_status
    END                                                 AS [MDFe/Status],
    m.distribution_pole                                 AS [Polo de distribuição],
    m.vehicle_plate                                     AS [Veículo/Placa],
    m.vehicle_type                                      AS [Tipo Veículo],
    m.vehicle_owner                                     AS [Proprietário/Nome],
    proprietario_documento.proprietario_documento       AS [Proprietário/Documento],
    tipo_motorista.tipo_motorista                       AS [Tipo Motorista],
    m.driver_name                                       AS [Motorista],
    m.vehicle_departure_km                              AS [Km saída],
    m.closing_km                                        AS [Km chegada],
    m.traveled_km                                       AS [KM viagem],
    CASE WHEN m.manual_km = 1 THEN 'é manual'
         WHEN m.manual_km = 0 THEN 'não é manual'
         ELSE NULL
    END                                                 AS [Km manual],
    m.invoices_count                                    AS [Qtd NF],
    m.invoices_volumes                                  AS [Volumes NF],
    m.invoices_weight                                   AS [Peso NF],
    m.total_taxed_weight                                AS [Total peso taxado],
    m.total_cubic_volume                                AS [Total M3],
    m.invoices_value                                    AS [Valor NF],
    m.manifest_freights_total                           AS [Fretes/Total],
    (COALESCE(m.manifest_freights_total, 0) + COALESCE(sub_coleta.receita_coleta, 0))
                                                        AS [Receita Total Transportada],
    m.pick_sequence_code                                AS [Coleta/Número],
    m.contract_number                                   AS [CIOT/Número],
    CASE m.contract_type
        WHEN 'aggregate' THEN 'prestador agregado'
        WHEN 'driver' THEN 'motorista autônomo'
        ELSE m.contract_type
    END                                                 AS [Tipo de contrato],
    CASE m.calculation_type
        WHEN 'price_table' THEN 'tabela de preço'
        WHEN 'agreed' THEN 'acordado'
        ELSE m.calculation_type
    END                                                 AS [Tipo de cálculo],
    CASE m.cargo_type
        WHEN 'fractioned' THEN 'carga fracionada'
        WHEN 'closed' THEN 'carga fechada'
        ELSE m.cargo_type
    END                                                 AS [Tipo de carga],
    m.daily_subtotal                                    AS [Diária],
    m.total_cost                                        AS [Custo total],
    m.freight_subtotal                                  AS [Valor frete],
    m.fuel_subtotal                                     AS [Combustível],
    m.toll_subtotal                                     AS [Pedágio],
    m.driver_services_total                             AS [Serviços motorista/Total],
    m.operational_expenses_total                        AS [Despesa operacional],
    m.inss_value                                        AS [Dados do agregado/INSS],
    m.sest_senat_value                                  AS [Dados do agregado/SEST/SENAT],
    m.ir_value                                          AS [Dados do agregado/IR],
    m.paying_total                                      AS [Saldo a pagar],
    m.uniq_destinations_count                           AS [Destinos únicos/Qtd],
    m.generate_mdfe                                     AS [Gerar MDF-e],
    m.monitoring_request                                AS [Solicitou Monitoramento],
    CASE 
        WHEN m.monitoring_request = 1 THEN 'sim'
        WHEN m.monitoring_request = 0 THEN 'não'
        ELSE NULL
    END                                                 AS [Solicitação Monitoramento],
    m.mobile_read_at                                    AS [Leitura Móvel/Em],
    m.km                                                AS [KM Total],
    m.delivery_manifest_items_count                     AS [Itens/Entrega],
    m.transfer_manifest_items_count                     AS [Itens/Transferência],
    m.pick_manifest_items_count                         AS [Itens/Coleta],
    m.dispatch_draft_manifest_items_count               AS [Itens/Despacho Rascunho],
    m.consolidation_manifest_items_count                AS [Itens/Consolidação],
    m.reverse_pick_manifest_items_count                 AS [Itens/Coleta Reversa],
    m.manifest_items_count                              AS [Itens/Total],
    m.finalized_manifest_items_count                    AS [Itens/Finalizados],
    m.calculated_pick_count                             AS [Calculado/Coleta],
    m.calculated_delivery_count                         AS [Calculado/Entrega],
    m.calculated_dispatch_count                         AS [Calculado/Despacho],
    m.calculated_consolidation_count                    AS [Calculado/Consolidação],
    m.calculated_reverse_pick_count                     AS [Calculado/Coleta Reversa],
    m.pick_subtotal                                     AS [Valor/Coletas],
    m.delivery_subtotal                                 AS [Valor/Entregas],
    m.dispatch_subtotal                                 AS [Despachos],
    m.consolidation_subtotal                            AS [Consolidações],
    m.reverse_pick_subtotal                             AS [Coleta Reversa],
    m.advance_subtotal                                  AS [Adiantamento],
    m.fleet_costs_subtotal                              AS [Custos Frota],
    m.additionals_subtotal                              AS [Adicionais],
    m.discounts_subtotal                                AS [Descontos],
    m.discount_value                                    AS [Desconto/Valor],
    m.adjustment_comments                               AS [Liberação de Custo de Agregado/Comentários],
    m.iks_id                                            AS [IKS ID],
    m.programacao_sequence_code                         AS [Programação/Número],
    m.programacao_starting_at                           AS [Programação/Início],
    m.programacao_ending_at                             AS [Programação/Término],
    m.trailer1_license_plate                            AS [Carreta 1/Placa],
    m.trailer1_weight_capacity                          AS [Carreta 1/Capacidade Peso],
    m.trailer2_license_plate                            AS [Carreta 2/Placa],
    m.trailer2_weight_capacity                          AS [Carreta 2/Capacidade Peso],
    m.vehicle_weight_capacity                           AS [Veículo/Capacidade Peso],
    m.vehicle_cubic_weight                              AS [Veículo/Peso Cubado],
    CASE
        WHEN COALESCE(m.trailer1_weight_capacity, 0) = 0
            THEN COALESCE(m.vehicle_weight_capacity, 0)
        ELSE
            COALESCE(m.trailer1_weight_capacity, 0) + COALESCE(m.trailer2_weight_capacity, 0)
    END                                                 AS [Capacidade Lotação Kg],
    REPLACE(REPLACE(REPLACE(m.unloading_recipient_names, '[', ''), ']', ''), '"', '')
                                                        AS [Descarregamento/Destinatários],
    REPLACE(REPLACE(REPLACE(m.unloading_recipient_names, '[', ''), ']', ''), '"', '')
                                                        AS [Local de Descarregamento],
    REPLACE(REPLACE(REPLACE(m.delivery_region_names, '[', ''), ']', ''), '"', '')
                                                        AS [Entrega/Regiões],
    m.programacao_cliente                               AS [Programação/Cliente],
    m.programacao_tipo_servico                          AS [Programação/Tipo Serviço],
    m.creation_user_name                                AS [Usuário/Emissor],
    m.adjustment_user_name                              AS [Usuário/Ajuste],
    m.obs_operacional                                   AS [Liberação/Comentários Operacionais],
    m.obs_financeira                                    AS [Comentários Fechamento],
    m.metadata                                          AS [Metadata],
    m.data_extracao                                     AS [Data de extracao]
FROM dbo.manifestos AS m
LEFT JOIN (
    SELECT
        c.sequence_code AS sequence_code,
        SUM(COALESCE(f.valor_total, 0)) AS receita_coleta
    FROM dbo.coletas AS c
    CROSS APPLY (
        SELECT DISTINCT pick_items.pick_item_id
        FROM OPENJSON(CASE WHEN ISJSON(c.pick_items_ids) = 1 THEN c.pick_items_ids END)
        WITH (pick_item_id BIGINT '$') AS pick_items
        WHERE pick_items.pick_item_id IS NOT NULL
    ) AS coleta_item
    JOIN dbo.fretes AS f
        ON f.pick_item_id = coleta_item.pick_item_id
       AND COALESCE(f.excluido_na_origem, 0) = 0
    WHERE COALESCE(c.excluido_na_origem, 0) = 0
    GROUP BY c.sequence_code
) AS sub_coleta ON sub_coleta.sequence_code = m.pick_sequence_code
OUTER APPLY OPENJSON(CASE WHEN ISJSON(m.metadata) = 1 THEN m.metadata END)
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
          OR UPPER(LTRIM(RTRIM(COALESCE(m.vehicle_owner, '')))) COLLATE Latin1_General_CI_AI = N'RODOGARCIA TRANSPORTES RODOVIARIOS LTDA'
        THEN N'Frota Própria'
        WHEN LOWER(LTRIM(RTRIM(COALESCE(m.contract_type, '')))) COLLATE Latin1_General_CI_AI IN (
            N'aggregate',
            N'aggregated',
            N'agregado',
            N'prestador agregado',
            N'exclusive',
            N'exclusivo'
        )
          OR LOWER(LTRIM(RTRIM(COALESCE(m.contract_type, '')))) COLLATE Latin1_General_CI_AI LIKE N'%agreg%'
          OR LOWER(LTRIM(RTRIM(COALESCE(m.contract_type, '')))) COLLATE Latin1_General_CI_AI LIKE N'%exclus%'
        THEN N'Agregado'
        ELSE N'Terceiro / Autônomo'
    END AS tipo_motorista
) tipo_motorista
WHERE m.excluido_na_origem = 0;
GO

PRINT 'View vw_manifestos_powerbi criada/atualizada com sucesso!';
GO
