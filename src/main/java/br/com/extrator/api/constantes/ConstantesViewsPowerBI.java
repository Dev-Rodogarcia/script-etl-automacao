package br.com.extrator.api.constantes;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import br.com.extrator.util.validacao.ConstantesEntidades;

/**
 * Constantes centralizadas para Views do Power BI.
 * Utiliza Record para agrupar nome e SQL de cada view,
 * facilitando manutenção e evitando duplicação entre repositories e ExportadorCSV.
 * 
 * Para adicionar/modificar uma view: apenas editar a entrada no Map VIEWS.
 * As alterações refletem automaticamente em todos os repositories e no ExportadorCSV.
 * 
 * @author Sistema de Extração ESL Cloud
 * @version 1.0
 */
public final class ConstantesViewsPowerBI {

    private ConstantesViewsPowerBI() {
        // Impede instanciação
    }

    // ========== RECORD DE VIEW ==========
    /**
     * Record que agrupa nome e SQL de uma view do Power BI.
     * 
     * @param nomeView Nome da view (ex: "vw_faturas_por_cliente_powerbi")
     * @param sqlView SQL completo da view (CREATE OR ALTER VIEW...)
     */
    public record ViewPowerBI(
        String nomeView,
        String sqlView
    ) {}

    // ========== VIEWS SQL ==========
    
    private static final String SQL_VIEW_FATURAS_POR_CLIENTE = """
        CREATE OR ALTER VIEW dbo.vw_faturas_por_cliente_powerbi AS
        SELECT
            unique_id AS [ID Único],
            filial AS [Filial],
            estado AS [Estado],
            numero_cte AS [CT-e/Número],
            CASE 
                WHEN numero_cte IS NOT NULL THEN CONVERT(NVARCHAR(50), numero_cte)
                WHEN numero_nfse IS NOT NULL THEN CONVERT(NVARCHAR(50), numero_nfse)
                ELSE NULL
            END AS [Número do Documento],
            chave_cte AS [CT-e/Chave],
            data_emissao_cte AS [CT-e/Data de emissão],
            valor_frete AS [Frete/Valor dos CT-es],
            third_party_ctes_value AS [Terceiros/Valor CT-es],
            status_cte AS [CT-e/Status],
            status_cte_result AS [CT-e/Resultado],
            tipo_frete AS [Tipo],
            classificacao AS [Classificação],
            pagador_nome AS [Pagador do frete/Nome],
            pagador_documento AS [Pagador do frete/Documento],
            remetente_nome AS [Remetente/Nome],
            remetente_documento AS [Remetente/Documento],
            destinatario_nome AS [Destinatário/Nome],
            destinatario_documento AS [Destinatário/Documento],
            vendedor_nome AS [Vendedor/Nome],
            numero_nfse AS [NFS-e/Número],
            serie_nfse AS [NFS-e/Série],
            numero_nfse AS [fit_nse_number],
            CASE WHEN fit_ant_document IS NOT NULL THEN 'Faturado' ELSE 'Aguardando Faturamento' END AS [Status do Processo],
            fit_ant_document AS [Fatura/N° Documento],
            fit_ant_issue_date AS [Fatura/Emissão],
            fit_ant_value AS [Fatura/Valor],
            valor_fatura AS [Fatura/Valor Total],
            numero_fatura AS [Fatura/Número],
            data_emissao_fatura AS [Fatura/Emissão Fatura],
            data_vencimento_fatura AS [Parcelas/Vencimento],
            data_baixa_fatura AS [Fatura/Baixa],
            fit_ant_ils_original_due_date AS [Fatura/Data Vencimento Original],
            notas_fiscais AS [Notas Fiscais],
            pedidos_cliente AS [Pedidos/Cliente],
            metadata AS [Metadata],
            data_extracao AS [Data da Última Atualização]
        FROM dbo.faturas_por_cliente
        """;

