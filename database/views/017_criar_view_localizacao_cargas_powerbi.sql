-- ============================================
-- Script de criação da view 'vw_localizacao_cargas_powerbi'
-- Execute este script UMA VEZ antes de colocar o sistema em produção
-- ============================================

CREATE OR ALTER VIEW dbo.vw_localizacao_cargas_powerbi AS
SELECT

    CAST(lc.service_at AS TIME(0)) AS [Hora (Solicitacao)],
    lc.sequence_number AS [N° Minuta], -- CHECK
    REPLACE(lc.type, 'Freight::', '') AS [Tipo], -- CHECK
    lc.service_at AS [Data do frete], -- CHECK
    lc.invoices_volumes AS [Volumes], -- CHECK
    lc.taxed_weight AS [Peso Taxado], -- CHECK
    lc.taxed_weight_decimal AS [Peso Taxado Decimal],
    lc.invoices_value AS [Valor NF], -- CHECK
    lc.invoices_value_decimal AS [Valor NF Decimal],
    lc.total_value AS [Valor Frete], -- CHECK
    lc.service_type AS [Tipo Serviço], -- CHECK
    lc.branch_nickname AS [Filial Emissora], -- CHECK
    lc.predicted_delivery_at AS [Previsão Entrega/Previsão de entrega], -- CHECK
    lc.destination_location_name AS [Região Destino], -- CHECK
    lc.destination_branch_nickname AS [Filial Destino], -- CHECK
    lc.destination_branch_nickname AS [Responsável pela Região de Destino],
    COALESCE(regiao_alias.sigla, N'SEM_MAP') AS [Sigla Responsável Região Destino],
    lc.classification AS [Classificação], -- CHECK
    CASE lc.status
        WHEN 'pending' THEN 'Pendente'
        WHEN 'delivering' THEN 'Em entrega'
        WHEN 'in_warehouse' THEN 'Em armazém'
        WHEN 'in_transfer' THEN 'Em transferência'
        WHEN 'manifested' THEN 'Manifestado'
        WHEN 'finished' THEN 'Finalizado'
        WHEN 'delivered' THEN 'Entregue'
        WHEN 'canceled' THEN 'Cancelado'
        WHEN 'cancelled' THEN 'Cancelado'
        ELSE lc.status
    END AS [Status Carga], -- CHECK
    COALESCE(lc.status_normalized, NULLIF(LOWER(LTRIM(RTRIM(lc.status))), ''), N'sem_status') AS [Status Normalizado],
    CASE WHEN COALESCE(lc.status_normalized, LOWER(LTRIM(RTRIM(lc.status)))) IN (N'finished', N'delivered', N'canceled', N'cancelled', N'finalizado', N'entregue', N'cancelado') THEN 1 ELSE 0 END AS [Status Terminal],
    CASE WHEN COALESCE(lc.status_normalized, LOWER(LTRIM(RTRIM(lc.status)))) IN (N'canceled', N'cancelled', N'cancelado') THEN 1 ELSE 0 END AS [Cancelado Flag],
    lc.status_branch_nickname AS [Filial Atual], -- n encontrado no excel
    lc.origin_location_name AS [Região Origem], -- CHECK
    lc.origin_branch_nickname AS [Filial Origem], -- CHECK
    lc.fit_fln_cln_nickname AS [Localização Atual],
    lc.localizacao_hash AS [Hash Localização],
    lc.metadata AS [Metadata], -- Dados brutos do JSON...
    lc.data_extracao AS [Data de extracao] -- Dados da extracao...
FROM dbo.localizacao_cargas lc
LEFT JOIN dbo.localizacao_cargas_regiao_destino_alias regiao_alias
       ON regiao_alias.nome_responsavel = lc.destination_branch_nickname
      AND regiao_alias.ativo = 1
WHERE COALESCE(lc.excluido_na_origem, 0) = 0;
GO

PRINT 'View vw_localizacao_cargas_powerbi criada/atualizada com sucesso!';
GO
