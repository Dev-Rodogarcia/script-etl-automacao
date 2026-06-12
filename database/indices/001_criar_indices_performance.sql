-- ============================================================================
-- CORRECAO CRITICA #7: Indices para Melhorar Performance
-- ============================================================================
-- Arquivo: 001_criar_indices_performance.sql
-- Descricao: Cria indices otimizados para queries de auditoria e busca
-- Data: 04/02/2026
-- Autor: Sistema de Auditoria
-- ============================================================================


PRINT 'Iniciando criacao de indices de performance...';

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;
GO

-- ============================================================================
-- MANIFESTOS - Indices para otimizar queries de auditoria e busca
-- ============================================================================

PRINT 'Criando indices para tabela MANIFESTOS...';

-- Indice para queries de auditoria por data_extracao (usado em validacoes)
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_manifestos_data_extracao' AND object_id = OBJECT_ID('dbo.manifestos'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_manifestos_data_extracao
    ON dbo.manifestos(data_extracao DESC)
    INCLUDE (sequence_code, status);

    PRINT '  Indice IX_manifestos_data_extracao criado';
END
ELSE
    PRINT '    Indice IX_manifestos_data_extracao ja existe';

-- Indice composto para busca por sequence_code + data
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_manifestos_busca_sequence' AND object_id = OBJECT_ID('dbo.manifestos'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_manifestos_busca_sequence
    ON dbo.manifestos(sequence_code, data_extracao DESC)
    INCLUDE (mdfe_number, pick_sequence_code);

    PRINT '  Indice IX_manifestos_busca_sequence criado';
END
ELSE
    PRINT '    Indice IX_manifestos_busca_sequence ja existe';

-- Indice para busca por created_at (campo de data do negocio)
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_manifestos_created_at' AND object_id = OBJECT_ID('dbo.manifestos'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_manifestos_created_at
    ON dbo.manifestos(created_at DESC)
    INCLUDE (sequence_code, status, branch_nickname);

    PRINT '  Indice IX_manifestos_created_at criado';
END
ELSE
    PRINT '    Indice IX_manifestos_created_at ja existe';

-- ============================================================================
-- COTACOES - Indices para otimizar queries
-- ============================================================================

PRINT 'Criando indices para tabela COTACOES...';

-- Indice para queries de auditoria por data_extracao
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_cotacoes_data_extracao' AND object_id = OBJECT_ID('dbo.cotacoes'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_cotacoes_data_extracao
    ON dbo.cotacoes(data_extracao DESC)
    INCLUDE (sequence_code, customer_name, total_value);

    PRINT '  Indice IX_cotacoes_data_extracao criado';
END
ELSE
    PRINT '    Indice IX_cotacoes_data_extracao ja existe';

-- Indice para busca por requested_at (data do negocio)
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_cotacoes_requested_at' AND object_id = OBJECT_ID('dbo.cotacoes'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_cotacoes_requested_at
    ON dbo.cotacoes(requested_at DESC)
    INCLUDE (sequence_code, customer_name, branch_nickname);

    PRINT '  Indice IX_cotacoes_requested_at criado';
END
ELSE
    PRINT '    Indice IX_cotacoes_requested_at ja existe';

-- Indice para filtro sargavel por periodo e usuario emissor de cotacao
-- O dashboard sempre reduz a maior volumetria por requested_at; usuario e filtro opcional.
IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_cotacoes_usuario_key_requested_at' AND object_id = OBJECT_ID('dbo.cotacoes'))
BEGIN
    DROP INDEX IX_cotacoes_usuario_key_requested_at ON dbo.cotacoes;
    PRINT '  Indice legado IX_cotacoes_usuario_key_requested_at removido';
END