    private static final String SQL_VIEW_FRETES = """
        CREATE OR ALTER VIEW dbo.vw_fretes_powerbi AS
        SELECT
            id AS [ID],
            chave_cte AS [Chave CT-e],
            numero_cte AS [Nº CT-e],
            serie_cte AS [Série],
            cte_issued_at AS [CT-e Emissão],
            cte_emission_type AS [CT-e Tipo Emissão],
            cte_id AS [CT-e ID],
            cte_created_at AS [CT-e Criado em],
            CASE 
                WHEN cte_id IS NOT NULL OR chave_cte IS NOT NULL OR numero_cte IS NOT NULL OR serie_cte IS NOT NULL THEN 'CT-e'
                WHEN nfse_number IS NOT NULL OR nfse_series IS NOT NULL OR nfse_xml_document IS NOT NULL OR nfse_integration_id IS NOT NULL THEN 'NFS-e'
                ELSE 'Pendente/Não Emitido'
            END AS [Documento Oficial/Tipo],
            CASE 
                WHEN cte_id IS NOT NULL OR chave_cte IS NOT NULL THEN chave_cte
                ELSE NULL
            END AS [Documento Oficial/Chave],
            CASE 
                WHEN cte_id IS NOT NULL OR chave_cte IS NOT NULL THEN CONVERT(NVARCHAR(50), numero_cte)
                ELSE CONVERT(NVARCHAR(50), nfse_number)
            END AS [Documento Oficial/Número],
            CASE 
                WHEN cte_id IS NOT NULL OR chave_cte IS NOT NULL THEN CONVERT(NVARCHAR(50), serie_cte)
                ELSE nfse_series
            END AS [Documento Oficial/Série],
            CASE 
                WHEN cte_id IS NOT NULL OR chave_cte IS NOT NULL THEN NULL
                ELSE nfse_xml_document
            END AS [Documento Oficial/XML],
            servico_em AS [Data frete],
            criado_em AS [Criado em],
            valor_total AS [Valor Total do Serviço],
            valor_notas AS [Valor NF],
            peso_notas AS [Kg NF],
            subtotal AS [Valor Frete],
            invoices_total_volumes AS [Volumes],
            taxed_weight AS [Kg Taxado],
            real_weight AS [Kg Real],
            cubages_cubed_weight AS [Kg Cubado],
            total_cubic_volume AS [M3],
            pagador_nome AS [Pagador],
            pagador_documento AS [Pagador Doc],
            pagador_id AS [Pagador ID],
            remetente_nome AS [Remetente],
            remetente_documento AS [Remetente Doc],
            remetente_id AS [Remetente ID],
            origem_cidade AS [Origem],
            origem_uf AS [UF Origem],
            destinatario_nome AS [Destinatario],
            destinatario_documento AS [Destinatario Doc],
            destinatario_id AS [Destinatario ID],
            destino_cidade AS [Destino],
            destino_uf AS [UF Destino],
            filial_nome AS [Filial],
            filial_apelido AS [Filial Apelido],
            filial_cnpj AS [Filial CNPJ],
            tabela_preco_nome AS [Tabela de Preço],
            classificacao_nome AS [Classificação],
            centro_custo_nome AS [Centro de Custo],
            usuario_nome AS [Usuário],
            numero_nota_fiscal AS [NF],
            reference_number AS [Referência],
            id_corporacao AS [Corp ID],
            id_cidade_destino AS [Cidade Destino ID],
            data_previsao_entrega AS [Previsão de Entrega],
            modal AS [Modal],
            CASE status
                WHEN 'pending' THEN 'pendente'
                WHEN 'finished' THEN 'finalizado'
                WHEN 'in_transit' THEN 'em trânsito'
                WHEN 'standby' THEN 'aguardando'
                WHEN 'manifested' THEN 'registrado'
                WHEN 'occurrence_treatment' THEN 'tratamento de ocorrência'
                ELSE status
            END AS [Status],
            REPLACE(tipo_frete, 'Freight::', '') AS [Tipo Frete],
            service_type AS [Service Type],
            CASE WHEN insurance_enabled = 1 THEN 'Com seguro'
                 WHEN insurance_enabled = 0 THEN 'Sem seguro'
                 ELSE NULL
            END AS [Seguro Habilitado],
            gris_subtotal AS [GRIS],
            tde_subtotal AS [TDE],
            freight_weight_subtotal AS [Frete Peso],
            ad_valorem_subtotal AS [Ad Valorem],
            toll_subtotal AS [Pedágio],
            itr_subtotal AS [ITR],
            modal_cte AS [Modal CT-e],
            redispatch_subtotal AS [Redispatch],
            suframa_subtotal AS [SUFRAMA],
            CASE payment_type
                WHEN 'bill' THEN 'cobrança'
                WHEN 'cash' THEN 'dinheiro'
                ELSE payment_type
            END AS [Tipo Pagamento],
            CASE previous_document_type
                WHEN 'electronic' THEN 'eletrônico'
                ELSE previous_document_type
            END AS [Doc Anterior],
            products_value AS [Valor Produtos],
            trt_subtotal AS [TRT],
            fiscal_cst_type AS [ICMS CST],
            fiscal_cfop_code AS [CFOP],
            fiscal_tax_value AS [Valor ICMS],
            fiscal_pis_value AS [Valor PIS],
            fiscal_cofins_value AS [Valor COFINS],
            fiscal_calculation_basis AS [Base de Cálculo ICMS],
            fiscal_tax_rate AS [Alíquota ICMS %],
            fiscal_pis_rate AS [Alíquota PIS %],
            fiscal_cofins_rate AS [Alíquota COFINS %],
            CASE WHEN fiscal_has_difal = 1 THEN 'possui'
                 WHEN fiscal_has_difal = 0 THEN 'não possui'
                 ELSE NULL
            END AS [Possui DIFAL],
            fiscal_difal_origin AS [DIFAL Origem],
            fiscal_difal_destination AS [DIFAL Destino],
            nfse_series AS [Série NFS-e],
            nfse_number AS [Nº NFS-e],
            nfse_integration_id AS [NFS-e/ID Integração],
            nfse_status AS [NFS-e/Status],
            nfse_issued_at AS [NFS-e/Emissão],
            nfse_cancelation_reason AS [NFS-e/Cancelamento/Motivo],
            nfse_pdf_service_url AS [NFS-e/PDF],
            nfse_corporation_id AS [NFS-e/Filial ID],
            nfse_service_description AS [NFS-e/Serviço/Descrição],
            nfse_xml_document AS [NFS-e/XML],
            insurance_id AS [Seguro ID],
            other_fees AS [Outras Tarifas],
            km AS [KM],
            payment_accountable_type AS [Tipo Contábil Pagamento],
            insured_value AS [Valor Segurado],
            CASE WHEN globalized = 1 THEN 'verdadeiro'
                 WHEN globalized = 0 THEN 'falso'
                 ELSE NULL
            END AS [Globalizado],
            sec_cat_subtotal AS [SEC/CAT],
            CASE globalized_type
                WHEN 'none' THEN 'nenhum'
                ELSE globalized_type
            END AS [Tipo Globalizado],
            price_table_accountable_type AS [Tipo Contábil Tabela],
            insurance_accountable_type AS [Tipo Contábil Seguro],
            metadata AS [Metadata],
            data_extracao AS [Data de extracao]
        FROM dbo.fretes
        """;

