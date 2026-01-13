-- ============================================
-- Script de criação da view 'vw_coletas_powerbi'
-- Execute este script UMA VEZ antes de colocar o sistema em produção
-- ============================================

CREATE OR ALTER VIEW dbo.vw_coletas_powerbi AS
SELECT
    id AS [ID],
    sequence_code AS [Coleta],
    request_date AS [Solicitacao],
    request_hour AS [Hora (Solicitacao)],
    service_date AS [Agendamento],
    service_start_hour AS [Horario (Inicio)],
    finish_date AS [Finalizacao],
    service_end_hour AS [Hora (Fim)],
    CASE LOWER(status)
        WHEN 'pending' THEN N'pendente'
        WHEN 'done' THEN N'concluído'
        WHEN 'canceled' THEN N'cancelado'
        WHEN 'finished' THEN N'finalizado'
        WHEN 'treatment' THEN N'em tratamento'
        WHEN 'in_transit' THEN N'em trânsito'
        WHEN 'manifested' THEN N'Manifestado'
        ELSE status
    END AS [Status],
    requester AS [Solicitante],
    total_volumes AS [Volumes],
    total_weight AS [Peso Real],
    total_value AS [Valor NF],
    comments AS [Observacoes],
    agent_id AS [Agente],
    manifest_item_pick_id AS [Numero Manifesto],
    vehicle_type_id AS [Veiculo],
    cliente_id AS [Cliente ID],
    cliente_nome AS [Cliente],
    cliente_doc AS [Cliente Doc],
    local_coleta AS [Local da Coleta],
    numero_coleta AS [Numero],
    complemento_coleta AS [Complemento],
    cidade_coleta AS [Cidade],
    bairro_coleta AS [Bairro],
    uf_coleta AS [UF],
    cep_coleta AS [CEP],
    filial_id AS [Filial ID],
    filial_nome AS [Filial],
    filial_cnpj AS [Filial CNPJ],
    usuario_id AS [Usuario ID],
    usuario_nome AS [Usuario],
    cancellation_reason AS [Motivo Cancel.],
    cancellation_user_id AS [Usuario Cancel. ID],
    cargo_classification_id AS [Classificacao Carga ID],
    cost_center_id AS [Centro de Custo ID],
    destroy_reason AS [Motivo Exclusao],
    destroy_user_id AS [Usuario Exclusao ID],
    invoices_cubed_weight AS [Peso Cubado NF],
    lunch_break_end_hour AS [Hora Fim Almoco],
    lunch_break_start_hour AS [Hora Inicio Almoco],
    notification_email AS [Email Notificacao],
    notification_phone AS [Telefone Notificacao],
    pick_type_id AS [Tipo Coleta ID],
    pickup_location_id AS [Local Coleta ID],
    status_updated_at AS [Status Atualizado Em],
    metadata AS [Metadata],
    data_extracao AS [Data de extracao]
FROM dbo.coletas;
GO

PRINT 'View vw_coletas_powerbi criada/atualizada com sucesso!';
GO