IF COL_LENGTH('dbo.cotacoes', 'user_name_key') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_cotacoes_requested_at_usuario_key' AND object_id = OBJECT_ID('dbo.cotacoes'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_cotacoes_requested_at_usuario_key
    ON dbo.cotacoes(requested_at DESC, user_name_key)
    INCLUDE (sequence_code, user_name, customer_name, branch_nickname, total_value, taxed_weight, cte_issued_at, nfse_issued_at);

    PRINT '  Indice IX_cotacoes_requested_at_usuario_key criado';
END
ELSE
    PRINT '    Indice IX_cotacoes_requested_at_usuario_key ja existe ou coluna user_name_key ausente';

-- ============================================================================
-- CONTAS A PAGAR - Indices para otimizar queries
-- ============================================================================

PRINT 'Criando indices para tabela CONTAS_A_PAGAR...';

-- Indice para queries de auditoria por issue_date (IMPORTANTE!)
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_contas_pagar_issue_date' AND object_id = OBJECT_ID('dbo.contas_a_pagar'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_contas_pagar_issue_date
    ON dbo.contas_a_pagar(issue_date DESC)
    INCLUDE (sequence_code, status_pagamento, valor_a_pagar, nome_fornecedor);

    PRINT '  Indice IX_contas_pagar_issue_date criado';
END
ELSE
    PRINT '    Indice IX_contas_pagar_issue_date ja existe';

-- Indice para busca por status_pagamento
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_contas_pagar_status' AND object_id = OBJECT_ID('dbo.contas_a_pagar'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_contas_pagar_status
    ON dbo.contas_a_pagar(status_pagamento, issue_date DESC);

    PRINT '  Indice IX_contas_pagar_status criado';
END
ELSE
    PRINT '    Indice IX_contas_pagar_status ja existe';

-- Indice para competencia (ano + mes)
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_contas_pagar_competencia' AND object_id = OBJECT_ID('dbo.contas_a_pagar'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_contas_pagar_competencia
    ON dbo.contas_a_pagar(ano_competencia DESC, mes_competencia DESC)
    INCLUDE (valor_a_pagar, status_pagamento);

    PRINT '  Indice IX_contas_pagar_competencia criado';
END
ELSE
    PRINT '    Indice IX_contas_pagar_competencia ja existe';

-- ============================================================================
-- COLETAS - Indices para otimizar queries
-- ============================================================================

PRINT 'Criando indices para tabela COLETAS...';

-- Indice para queries de auditoria por data_extracao
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_coletas_data_extracao' AND object_id = OBJECT_ID('dbo.coletas'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_coletas_data_extracao
    ON dbo.coletas(data_extracao DESC)
    INCLUDE (id, sequence_code, status, cliente_nome);

    PRINT '  Indice IX_coletas_data_extracao criado';
END
ELSE
    PRINT '    Indice IX_coletas_data_extracao ja existe';

-- Indice para busca por service_date
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_coletas_service_date' AND object_id = OBJECT_ID('dbo.coletas'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_coletas_service_date
    ON dbo.coletas(service_date DESC)
    INCLUDE (sequence_code, status, cliente_nome);

    PRINT '  Indice IX_coletas_service_date criado';
END
ELSE
    PRINT '    Indice IX_coletas_service_date ja existe';

-- Indice para dashboard de Coletas por data de solicitacao, status e origem
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_coletas_request_date_dashboard' AND object_id = OBJECT_ID('dbo.coletas'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_coletas_request_date_dashboard
    ON dbo.coletas(request_date, status, pick_region, cidade_coleta)
    INCLUDE (id, sequence_code, filial_nome, cliente_nome, usuario_nome, data_extracao);

    PRINT '  Indice IX_coletas_request_date_dashboard criado';
END
ELSE
    PRINT '    Indice IX_coletas_request_date_dashboard ja existe';

-- ============================================================================
-- FRETES - Indices para otimizar queries
-- ============================================================================

PRINT 'Criando indices para tabela FRETES...';

