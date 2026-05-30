-- Valida se uma recriacao limpa do banco deixou o schema essencial coerente.
-- Seguro para rodar em banco vazio: confere estrutura, migrations, views e indices.

SET NOCOUNT ON;

DECLARE @falhas TABLE (
    tipo NVARCHAR(50) NOT NULL,
    nome NVARCHAR(255) NOT NULL,
    detalhe NVARCHAR(500) NULL
);

DECLARE @tabelas TABLE (nome SYSNAME NOT NULL);
INSERT INTO @tabelas (nome) VALUES
    (N'coletas'),
    (N'fretes'),
    (N'manifestos'),
    (N'cotacoes'),
    (N'localizacao_cargas'),
    (N'localizacao_cargas_regiao_destino_alias'),
    (N'contas_a_pagar'),
    (N'faturas_por_cliente'),
    (N'log_extracoes'),
    (N'page_audit'),
    (N'dim_usuarios'),
    (N'sys_execution_history'),
    (N'sys_auditoria_temp'),
    (N'sys_execution_audit'),
    (N'sys_execution_watermark'),
    (N'dim_usuarios_historico'),
    (N'schema_migrations'),
    (N'etl_invalid_records'),
    (N'inventario'),
    (N'sinistros'),
    (N'sys_replay_idempotency'),
    (N'sys_reconciliation_quarantine'),
    (N'raster_viagens'),
    (N'raster_viagem_paradas');

INSERT INTO @falhas (tipo, nome, detalhe)
SELECT N'TABELA', t.nome, N'Tabela dbo.' + t.nome + N' ausente'
FROM @tabelas t
WHERE OBJECT_ID(N'dbo.' + t.nome, N'U') IS NULL;

DECLARE @views TABLE (nome SYSNAME NOT NULL);
INSERT INTO @views (nome) VALUES
    (N'vw_faturas_por_cliente_powerbi'),
    (N'vw_fretes_powerbi'),
    (N'vw_coletas_powerbi'),
    (N'vw_cotacoes_powerbi'),
    (N'vw_contas_a_pagar_powerbi'),
    (N'vw_localizacao_cargas_powerbi'),
    (N'vw_manifestos_powerbi'),
    (N'vw_bi_monitoramento'),
    (N'vw_inventario_powerbi'),
    (N'vw_sinistros_powerbi'),
    (N'vw_raster_sm_transit_time'),
    (N'vw_dim_filiais'),
    (N'vw_dim_clientes'),
    (N'vw_dim_veiculos'),
    (N'vw_dim_motoristas'),
    (N'vw_dim_planocontas'),
    (N'vw_dim_usuarios');

INSERT INTO @falhas (tipo, nome, detalhe)
SELECT N'VIEW', v.nome, N'View dbo.' + v.nome + N' ausente'
FROM @views v
WHERE OBJECT_ID(N'dbo.' + v.nome, N'V') IS NULL;

DECLARE @migrations TABLE (migration_id NVARCHAR(255) NOT NULL);
INSERT INTO @migrations (migration_id) VALUES
    (N'001_criar_tabela_schema_migrations'),
    (N'002_corrigir_constraint_manifestos'),
    (N'004_adicionar_request_hour_coletas'),
    (N'005_alinhar_sys_execution_history_schema'),
    (N'006_alterar_fretes_indicadores_gestao'),
    (N'007_adicionar_fk_seletiva_manifestos_coletas'),
    (N'008_criar_tabela_sys_replay_idempotency'),
    (N'009_criar_tabela_sys_reconciliation_quarantine'),
    (N'010_harden_coletas_sequence_code'),
    (N'011_alinhar_chave_merge_manifestos_orfaos'),
    (N'012_adicionar_frete_cortesia'),
    (N'013_ajustar_precisao_cubagem_fretes'),
    (N'014_criar_tabelas_raster'),
    (N'015_adicionar_cliente_cnpj_faturas_por_cliente'),
    (N'016_materializar_faturamento_fretes'),
    (N'017_localizacao_cargas_dashboard_operacional'),
    (N'018_adicionar_indice_coletas_request_date_dashboard'),
    (N'019_adicionar_comprovante_fretes_performance'),
    (N'020_adicionar_tipo_motorista_manifestos'),
    (N'021_materializar_comprovante_inventario'),
    (N'022_corrigir_volumes_fretes_faturamento'),
    (N'023_adicionar_noop_count_log_extracoes'),
    (N'024_drop_faturas_graphql'),
    (N'025_materializar_chave_responsavel_destino'),
    (N'026_materializar_chave_usuario_cotacoes');