    private static final String SQL_VIEW_COLETAS = """
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
        FROM dbo.coletas
        """;

    private static final String SQL_VIEW_FATURAS_GRAPHQL = """
        CREATE OR ALTER VIEW dbo.vw_faturas_graphql_powerbi AS
        SELECT
            id                 AS [ID],
            document           AS [Fatura/N° Documento],
            issue_date         AS [Emissão],
            due_date           AS [Vencimento],
            original_due_date  AS [Vencimento Original],
            value              AS [Valor],
            paid_value         AS [Valor Pago],
            value_to_pay       AS [Valor a Pagar],
            discount_value     AS [Valor Desconto],
            interest_value     AS [Valor Juros],
            paid               AS [Pago],
            status             AS [Status],
            type               AS [Tipo],
            comments           AS [Observações],
            sequence_code      AS [Código Sequencial],
            competence_month   AS [Mês Competência],
            competence_year    AS [Ano Competência],
            created_at         AS [Data Criação],
            updated_at         AS [Data Atualização],
            corporation_id     AS [Filial/ID],
            corporation_name   AS [Filial/Nome],
            corporation_cnpj   AS [Filial/CNPJ],
            metadata           AS [Metadata],
            data_extracao      AS [Data de extracao]
        FROM dbo.faturas_graphql
        """;

    private static final String SQL_VIEW_COTACOES = """
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
        FROM dbo.cotacoes
        """;

