PRINT 'Migration 020: adicionar Tipo Motorista na view de manifestos';
GO

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;
SET XACT_ABORT ON;
GO

IF OBJECT_ID(N'dbo.schema_migrations', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.schema_migrations (
        migration_id NVARCHAR(255) NOT NULL,
        applied_at DATETIME2(0) NOT NULL CONSTRAINT DF_schema_migrations_applied_at DEFAULT SYSUTCDATETIME(),
        notes NVARCHAR(MAX) NULL,
        CONSTRAINT PK_schema_migrations PRIMARY KEY (migration_id)
    );
END;
GO

IF OBJECT_ID(N'dbo.manifestos', N'U') IS NULL
BEGIN
    RAISERROR('Tabela dbo.manifestos nao encontrada. Execute os scripts-base antes da migration 020.', 16, 1);
    RETURN;
END;
GO

IF OBJECT_ID(N'dbo.manifestos_frota_propria_cnpjs', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.manifestos_frota_propria_cnpjs (
        cnpj NVARCHAR(14) NOT NULL,
        descricao NVARCHAR(255) NULL,
        ativo BIT NOT NULL CONSTRAINT DF_manifestos_frota_propria_cnpjs_ativo DEFAULT 1,
        criado_em DATETIME2(0) NOT NULL CONSTRAINT DF_manifestos_frota_propria_cnpjs_criado_em DEFAULT SYSUTCDATETIME(),
        atualizado_em DATETIME2(0) NOT NULL CONSTRAINT DF_manifestos_frota_propria_cnpjs_atualizado_em DEFAULT SYSUTCDATETIME(),
        CONSTRAINT PK_manifestos_frota_propria_cnpjs PRIMARY KEY (cnpj)
    );
    PRINT 'Tabela dbo.manifestos_frota_propria_cnpjs criada.';
END
ELSE
BEGIN
    PRINT 'Tabela dbo.manifestos_frota_propria_cnpjs ja existe.';
END;
GO

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
    capacidade_kg                                       AS [Capacidade Lotação Kg],
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
FROM dbo.manifestos
OUTER APPLY (
    SELECT NULLIF(
        REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
            COALESCE(
                CASE WHEN ISJSON(metadata) = 1 THEN JSON_VALUE(metadata, '$.mft_vie_onr_document') END,
                CASE WHEN ISJSON(metadata) = 1 THEN JSON_VALUE(metadata, '$.mft_vie_onr_cnpj') END,
                CASE WHEN ISJSON(metadata) = 1 THEN JSON_VALUE(metadata, '$.vehicle_owner_document') END,
                CASE WHEN ISJSON(metadata) = 1 THEN JSON_VALUE(metadata, '$.owner_document') END,
                CASE WHEN ISJSON(metadata) = 1 THEN JSON_VALUE(metadata, '$.mft_vie_owner_document') END
            ),
            '.', ''), '-', ''), '/', ''), ' ', ''), CHAR(9), ''
        ),
        ''
    ) AS proprietario_documento
) proprietario_documento
OUTER APPLY (
    SELECT CASE
        WHEN EXISTS (
            SELECT 1
            FROM dbo.manifestos_frota_propria_cnpjs cnpj
            WHERE cnpj.ativo = 1
              AND cnpj.cnpj = proprietario_documento.proprietario_documento
        )
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
) tipo_motorista;
GO

IF NOT EXISTS (SELECT 1 FROM dbo.schema_migrations WHERE migration_id = N'020_adicionar_tipo_motorista_manifestos')
BEGIN
    INSERT INTO dbo.schema_migrations (migration_id, notes)
    VALUES (
        N'020_adicionar_tipo_motorista_manifestos',
        N'Publica Proprietário/Documento e Tipo Motorista na view de manifestos, com CNPJs de frota própria configuráveis.'
    );
END;
GO

PRINT 'Migration 020_adicionar_tipo_motorista_manifestos concluida com sucesso.';
GO
