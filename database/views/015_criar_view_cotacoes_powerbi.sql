-- ============================================
-- Script de criação da view 'vw_cotacoes_powerbi'
-- Execute este script UMA VEZ antes de colocar o sistema em produção
-- ============================================

CREATE OR ALTER VIEW dbo.vw_cotacoes_powerbi AS
SELECT
    sequence_code                                   AS [N° Cotação],
    requested_at                                    AS [Data Cotação],
    branch_nickname                                 AS [Filial],
    requester_name                                  AS [Solicitante],
    customer_name                                   AS [Cliente Pagador],
    customer_doc                                    AS [CNPJ/CPF Cliente],
    origin_city                                     AS [Cidade Origem],
    origin_state                                    AS [UF Origem],
    destination_city                                AS [Cidade Destino],
    destination_state                               AS [UF Destino],
    volumes                                         AS [Volume],
    real_weight                                     AS [Peso real],
    taxed_weight                                    AS [Peso taxado],
    invoices_value                                  AS [Valor NF],
    total_value                                     AS [Valor frete],
    price_table                                     AS [Tabela],
    user_name                                       AS [Usuário],
    company_name                                    AS [Empresa],
    operation_type                                  AS [Tipo de operação],
    origin_postal_code                              AS [CEP Origem],
    destination_postal_code                         AS [CEP Destino],
    metadata                                        AS [Metadata],
    data_extracao                                   AS [Data de extracao],
    CASE
        WHEN cte_issued_at IS NOT NULL
            OR nfse_issued_at IS NOT NULL
        THEN 'Convertida'
        WHEN disapprove_comments IS NOT NULL
            AND LEN(disapprove_comments) > 0
        THEN 'Reprovada'
        ELSE 'Pendente'
    END                                             AS [Status Conversão],
    disapprove_comments                             AS [Motivo Perda],
    freight_comments                                AS [Observações para o frete],
    cte_issued_at                                   AS [CT-e/Data de emissão],
    nfse_issued_at                                  AS [Nfse/Data de emissão],
    customer_nickname                               AS [Pagador/Nome fantasia],
    sender_document                                 AS [Remetente/CNPJ],
    sender_nickname                                 AS [Remetente/Nome fantasia],
    receiver_document                               AS [Destinatário/CNPJ],
    receiver_nickname                               AS [Destinatário/Nome fantasia],
    discount_subtotal                               AS [Descontos/Subtotal parcelas],
    itr_subtotal                                    AS [Trechos/ITR],
    tde_subtotal                                    AS [Trechos/TDE],
    collect_subtotal                                AS [Trechos/Coleta],
    delivery_subtotal                               AS [Trechos/Entrega],
    other_fees                                      AS [Trechos/Outros valores]
FROM dbo.cotacoes;
GO

PRINT 'View vw_cotacoes_powerbi criada/atualizada com sucesso!';
GO