-- Indice para queries de auditoria por data_extracao
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_fretes_data_extracao' AND object_id = OBJECT_ID('dbo.fretes'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_fretes_data_extracao
    ON dbo.fretes(data_extracao DESC)
    INCLUDE (id, servico_em, status, valor_total);

    PRINT '  Indice IX_fretes_data_extracao criado';
END
ELSE
    PRINT '    Indice IX_fretes_data_extracao ja existe';

-- Indice para busca por servico_em
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_fretes_servico_em' AND object_id = OBJECT_ID('dbo.fretes'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_fretes_servico_em
    ON dbo.fretes(servico_em DESC)
    INCLUDE (id, status, pagador_nome, valor_total);

    PRINT '  Indice IX_fretes_servico_em criado';
END
ELSE
    PRINT '    Indice IX_fretes_servico_em ja existe';

-- Indice para o painel de faturamento por data de referencia materializada
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_fretes_faturamento_data_elegivel' AND object_id = OBJECT_ID('dbo.fretes'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_fretes_faturamento_data_elegivel
    ON dbo.fretes(data_referencia_faturamento DESC, is_elegivel_faturamento)
    INCLUDE (id, valor_total, subtotal, status, filial_nome, pagador_nome, classificacao_nome);

    PRINT '  Indice IX_fretes_faturamento_data_elegivel criado';
END
ELSE
    PRINT '    Indice IX_fretes_faturamento_data_elegivel ja existe';

-- Indice para filtros de faturamento por responsavel materializado
IF COL_LENGTH('dbo.fretes', 'filial_nome_key') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_fretes_faturamento_responsavel_key' AND object_id = OBJECT_ID('dbo.fretes'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_fretes_faturamento_responsavel_key
    ON dbo.fretes(data_referencia_faturamento DESC, filial_nome_key, is_elegivel_faturamento)
    INCLUDE (id, corporation_sequence_number, valor_total, subtotal, status, filial_nome, pagador_nome, classificacao_nome);

    PRINT '  Indice IX_fretes_faturamento_responsavel_key criado';
END
ELSE
    PRINT '    Indice IX_fretes_faturamento_responsavel_key ja existe ou coluna filial_nome_key ausente';

-- Indice para filtros de performance por previsao e responsavel fallback
IF COL_LENGTH('dbo.fretes', 'filial_nome_key') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_fretes_performance_previsao_key' AND object_id = OBJECT_ID('dbo.fretes'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_fretes_performance_previsao_key
    ON dbo.fretes(data_previsao_entrega, filial_nome_key)
    INCLUDE (id, corporation_sequence_number, finished_at, fit_dpn_performance_finished_at, status, valor_notas, taxed_weight, filial_nome, data_extracao);

    PRINT '  Indice IX_fretes_performance_previsao_key criado';
END
ELSE
    PRINT '    Indice IX_fretes_performance_previsao_key ja existe ou coluna filial_nome_key ausente';

-- Indice de cobertura para deduplicacao por minuta no Dashboard de Performance
IF COL_LENGTH('dbo.fretes', 'corporation_sequence_number') IS NOT NULL
   AND COL_LENGTH('dbo.fretes', 'filial_nome_key') IS NOT NULL
   AND COL_LENGTH('dbo.fretes', 'data_previsao_entrega') IS NOT NULL
   AND COL_LENGTH('dbo.fretes', 'finished_at') IS NOT NULL
   AND COL_LENGTH('dbo.fretes', 'fit_dpn_performance_finished_at') IS NOT NULL
   AND COL_LENGTH('dbo.fretes', 'pagador_nome') IS NOT NULL
   AND COL_LENGTH('dbo.fretes', 'filial_nome') IS NOT NULL
   AND COL_LENGTH('dbo.fretes', 'destino_cidade') IS NOT NULL
   AND COL_LENGTH('dbo.fretes', 'destino_uf') IS NOT NULL
   AND COL_LENGTH('dbo.fretes', 'status') IS NOT NULL
   AND COL_LENGTH('dbo.fretes', 'taxed_weight') IS NOT NULL
   AND COL_LENGTH('dbo.fretes', 'valor_notas') IS NOT NULL
   AND COL_LENGTH('dbo.fretes', 'data_extracao') IS NOT NULL
   AND COL_LENGTH('dbo.fretes', 'excluido_na_origem') IS NOT NULL
   AND NOT EXISTS (
       SELECT 1
       FROM sys.indexes
       WHERE name = 'IX_fretes_performance_minuta_cobertura'
         AND object_id = OBJECT_ID('dbo.fretes')
   )