    private static final String SQL_VIEW_CONTAS_A_PAGAR = """
        CREATE OR ALTER VIEW dbo.vw_contas_a_pagar_powerbi AS
        SELECT
            sequence_code AS [Lançamento a Pagar/N°],
            document_number AS [N° Documento],
            issue_date AS [Emissão],
            tipo_lancamento AS [Tipo],
            valor_original AS [Valor],
            valor_juros AS [Juros],
            valor_desconto AS [Desconto],
            valor_a_pagar AS [Valor a pagar],
            CASE WHEN status_pagamento = 'PAGO' THEN 'Sim' ELSE 'Não' END AS [Pago],
            valor_pago AS [Valor pago],
            nome_fornecedor AS [Fornecedor/Nome],
            nome_filial AS [Filial],
            classificacao_contabil AS [Conta Contábil/Classificação],
            descricao_contabil AS [Conta Contábil/Descrição],
            valor_contabil AS [Conta Contábil/Valor],
            nome_centro_custo AS [Centro de custo/Nome],
            valor_centro_custo AS [Centro de custo/Valor],
            area_lancamento AS [Área de Lançamento],
            mes_competencia AS [Mês de Competência],
            ano_competencia AS [Ano de Competência],
            data_criacao AS [Data criação],
            observacoes AS [Observações],
            descricao_despesa AS [Descrição da despesa],
            data_liquidacao AS [Baixa/Data liquidação],
            data_transacao AS [Data transação],
            nome_usuario AS [Usuário/Nome],
            status_pagamento AS [Status Pagamento],
            CASE WHEN reconciliado = 1 THEN 'Conciliado'
                 WHEN reconciliado = 0 THEN 'Não conciliado'
                 ELSE NULL
            END AS [Conciliado],
            metadata AS [Metadata],
            data_extracao AS [Data de extracao]
        FROM dbo.contas_a_pagar
        """;

    private static final String SQL_VIEW_LOCALIZACAO_CARGAS = """
        CREATE OR ALTER VIEW dbo.vw_localizacao_cargas_powerbi AS
        SELECT
            sequence_number AS [N° Minuta],
            REPLACE(type, 'Freight::', '') AS [Tipo],
            service_at AS [Data do frete],
            invoices_volumes AS [Volumes],
            taxed_weight AS [Peso Taxado],
            invoices_value AS [Valor NF],
            total_value AS [Valor Frete],
            service_type AS [Tipo Serviço],
            branch_nickname AS [Filial Emissora],
            predicted_delivery_at AS [Previsão Entrega/Previsão de entrega],
            destination_location_name AS [Região Destino],
            destination_branch_nickname AS [Filial Destino],
            classification AS [Classificação],
            CASE status
                WHEN 'pending' THEN 'Pendente'
                WHEN 'delivering' THEN 'Em entrega'
                WHEN 'in_warehouse' THEN 'Em armazém'
                WHEN 'in_transfer' THEN 'Em transferência'
                WHEN 'manifested' THEN 'Manifestado'
                WHEN 'finished' THEN 'Finalizado'
                ELSE status
            END AS [Status Carga],
            status_branch_nickname AS [Filial Atual],
            origin_location_name AS [Região Origem],
            origin_branch_nickname AS [Filial Origem],
            TRY_CONVERT(DECIMAL(10,6), JSON_VALUE(metadata, '$.latitude')) AS [Latitude],
            TRY_CONVERT(DECIMAL(10,6), JSON_VALUE(metadata, '$.longitude')) AS [Longitude],
            TRY_CONVERT(DECIMAL(10,2), JSON_VALUE(metadata, '$.speed')) AS [Velocidade],
            TRY_CONVERT(DECIMAL(10,2), JSON_VALUE(metadata, '$.altitude')) AS [Altitude],
            JSON_VALUE(metadata, '$.device_id') AS [Dispositivo ID],
            JSON_VALUE(metadata, '$.device_type') AS [Dispositivo Tipo],
            JSON_VALUE(metadata, '$.address') AS [Endereço],
            metadata AS [Metadata],
            data_extracao AS [Data de extracao]
        FROM dbo.localizacao_cargas
        """;

