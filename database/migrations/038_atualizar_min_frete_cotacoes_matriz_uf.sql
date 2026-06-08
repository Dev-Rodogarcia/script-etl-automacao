PRINT 'Migration 038: atualizar Min. Frete/KG de cotacoes por matriz UF x UF';
GO

CREATE OR ALTER VIEW dbo.vw_cotacoes_powerbi AS
SELECT
    -- =============================================
    -- 1. IDENTIFICADORES E DATAS
    -- =============================================
    sequence_code                                   AS [N° Cotação],
    requested_at                                    AS [Data Cotação],

    -- !!! CORREÇÃO DO ERRO DO POWER BI !!!
    -- Adicionada a coluna de Hora para evitar quebra no ETL
    CAST(requested_at AS TIME(0))                   AS [Hora (Solicitacao)],

    data_extracao                                   AS [Data de extracao],

    -- =============================================
    -- 2. UNIDADES E USUÁRIOS
    -- =============================================
    branch_nickname                                 AS [Filial],
    branch_nickname                                 AS [Unidade],           -- Nova
    branch_nickname                                 AS [Unidade_Origem],    -- Nova
    requester_name                                  AS [Solicitante],
    user_name                                       AS [Usuário],
    user_name_key                                   AS [Usuario Key],
    user_name                                       AS [Setor_Usuario],     -- Nova (Placeholder)
    company_name                                    AS [Empresa],

    -- =============================================
    -- 3. CLIENTES
    -- =============================================
    customer_name                                   AS [Cliente Pagador],
    customer_doc                                    AS [CNPJ/CPF Cliente],
    customer_nickname                               AS [Pagador/Nome fantasia],
    customer_name                                   AS [Cliente Grupo],     -- Nova
    COALESCE(customer_nickname, customer_name)      AS [Cliente],           -- Nova

    -- =============================================
    -- 4. ROTA E LOCALIZAÇÃO
    -- =============================================
    origin_city                                     AS [Cidade Origem],
    origin_state                                    AS [UF Origem],
    origin_postal_code                              AS [CEP Origem],
    CONCAT(origin_city, ' - ', origin_state)        AS [Origem],            -- Nova

    destination_city                                AS [Cidade Destino],
    destination_state                               AS [UF Destino],
    destination_postal_code                         AS [CEP Destino],
    CONCAT(destination_city, ' - ', destination_state) AS [Destino],        -- Nova

    CONCAT(origin_city, ' - ', origin_state, ' x ', destination_city, ' - ', destination_state) AS [Trecho], -- Nova

    -- =============================================
    -- 5. CARGA E VALORES (Métricas)
    -- =============================================
    volumes                                         AS [Volume],
    real_weight                                     AS [Peso real],
    taxed_weight                                    AS [Peso taxado],
    invoices_value                                  AS [Valor NF],
    total_value                                     AS [Valor frete],

    -- KPI: Min. Frete/KG (Matriz fixa por UF Origem x UF Destino)
    CASE
        WHEN origin_state = 'SP' AND destination_state = 'SP' THEN 0.66
        WHEN origin_state = 'SP' AND destination_state = 'RJ' THEN 1.21
        WHEN origin_state = 'SP' AND destination_state = 'PR' THEN 0.74
        WHEN origin_state = 'SP' AND destination_state = 'RS' THEN 1.70
        WHEN origin_state = 'SP' AND destination_state = 'PE' THEN 1.54
        WHEN origin_state = 'RJ' AND destination_state = 'SP' THEN 1.21
        WHEN origin_state = 'RJ' AND destination_state = 'RJ' THEN 0.66
        WHEN origin_state = 'RJ' AND destination_state = 'PR' THEN 0.98
        WHEN origin_state = 'RJ' AND destination_state = 'RS' THEN 2.08
        WHEN origin_state = 'RJ' AND destination_state = 'PE' THEN 1.95
        WHEN origin_state = 'PR' AND destination_state = 'SP' THEN 0.74
        WHEN origin_state = 'PR' AND destination_state = 'RJ' THEN 0.88
        WHEN origin_state = 'PR' AND destination_state = 'PR' THEN 0.55
        WHEN origin_state = 'PR' AND destination_state = 'RS' THEN 0.98
        WHEN origin_state = 'PR' AND destination_state = 'PE' THEN 1.83
        WHEN origin_state = 'RS' AND destination_state = 'SP' THEN 1.70
        WHEN origin_state = 'RS' AND destination_state = 'RJ' THEN 1.95
        WHEN origin_state = 'RS' AND destination_state = 'PR' THEN 1.46
        WHEN origin_state = 'RS' AND destination_state = 'RS' THEN 0.98
        WHEN origin_state = 'RS' AND destination_state = 'PE' THEN 2.20
        WHEN origin_state = 'PE' AND destination_state = 'SP' THEN 1.46
        WHEN origin_state = 'PE' AND destination_state = 'RJ' THEN 2.20
        WHEN origin_state = 'PE' AND destination_state = 'PR' THEN 2.20
        WHEN origin_state = 'PE' AND destination_state = 'RS' THEN 2.32
        WHEN origin_state = 'PE' AND destination_state = 'PE' THEN 0.55
        ELSE 0.00
    END                                             AS [Min. Frete/KG],     -- Nova

    price_table                                     AS [Tabela],
    operation_type                                  AS [Tipo de operação],
    metadata                                        AS [Metadata],

    -- =============================================
    -- 6. STATUS E REFINAMENTOS
    -- =============================================
    -- Status Original
    CASE
        WHEN cte_issued_at IS NOT NULL OR nfse_issued_at IS NOT NULL THEN 'Convertida'
        WHEN disapprove_comments IS NOT NULL AND LEN(disapprove_comments) > 0 THEN 'Reprovada'
        ELSE 'Pendente'
    END                                             AS [Status Conversão],

    -- Novos Status Solicitados
    CASE
        WHEN cte_issued_at IS NOT NULL OR nfse_issued_at IS NOT NULL THEN 'Convertida'
        WHEN disapprove_comments IS NOT NULL AND LEN(disapprove_comments) > 0 THEN 'Reprovada'
        ELSE 'Pendente'
    END                                             AS [Status_Sistema],    -- Nova

    CASE
        WHEN cte_issued_at IS NOT NULL THEN 'Emitido'
        ELSE 'Pendente'
    END                                             AS [Status_Sistema_CTe], -- Nova

    CASE
        WHEN nfse_issued_at IS NOT NULL THEN 'Emitida'
        ELSE 'Pendente'
    END                                             AS [Status_Sistema_NFSe], -- Nova

    CASE
        WHEN cte_issued_at IS NOT NULL THEN 'Sim'
        ELSE 'Não'
    END                                             AS [Refino_CTe],        -- Nova

    -- =============================================
    -- 7. DETALHES FISCAIS E OBSERVACÕES
    -- =============================================
    disapprove_comments                             AS [Motivo Perda],
    freight_comments                                AS [Observações para o frete],
    cte_issued_at                                   AS [CT-e/Data de emissão],
    nfse_issued_at                                  AS [Nfse/Data de emissão],
    sender_document                                 AS [Remetente/CNPJ],
    sender_nickname                                 AS [Remetente/Nome fantasia],
    receiver_document                               AS [Destinatário/CNPJ],
    receiver_nickname                               AS [Destinatário/Nome fantasia],

    -- =============================================
    -- 8. COMPOSIÇÃO DE FRETE (TRECHOS)
    -- =============================================
    discount_subtotal                               AS [Descontos/Subtotal parcelas],
    itr_subtotal                                    AS [Trechos/ITR],
    tde_subtotal                                    AS [Trechos/TDE],
    collect_subtotal                                AS [Trechos/Coleta],
    delivery_subtotal                               AS [Trechos/Entrega],
    other_fees                                      AS [Trechos/Outros valores]

FROM dbo.cotacoes
WHERE COALESCE(excluido_na_origem, 0) = 0;
GO

IF OBJECT_ID(N'dbo.schema_migrations', N'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM dbo.schema_migrations WHERE migration_id = N'038_atualizar_min_frete_cotacoes_matriz_uf')
BEGIN
    INSERT INTO dbo.schema_migrations (migration_id, notes)
    VALUES (
        N'038_atualizar_min_frete_cotacoes_matriz_uf',
        N'Atualiza dbo.vw_cotacoes_powerbi para calcular Min. Frete/KG por matriz fixa de UF origem x UF destino.'
    );
END;
GO

PRINT 'View vw_cotacoes_powerbi atualizada com matriz UF x UF para Min. Frete/KG.';
GO
