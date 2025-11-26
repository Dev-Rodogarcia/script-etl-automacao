DECLARE @missing NVARCHAR(MAX);

WITH expected AS (
    SELECT 'cotacoes' AS tabela, v.col AS coluna FROM (VALUES
        ('sequence_code'),('requested_at'),('operation_type'),('customer_doc'),('customer_name'),
        ('origin_city'),('origin_state'),('destination_city'),('destination_state'),('price_table'),
        ('volumes'),('taxed_weight'),('invoices_value'),('total_value'),('user_name'),
        ('branch_nickname'),('company_name'),('requester_name'),('real_weight'),('origin_postal_code'),
        ('destination_postal_code'),('customer_nickname'),('sender_document'),('sender_nickname'),
        ('receiver_document'),('receiver_nickname'),('disapprove_comments'),('freight_comments'),
        ('discount_subtotal'),('itr_subtotal'),('tde_subtotal'),('collect_subtotal'),('delivery_subtotal'),
        ('other_fees'),('cte_issued_at'),('nfse_issued_at'),('metadata'),('data_extracao')
    ) AS v(col)
    UNION ALL
    SELECT 'fretes', v.col FROM (VALUES
        ('id'),('servico_em'),('criado_em'),('status'),('modal'),('tipo_frete'),('valor_total'),('valor_notas'),('peso_notas'),
        ('id_corporacao'),('id_cidade_destino'),('data_previsao_entrega'),('pagador_id'),('pagador_nome'),('remetente_id'),('remetente_nome'),
        ('origem_cidade'),('origem_uf'),('destinatario_id'),('destinatario_nome'),('destino_cidade'),('destino_uf'),('filial_nome'),
        ('numero_nota_fiscal'),('tabela_preco_nome'),('classificacao_nome'),('centro_custo_nome'),('usuario_nome'),('reference_number'),
        ('chave_cte'),('numero_cte'),('serie_cte'),('invoices_total_volumes'),('taxed_weight'),('real_weight'),('total_cubic_volume'),
        ('subtotal'),
        ('service_type'),('insurance_enabled'),('gris_subtotal'),('tde_subtotal'),('modal_cte'),('redispatch_subtotal'),('suframa_subtotal'),('payment_type'),('previous_document_type'),
        ('products_value'),('trt_subtotal'),('nfse_series'),('nfse_number'),('insurance_id'),('other_fees'),('km'),('payment_accountable_type'),('insured_value'),('globalized'),('sec_cat_subtotal'),('globalized_type'),('price_table_accountable_type'),('insurance_accountable_type'),
        ('metadata'),('data_extracao')
    ) AS v(col)
    UNION ALL
    SELECT 'coletas', v.col FROM (VALUES
        ('id'),('sequence_code'),('request_date'),('service_date'),('status'),('total_value'),('total_weight'),('total_volumes'),
        ('cliente_id'),('cliente_nome'),('local_coleta'),('cidade_coleta'),('uf_coleta'),('usuario_id'),('usuario_nome'),
        ('request_hour'),('service_start_hour'),('finish_date'),('service_end_hour'),('requester'),('taxed_weight'),('comments'),
        ('agent_id'),('manifest_item_pick_id'),('vehicle_type_id'),('cancellation_reason'),('cancellation_user_id'),('cargo_classification_id'),
        ('cost_center_id'),('destroy_reason'),('destroy_user_id'),('invoices_cubed_weight'),('lunch_break_end_hour'),('lunch_break_start_hour'),
        ('notification_email'),('notification_phone'),('pick_type_id'),('pickup_location_id'),('status_updated_at'),('metadata'),('data_extracao')
    ) AS v(col)
    UNION ALL
    SELECT 'contas_a_pagar', v.col FROM (VALUES
        ('sequence_code'),('document_number'),('issue_date'),('tipo_lancamento'),('valor_original'),('valor_juros'),('valor_desconto'),
        ('valor_a_pagar'),('valor_pago'),('status_pagamento'),('mes_competencia'),('ano_competencia'),('data_criacao'),('data_liquidacao'),
        ('data_transacao'),('nome_fornecedor'),('nome_filial'),('nome_centro_custo'),('valor_centro_custo'),('classificacao_contabil'),
        ('descricao_contabil'),('valor_contabil'),('area_lancamento'),('observacoes'),('descricao_despesa'),('nome_usuario'),('reconciliado'),
        ('metadata'),('data_extracao')
    ) AS v(col)
    UNION ALL
    SELECT 'faturas_por_cliente', v.col FROM (VALUES
        ('unique_id'),('valor_frete'),('valor_fatura'),('third_party_ctes_value'),('numero_cte'),('chave_cte'),('numero_nfse'),('status_cte'),
        ('data_emissao_cte'),('numero_fatura'),('data_emissao_fatura'),('data_vencimento_fatura'),('data_baixa_fatura'),('fit_ant_ils_original_due_date'),
        ('fit_ant_document'),('fit_ant_issue_date'),('fit_ant_value'),('filial'),('tipo_frete'),('classificacao'),('estado'),
        ('pagador_nome'),('pagador_documento'),('remetente_nome'),('remetente_documento'),('destinatario_nome'),('destinatario_documento'),
        ('vendedor_nome'),('notas_fiscais'),('pedidos_cliente'),('metadata'),('data_extracao')
    ) AS v(col)
    UNION ALL
    SELECT 'localizacao_cargas', v.col FROM (VALUES
        ('sequence_number'),('type'),('service_at'),('invoices_volumes'),('taxed_weight'),('invoices_value'),('total_value'),('service_type'),
        ('branch_nickname'),('predicted_delivery_at'),('destination_location_name'),('destination_branch_nickname'),('classification'),('metadata'),('data_extracao')
    ) AS v(col)
    UNION ALL
    SELECT 'manifestos', v.col FROM (VALUES
        ('sequence_code'),('identificador_unico'),('status'),('created_at'),('departured_at'),('closed_at'),('finished_at'),('mdfe_number'),('mdfe_key'),
        ('mdfe_status'),('distribution_pole'),('classification'),('vehicle_plate'),('vehicle_type'),('vehicle_owner'),('driver_name'),('branch_nickname'),
        ('vehicle_departure_km'),('closing_km'),('traveled_km'),('invoices_count'),('invoices_volumes'),('invoices_weight'),('total_taxed_weight'),
        ('total_cubic_volume'),('invoices_value'),('manifest_freights_total'),('pick_sequence_code'),('contract_number'),('contract_type'),
        ('calculation_type'),('cargo_type'),('daily_subtotal'),('total_cost'),('freight_subtotal'),('fuel_subtotal'),('toll_subtotal'),('driver_services_total'),
        ('operational_expenses_total'),('inss_value'),('sest_senat_value'),('ir_value'),('paying_total'),('manual_km'),('generate_mdfe'),('monitoring_request'),
        ('uniq_destinations_count'),('mobile_read_at'),('km'),('manifest_items_count'),('finalized_manifest_items_count'),('metadata'),('data_extracao')
    ) AS v(col)
), actual AS (
    SELECT LOWER(TABLE_NAME) AS tabela, LOWER(COLUMN_NAME) AS coluna
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'dbo'
), missing AS (
    SELECT e.tabela, e.coluna
    FROM expected e
    LEFT JOIN actual a ON a.tabela = e.tabela AND a.coluna = e.coluna
    WHERE a.coluna IS NULL
)
SELECT @missing = STRING_AGG(e.tabela + '.' + e.coluna, ', ') FROM missing e;

IF @missing IS NOT NULL AND LEN(@missing) > 0
BEGIN
    DECLARE @msg NVARCHAR(MAX) = 'Faltam colunas: ' + @missing;
    THROW 50001, @msg, 1;
END
ELSE
BEGIN
    SELECT 'OK' AS status, GETDATE() AS verificado_em;
END