BEGIN
    CREATE NONCLUSTERED INDEX IX_fretes_performance_minuta_cobertura
    ON dbo.fretes(corporation_sequence_number)
    INCLUDE (
        data_previsao_entrega,
        finished_at,
        fit_dpn_performance_finished_at,
        pagador_nome,
        filial_nome,
        filial_nome_key,
        destino_cidade,
        destino_uf,
        status,
        taxed_weight,
        valor_notas,
        data_extracao,
        excluido_na_origem
    )
    WITH (DATA_COMPRESSION = PAGE);

    PRINT '  Indice IX_fretes_performance_minuta_cobertura criado';
END
ELSE
    PRINT '    Indice IX_fretes_performance_minuta_cobertura ja existe ou colunas obrigatorias ausentes';

-- Indice para ligar receita de fretes aos itens de coleta vindos da API GraphQL
IF COL_LENGTH('dbo.fretes', 'pick_item_id') IS NOT NULL
   AND COL_LENGTH('dbo.fretes', 'valor_total') IS NOT NULL
   AND COL_LENGTH('dbo.fretes', 'excluido_na_origem') IS NOT NULL
   AND NOT EXISTS (
       SELECT 1
       FROM sys.indexes
       WHERE name = 'IX_fretes_pick_item_id'
         AND object_id = OBJECT_ID('dbo.fretes')
   )
BEGIN
    CREATE NONCLUSTERED INDEX IX_fretes_pick_item_id
    ON dbo.fretes(pick_item_id)
    INCLUDE (id, valor_total, excluido_na_origem)
    WHERE pick_item_id IS NOT NULL;

    PRINT '  Indice IX_fretes_pick_item_id criado';
END
ELSE
    PRINT '    Indice IX_fretes_pick_item_id ja existe ou colunas obrigatorias ausentes';

-- ============================================================================
-- LOCALIZACAO DE CARGAS - Indices para otimizar queries
-- ============================================================================

PRINT 'Criando indices para tabela LOCALIZACAO_CARGAS...';

-- Indice para queries de auditoria por data_extracao
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_localizacao_data_extracao' AND object_id = OBJECT_ID('dbo.localizacao_cargas'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_localizacao_data_extracao
    ON dbo.localizacao_cargas(data_extracao DESC)
    INCLUDE (sequence_number, service_at);

    PRINT '  Indice IX_localizacao_data_extracao criado';
END
ELSE
    PRINT '    Indice IX_localizacao_data_extracao ja existe';

-- Indice para busca por service_at
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_localizacao_service_at' AND object_id = OBJECT_ID('dbo.localizacao_cargas'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_localizacao_service_at
    ON dbo.localizacao_cargas(service_at DESC)
    INCLUDE (sequence_number);

    PRINT '  Indice IX_localizacao_service_at criado';
END
ELSE
    PRINT '    Indice IX_localizacao_service_at ja existe';

-- Indice para painel Localizacao de Cargas por filial atual e regiao de destino
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_localizacao_tracking_dashboard' AND object_id = OBJECT_ID('dbo.localizacao_cargas'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_localizacao_tracking_dashboard
    ON dbo.localizacao_cargas(status_branch_nickname, service_at DESC, status_normalized, destination_branch_nickname)
    INCLUDE (sequence_number, localizacao_hash, predicted_delivery_at, total_value, taxed_weight_decimal, invoices_value_decimal, invoices_volumes, data_extracao);

    PRINT '  Indice IX_localizacao_tracking_dashboard criado';