IF OBJECT_ID(N'dbo.schema_migrations', N'U') IS NOT NULL
BEGIN
    INSERT INTO @falhas (tipo, nome, detalhe)
    SELECT N'MIGRATION', m.migration_id, N'Migration nao registrada em dbo.schema_migrations'
    FROM @migrations m
    WHERE NOT EXISTS (
        SELECT 1
        FROM dbo.schema_migrations sm
        WHERE sm.migration_id = m.migration_id
    );
END;

IF COL_LENGTH(N'dbo.coletas', N'request_hour') IS NULL
    INSERT INTO @falhas VALUES (N'COLUNA', N'dbo.coletas.request_hour', N'Coluna da migration 004 ausente');

IF COL_LENGTH(N'dbo.fretes', N'finished_at') IS NULL
    INSERT INTO @falhas VALUES (N'COLUNA', N'dbo.fretes.finished_at', N'Coluna da migration 006 ausente');

IF COL_LENGTH(N'dbo.fretes', N'fit_dpn_performance_finished_at') IS NULL
    INSERT INTO @falhas VALUES (N'COLUNA', N'dbo.fretes.fit_dpn_performance_finished_at', N'Coluna da migration 006 ausente');

IF COL_LENGTH(N'dbo.fretes', N'corporation_sequence_number') IS NULL
    INSERT INTO @falhas VALUES (N'COLUNA', N'dbo.fretes.corporation_sequence_number', N'Coluna da migration 006 ausente');

IF COL_LENGTH(N'dbo.log_extracoes', N'noop_count') IS NULL
    INSERT INTO @falhas VALUES (N'COLUNA', N'dbo.log_extracoes.noop_count', N'Coluna da migration 023 ausente');

IF COL_LENGTH(N'dbo.fretes', N'cortesia') IS NULL
    INSERT INTO @falhas VALUES (N'COLUNA', N'dbo.fretes.cortesia', N'Coluna da migration 012 ausente');

IF COL_LENGTH(N'dbo.fretes', N'data_referencia_faturamento') IS NULL
    INSERT INTO @falhas VALUES (N'COLUNA', N'dbo.fretes.data_referencia_faturamento', N'Coluna da migration 016 ausente');

IF COL_LENGTH(N'dbo.fretes', N'is_elegivel_faturamento') IS NULL
    INSERT INTO @falhas VALUES (N'COLUNA', N'dbo.fretes.is_elegivel_faturamento', N'Coluna da migration 016 ausente');

IF COL_LENGTH(N'dbo.fretes', N'filial_nome_key') IS NULL
    INSERT INTO @falhas VALUES (N'COLUNA', N'dbo.fretes.filial_nome_key', N'Chave materializada de filial/responsavel fallback ausente');

IF COL_LENGTH(N'dbo.cotacoes', N'user_name_key') IS NULL
    INSERT INTO @falhas VALUES (N'COLUNA', N'dbo.cotacoes.user_name_key', N'Chave materializada de usuario emissor de cotacao ausente');

IF COL_LENGTH(N'dbo.inventario', N'flag_comprovante_anexado') IS NULL
    INSERT INTO @falhas VALUES (N'COLUNA', N'dbo.inventario.flag_comprovante_anexado', N'Flag materializada de comprovante anexado ausente');

IF COL_LENGTH(N'dbo.faturas_por_cliente', N'cliente_cnpj') IS NULL
    INSERT INTO @falhas VALUES (N'COLUNA', N'dbo.faturas_por_cliente.cliente_cnpj', N'Coluna da migration 015 ausente');

IF COL_LENGTH(N'dbo.localizacao_cargas', N'localizacao_hash') IS NULL
    INSERT INTO @falhas VALUES (N'COLUNA', N'dbo.localizacao_cargas.localizacao_hash', N'Coluna da migration 017 ausente');

IF COL_LENGTH(N'dbo.localizacao_cargas', N'status_normalized') IS NULL
    INSERT INTO @falhas VALUES (N'COLUNA', N'dbo.localizacao_cargas.status_normalized', N'Coluna da migration 017 ausente');

IF COL_LENGTH(N'dbo.localizacao_cargas', N'taxed_weight_decimal') IS NULL
    INSERT INTO @falhas VALUES (N'COLUNA', N'dbo.localizacao_cargas.taxed_weight_decimal', N'Coluna da migration 017 ausente');

