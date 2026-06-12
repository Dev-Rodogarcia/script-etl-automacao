-- ============================================
-- Script de criação da view 'vw_manifestos_powerbi'
-- Execute este script UMA VEZ antes de colocar o sistema em produção
-- ============================================

CREATE OR ALTER VIEW dbo.vw_manifestos_powerbi AS
WITH CTE_Coletas_Receita AS (
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
)
SELECT
    CAST(MAX(m.created_at) AS TIME(0))                 AS [Hora (Solicitacao)],
    CAST(MAX(m.created_at) AS TIME(0))                 AS [Hora (Criação)],
    m.sequence_code                                     AS [Número],
    MAX(m.identificador_unico)                          AS [Identificador Único],
    CASE MAX(m.status)
        WHEN 'closed' THEN 'encerrado'
        WHEN 'in_transit' THEN 'em trânsito'
        WHEN 'pending' THEN 'pendente'
        ELSE MAX(m.status)
    END                                                 AS [Status],
    MAX(m.classification)                               AS [Classificação],
    MAX(m.branch_nickname)                              AS [Filial],
    MAX(m.branch_nickname)                              AS [Filial Emissora],
    MAX(m.created_at)                                   AS [Data criação],
    MAX(m.departured_at)                                AS [Saída],
    MAX(m.closed_at)                                    AS [Fechamento],
    MAX(m.finished_at)                                  AS [Chegada],
    MAX(m.mdfe_number)                                  AS [MDFe],
    MAX(m.mdfe_key)                                     AS [MDF-es/Chave],
    CASE MAX(m.mdfe_status)
        WHEN 'pending' THEN 'pendente'
        WHEN 'closed' THEN 'encerrado'
        WHEN 'issued' THEN 'emitido'
        WHEN 'rejected' THEN 'rejeitado'
        ELSE MAX(m.mdfe_status)
    END                                                 AS [MDFe/Status],
    MAX(m.distribution_pole)                            AS [Polo de distribuição],
    MAX(m.vehicle_plate)                                AS [Veículo/Placa],
    MAX(m.vehicle_type)                                 AS [Tipo Veículo],
    MAX(m.vehicle_owner)                                AS [Proprietário/Nome],
    MAX(proprietario_documento.proprietario_documento)  AS [Proprietário/Documento],
    MAX(tipo_motorista.tipo_motorista)                  AS [Tipo Motorista],
    MAX(m.driver_name)                                  AS [Motorista],
    MAX(m.vehicle_departure_km)                         AS [Km saída],
    MAX(m.closing_km)                                   AS [Km chegada],
    MAX(m.traveled_km)                                  AS [KM viagem],
    CASE MAX(CAST(m.manual_km AS INT))
        WHEN 1 THEN 'é manual'
        WHEN 0 THEN 'não é manual'
        ELSE NULL
    END                                                 AS [Km manual],
    MAX(m.invoices_count)                               AS [Qtd NF],
    MAX(m.invoices_volumes)                             AS [Volumes NF],
    MAX(m.invoices_weight)                              AS [Peso NF],
    MAX(m.total_taxed_weight)                           AS [Total peso taxado],
    MAX(m.total_cubic_volume)                           AS [Total M3],
    MAX(m.invoices_value)                               AS [Valor NF],
    MAX(m.manifest_freights_total)                      AS [Fretes/Total],
    SUM(COALESCE(sub_coleta.receita_coleta, 0))         AS [Coletas/Total],
    (
        MAX(COALESCE(m.manifest_freights_total, 0))
        + SUM(COALESCE(sub_coleta.receita_coleta, 0))
    )                                                   AS [Receita Total Transportada],
    STRING_AGG(CAST(m.pick_sequence_code AS NVARCHAR(MAX)), N', ')
                                                        AS [Coleta/Número],
    MAX(m.contract_number)                              AS [CIOT/Número],
    CASE MAX(m.contract_type)
        WHEN 'aggregate' THEN 'prestador agregado'
        WHEN 'driver' THEN 'motorista autônomo'
        ELSE MAX(m.contract_type)
    END                                                 AS [Tipo de contrato],
    CASE MAX(m.calculation_type)
        WHEN 'price_table' THEN 'tabela de preço'
        WHEN 'agreed' THEN 'acordado'
        ELSE MAX(m.calculation_type)
    END                                                 AS [Tipo de cálculo],
    CASE MAX(m.cargo_type)
        WHEN 'fractioned' THEN 'carga fracionada'
        WHEN 'closed' THEN 'carga fechada'
        ELSE MAX(m.cargo_type)
    END                                                 AS [Tipo de carga],
    MAX(m.daily_subtotal)                               AS [Diária],
    MAX(m.total_cost)                                   AS [Custo total],
    MAX(m.freight_subtotal)                             AS [Valor frete],
    MAX(m.fuel_subtotal)                                AS [Combustível],
    MAX(m.toll_subtotal)                                AS [Pedágio],
    MAX(m.driver_services_total)                        AS [Serviços motorista/Total],
    MAX(m.operational_expenses_total)                   AS [Despesa operacional],
    MAX(m.inss_value)                                   AS [Dados do agregado/INSS],
    MAX(m.sest_senat_value)                             AS [Dados do agregado/SEST/SENAT],
    MAX(m.ir_value)                                     AS [Dados do agregado/IR],
    MAX(m.paying_total)                                 AS [Saldo a pagar],
    MAX(m.uniq_destinations_count)                      AS [Destinos únicos/Qtd],
    CAST(MAX(CAST(m.generate_mdfe AS INT)) AS BIT)      AS [Gerar MDF-e],
    CAST(MAX(CAST(m.monitoring_request AS INT)) AS BIT) AS [Solicitou Monitoramento],
    CASE MAX(CAST(m.monitoring_request AS INT))
        WHEN 1 THEN 'sim'
        WHEN 0 THEN 'não'
        ELSE NULL
    END                                                 AS [Solicitação Monitoramento],
    MAX(m.mobile_read_at)                               AS [Leitura Móvel/Em],
    MAX(m.km)                                           AS [KM Total],
    MAX(m.delivery_manifest_items_count)                AS [Itens/Entrega],
    MAX(m.transfer_manifest_items_count)                AS [Itens/Transferência],
    MAX(m.pick_manifest_items_count)                    AS [Itens/Coleta],
    MAX(m.dispatch_draft_manifest_items_count)          AS [Itens/Despacho Rascunho],
    MAX(m.consolidation_manifest_items_count)           AS [Itens/Consolidação],
    MAX(m.reverse_pick_manifest_items_count)            AS [Itens/Coleta Reversa],
    MAX(m.manifest_items_count)                         AS [Itens/Total],
    MAX(m.finalized_manifest_items_count)               AS [Itens/Finalizados],
    MAX(m.calculated_pick_count)                        AS [Calculado/Coleta],
    MAX(m.calculated_delivery_count)                    AS [Calculado/Entrega],
    MAX(m.calculated_dispatch_count)                    AS [Calculado/Despacho],
    MAX(m.calculated_consolidation_count)               AS [Calculado/Consolidação],
    MAX(m.calculated_reverse_pick_count)                AS [Calculado/Coleta Reversa],
    MAX(m.pick_subtotal)                                AS [Valor/Coletas],
    MAX(m.delivery_subtotal)                            AS [Valor/Entregas],
    MAX(m.dispatch_subtotal)                            AS [Despachos],
    MAX(m.consolidation_subtotal)                       AS [Consolidações],
    MAX(m.reverse_pick_subtotal)                        AS [Coleta Reversa],
    MAX(m.advance_subtotal)                             AS [Adiantamento],
    MAX(m.fleet_costs_subtotal)                         AS [Custos Frota],
    MAX(m.additionals_subtotal)                         AS [Adicionais],
    MAX(m.discounts_subtotal)                           AS [Descontos],
    MAX(m.discount_value)                               AS [Desconto/Valor],
    MAX(m.adjustment_comments)                          AS [Liberação de Custo de Agregado/Comentários],
    MAX(m.iks_id)                                       AS [IKS ID],
    MAX(m.programacao_sequence_code)                    AS [Programação/Número],
    MAX(m.programacao_starting_at)                      AS [Programação/Início],
    MAX(m.programacao_ending_at)                        AS [Programação/Término],
    MAX(m.trailer1_license_plate)                       AS [Carreta 1/Placa],
    MAX(m.trailer1_weight_capacity)                     AS [Carreta 1/Capacidade Peso],
    MAX(m.trailer2_license_plate)                       AS [Carreta 2/Placa],
    MAX(m.trailer2_weight_capacity)                     AS [Carreta 2/Capacidade Peso],
    MAX(m.vehicle_weight_capacity)                      AS [Veículo/Capacidade Peso],
    MAX(m.vehicle_cubic_weight)                         AS [Veículo/Peso Cubado],
    CASE
        WHEN COALESCE(MAX(m.trailer1_weight_capacity), 0) = 0
            THEN COALESCE(MAX(m.vehicle_weight_capacity), 0)
        ELSE
            COALESCE(MAX(m.trailer1_weight_capacity), 0) + COALESCE(MAX(m.trailer2_weight_capacity), 0)
    END                                                 AS [Capacidade Lotação Kg],
    MAX(REPLACE(REPLACE(REPLACE(m.unloading_recipient_names, '[', ''), ']', ''), '"', ''))
                                                        AS [Descarregamento/Destinatários],
    MAX(REPLACE(REPLACE(REPLACE(m.unloading_recipient_names, '[', ''), ']', ''), '"', ''))
                                                        AS [Local de Descarregamento],
    MAX(REPLACE(REPLACE(REPLACE(m.delivery_region_names, '[', ''), ']', ''), '"', ''))
                                                        AS [Entrega/Regiões],
    MAX(m.programacao_cliente)                          AS [Programação/Cliente],
    MAX(m.programacao_tipo_servico)                     AS [Programação/Tipo Serviço],
    MAX(m.creation_user_name)                           AS [Usuário/Emissor],
    MAX(m.adjustment_user_name)                         AS [Usuário/Ajuste],
    MAX(m.obs_operacional)                              AS [Liberação/Comentários Operacionais],
    MAX(m.obs_financeira)                               AS [Comentários Fechamento],
    MAX(m.metadata)                                     AS [Metadata],
    MAX(m.data_extracao)                                AS [Data de extracao]
FROM dbo.manifestos AS m
LEFT JOIN CTE_Coletas_Receita AS sub_coleta
    ON sub_coleta.sequence_code = m.pick_sequence_code
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
WHERE m.excluido_na_origem = 0
GROUP BY m.sequence_code;
GO

PRINT 'View vw_manifestos_powerbi criada/atualizada com sucesso!';
GO
