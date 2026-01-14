-- ============================================
-- Script de criação da view 'vw_coletas_powerbi'
-- Execute este script UMA VEZ antes de colocar o sistema em produção
-- ============================================

CREATE OR ALTER VIEW dbo.vw_coletas_powerbi AS
SELECT
    id AS [ID],
    sequence_code AS [Coleta], -- CHECK
    request_date AS [Solicitacao], -- CHECK
    service_date AS [Agendamento], -- CHECK
    finish_date AS [Finalizacao], -- CHECK
    CASE LOWER(status)
        WHEN 'pending' THEN N'Pendente'
        WHEN 'done' THEN N'Coletada'
        WHEN 'canceled' THEN N'Cancelada'
        WHEN 'finished' THEN N'Finalizada'
        WHEN 'treatment' THEN N'Em tratativa'
        WHEN 'in_transit' THEN N'Em trânsito'
        WHEN 'manifested' THEN N'Manifestada'
        ELSE status
    END AS [Status],
    total_volumes AS [Volumes], -- CHECK
    total_weight AS [Peso Real], -- CHECK
    taxed_weight AS [Peso Taxado], -- CHECK
    total_value AS [Valor NF], -- CHECK
    manifest_item_pick_id AS [Numero Manifesto],
    vehicle_type_id AS [Veiculo], -- CHECK
    cliente_nome AS [Cliente], -- CHECK
    cliente_doc AS [Cliente Doc],
    local_coleta AS [Local da Coleta],
    numero_coleta AS [Numero], -- CHECK
    complemento_coleta AS [Complemento], -- CHECK
    cidade_coleta AS [Cidade], -- CHECK
    bairro_coleta AS [Bairro], -- CHECK
    uf_coleta AS [UF], -- CHECK
    cep_coleta AS [CEP], -- CHECK
    pick_region AS [Região da Coleta], -- CHECK
    filial_id AS [Filial ID],
    filial_nome AS [Filial],
    usuario_nome AS [Usuario], -- CHECK
    cancellation_reason AS [Motivo Cancel.], -- CHECK
    cancellation_user_id AS [Usuario Cancel. ID], -- CHECK
    -- JOIN com dim_usuarios para obter nome do usuário que cancelou
    COALESCE(u_cancel.nome, CAST(c.cancellation_user_id AS VARCHAR(20))) AS [Usuario Cancel. Nome], -- CHECK
    destroy_reason AS [Motivo Exclusao], -- CHECK
    destroy_user_id AS [Usuario Exclusao ID], -- CHECK
    -- JOIN com dim_usuarios para obter nome do usuário que destruiu
    -- ⚠️ ATENÇÃO: Este JOIN assume que destroy_user_id é do tipo Individual (usuários do sistema)
    -- Se destroy_user_id for de outro tipo (ex: motorista, agente), o JOIN retornará NULL
    -- Validar via introspecção GraphQL ou dados reais antes de usar em produção
    COALESCE(u_destroy.nome, CAST(c.destroy_user_id AS VARCHAR(20))) AS [Usuario Exclusao Nome], -- CHECK
    status_updated_at AS [Status Atualizado Em], -- CHECK
    last_occurrence AS [Última Ocorrência], -- CHECK
    acao_ocorrencia AS [Ação da Ocorrência], -- CHECK
    numero_tentativas AS [Nº Tentativas], -- CHECK
    metadata AS [Metadata],
    data_extracao AS [Data de extracao]
FROM dbo.coletas c
LEFT JOIN dbo.dim_usuarios u_cancel ON c.cancellation_user_id = u_cancel.user_id
LEFT JOIN dbo.dim_usuarios u_destroy ON c.destroy_user_id = u_destroy.user_id;
GO

PRINT 'View vw_coletas_powerbi criada/atualizada com sucesso!';
GO