IF COL_LENGTH(N'dbo.localizacao_cargas', N'destination_branch_key') IS NULL
    INSERT INTO @falhas VALUES (N'COLUNA', N'dbo.localizacao_cargas.destination_branch_key', N'Chave materializada de responsavel destino ausente');

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_fpc_cliente_cnpj'
      AND object_id = OBJECT_ID(N'dbo.faturas_por_cliente')
)
    INSERT INTO @falhas VALUES (N'INDICE', N'IX_fpc_cliente_cnpj', N'Indice filtrado da migration 015 ausente');

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_fretes_faturamento_data_elegivel'
      AND object_id = OBJECT_ID(N'dbo.fretes')
)
    INSERT INTO @falhas VALUES (N'INDICE', N'IX_fretes_faturamento_data_elegivel', N'Indice da migration 016 ausente');

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_fretes_faturamento_responsavel_key'
      AND object_id = OBJECT_ID(N'dbo.fretes')
)
    INSERT INTO @falhas VALUES (N'INDICE', N'IX_fretes_faturamento_responsavel_key', N'Indice da migration 025 ausente');

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_fretes_performance_previsao_key'
      AND object_id = OBJECT_ID(N'dbo.fretes')
)
    INSERT INTO @falhas VALUES (N'INDICE', N'IX_fretes_performance_previsao_key', N'Indice da migration 025 ausente');

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_localizacao_tracking_dashboard'
      AND object_id = OBJECT_ID(N'dbo.localizacao_cargas')
)
    INSERT INTO @falhas VALUES (N'INDICE', N'IX_localizacao_tracking_dashboard', N'Indice da migration 017 ausente');

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_localizacao_destination_branch_key'
      AND object_id = OBJECT_ID(N'dbo.localizacao_cargas')
)
    INSERT INTO @falhas VALUES (N'INDICE', N'IX_localizacao_destination_branch_key', N'Indice da migration 025 ausente');

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_cotacoes_requested_at_usuario_key'
      AND object_id = OBJECT_ID(N'dbo.cotacoes')
)
    INSERT INTO @falhas VALUES (N'INDICE', N'IX_cotacoes_requested_at_usuario_key', N'Indice da migration 026 ausente');

IF EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_cotacoes_usuario_key_requested_at'
      AND object_id = OBJECT_ID(N'dbo.cotacoes')
)
    INSERT INTO @falhas VALUES (N'INDICE', N'IX_cotacoes_usuario_key_requested_at', N'Indice legado com usuario como chave lider deve ser removido');

IF EXISTS (
    SELECT 1
    FROM sys.indexes i
    WHERE i.name = N'IX_cotacoes_requested_at_usuario_key'
      AND i.object_id = OBJECT_ID(N'dbo.cotacoes')
)
AND NOT EXISTS (
    SELECT 1
    FROM sys.indexes i
    INNER JOIN sys.index_columns ic1
        ON ic1.object_id = i.object_id
       AND ic1.index_id = i.index_id
       AND ic1.key_ordinal = 1
    INNER JOIN sys.columns c1
        ON c1.object_id = ic1.object_id
       AND c1.column_id = ic1.column_id
    INNER JOIN sys.index_columns ic2
        ON ic2.object_id = i.object_id
       AND ic2.index_id = i.index_id
       AND ic2.key_ordinal = 2
    INNER JOIN sys.columns c2
        ON c2.object_id = ic2.object_id
       AND c2.column_id = ic2.column_id
    WHERE i.name = N'IX_cotacoes_requested_at_usuario_key'
      AND i.object_id = OBJECT_ID(N'dbo.cotacoes')
      AND c1.name = N'requested_at'
      AND ic1.is_descending_key = 1
      AND c2.name = N'user_name_key'
)
    INSERT INTO @falhas VALUES (N'INDICE', N'IX_cotacoes_requested_at_usuario_key', N'Ordem esperada: requested_at DESC como chave lider e user_name_key como segunda chave');

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_inventario_comprovante_minuta'
      AND object_id = OBJECT_ID(N'dbo.inventario')
      AND filter_definition LIKE N'%flag_comprovante_anexado%'
)
    INSERT INTO @falhas VALUES (N'INDICE', N'IX_inventario_comprovante_minuta', N'Indice filtrado de comprovante anexado ausente ou desatualizado');

IF NOT EXISTS (
    SELECT 1
    FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.vw_fretes_powerbi')
      AND name = N'Comprovante Anexado'
)
    INSERT INTO @falhas VALUES (N'VIEW_COLUNA', N'dbo.vw_fretes_powerbi.[Comprovante Anexado]', N'Coluna do KPI Comprovante Anexado ausente');