END
ELSE
    PRINT '    Indice IX_localizacao_tracking_dashboard ja existe';

-- Indice para resolver responsavel de destino por chave materializada
IF COL_LENGTH('dbo.localizacao_cargas', 'destination_branch_key') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_localizacao_destination_branch_key' AND object_id = OBJECT_ID('dbo.localizacao_cargas'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_localizacao_destination_branch_key
    ON dbo.localizacao_cargas(destination_branch_key, sequence_number)
    INCLUDE (destination_branch_nickname, destination_location_name, predicted_delivery_at, invoices_volumes, data_extracao);

    PRINT '  Indice IX_localizacao_destination_branch_key criado';
END
ELSE
    PRINT '    Indice IX_localizacao_destination_branch_key ja existe ou coluna destination_branch_key ausente';

-- Indice para reduzir I/O do MERGE por hash operacional
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_localizacao_hash_upsert' AND object_id = OBJECT_ID('dbo.localizacao_cargas'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_localizacao_hash_upsert
    ON dbo.localizacao_cargas(sequence_number, localizacao_hash)
    INCLUDE (status_normalized, status, status_branch_nickname, fit_fln_cln_nickname, destination_branch_nickname, predicted_delivery_at, service_at);

    PRINT '  Indice IX_localizacao_hash_upsert criado';
END
ELSE
    PRINT '    Indice IX_localizacao_hash_upsert ja existe';

-- ============================================================================
-- INVENTARIO - Indices para comprovante anexado na view de fretes
-- ============================================================================

PRINT 'Criando indices para tabela INVENTARIO...';

-- Indice filtrado para resolver o EXISTS de comprovante anexado por minuta sem ler descricao textual
IF EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'IX_inventario_comprovante_minuta'
      AND object_id = OBJECT_ID('dbo.inventario')
      AND (filter_definition IS NULL OR filter_definition NOT LIKE '%flag_comprovante_anexado%')
)
BEGIN
    DROP INDEX IX_inventario_comprovante_minuta ON dbo.inventario;
    PRINT '  Indice IX_inventario_comprovante_minuta antigo removido';
END

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_inventario_comprovante_minuta' AND object_id = OBJECT_ID('dbo.inventario'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_inventario_comprovante_minuta
    ON dbo.inventario(numero_minuta)
    WHERE flag_comprovante_anexado = 1;

    PRINT '  Indice IX_inventario_comprovante_minuta criado';
END
ELSE
    PRINT '    Indice IX_inventario_comprovante_minuta ja existe';

-- ============================================================================
-- SOFT DELETE - Indices filtrados para reconciliacao de ativos na origem
-- ============================================================================

PRINT 'Criando indices filtrados para registros ativos na origem...';

IF COL_LENGTH('dbo.coletas', 'excluido_na_origem') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_coletas_ativos_origem' AND object_id = OBJECT_ID('dbo.coletas'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_coletas_ativos_origem
    ON dbo.coletas(id)
    WHERE excluido_na_origem = 0;

    PRINT '  Indice IX_coletas_ativos_origem criado';
END
ELSE
    PRINT '    Indice IX_coletas_ativos_origem ja existe ou coluna excluido_na_origem ausente';

IF COL_LENGTH('dbo.fretes', 'excluido_na_origem') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_fretes_ativos_origem' AND object_id = OBJECT_ID('dbo.fretes'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_fretes_ativos_origem
    ON dbo.fretes(id)
    WHERE excluido_na_origem = 0;

    PRINT '  Indice IX_fretes_ativos_origem criado';
END
ELSE
    PRINT '    Indice IX_fretes_ativos_origem ja existe ou coluna excluido_na_origem ausente';