    private static final String SQL_VIEW_MANIFESTOS = """
        CREATE OR ALTER VIEW dbo.vw_manifestos_powerbi AS
        SELECT
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
            vehicle_type                                        AS [Tipo Veículo/Nome],
            vehicle_owner                                       AS [Proprietário/Nome],
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
            REPLACE(REPLACE(unloading_recipient_names, '[', ''), ']', '')
                                                                AS [Descarregamento/Destinatários],
            REPLACE(REPLACE(REPLACE(delivery_region_names, '[', ''), ']', ''), '"', '')
                                                                AS [Entrega/Regiões],
            programacao_cliente                                 AS [Programação/Cliente],
            programacao_tipo_servico                            AS [Programação/Tipo Serviço],
            creation_user_name                                  AS [Usuário/Emissor],
            adjustment_user_name                                AS [Usuário/Ajuste],
            metadata                                            AS [Metadata],
            data_extracao                                       AS [Data de extracao]
        FROM dbo.manifestos
        """;

    // ========== MAPA DE VIEWS ==========
    /**
     * Mapa de views por entidade.
     * Chave: ConstantesEntidades.* (ex: "fretes", "coletas")
     * Valor: ViewPowerBI com nome e SQL da view
     */
    private static final Map<String, ViewPowerBI> VIEWS;
    
    static {
        VIEWS = new HashMap<>();
        VIEWS.put(ConstantesEntidades.FATURAS_POR_CLIENTE, 
            new ViewPowerBI("vw_faturas_por_cliente_powerbi", SQL_VIEW_FATURAS_POR_CLIENTE));
        VIEWS.put(ConstantesEntidades.FRETES, 
            new ViewPowerBI("vw_fretes_powerbi", SQL_VIEW_FRETES));
        VIEWS.put(ConstantesEntidades.COLETAS, 
            new ViewPowerBI("vw_coletas_powerbi", SQL_VIEW_COLETAS));
        VIEWS.put(ConstantesEntidades.FATURAS_GRAPHQL, 
            new ViewPowerBI("vw_faturas_graphql_powerbi", SQL_VIEW_FATURAS_GRAPHQL));
        VIEWS.put(ConstantesEntidades.COTACOES, 
            new ViewPowerBI("vw_cotacoes_powerbi", SQL_VIEW_COTACOES));
        VIEWS.put(ConstantesEntidades.CONTAS_A_PAGAR, 
            new ViewPowerBI("vw_contas_a_pagar_powerbi", SQL_VIEW_CONTAS_A_PAGAR));
        VIEWS.put(ConstantesEntidades.LOCALIZACAO_CARGAS, 
            new ViewPowerBI("vw_localizacao_cargas_powerbi", SQL_VIEW_LOCALIZACAO_CARGAS));
        VIEWS.put(ConstantesEntidades.MANIFESTOS, 
            new ViewPowerBI("vw_manifestos_powerbi", SQL_VIEW_MANIFESTOS));
    }

    // ========== MÉTODO PRINCIPAL ==========
    /**
     * Obtém a view completa de uma entidade.
     * 
     * @param entidade Nome da entidade (usar ConstantesEntidades.*)
     * @return ViewPowerBI com nome e SQL
     * @throws IllegalArgumentException se a entidade não possuir view
     */
    public static ViewPowerBI obterView(final String entidade) {
        final ViewPowerBI view = VIEWS.get(entidade);
        if (view == null) {
            throw new IllegalArgumentException("Entidade não possui view Power BI: " + entidade);
        }
        return view;
    }

    /**
     * Verifica se uma entidade possui view Power BI.
     * 
     * @param entidade Nome da entidade
     * @return true se a entidade possui view
     */
    public static boolean possuiView(final String entidade) {
        return VIEWS.containsKey(entidade);
    }

    // ========== MÉTODOS AUXILIARES ==========
    /**
     * Obtém o nome da view de uma entidade.
     */
    public static String obterNomeView(final String entidade) {
        return obterView(entidade).nomeView();
    }

    /**
     * Obtém o SQL da view de uma entidade.
     */
    public static String obterSqlView(final String entidade) {
        return obterView(entidade).sqlView();
    }

    /**
     * Obtém todas as views configuradas.
     * Útil para o ExportadorCSV atualizar todas as views.
     * 
     * @return Collection com todas as ViewPowerBI
     */
    public static Collection<ViewPowerBI> obterTodasViews() {
        return VIEWS.values();
    }

    /**
     * Obtém o mapa completo de views por entidade.
     * 
     * @return Map imutável com entidade -> ViewPowerBI
     */
    public static Map<String, ViewPowerBI> obterMapaViews() {
        return Map.copyOf(VIEWS);
    }
}