IF NOT EXISTS (
    SELECT 1
    FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.vw_fretes_powerbi')
      AND name = N'Volumes'
)
    INSERT INTO @falhas VALUES (N'VIEW_COLUNA', N'dbo.vw_fretes_powerbi.[Volumes]', N'Coluna do KPI Volumes ausente');

IF NOT EXISTS (
    SELECT 1
    FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.vw_fretes_powerbi')
      AND name = N'Responsável Região Destino Key'
)
    INSERT INTO @falhas VALUES (N'VIEW_COLUNA', N'dbo.vw_fretes_powerbi.[Responsável Região Destino Key]', N'Chave controlada para filtro de responsavel ausente');

IF NOT EXISTS (
    SELECT 1
    FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.vw_cotacoes_powerbi')
      AND name = N'Usuario Key'
)
    INSERT INTO @falhas VALUES (N'VIEW_COLUNA', N'dbo.vw_cotacoes_powerbi.[Usuario Key]', N'Chave controlada para filtro de usuario emissor ausente');

IF OBJECT_DEFINITION(OBJECT_ID(N'dbo.vw_fretes_powerbi')) NOT LIKE N'%lc.invoices_volumes%'
    INSERT INTO @falhas VALUES (N'VIEW_DEFINICAO', N'dbo.vw_fretes_powerbi.[Volumes]', N'Volumes deve usar localizacao_cargas.invoices_volumes como fonte oficial');

IF EXISTS (
    SELECT 1
    FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.vw_fretes_powerbi')
      AND name = N'Comprovante Anexado'
)
AND COL_LENGTH(N'dbo.inventario', N'flag_comprovante_anexado') IS NOT NULL
BEGIN
    DECLARE @inventarioComComprovante BIT = 0;
    DECLARE @viewComComprovante BIT = 0;

    EXEC sys.sp_executesql
        N'SELECT @saida = CASE WHEN EXISTS (SELECT 1 FROM dbo.inventario WHERE flag_comprovante_anexado = 1) THEN 1 ELSE 0 END',
        N'@saida BIT OUTPUT',
        @saida = @inventarioComComprovante OUTPUT;

    EXEC sys.sp_executesql
        N'SELECT @saida = CASE WHEN EXISTS (SELECT 1 FROM dbo.vw_fretes_powerbi WHERE [Comprovante Anexado] = N''Sim'') THEN 1 ELSE 0 END',
        N'@saida BIT OUTPUT',
        @saida = @viewComComprovante OUTPUT;

    IF @inventarioComComprovante = 1 AND @viewComComprovante = 0
        INSERT INTO @falhas VALUES (N'DADO', N'dbo.vw_fretes_powerbi.[Comprovante Anexado]', N'Inventario possui comprovantes, mas a view de fretes nao publica Sim');
END;

IF EXISTS (
    SELECT 1
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'dbo'
      AND TABLE_NAME = 'coletas'
      AND COLUMN_NAME = 'sequence_code'
      AND IS_NULLABLE = 'YES'
)
    INSERT INTO @falhas VALUES (N'COLUNA', N'dbo.coletas.sequence_code', N'Deve estar NOT NULL apos migration 010');

IF EXISTS (
    SELECT 1
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'dbo'
      AND TABLE_NAME = 'fretes'
      AND COLUMN_NAME IN ('total_cubic_volume', 'cubages_cubed_weight')
      AND (NUMERIC_PRECISION <> 18 OR NUMERIC_SCALE <> 6)
)
    INSERT INTO @falhas VALUES (N'COLUNA', N'dbo.fretes.cubagem', N'Campos de cubagem devem ser DECIMAL(18,6)');

IF NOT EXISTS (
    SELECT 1
    FROM sys.foreign_keys
    WHERE name = N'FK_raster_viagem_paradas_viagens'
      AND parent_object_id = OBJECT_ID(N'dbo.raster_viagem_paradas')
)
    INSERT INTO @falhas VALUES (N'FK', N'FK_raster_viagem_paradas_viagens', N'FK Raster paradas -> viagens ausente');

SELECT tipo, nome, detalhe
FROM @falhas
ORDER BY tipo, nome;

IF EXISTS (SELECT 1 FROM @falhas)
BEGIN
    THROW 51034, 'Schema de recriacao inconsistente. Verifique o resultado de database/validacao/034_validar_schema_recriacao.sql.', 1;
END;

PRINT 'Schema de recriacao validado com sucesso.';