IF COL_LENGTH('dbo.manifestos', 'excluido_na_origem') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_manifestos_ativos_origem' AND object_id = OBJECT_ID('dbo.manifestos'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_manifestos_ativos_origem
    ON dbo.manifestos(chave_merge_hash)
    WHERE excluido_na_origem = 0;

    PRINT '  Indice IX_manifestos_ativos_origem criado';
END
ELSE
    PRINT '    Indice IX_manifestos_ativos_origem ja existe ou coluna excluido_na_origem ausente';

IF COL_LENGTH('dbo.cotacoes', 'excluido_na_origem') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_cotacoes_ativos_origem' AND object_id = OBJECT_ID('dbo.cotacoes'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_cotacoes_ativos_origem
    ON dbo.cotacoes(sequence_code)
    WHERE excluido_na_origem = 0;

    PRINT '  Indice IX_cotacoes_ativos_origem criado';
END
ELSE
    PRINT '    Indice IX_cotacoes_ativos_origem ja existe ou coluna excluido_na_origem ausente';

IF COL_LENGTH('dbo.localizacao_cargas', 'excluido_na_origem') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_localizacao_cargas_ativos_origem' AND object_id = OBJECT_ID('dbo.localizacao_cargas'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_localizacao_cargas_ativos_origem
    ON dbo.localizacao_cargas(sequence_number)
    WHERE excluido_na_origem = 0;

    PRINT '  Indice IX_localizacao_cargas_ativos_origem criado';
END
ELSE
    PRINT '    Indice IX_localizacao_cargas_ativos_origem ja existe ou coluna excluido_na_origem ausente';

IF COL_LENGTH('dbo.contas_a_pagar', 'excluido_na_origem') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_contas_a_pagar_ativos_origem' AND object_id = OBJECT_ID('dbo.contas_a_pagar'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_contas_a_pagar_ativos_origem
    ON dbo.contas_a_pagar(sequence_code)
    WHERE excluido_na_origem = 0;

    PRINT '  Indice IX_contas_a_pagar_ativos_origem criado';
END
ELSE
    PRINT '    Indice IX_contas_a_pagar_ativos_origem ja existe ou coluna excluido_na_origem ausente';

IF COL_LENGTH('dbo.faturas_por_cliente', 'excluido_na_origem') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_faturas_por_cliente_ativos_origem' AND object_id = OBJECT_ID('dbo.faturas_por_cliente'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_faturas_por_cliente_ativos_origem
    ON dbo.faturas_por_cliente(unique_id)
    WHERE excluido_na_origem = 0;

    PRINT '  Indice IX_faturas_por_cliente_ativos_origem criado';
END
ELSE
    PRINT '    Indice IX_faturas_por_cliente_ativos_origem ja existe ou coluna excluido_na_origem ausente';

IF COL_LENGTH('dbo.inventario', 'excluido_na_origem') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_inventario_ativos_origem' AND object_id = OBJECT_ID('dbo.inventario'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_inventario_ativos_origem
    ON dbo.inventario(identificador_unico)
    WHERE excluido_na_origem = 0;

    PRINT '  Indice IX_inventario_ativos_origem criado';
END
ELSE
    PRINT '    Indice IX_inventario_ativos_origem ja existe ou coluna excluido_na_origem ausente';

IF COL_LENGTH('dbo.sinistros', 'excluido_na_origem') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_sinistros_ativos_origem' AND object_id = OBJECT_ID('dbo.sinistros'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_sinistros_ativos_origem
    ON dbo.sinistros(identificador_unico)
    WHERE excluido_na_origem = 0;

    PRINT '  Indice IX_sinistros_ativos_origem criado';
END
ELSE
    PRINT '    Indice IX_sinistros_ativos_origem ja existe ou coluna excluido_na_origem ausente';

IF COL_LENGTH('dbo.dim_usuarios', 'excluido_na_origem') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_dim_usuarios_ativos_origem' AND object_id = OBJECT_ID('dbo.dim_usuarios'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_dim_usuarios_ativos_origem
    ON dbo.dim_usuarios(user_id)
    WHERE excluido_na_origem = 0;

    PRINT '  Indice IX_dim_usuarios_ativos_origem criado';
END
ELSE
    PRINT '    Indice IX_dim_usuarios_ativos_origem ja existe ou coluna excluido_na_origem ausente';

IF COL_LENGTH('dbo.raster_viagens', 'excluido_na_origem') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_raster_viagens_ativos_origem' AND object_id = OBJECT_ID('dbo.raster_viagens'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_raster_viagens_ativos_origem
    ON dbo.raster_viagens(cod_solicitacao)
    WHERE excluido_na_origem = 0;

    PRINT '  Indice IX_raster_viagens_ativos_origem criado';
END
ELSE
    PRINT '    Indice IX_raster_viagens_ativos_origem ja existe ou coluna excluido_na_origem ausente';

IF COL_LENGTH('dbo.raster_viagem_paradas', 'excluido_na_origem') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_raster_viagem_paradas_ativos_origem' AND object_id = OBJECT_ID('dbo.raster_viagem_paradas'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_raster_viagem_paradas_ativos_origem
    ON dbo.raster_viagem_paradas(cod_solicitacao, ordem)
    WHERE excluido_na_origem = 0;

    PRINT '  Indice IX_raster_viagem_paradas_ativos_origem criado';
END
ELSE
    PRINT '    Indice IX_raster_viagem_paradas_ativos_origem ja existe ou coluna excluido_na_origem ausente';

-- ============================================================================
-- LOG_EXTRACOES - Indices para otimizar queries de auditoria
-- ============================================================================

PRINT 'Criando indices para tabela LOG_EXTRACOES...';

-- Indice para busca por entidade + timestamp
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_log_extracoes_busca' AND object_id = OBJECT_ID('dbo.log_extracoes'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_log_extracoes_busca
    ON dbo.log_extracoes(entidade, timestamp_fim DESC)
    INCLUDE (status_final, registros_extraidos, paginas_processadas);

    PRINT '  Indice IX_log_extracoes_busca criado';
END
ELSE
    PRINT '    Indice IX_log_extracoes_busca ja existe';

-- ============================================================================
-- ESTATISTICAS DOS INDICES
-- ============================================================================

PRINT '';
PRINT 'Estatisticas de Indices Criados:';

SELECT
    OBJECT_NAME(i.object_id) AS Tabela,
    i.name AS Nome_Indice,
    i.type_desc AS Tipo,
    CAST(ROUND((SUM(a.used_pages) * 8) / 1024.0, 2) AS DECIMAL(10,2)) AS Tamanho_MB
FROM sys.indexes i
INNER JOIN sys.partitions p ON i.object_id = p.object_id AND i.index_id = p.index_id
INNER JOIN sys.allocation_units a ON p.partition_id = a.container_id
WHERE i.name LIKE 'IX_%'
  AND OBJECT_NAME(i.object_id) IN (
      'manifestos', 'cotacoes', 'contas_a_pagar', 'coletas', 'fretes', 'localizacao_cargas',
      'faturas_por_cliente', 'inventario', 'sinistros', 'dim_usuarios', 'raster_viagens',
      'raster_viagem_paradas', 'log_extracoes'
  )
GROUP BY i.object_id, i.name, i.type_desc
ORDER BY Tabela, Nome_Indice;

PRINT '';
PRINT 'Script de criacao de indices concluido com sucesso!';
PRINT '';
PRINT 'RECOMENDACOES:';
PRINT '   1. Execute UPDATE STATISTICS apos carga massiva de dados';
PRINT '   2. Configure AUTO_UPDATE_STATISTICS = ON';
PRINT '   3. Monitore fragmentacao dos indices mensalmente';
PRINT '   4. Considere REBUILD se fragmentacao > 30%';

GO
